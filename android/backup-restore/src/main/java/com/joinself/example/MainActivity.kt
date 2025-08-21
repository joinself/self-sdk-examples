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
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.Message
import com.joinself.sdk.ui.integrateUIFlows
import com.joinself.sdk.ui.openBackupFlow
import com.joinself.sdk.ui.openRegistrationFlow
import com.joinself.sdk.ui.openRestoreFlow
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // init the sdk
        SelfSDK.initialize(applicationContext,
            log = { Log.d("SelfSDK", it) }
        )

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val navController = rememberNavController()
            val selfModifier = SelfModifier.sdk()

            val account = remember {
                // the sdk will store data in this directory, make sure it exists.
                val storagePath = File(applicationContext.filesDir.absolutePath + "/account1")
                if (!storagePath.exists()) storagePath.mkdirs()
                Account.Builder()
                    .setContext(applicationContext)
                    .setEnvironment(Environment.production)
                    .setSandbox(true)
                    .setStoragePath(storagePath.absolutePath)
                    .setCallbacks(object : Account.Callbacks {
                        override fun onMessage(message: Message) {
                            Log.d("Self", "onMessage: ${message.id()}")
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
            }
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
                            modifier = Modifier.padding(top = 20.dp),
                            onClick = {
                                coroutineScope.launch {
                                    account.openRegistrationFlow { isSuccess, error ->
                                        isRegistered = isSuccess
                                    }
                                }
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Create Account")
                        }

                        Button(
                            modifier = Modifier,
                            onClick = {
                                coroutineScope.launch(Dispatchers.Main) {
                                    account.openBackupFlow(onFinish = { isSuccess, error ->
                                        if (isSuccess) {
                                            coroutineScope.launch(Dispatchers.Main) {
                                                Toast.makeText(applicationContext, "Backup successfully", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    })
                                }
                            },
                            enabled = isRegistered
                        ) {
                            Text(text = "Backup")
                        }

                        Button(
                            modifier = Modifier,
                            onClick = {
                                coroutineScope.launch(Dispatchers.Main) {
                                    account.openRestoreFlow(onFinish = { isSuccess, error ->
                                        if (isSuccess) {
                                            isRegistered = true
                                            coroutineScope.launch(Dispatchers.Main) {
                                                Toast.makeText(applicationContext, "Restore successfully", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    })
                                }
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Restore")
                        }
                    }
                }
            }
        }
    }
}