package com.kimapps.signing.layer.domain.coordinator

import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SigningCoordinator - Cross-module bridge between the feature layer and the
 * signing screen.
 *
 * Solves the problem of two independent modules needing to communicate without
 * a direct dependency on each other:
 * - A **feature module** (e.g. withdraw) calls [requestSignature] and suspends,
 *   waiting for the user to complete or cancel the signing flow.
 * - The **signing screen** (SigningViewModel) calls [provideResult] when the
 *   user finishes, which unblocks the suspended feature coroutine.
 *
 * The coordination mechanism uses [CompletableDeferred] — one per in-flight
 * request — keyed by the challenge string. This allows multiple concurrent
 * signing requests to be tracked independently without them interfering.
 *
 * Marked [@Singleton] so the same instance is shared between the feature
 * ViewModel and the SigningViewModel, which live in different scopes.
 */
@Singleton
class SigningCoordinator @Inject constructor() {

    /**
     * Hot shared flow that carries signing requests to any subscriber.
     * extraBufferCapacity = 1 ensures tryEmit never drops an emission
     * even if there is no active collector at the exact moment of emission
     * (e.g. during a brief navigation transition).
     */
    private val _signingRequest = MutableSharedFlow<SigningRequest>(extraBufferCapacity = 1)

    /**
     * Tracks every in-flight signing request by its challenge string.
     * Each entry maps a challenge → its [CompletableDeferred], which is
     * completed by [provideResult] when the signing screen finishes.
     *
     * Accessed from both the feature coroutine and the signing ViewModel,
     * potentially on different threads. [ConcurrentHashMap] provides
     * lock-free thread-safety for individual read/write operations without
     * requiring explicit synchronization.
     */
    private val activeRequests =
        ConcurrentHashMap<String, CompletableDeferred<SigningResultEntity>>()

    /**
     * Called by feature modules (e.g. WithdrawViewModel) to request a signature.
     *
     * **Suspends** the caller's coroutine until [provideResult] is called with
     * the same rq.challenge, then returns the result and cleans up.
     * The caller therefore never needs to poll or register a callback —
     * it simply awaits the result like a regular function return.
     *
     * Thread-safety: [activeRequests] is a [ConcurrentHashMap]. Individual map
     * operations ([remove], index reads) are thread-safe, but [getOrPut] is not
     * a single atomic check-then-insert — the lambda can execute on multiple
     * threads simultaneously for the same key. This is acceptable here because
     * challenge strings are unique per request, guaranteeing a single caller.
     *
     * @param rq The signing request containing the challenge string and operation type.
     * @return   The [SigningResultEntity] produced by the signing screen:
     *           [SigningResultEntity.Signed], [SigningResultEntity.Cancelled], or
     *           [SigningResultEntity.Error].
     */
    suspend fun requestSignature(rq: SigningRequest): SigningResultEntity {
        // getOrPut returns the existing deferred if one is already mapped to this
        // challenge, or atomically inserts and returns a new CompletableDeferred.
        // Note: ConcurrentHashMap.getOrPut is NOT a single atomic operation —
        // the lambda may be invoked even if another thread wins the race and
        // inserts first. In practice this is safe here because each challenge
        // string is unique per request, so only one coroutine will ever call
        // this for the same key at a time.
        val deferred =
            activeRequests.getOrPut(rq.challenge) { CompletableDeferred() }

        // emit() suspends if the buffer is full; extraBufferCapacity = 1
        // ensures it returns immediately as long as no previous emission
        // is still pending, which is the expected case for unique challenges.
        _signingRequest.emit(rq)

        // Suspend this coroutine until SigningViewModel calls provideResult.
        // The finally block runs whether the deferred completes normally
        // or the coroutine is cancelled (e.g. user leaves the feature screen).
        return try {
            deferred.await()
        } finally {
            // Always clean up the entry regardless of how the coroutine ended —
            // normal completion, cancellation (e.g. back press), or an exception.
            // Leaving a stale deferred in the map would leak memory and could
            // cause a future request with the same challenge to resume the wrong caller.
            activeRequests.remove(rq.challenge)
        }
    }

    /**
     * Called by SigningViewModel when the signing process has finished.
     *
     * Looks up the [CompletableDeferred] for the given [challenge] and
     * completes it with [result], which unblocks the coroutine suspended
     * in [requestSignature] and delivers the result to the feature module.
     *
     * If no deferred exists for the challenge (e.g. the feature screen was
     * already dismissed), the call is silently ignored.
     *
     * @param challenge The challenge string that identifies which request to complete.
     * @param result    The outcome of the signing flow to deliver to the caller.
     */
    fun provideResult(challenge: String, result: SigningResultEntity) {
        // complete() transitions the deferred from "pending" to "done",
        // waking the coroutine suspended in deferred.await() above.
        // ConcurrentHashMap guarantees a safe read here without additional locking.
        activeRequests[challenge]?.complete(result)
    }
}