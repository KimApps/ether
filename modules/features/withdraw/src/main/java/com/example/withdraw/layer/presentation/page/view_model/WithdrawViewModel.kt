package com.example.withdraw.layer.presentation.page.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.withdraw.layer.domain.request_models.GetQuotationRequest
import com.example.withdraw.layer.domain.request_models.SubmitWithdrawRequest
import com.example.withdraw.layer.domain.use_cases.GetQuotationUseCase
import com.example.withdraw.layer.domain.use_cases.SubmitWithdrawUseCase
import com.kimapps.signing.layer.domain.coordinator.SigningCoordinator
import com.kimapps.signing.layer.domain.entity_models.SigningResultEntity
import com.kimapps.signing.layer.domain.enums.OperationType
import com.kimapps.signing.layer.domain.request_models.SigningRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val signingCoordinator: SigningCoordinator,
    private val getQuotationUseCase: GetQuotationUseCase,
    private val submitWithdrawUseCase: SubmitWithdrawUseCase
) :
    ViewModel() {
    private val _state = MutableStateFlow(WithdrawState())
    val state = _state.asStateFlow()

    private val _effect = Channel<WithdrawEffect>()
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: WithdrawIntent) {
        when (intent) {
            is WithdrawIntent.OnAmountChanged -> onAmountChanged(intent.amount)
            is WithdrawIntent.OnWithdrawClicked -> onWithdraw()
        }
    }

    private fun onAmountChanged(amount: String) {
        // Filter out non-digit characters
        val filteredAmount = amount.trim().filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amount = filteredAmount) }
    }

    private fun onWithdraw() {
        viewModelScope.launch {
            val amount = _state.value.amount.toDoubleOrNull() ?: 0.0
            if (amount == 0.0) {
                _effect.send(WithdrawEffect.AmountNotValid)
                return@launch
            }
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Get the quotation from the backend
                val quotation = getQuotationUseCase(GetQuotationRequest(amount = amount))
                // Navigate to Signing page
                _effect.send(
                    WithdrawEffect.NavigateToSigning(
                        quotation.challenge,
                        OperationType.WITHDRAWAL.name
                    )
                )
                // This is the "Coordinator" call. It will suspend here
                // until the user interacts with the SigningPage.
                val signingResult = signingCoordinator.requestSignature(
                    SigningRequest(
                        challenge = quotation.challenge,
                        operationType = OperationType.WITHDRAWAL
                    )
                )
                // Process the result
                handleSigningResult(signingResult, quotation.id)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun handleSigningResult(result: SigningResultEntity, id: String) {
        when (result) {
            is SigningResultEntity.Signed -> {
                val withdrawResult = submitWithdrawUseCase(
                    SubmitWithdrawRequest(
                        id = id,
                        signature = result.signature
                    )
                )
                if (withdrawResult) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            amount = "",
                            error = null
                        )
                    }
                    // Send success effect to notify user of successful withdrawal
                    _effect.send(WithdrawEffect.WithdrawSuccess)
                } else {
                    val errorMsg = "Transaction submission failed. Please try again."
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                }
            }

            is SigningResultEntity.Error -> {
                _state.update { it.copy(isLoading = false, error = result.message) }
            }

            is SigningResultEntity.Cancelled -> {
                _effect.send(WithdrawEffect.WithdrawCancelled)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}