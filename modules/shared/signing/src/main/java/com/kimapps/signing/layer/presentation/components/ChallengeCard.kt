package com.kimapps.signing.layer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ChallengeCard - Displays a compact preview of the signing challenge.
 *
 * Shows only the first 7 characters followed by "..." to avoid
 * exposing the full challenge string in the UI while still giving
 * the user a recognisable identifier of what they are signing.
 *
 * @param challenge The raw challenge string received from the server.
 */
@Composable
fun ChallengeCard(challenge: String) {
    // Subtle surface card to visually separate the challenge from the rest of the screen
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // Semi-transparent surfaceVariant keeps the card visible on any background
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
    ) {
        // Row lays the label and value on opposite ends of the card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Static label so the user understands what the value next to it represents
            Text("CHALLENGE:", style = MaterialTheme.typography.labelSmall)

            // Truncated challenge value – monospace font makes the hash-like string
            // easier to read and visually distinct from regular body text
            Text(
                text = "${challenge.take(7)}...",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}