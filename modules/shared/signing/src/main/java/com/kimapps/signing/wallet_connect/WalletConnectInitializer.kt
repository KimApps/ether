package com.kimapps.signing.wallet_connect

import android.app.Application
import android.content.Context
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit

class WalletConnectInitializer(
    private val context: Context,
    private val walletManager: WalletConnectManager
) {
    private var isInitialized = false
    fun init() {
        if (isInitialized) return
        val application = context as Application
        val projectId = "d69d95933ce80c7994eb21fe9d31eb74"
        val appMetaData = Core.Model.AppMetaData(
            name = "ether",
            description = "signing flow",
            url = "https://kimapps.com",
            icons = listOf("https://reown.com/favicon.ico"),
            redirect = "kotlin-signer-wc:/request"
        )

        CoreClient.initialize(
            projectId = projectId,
            connectionType = ConnectionType.AUTOMATIC,
            application = application,
            metaData = appMetaData,
            telemetryEnabled = false,
            onError = { e ->
                println("CoreClient Error: $e")
            }
        )

        val initParams = Wallet.Params.Init(core = CoreClient)
        WalletKit.initialize(initParams) { error ->
            // In a real app, log this to Crashlytics
            println("WalletKit Init Error: $error")
        }
        WalletKit.setWalletDelegate(walletManager)
        isInitialized = true
    }
}