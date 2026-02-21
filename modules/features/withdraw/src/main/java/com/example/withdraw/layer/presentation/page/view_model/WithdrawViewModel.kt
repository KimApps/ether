package com.example.withdraw.layer.presentation.page.view_model

import android.util.Log
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

/**
 * ViewModel for the Withdraw screen, responsible for orchestrating the full withdrawal
 * flow: amount validation → quotation fetch → Passkey / WalletConnect signing → submission.
 *
 * ## Architecture
 * Follows the MVI pattern with three public surfaces:
 * - [state] – a `StateFlow` of [WithdrawState] that the UI collects for rendering.
 * - [effect] – a `Flow` backed by a [Channel] for one-time events (navigation, snackbars).
 * - [onIntent] – the single entry point for all user actions ([WithdrawIntent]).
 *
 * ## Signing flow
 * The withdrawal requires a cryptographic signature before submission. The flow is:
 * 1. Fetch a transaction quotation from the backend (contains a `challenge` and a
 *    transaction `id`).
 * 2. Emit `NavigateToSigning` so the UI opens the Signing screen with the challenge.
 * 3. **Suspend** on [SigningCoordinator.requestSignature] until the user either approves
 *    or dismisses the Signing screen.
 * 4. Handle the result: submit the withdrawal on `Signed`, show an error on `Error`, or
 *    show a cancellation snackbar on `Cancelled`.
 *
 * The coroutine launched in [onWithdraw] is bound to [viewModelScope] so it is
 * automatically cancelled if the ViewModel is cleared (e.g. the user navigates away
 * before signing).
 *
 * @param signingCoordinator Shared singleton that suspends the coroutine until signing
 *                           is complete. Injected so it can be shared with the Signing
 *                           ViewModel without leaking lifecycle references.
 * @param getQuotationUseCase Fetches a transaction quotation (challenge + id) from the
 *                            backend for the given withdrawal amount.
 * @param submitWithdrawUseCase Submits the signed transaction to the backend using the
 *                              quotation id and the Passkey/WalletConnect signature.
 */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val signingCoordinator: SigningCoordinator,
    private val getQuotationUseCase: GetQuotationUseCase,
    private val submitWithdrawUseCase: SubmitWithdrawUseCase
) :
    ViewModel() {

    // Backing mutable state, only writable inside this ViewModel.
    private val _state = MutableStateFlow(WithdrawState())

    /** Read-only state consumed by the UI. */
    val state = _state.asStateFlow()

    // Channel capacity defaults to RENDEZVOUS (0), meaning effects are suspended until
    // the UI collects them. This prevents effects from being lost on rapid emissions.
    private val _effect = Channel<WithdrawEffect>()

    /** One-time event stream consumed by the UI via `LaunchedEffect(Unit)`. */
    val effect = _effect.receiveAsFlow()

    /**
     * Single entry point for all UI-driven actions.
     *
     * Dispatches each [WithdrawIntent] to the appropriate private handler. Keeping all
     * intent routing here makes the ViewModel easy to unit-test without touching the UI.
     *
     * @param intent The action triggered by the user on the Withdraw screen.
     */
    fun onIntent(intent: WithdrawIntent) {
        when (intent) {
            is WithdrawIntent.OnAmountChanged -> onAmountChanged(intent.amount)
            is WithdrawIntent.OnWithdrawClicked -> onWithdraw()
        }
    }

    /**
     * Sanitizes and stores the new amount value whenever the user types in the field.
     *
     * Non-digit, non-decimal characters (e.g. spaces, letters) are stripped so the
     * stored value is always parseable as a `Double` or is empty.
     *
     * @param amount The raw string from the text field before filtering.
     */
    private fun onAmountChanged(amount: String) {
        // Filter out non-digit characters
        val filteredAmount = amount.trim().filter { it.isDigit() || it == '.' }
        _state.update { it.copy(amount = filteredAmount) }
    }

    /**
     * Orchestrates the full withdrawal flow when the user taps "Withdraw".
     *
     * Steps:
     * 1. Parse the current amount; emit [WithdrawEffect.AmountNotValid] and bail out early
     *    if the value is zero or unparseable.
     * 2. Enter the loading state and clear any previous error.
     * 3. Call [GetQuotationUseCase] to obtain a backend quotation containing a `challenge`
     *    and a transaction `id`.
     * 4. Emit [WithdrawEffect.NavigateToSigning] so the UI navigates to the Signing screen.
     * 5. **Suspend** on [SigningCoordinator.requestSignature] — this coroutine blocks here
     *    until the Signing ViewModel delivers a result via the coordinator.
     * 6. Delegate the result to [handleSigningResult].
     *
     * Any uncaught exception from the quotation fetch or the coordinator is caught and
     * stored as an error message in state, which the UI displays inline.
     */
    private fun onWithdraw() {
        viewModelScope.launch {
            val amount = _state.value.amount.toDoubleOrNull() ?: 0.0
            if (amount == 0.0) {
                // Amount is blank or zero — show a validation snackbar without entering
                // the loading state so the user can correct the input immediately.
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
                // Covers network failures from the quotation fetch and any unexpected
                // error from the coordinator. The loading spinner is cleared and the
                // error message is shown inline beneath the action button.
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Processes the outcome of the signing step and updates state or emits effects.
     *
     * This function is `suspend` so it can call `_effect.send()` directly without
     * launching a nested coroutine.
     *
     * @param result The signing outcome delivered by the coordinator once the user
     *               interacts with the Signing screen.
     * @param id     The quotation id returned by the backend; required by the submit
     *               endpoint to match the signature to the pending transaction.
     */
    private suspend fun handleSigningResult(result: SigningResultEntity, id: String) {
        when (result) {
            is SigningResultEntity.Signed -> {
                // Signing succeeded — attempt to submit the withdrawal to the backend.
                try {
                    val withdrawResult = submitWithdrawUseCase(
                        SubmitWithdrawRequest(
                            id = id,
                            signature = result.signature
                        )
                    )
                    if (withdrawResult) {
                        // Backend confirmed the transaction: reset the form and notify the UI.
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
                        // Backend returned a failure response (non-exception path).
                        val errorMsg = "Transaction submission failed. Please try again."
                        _state.update { it.copy(isLoading = false, error = errorMsg) }
                    }
                } catch (e: Exception) {
                    // Network or server error during submission. The user can retry by
                    // tapping "Withdraw" again; signing is required once more because the
                    // challenge is single-use.
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }

            is SigningResultEntity.Error -> {
                // The signing flow itself encountered an error (e.g. Passkey assertion
                // failed). Display the error message returned by the coordinator.
                _state.update { it.copy(isLoading = false, error = result.message) }
            }

            is SigningResultEntity.Cancelled -> {
                // The user dismissed the Signing screen without approving. Show a
                // non-blocking snackbar and clear the loading state so the user can
                // adjust the amount or retry.
                _effect.send(WithdrawEffect.WithdrawCancelled)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}