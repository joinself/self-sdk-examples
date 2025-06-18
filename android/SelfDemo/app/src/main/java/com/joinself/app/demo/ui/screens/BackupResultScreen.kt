package com.joinself.app.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // General success
import androidx.compose.material.icons.filled.Error // General error
import androidx.compose.material.icons.filled.CloudDone // Specific success for backup
import androidx.compose.material.icons.filled.CloudOff // Specific error for backup
import androidx.compose.material.icons.filled.Info // For info messages
import androidx.compose.material.icons.filled.Security // For system managed security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.joinself.app.demo.ui.theme.* // Assuming you have these defined

// Re-using the BackupResult enum from the previous example
 enum class BackupResult {
     Success,
     Failure
 }

@Composable
fun BackupResultScreen(
    backupResult: BackupResult,
    onContinue: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSuccess = backupResult == BackupResult.Success

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
                // Hero Section - Success or Failure
                if (isSuccess) {
                    HeroSection(
                        icon = Icons.Filled.CloudDone,
                        title = "Backup Successful",
                        subtitle = "Your account data has been securely backed up by the Self system."
                    )
                } else {
                    HeroSection(
                        icon = Icons.Filled.CloudOff,
                        title = "Backup Failed",
                        subtitle = "We encountered an issue while trying to back up your account."
                    )
                }
            }

            item {
                // Result details card
                if (isSuccess) {
                    InfoCard(
                        icon = Icons.Filled.CheckCircle,
                        title = "Backup Complete & Secured",
                        message = "Your information is now safely stored and managed by the Self system. You can restore your account through identity verification if needed.",
                        type = AlertType.Success
                    )
                } else {
                    AlertCard(
                        title = "Error Details",
                        message = "The backup process could not be completed. Please check your internet connection and try again. If the problem persists, contact support.",
                        type = AlertType.Error
                    )
                }
            }

            // Optional: Add more sections if needed
            if (isSuccess) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Backup Details",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )
                        ProcessStep(
                            number = 1,
                            title = "System-Managed Recovery",
                            description = "The Self system has secured your backup. Account recovery will involve identity verification."
                        )
                        ProcessStep(
                            number = 2, // You can keep numbering for other steps if desired
                            title = "Data Encrypted",
                            description = "Your account data was encrypted for security."
                        )
                        ProcessStep(
                            number = 3,
                            title = "Secure Upload",
                            description = "Encrypted data was uploaded and stored securely."
                        )
                    }
                }
            } else {
                // Troubleshooting for failed backup
                item {
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
                            title = "Check Network",
                            description = "Ensure you have a stable internet connection."
                        )
                        // Add other relevant troubleshooting steps
                        if (onRetry != null) {
                            ProcessStep(
                                number = 2,
                                title = "Try Again",
                                description = "Attempt the backup process once more."
                            )
                        }
                    }
                }
            }
        }

        // Fixed Buttons at Bottom
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(AppSpacing.screenPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
        ) {
            PrimaryButton(
                title = if (isSuccess) "Done" else "Continue",
                onClick = onContinue
            )
            if (!isSuccess && onRetry != null) {
                SecondaryButton(
                    title = "Retry Backup",
                    onClick = onRetry
                )
            }
        }
    }
}

// Assume definitions for HeroSection, InfoCard, AlertCard, ProcessStep, PrimaryButton, SecondaryButton, AlertType, AppSpacing, AppFonts, AppColors

// --- PREVIEWS ---
@Preview(showBackground = true, name = "Backup Result Success (System Managed)")
@Composable
fun BackupResultScreenSuccessSystemManagedPreview() {
    BackupResultScreen(
        backupResult = BackupResult.Success,
        onContinue = {}
    )
}

@Preview(showBackground = true, name = "Backup Result Failure (System Managed)")
@Composable
fun BackupResultScreenFailureSystemManagedPreview() {
    BackupResultScreen(
        backupResult = BackupResult.Failure,
        onContinue = {},
        onRetry = {}
    )
}