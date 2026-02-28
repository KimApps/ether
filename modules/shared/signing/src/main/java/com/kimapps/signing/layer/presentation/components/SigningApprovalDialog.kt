package com.kimapps.signing.layer.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SigningApprovalDialog - A confirmation dialog shown when an external dApp sends
 * a WalletConnect session request that requires the user's explicit approval.
 *
 * This dialog is the last security gate before a signature is produced and sent
 * back to the dApp. The user must consciously tap **Approve** or **Reject** —
 * tapping outside the dialog (onDismissRequest) is treated as a rejection to
 * prevent accidental approvals.
 *
 * @param challenge  The raw challenge string being signed. Only the first 20
 *                   characters are displayed to keep the dialog compact while
 *                   still giving the user a recognisable preview.
 * @param onApprove  Called when the user taps "Approve". The caller is responsible
 *                   for generating the signature and responding to the dApp via WalletKit.
 * @param onReject   Called when the user taps "Reject" or dismisses the dialog.
 *                   The caller should send an error response to the dApp via WalletKit.
 */
@Composable
fun SigningApprovalDialog(
    challenge: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        // Treat a tap outside the dialog the same as an explicit rejection
        // to avoid the dApp being left waiting for a response indefinitely
        onDismissRequest = onReject,
        title = { Text("Confirm Signature") },
        text = {
            Column {
                // Inform the user that this request originates from an external source
                // (a dApp connected via WalletConnect), not from the app itself
                Text("An external application is requesting a signature.")

                Spacer(modifier = Modifier.height(12.dp))

                // Show a truncated challenge preview so the user can cross-check it
                // with what the dApp is displaying on its own UI
                Text(
                    "Challenge: ${challenge.take(20)}...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            // Filled button = primary / affirmative action (approve the signature)
            Button(onClick = onApprove) { Text("Approve") }
        },
        dismissButton = {
            // Text button = secondary / destructive action (reject and notify the dApp)
            TextButton(onClick = onReject) { Text("Reject") }
        }
    )
}