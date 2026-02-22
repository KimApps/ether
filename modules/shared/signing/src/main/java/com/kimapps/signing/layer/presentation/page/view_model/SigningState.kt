package com.kimapps.signing.layer.presentation.page.view_model

import com.kimapps.signing.layer.domain.enums.OperationType
import com.reown.walletkit.client.Wallet

/**
 * SigningState - Immutable snapshot of the signing screen's UI state.
 *
 * Follows the MVI (Model-View-Intent) pattern: the composable reads from
 * this data class and renders itself entirely based on its values.
 * The ViewModel produces a new copy via [copy] on every state change —
 * no field is ever mutated in place.
 *
 * All fields have sensible defaults so the ViewModel can be constructed
 * with [SigningState()] and then seeded lazily via [SigningIntent.OnInit].
 */
data class SigningState(

    /**
     * The raw challenge string received from the server that must be signed.
     * Starts empty and is populated by [SigningIntent.OnInit].
     * Cleared back to "" after a successful sign so the screen closes cleanly.
     */
    val challenge: String = "",

    /**
     * The type of operation being authorised (e.g. WITHDRAWAL).
     * Used only for display purposes in SigningHeader — does not affect
     * the signing logic itself.
     * Defaults to WITHDRAWAL to match the most common use-case.
     */
    val operationType: OperationType = OperationType.WITHDRAWAL,

    /**
     * True while a signing operation is in progress (Passkey or WalletConnect).
     * When true the action buttons are hidden and a CircularProgressIndicator
     * is shown instead, preventing the user from triggering duplicate requests.
     */
    val isLoading: Boolean = false,

    /**
     * Holds an error message to display below the action buttons.
     * Null when there is no error to show.
     * Set when a signing use-case or WalletConnect call throws an exception.
     */
    val error: String? = null,

    /**
     * Current text value of the WalletConnect URI input field.
     * Updated on every keystroke via [SigningIntent.OnPairingUriChanged].
     * Passed to WalletConnectManager.pair() when the user taps "Pair Wallet".
     */
    val pairingUri: String = "",

    /**
     * Controls visibility of the WalletConnect URI input field and "Pair Wallet" button.
     * Set to true when the user taps "Connect EOA Wallet".
     * Set back to false once pairing has been initiated via [SigningIntent.OnPairClicked].
     */
    val showPairingInput: Boolean = false,

    /**
     * True once WalletConnectManager receives onSessionSettleResponse with a
     * successful result, indicating an active WalletConnect session is established.
     * Switches the WalletConnectSection from the pairing UI to the
     * "Wallet Connected" confirmation card.
     */
    val isWalletConnected: Boolean = false,

    /**
     * Holds the incoming WalletConnect session request sent by the dApp
     * (e.g. a personal_sign call). Non-null value triggers the
     * [SigningApprovalDialog] to be shown over the screen.
     * Cleared back to null after the user approves or rejects the request.
     */
    val pendingRequest: Wallet.Model.SessionRequest? = null,

    /**
     *
     */
    val isAwaitingApprovalFromDapp: Boolean = false,

    /**
     * The EOA wallet address currently connected via WalletConnect.
     * Null when no session is active. Populated from the first account in the
     * settled session's namespace (CAIP-10 address with the "eip155:1:" prefix stripped).
     * Displayed in the UI as "Sign with 0x1234..." so the user can verify
     * which account has been connected before approving a signing request.
     */
    val connectedAddress: String? = null,
)