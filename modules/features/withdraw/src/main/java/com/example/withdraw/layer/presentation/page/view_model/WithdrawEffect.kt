package com.example.withdraw.layer.presentation.page.view_model

sealed class WithdrawEffect {
    data class NavigateToSigning(val challenge: String) : WithdrawEffect()
    object AmountNotValid : WithdrawEffect()
}

