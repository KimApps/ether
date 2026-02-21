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

@HiltViewModel
class SigningViewModel @Inject constructor(
    private val coordinator: SigningCoordinator,
    private val signUseCase: SignChallengeUseCase,
    walletConnectInitializer: WalletConnectInitializer,
    private val walletManager: WalletConnectManager,
) : ViewModel() {

    // state flow to emit state changes
    private val _state = MutableStateFlow(SigningState())

    // for external classes to subscribe to the flow
    val state: StateFlow<SigningState> = _state.asStateFlow()

    // channel to emit effects
    private val _effect = Channel<SigningEffect>()

    // for external classes to subscribe to the channel
    val effect = _effect.receiveAsFlow()

    init {
        // initialize wallet connect
        walletConnectInitializer.init()
        // Observe connection status to update UI
        viewModelScope.launch {
            walletManager.isConnected.collect { connected ->
                _state.update { it.copy(isWalletConnected = connected) }
            }
        }

        // Observe incoming signing requests from the Dapp/WalletConnect
        viewModelScope.launch {
            walletManager.sessionRequests.collect { request ->
                // state to show an "Approve/Reject" dialog
                _state.update { it.copy(pendingRequest = request) }
            }
        }
    }

    // handle user actions
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

    private fun onInit(challenge: String, type: OperationType) {
        // initial values for the state
        _state.update { it.copy(challenge = challenge, operationType = type) }
    }

    private fun onSign() {
        // prevent multiple clicks or signing without challenge
        if (_state.value.isLoading || _state.value.challenge.isEmpty()) return
        // challenge value for signing
        val challenge = _state.value.challenge
        // operation type for signing
        val type = _state.value.operationType
        // start signing process
        viewModelScope.launch {
            // show loading
            _state.update { it.copy(isLoading = true) }
            // sign the challenge
            val result =
                signUseCase(SigningRequest(challenge = challenge, operationType = type))
            // provide the result to the coordinator
            coordinator.provideResult(challenge, result)
            // hide loading and reset challenge
            _state.update { it.copy(isLoading = false, challenge = "") }
            // close the screen
            _effect.send(SigningEffect.Close)
        }
    }

    private fun onCancel() {
        // challenge value to cancel the signing process
        val challenge = _state.value.challenge
        // cancel the signing process
        viewModelScope.launch {
            // provide the Cancel result to the coordinator
            coordinator.provideResult(challenge, SigningResultEntity.Cancelled)
            // close the screen
            _effect.send(SigningEffect.Close)
        }
    }

    private fun onSignWithWallet() {
        // Show the pairing URI input so user can paste a WalletConnect URI
        _state.update { it.copy(showPairingInput = true, error = null) }
    }

    private fun onPairingUriChanged(uri: String) {
        _state.update { it.copy(pairingUri = uri) }
    }
    private fun onPair() {
        // Now use the state's URI to pair
        walletManager.pair(_state.value.pairingUri)
        _state.update { it.copy(showPairingInput = false) }
    }

    private fun onWalletSignRejected() {
        val request = _state.value.pendingRequest
        if (request != null) {
            walletManager.rejectRequest(request)
        }
        _state.update { it.copy(pendingRequest = null) }
    }
    private fun onWalletSignApproved() {
        // 1. Validation check
        if (_state.value.isLoading) return
        val request = _state.value.pendingRequest ?: return
        val challenge = _state.value.challenge

        viewModelScope.launch {
            // 2. Generate the mock signature
            val mockSignature = "0x-EOA-MOCK-SIG-${challenge.take(10)}"

            // 3. Respond to the external Dapp (WalletConnect)
            walletManager.approveRequest(request, mockSignature)

            // 4. Provide the result to your app's coordinator
            // Matches the 'onSign' logic: provideResult(challenge, result)
            coordinator.provideResult(
                challenge,
                SigningResultEntity.Signed(mockSignature)
            )

            // 5. Update state and clean up
            _state.update { it.copy(pendingRequest = null, challenge = "") }

            // 6. Close the screen using your existing _effect channel
            _effect.send(SigningEffect.Close)
        }
    }
}