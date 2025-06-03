package com.joinself.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joinself.common.Environment
import com.joinself.common.exception.InvalidCredentialException
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.ChatMessage
import com.joinself.sdk.models.Receipt
import com.joinself.sdk.ui.adQRCodeRoute
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

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val selfModifier = SelfModifier.sdk()

            var isRegistered by remember { mutableStateOf(account.registered()) }
            var groupAddress by remember { mutableStateOf("") }
            val messages = remember { mutableStateListOf<String>() }
            var inputMessage by remember { mutableStateOf("") }

            fun sendChat() {
                // build a chat message
                val chat = ChatMessage.Builder()
                    .setToIdentifier(groupAddress)
                    .setMessage(inputMessage)
                    .build()

                // send chat to server
                coroutineScope.launch(Dispatchers.IO) {
                    account.send(chat) { messageId, _ ->
                        messages.add(inputMessage)
                        inputMessage = ""
                    }
                }
            }

            LaunchedEffect(Unit) {
                // need to wait for account connected
                account.setOnStatusListener { status ->
                    println("onStatus $status")
                }

                // listen to messages from server
                account.setOnMessageListener { msg ->
                    when (msg) {
                        is ChatMessage -> {
                            messages.add(msg.message()) // append to the message list
                        }
                        is Receipt -> {
                            println("receipt message")
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(modifier = Modifier.padding(top = 40.dp), text = "Registered: ${isRegistered}")
                        Button(
                            onClick = {
                                navController.navigate("livenessRoute")
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Create Account")
                        }

                        Button(
                            onClick = {
                                navController.navigate("qrRoute")
                            },
                            enabled = isRegistered && groupAddress.isEmpty()
                        ) {
                            Text(text = "Scan QRCode")
                        }

                        Text(text = "Group address: $groupAddress")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(modifier = Modifier.weight(1f),
                                value = inputMessage,
                                onValueChange = {
                                    inputMessage = it
                                },
                                enabled = groupAddress.isNotEmpty(),
                                placeholder = { Text("enter chat message") }
                            )
                            Button(modifier = Modifier.width(80.dp), contentPadding = PaddingValues(0.dp),
                                onClick = {
                                    sendChat()
                                },
                                enabled = isRegistered && groupAddress.isNotEmpty()
                            ) {
                                Text(text = "Send")
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
                            items(messages) { msg ->
                                Text(
                                    text = msg,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // add liveness check to main navigation
                addLivenessCheckRoute(navController, route = "livenessRoute", selfModifier = selfModifier,
                    account = { account },
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

                // integrate qrcode flow
                adQRCodeRoute(navController, "qrRoute", selfModifier = selfModifier,
                    onFinish = { qrCodeBytes, _ ->
                        coroutineScope.launch(Dispatchers.IO) {
                            // parse qrcode first and check the correct environment
                            val discoveryData = Account.qrCode(qrCodeBytes)
                            if (discoveryData?.sandbox == false) {
                                return@launch
                            }

                            // then connect with the connection in the qrcode
                            account.connectWith(qrCodeBytes)
                            groupAddress = discoveryData?.address ?: "" // keep address to send data

                            coroutineScope.launch(Dispatchers.Main) {
                                navController.popBackStack("qrRoute", true)
                            }
                        }
                    },
                    onExit = {
                        coroutineScope.launch(Dispatchers.Main) {
                            navController.popBackStack("qrRoute", true)
                        }
                    }
                )
            }
        }
    }
}