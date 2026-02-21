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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.withdraw.layer.presentation.page.view_model.WithdrawEffect
import com.example.withdraw.layer.presentation.page.view_model.WithdrawIntent
import com.example.withdraw.layer.presentation.page.view_model.WithdrawViewModel

/**
 * WithdrawPage - Main composable screen for handling fund withdrawal
 *
 * This page allows users to enter an amount they wish to withdraw and initiates
 * the withdrawal process. It handles navigation to signing and displays feedback
 * through snackbars and loading states.
 *
 * @param viewModel The ViewModel that manages the withdrawing state and business logic
 * @param onNavigateToSigning Callback invoked when user needs to sign the transaction,
 *                           receives the challenge string for signing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawPage(
    viewModel: WithdrawViewModel = hiltViewModel(),
    onNavigateToSigning: (String, String) -> Unit,
    onBack: () -> Unit
) {
    // Collect the current state from the ViewModel as a Compose State
    val state by viewModel.state.collectAsState()

    // Remember snackbar host state across recompositions for showing messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Side effect to collect and handle one-time effects from the ViewModel
    // This runs once when the composable enters the composition
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // Show snackbar when amount validation fails
                is WithdrawEffect.AmountNotValid -> {
                    snackbarHostState.showSnackbar("Please enter a valid amount")
                }
                // Navigate to signing screen with the challenge when transaction is ready
                is WithdrawEffect.NavigateToSigning -> {
                    onNavigateToSigning(effect.challenge, effect.type)
                }
                is WithdrawEffect.WithdrawSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = "Withdrawal submitted successfully!",
                        withDismissAction = true
                    )
                }
            }
        }
    }

    // Main screen structure using Material Design 3 Scaffold
    // Provides standard app structure with top bar and snackbar hosting
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Withdraw Funds") },
                navigationIcon = {
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
        // Main content column with centered alignment and even spacing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructional text explaining the purpose of the screen
            Text(
                text = "Enter the amount you wish to withdraw",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Input field for withdrawal amount
            // - Only accepts decimal numbers via keyboard type
            // - Disabled during loading to prevent changes
            // - Shows dollar sign prefix for clarity
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onIntent(WithdrawIntent.OnAmountChanged(it)) },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !state.isLoading,
                prefix = { Text("$ ") }
            )

            // Flexible spacer to push the button to the bottom
            Spacer(modifier = Modifier.weight(1f))

            // Withdraw action button
            // - Shows loading indicator during processing
            // - Disabled when loading or amount field is empty
            Button(
                onClick = { viewModel.onIntent(WithdrawIntent.OnWithdrawClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && state.amount.isNotBlank()
            ) {
                if (state.isLoading) {
                    // Show circular progress indicator while processing
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Withdraw")
                }
            }

            // Error message display (only shown when error exists)
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
