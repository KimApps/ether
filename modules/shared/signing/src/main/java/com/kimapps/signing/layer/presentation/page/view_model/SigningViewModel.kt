package com.kimapps.signing.layer.presentation.page.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimapps.signing.layer.domain.coordinator.SigningCoordinator
import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.enums.OperationType
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import com.kimapps.signing.layer.domain.use_cases.SignChallengeUseCase
import com.kimapps.signing.wallet_connect.WalletConnectInitializer
import com.kimapps.signing.wallet_connect.WalletConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SigningViewModel - Owns all business logic and state for the signing screen.
 *
 * Follows the MVI pattern:
 * - [state]  – a single [StateFlow] the UI observes and renders from.
 * - [effect] – a [Channel]-backed flow for one-time navigation/UI events.
 * - [onIntent] – the single entry-point for all user actions.
 *
 * Responsibilities:
 * 1. Initialise WalletConnect on first creation and keep the UI in sync
 *    with the connection state and incoming session requests.
 * 2. Sign the challenge either via Passkey ([signUseCase]) or via an
 *    approved WalletConnect session request.
 * 3. Deliver every signing outcome to [SigningCoordinator] so the feature
 *    module that requested the signature can resume its own flow.
 *
 * @param coordinator          Suspends the caller until a signing result is ready.
 * @param signUseCase          Executes the Passkey signing flow.
 * @param walletConnectInitializer Bootstraps CoreClient and WalletKit once per process.
 * @param walletManager        Wraps the WalletKit delegate and exposes reactive flows.
 */
@HiltViewModel
class SigningViewModel @Inject constructor(
    private val coordinator: SigningCoordinator,
    private val signUseCase: SignChallengeUseCase,
    walletConnectInitializer: WalletConnectInitializer,
    private val walletManager: WalletConnectManager,
) : ViewModel() {

    // Backing MutableStateFlow — private so only this ViewModel can mutate state
    private val _state = MutableStateFlow(SigningState())

    // Read-only StateFlow exposed to the UI; every emission triggers a recomposition
    val state: StateFlow<SigningState> = _state.asStateFlow()

    // Backing Channel with default (RENDEZVOUS) capacity — each effect is
    // delivered exactly once even if the collector is temporarily inactive
    private val _effect = Channel<SigningEffect>()

    // Exposed as a Flow so the UI collects it inside a LaunchedEffect
    val effect = _effect.receiveAsFlow()

    init {
        // Bootstrap WalletConnect (CoreClient + WalletKit) the first time this
        // ViewModel is created. The initialiser is idempotent — calling it again
        // while already initialised is a no-op.
        walletConnectInitializer.init()

        // Mirror the WalletConnect connection state into the UI state so the
        // WalletConnectSection automatically switches between the pairing input
        // and the "Wallet Connected" confirmation card.
        viewModelScope.launch {
            runCatching {
                walletManager.isConnected.collect { connected ->
                    _state.update {
                        it.copy(
                            isWalletConnected = connected,
                            isAwaitingApprovalFromDapp = false
                        )
                    }
                }
            }
        }

        // Collect incoming session requests emitted by WalletConnectManager
        // whenever the paired dApp sends a personal_sign (or similar) call.
        // Storing the request in state causes the SigningApprovalDialog to appear.
        viewModelScope.launch {
            walletManager.sessionRequests.collect { request ->
                _state.update { it.copy(pendingRequest = request) }
            }
        }
    }

    /**
     * Single entry-point for all UI events.
     * Delegates each [SigningIntent] subtype to a private handler function,
     * keeping this function as a clean dispatch table with no logic of its own.
     */
    fun onIntent(intent: SigningIntent) {
        when (intent) {
            is SigningIntent.OnInit -> onInit(intent.challenge, intent.type)
            is SigningIntent.OnSignClicked -> onSign()
            is SigningIntent.OnCancelClicked -> onCancel()
            is SigningIntent.OnSignWithWalletClicked -> onSignWithWallet()
            is SigningIntent.OnPairingUriChanged -> onPairingUriChanged(intent.uri)
            is SigningIntent.OnPairClicked -> onPair()
            is SigningIntent.RejectWalletSign -> onWalletSignRejected()
            is SigningIntent.ApproveWalletSign -> onWalletSignApproved()
        }
    }

    // ─────────────────────────────────────────────
    // Private intent handlers
    // ─────────────────────────────────────────────

    /** Seeds the state with the challenge and operation type from the navigation route. */
    private fun onInit(challenge: String, type: OperationType) {
        _state.update { it.copy(challenge = challenge, operationType = type) }
    }

    /**
     * Executes the Passkey signing flow.
     * Guards against duplicate invocations (isLoading check) and an empty
     * challenge, then delegates to [SignChallengeUseCase] inside a coroutine.
     *
     * On success: delivers [SigningResultEntity.Signed] to the coordinator and closes the screen.
     * On failure: surfaces the error message in [SigningState.error], delivers
     * [SigningResultEntity.Error] to the coordinator so the upstream flow is
     * not left suspended indefinitely, and keeps the screen open for a retry.
     */
    private fun onSign() {
        // Guard: ignore taps while already signing or if challenge is not yet loaded
        if (_state.value.isLoading || _state.value.challenge.isEmpty()) return

        val challenge = _state.value.challenge
        val type = _state.value.operationType

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Suspend until the Passkey credential manager resolves the signature
                val result =
                    signUseCase(SigningRequest(challenge = challenge, operationType = type))

                // Hand the result to the coordinator so the upstream feature flow can resume
                coordinator.provideResult(challenge, result)

                // Reset loading + challenge, then close the screen
                _state.update { it.copy(isLoading = false, challenge = "") }
                _effect.send(SigningEffect.Close)
            } catch (e: Exception) {
                // Surface the error to the UI so the user can read it and retry
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Signing failed: ${e.localizedMessage}"
                    )
                }

                // We notify the coordinator of the error so the calling flow
                // (e.g. WithdrawViewModel) is not left suspended indefinitely
                coordinator.provideResult(
                    challenge,
                    SigningResultEntity.Error(e.message ?: "Unknown Error")
                )
            }

        }
    }

    /**
     * Handles a user-initiated cancellation.
     * Delivers [SigningResultEntity.Cancelled] to the coordinator so the caller
     * knows the user opted out, then closes the screen.
     */
    private fun onCancel() {
        val challenge = _state.value.challenge
        viewModelScope.launch {
            coordinator.provideResult(challenge, SigningResultEntity.Cancelled)
            _effect.send(SigningEffect.Close)
        }
    }

    /**
     * Reveals the WalletConnect URI input field and clears any previous error,
     * transitioning the UI from the default button to the pairing-input state.
     */
    private fun onSignWithWallet() {
        _state.update { it.copy(showPairingInput = true, error = null) }
    }

    /** Keeps [SigningState.pairingUri] in sync with every keystroke in the URI field. */
    private fun onPairingUriChanged(uri: String) {
        _state.update { it.copy(pairingUri = uri) }
    }

    /**
     * Initiates the WalletConnect pairing handshake using the URI the user pasted.
     * Hides the input field immediately so the UI transitions to a waiting state
     * while WalletKit negotiates the session in the background.
     */
    private fun onPair() {
        walletManager.pair(_state.value.pairingUri)
        // Hide the input field — connection state is updated via isConnected flow
        _state.update {
            it.copy(
                showPairingInput = false,
                isAwaitingApprovalFromDapp = true
            )
        }
    }

    /**
     * Rejects the pending WalletConnect session request.
     * Sends a JSON-RPC error back to the dApp via WalletKit so it knows
     * the request was explicitly declined, then clears the dialog.
     */
    private fun onWalletSignRejected() {
        val request = _state.value.pendingRequest
        if (request != null) {
            // Notify the dApp — omitting this would leave it waiting indefinitely
            walletManager.rejectRequest(request)
        }
        _state.update { it.copy(pendingRequest = null) }
    }

    /**
     * Approves the pending WalletConnect session request end-to-end:
     * 1. Guards against re-entry while a previous approval is in flight.
     * 2. Builds a mock EOA signature from the challenge prefix.
     * 3. Sends the signature back to the dApp via WalletKit (JSON-RPC result).
     * 4. Delivers a [SigningResultEntity.Signed] to the coordinator so the
     *    upstream withdraw flow can resume with the signature.
     * 5. Cleans up state and closes the screen.
     */
    private fun onWalletSignApproved() {
        // Guard: prevent a second approval while the first is still processing
        if (_state.value.isLoading) return
        val request = _state.value.pendingRequest ?: return
        val challenge = _state.value.challenge

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Mock signature — in production this would be a real ECDSA signature
                // produced by the connected EOA wallet's private key
                val mockSignature = "0x-EOA-MOCK-SIG-${challenge.take(10)}"

                // Respond to the dApp so it can proceed with its own transaction flow
                walletManager.approveRequest(request, mockSignature)

                // Deliver the result to SigningCoordinator to unblock the withdraw flow
                coordinator.provideResult(challenge, SigningResultEntity.Signed(mockSignature))

                // Clear the dialog and challenge, then emit Close to pop this screen
                _state.update { it.copy(pendingRequest = null, challenge = "") }
                _effect.send(SigningEffect.Close)
            } catch (e: Exception) {
                coordinator.provideResult(
                    challenge,
                    SigningResultEntity.Error(e.message ?: "Unknown Error")
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Wallet signing failed: ${e.localizedMessage}"
                    )
                }
            }

        }
    }

    override fun onCleared() {
        super.onCleared()
        // close channel to prevent resource leaks
        _effect.close()
    }
}