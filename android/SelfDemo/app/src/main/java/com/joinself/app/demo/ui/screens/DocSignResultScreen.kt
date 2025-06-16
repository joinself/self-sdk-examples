package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DocSignResultScreen(
    isSuccess: Boolean,
    onContinue: () -> Unit,
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
                // Hero Section - Success or Failure
                if (isSuccess) {
                    HeroSection(
                        icon = Icons.Filled.CheckCircle,
                        title = "Document Signed Successfully",
                        subtitle = "Your digital signature has been added to the document."
                    )
                } else {
                    HeroSection(
                        icon = Icons.Filled.Error,
                        title = "Document Signing Rejected",
                        subtitle = "You rejected the document signing request."
                    )
                }
            }

            item {
                // Result details
                if (isSuccess) {
                    InfoCard(
                        icon = Icons.Filled.Verified,
                        title = "Signature Complete",
                        message = "Your cryptographic signature has been successfully applied to the document. The document is now legally enforceable and cannot be modified.",
                        type = AlertType.Success
                    )
                }
            }

            if (isSuccess) {

                item {
                    // Security confirmation
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Security Verification",
                            style = AppFonts.heading,
                            color = AppColors.textPrimary
                        )

                        FeatureRow(
                            icon = Icons.Filled.Security,
                            title = "Tamper-Proof Signature",
                            description = "Your signature is cryptographically secure and cannot be forged"
                        )
                    }
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
                title = "Continue",
                onClick = onContinue
            )
        }
    }
}