package com.kimapps.signing.layer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kimapps.ui.buttons.AppButton
import com.kimapps.ui.buttons.AppButtonType

/**
 * WalletConnectSection - Handles the entire WalletConnect pairing and connection UI
 * as a self-contained component with three mutually exclusive visual states:
 *
 * 1. **Connected** ([isConnected] = true) – shows a confirmation card telling the
 *    user to trigger a sign request from the dApp website.
 * 2. **Pairing input** ([showPairingInput] = true) – shows a URI text field, a
 *    "Pair Wallet" button, and a link to the WalletConnect test dApp so the user
 *    can obtain a valid `wc:` URI to paste.
 * 3. **Default** (both false) – shows a single "Connect EOA Wallet" outlined button
 *    that transitions to state 2 when tapped.
 *
 * The component is stateless — all state lives in the ViewModel and is passed in
 * via parameters. UI events are reported back through lambda callbacks.
 *
 * @param isConnected        True once WalletConnectManager reports a settled session.
 * @param connectedAddress   The EOA address of the connected wallet (e.g. "0x1234...abcd").
 *                           Null when no session is active. Displayed as "Sign with 0x1234..."
 *                           so the user can verify which account has been connected.
 * @param showPairingInput   True when the user has tapped "Connect" and the URI
 *                           input field should be visible.
 * @param pairingUri         Current text value of the URI input field.
 * @param onConnectClick     Called when the user taps "Connect EOA Wallet" (state 3 → 2).
 * @param onUriChanged       Called on every keystroke in the URI field so the ViewModel
 *                           can keep [pairingUri] up to date.
 * @param onPairClick        Called when the user taps "Pair Wallet". The ViewModel passes
 *                           the URI to WalletConnectManager.pair() which triggers
 *                           CoreClient.Pairing.pair() under the hood.
 * @param onOpenBrowserClick Called when the user taps the test dApp link. Opens the
 *                           WalletConnect demo site in the device browser so the user
 *                           can copy a fresh `wc:` URI.
 */
@Composable
fun WalletConnectSection(
    isConnected: Boolean,
    connectedAddress: String?,
    showPairingInput: Boolean,
    pairingUri: String,
    isAwaitingApproval: Boolean,
    onConnectClick: () -> Unit,
    onUriChanged: (String) -> Unit,
    onPairClick: () -> Unit,
    onOpenBrowserClick: () -> Unit
) {
    // Outer column groups all WalletConnect controls with consistent vertical spacing
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Visual separator between the Passkey button above and the WalletConnect section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Render one of three states depending on the current connection + UI flags
        when {

            // ── State 1: Session is active ────────────────────────────────────────
            // Shown after onSessionSettleResponse fires in WalletConnectManager.
            // The user must now go back to the dApp and trigger a personal_sign request,
            // which will arrive as a SessionRequest and surface the approval dialog.
            isConnected -> {
                // Truncate the address to "0x1234...abcd" format — short enough to fit
                // on screen but still recognisable for the user to verify the right account.
                val displayAddress = connectedAddress
                    ?.let { addr ->
                        if (addr.length > 10) "${addr.take(6)}...${addr.takeLast(4)}" else addr
                    }
                    ?: "Unknown"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    // Subtle tinted background to signal a positive / success state
                    // without being as prominent as a full primaryContainer fill
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            // Shows the requirement: "Sign with 0x1234..."
                            text = "Sign with $displayAddress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Trigger sign request from the website",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            isAwaitingApproval -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Approve the Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Go to your browser or the Dapp to approve the connection request.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                    }
                }
            }

            // ── State 2: Pairing input visible ────────────────────────────────────
            // Shown after the user taps "Connect EOA Wallet".
            // The user must paste a wc: URI obtained from a dApp (e.g. the test dApp link below).
            showPairingInput -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Text field for the wc: pairing URI copied from a dApp QR code or link
                    OutlinedTextField(
                        value = pairingUri,
                        onValueChange = onUriChanged,
                        label = { Text("WalletConnect URI") },
                        // Placeholder shows the expected URI prefix so the user knows the format
                        placeholder = { Text("wc:...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // "Pair Wallet" is only enabled once the URI starts with "wc:"
                    // to prevent sending an obviously invalid string to CoreClient
                    AppButton(
                        text = "Pair Wallet",
                        onClick = onPairClick,
                        enabled = pairingUri.startsWith("wc:")
                    )

                    // Convenience link that opens the WalletConnect test dApp in a browser.
                    // The test dApp at react-app.walletconnect.com lets developers connect
                    // this wallet, then send a personal_sign request to test the full flow
                    // without needing a real production dApp.
                    AppButton(
                        type = AppButtonType.TEXT,
                        onClick = onOpenBrowserClick,
                        text = "Get URI from WalletConnect Test Dapp ↗️"
                    )
                }
            }

            // ── State 3: Default / entry point ────────────────────────────────────
            // Initial state. OutlinedButton (secondary style) signals this is an
            // optional alternative to the primary Passkey button above.
            else -> {
                AppButton(
                    text = "Connect EOA Wallet",
                    type = AppButtonType.SECONDARY,
                    onClick = onConnectClick
                )
            }
        }
    }
}