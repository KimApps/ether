package com.kimapps.ui.buttons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

enum class AppButtonType {
    PRIMARY,   // Filled Button
    SECONDARY, // Outlined Button
    TEXT       // Text Button (No border/background)
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AppButtonType = AppButtonType.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    wrapContent: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    shape: Shape = CircleShape
) {
    // Define the base modifier
    val baseModifier = if (wrapContent) {
        modifier
    } else {
        modifier.fillMaxWidth()
    }

    // Shared content lambda to avoid repetition
    val content: @Composable RowScope.() -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (type == AppButtonType.PRIMARY)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary
            )
        } else {
            Text(text = text, style = textStyle)
        }
    }

    when (type) {
        AppButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = baseModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                content = content
            )
        }

        AppButtonType.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                modifier = baseModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                content = content
            )
        }

        AppButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = baseModifier,
                enabled = enabled && !isLoading,
                shape = shape,
                content = content
            )
        }
    }
}