package com.example.withdraw.layer.presentation.page.view_model

sealed class WithdrawEffect {
    data class NavigateToSigning(val challenge: String, val type: String) : WithdrawEffect()
    object AmountNotValid : WithdrawEffect()
}

