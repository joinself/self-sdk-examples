package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joinself.app.demo.ServerRequestState

@Composable
fun DocSignStartScreen(
    requestState: ServerRequestState,
    onSign: () -> Unit,
    onReject: () -> Unit,
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
                    icon = Icons.Filled.PictureAsPdf,
                    title = "PDF Document Signing",
                    subtitle = if(requestState != ServerRequestState.RequestReceived) { "Waiting for a request from server..."} else {"The server has requested you to sign a PDF document. Review the details below and choose whether to sign or reject."}
                )
            }

            item {
                // Information about the document
                InfoCard(
                    icon = Icons.Filled.PictureAsPdf,
                    title = "PDF Agreement Document",
                    message = "You are being asked to sign a PDF document. This creates a verifiable digital signature using your cryptographic credentials.",
                    type = AlertType.Info
                )
            }

            item {
                // Security information
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    Text(
                        text = "Security & Legal Notice",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    FeatureRow(
                        icon = Icons.Filled.Security,
                        title = "Cryptographic Authentication",
                        description = "Your signature is created using secure cryptographic keys unique to you"
                    )

                    FeatureRow(
                        icon = Icons.Filled.CheckCircle,
                        title = "Non-Repudiation",
                        description = "The signature provides legal proof of your agreement to the terms of the document."
                    )
                }
            }
        }

        // Fixed Action Buttons at Bottom
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
        ) {
            PrimaryButton(
                title = "Sign Document",
                isDisabled = requestState != ServerRequestState.RequestReceived,
                onClick = onSign
            )
            
            SecondaryButton(
                title = "Reject",
                isDisabled = requestState != ServerRequestState.RequestReceived,
                onClick = onReject
            )
        }
    }
}

