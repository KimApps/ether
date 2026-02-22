# Trade-offs & Design Decisions

This document captures the concrete decisions made during development, what was
considered as an alternative, why the current approach was chosen, and what the
production path would look like.

---

## 1. `CompletableDeferred` in `SigningCoordinator` (cross-module bridge)

**Chosen approach:** `SigningCoordinator` holds a `mutableMapOf<String, CompletableDeferred<SigningResultEntity>>`.
`WithdrawViewModel` calls `requestSignature()`, which suspends on `deferred.await()`.
`SigningViewModel` calls `provideResult()` to complete the deferred and unblock the caller.

**Alternatives considered:**

| Alternative | Why rejected |
|---|---|
| Shared `StateFlow` | Replays the last value to new collectors — a new SigningViewModel would incorrectly receive an already-resolved result |
| `Channel<SigningResultEntity>` | One-to-one delivery is fine, but a Channel has no natural key/challenge mapping for concurrent requests |
| Callback lambda passed via nav argument | Nav arguments are serialised strings; lambdas cannot be serialised |
| Event bus (e.g. `MutableSharedFlow` in a singleton) | Works, but receivers must filter by challenge themselves — moves coordination logic into every consumer |

**Trade-off:** `CompletableDeferred` is a clean one-shot promise per challenge key, but it
does not automatically handle cancellation when the user navigates back before
`provideResult` is called. The `finally` block in `requestSignature` removes the
entry from the map, but the deferred is never explicitly cancelled — the calling
coroutine (in `viewModelScope`) is cancelled by the OS, which unblocks `await()`
via `CancellationException`. This is correct but relies on `viewModelScope`
cancellation rather than an explicit `deferred.cancel()` call.

**Production path:** Wrap `deferred.await()` in `suspendCancellableCoroutine` and
call `deferred.cancel()` inside `invokeOnCancellation` so back-press is
explicitly propagated even if the ViewModel scope outlives the screen.

---

## 2. Mock data sources instead of real API calls

**Chosen approach:** `SigningRemoteDS` and `WithdrawRemoteDS` simulate network
latency with `delay()` and return hardcoded responses. `NetworkClient` (Retrofit
+ Ktor) is fully built but not wired to any real backend.

**Trade-off:** The entire architecture — use cases, repository interfaces, mappers,
DI bindings — is production-ready. Swapping a mock DS for a real one is a
single class replacement with no changes to domain or presentation layers.
The assignment is evaluating architecture, not a live backend.

**What was sacrificed:** Real error paths (timeouts, 4xx/5xx) are not exercised
by the current UI flows. The `AppNetworkException` sealed hierarchy exists and
is mapped correctly, but it is never triggered in a normal run.

**Production path:** Replace `WithdrawRemoteDS` with a `RetrofitApiClientImpl`
or `KtorApiClient` call through `NetworkClient`. No use-case or ViewModel code
changes.

---

## 3. Two HTTP clients (Retrofit + Ktor) shipped simultaneously

**Chosen approach:** Both `RetrofitApiClientImpl` and `KtorApiClient` implement
the same `ApiClient` interface. Hilt binds one of them to `ApiClient` in
`NetworkModule`. The other is provided but unused at runtime.

**Trade-off:** Doubles the HTTP dependency footprint (OkHttp + Ktor engine both
in the APK). In a production app you would pick one and delete the other.

**Reason for keeping both:** Demonstrates the abstraction layer — the interface
makes the HTTP library a swappable implementation detail, not a hard dependency.
A reviewer can see both implementations and verify the abstraction holds.

**Production path:** Delete one implementation and its Hilt `@Provides` method.
One line change in `NetworkModule` to switch.

---

## 4. WalletConnect session proposals are auto-approved

**Chosen approach:** `WalletConnectManager.onSessionProposal()` immediately calls
`WalletKit.approveSession()` with a hardcoded `eip155:1` (Ethereum mainnet)
namespace and a static mock account address.

**Trade-off:** Skips the account-selection UI that a real wallet shows. The user
never sees which account they are connecting or which chains the dApp is
requesting access to.

**Reason:** This is a mock wallet for demonstration. The goal is to show the
WalletConnect integration end-to-end (pair → session → sign → respond), not to
build a full wallet account manager.

**Production path:** Show a modal before approving that lists the dApp's requested
chains and accounts. Let the user select from locally stored HD wallet accounts
before calling `approveSession`.

---

## 5. Mock EOA signature (`0x-EOA-MOCK-SIG-...`)

**Chosen approach:** `SigningViewModel.onWalletSignApproved()` builds a fake
signature string `"0x-EOA-MOCK-SIG-${challenge.take(10)}"` and sends it back
to the dApp as the JSON-RPC result.

**Trade-off:** The dApp receives a string that is not a valid ECDSA signature.
Any on-chain verification would reject it.

**Reason:** Producing a real `personal_sign` result requires an actual private key
or HSM. Integrating a software wallet (e.g. Web3j `Credentials`) is out of scope
for the assignment but is the natural next step.

**Production path:** Use `Web3j.sign(challenge, credentials)` or delegate to the
Android Keystore via a hardware-backed `ECKeyPair` to produce a real 65-byte
ECDSA signature in `r + s + v` format.

---

## 6. Navigation3 (pre-stable, version 1.0.1)

**Chosen approach:** Jetpack Navigation3 with `@Serializable` data-class routes
(`AppRoute.Home`, `AppRoute.Withdraw`, `AppRoute.Signing`). Type-safe arguments
are passed directly as route properties — no manual string encoding.

**Alternatives considered:**

| Alternative | Why not chosen |
|---|---|
| Navigation Component (stable, XML) | XML graph doesn't compose naturally with multi-module Compose |
| Navigation Compose (stable, `NavController`) | String-based deep links require manual encoding/decoding of arguments; less type-safe |
| Custom back-stack `List<AppRoute>` with `when` | Considered, but Navigation3 provides `NavDisplay` and `entryProvider` for free |

**Trade-off:** Navigation3 is pre-stable — its API may change before 1.0 GA.
Chose it anyway to demonstrate awareness of the upcoming standard and to avoid
the boilerplate of string-based routes.

---

## 7. `SharedFlow` for `sessionRequests` vs `Channel`

**Chosen approach:** `WalletConnectManager` emits incoming `personal_sign` requests
through a `MutableSharedFlow(replay = 0)`.

**Trade-off vs `Channel`:**

| | `SharedFlow(replay=0)` | `Channel` |
|---|---|---|
| Missed emissions | Dropped if no collector is active | Buffered and delivered later |
| Multiple collectors | All receive the same emission | Only one receives each item |
| Replay on re-subscribe | No (correct — old request should not reappear) | N/A |

Chosen because a session request that arrives while no ViewModel is collecting
(e.g. during a configuration change) should be **dropped**, not queued. Queuing
an already-expired request and showing its dialog after re-creation would confuse
the user.

**Production path:** If request delivery guarantee is needed (user must always see
every request), use `Channel(BUFFERED)` and handle expiry on the consumer side.

---

## 8. `applicationScope` for emitting WalletConnect events

**Chosen approach:** `WalletConnectManager` receives an `applicationScope:
CoroutineScope` (bound to `ProcessLifecycleOwner` in `AppModule`) and uses it
to `launch` emissions of session requests.

**Trade-off:** Using `viewModelScope` inside `WalletConnectManager` would be wrong
because the manager is a `@Singleton` that outlives any single ViewModel. The
application scope ensures that SDK callbacks (which arrive on background threads)
can always emit to the flow, even while no ViewModel is alive.

**What was sacrificed:** `applicationScope` coroutines are never automatically
cancelled — a leaked coroutine here would run forever. The current `launch`
bodies are short-lived (`emit` calls), so this is safe in practice.

---

## 9. `SharedPreferences` (`LocalStorageClient`) alongside DataStore (`TokenManager`)

**Chosen approach:** `core:local-storage` provides both a `LocalStorageClient`
(wrapping `SharedPreferences`) and a `DataStore<Preferences>` instance. They
coexist in the same module.

**Trade-off:** `SharedPreferences` is synchronous and has no coroutine support.
`DataStore` is the modern replacement. Having both adds surface area and the
risk that a developer reaches for the wrong one.

**Reason:** `LocalStorageClient` is a general-purpose key/value store for simple,
non-sensitive data (e.g. UI preferences). `DataStore` (via `TokenManager`) is
used for auth tokens, where the coroutine-safe, transactional API matters.
In production, `SharedPreferences` would be removed and everything migrated to
`DataStore` or `EncryptedSharedPreferences` for sensitive data.

---

## 10. `ErrorLoggerService` abstraction with `LogcatErrorLogger`

**Chosen approach:** All use cases inject `ErrorLoggerService` and call
`logException()` in `catch` blocks. The only binding is `LogcatErrorLogger`,
which forwards to Android Logcat.

**Trade-off:** In production this provides zero alerting — a crash silently
printed to Logcat is invisible in the field.

**Reason:** Firebase Crashlytics / Sentry require project setup and API keys.
The abstraction demonstrates the pattern without the setup overhead. A
`CrashlyticsErrorLogger` would be a one-class addition with a one-line change
in `ErrorLoggerModule`.

---

## 11. `Parcelable` on `SigningResultEntity`

**Chosen approach:** `SigningResultEntity` is `@Parcelize` / `Parcelable`.

**Trade-off:** Parcelable adds a small amount of generated boilerplate and
couples the domain entity to the Android framework (`android.os.Parcelable`).
Clean Architecture purists would keep domain entities framework-free.

**Reason:** `SigningResultEntity` needs to survive process death if it is ever
passed across process boundaries (e.g. via a `Bundle`). In the current
implementation it is never put in a Bundle — it travels through
`CompletableDeferred` in memory — so `Parcelable` is technically unnecessary
but costs nothing and future-proofs the type.

**Production path:** If strict Clean Architecture is required, move `Parcelable`
to a separate DTO/model in the presentation layer and map from the domain entity.

---

## 12. Hilt over manual DI or Koin

**Chosen approach:** Dagger Hilt with KSP annotation processing throughout all
modules.

**Trade-off vs Koin:**

| | Hilt | Koin |
|---|---|---|
| Verification | Compile-time | Runtime |
| Android ViewModel integration | `@HiltViewModel` built-in | `viewModel {}` DSL |
| Learning curve | Higher | Lower |
| APK size impact | Code-gen at compile time | Reflection at runtime |

Chosen because compile-time verification means a missing binding is a build
error, not a `NullPointerException` in production. The `@HiltViewModel`
annotation also removes the `ViewModelFactory` boilerplate entirely.

