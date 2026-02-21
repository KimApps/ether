package com.example.withdraw.layer.presentation.page.view_model

/**
 * One-time side effects produced by WithdrawViewModel and consumed by the UI layer.
 *
 * Effects are emitted through a [kotlinx.coroutines.channels.Channel] so each event is
 * delivered exactly once, even across recompositions. The UI collects them inside a
 * `LaunchedEffect(Unit)` block in `WithdrawPage`.
 */
sealed class WithdrawEffect {

    /**
     * Signals that the quotation was fetched successfully and the user must now sign
     * the transaction on the Signing screen.
     *
     * @param challenge The opaque challenge string returned by the quotation endpoint.
     *                  It uniquely identifies the pending transaction and is passed to
     *                  the Signing screen so the user can authorize it.
     * @param type The operation type string (e.g. `OperationType.WITHDRAWAL.name`) that
     *             the Signing screen displays and embeds in the signing request.
     */
    data class NavigateToSigning(val challenge: String, val type: String) : WithdrawEffect()

    /**
     * Emitted when the user taps "Withdraw" but the entered amount is zero or blank.
     * The UI reacts by showing a non-blocking snackbar — no navigation occurs and the
     * loading state is never entered.
     */
    object AmountNotValid : WithdrawEffect()

    /**
     * Emitted after the signed transaction has been submitted to the backend and the
     * backend confirmed success. The UI shows a success snackbar and resets the form.
     */
    object WithdrawSuccess : WithdrawEffect()

    /**
     * Emitted when the user cancels or dismisses the Signing screen without approving.
     * Corresponds to SigningResultEntity.Cancelled being returned by the coordinator.
     * The UI shows a "Signing cancelled" snackbar and the loading spinner is hidden.
     */
    object WithdrawCancelled : WithdrawEffect()
}

