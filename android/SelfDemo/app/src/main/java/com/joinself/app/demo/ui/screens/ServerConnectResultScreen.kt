package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ServerConnectResultScreen(
    serverAddress: String,
    isConnecting: Boolean,
    connectionSuccess: Boolean?,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Timeout handling
    var hasTimedOut by remember { mutableStateOf(false) }
    
    // Start timeout when connecting begins
    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            hasTimedOut = false
            kotlinx.coroutines.delay(10000) // 10 second timeout
            if (isConnecting && connectionSuccess == null) {
                hasTimedOut = true
                onTimeout()
            }
        }
    }
    
    // Auto-navigate on successful connection
    LaunchedEffect(connectionSuccess) {
        if (connectionSuccess == true) {
            kotlinx.coroutines.delay(1500) // Brief delay to show success state
            onContinue()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // DEBUG: Screen Name Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.primary.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            androidx.compose.material3.Text(
                text = "DEBUG: SERVER_CONNECTION_PROCESSING",
                style = AppFonts.caption,
                color = AppColors.primary,
                modifier = Modifier.padding(4.dp)
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                // Hero Section - different states
                when {
                    isConnecting && !hasTimedOut -> {
                        HeroSection(
                            icon = Icons.Filled.CloudSync,
                            title = "Connecting to Server",
                            subtitle = "Establishing secure connection with the authentication server. Please wait..."
                        )
                    }
                    hasTimedOut -> {
                        HeroSection(
                            icon = Icons.Filled.Error,
                            title = "Connection Timed Out",
                            subtitle = "The connection attempt took too long. Please check your network and try again."
                        )
                    }
                    connectionSuccess == true -> {
                        HeroSection(
                            icon = Icons.Filled.CheckCircle,
                            title = "Server Connected",
                            subtitle = "Your Self account is now connected to the server. Redirecting to actions..."
                        )
                    }
                    connectionSuccess == false -> {
                        HeroSection(
                            icon = Icons.Filled.Error,
                            title = "Connection Failed",
                            subtitle = "We couldn't establish a connection to the server. Please check the address and try again."
                        )
                    }
                }
            }

            // Server address info
            item {
                InfoCard(
                    icon = Icons.Filled.CloudSync,
                    title = "Server Address",
                    message = "Connecting to: ${serverAddress.take(16)}...${serverAddress.takeLast(16)}",
                    type = AlertType.Info
                )
            }

            if (isConnecting && !hasTimedOut) {
                // Connection process steps
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Connection Process",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        ProcessStep(
                            number = 1,
                            title = "Contacting Server",
                            description = "Reaching out to the specified server address"
                        )

                        ProcessStep(
                            number = 2,
                            title = "Secure Handshake",
                            description = "Establishing encrypted connection and verifying server identity"
                        )

                        ProcessStep(
                            number = 3,
                            title = "Account Registration",
                            description = "Registering your Self account with the server"
                        )
                    }
                }
            } else if (connectionSuccess == true) {
                // Success content
                item {
                    InfoCard(
                        icon = Icons.Filled.CheckCircle,
                        title = "Connection Established",
                        message = "Your Self account is registered and connected to the server. All communications are encrypted.",
                        type = AlertType.Success
                    )
                }

                item {
                    // Available features
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Now Available",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        FeatureRow(
                            icon = Icons.Filled.CheckCircle,
                            title = "Secure Authentication",
                            description = "Use your biometric credentials for secure login"
                        )
                    }
                }
            } else if (connectionSuccess == false) {
                // Failure content
                item {
                    AlertCard(
                        title = "Connection Failed",
                        message = "The connection could not be established. This might be due to an incorrect server address, network issues, or server unavailability.",
                        type = AlertType.Error
                    )
                }

                item {
                    // Troubleshooting steps
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Troubleshooting",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        ProcessStep(
                            number = 1,
                            title = "Verify Address",
                            description = "Double-check the 66-character server address is correct"
                        )

                        ProcessStep(
                            number = 2,
                            title = "Check Network",
                            description = "Ensure you have a stable internet connection"
                        )

                        ProcessStep(
                            number = 3,
                            title = "Server Status",
                            description = "The server might be temporarily unavailable"
                        )
                    }
                }
            }
        }

        // Fixed Primary Button at Bottom - only show on failure or timeout
        if (connectionSuccess == false || hasTimedOut) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(AppSpacing.screenPadding)
            ) {
                    PrimaryButton(
                    title = if (hasTimedOut) "Connection Timed Out - Try Again" else "Try Again",
                        onClick = onRetry
                    )
            }
        }
    }
} 