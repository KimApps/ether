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
        _state.update { it.copy(amount = amount) }
    }

    private fun onWithdraw() {
        viewModelScope.launch {
            val amount = _state.value.amount.toDoubleOrNull() ?: 0.0
            if (amount == 0.0) {
                _effect.send(WithdrawEffect.AmountNotValid)
            } else {
                _state.update { it.copy(isLoading = true, error = null) }
                val quotation = getQuotationUseCase(GetQuotationRequest(amount = amount))
                val signingResult = signingCoordinator.requestSignature(
                    SigningRequest(
                        challenge = quotation.challenge,
                        operationType = OperationType.WITHDRAWAL
                    )
                )
                when (signingResult) {
                    is SigningResultEntity.Signed -> {
                        val withdrawResult = submitWithdrawUseCase(
                            SubmitWithdrawRequest(
                                id = quotation.id,
                                signature = signingResult.signature
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
                        } else {
                            val errorMsg = "Transaction submission failed. Please try again."
                            _state.update { it.copy(isLoading = false, error = errorMsg) }
                        }
                    }

                    is SigningResultEntity.Error -> {
                        _state.update { it.copy(isLoading = false, error = signingResult.message) }
                    }

                    is SigningResultEntity.Cancelled -> {
                        _state.update { it.copy(isLoading = false, error = "Signing cancelled") }
                    }
                }
            }

        }
    }
}