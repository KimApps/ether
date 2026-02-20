package com.kimapps.signing.layer.presentation.page.view_model

import com.kimapps.signing.layer.domain.enums.OperationType

sealed class SigningIntent {
    data class OnInit(val challenge: String, val type: OperationType) : SigningIntent()
    object OnSignClicked : SigningIntent()
    object OnCancelClicked : SigningIntent()
}