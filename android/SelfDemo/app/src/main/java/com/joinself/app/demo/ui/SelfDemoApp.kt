package com.joinself.app.demo.ui

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

sealed class MainRoute {
    @Serializable object Initializing
    @Serializable object Registration
    @Serializable object ConnectToServer
    @Serializable object ConnectingToServer
    @Serializable object ServerConnectionReady
    @Serializable object AuthStart
    @Serializable object AuthResult
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

    NavHost(
        navController = navController,
        startDestination = MainRoute.Initializing,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {

        composable<MainRoute.Initializing> {
            InitializeSDKScreen(
                isLoading = true,
                errorMessage = null,
                onRetry = {

                }
            )

            LaunchedEffect(Unit) {
                delay(3000)
                navController.navigate(MainRoute.Registration)
            }
        }

        composable<MainRoute.Registration> {
            RegistrationIntroScreen(
                onStartRegistration = {
//                    navController.navigate("livenessRoute")
                    navController.navigate(MainRoute.ConnectToServer)
                },
                onOpenSettings = onOpenSettings
            )
        }

        composable<MainRoute.ConnectToServer> {
            ServerConnectStartScreen(
                onContinue = { address ->
                    navController.navigate(MainRoute.ConnectingToServer)
                }
            )
        }
        composable<MainRoute.ConnectingToServer> {
            ServerConnectResultScreen(
                serverAddress =  "",
                isConnecting = false,
                connectionSuccess = true,
                onContinue = {
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
                    navController.navigate(MainRoute.AuthStart)
                },
                onVerifyCredentials = {

                },
                onProvideCredentials = {

                },
                onSignDocuments = {

                }
            )
        }

        composable<MainRoute.AuthStart> {
            AuthStartScreen(
                onStartAuthentication = {
                    navController.navigate("livenessRoute")
                }
            )
        }
        composable<MainRoute.AuthResult> {
            AuthResultScreen(
                isSuccess = true,
                onContinue = {

                }
            )
        }


        // add liveness check to main navigation
        addLivenessCheckRoute(
            navController, route = "livenessRoute", selfModifier = selfModifier,
            account = { viewModel.account },
            withCredential = true,
            onFinish = { selfie, credentials ->


                // nav back to main
                coroutineScope.launch(Dispatchers.Main) {
                    navController.popBackStack("livenessRoute", true)
                }
            }
        )
    }
}