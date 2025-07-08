package com.joinself.app.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RememberMe
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joinself.app.demo.ui.theme.AlertCard
import com.joinself.app.demo.ui.theme.AlertType
import com.joinself.app.demo.ui.theme.AppColors
import com.joinself.app.demo.ui.theme.AppFonts
import com.joinself.app.demo.ui.theme.AppSpacing
import com.joinself.app.demo.ui.theme.HeroSection
import com.joinself.app.demo.ui.theme.InfoCard
import com.joinself.app.demo.ui.theme.PrimaryButton
import com.joinself.app.demo.ui.theme.ProcessStep
import com.joinself.app.demo.ui.theme.StatusCard

@Composable
fun RegistrationIntroScreen(
    onStartRegistration: () -> Unit,
    onStartRestore: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRequestingPermission by remember { mutableStateOf(false) }
    var showPermissionError by remember { mutableStateOf(false) }
    var cameraPermissionStatus by remember { mutableStateOf("Camera access will be requested") }
    var hasPermissionIssue by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier.padding(AppSpacing.screenPadding)
                .clickable {
                    onStartRestore.invoke()
                },
            text = "Restore Account"
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                // Hero Section
                HeroSection(
                    icon = Icons.Filled.AccountCircle,
                    title = "Register Your Account",
                    subtitle = "Complete a quick liveness check to securely register your Self account."
                )
            }

//            item {
//                // Process Steps Section
//                Column(
//                    verticalArrangement = Arrangement.spacedBy(20.dp)
//                ) {
//                    Text(
//                        text = "What to Expect",
//                        style = AppFonts.heading,
//                        color = AppColors.textPrimary
//                    )
//
//                    Column(
//                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
//                    ) {
//                        ProcessStep(
//                            number = 1,
//                            title = "Camera Access",
//                            description = "We'll ask for camera permission when you start"
//                        )
//
//                        ProcessStep(
//                            number = 2,
//                            title = "Position Your Face",
//                            description = "Look directly at the camera and follow on-screen instructions"
//                        )
//                    }
//                }
//            }

            item {
                // Privacy & Security Info
                InfoCard(
                    icon = Icons.Filled.RememberMe,
                    title = "Liveness Check Required",
                    message = "You will be asked for camera permission when you start. Look directly at the camera and follow the on-screen instructions.",
                    type = AlertType.Info
                )
            }

            // Camera Permission Status (only show if there's an issue)
            if (hasPermissionIssue) {
                item {
                    StatusCard(
                        title = "Camera Permission",
                        status = cameraPermissionStatus,
                        statusColor = AppColors.warning,
                        subtitle = "You can update this in Settings or try again below",
                        icon = Icons.Filled.Camera
                    )
                }
            }

            // Error Display
            if (showPermissionError) {
                item {
                    AlertCard(
                        title = "Camera Access Required",
                        message = "Camera access is required for account registration. Please enable camera permission in Settings to continue.",
                        type = AlertType.Warning,
                        action = {
                            onOpenSettings()
                            showPermissionError = false
                        },
                        actionTitle = "Open Settings"
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
                title = "Start",
                onClick = {
                    isRequestingPermission = true
                    onStartRegistration()
                    isRequestingPermission = false
                },
                isLoading = isRequestingPermission,
                isDisabled = isRequestingPermission
            )
        }
    }
}

@Preview(showBackground = true, name = "Registration Intro Screen")
@Composable
fun RegistrationIntroScreenPreview() {
    RegistrationIntroScreen(
        onStartRegistration = {},
        onStartRestore = {},
        onOpenSettings = {}
    )
}