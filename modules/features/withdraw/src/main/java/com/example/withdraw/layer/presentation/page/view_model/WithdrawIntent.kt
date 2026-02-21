package com.example.withdraw.layer.presentation.page.view_model

/**
 * User-driven actions that the WithdrawPage can dispatch to WithdrawViewModel.
 *
 * Following the MVI (Model-View-Intent) pattern, all UI interactions are funnelled
 * through a single `onIntent(intent)` entry point on the ViewModel, keeping the
 * presentation layer unidirectional and testable.
 */
sealed class WithdrawIntent {

    /**
     * Dispatched on every keystroke in the amount text field.
     *
     * @param amount The raw string value from the text field. The ViewModel filters
     *               this to digits and decimal points before updating the state.
     */
    data class OnAmountChanged(val amount: String) : WithdrawIntent()

    /**
     * Dispatched when the user taps the primary "Withdraw" button.
     * The ViewModel validates the amount and, if valid, begins the quotation and
     * signing flow.
     */
    object OnWithdrawClicked : WithdrawIntent()
}
