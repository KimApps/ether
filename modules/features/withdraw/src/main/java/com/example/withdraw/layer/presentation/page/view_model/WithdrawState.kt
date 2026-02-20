package com.example.withdraw.layer.presentation.page.view_model

data class WithdrawState(
    val amount: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)