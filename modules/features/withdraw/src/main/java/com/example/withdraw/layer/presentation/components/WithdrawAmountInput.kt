package com.example.withdraw.layer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * A full-width outlined text field for entering a withdrawal amount.
 *
 * The field is pre-configured for decimal numeric input and prefixed with a
 * dollar sign so the user always sees the currency context.
 *
 * @param amount The current text value displayed in the field.
 * @param enabled Whether the field accepts user input. Set to `false` while a
 *                withdraw operation is in progress to prevent edits.
 * @param onAmountChange Callback invoked every time the user changes the text,
 *                       receiving the updated string value.
 */
@Composable
fun WithdrawAmountInput(
    amount: String,
    enabled: Boolean,
    onAmountChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth(),
        // Restrict the soft keyboard to decimal numbers only
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        // Currency prefix shown inside the field to the left of the amount
        prefix = { Text("$ ") }
    )
}