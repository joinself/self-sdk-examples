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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import com.joinself.common.Environment
import com.joinself.common.exception.InvalidCredentialException
import com.joinself.sdk.SelfSDK
import com.joinself.sdk.models.Account
import com.joinself.sdk.models.Claim
import com.joinself.sdk.ui.addDocumentVerificationRoute
import com.joinself.sdk.ui.addEmailRoute
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    val LOGTAG = "Self"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // init the sdk
        SelfSDK.initialize(applicationContext,
            pushToken = null,
            log = { Log.d(LOGTAG, it) }
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
            val claims = remember { mutableStateListOf<Claim>() }

            fun refreshClaims() {
                val credentials = account.credentialsByType()
                claims.clear()
                claims.addAll(credentials.flatMap { cred -> cred.credentials.flatMap { it.claims() } })
            }

            LaunchedEffect(Unit) {
                // need to wait for account connected
                account.setOnStatusListener { status ->
                    if (status == 0L) {
                        refreshClaims()
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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(modifier = Modifier.padding(top = 40.dp), text = "Registered: $isRegistered")
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
                                navController.navigate("livenessRoute")
                            },
                            enabled = isRegistered
                        ) {
                            Text(text = "Liveness Verification")
                        }

                        Button(
                            onClick = {
                                navController.navigate("emailRoute")
                            },
                            enabled = isRegistered
                        ) {
                            Text(text = "Email Verification")
                        }

                        Button(
                            onClick = {
                                navController.navigate("documentRoute")
                            },
                            enabled = isRegistered
                        ) {
                            Text(text = "Document Verification")
                        }

                        // list all verified credentials
                        Text(
                            modifier = Modifier.padding(top = 10.dp),
                            fontWeight = FontWeight.Bold,
                            text = "Credentials on Self Account: ${claims.size}"
                        )
                        LazyColumn {
                            items(claims) { claim ->
                                Text(text = "${claim.subject()}: ${claim.value()}")
                            }
                        }
                    }
                }

                // add liveness check to main navigation
                addLivenessCheckRoute(navController, route = "livenessRoute", selfModifier = selfModifier,
                    account = { account },
                    withCredential = true,
                    onFinish = { selfie, credentials ->
                        // check result selfie image and credentials from server
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
                        } else if (credentials.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Liveness check successfully", Toast.LENGTH_LONG).show()
                            }
                        }
                        // nav back to main
                        coroutineScope.launch(Dispatchers.Main) {
                            navController.popBackStack("livenessRoute", true)
                        }
                    }
                )

                // integrate email verification flow
                addEmailRoute(navController, route = "emailRoute", selfModifier = selfModifier,
                    account = { account },
                    onFinish = { isSuccess, error ->
                        if (isSuccess) {
                            refreshClaims() // refresh email credentials to display

                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Email verification successfully", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                // integrate passport, idcard verification flow
                addDocumentVerificationRoute(navController, route = "documentRoute", selfModifier = selfModifier,account = { account },
                    isDevMode = { false }, // true for testing only
                    onFinish = { isSuccess, error ->
                        if (isSuccess) {
                            refreshClaims() // refresh email credentials to display

                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Document verification successfully", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}