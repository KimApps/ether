package com.kimapps.home.home.presentation.page.view_model

sealed interface HomeIntent {
    data object OnWithdrawClick: HomeIntent
}