package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InitializeSDKScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                // Hero Section
                if (isLoading) {
                    HeroSection(
                        icon = Icons.Filled.CloudSync,
                        title = "Initializing Self SDK",
                        subtitle = "Setting up your Self environment. This may take a few moments on first launch."
                    )
                } else if (errorMessage != null) {
                    HeroSection(
                        icon = Icons.Filled.Error,
                        title = "Initialization Failed",
                        subtitle = "We encountered an issue setting up the Self SDK. Please check your connection and try again."
                    )
                }
            }

            if (isLoading) {
                // Loading content
                item {
                    // Progress steps
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Initialization Steps",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        ProcessStep(
                            number = 1,
                            title = "Loading SDK Components",
                            description = "Initializing cryptographic libraries and security modules"
                        )

                        ProcessStep(
                            number = 2,
                            title = "Setting Up Storage",
                            description = "Creating secure storage for your account credentials"
                        )

                        ProcessStep(
                            number = 3,
                            title = "Establishing Environment",
                            description = "Connecting to Self network and configuring environment"
                        )
                    }
                }

                item {
                    // Information about Self
                    InfoCard(
                        icon = Icons.Filled.CloudSync,
                        title = "Self SDK",
                        message = "Self provides secure, decentralized identity verification. Your biometric data stays on your device while enabling powerful authentication capabilities.",
                        type = AlertType.Info
                    )
                }
            } else if (errorMessage != null) {
                // Error content
                item {
                    AlertCard(
                        title = "Initialization Error",
                        message = errorMessage,
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
                            title = "Check Connection",
                            description = "Ensure you have a stable internet connection"
                        )

                        ProcessStep(
                            number = 2,
                            title = "Restart App",
                            description = "Close and reopen the application to retry initialization"
                        )

                        ProcessStep(
                            number = 3,
                            title = "Check Storage",
                            description = "Ensure the app has sufficient storage space available"
                        )
                    }
                }
            }
        }

        // Fixed Retry Button at Bottom (only show if error)
        if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(AppSpacing.screenPadding)
            ) {
                PrimaryButton(
                    title = "Retry Initialization",
                    onClick = onRetry
                )
            }
        }
    }
} 