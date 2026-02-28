package com.kimapps.ether.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {
    // single source of truth for the entire app navigation stack
    var backstack by mutableStateOf<List<AppRoute>>(listOf(AppRoute.Home))
        private set

    fun navigateTo(route: AppRoute) {
        // add route to backstack
        if (backstack.last() != route) {
            backstack = backstack + route
        }
    }

    fun pop() {
        // only pop if we have more than one route on the backstack
        if (backstack.size > 1) {
            backstack = backstack.dropLast(1)
        }
    }

    // reset backstack and navigate to home
    fun resetToHome() {
        backstack = listOf(AppRoute.Home)
    }
}