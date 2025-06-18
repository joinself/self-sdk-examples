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

// Enum to represent the different outcomes of the restore process
enum class RestoreOutcome {
    Success, // Identity verified, data restored
    VerificationFailed, // Liveness or Selfie check failed
    DataRecoveryFailed, // Verification passed, but system couldn't restore data
    GenericFailure // Other types of errors
}

@Composable
fun RestoreResultScreen(
    restoreOutcome: RestoreOutcome,
    onContinue: () -> Unit,
    onRetryIdentityVerification: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSuccess = restoreOutcome == RestoreOutcome.Success

    // Determine messages and icons based on outcome
    val heroIcon = when (restoreOutcome) {
        RestoreOutcome.Success -> Icons.Filled.SecurityUpdateGood
        RestoreOutcome.VerificationFailed -> Icons.Filled.VerifiedUser // Or specific liveness/selfie error icon
        RestoreOutcome.DataRecoveryFailed -> Icons.Filled.SyncProblem
        RestoreOutcome.GenericFailure -> Icons.Filled.Error
    }

    val heroTitle = when (restoreOutcome) {
        RestoreOutcome.Success -> "Account Restored!"
        RestoreOutcome.VerificationFailed -> "Identity Verification Failed"
        RestoreOutcome.DataRecoveryFailed -> "Data Recovery Issue"
        RestoreOutcome.GenericFailure -> "Restoration Failed"
    }

    val heroSubtitle = when (restoreOutcome) {
        RestoreOutcome.Success -> "Your account data has been successfully and securely restored."
        RestoreOutcome.VerificationFailed -> "We couldn't verify your identity at this time. Please ensure good lighting and follow the instructions carefully."
        RestoreOutcome.DataRecoveryFailed -> "Your identity was verified, but we encountered an issue while trying to restore your account data."
        RestoreOutcome.GenericFailure -> "We encountered an unexpected issue while trying to restore your account."
    }

    val cardTitle = when (restoreOutcome) {
        RestoreOutcome.Success -> "Restoration Complete"
        else -> "What Happened?"
    }

    val cardMessage = when (restoreOutcome) {
        RestoreOutcome.Success -> "You can now access your account with all your previous information. Welcome back!"
        RestoreOutcome.VerificationFailed -> "The liveness check or selfie verification could not be completed successfully. You can try the verification process again."
        RestoreOutcome.DataRecoveryFailed -> "Please try again in a few moments. If the problem persists, you may need to contact support or try setting up a new account if no critical data was in the backup."
        RestoreOutcome.GenericFailure -> "Please check your internet connection and try again. If the problem continues, please contact our support team."
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
                        if (restoreOutcome == RestoreOutcome.VerificationFailed && onRetryIdentityVerification != null) {
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
            if (restoreOutcome == RestoreOutcome.VerificationFailed && onRetryIdentityVerification != null) {
                SecondaryButton(
                    title = "Retry Verification",
                    onClick = onRetryIdentityVerification
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
        restoreOutcome = RestoreOutcome.Success,
        onContinue = {}
    )
}

@Preview(showBackground = true, name = "Restore Result Verification Failed")
@Composable
fun RestoreResultScreenVerificationFailedPreview() {
    RestoreResultScreen(
        restoreOutcome = RestoreOutcome.VerificationFailed,
        onContinue = {},
        onRetryIdentityVerification = {}
    )
}

@Preview(showBackground = true, name = "Restore Result Data Recovery Failed")
@Composable
fun RestoreResultScreenDataRecoveryFailedPreview() {
    RestoreResultScreen(
        restoreOutcome = RestoreOutcome.DataRecoveryFailed,
        onContinue = {}
        // onContactSupport = {} // Example
    )
}