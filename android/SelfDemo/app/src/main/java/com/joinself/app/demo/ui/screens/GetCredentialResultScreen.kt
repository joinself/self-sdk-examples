package com.joinself.app.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ErrorOutline
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



@Composable
fun GetCredentialResultScreen(
    isSuccess: Boolean = true,
    credentialName: String = "Digital Credential",
    onContinue: () -> Unit, // Navigate to next screen (e.g., wallet, home)
    onRetry: (() -> Unit)? = null, // Optional: To retry the credential issuance
    modifier: Modifier = Modifier
) {
    val heroIcon = if (isSuccess) Icons.Filled.AssignmentTurnedIn else Icons.Filled.ErrorOutline
    val heroTitle = if (isSuccess) "$credentialName Issued!" else "$credentialName Issuance Failed"
    val successMessage = "Your $credentialName has been successfully issued and added to your wallet."
    val failureMessage = "There was an issue issuing your $credentialName. Please try again or contact support."
    val heroSubtitle = if (isSuccess) successMessage else failureMessage

    val cardTitle = if (isSuccess) "Credential Ready" else "Error Details"
    val cardMessage = if (isSuccess) {
        "You can now use your $credentialName. It has been securely signed by our server and is ready for use."
    } else {
        "We couldn't generate and sign your $credentialName at this time. Please check your connection or try again."
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
                if (isSuccess) {
                    InfoCard(
                        icon = Icons.Filled.VerifiedUser, // Or specific success icon
                        title = cardTitle,
                        message = cardMessage,
                        type = cardAlertType
                    )
                } else {
                    AlertCard( // Assuming AlertCard for errors
                        // icon = Icons.Filled.CloudOff, // Optional: if AlertCard supports icon
                        title = cardTitle,
                        message = cardMessage,
                        type = cardAlertType
                    )
                }
            }

            // Optional: Further details or actions
            if (isSuccess) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)) {
                        Text(
                            text = "Next Steps",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )
                        ProcessStep( // Assuming ProcessStep is a reusable Composable
                            number = 1,
                            title = "View Your Credential",
                            description = "You can find your new $credentialName in your digital wallet or credentials list."
                        )
                    }
                }
            } else { // Failure Case
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)) {
                        Text(
                            text = "Troubleshooting",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )
                        ProcessStep(
                            number = 1,
                            title = "Check Network",
                            description = "Ensure you have a stable internet connection."
                        )
                        if (onRetry != null) {
                            ProcessStep(
                                number = 2,
                                title = "Try Again",
                                description = "You can attempt to get your $credentialName again."
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
                title = if (isSuccess) "Done" else "Continue", // Or "View Wallet", "Close"
                onClick = onContinue
            )
            if (!isSuccess && onRetry != null) {
                SecondaryButton( // Assuming SecondaryButton or equivalent
                    title = "Retry Getting $credentialName",
                    onClick = onRetry
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Get Credential Result Success")
@Composable
fun GetCredentialResultScreenSuccessPreview() {
    // YourAppTheme {
    GetCredentialResultScreen(
        isSuccess = true,
        credentialName = "Custom Credentials",
        onContinue = {}
    )
    // }
}

@Preview(showBackground = true, name = "Get Credential Result Failure")
@Composable
fun GetCredentialResultScreenFailurePreview() {
    // YourAppTheme {
    GetCredentialResultScreen(
        isSuccess = false,
        credentialName = "Proof of Age",
        onContinue = {},
        onRetry = {}
    )
    // }
}

@Preview(showBackground = true, name = "Get Credential Result Failure No Retry")
@Composable
fun GetCredentialResultScreenFailureNoRetryPreview() {
    // YourAppTheme {
    GetCredentialResultScreen(
        isSuccess = false,
        credentialName = "Access Pass",
        onContinue = {}
        // onRetry is null
    )
    // }
}