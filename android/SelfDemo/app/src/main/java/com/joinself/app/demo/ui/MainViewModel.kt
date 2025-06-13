package com.joinself.app.demo.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.joinself.common.Environment
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import java.io.File

class MainViewModel(context: Context): ViewModel() {

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
    }






}