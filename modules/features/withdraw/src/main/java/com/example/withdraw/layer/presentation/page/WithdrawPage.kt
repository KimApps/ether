package com.example.withdraw.layer.presentation.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.withdraw.layer.presentation.components.WithdrawActionButton
import com.example.withdraw.layer.presentation.components.WithdrawAmountInput
import com.example.withdraw.layer.presentation.components.WithdrawHeader
import com.example.withdraw.layer.presentation.page.view_model.WithdrawEffect
import com.example.withdraw.layer.presentation.page.view_model.WithdrawIntent
import com.example.withdraw.layer.presentation.page.view_model.WithdrawViewModel

/**
 * WithdrawPage - Main composable screen for handling fund withdrawal.
 *
 * Supports the following flow:
 * 1. User types an amount into the input field.
 * 2. User taps "Withdraw" — ViewModel validates the amount and requests a quotation.
 * 3. If valid, [WithdrawEffect.NavigateToSigning] is emitted and the caller
 *    navigates to the signing screen with the challenge string.
 * 4. After signing completes (success or cancel), a snackbar confirms the outcome.
 *
 * UI events travel in one direction:
 *   User action → [WithdrawIntent] → [WithdrawViewModel] → WithdrawState / [WithdrawEffect]
 *
 * One-time effects (navigation, snackbars) are delivered through a Channel so
 * they are consumed exactly once, even across recompositions.
 *
 * @param viewModel            Hilt-injected ViewModel; can be overridden in tests.
 * @param onNavigateToSigning  Called when the transaction is ready to be signed.
 *                             Receives the challenge string and operation type string
 *                             to pass as navigation arguments to the signing screen.
 * @param onBack               Called when the user taps the back arrow in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawPage(
    viewModel: WithdrawViewModel = hiltViewModel(),
    onNavigateToSigning: (String, String) -> Unit,
    onBack: () -> Unit
) {
    // Collect the full UI state as Compose State — any field change triggers recomposition
    val state by viewModel.state.collectAsStateWithLifecycle()

    // SnackbarHostState is remembered across recompositions so pending snackbars
    // are not dropped when the composition updates
    val snackbarHostState = remember { SnackbarHostState() }

    // ─────────────────────────────────────────────
    // movableContentOf blocks
    //
    // Each block is keyed on the state slice it reads.
    // movableContentOf preserves internal Compose state and layout nodes when the
    // composable moves in the tree, preventing unnecessary recompositions.
    // ─────────────────────────────────────────────

    // Static header — no state dependency, created once for the lifetime of the screen
    val header = remember { movableContentOf { WithdrawHeader() } }

    // Amount input — re-created only when the amount text or loading flag changes
    val input = remember(state.amount, state.isLoading) {
        movableContentOf {
            WithdrawAmountInput(
                amount = state.amount,
                // Disable input while a network request or signing flow is in progress
                enabled = !state.isLoading,
                onAmountChange = { viewModel.onIntent(WithdrawIntent.OnAmountChanged(it)) },
            )
        }
    }

    // Submit button — re-created when loading state or amount text changes.
    // The button is only enabled when not loading AND the amount field is not blank,
    // preventing empty or duplicate submissions.
    val actionButton = remember(state.isLoading, state.amount) {
        movableContentOf {
            WithdrawActionButton(
                isLoading = state.isLoading,
                enabled = !state.isLoading && state.amount.isNotBlank(),
                onClick = { viewModel.onIntent(WithdrawIntent.OnWithdrawClicked) }
            )
        }
    }

    // ─────────────────────────────────────────────
    // One-time effects
    //
    // Unit key means this collector is registered once and lives for the full
    // lifetime of the composable — it is NOT restarted on each recomposition.
    // ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // Validation failed — tell the user the amount is invalid without
                // blocking the UI (snackbar auto-dismisses)
                is WithdrawEffect.AmountNotValid -> {
                    snackbarHostState.showSnackbar("Please enter a valid amount")
                }

                // Quotation fetched successfully — hand off to the signing screen.
                // The challenge string is the payload the user must sign; the type
                // string identifies the operation (e.g. "WITHDRAWAL") for display.
                is WithdrawEffect.NavigateToSigning -> {
                    onNavigateToSigning(effect.challenge, effect.type)
                }

                // User cancelled on the signing screen — signing coordinator returned
                // SigningResultEntity.Cancelled; inform the user and stay on this screen
                is WithdrawEffect.WithdrawCancelled -> {
                    snackbarHostState.showSnackbar(
                        message = "Signing cancelled",
                        withDismissAction = true
                    )
                }

                // Signing succeeded and withdrawal was submitted — confirm to the user
                is WithdrawEffect.WithdrawSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = "Withdrawal submitted successfully!",
                        withDismissAction = true
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // UI Layout
    // ─────────────────────────────────────────────

    // Scaffold provides the standard Material 3 screen structure:
    // top bar for navigation + title, snackbar host for messages
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Withdraw Funds") },
                navigationIcon = {
                    // Back arrow — returns to the previous screen (e.g. Home)
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)   // respect Scaffold insets (status bar, nav bar)
                .padding(24.dp),    // additional screen-edge margin
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen title / subtitle explaining what this screen does
            header()

            // Amount text field — disabled while the submit flow is running
            input()

            // Push action button and error text to the bottom of the screen
            Spacer(modifier = Modifier.weight(1f))

            // Primary CTA: "Withdraw" button (or loading indicator while in progress)
            actionButton()

            // Inline error text — only rendered when WithdrawState.error is non-null.
            // Complements the snackbar for persistent, contextual error display
            // (e.g. a server error that the user should read before retrying).
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
