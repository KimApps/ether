package com.kimapps.home.home.presentation.page.view_model

sealed interface HomeEffect {
    data object NavigateToWithdraw : HomeEffect
}