package com.kimapps.signing.wallet_connect

import android.app.Application
import android.content.Context
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WalletConnectInitializer - Bootstraps the WalletConnect SDK exactly once
 * per application process.
 *
 * Must be called before any other WalletKit or CoreClient API is used.
 * The [init] function is idempotent — repeated calls after the first
 * successful initialization are silently ignored via the [isInitialized]
 * atomic guard.
 *
 * Initialization order mandated by the SDK:
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
     * Atomic guard that prevents concurrent or repeated initialization.
     *
     * [AtomicBoolean.compareAndSet] flips the flag from `false` → `true`
     * and returns `true` only for the first caller — all subsequent calls
     * (including concurrent ones from different threads) see the flag already
     * set and skip the initialization block entirely. This is safer than a
     * plain `Boolean` because `compareAndSet` is a single atomic CPU
     * instruction, eliminating the read-check-write race that a regular
     * `if (bool)` check would have.
     */
    private val isInitialized = AtomicBoolean(false)

    /**
     * Performs the one-time SDK bootstrap. Subsequent calls are no-ops.
     *
     * Should be called as early as possible — ideally from the ViewModel's
     * init block or from [Application.onCreate] — so the relay WebSocket
     * connection is established before the user interacts with the screen.
     */
    fun init() {
        // compareAndSet(false, true) atomically flips the flag and returns true
        // only on the first call — all subsequent or concurrent calls short-circuit here.
        if (isInitialized.compareAndSet(false, true)) {
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
                    // Log relay initialization errors — in production, send to Crashlytics
                    Log.e("WC_INIT", "CoreClient Error", e.throwable)
                }
            )

            // Step 2 — Initialise WalletKit on top of the already-configured CoreClient
            val initParams = Wallet.Params.Init(core = CoreClient)
            WalletKit.initialize(initParams) { error ->
                // Called on the background thread if initialization fails.
                // In production, log this to Crashlytics and surface a user-facing error.
                Log.e("WC_INIT", "WalletKit Init Error: ${error.throwable.message}")
            }

            // Step 3 — Register the delegate AFTER WalletKit.initialize so the SDK
            // is fully ready to route callbacks. Registering before initialization
            // would cause the delegate to receive no events.
            WalletKit.setWalletDelegate(walletManager)
        }
    }
}