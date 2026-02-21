package com.kimapps.signing.wallet_connect

import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletConnectManager @Inject constructor(
    private val applicationScope: CoroutineScope,
) : WalletKit.WalletDelegate {

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _sessionRequests = MutableSharedFlow<Wallet.Model.SessionRequest>()
    val sessionRequests = _sessionRequests.asSharedFlow()

    // Called when a Dapp sends a pairing URI (QR Code or deep link)
    fun pair(uri: String) {
        val pairParams = Core.Params.Pair(uri)
        CoreClient.Pairing.pair(pairParams) { error ->
            println("Pairing Error: ${error.throwable.stackTraceToString()}")
        }
    }

    /**
     * Approves a pending session request by responding with the provided signature.
     * Called when the user taps "Approve" on the signing dialog.
     */
    fun approveRequest(request: Wallet.Model.SessionRequest, signature: String) {
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = request.topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                id = request.request.id,
                result = signature
            )
        )
        WalletKit.respondSessionRequest(
            params = response,
            onSuccess = { println("WalletConnect: request approved successfully") },
            onError = { error -> println("WalletConnect: approve error ${error.throwable}") }
        )
    }

    /**
     * Rejects a pending session request.
     * Called when the user taps "Reject" on the signing dialog.
     */
    fun rejectRequest(request: Wallet.Model.SessionRequest) {
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = request.topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                id = request.request.id,
                code = 4001,
                message = "User rejected the request"
            )
        )
        WalletKit.respondSessionRequest(
            params = response,
            onSuccess = { println("WalletConnect: request rejected") },
            onError = { error -> println("WalletConnect: reject error ${error.throwable}") }
        )
    }

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        // Auto-approve with Ethereum mainnet namespace
        val namespaces = mapOf(
            "eip155" to Wallet.Model.Namespace.Session(
                chains = listOf("eip155:1"),
                methods = listOf("personal_sign", "eth_sendTransaction", "eth_signTransaction"),
                events = listOf("accountsChanged", "chainChanged"),
                accounts = listOf("eip155:1:0x57f48fAFeC1d76B27e3f29b8d277b6218CDE6092") // Mock account
            )
        )
        val approveParams = Wallet.Params.SessionApprove(sessionProposal.proposerPublicKey, namespaces)
        WalletKit.approveSession(approveParams) { error ->
            println("Approve Session Error: $error")
        }
    }

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext
    ) {
        // Emit the request so the ViewModel can show an Approve/Reject dialog
        applicationScope.launch {
            _sessionRequests.emit(sessionRequest)
        }
    }

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        _isConnected.value = false
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        _isConnected.value = settleSessionResponse is Wallet.Model.SettledSessionResponse.Result
    }

    override fun onSessionExtend(session: Wallet.Model.Session) {}

    override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {}

    override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
        println("WalletConnect connection state: $state")
    }

    override fun onError(error: Wallet.Model.Error) {
        println("WalletConnect Error: ${error.throwable}")
    }
}