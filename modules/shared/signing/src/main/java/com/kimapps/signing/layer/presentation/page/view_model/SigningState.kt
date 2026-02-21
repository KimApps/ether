package com.kimapps.signing.layer.presentation.page.view_model

import com.kimapps.signing.layer.domain.enums.OperationType
import com.reown.walletkit.client.Wallet

data class SigningState(
    val challenge: String = "",
    val operationType: OperationType = OperationType.WITHDRAWAL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pairingUri: String = "",
    val showPairingInput: Boolean = false,
    val isWalletConnected: Boolean = false,
    val pendingRequest: Wallet.Model.SessionRequest? = null
)