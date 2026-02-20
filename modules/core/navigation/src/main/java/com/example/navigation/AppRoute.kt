package com.example.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Withdraw : AppRoute

    @Serializable
    data class Signing(
        val challenge: String,
        val operationType: String
    ) : AppRoute

    @Serializable
    data object SuccessWithdraw : AppRoute

}