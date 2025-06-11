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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joinself.common.Constants
import com.joinself.common.CredentialType
import com.joinself.common.Environment
import com.joinself.common.exception.InvalidCredentialException
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.Credential
import com.joinself.sdk.models.CredentialMessage
import com.joinself.sdk.models.CredentialRequest
import com.joinself.sdk.models.CredentialResponse
import com.joinself.sdk.models.ResponseStatus
import com.joinself.sdk.models.VerificationRequest
import com.joinself.sdk.models.VerificationResponse
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.ui.component.HorizontalDialog
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
            log = { Log.d("Self", it) }
        )

        // the sdk will store data in this directory, make sure it exists.
        val storagePath = File(applicationContext.filesDir.absolutePath + "/account1")
        if (!storagePath.exists()) storagePath.mkdirs()

        val account = Account.Builder()
            .setContext(applicationContext)
            .setEnvironment(Environment.production)
            .setSandbox(true)
            .setStoragePath(storagePath.absolutePath)
            .build()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val selfModifier = SelfModifier.sdk()

            var isRegistered by remember { mutableStateOf(account.registered()) }
            var groupAddress by remember { mutableStateOf("") }
            var serverInboxAddress by remember { mutableStateOf("") }

            var statusText by remember { mutableStateOf("") }
            var receivedCredentials = remember { mutableStateListOf<Credential>() }

            // connect with server by an inbox address, a group address is returned.
            fun connect() {
                statusText = ""
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val gAdress = account.connectWith(serverInboxAddress, info = mapOf())
                        if (gAdress.isNotEmpty()) {
                            groupAddress = gAdress

                            statusText = "group address: $gAdress"
                        }
                    } catch (ex: Exception) {
                        Log.e("Self", ex.message, ex)
                        statusText = "wrong server address"
                    }
                }
            }

            var credentialRequest: CredentialRequest? = null
            fun sendCredentialResponse(credentials: List<Credential>) {
                if (credentialRequest == null) return

                val credentialResponse = CredentialResponse.Builder()
                    .setRequestId(credentialRequest!!.id())
                    .setTypes(credentialRequest!!.types())
                    .setToIdentifier(credentialRequest!!.toIdentifier())
                    .setFromIdentifier(credentialRequest!!.fromIdentifier())
                    .setStatus(ResponseStatus.accepted)
                    .setCredentials(credentials)
                    .build()

                coroutineScope.launch(Dispatchers.IO) {
                    account.send(credentialResponse)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Credential response sent.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            val storedCredentials = remember { mutableStateListOf<Credential>() }
            fun refreshClaims() {
                storedCredentials.clear()
                storedCredentials.addAll(account.credentials())
            }

            LaunchedEffect(Unit) {
                account.setOnStatusListener {
                    refreshClaims()
                }

                account.setOnMessageListener { msg ->
                    when (msg) {
                        // receive custom credentials messages
                        is CredentialMessage -> {
                            Log.d("Self", "received credential message")
                            receivedCredentials.addAll(msg.credentials())
                        }
                    }
                }

                account.setOnRequestListener { msg ->
                    when (msg) {
                        is CredentialRequest -> {
                            // check if it's a liveness check request, then open Liveness UI flow
                            if (msg.details().any { it.types().contains(CredentialType.Liveness) && it.subject() == Constants.SUBJECT_SOURCE_IMAGE_HASH }) {
                                Log.d("Self", "received liveness request")
                                credentialRequest = msg

                                coroutineScope.launch(Dispatchers.Main) {
                                    navController.navigate("livenessRoute")
                                }
                            }

                        }
                        is VerificationRequest -> {
                            // check the request is agreement, this example will respond automatically to the request
                            // users need to handle msg.proofs() which contains agreement content, to display to user
                            if (msg.types().contains(CredentialType.Agreement)) {
                                val verificationResponse = VerificationResponse.Builder()
                                    .setRequestId(msg.id())
                                    .setTypes(msg.types())
                                    .setToIdentifier(msg.toIdentifier())
                                    .setFromIdentifier(msg.fromIdentifier())
                                    .setStatus(ResponseStatus.accepted)
                                    .build()

                                coroutineScope.launch(Dispatchers.IO) {
                                    account.send(verificationResponse)

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(applicationContext, "Agreement response sent.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NavHost(navController = navController,
                startDestination = "main",
                modifier = Modifier.systemBarsPadding(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                composable("main") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(modifier = Modifier.padding(top = 40.dp).align(Alignment.CenterHorizontally), text = "Registered: ${isRegistered}")
                        Button(modifier = Modifier.align(Alignment.CenterHorizontally),
                            onClick = {
                                navController.navigate("livenessRoute")
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Create Account")
                        }

                        // connect to server
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(modifier = Modifier.weight(1f), enabled = groupAddress.isEmpty(),
                                value = serverInboxAddress,
                                onValueChange = { serverInboxAddress = it },
                                placeholder = { Text("enter server inbox address") }
                            )
                            Button(
                                modifier = Modifier.width(80.dp), contentPadding = PaddingValues(0.dp),
                                onClick = {
                                    connect()
                                },
                                enabled = isRegistered && serverInboxAddress.isNotEmpty() && groupAddress.isEmpty(),
                            ) {
                                Text(text = "Connect")
                            }
                        }
                        Text(text = statusText)

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            fontWeight = FontWeight.Bold,
                            text = "Credentials on Self Account: ${storedCredentials.size}"
                        )
                        LazyColumn {
                            items(storedCredentials) { cred ->
                                Text(text = "${cred.types()}\n   ${cred.claims().joinToString { "${it.subject()}:${it.value()}\n" }}")
                            }
                        }
                    }

                    if (receivedCredentials.isNotEmpty()) {
                        val credentialString = receivedCredentials.flatMap { it.claims() }.joinToString { "${it.subject()}:${it.value()}\n" }

                        HorizontalDialog(
                            selfModifier = selfModifier,
                            title = "Would you like to store these credentials?",
                            message = credentialString,
                            leftButtonText = "No",
                            rightButtonText = "Yes",
                            onLeft = {
                                receivedCredentials.clear()
                            },
                            onRight = {
                                // store credentials to sdk
                                account.storeCredentials(receivedCredentials)
                                receivedCredentials.clear()

                                refreshClaims()
                            }
                        )
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
                        } else if (credentialRequest != null) {
                            sendCredentialResponse(credentials)
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