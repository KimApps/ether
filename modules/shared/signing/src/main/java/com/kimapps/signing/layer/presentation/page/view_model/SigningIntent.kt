package com.kimapps.signing.layer.presentation.page.view_model

import com.kimapps.signing.layer.domain.enums.OperationType

/**
 * SigningIntent - Represents every possible user action or system event
 * that can change the state of the signing screen.
 *
 * Follows the MVI (Model-View-Intent) pattern: the UI never mutates state
 * directly — it sends an intent to [SigningViewModel.onIntent] which decides
 * how the state should change in response.
 *
 * Sealed class guarantees that every intent is handled in the `when`
 * expression inside the ViewModel, preventing silent omissions.
 */
sealed class SigningIntent {

    /**
     * Fired once when the screen first appears.
     * Seeds the ViewModel with the [challenge] string to be signed and the
     * [type] of operation being authorised (e.g. WITHDRAWAL), both of which
     * are passed in as navigation arguments.
     */
    data class OnInit(val challenge: String, val type: OperationType) : SigningIntent()

    /**
     * Fired when the user taps "Sign with Passkey".
     * Triggers biometric / device-credential signing via SignChallengeUseCase.
     */
    object OnSignClicked : SigningIntent()

    /**
     * Fired when the user taps the back arrow / cancel icon in the top bar.
     * Delivers a SigningResultEntity.Cancelled result to SigningCoordinator
     * and closes the screen.
     */
    object OnCancelClicked : SigningIntent()

    /**
     * Fired when the user taps "Connect EOA Wallet".
     * Transitions the UI from the default state to the pairing-input state,
     * revealing the WalletConnect URI text field.
     */
    object OnSignWithWalletClicked : SigningIntent()

    /**
     * Fired on every keystroke in the WalletConnect URI input field.
     * Keeps [SigningState.pairingUri] in sync so the ViewModel always holds
     * the latest value when the user taps "Pair Wallet".
     *
     * @param uri The full current text of the URI input field.
     */
    data class OnPairingUriChanged(val uri: String) : SigningIntent()

    /**
     * Fired when the user taps "Pair Wallet".
     * Passes SigningState.pairingUri to WalletConnectManager.pair() which
     * calls CoreClient.Pairing.pair() and initiates the WalletConnect handshake.
     * On success the session is auto-approved and [SigningState.isWalletConnected]
     * becomes true.
     */
    object OnPairClicked : SigningIntent()

    /**
     * Fired when the user taps "Reject" in the approval dialog.
     * Sends a JSON-RPC error response back to the dApp via WalletKit so the
     * dApp knows the request was explicitly declined, then clears
     * [SigningState.pendingRequest] to dismiss the dialog.
     */
    object RejectWalletSign : SigningIntent()

    /**
     * Fired when the user taps "Approve" in the approval dialog.
     * Generates a mock signature, responds to the dApp via WalletKit,
     * delivers the result to SigningCoordinator, and closes the screen.
     */
    object ApproveWalletSign : SigningIntent()
}