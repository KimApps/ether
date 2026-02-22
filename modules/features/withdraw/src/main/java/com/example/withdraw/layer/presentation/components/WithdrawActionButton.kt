package com.example.withdraw.layer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kimapps.ui.buttons.AppButton

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
    AppButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        isLoading = isLoading,
        text = "Withdraw"
    )
}