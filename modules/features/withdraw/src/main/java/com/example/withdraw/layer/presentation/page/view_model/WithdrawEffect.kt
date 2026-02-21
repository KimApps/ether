package com.example.withdraw.layer.presentation.page.view_model

/**
 * One-shot side-effects emitted by [WithdrawViewModel] and consumed exactly once by the UI.
 *
 * Because these are delivered via a [kotlinx.coroutines.channels.Channel], each effect
 * is guaranteed to be handled by a single observer, making them suitable for actions
 * such as navigation or showing a snackbar that must not be replayed on re-composition.
 */
sealed class WithdrawEffect {

    /**
     * Instructs the UI to navigate to the Signing screen so the user can
     * authorise the withdrawal transaction.
     *
     * @param challenge The cryptographic challenge string returned by the quotation endpoint,
     *                  which the signing screen must sign.
     * @param type      The operation type label (e.g. "WITHDRAWAL") forwarded to the
     *                  signing screen for display and validation purposes.
     */
    data class NavigateToSigning(val challenge: String, val type: String) : WithdrawEffect()

    /** Emitted when the user taps "Withdraw" but the entered amount is zero or invalid. */
    object AmountNotValid : WithdrawEffect()

    /** Emitted after the backend confirms the withdrawal transaction was accepted. */
    object WithdrawSuccess : WithdrawEffect()

    /** Emitted when the user cancels the signing flow before the transaction is submitted. */
    object WithdrawCancelled : WithdrawEffect()
}

