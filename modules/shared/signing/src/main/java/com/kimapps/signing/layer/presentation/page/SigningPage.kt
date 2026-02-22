package com.kimapps.signing.layer.presentation.page

import SigningApprovalDialog
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kimapps.signing.layer.domain.enums.OperationType
import com.kimapps.signing.layer.presentation.components.ChallengeCard
import com.kimapps.signing.layer.presentation.components.SigningHeader
import com.kimapps.signing.layer.presentation.components.WalletConnectSection
import com.kimapps.signing.layer.presentation.page.view_model.SigningEffect
import com.kimapps.signing.layer.presentation.page.view_model.SigningIntent
import com.kimapps.signing.layer.presentation.page.view_model.SigningViewModel
import com.kimapps.ui.buttons.AppButton

/**
 * SigningPage - The screen responsible for signing a transaction challenge.
 *
 * Supports two signing methods:
 * 1. **Passkey** – signs directly on the device using biometrics / device credentials.
 * 2. **WalletConnect** – pairs with an external EOA wallet (e.g. MetaMask) via a URI,
 *    waits for the dApp to send a `personal_sign` session request, then approves it.
 *
 * Flow:
 *  1. Screen opens with a [challenge] string and an [operationType] (e.g. WITHDRAWAL).
 *  2. User chooses a signing method.
 *  3. On success the result is delivered through SigningCoordinator and the screen closes.
 *  4. On cancel/back the [onBack] callback is invoked.
 *
 * @param challenge    The raw challenge string that must be signed.
 * @param operationType The type of operation being authorised (used for display only).
 * @param onBack       Called when the screen should be dismissed (success or cancel).
 * @param viewModel    Hilt-injected ViewModel; can be overridden in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigningPage(
    challenge: String,
    operationType: OperationType,
    onBack: () -> Unit,
    viewModel: SigningViewModel = hiltViewModel()
) {
    // Context is needed to launch an external browser intent for the WalletConnect test dApp
    val context = LocalContext.current

    // Collect the full UI state from the ViewModel as Compose State.
    // Any change to the state triggers a recomposition of the affected subtree.
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ─────────────────────────────────────────────
    // Back-press interception
    // ─────────────────────────────────────────────

    // Intercept the system back button (and gesture) before NavDisplay handles it.
    // Without this, NavDisplay.onBack pops the screen immediately and
    // SigningCoordinator never receives a Cancelled result, leaving the
    // WithdrawViewModel suspended on deferred.await() forever.
    // By routing back through OnCancelClicked we guarantee that:
    //   1. provideResult(Cancelled) is called → WithdrawViewModel resumes.
    //   2. SigningEffect.Close is emitted → onBack() pops the screen.
    // This is the single exit path for both cancel and success, ensuring
    // the coordinator is always notified before the composable leaves the backstack.
    BackHandler {
        viewModel.onIntent(SigningIntent.OnCancelClicked)
    }

    // ─────────────────────────────────────────────
    // Side-effects
    // ─────────────────────────────────────────────

    // Initialize the ViewModel with the route data
    // Re-runs only when challenge or operationType changes (i.e. on first composition).
    LaunchedEffect(challenge, operationType) {
        viewModel.onIntent(SigningIntent.OnInit(challenge, operationType))
    }

    // Handle one-time side effects emitted through the effect Channel.
    // Using Unit as the key means this collector is set up once and lives
    // for the full lifetime of the composable.
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // Close this screen – triggered after a successful sign or a cancel
                is SigningEffect.Close -> onBack()
            }
        }
    }

    // ─────────────────────────────────────────────
    // UI Layout
    // ─────────────────────────────────────────────

    Scaffold(
        topBar = { /* ... keep top bar ... */ }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Operation title (e.g. "WITHDRAWAL")
            SigningHeader(state.operationType.name)

            // Truncated challenge preview so the user knows what they are signing
            ChallengeCard(state.challenge)

            // Push action buttons to the bottom of the screen
            Spacer(modifier = Modifier.weight(1f))

            if (state.isLoading) {
                // Shown while the passkey signing coroutine or network call is in progress
                CircularProgressIndicator()
            } else {
                // Primary action: sign using the device Passkey (biometrics)
                AppButton(
                    text = "Sign with Passkey",
                    onClick = { viewModel.onIntent(SigningIntent.OnSignClicked) }
                )
                // Secondary action group: WalletConnect pairing + signing controls
                WalletConnectSection(
                    isConnected = state.isWalletConnected,
                    connectedAddress = state.connectedAddress,
                    showPairingInput = state.showPairingInput,
                    pairingUri = state.pairingUri,
                    isAwaitingApproval = state.isAwaitingApprovalFromDapp,
                    onConnectClick = { viewModel.onIntent(SigningIntent.OnSignWithWalletClicked) },
                    onUriChanged = { viewModel.onIntent(SigningIntent.OnPairingUriChanged(it)) },
                    onPairClick = { viewModel.onIntent(SigningIntent.OnPairClicked) },
                    onOpenBrowserClick = {
                        // Open the official WalletConnect test dApp in the browser.
                        // The dApp lets you send a personal_sign request to this wallet,
                        // which will arrive as a SessionRequest and show the approval dialog.
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://react-app.walletconnect.com/".toUri()
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Approval dialog is rendered outside the Column so it overlays the entire Scaffold.
        // Only shown when the dApp has sent a pending session request.
        state.pendingRequest?.let {
            SigningApprovalDialog(
                challenge = state.challenge,
                // Approve: signs with a mock signature and notifies the dApp via WalletKit
                onApprove = { viewModel.onIntent(SigningIntent.ApproveWalletSign) },
                // Reject: sends an error response to the dApp and clears the pending request
                onReject = { viewModel.onIntent(SigningIntent.RejectWalletSign) }
            )
        }
    }
}