package com.joinself.app.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup // Primary icon for backup
import androidx.compose.material.icons.filled.CloudUpload // Alternative or supporting icon
import androidx.compose.material.icons.filled.Info // For informational sections
import androidx.compose.material.icons.filled.Lock // For security information
import androidx.compose.material.icons.filled.Security // For security aspects
import androidx.compose.material.icons.filled.CheckCircle // For features/benefits
import androidx.compose.material.icons.filled.EnhancedEncryption // Icon for system-managed recovery
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.joinself.app.demo.ui.theme.* // Assuming you have these defined

enum class BackupScreenState {
    ReadyToStart,
    LoadingPrerequisites,
    // Other states as needed
}

@Composable
fun BackupStartScreen(
    // backupScreenState: BackupScreenState,
    onStartBackup: () -> Unit,
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
                    icon = Icons.Filled.Backup,
                    title = "Secure Your Account",
                    subtitle = "Create an encrypted backup of your account data, managed by the Self system, to prevent data loss and enable easy recovery."
                )
            }

            item {
                InfoCard(
                    icon = Icons.Filled.Info,
                    title = "What is Account Backup?",
                    message = "Backing up your account creates an encrypted copy of your essential data. The Self system securely manages the recovery mechanisms, allowing you to restore your account on a new device after identity verification.",
                    type = AlertType.Info
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "How Backup Works",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    ProcessStep(
                        number = 1,
                        title = "Prepare Data",
                        description = "Your essential account data is prepared for backup."
                    )

                    ProcessStep(
                        number = 2,
                        title = "Encrypt & Secure",
                        description = "The data is strongly encrypted, and the Self system sets up secure recovery protocols."
                    )

                    ProcessStep(
                        number = 3,
                        title = "Backup Complete",
                        description = "Your encrypted data is backed up. You can restore it later through identity verification."
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "Security & Peace of Mind",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    FeatureRow(
                        icon = Icons.Filled.Lock,
                        title = "End-to-End Encryption",
                        description = "Your backup data is encrypted before it leaves your device and remains encrypted at rest."
                    )

                    FeatureRow(
                        icon = Icons.Filled.Security,
                        title = "System-Managed Recovery",
                        description = "The Self system handles the complexities of secure recovery, ensuring only you can restore after verification."
                    )

                    FeatureRow(
                        icon = Icons.Filled.CheckCircle,
                        title = "Easy Restoration",
                        description = "Restore your account on any supported device through a guided identity verification process."
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
                title = "Start Backup Process",
                // isDisabled = backupScreenState != BackupScreenState.ReadyToStart,
                onClick = onStartBackup
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
@Preview(showBackground = true, name = "Backup Start Screen (System Managed)")
@Composable
fun BackupStartScreenSystemManagedPreview() {
    // YourAppTheme {
    BackupStartScreen(
        onStartBackup = {}
    )
    // }
}