package com.kimapps.signing.layer.presentation.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    viewModel: SigningViewModel = hiltViewModel(),
    challenge: String,
    operationType: OperationType,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    //  Fire OnInit intent once when the page is first displayed
    LaunchedEffect(challenge, operationType) {
        viewModel.onIntent(
            SigningIntent.OnInit(
                challenge = challenge,
                type = operationType
            )
        )
    }

    // handle one time effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SigningEffect.Close -> onBack()
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Verify Transaction",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onIntent(SigningIntent.OnCancelClicked) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
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
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = "Signing ${state.operationType.name}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Challenge ${state.challenge.take(10)}...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (state.isLoading) {
                CircularProgressIndicator()
                Text(
                    "Signing in progress...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Text("Sign with Passkey")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Text("Sign with Wallet")
                }
            }

            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

    }

}