package com.kimapps.signing.layer.presentation.page.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimapps.signing.layer.domain.coordinator.SigningCoordinator
import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.enums.OperationType
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import com.kimapps.signing.layer.domain.use_cases.SignChallengeUseCase
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
    private val signUseCase: SignChallengeUseCase
) : ViewModel() {
    // state flow to emit state changes
    private val _state = MutableStateFlow(SigningState())

    // for external classes to subscribe to the flow
    val state: StateFlow<SigningState> = _state.asStateFlow()

    // channel to emit effects
    private val _effect = Channel<SigningEffect>()

    // for external classes to subscribe to the channel
    val effect = _effect.receiveAsFlow()

    // handle user actions
    fun onIntent(intent: SigningIntent) {
        when (intent) {
            is SigningIntent.OnInit -> onInit(intent.challenge, intent.type)
            SigningIntent.OnSignClicked -> onSign()
            SigningIntent.OnCancelClicked -> onCancel()
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
}