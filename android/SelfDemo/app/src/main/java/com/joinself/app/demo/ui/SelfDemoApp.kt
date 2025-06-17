package com.joinself.app.demo.ui

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joinself.app.demo.ui.screens.AuthRequestResultScreen
import com.joinself.app.demo.ui.screens.AuthRequestStartScreen
import com.joinself.app.demo.ui.screens.DocSignResultScreen
import com.joinself.app.demo.ui.screens.DocSignStartScreen
import com.joinself.app.demo.ui.screens.InitializeSDKScreen
import com.joinself.app.demo.ui.screens.RegistrationIntroScreen
import com.joinself.app.demo.ui.screens.SelectActionScreen
import com.joinself.app.demo.ui.screens.ServerConnectResultScreen
import com.joinself.app.demo.ui.screens.ServerConnectStartScreen
import com.joinself.app.demo.ui.screens.ShareCredentialApprovalScreen
import com.joinself.app.demo.ui.screens.ShareCredentialResultScreen
import com.joinself.app.demo.ui.screens.ShareCredentialSelectionScreen
import com.joinself.app.demo.ui.screens.VerifyDocumentResultScreen
import com.joinself.app.demo.ui.screens.VerifyDocumentStartScreen
import com.joinself.app.demo.ui.screens.VerifyEmailResultScreen
import com.joinself.app.demo.ui.screens.VerifyEmailStartScreen
import com.joinself.app.demo.ui.screens.VerifySelectionScreen
import com.joinself.common.CredentialType
import com.joinself.common.exception.InvalidCredentialException
import com.joinself.sdk.models.ResponseStatus
import com.joinself.sdk.ui.addDocumentVerificationRoute
import com.joinself.sdk.ui.addEmailRoute
import com.joinself.sdk.ui.addLivenessCheckRoute
import com.joinself.sdk.utils.popAllBackStacks
import com.joinself.ui.theme.SelfModifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    @Serializable object ShareCredentialSelection
    @Serializable object ShareCredentialApproval
    @Serializable object ShareCredentialResult
    @Serializable object DocumentSignStart
    @Serializable object DocumentSignResult

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

    var credentialType by remember { mutableStateOf("") }
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
                    navController.popBackStack()
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
                    navController.navigate(MainRoute.ShareCredentialSelection)
                },
                onSignDocuments = {
                    navController.navigate(MainRoute.DocumentSignStart)
                }
            )
            LaunchedEffect(Unit) {
                viewModel.resetState(ServerRequestState.None)
            }
        }

        composable<MainRoute.AuthRequestStart> {
            AuthRequestStartScreen(
                requestState = appState.requestState,
                onStartAuthentication = {
                    navController.navigate(MainRoute.LivenessRoute)
                }
            )

            LaunchedEffect(appState.requestState) {
                Log.d(TAG, "auth request state: ${appState.requestState}")
                when (appState.requestState) {
                    ServerRequestState.None -> {
                        withContext(Dispatchers.IO){
                            viewModel.notifyServerForRequest(SERVER_REQUESTS.REQUEST_CREDENTIAL_AUTH)
                        }
                    }
                    ServerRequestState.ResponseSent -> {
                        withContext(Dispatchers.Main){
                            navController.navigate(MainRoute.AuthResultResult)
                        }
                    }
                    else -> {

                    }
                }
            }
        }
        composable<MainRoute.AuthResultResult> {
            AuthRequestResultScreen(
                requestState = appState.requestState,
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
        composable<MainRoute.ShareCredentialSelection> {
            ShareCredentialSelectionScreen(
                onProvideEmail = {
                    credentialType = CredentialType.Email
                    navController.navigate(MainRoute.ShareCredentialApproval)
                },
                onProvideDocument = {
                    credentialType = CredentialType.Document
                    navController.navigate(MainRoute.ShareCredentialApproval)
                },
                onBack = {

                }
            )
        }
        composable<MainRoute.ShareCredentialApproval> {
            ShareCredentialApprovalScreen(
                credentialType = credentialType,
                requestState = appState.requestState,
                onApprove = {
                    viewModel.shareCredential(status = ResponseStatus.accepted)
                },
                onDeny = {
                    viewModel.shareCredential(status = ResponseStatus.rejected)
                }
            )
            LaunchedEffect(appState.requestState) {
                Log.d(TAG, "credential request state: ${appState.requestState}")
                when (appState.requestState) {
                    ServerRequestState.None -> {
                        withContext(Dispatchers.IO) {
                            if (credentialType == CredentialType.Email) viewModel.notifyServerForRequest(SERVER_REQUESTS.REQUEST_CREDENTIAL_EMAIL)
                            else if (credentialType == CredentialType.Document) viewModel.notifyServerForRequest(SERVER_REQUESTS.REQUEST_CREDENTIAL_DOCUMENT)
                        }
                    }
                    ServerRequestState.ResponseSent -> {
                        withContext(Dispatchers.Main) {
                            navController.navigate(MainRoute.ShareCredentialResult)
                        }
                    }
                    else -> {

                    }
                }
            }
        }
        composable<MainRoute.ShareCredentialResult> {
            ShareCredentialResultScreen(
                requestState = appState.requestState,
                credentialType = credentialType,
                onContinue = {
                    navController.popBackStack(MainRoute.ServerConnectionReady, inclusive = false)
                }
            )
        }
        composable<MainRoute.DocumentSignStart> {
            DocSignStartScreen(
                requestState = appState.requestState,
                onSign = {
                    viewModel.sendDocSignResponse(status = ResponseStatus.accepted)
                },
                onReject = {
                    viewModel.sendDocSignResponse(status = ResponseStatus.rejected)
                }
            )
            LaunchedEffect(appState.requestState) {
                Log.d(TAG, "docsign request state: ${appState.requestState}")
                when (appState.requestState) {
                    ServerRequestState.None -> {
                        withContext(Dispatchers.IO){
                            viewModel.notifyServerForRequest(SERVER_REQUESTS.REQUEST_DOCUMENT_SIGNING)
                        }
                    }
                    ServerRequestState.ResponseSent -> {
                        withContext(Dispatchers.Main){
                            navController.navigate(MainRoute.DocumentSignResult)
                        }
                    }
                    else -> {

                    }
                }
            }
        }
        composable<MainRoute.DocumentSignResult> {
            DocSignResultScreen(
                requestState = appState.requestState,
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
                } else {
                    viewModel.sendCredentialResponse(credentials, status = ResponseStatus.accepted)
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