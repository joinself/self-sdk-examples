package com.joinself.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joinself.common.Environment
import com.joinself.common.exception.InvalidCredentialException
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.ChatMessage
import com.joinself.sdk.models.CredentialRequest
import com.joinself.sdk.models.CredentialResponse
import com.joinself.sdk.models.Receipt
import com.joinself.sdk.models.SigningRequest
import com.joinself.sdk.models.SigningResponse
import com.joinself.sdk.models.VerificationRequest
import com.joinself.sdk.models.VerificationResponse
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // init the sdk
        SelfSDK.initialize(applicationContext,
            pushToken = null,
            log = { Log.d("Self", it) }
        )

        // the sdk will store data in this directory, make sure it exists.
        val storagePath = File(applicationContext.filesDir.absolutePath + "/account1")
        if (!storagePath.exists()) storagePath.mkdirs()

        val account = Account.Builder()
            .setContext(applicationContext)
            .setEnvironment(Environment.preview)
            .setSandbox(true)
            .setStoragePath(storagePath.absolutePath)
            .build()

        // listen to callbacks to receive data from the SDK
        account.setOnInfoRequest { key ->
            println("info request $key")
        }
        account.setOnInfoResponse { fromAddress, data ->
        }

        account.setOnStatusListener { status ->
            println("onStatus $status")
        }
        account.setOnRelayConnectListener {
            println("onRelay connectted")
        }

        account.setOnMessageListener { msg ->
            when (msg) {
                is ChatMessage -> println("chat messages")
                is Receipt -> println("receipt message")
            }
        }

        account.setOnRequestListener { msg ->
            when (msg) {
                is CredentialRequest -> println("credential request")
                is VerificationRequest -> println("verification request")
                is SigningRequest -> println("signing request")
            }
        }
        account.setOnResponseListener { msg ->
            when (msg) {
                is CredentialResponse -> println("credential response")
                is VerificationResponse -> println("verification response")
                is SigningResponse -> println("signing response")
            }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val selfModifier = SelfModifier.sdk()

            var isRegistered by remember { mutableStateOf(account.registered()) }

            NavHost(navController = navController,
                startDestination = "main",
                modifier = Modifier.systemBarsPadding(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                composable("main") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(modifier = Modifier.padding(top = 40.dp), text = "Registered: ${isRegistered}")
                        Button(
                            modifier = Modifier.padding(top = 20.dp),
                            onClick = {
                                navController.navigate("livenessRoute")
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Create Account")
                        }
                    }
                }

                // add liveness check to main navigation
                addLivenessCheckRoute(navController, route = "livenessRoute", selfModifier = selfModifier,
                    account = {
                        account
                    },
                    withCredential = true,
                    onFinish = { selfie, credentials ->
                        if (!account.registered()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    if (selfie.isNotEmpty() && credentials.isNotEmpty()) {
                                        val success = account.register(selfieImage = selfie, credentials = credentials)
                                        if (success) {
                                            isRegistered = true
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(applicationContext, "Register account successfully", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } catch (_: InvalidCredentialException) { }
                            }
                        }
                        // nav back to main
                        coroutineScope.launch(Dispatchers.Main) {
                            navController.popBackStack("livenessRoute", true)
                        }
                    }
                )
            }
        }
    }
}