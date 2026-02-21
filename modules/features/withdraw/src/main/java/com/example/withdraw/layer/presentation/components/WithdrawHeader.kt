package com.example.withdraw.layer.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * A static header displayed at the top of the withdraw screen.
 *
 * Renders a short instructional message that guides the user to enter
 * the amount they wish to withdraw. Styled with [MaterialTheme.typography.bodyLarge]
 * and tinted with [MaterialTheme.colorScheme.onSurfaceVariant] to keep it
 * visually secondary to the main input controls.
 */
@Composable
fun WithdrawHeader() {
    Text(
        text = "Enter the amount you wish to withdraw",
        // Use bodyLarge for readable but non-dominant instructional copy
        style = MaterialTheme.typography.bodyLarge,
        // onSurfaceVariant keeps the header visually de-emphasised
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}