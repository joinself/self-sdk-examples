package com.joinself.app.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.joinself.app.demo.ui.RestoreState
import com.joinself.app.demo.ui.theme.AlertCard
import com.joinself.app.demo.ui.theme.AlertType
import com.joinself.app.demo.ui.theme.AppColors
import com.joinself.app.demo.ui.theme.AppFonts
import com.joinself.app.demo.ui.theme.AppSpacing
import com.joinself.app.demo.ui.theme.HeroSection
import com.joinself.app.demo.ui.theme.InfoCard
import com.joinself.app.demo.ui.theme.PrimaryButton
import com.joinself.app.demo.ui.theme.ProcessStep
import com.joinself.app.demo.ui.theme.SecondaryButton

@Composable
fun RestoreResultScreen(
    restoreState: RestoreState,
    onContinue: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSuccess = restoreState == RestoreState.Success

    // Determine messages and icons based on outcome
    val heroIcon = when (restoreState) {
        is RestoreState.Success -> Icons.Filled.SecurityUpdateGood
        is RestoreState.VerificationFailed -> Icons.Filled.VerifiedUser // Or specific liveness/selfie error icon
        is RestoreState.DataRecoveryFailed -> Icons.Filled.SyncProblem
        is RestoreState.Error -> Icons.Filled.Error
        else -> Icons.Filled.Error
    }

    val heroTitle = when (restoreState) {
        is RestoreState.Success -> "Account Restored!"
        is RestoreState.VerificationFailed -> "Identity Verification Failed"
        is RestoreState.DataRecoveryFailed -> "Data Recovery Issue"
        is RestoreState.Error -> "Restoration Failed"
        else -> ""
    }

    val heroSubtitle = when (restoreState) {
        is RestoreState.Success -> "Your account data has been successfully and securely restored."
        is RestoreState.VerificationFailed -> "We couldn't verify your identity at this time. Please ensure good lighting and follow the instructions carefully."
        is RestoreState.DataRecoveryFailed -> "Your identity was verified, but we encountered an issue while trying to restore your account data."
        is RestoreState.Error -> "We encountered an unexpected issue while trying to restore your account."
        else -> ""
    }

    val cardTitle = when (restoreState) {
        is RestoreState.Success -> "Restoration Complete"
        else -> "What Happened?"
    }

    val cardMessage = when (restoreState) {
        is RestoreState.Success -> "You can now access your account with all your previous information. Welcome back!"
        is RestoreState.VerificationFailed -> "The liveness check or selfie verification could not be completed successfully. You can try the verification process again."
        is RestoreState.DataRecoveryFailed -> "Please try again in a few moments. If the problem persists, you may need to contact support or try setting up a new account if no critical data was in the backup."
        is RestoreState.Error -> "Please check your internet connection and try again. If the problem continues, please contact our support team."
        else -> ""
    }

    val cardAlertType = if (isSuccess) AlertType.Success else AlertType.Error

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
                    icon = heroIcon,
                    title = heroTitle,
                    subtitle = heroSubtitle
                )
            }

            item {
                // Result details card
                if (isSuccess) {
                    InfoCard(
                        icon = Icons.Filled.CheckCircle, // Or heroIcon
                        title = cardTitle,
                        message = cardMessage,
                        type = cardAlertType
                    )
                } else {
                    AlertCard(
                        // icon = heroIcon, // Optional: if AlertCard supports an icon
                        title = cardTitle,
                        message = cardMessage,
                        type = cardAlertType
                    )
                }
            }

            // Optional: Further details or steps based on the outcome
            if (!isSuccess) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        Text(
                            text = "Next Steps",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )
                        if (restoreState == RestoreState.VerificationFailed && onRetry != null) {
                            ProcessStep(
                                number = 1,
                                title = "Retry Identity Verification",
                                description = "You can attempt the liveness and selfie check again."
                            )
                        }
                        ProcessStep(
                            number = 2,
                            title = "Check Connection",
                            description = "Ensure you have a stable internet connection."
                        )
                        // Add more troubleshooting or guidance steps here
                    }
                }
            }
        }

        // Fixed Buttons at Bottom
        Column(
            modifier = Modifier
                .background(Color.White) // Match LazyColumn background
                .padding(AppSpacing.screenPadding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
        ) {
            PrimaryButton(
                title = if (isSuccess) "Get Started" else "Continue", // Or "Go to Home", "Close"
                onClick = onContinue
            )
            if (restoreState == RestoreState.VerificationFailed && onRetry != null) {
                SecondaryButton(
                    title = "Retry Verification",
                    onClick = onRetry
                )
            }
            // Add other buttons like "Contact Support" if applicable for certain errors
        }
    }
}

@Preview(showBackground = true, name = "Restore Result Success")
@Composable
fun RestoreResultScreenSuccessPreview() {
    RestoreResultScreen(
        restoreState = RestoreState.Success,
        onContinue = {}
    )
}

@Preview(showBackground = true, name = "Restore Result Verification Failed")
@Composable
fun RestoreResultScreenVerificationFailedPreview() {
    RestoreResultScreen(
        restoreState = RestoreState.VerificationFailed,
        onContinue = {},
        onRetry = {}
    )
}

@Preview(showBackground = true, name = "Restore Result Data Recovery Failed")
@Composable
fun RestoreResultScreenDataRecoveryFailedPreview() {
    RestoreResultScreen(
        restoreState = RestoreState.DataRecoveryFailed,
        onContinue = {}
        // onContactSupport = {} // Example
    )
}