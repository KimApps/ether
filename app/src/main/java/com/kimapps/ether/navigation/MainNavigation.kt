package com.kimapps.ether.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.navigation.AppRoute
import com.example.withdraw.layer.presentation.page.WithdrawPage
import com.kimapps.home.home.presentation.page.HomePage
import com.kimapps.signing.layer.domain.enums.OperationType
import com.kimapps.signing.layer.presentation.page.SigningPage

@Composable
fun MainNavigation(
    navigation: NavigationViewModel = hiltViewModel()
) {
    NavDisplay(
        backStack = navigation.backstack,
        onBack = { navigation.pop() },
        entryProvider = entryProvider {
            entry<AppRoute.Home> {
                HomePage(
                    onWithdrawClick = { navigation.navigateTo(AppRoute.Withdraw) }
                )
            }
            entry<AppRoute.Withdraw> {
                WithdrawPage(
                    onNavigateToSigning = { challenge, type ->
                        navigation.navigateTo(
                            AppRoute.Signing(challenge, type)
                        )
                    },
                    onBack = { navigation.pop() },
                )
            }
            entry<AppRoute.Signing> {
                // only developer's mistake can produce error here
                val type = requireNotNull(
                    OperationType.fromString(it.operationType)
                ) {
                    "Invalid OperationType string: ${it.operationType}"
                }
                SigningPage(
                    challenge = it.challenge,
                    operationType = type,
                    onBack = { navigation.pop() }
                )
            }
        }
    )
}