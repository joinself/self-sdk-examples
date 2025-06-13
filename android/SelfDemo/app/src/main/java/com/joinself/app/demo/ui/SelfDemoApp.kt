package com.joinself.app.demo.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joinself.app.demo.ui.screens.InitializeSDKScreen
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

sealed class MainRoute {
    @Serializable object Initializing
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
                Text(modifier = Modifier.padding(top = 40.dp), text = "Registered")
            }
        }

        composable<MainRoute.Initializing> {
            InitializeSDKScreen(
                isLoading = true,
                errorMessage = null,
                onRetry = {

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