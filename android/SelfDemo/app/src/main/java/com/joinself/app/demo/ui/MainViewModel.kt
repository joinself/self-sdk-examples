package com.joinself.app.demo.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joinself.common.CredentialType
import com.joinself.common.Environment
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.ChatMessage
import com.joinself.sdk.models.Claim
import com.joinself.sdk.models.Credential
import com.joinself.sdk.models.CredentialRequest
import com.joinself.sdk.models.CredentialResponse
import com.joinself.sdk.models.ResponseStatus
import com.joinself.sdk.models.VerificationRequest
import com.joinself.sdk.models.VerificationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MainViewModel"


sealed class InitializationState {
    data object None : ServerState()
    data object Loading : InitializationState()
    data object Success : InitializationState()
    data class Error(val message: String) : InitializationState()
}
sealed class ServerState {
    data object None : ServerState()
    data object Connecting : ServerState()
    data object Success : ServerState()
    data class Error(val message: String) : ServerState()
}
sealed class ServerRequestState {
    data object None : ServerRequestState()
    data object RequestSent : ServerRequestState()
    data object RequestReceived : ServerRequestState()
    data class RequestError(val message: String) : ServerRequestState()
    data object ResponseSent : ServerRequestState()
}

sealed class SERVER_REQUESTS {
    companion object {
        val REQUEST_CREDENTIAL_AUTH: String = "REQUEST_CREDENTIAL_AUTH"
        val REQUEST_CREDENTIAL_EMAIL: String = "REQUEST_CREDENTIAL_EMAIL"
        val REQUEST_CREDENTIAL_DOCUMENT: String = "REQUEST_CREDENTIAL_DOCUMENT"
        val REQUEST_DOCUMENT_SIGNING: String = "REQUEST_DOCUMENT_SIGNING"
    }
}

data class AppUiState(
    var isRegistered: Boolean = false,
    var initialization: InitializationState = InitializationState.Loading,
    var serverState: ServerState = ServerState.None,
    var requestState: ServerRequestState = ServerRequestState.None
)

class MainViewModel(context: Context): ViewModel() {
    private val _appUiState = MutableStateFlow(AppUiState())
    val appStateFlow: StateFlow<AppUiState> = _appUiState.asStateFlow()

    val account: Account
    var groupAddress: String = ""
    var serverInboxAddress: String = ""
    var credentialRequest: CredentialRequest? = null
    var verificationRequest: VerificationRequest? = null

    init {
        // init the sdk
        SelfSDK.initialize(
            context,
            pushToken = null,
            log = { Log.d("SelfSDK", it) }
        )

        // the sdk will store data in this directory, make sure it exists.
        val storagePath = File(context.filesDir.absolutePath + "/account1")
        if (!storagePath.exists()) storagePath.mkdirs()

        account = Account.Builder()
            .setContext(context)
            .setEnvironment(Environment.production)
            .setSandbox(true)
            .setStoragePath(storagePath.absolutePath)
            .build()

        _appUiState.update {
            it.copy(
                isRegistered = account.registered(),
            )
        }

        account.setOnStatusListener { status ->
            Log.d(TAG, "initialize status $status")

            _appUiState.update {
                it.copy(
                    initialization = if (status == 0L) InitializationState.Success else InitializationState.Error("can't initialize account"),
                )
            }
        }
        account.setOnRequestListener { msg ->
            when (msg) {
                is CredentialRequest -> {
                    credentialRequest = msg
                    _appUiState.update { it.copy(requestState = ServerRequestState.RequestReceived) }

                    // check if it's a liveness check request, then open Liveness UI flow
//                    if (msg.details().any { it.types().contains(CredentialType.Liveness) && it.subject() == Constants.SUBJECT_SOURCE_IMAGE_HASH }) {
//                        Log.d("Self", "received liveness request")
//                    }

                }
                is VerificationRequest -> {
                    // check the request is agreement, this example will respond automatically to the request
                    // users need to handle msg.proofs() which contains agreement content, to display to user
                    if (msg.types().contains(CredentialType.Agreement)) {
                        verificationRequest = msg
                        _appUiState.update { it.copy(requestState = ServerRequestState.RequestReceived) }
                    }
                }
            }
        }
    }


    fun isRegistered() : Boolean {
        return account.registered()
    }
    suspend fun register(selfie: ByteArray, credentials: List<Credential>): Boolean {
        val success = account.register(selfieImage = selfie, credentials = credentials)

        return success
    }

    suspend fun connect(inboxAddress: String) {
        try {
            _appUiState.update { it.copy(serverState = ServerState.Connecting) }
            serverInboxAddress = inboxAddress

            groupAddress = account.connectWith(serverInboxAddress, info = mapOf())
            if (groupAddress.isNotEmpty()) {
                _appUiState.update { it.copy(serverState = ServerState.Success) }
            } else {
                _appUiState.update { it.copy(serverState = ServerState.Error("failed to connect to server")) }
            }
        } catch (ex: Exception) {
            Log.e("Self", ex.message, ex)
            _appUiState.update { it.copy(serverState = ServerState.Error(ex.message ?: "failed to connect to server")) }
        }
    }

    fun resetState(requestState: ServerRequestState) {
        _appUiState.update { it.copy(requestState = requestState) }
        credentialRequest = null
        verificationRequest = null
    }


    suspend fun notifyServerForRequest(message: String) {
        val chat = ChatMessage.Builder()
            .setToIdentifier(groupAddress)
            .setMessage(message)
            .build()

        // send chat to server
        account.send(chat) { messageId, _ ->
            _appUiState.update { it.copy(requestState = ServerRequestState.RequestSent) }
        }
    }

    fun sendCredentialResponse(credentials: List<Credential>, status: ResponseStatus) {
        if (credentialRequest == null) return

        val credentialResponse = CredentialResponse.Builder()
            .setRequestId(credentialRequest!!.id())
            .setTypes(credentialRequest!!.types())
            .setToIdentifier(credentialRequest!!.toIdentifier())
            .setFromIdentifier(credentialRequest!!.fromIdentifier())
            .setStatus(status)
            .setCredentials(credentials)
            .build()

        viewModelScope.launch(Dispatchers.IO) {
            account.send(credentialResponse) { messageId, _ ->
                _appUiState.update { it.copy(requestState = ServerRequestState.ResponseSent) }
            }
        }
    }

    fun shareCredential(status: ResponseStatus) {
        if (credentialRequest == null) return

        val details = credentialRequest!!.details().map {
            Claim.Builder()
                .setTypes(it.types())
                .setSubject(it.subject())
                .setComparisonOperator(it.comparisonOperator())
                .setValue(it.value())
                .build()
        }
        val storedCredentials = account.lookUpCredentials(details)

        sendCredentialResponse(storedCredentials, status)
    }

    fun sendDocSignResponse(status: ResponseStatus) {
        if (verificationRequest == null) return

        val verificationResponse = VerificationResponse.Builder()
            .setRequestId(verificationRequest!!.id())
            .setTypes(verificationRequest!!.types())
            .setToIdentifier(verificationRequest!!.toIdentifier())
            .setFromIdentifier(verificationRequest!!.fromIdentifier())
            .setStatus(status)
            .build()
        viewModelScope.launch(Dispatchers.IO) {
            account.send(verificationResponse) { messageId, _ ->
                _appUiState.update { it.copy(requestState = ServerRequestState.ResponseSent) }
            }
        }
    }

}