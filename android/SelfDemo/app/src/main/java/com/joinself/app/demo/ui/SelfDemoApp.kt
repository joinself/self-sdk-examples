package com.joinself.app.demo.ui

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joinself.app.demo.ui.screens.AuthResultScreen
import com.joinself.app.demo.ui.screens.AuthStartScreen
import com.joinself.app.demo.ui.screens.InitializeSDKScreen
import com.joinself.app.demo.ui.screens.RegistrationIntroScreen
import com.joinself.app.demo.ui.screens.SelectActionScreen
import com.joinself.app.demo.ui.screens.ServerConnectResultScreen
import com.joinself.app.demo.ui.screens.ServerConnectStartScreen
import com.joinself.app.demo.ui.screens.VerifyDocumentResultScreen
import com.joinself.app.demo.ui.screens.VerifyDocumentStartScreen
import com.joinself.app.demo.ui.screens.VerifyEmailResultScreen
import com.joinself.app.demo.ui.screens.VerifyEmailStartScreen
import com.joinself.app.demo.ui.screens.VerifySelectionScreen
import com.joinself.common.exception.InvalidCredentialException
import com.joinself.sdk.ui.addDocumentVerificationRoute
import com.joinself.sdk.ui.addEmailRoute
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.sdk.utils.popAllBackStacks
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


private const val TAG = "SelfDemoApp"

sealed class MainRoute {
    @Serializable object Initializing
    @Serializable object Registration
    @Serializable object ConnectToServer
    @Serializable object ConnectingToServer
    @Serializable object ServerConnectionReady
    @Serializable object AuthRequestStart
    @Serializable object AuthResultResult
    @Serializable object VerifySelection
    @Serializable object VerifyEmailStart
    @Serializable object VerifyEmailResult
    @Serializable object VerifyDocumentStart
    @Serializable object VerifyDocumentResult

    companion object {
        val LivenessRoute = "livenessRoute"
        val EmailRoute = "emailRoute"
        val DocumentRoute = "documentRoute"
        val QRCodeRoute = "qrCodeRoute"
    }
}

@Composable
fun SelfDemoApp(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val selfModifier = SelfModifier.sdk()

    val viewModel: MainViewModel = viewModel {
        MainViewModel(context)
    }

    val appState by viewModel.appStateFlow.collectAsState()

    NavHost(
        navController = navController,
        startDestination = MainRoute.Initializing,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {

        composable<MainRoute.Initializing> {
            InitializeSDKScreen(
                initialization = appState.initialization,
                onRetry = {

                }
            )

            LaunchedEffect(appState.initialization) {
                when (val status = appState.initialization) {
                    is InitializationState.Success -> {
                        val route = if (viewModel.isRegistered()) MainRoute.ConnectToServer else MainRoute.Registration
                        navController.navigate(route)
                    }
                    is InitializationState.Error -> {

                    }
                    else -> {}
                }
            }


        }

        composable<MainRoute.Registration> {
            RegistrationIntroScreen(
                onStartRegistration = {
                    navController.navigate(MainRoute.LivenessRoute)
                },
                onOpenSettings = onOpenSettings
            )
        }

        composable<MainRoute.ConnectToServer> {
            ServerConnectStartScreen(
                onContinue = { address ->
                    coroutineScope.launch(Dispatchers.IO) {
                        viewModel.connect(inboxAddress = address)
                    }
                    navController.navigate(MainRoute.ConnectingToServer)
                }
            )
        }
        composable<MainRoute.ConnectingToServer> {
            ServerConnectResultScreen(
                serverAddress = viewModel.serverInboxAddress,
                serverState = appState.serverState,
                onContinue = {
                    navController.popAllBackStacks()
                    navController.navigate(MainRoute.ServerConnectionReady)
                },
                onRetry = {

                },
                onTimeout = {

                }
            )
        }

        composable<MainRoute.ServerConnectionReady> {
            SelectActionScreen(
                onAuthenticate = {
                    navController.navigate(MainRoute.AuthRequestStart)
                },
                onVerifyCredentials = {
                    navController.navigate(MainRoute.VerifySelection)
                },
                onProvideCredentials = {

                },
                onSignDocuments = {

                }
            )
        }

        composable<MainRoute.AuthRequestStart> {
            AuthStartScreen(
                onStartAuthentication = {
//                    navController.navigate(MainRoute.LivenessRoute)
                    navController.navigate(MainRoute.AuthResultResult)
                }
            )
        }
        composable<MainRoute.AuthResultResult> {
            AuthResultScreen(
                isSuccess = true,
                onContinue = {
                    navController.popBackStack(MainRoute.ServerConnectionReady, inclusive = false)
                }
            )
        }

        composable<MainRoute.VerifySelection> {
            VerifySelectionScreen(
                onVerifyIdentityDocument = {
                    navController.navigate(MainRoute.VerifyDocumentStart)
                },
                onVerifyEmail = {
                    navController.navigate(MainRoute.VerifyEmailStart)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<MainRoute.VerifyEmailStart> {
            VerifyEmailStartScreen(
                onStartVerification = {
                    navController.navigate(MainRoute.EmailRoute)
                }
            )
        }
        composable<MainRoute.VerifyEmailResult> {
            VerifyEmailResultScreen(
                isSuccess = true,
                onContinue = {
                    navController.popBackStack(MainRoute.ServerConnectionReady, inclusive = false)
                }
            )
        }
        composable<MainRoute.VerifyDocumentStart> {
            VerifyDocumentStartScreen(
                onStartVerification = {
                    navController.navigate(MainRoute.DocumentRoute)
                }
            )
        }
        composable<MainRoute.VerifyDocumentResult> {
            VerifyDocumentResultScreen(
                isSuccess = true,
                onContinue = {
                    navController.popBackStack(MainRoute.ServerConnectionReady, inclusive = false)
                }
            )
        }

        // add liveness check to main navigation
        addLivenessCheckRoute(navController, route = MainRoute.LivenessRoute, selfModifier = selfModifier,
            account = { viewModel.account },
            withCredential = true,
            onFinish = { selfie, credentials ->
                if (!viewModel.isRegistered()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            if (selfie.isNotEmpty() && credentials.isNotEmpty()) {
                                val success = viewModel.register(selfie = selfie, credentials = credentials)
                                if (success) {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        navController.navigate(MainRoute.ConnectToServer)
                                    }
                                }
                            }
                        } catch (_: InvalidCredentialException) { }
                    }
                }
                // nav back to main
                coroutineScope.launch(Dispatchers.Main) {
                    navController.popBackStack(MainRoute.LivenessRoute, true)
                }
            }
        )

        addEmailRoute(navController, route = MainRoute.EmailRoute, selfModifier = selfModifier,
            account = { viewModel.account },
            onFinish = { isSuccess, error ->
                if (isSuccess) {
                    coroutineScope.launch(Dispatchers.Main) {
                        navController.navigate(MainRoute.VerifyEmailResult)
                    }
                }
            }
        )

        // integrate passport, idcard verification flow
        addDocumentVerificationRoute(navController, route = MainRoute.DocumentRoute, selfModifier = selfModifier,account = { viewModel.account },
            isDevMode = { false }, // true for testing only
            onFinish = { isSuccess, error ->
                if (isSuccess) {
                    coroutineScope.launch(Dispatchers.Main) {
                        navController.navigate(MainRoute.VerifyDocumentResult)
                    }
                }
            }
        )
    }
}