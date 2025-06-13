package com.joinself.app.demo.ui.screens
import com.joinself.app.demo.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SelectActionScreen(
    onAuthenticate: () -> Unit,
    onVerifyCredentials: () -> Unit,
    onProvideCredentials: () -> Unit,
    onSignDocuments: () -> Unit,
    modifier: Modifier = Modifier,
    isAuthenticating: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // DEBUG: Screen Name Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.primary.copy(alpha = 0.1f))
                .padding(8.dp)
        ) {
            androidx.compose.material3.Text(
                text = "DEBUG: ACTION_SELECTION",
                style = AppFonts.caption,
                color = AppColors.primary,
                modifier = Modifier.padding(4.dp)
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                // Hero Section
                HeroSection(
                    icon = Icons.Filled.VerifiedUser,
                    title = "Server Connection Ready",
                    subtitle = "Your Self account is connected to the server and ready to use. Choose an action below to get started with secure authentication and verification."
                )
            }



            item {
                // Available actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing)
                ) {
                    androidx.compose.material3.Text(
                        text = "Available Actions",
                        style = AppFonts.heading,
                        color = AppColors.textPrimary
                    )

                    // Action cards
                    ActionCard(
                        icon = Icons.Filled.Security,
                        title = "Authenticate",
                        description = "Use your biometric credentials to securely log in to services",
                        onClick = onAuthenticate
                    )

                    ActionCard(
                        icon = Icons.Filled.VerifiedUser,
                        title = "Verify Credentials",
                        description = "Verify information about you such as email and government issued ID",
                        onClick = onVerifyCredentials
                    )

                    ActionCard(
                        icon = Icons.Filled.Share,
                        title = "Provide Credentials",
                        description = "Securely share verified information about you",
                        onClick = onProvideCredentials
                    )

                    ActionCard(
                        icon = Icons.Filled.Edit,
                        title = "Sign Documents",
                        description = "Securely review, sign, and share documents",
                        onClick = onSignDocuments
                    )


                }
            }


        }
    }
    
    // Full-screen overlay with spinner when authenticating
    if (isAuthenticating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AppColors.primary,
                strokeWidth = 4.dp
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            AppColors.secondary.copy(alpha = 0.3f)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.componentSpacing),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.material3.Text(
                    text = title,
                    style = AppFonts.subheading,
                    color = AppColors.textPrimary
                )
                androidx.compose.material3.Text(
                    text = description,
                    style = AppFonts.body,
                    color = AppColors.textSecondary
                )
            }

            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Go to $title",
                tint = AppColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 