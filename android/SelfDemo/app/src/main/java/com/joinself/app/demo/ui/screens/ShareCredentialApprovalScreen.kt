package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.joinself.app.demo.ui.ServerRequestState

@Composable
fun ShareCredentialApprovalScreen(
    credentialType: String, // "email" or "document"
    requestState: ServerRequestState,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, description, icon) = when (credentialType) {
        "email" -> Triple(
            "Share Email Credential?",
            "The server is requesting proof of your verified email address. Your actual email address will not be shared - only cryptographic proof that you own a verified email.",
            Icons.Filled.Security
        )
        "document" -> Triple(
            "Share ID Number?", 
            "The server is requesting proof of your verified ID number. Your personal information will not be shared - only cryptographic proof that you have a verified ID number.",
            Icons.Filled.Security
        )
        else -> Triple(
            "Share Credential?",
            "The server is requesting access to one of your verified credentials.",
            Icons.Filled.Share
        )
    }

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
                    icon = icon,
                    title = title,
                    subtitle = description
                )
            }

            item {
                // Request details
                InfoCard(
                    icon = Icons.Filled.Share,
                    title = "Server Request",
                    message = if(requestState != ServerRequestState.RequestReceived) {"Waiting for a request from server..."} else {"The server has requested access to your verified ${credentialType} credential. This is a secure, privacy-preserving request that doesn't expose your personal information."},
                    type = AlertType.Info
                )
            }

            item {
                // What will be shared section
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "What Will Be Shared",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    FeatureRow(
                        icon = Icons.Filled.CheckCircle,
                        title = "Verification Status",
                        description = "Proof that you have a verified ${credentialType} credential"
                    )

                    FeatureRow(
                        icon = Icons.Filled.Security,
                        title = "Cryptographic Proof",
                        description = "Zero-knowledge proof of credential ownership"
                    )

                    FeatureRow(
                        icon = Icons.Filled.Error,
                        title = "Personal Data",
                        description = "❌ Your actual ${if (credentialType == "email") "email address" else "ID number"} will NOT be shared"
                    )
                }
            }

            item {
                // Privacy note
                InfoCard(
                    icon = Icons.Filled.Security,
                    title = "Privacy Protected",
                    message = "This credential sharing uses advanced cryptography to prove you have verified information without revealing the actual data. The server only learns that you have a valid ${credentialType} credential.",
                    type = AlertType.Success
                )
            }
        }

        // Fixed action buttons at bottom
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
        ) {
            PrimaryButton(
                title = "Approve & Share Credential",
                isDisabled = requestState != ServerRequestState.RequestReceived,
                onClick = onApprove
            )
            
            SecondaryButton(
                title = "Deny Request",
                isDisabled = requestState != ServerRequestState.RequestReceived,
                onClick = onDeny
            )
        }
    }
} 