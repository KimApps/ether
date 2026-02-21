# Ether — Android Interview Project

A modular Android wallet application demonstrating a production-ready architecture,
featuring a **Passkey signing flow** and a **WalletConnect (Reown WalletKit) EOA wallet
integration** as a bonus feature.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [MVI Pattern](#mvi-pattern)
3. [Module Structure](#module-structure)
4. [Core Modules](#core-modules)
    - [network](#corenetwork)
    - [local-storage](#corelocal-storage)
    - [navigation](#corenavigation)
    - [utils](#coreutils)
    - [error-logger](#coreerror-logger)
5. [Feature Modules](#feature-modules)
    - [home](#featureshome)
    - [withdraw](#featureswithdraw)
6. [Shared Modules](#shared-modules)
    - [signing](#sharedsigning)
    - [user](#shareduser)
    - [ui](#sharedui)
7. [WalletConnect Integration](#walletconnect-integration)
8. [Signing Flow — End to End](#signing-flow--end-to-end)
9. [Tech Stack](#tech-stack)
10. [Build Configuration](#build-configuration)

---

## Architecture Overview

The project follows **Clean Architecture** split across three horizontal layers,
implemented inside a **multi-module Gradle project**:

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │  Compose UI · ViewModels · MVI
├─────────────────────────────────────────┤
│             Domain Layer                │  Use Cases · Entities · Repository interfaces
├─────────────────────────────────────────┤
│              Data Layer                 │  Repository implementations · Data sources · DTOs · Mappers
└─────────────────────────────────────────┘
```

Each module has its own `layer/` folder containing these three sub-layers, keeping
business logic completely independent of Android framework classes.

**Key architectural decisions:**

| Decision | Rationale |
|---|---|
| Multi-module | Enforces strict boundaries — a feature module physically cannot import signing internals |
| Clean Architecture | Domain layer has zero Android dependencies; easy to unit test |
| MVI | Single source of truth per screen; unidirectional data flow; predictable state |
| Hilt | Compile-time verified DI; no runtime crashes from missing bindings |
| Kotlin Coroutines + Flow | Structured concurrency; reactive UI without RxJava overhead |

---

## MVI Pattern

Every screen in the app follows **Model-View-Intent**:

```
User Action
    │
    ▼
Intent (sealed class)
    │
    ▼
ViewModel.onIntent()  ──►  State (data class, immutable)
    │                           │
    ▼                           ▼
Effect (Channel, one-shot)   UI (Compose collectAsState)
    │
    ▼
Navigation / system call
```

### Three contracts per screen

| Class | Type | Purpose |
|---|---|---|
| `XState` | `data class` | Full immutable snapshot of what the UI renders |
| `XIntent` | `sealed class` | Every possible user action or system event |
| `XEffect` | `sealed class` | One-time events (navigation, toasts) delivered via `Channel` |

**Why `Channel` for effects instead of `StateFlow`?**
A `Channel` delivers each emission exactly once regardless of recompositions.
A `StateFlow` replays the last value to new collectors, which would cause
navigation actions to fire again on screen re-entry.

---

## Module Structure

```
Ether/
├── app/                          # Application entry point
├── modules/
│   ├── core/
│   │   ├── network/              # HTTP clients (Retrofit + Ktor), token management
│   │   ├── local-storage/        # SharedPreferences wrapper + DataStore
│   │   ├── navigation/           # Shared AppRoute sealed interface
│   │   ├── error-logger/         # ErrorLoggerService interface + LogcatErrorLogger impl
│   │   └── utils/                # Common utilities
│   ├── features/
│   │   ├── home/                 # Home screen
│   │   └── withdraw/             # Withdraw flow
│   └── shared/
│       ├── signing/              # Signing screen + WalletConnect integration
│       ├── user/                 # User data (profile, session)
│       └── ui/                   # Shared Compose design system components
└── gradle/
    └── libs.versions.toml        # Version catalog (single source of truth for all deps)
```

---

## Core Modules

### `core:network`

> **Not actively used in the current flow** (signing uses mock data sources),
> but fully built as a reusable production-ready HTTP layer.

The most interesting core module. It solves a common problem: **feature modules
should not care which HTTP library you use.** The solution is a two-layer abstraction:

#### `ApiClient` interface
A single interface with `get`, `post`, `put`, `delete`, `patch` methods that return
an `ApiResponse` (just a wrapper around the raw body string). Two concrete
implementations exist behind this interface:

| Implementation | Library | Notes |
|---|---|---|
| `RetrofitApiClientImpl` | Retrofit 2 + OkHttp | Classic Retrofit service backed by `RetrofitApiService` |
| `KtorApiClient` | Ktor | Kotlin-first HTTP client with coroutine-native API |

Both are wired in `NetworkModule` and **Hilt injects whichever one is bound to
`ApiClient`**. Swapping the HTTP library is a one-line change in the DI module.

#### `NetworkClient`
A thin wrapper on top of `ApiClient` that adds **Gson deserialisation** using
inline reified generics, so call sites look like:

```kotlin
val user: UserDto = networkClient.get<UserDto>(ApiEndpoint.PROFILE)
```

#### Token management
- `TokenManager` — stores `access_token` and `refresh_token` in **Jetpack DataStore**
  (the coroutine-safe replacement for `SharedPreferences`). Exposes both `Flow`-based
  and blocking accessors.
- `AuthInterceptor` (OkHttp) — attaches `Authorization: Bearer <token>` to every request.
- `RefreshTokenInterceptor` (OkHttp) — intercepts `401` responses, calls the refresh
  endpoint, saves the new tokens, and retries the original request transparently.
- Ktor equivalent is configured directly in `NetworkModule` using Ktor's built-in
  `Auth` plugin with `bearer { }` DSL and a `loadTokens` / `refreshTokens` callback.

#### Error hierarchy
`AppNetworkException` is a sealed class that maps raw HTTP status codes to
meaningful domain exceptions:

```
AppNetworkException
├── AppBadResponseException   (4xx)
├── AppUnauthorizedException  (401)
├── AppForbiddenException     (403)
├── AppServerException        (5xx)
├── AppTimeoutException
├── AppConnectionException
└── AppUnknownException
```

Both `RetrofitNetworkException` and `KtorNetworkException` catch library-specific
errors and re-throw them as the appropriate `AppNetworkException` subtype, so the
domain layer never sees Retrofit or Ktor types.

---

### `core:local-storage`

Provides two storage options:

| Class | Storage | Use case |
|---|---|---|
| `LocalStorageClient` | `SharedPreferences` | Simple key/value: strings, ints, booleans |
| `DataStore<Preferences>` | Jetpack DataStore | Coroutine-safe; used by `TokenManager` in `core:network` |

Both are provided as singletons through `LocalStorageModule`.

---

### `core:navigation`

Defines the **single shared `AppRoute` sealed interface** that all modules reference.
This prevents circular dependencies: feature modules depend on `core:navigation`
but not on each other.

```kotlin
sealed interface AppRoute {
    data object Home : AppRoute
    data object Withdraw : AppRoute
    data class Signing(val challenge: String, val operationType: String) : AppRoute
}
```

Routes are `@Serializable` so they can be passed as Navigation 3 typed arguments
without manual string encoding.

---

### `core:utils`

Placeholder module for shared extension functions and utilities.
Currently empty — ready to receive helpers like date formatters, string extensions, etc.

---

### `core:error-logger`

A thin observability abstraction that decouples error reporting from any specific
crash-reporting SDK.

#### `ErrorLoggerService` interface

```kotlin
interface ErrorLoggerService {
    fun logException(
        featureName: String,
        errorTitle: String,
        exception: Throwable,
        stackTrace: Array<StackTraceElement>? = null
    )
}
```

Every use case across the app receives `ErrorLoggerService` through constructor
injection and calls it inside `catch` blocks. The domain layer never references
Logcat, Firebase, or Sentry directly — it only knows about this interface.

#### `LogcatErrorLogger` — development implementation

The current binding (wired in `ErrorLoggerModule`) simply forwards exceptions to
Android Logcat:

```kotlin
Log.e("ErrorLogger --->", "[$featureName] $errorTitle", exception)
```

This is intentional for this project to avoid Firebase / Sentry setup overhead.

#### Swapping the implementation in a real app

Because `ErrorLoggerService` is bound through Hilt's `@Binds`, replacing the
implementation is a **one-line change** in `ErrorLoggerModule` — no call sites
need to be touched.

| Environment | Implementation class | What it does |
|---|---|---|
| Debug / CI | `LogcatErrorLogger` *(current)* | Prints to Logcat |
| Production | `CrashlyticsErrorLogger` | `FirebaseCrashlytics.getInstance().recordException(e)` |
| Production (alt) | `SentryErrorLogger` | `Sentry.captureException(e)` |

Example Crashlytics implementation:

```kotlin
@Singleton
class CrashlyticsErrorLogger @Inject constructor() : ErrorLoggerService {
    override fun logException(
        featureName: String,
        errorTitle: String,
        exception: Throwable,
        stackTrace: Array<StackTraceElement>?
    ) {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("feature", featureName)
            setCustomKey("errorTitle", errorTitle)
            recordException(exception)
        }
    }
}
```

Then in `ErrorLoggerModule`, simply change:

```kotlin
// Before
abstract fun bindErrorLoggerService(impl: LogcatErrorLogger): ErrorLoggerService

// After
abstract fun bindErrorLoggerService(impl: CrashlyticsErrorLogger): ErrorLoggerService
```

#### Where it is used

| Use case | Module | `featureName` tag |
|---|---|---|
| `SignChallengeUseCase` | `shared:signing` | `"signing"` |
| `GetQuotationUseCase` | `features:withdraw` | `"withdraw"` |
| `SubmitWithdrawUseCase` | `features:withdraw` | `"withdraw"` |

---

## Feature Modules

### `features:home`

Entry point of the app. Navigates to the Withdraw flow.

### `features:withdraw`

Handles the withdrawal form:
- User inputs amount and destination address.
- Calls `GetQuotationUseCase` to fetch a transaction quotation.
- Calls `SubmitWithdrawUseCase` which internally calls `SigningCoordinator.requestSignature()`
  to suspend and wait for the signing screen to complete.
- Once a `SigningResultEntity.Signed` result is received, submits the transaction.

Both `GetQuotationUseCase` and `SubmitWithdrawUseCase` inject `ErrorLoggerService`
and log any caught exception before re-throwing it, so the ViewModel receives the
original exception while the crash reporter captures it silently in the background.

`TransactionQuotationMapper` maps the raw `TransactionQuotationDto` from the data
source into the clean `TransactionQuotationEntity` used by the domain layer.

---

## Shared Modules

### `shared:signing`

The most complex module in the project. Contains:

#### Presentation
- `SigningPage` — Compose screen with two signing paths
- `SigningViewModel` — MVI ViewModel, orchestrates both paths
- `SigningState` / `SigningIntent` / `SigningEffect` — MVI contracts
- **Components:**
  - `SigningHeader` — operation title
  - `ChallengeCard` — truncated challenge preview
  - `WalletConnectSection` — full WalletConnect pairing UI (3 states)
  - `SigningApprovalDialog` — approve/reject dialog for incoming dApp requests

#### Domain
- `SigningCoordinator` — `@Singleton` bridge between the withdraw feature and the
  signing screen. Uses `CompletableDeferred` to suspend the feature coroutine until
  signing completes.
- `SignChallengeUseCase` — delegates Passkey signing to the repository. Injects
  `ErrorLoggerService` and logs any exception before re-throwing it.
- `SignWithWalletUseCase` — delegates WalletConnect signing to the repository.
- `SigningResultEntity` — sealed result: `Signed(signature)`, `Cancelled`, `Error(message)`.

#### Data
- `SigningRemoteDS` — mock data source; simulates a 2-second signing delay.
- `SigningResultMapper` — maps `SigningResultDto` → `SigningResultEntity`.

#### WalletConnect
- `WalletConnectInitializer` — bootstraps `CoreClient` + `WalletKit` once per process.
- `WalletConnectManager` — implements `WalletKit.WalletDelegate`; exposes reactive
  `isConnected: StateFlow` and `sessionRequests: SharedFlow` to the ViewModel.

---

### `shared:user`

Manages the authenticated user's data:
- `UserRepository` / `UserRepositoryImpl`
- `UserLocalDS` (DataStore) + `UserRemoteDS` (mock API)
- `UserMapper` — maps `UserDto` → `UserEntity`
- `GetUserUseCase`

---

### `shared:ui`

Shared Compose design system. Placeholder for reusable design tokens, typography,
colour schemes, and common components used across multiple feature modules.

---

## WalletConnect Integration

> **Bonus feature** — demonstrates how to integrate the Reown WalletKit SDK
> to turn the app into an EIP-1193 compatible wallet.

### How it works — step by step

```
1. User taps "Connect EOA Wallet"
       │
       ▼
2. URI input field appears. User opens https://react-app.walletconnect.com
   in a browser, connects, and copies the wc: pairing URI.
       │
       ▼
3. User pastes the URI and taps "Pair Wallet"
   → ViewModel calls WalletConnectManager.pair(uri)
   → CoreClient.Pairing.pair() sends a handshake to the WalletConnect relay
       │
       ▼
4. WalletConnectManager.onSessionProposal() fires (background thread)
   → Auto-approves with Ethereum mainnet (eip155:1) namespace
   → WalletKit.approveSession()
       │
       ▼
5. WalletConnectManager.onSessionSettleResponse() fires
   → isConnected = true
   → UI switches to "Wallet Connected ✅" card
       │
       ▼
6. User goes back to the browser dApp and triggers a personal_sign request
       │
       ▼
7. WalletConnectManager.onSessionRequest() fires
   → Emits the request through sessionRequests SharedFlow (application scope)
   → ViewModel stores it in state.pendingRequest
   → SigningApprovalDialog appears
       │
       ▼
8a. User taps "Approve"
    → ViewModel generates mock signature
    → WalletConnectManager.approveRequest() sends JsonRpcResult back to dApp
    → coordinator.provideResult() unblocks the withdraw flow
    → screen closes

8b. User taps "Reject"
    → WalletConnectManager.rejectRequest() sends JsonRpcError (EIP-1193 code 4001)
    → dialog dismissed
```

### Key design decisions

| Decision | Reason |
|---|---|
| `@Singleton` for `WalletConnectManager` | SDK callbacks arrive on a background thread after the ViewModel may have been recreated; singleton ensures they are never lost |
| `applicationScope` for emitting `sessionRequests` | ViewModel scope is cancelled on screen leave; app scope survives configuration changes |
| `SharedFlow` (no replay) for session requests | Each request should be seen once; replay would re-show an already-handled dialog |
| `StateFlow` for `isConnected` | Connection state should always reflect the current truth; new collectors need the latest value immediately |
| Auto-approve session proposals | Mock wallet — production wallets would show an account selection UI |

---

## Signing Flow — End to End

```
WithdrawViewModel
    │
    │  coordinator.requestSignature(rq)  ← suspends here
    ▼
SigningCoordinator
    │  emits to signingRequest SharedFlow
    ▼
Navigation layer (observer)
    │  navigates to Signing(challenge, operationType)
    ▼
SigningPage / SigningViewModel
    │
    ├─── Passkey path ────────────────────────────────────────┐
    │    SignChallengeUseCase → SigningRemoteDS.signChallenge  │
    │    (mock: 2s delay + challenge + "_signed")              │
    │                                                          │
    └─── WalletConnect path ──────────────────────────────────┘
         pair URI → auto-approve session → wait for
         onSessionRequest → show dialog → approve
         → WalletKit.respondSessionRequest

Both paths end with:
    coordinator.provideResult(challenge, SigningResultEntity.Signed(sig))   ← success
    coordinator.provideResult(challenge, SigningResultEntity.Error(msg))     ← caught exception
        │
        ▼
    CompletableDeferred.complete(result)
        │
        ▼
    WithdrawViewModel resumes with the signature (or handles the error)
        │
        ▼
    SubmitWithdrawUseCase → transaction submitted
```

---

## Tech Stack

| Category | Library | Version |
|---|---|---|
| Language | Kotlin | 2.2.21 |
| UI | Jetpack Compose + Material 3 | BOM 2025.12.00 |
| Navigation | Navigation3 (typed routes) | 1.0.1 |
| DI | Hilt | 2.57.2 |
| DI annotation processing | KSP | 2.3.3 |
| HTTP (option A) | Retrofit 2 + OkHttp 5 | 3.0.0 / 5.3.2 |
| HTTP (option B) | Ktor | 3.3.3 |
| JSON (Retrofit) | Gson | bundled with Retrofit |
| JSON (Ktor) | kotlinx.serialization | 1.9.0 |
| Local storage | Jetpack DataStore Preferences | 1.2.0 |
| Database | Room | 2.8.4 |
| WalletConnect | Reown WalletKit + android-core | 1.6.6 |
| Coroutines | kotlinx.coroutines | bundled with Kotlin |
| Lifecycle | ViewModel KTX | 2.10.0 |

---

## Build Configuration

All dependency versions are declared in a **single version catalog**:

```
gradle/libs.versions.toml
```

No version numbers appear in any `build.gradle.kts` file — every dependency
is referenced by its catalog alias (e.g. `libs.bundles.compose`,
`libs.walletkit`). This makes upgrades a one-line change with zero risk
of version mismatches across modules.

### SDK targets

| Property | Value |
|---|---|
| `compileSdk` | 36 |
| `minSdk` | 26 (Android 8.0) |
| `targetSdk` | 36 |
| `jvmTarget` | JVM 11 |

