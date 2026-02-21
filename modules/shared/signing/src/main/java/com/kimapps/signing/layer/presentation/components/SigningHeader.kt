package com.kimapps.signing.layer.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight

/**
 * SigningHeader - Top section of the signing screen that identifies
 * the operation the user is about to authorise.
 *
 * Displays two lines:
 * - The operation name in a large bold headline (e.g. "WITHDRAWAL").
 * - A static subtitle prompting the user to review the details below.
 *
 * @param operationName The display name of the operation type, derived from
 *                      OperationType.name. Shown in the primary brand colour
 *                      so it stands out as the most important piece of information.
 */
@Composable
fun SigningHeader(operationName: String) {
    // Centre-align both texts so the header reads as a cohesive title block
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {}

    // Operation name – bold + primary colour draws the user's eye immediately
    // and makes it clear which action they are authorising
    Text(
        text = operationName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    // Static subtitle – sets the user's expectation to review the challenge
    // card below before tapping a signing button
    Text(
        text = "Verify the details below to sign",
        style = MaterialTheme.typography.bodyMedium
    )
}