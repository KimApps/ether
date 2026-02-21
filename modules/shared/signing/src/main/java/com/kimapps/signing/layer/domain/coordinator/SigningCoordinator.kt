package com.kimapps.signing.layer.domain.coordinator

import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
     * Read-only stream of signing requests.
     * Observed by the navigation layer or app-level ViewModel to know when
     * to push the signing screen onto the back stack.
     */
    val signingRequest = _signingRequest.asSharedFlow()

    /**
     * Tracks every in-flight signing request by its challenge string.
     * Each entry maps a challenge → its [CompletableDeferred], which is
     * completed by [provideResult] when the signing screen finishes.
     *
     * Accessed from both the feature coroutine and the signing ViewModel,
     * potentially on different threads — all reads and writes are wrapped
     * in [synchronized] to prevent race conditions.
     */
    private val activeRequests = mutableMapOf<String, CompletableDeferred<SigningResultEntity>>()

    /**
     * Called by feature modules (e.g. WithdrawViewModel) to request a signature.
     *
     * **Suspends** the caller's coroutine until [provideResult] is called with
     * the same rq.challenge, then returns the result and cleans up.
     * The caller therefore never needs to poll or register a callback —
     * it simply awaits the result like a regular function return.
     *
     * Thread-safety: the [activeRequests] map is mutated under [synchronized]
     * both here and in [provideResult] to guard against concurrent access.
     *
     * @param rq The signing request containing the challenge string and operation type.
     * @return   The [SigningResultEntity] produced by the signing screen:
     *           [SigningResultEntity.Signed], [SigningResultEntity.Cancelled], or
     *           [SigningResultEntity.Error].
     */
    suspend fun requestSignature(rq: SigningRequest): SigningResultEntity {
        // Create a new deferred for this specific challenge.
        // CompletableDeferred acts as a one-shot "promise" that can be
        // completed exactly once from any thread.
        val deferred = CompletableDeferred<SigningResultEntity>()

        // Register the deferred under the challenge key so provideResult
        // can look it up and complete it when the signing screen is done
        synchronized(activeRequests) {
            activeRequests[rq.challenge] = deferred
        }

        // Notify the navigation layer to open the signing screen.
        // tryEmit is safe here because extraBufferCapacity = 1 guarantees
        // the emission is buffered even without an active collector
        _signingRequest.tryEmit(rq)

        // Suspend this coroutine until SigningViewModel calls provideResult.
        // The finally block runs whether the deferred completes normally
        // or the coroutine is cancelled (e.g. user leaves the feature screen).
        return try {
            deferred.await()
        } finally {
            // Always remove the entry to prevent a memory leak, regardless
            // of whether the result was a success, cancellation, or error
            synchronized(activeRequests) {
                activeRequests.remove(rq.challenge)
            }
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
        synchronized(activeRequests) {
            // complete() transitions the deferred from "pending" to "done",
            // waking the coroutine suspended in deferred.await() above
            activeRequests[challenge]?.complete(result)
        }
    }
}