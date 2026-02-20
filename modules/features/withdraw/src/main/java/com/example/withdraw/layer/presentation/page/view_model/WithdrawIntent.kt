package com.example.withdraw.layer.presentation.page.view_model

sealed class WithdrawIntent {
    data class OnAmountChanged(val amount: String) : WithdrawIntent()
    object OnWithdrawClicked : WithdrawIntent()
}
