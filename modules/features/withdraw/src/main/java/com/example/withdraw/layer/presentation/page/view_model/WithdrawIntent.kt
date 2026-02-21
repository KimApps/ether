package com.example.withdraw.layer.presentation.page.view_model

/**
 * Represents all possible user actions on the Withdraw screen.
 *
 * Each subclass maps to a discrete UI event and is forwarded to
 * [WithdrawViewModel.onIntent] as the single entry-point for state changes.
 */
sealed class WithdrawIntent {

    /**
     * Fired every time the user edits the amount text field.
     *
     * @param amount The raw string value currently in the input field.
     *               The ViewModel is responsible for sanitising this value
     *               before using it in any business logic.
     */
    data class OnAmountChanged(val amount: String) : WithdrawIntent()

    /** Fired when the user taps the "Withdraw" button to initiate the transaction flow. */
    object OnWithdrawClicked : WithdrawIntent()
}
