package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.joinself.app.demo.ui.ServerRequestState


@Composable
fun AuthRequestStartScreen(
    requestState: ServerRequestState,
    onStartAuthentication: () -> Unit,
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
                HeroSection(
                    icon = Icons.Filled.Security,
                    title = "Authentication Request",
                    subtitle = if(requestState != ServerRequestState.Received) "Waiting for a request from server..." else "The server has requested you to authenticate using your biometric credentials. Complete the liveness check to verify your identity."
                )
            }

            item {
                // Information about the process
                InfoCard(
                    icon = Icons.Filled.FaceRetouchingNatural,
                    title = "Biometric Verification Required",
                    message = "You will be asked to take a selfie to verify your liveness and identity. This process is secure and your biometric data stays on your device.",
                    type = AlertType.Info
                )
            }

            item {
                // Process steps
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "Authentication Process",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    ProcessStep(
                        number = 1,
                        title = "Camera Access",
                        description = "Grant camera permission for biometric capture"
                    )

                    ProcessStep(
                        number = 2,
                        title = "Liveness Check",
                        description = "Follow on-screen instructions for facial verification"
                    )

                    ProcessStep(
                        number = 3,
                        title = "Credential Generation",
                        description = "Secure credentials will be generated and sent to the server"
                    )
                }
            }

            item {
                // Security information
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "Security & Privacy",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    FeatureRow(
                        icon = Icons.Filled.Security,
                        title = "Zero-Knowledge Verification",
                        description = "Your biometric data never leaves your device"
                    )

                    FeatureRow(
                        icon = Icons.Filled.CheckCircle,
                        title = "Encrypted Communication",
                        description = "All data is encrypted end-to-end during transmission"
                    )
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
                title = "Start Authentication",
                isDisabled = requestState != ServerRequestState.Received,
                onClick = onStartAuthentication
            )
        }
    }
} 