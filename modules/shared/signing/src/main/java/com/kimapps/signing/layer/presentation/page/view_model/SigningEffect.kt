package com.kimapps.signing.layer.presentation.page.view_model

sealed class SigningEffect {
    object Close : SigningEffect()
    object OpenReownModal : SigningEffect()
}