package com.joinself.app.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
// Specific icons for restore
import androidx.compose.material.icons.filled.Restore
// import androidx.compose.material.icons.filled.VpnKey // No longer explicitly entered by user
import androidx.compose.material.icons.filled.Info // For informational sections
import androidx.compose.material.icons.filled.SecurityUpdateGood // For successful restore benefits
import androidx.compose.material.icons.filled.VerifiedUser // For liveness/selfie check
import androidx.compose.material.icons.filled.CameraAlt // For selfie capture
import androidx.compose.material.icons.filled.SyncLock // For secure sync/restore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.joinself.app.demo.ui.theme.* // Assuming you have these defined

enum class RestoreScreenState {
    ReadyToStart,
    // Add other states if needed
}

@Composable
fun RestoreStartScreen(
    // restoreScreenState: RestoreScreenState,
    onStartRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // Or your app's background color
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                HeroSection(
                    icon = Icons.Filled.Restore,
                    title = "Restore Your Account",
                    subtitle = "Verify your identity through a liveness check and selfie to securely restore your account data."
                )
            }

            item {
                InfoCard(
                    icon = Icons.Filled.Info,
                    title = "How Account Restoration Works",
                    message = "To ensure security, you'll first complete a liveness check and verify your identity with a selfie. Once confirmed, the Self system will automatically retrieve your encrypted backup.",
                    type = AlertType.Info
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "Restoration Steps",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    ProcessStep(
                        number = 1,
                        title = "Liveness Check",
                        description = "Perform a quick liveness check to confirm you're a real person."
                    )

                    ProcessStep(
                        number = 2,
                        title = "Selfie Verification",
                        description = "Take a selfie. We'll compare it with the selfie from your backup to verify your identity."
                    )

                    ProcessStep(
                        number = 3,
                        title = "Automatic Data Recovery",
                        description = "After successful verification, your encrypted account data will be securely restored by the Self system."
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "What You'll Need",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )
                    ProcessStep(
                        number = 1,
                        title = "This Device & Camera",
                        description = "You'll need access to this device's camera for the liveness check and selfie."
                    )
                    // Removed "Your Recovery Phrase" as user doesn't enter it.
                    ProcessStep(
                        number = 2,
                        title = "Stable Internet Connection",
                        description = "A good connection is needed for verification and to download your data."
                    )
                }
            }


            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "Secure & Reliable",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    FeatureRow(
                        icon = Icons.Filled.SecurityUpdateGood,
                        title = "Enhanced Identity Verification",
                        description = "Liveness and selfie checks ensure only you can access your account."
                    )

                    FeatureRow(
                        icon = Icons.Filled.SyncLock,
                        title = "Automated & Encrypted",
                        description = "Your data remains encrypted and is automatically restored by the Self system after verification."
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(AppSpacing.screenPadding)
                .fillMaxWidth()
        ) {
            PrimaryButton(
                title = "Begin Identity Verification", // Changed button title
                // isDisabled = restoreScreenState != RestoreScreenState.ReadyToStart,
                onClick = onStartRestore
            )
        }
    }
}

// Ensure you have these Composables defined or adapt them:
// @Composable fun HeroSection(icon: ImageVector, title: String, subtitle: String) { /* ... */ }
// @Composable fun InfoCard(icon: ImageVector, title: String, message: String, type: AlertType) { /* ... */ }
// @Composable fun ProcessStep(icon: ImageVector? = null, number: Int? = null, title: String, description: String) { /* ... */ }
// @Composable fun FeatureRow(icon: ImageVector, title: String, description: String) { /* ... */ }
// enum class AlertType { Success, Info, Warning, Error }
// @Composable fun PrimaryButton(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, isDisabled: Boolean = false) { /* ... */ }


// --- PREVIEWS ---
@Preview(showBackground = true, name = "Restore Start Screen (Automated Recovery)")
@Composable
fun RestoreStartScreenAutomatedRecoveryPreview() {
    // YourAppTheme {
    RestoreStartScreen(
        onStartRestore = {}
    )
    // }
}