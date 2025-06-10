package com.joinself.example

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.ui.component.LoadingDialog
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
            log = { Log.d("SelfSDK", it) }
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
            var isDisplayProgressDialog by remember { mutableStateOf(false) }

            // save to file system
            var backupByteArray by remember { mutableStateOf(byteArrayOf()) }
            val saveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream")
            ) { uri: Uri? ->
                uri?.let {
                    applicationContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(backupByteArray)
                    }
                }
            }

            // restore: selfie capture, pickup the backup file
            var isRestoreFlow by remember { mutableStateOf(false) }
            var selfieByteArray by remember { mutableStateOf(byteArrayOf()) }
            val pickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val backupBytes = applicationContext.contentResolver.openInputStream(it)?.use { input ->
                        input.readBytes()
                    }
                    if (backupBytes != null && backupBytes.isNotEmpty() && selfieByteArray.isNotEmpty()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                isDisplayProgressDialog = true
                                val credentials = account.restore(backupBytes, selfieByteArray)
                                isRegistered = credentials.isNotEmpty()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(applicationContext, "Restore result: ${credentials.isNotEmpty()}", Toast.LENGTH_LONG).show()
                                }
                            } catch (ex: Exception) {
                                Log.e("SelfSDK", "restore error", ex)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(applicationContext, "Restore failed: ${ex.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            isDisplayProgressDialog = false
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
                            modifier = Modifier.padding(top = 20.dp),
                            onClick = {
                                navController.navigate("livenessRoute")
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Create Account")
                        }

                        Button(
                            modifier = Modifier,
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    backupByteArray = account.backup()

                                    withContext(Dispatchers.Main) {
                                        saveLauncher.launch("self_sdk.backup")
                                    }
                                }
                            },
                            enabled = isRegistered
                        ) {
                            Text(text = "Backup")
                        }

                        Button(
                            modifier = Modifier,
                            onClick = {
                                isRestoreFlow = true
                                navController.navigate("livenessRoute")
                            },
                            enabled = !isRegistered
                        ) {
                            Text(text = "Restore")
                        }

                        if (isDisplayProgressDialog) {
                            LoadingDialog(selfModifier)
                        }
                    }
                }

                // add liveness check to main navigation
                addLivenessCheckRoute(navController, route = "livenessRoute", selfModifier = selfModifier,
                    account = { account },
                    withCredential = true,
                    onFinish = { selfie, credentials ->
                        if (isRestoreFlow) {
                            selfieByteArray = selfie
                            pickerLauncher.launch("application/octet-stream")
                        } else if (!account.registered()) {
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