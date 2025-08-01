package com.joinself.example

import android.os.Bundle
import android.util.Log
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
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.ChatMessage
import com.joinself.sdk.models.Message
import com.joinself.sdk.models.PublicKey
import com.joinself.sdk.models.Receipt
import com.joinself.sdk.ui.integrateUIFlows
import com.joinself.sdk.ui.openQRCodeFlow
import com.joinself.sdk.ui.openRegistrationFlow
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var account: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // init the sdk
        SelfSDK.initialize(applicationContext,
            log = { Log.d("Self", it) }
        )

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val selfModifier = SelfModifier.sdk()

            var groupAddress by remember { mutableStateOf<PublicKey?>(null) }
            val messages = remember { mutableStateListOf<String>() }
            var inputMessage by remember { mutableStateOf("") }

            fun sendChat() {
                requireNotNull(groupAddress)

                // build a chat message
                val chat = ChatMessage.Builder()
                    .setMessage(inputMessage)
                    .build()

                // send chat to server
                coroutineScope.launch(Dispatchers.IO) {
                    val messageId = account.send(toAddress = groupAddress!!, message = chat)
                    messages.add(inputMessage)
                    inputMessage = ""
                }
            }

            // send delivered receipt
            fun sendReceipt(message: ChatMessage) {
                val receipt = Receipt.Builder()
                    .setDelivered(listOf(message.id()))
                    .build()
                coroutineScope.launch(Dispatchers.IO) {
                    account.send(groupAddress!!,receipt)
                }
            }

            // the sdk will store data in this directory, make sure it exists.
            val storagePath = File(applicationContext.filesDir.absolutePath + "/account1")
            if (!storagePath.exists()) storagePath.mkdirs()
            account = Account.Builder()
                .setContext(applicationContext)
                .setEnvironment(Environment.production)
                .setSandbox(true)
                .setStoragePath(storagePath.absolutePath)
                .setCallbacks(object : Account.Callbacks {
                    override fun onMessage(message: Message) {
                        Log.d("Self", "onMessage: ${message.id()}")
                        when (message) {
                            is ChatMessage -> {
                                messages.add(message.message()) // append text to the message list

                                sendReceipt(message) // send delivered receipt
                            }
                            is Receipt -> {
                                println("receipt message")
                            }
                        }
                    }
                    override fun onConnect() {
                        Log.d("Self", "onConnect")
                    }
                    override fun onDisconnect(errorMessage: String?) {
                        Log.d("Self", "onDisconnect: $errorMessage")
                    }
                    override fun onAcknowledgement(id: String) {
                        Log.d("Self", "onAcknowledgement: $id")
                    }
                    override fun onError(id: String, errorMessage: String?) {
                        Log.d("Self", "onError: $errorMessage")
                    }
                })
                .build()
            var isRegistered by remember { mutableStateOf(account.registered()) }

            NavHost(navController = navController,
                startDestination = "main",
                modifier = Modifier.systemBarsPadding(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None }
            ) {
                SelfSDK.integrateUIFlows(this, navController, selfModifier = selfModifier)

                composable("main") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(modifier = Modifier.padding(top = 40.dp), text = "Registered: $isRegistered")
                        Button(
                            onClick = {
                                account.openRegistrationFlow { isSuccess, error ->
                                    isRegistered = isSuccess
                                }
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Create Account")
                        }

                        Button(
                            onClick = {
                                account.openQRCodeFlow(
                                    onFinish = { qrCode, discoverData ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            groupAddress = account.connectWith(qrCode)
                                        }
                                    },
                                    onExit = {}
                                )
                            },
                            enabled = isRegistered
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
                                enabled = groupAddress != null,
                                placeholder = { Text("enter chat message") }
                            )
                            Button(modifier = Modifier.width(80.dp), contentPadding = PaddingValues(0.dp),
                                onClick = {
                                    sendChat()
                                },
                                enabled = isRegistered && groupAddress != null
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
            }
        }
    }
}