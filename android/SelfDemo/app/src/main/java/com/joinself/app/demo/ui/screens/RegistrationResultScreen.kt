package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationResultScreen(
    isSuccess: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // Ensure white background
    ) {
        // DEBUG: Screen Name Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.primary.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            androidx.compose.material3.Text(
                text = "DEBUG: REGISTRATION_RESULT",
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
                // Hero Section - different for success vs failure
                if (isSuccess) {
                    HeroSection(
                        icon = Icons.Filled.CheckCircle,
                        title = "Registration Successful!",
                        subtitle = "Your Self account has been created successfully. You can now use secure authentication and verification features."
                    )
                } else {
                    HeroSection(
                        icon = Icons.Filled.Error,
                        title = "Registration Failed",
                        subtitle = "We encountered an issue creating your Self account. Please try the registration process again."
                    )
                }
            }

            if (isSuccess) {
                // Success content
                item {
                    // Success status card
                    InfoCard(
                        icon = Icons.Filled.CheckCircle,
                        title = "Account Created",
                        message = "Your biometric credentials have been securely registered with the Self network. Your account is now ready for authentication.",
                        type = AlertType.Success
                    )
                }

                item {
                    // Next steps for successful registration
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "What's Next",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        FeatureRow(
                            icon = Icons.Filled.CheckCircle,
                            title = "Server Connection",
                            description = "Connect to authentication servers to start using your account"
                        )

                        FeatureRow(
                            icon = Icons.Filled.CheckCircle,
                            title = "Secure Authentication",
                            description = "Use your biometric credentials for secure login and verification"
                        )
                    }
                }
            } else {
                // Failure content
                item {
                    // Error information
                    AlertCard(
                        title = "Registration Issue",
                        message = "The registration process could not be completed. This might be due to network connectivity, camera issues, or server problems. Please check your connection and try again.",
                        type = AlertType.Error
                    )
                }

                item {
                    // Troubleshooting steps
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Before Trying Again",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        ProcessStep(
                            number = 1,
                            title = "Check Camera",
                            description = "Ensure camera permission is granted and working properly"
                        )

                        ProcessStep(
                            number = 2,
                            title = "Check Connection",
                            description = "Verify you have a stable internet connection"
                        )

                        ProcessStep(
                            number = 3,
                            title = "Try Again",
                            description = "Return to registration and follow the liveness check steps carefully"
                        )
                    }
                }
            }
        }

        // Fixed Primary Button at Bottom
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(AppSpacing.screenPadding)
        ) {
            PrimaryButton(
                title = if (isSuccess) "Continue to Server Connection" else "Try Registration Again",
                onClick = onContinue
            )
        }
    }
} 