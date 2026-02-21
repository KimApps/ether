package com.kimapps.signing.wallet_connect

import android.app.Application
import android.content.Context
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit

/**
 * WalletConnectInitializer - Bootstraps the WalletConnect SDK exactly once
 * per application process.
 *
 * Must be called before any other WalletKit or CoreClient API is used.
 * The [init] function is idempotent — repeated calls after the first
 * successful initialisation are silently ignored via the [isInitialized] guard.
 *
 * Initialisation order mandated by the SDK:
 * 1. [CoreClient.initialize] – sets up the relay connection and app metadata.
 * 2. [WalletKit.initialize] – builds the WalletKit client on top of CoreClient.
 * 3. [WalletKit.setWalletDelegate] – registers the delegate that receives
 *    session proposals, session requests, and connection state changes.
 *
 * @param context       Application context used to cast to [Application] for
 *                      CoreClient. Must be the application context, not an
 *                      Activity context, to avoid memory leaks.
 * @param walletManager The [WalletConnectManager] instance that implements
 *                      [WalletKit.WalletDelegate] and will receive all
 *                      WalletConnect callbacks.
 */
class WalletConnectInitializer(
    private val context: Context,
    private val walletManager: WalletConnectManager
) {
    /**
     * Guards against re-initialisation.
     * WalletKit throws if initialised twice in the same process, so this
     * flag ensures [init] is safe to call from the ViewModel's init block
     * on every screen entry without risk of a crash.
     */
    private var isInitialized = false

    /**
     * Performs the one-time SDK bootstrap. Subsequent calls are no-ops.
     *
     * Should be called as early as possible — ideally from the ViewModel's
     * init block or from [Application.onCreate] — so the relay WebSocket
     * connection is established before the user interacts with the screen.
     */
    fun init() {
        // Early exit if already initialised — makes this safe to call multiple times
        if (isInitialized) return

        // CoreClient requires an Application reference (not an Activity) to attach
        // lifecycle-aware components without leaking the calling Activity
        val application = context as Application

        // Project ID obtained from https://dashboard.walletconnect.com
        // Identifies this app on the WalletConnect relay network
        // TODO: Move to local.properties + BuildConfig before production.
        val projectId = "d69d95933ce80c7994eb21fe9d31eb74"

        // Metadata shown to the dApp when it receives the session proposal,
        // so the user can see which wallet app they are connecting to
        val appMetaData = Core.Model.AppMetaData(
            name = "ether",
            description = "signing flow",
            url = "https://kimapps.com",
            icons = listOf("https://reown.com/favicon.ico"),
            // Deep-link URI the dApp uses to redirect back to this wallet
            // after a session approval or sign-request response
            redirect = "kotlin-signer-wc:/request"
        )

        // Step 1 — Initialise CoreClient with the relay connection type and app metadata.
        // AUTOMATIC connection type means the SDK manages the WebSocket lifecycle
        // automatically (reconnects on network changes, etc.)
        CoreClient.initialize(
            projectId = projectId,
            connectionType = ConnectionType.AUTOMATIC,
            application = application,
            metaData = appMetaData,
            // Telemetry is disabled here; enable in production to help WalletConnect
            // diagnose relay connectivity issues without exposing user data
            telemetryEnabled = false,
            onError = { e ->
                // Log relay initialisation errors — in production, send to Crashlytics
                println("CoreClient Error: $e")
            }
        )

        // Step 2 — Initialise WalletKit on top of the already-configured CoreClient
        val initParams = Wallet.Params.Init(core = CoreClient)
        WalletKit.initialize(initParams) { error ->
            // Called on the background thread if initialisation fails.
            // In production, log this to Crashlytics and surface a user-facing error.
            println("WalletKit Init Error: $error")
        }

        // Step 3 — Register the delegate AFTER WalletKit.initialize so the SDK
        // is fully ready to route callbacks. Registering before initialisation
        // would cause the delegate to receive no events.
        WalletKit.setWalletDelegate(walletManager)

        // Mark as done so future calls to init() are skipped
        isInitialized = true
    }
}