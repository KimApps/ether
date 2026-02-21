package com.example.withdraw.layer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A full-width action button for the withdraw flow.
 *
 * Displays a [CircularProgressIndicator] while a withdraw operation is in progress,
 * or a "Withdraw" label when idle.
 *
 * @param isLoading Whether a withdraw operation is currently in progress.
 *                  When `true`, a loading spinner is shown instead of the button label.
 * @param enabled Whether the button is interactive. Typically `false` when input is invalid
 *                or another operation is already running.
 * @param onClick Callback invoked when the user taps the button.
 */
@Composable
fun WithdrawActionButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        if (isLoading) {
            // Show a spinner while the withdraw request is in flight
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            // Show the default action label when idle
            Text("Withdraw")
        }
    }
}