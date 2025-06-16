package com.joinself.app.demo.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.joinself.common.Environment
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.ChatMessage
import com.joinself.sdk.models.Credential
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

data class AppUiState(
    var isRegistered: Boolean = false,
    var initialization: InitializationState = InitializationState.Loading,
    var serverState: ServerState = ServerState.None
)

class MainViewModel(context: Context): ViewModel() {
    private val _appUiState = MutableStateFlow(AppUiState())
    val appStateFlow: StateFlow<AppUiState> = _appUiState.asStateFlow()

    val account: Account
    var groupAddress: String = ""
    var serverInboxAddress: String = ""

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
            _appUiState.update { it.copy(serverState = ServerState.Success) }
        } catch (ex: Exception) {
            Log.e("Self", ex.message, ex)
            _appUiState.update { it.copy(serverState = ServerState.Error(ex.message ?: "failed to connect to server")) }
        }
    }



    suspend fun sendChat(message: String) {
        // build a chat message
        val chat = ChatMessage.Builder()
            .setToIdentifier(groupAddress)
            .setMessage(message)
            .build()

        // send chat to server
        account.send(chat) { messageId, _ ->

        }
    }


}