package com.joinself.app.demo.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.joinself.common.Environment
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.Credential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

private const val TAG = "MainViewModel"


sealed class Initialization {
    data object Success : Initialization()
    data class Error(val message: String) : Initialization()
    data object Loading : Initialization()
    data object Empty : Initialization()
}

data class AppUiState(
    var isRegistered: Boolean = false,
    var initialization: Initialization = Initialization.Loading
)

class MainViewModel(context: Context): ViewModel() {
    private val _appUiState = MutableStateFlow(AppUiState())
    val appStateFlow: StateFlow<AppUiState> = _appUiState.asStateFlow()

    val account: Account
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
                    initialization = if (status == 0L) Initialization.Success else Initialization.Error("can't initialize account"),
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






}