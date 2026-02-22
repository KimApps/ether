package com.example.withdraw.layer.presentation.page.view_model

/**
 * Represents the complete UI state of the Withdraw screen.
 *
 * This is a pure, immutable snapshot consumed by the UI layer.
 * The [WithdrawViewModel] produces a new copy of this state whenever
 * something changes, driving recomposition through a [kotlinx.coroutines.flow.StateFlow].
 *
 * @property amount    The current value of the amount text field as a raw string.
 *                     Defaults to an empty string (blank field on first load).
 * @property isLoading Whether an async operation (quotation fetch or transaction submission)
 *                     is currently in progress. When `true`, the UI should show a loading
 *                     indicator and disable interactive controls.
 * @property error     A human-readable error message to display when an operation fails,
 *                     or `null` when there is no error.
 * @property isSuccess `true` once the backend confirms the withdrawal was accepted.
 *                     The UI can use this flag to show a success message or navigate away.
 */
data class WithdrawState(
    val amount: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
)