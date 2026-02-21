package com.kimapps.signing.layer.presentation.page.view_model

/**
 * SigningEffect - One-time side effects emitted by [SigningViewModel].
 *
 * Effects are delivered through a [kotlinx.coroutines.channels.Channel] so each
 * event is consumed exactly once, regardless of recompositions. This makes them
 * suitable for navigation actions and UI events that must not be replayed when
 * the screen re-subscribes to the flow.
 *
 * Sealed class guarantees that every possible effect is handled in the
 * `when` expression inside the `LaunchedEffect` collector in SigningPage.
 */
sealed class SigningEffect {

    /**
     * Instructs the UI to close the signing screen.
     *
     * Emitted in two scenarios:
     * - **Success** – after a signature has been produced (Passkey or WalletConnect)
     *   and the result delivered to SigningCoordinator.
     * - **Cancel** – after the user taps the back / cancel button and a
     *   Cancelled result has been delivered to SigningCoordinator.
     */
    object Close : SigningEffect()
}