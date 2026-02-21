package com.kimapps.signing.layer.presentation.page

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kimapps.signing.layer.domain.enums.OperationType
import com.kimapps.signing.layer.presentation.page.view_model.SigningEffect
import com.kimapps.signing.layer.presentation.page.view_model.SigningIntent
import com.kimapps.signing.layer.presentation.page.view_model.SigningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningPage(
    challenge: String,
    operationType: OperationType,
    onBack: () -> Unit,
    viewModel: SigningViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Initialize the ViewModel with the route data
    LaunchedEffect(challenge, operationType) {
        viewModel.onIntent(SigningIntent.OnInit(challenge, operationType))
    }

    // Handle one-time side effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SigningEffect.Close -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Authorize Operation") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onIntent(SigningIntent.OnCancelClicked) }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.operationType.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Verify the details below to sign",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Challenge Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CHALLENGE", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = state.challenge,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (state.isLoading) {
                CircularProgressIndicator()
                Text("Processing...", style = MaterialTheme.typography.bodySmall)
            } else {
                // Main Sign Button (Passkey Mock)
                Button(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = { viewModel.onIntent(SigningIntent.OnSignClicked) }
                ) {
                    Text("Sign with Passkey")
                }

                // EOA / WalletConnect Section
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (state.isWalletConnected) {
                    // State: Wallet is paired and ready
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Wallet Connected ✅\nTrigger sign request from the Dapp",
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (state.showPairingInput) {
                    // State: Manual Pairing Entry
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.pairingUri,
                            onValueChange = { viewModel.onIntent(SigningIntent.OnPairingUriChanged(it)) },
                            label = { Text("WalletConnect URI (wc:...)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste URI from Dapp") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { /* You could add a 'Back' intent here */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { viewModel.onIntent(SigningIntent.OnPairClicked) },
                                modifier = Modifier.weight(1f),
                                enabled = state.pairingUri.startsWith("wc:")
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                } else {
                    // State: Default/Disconnected
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        onClick = { viewModel.onIntent(SigningIntent.OnSignWithWalletClicked) }
                    ) {
                        Text("Connect EOA Wallet")
                    }
                }
            }
        }
    }

    // --- WalletConnect Request Dialog ---
    // This pops up when WalletConnectManager receives an 'onSessionRequest'
    if (state.pendingRequest != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(SigningIntent.RejectWalletSign) },
            title = { Text("Confirm Signature") },
            text = {
                Column {
                    Text("An external application is requesting a signature for this transaction.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Challenge: ${state.challenge.take(20)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.onIntent(SigningIntent.ApproveWalletSign) }) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(SigningIntent.RejectWalletSign) }) {
                    Text("Reject")
                }
            }
        )
    }
}