package com.example.withdraw.layer.presentation.page.view_model

/**
 * Immutable snapshot of everything the WithdrawPage UI needs to render itself.
 *
 * The ViewModel exposes this as a `StateFlow` so the composable always receives the
 * latest value and recomposes only the parts that actually changed.
 *
 * @param amount The raw text currently typed in the amount input field. The ViewModel
 *               strips non-numeric characters before storing it here, so this string
 *               is always parseable as a decimal number or empty.
 * @param isLoading `true` while the network calls (get quotation + submit withdrawal)
 *                  are in flight. The UI disables the input field and the action button,
 *                  and replaces the button label with a progress indicator.
 * @param error A human-readable error message to display below the action button, or
 *              `null` when there is no error. Cleared at the start of each new attempt.
 * @param isSuccess `true` after a withdrawal has been successfully submitted. Can be
 *                  used to trigger success animations or auto-navigation if needed.
 */
data class WithdrawState(
    val amount: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)