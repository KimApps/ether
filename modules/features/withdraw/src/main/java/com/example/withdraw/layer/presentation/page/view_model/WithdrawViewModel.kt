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
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for the Withdraw screen.
 *
 * Follows a unidirectional data-flow (UDF) pattern:
 * - UI sends [WithdrawIntent]s via [onIntent].
 * - Business logic updates [state] (a [MutableStateFlow]) that the UI observes.
 * - One-off events (navigation, toasts, etc.) are emitted through [effect] (a [Channel]).
 *
 * The withdraw flow involves two async steps:
 * 1. Fetch a price quotation from the backend.
 * 2. Collect a cryptographic signature from the user via [SigningCoordinator],
 *    then submit the signed transaction.
 *
 * @param signingCoordinator Suspending coordinator that drives the signing sub-flow.
 * @param getQuotationUseCase Fetches a withdrawal quotation (challenge + id) from the backend.
 * @param submitWithdrawUseCase Submits the signed withdrawal transaction to the backend.
 */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val signingCoordinator: SigningCoordinator,
    private val getQuotationUseCase: GetQuotationUseCase,
    private val submitWithdrawUseCase: SubmitWithdrawUseCase
) : ViewModel() {

    /** Backing state exposed as a read-only StateFlow to the UI. */
    private val _state = MutableStateFlow(WithdrawState())
    val state = _state.asStateFlow()

    /**
     * One-shot side-effects (e.g. navigation, snackbars) delivered via a [Channel]
     * so each event is consumed exactly once.
     */
    private val _effect = Channel<WithdrawEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /**
     * Single entry-point for all UI-driven actions.
     * Dispatches each [WithdrawIntent] to its dedicated handler.
     *
     * @param intent The user action to process.
     */
    fun onIntent(intent: WithdrawIntent) {
        when (intent) {
            is WithdrawIntent.OnAmountChanged -> onAmountChanged(intent.amount)
            is WithdrawIntent.OnWithdrawClicked -> onWithdraw()
        }
    }

    /**
     * Sanitises and stores the user-entered amount.
     *
     * Strips leading/trailing whitespace and removes any characters that are
     * neither digits nor a decimal point, preventing invalid numeric input
     * from reaching the backend.
     *
     * @param amount The raw string value from the text field.
     */
    private fun onAmountChanged(amount: String) {
        // Filter out non-digit characters, keeping only digits and '.'
        val filteredAmount = amount.trim().filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amount = filteredAmount) }
    }

    /**
     * Orchestrates the full withdraw flow:
     * 1. Validates that the entered amount is non-zero.
     * 2. Fetches a quotation (challenge + quotation id) from the backend.
     * 3. Navigates the user to the Signing screen.
     * 4. Suspends until the user completes or cancels signing via [SigningCoordinator].
     * 5. Delegates the signing result to [handleSigningResult].
     *
     * All network/signing errors are caught and surfaced through [WithdrawState.error].
     */
    private fun onWithdraw() {
        // Guard: prevent multiple clicks
        if (_state.value.isLoading) return

        viewModelScope.launch {
            val amount = _state.value.amount.toDoubleOrNull() ?: 0.0

            // Guard: reject a zero or unparseable amount before hitting the network
            if (amount <= 0.0) {
                _effect.send(WithdrawEffect.AmountNotValid)
                return@launch
            }

            // Show loading indicator and clear any previous error
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Step 1: Fetch the withdrawal quotation from the backend
                val quotation = getQuotationUseCase(GetQuotationRequest(amount = amount))

                // Step 2: Navigate to the Signing screen so the user can authorise the transaction
                _effect.send(
                    WithdrawEffect.NavigateToSigning(
                        quotation.challenge,
                        OperationType.WITHDRAWAL.name
                    )
                )

                // Reset loading here so if the user navigates BACK from signing,
                // the button is enabled again.
                _state.update { it.copy(isLoading = false) }

                // Step 3: Suspend here until the user finishes interacting with the Signing screen.
                // SigningCoordinator acts as a bridge between this coroutine and the signing sub-flow.
                val signingResult = signingCoordinator.requestSignature(
                    SigningRequest(
                        challenge = quotation.challenge,
                        operationType = OperationType.WITHDRAWAL
                    )
                )

                // Step 4: Handle whatever outcome the signing screen produced
                handleSigningResult(signingResult, quotation.id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Surface any unexpected error (network, serialisation, etc.) to the UI
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Processes the outcome returned by [SigningCoordinator] after the user
     * interacts with the Signing screen.
     *
     * - [SigningResultEntity.Signed]   → Submit the transaction with the collected signature.
     * - [SigningResultEntity.Error]    → Surface the error message to the UI.
     * - [SigningResultEntity.Cancelled] → Notify the UI that the flow was cancelled and reset loading.
     *
     * @param result The signing outcome from the Signing screen.
     * @param id     The quotation id required when submitting the signed transaction.
     */
    private suspend fun handleSigningResult(result: SigningResultEntity, id: String) {
        when (result) {
            is SigningResultEntity.Signed -> {
                try {
                    // loading to TRUE again because we are now hitting
                    // the network to SUBMIT the transaction.
                    _state.update { it.copy(isLoading = true) }
                    // Submit the transaction using the signature provided by the user
                    val withdrawResult = submitWithdrawUseCase(
                        SubmitWithdrawRequest(
                            id = id,
                            signature = result.signature
                        )
                    )

                    if (withdrawResult) {
                        // Transaction accepted — reset the form and signal success to the UI
                        _state.update {
                            it.copy(
                                isLoading = false,
                                amount = "",   // Clear the amount field for the next transaction
                                error = null
                            )
                        }
                        _effect.send(WithdrawEffect.WithdrawSuccess)
                    } else {
                        // Backend rejected the transaction without throwing an exception
                        val errorMsg = "Transaction submission failed. Please try again."
                        _state.update { it.copy(isLoading = false, error = errorMsg) }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Network or server error during submission
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }

            is SigningResultEntity.Error -> {
                // The signing screen encountered an error; propagate the message to the UI
                _state.update { it.copy(isLoading = false, error = result.message) }
            }

            is SigningResultEntity.Cancelled -> {
                // User cancelled the signing flow; dismiss loading without showing an error
                _effect.send(WithdrawEffect.WithdrawCancelled)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // close channel to avoid leaks
        _effect.close()
    }
}