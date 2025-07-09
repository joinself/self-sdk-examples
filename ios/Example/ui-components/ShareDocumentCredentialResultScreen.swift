//
//  ShareDocumentCredentialResultScreen.swift
//  ios-client
//

import SwiftUI

public struct ShareDocumentCredentialResultScreen: View {
    let success: Bool
    let onContinue: () -> Void
    
    public init(success: Bool, onContinue: @escaping () -> Void) {
        self.success = success
        self.onContinue = onContinue
    }
    
    public var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            
            // Scrollable content
            ScrollView {
                VStack(spacing: 40) {
                    if success {
                        // Success content
                        successContent
                    } else {
                        // Rejection content
                        rejectionContent
                    }
                }
                .padding(.bottom, 20) // Space above button
            }
            
            // Fixed button at bottom
            Button(action: {
                onContinue()
            }) {
                Text(success ? "Continue" : "Try Again")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.blue)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
            .background(Color.white)
        }
        .background(Color.white)
    }
    
    // MARK: - Success Content
    
    private var successContent: some View {
        VStack(spacing: 40) {
            // Checkmark Icon and Title Section
            VStack(spacing: 24) {
                // Blue Checkmark Circle
                ZStack {
                    Circle()
                        .fill(Color.blue)
                        .frame(width: 48, height: 48)
                    
                    Image(systemName: "checkmark")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                }
                .padding(.top, 40)
                
                // Title and Subtitle
                VStack(spacing: 12) {
                    Text("Credential Share Successfully")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .multilineTextAlignment(.center)
                    
                    Text("Your have shared your credentials successfully.")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
            }
            
            // Signature Complete Info Box
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.green)
                    
                    Text("Share Credentials Complete")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.black)
                    
                    Spacer()
                }
                
                Text("You have shared your credentials successully.")
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .lineLimit(nil)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.green.opacity(0.3), lineWidth: 1)
                    .background(Color.green.opacity(0.05))
            )
            .padding(.horizontal, 20)
            
            // Security Verification Section
            VStack(alignment: .leading, spacing: 16) {
                Text("Security Verification")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 20)
                
                HStack(spacing: 12) {
                    Image(systemName: "shield.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.green)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Tamper-Proof Signature")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.black)
                        
                        Text("Your signature is cryptographically secure and cannot be forged")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                }
                .padding(.horizontal, 20)
            }
        }
    }
    
    // MARK: - Rejection Content
    
    private var rejectionContent: some View {
        VStack(spacing: 40) {
            // Exclamation Icon and Title Section
            VStack(spacing: 24) {
                // Blue Exclamation Circle
                ZStack {
                    Circle()
                        .fill(Color.blue)
                        .frame(width: 48, height: 48)
                    
                    Image(systemName: "exclamationmark")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                }
                .padding(.top, 40)
                
                // Title and Subtitle
                VStack(spacing: 12) {
                    Text("Credential Share Failed")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .multilineTextAlignment(.center)
                    
                    Text("We were unable to share your credential with the server.")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                    
                    CardView(icon: "exclamationmark.circle", iconColor: .red, borderColor: .red, title: "Sharing Failed", description: "We were unable to share your credential with the server.")
                }
                
                VStack(alignment: .leading, spacing: 24) {
                    HStack {
                        Text("Trouble Shooting Tips")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.black)
                        Spacer()
                    }
                    
                    VStack(spacing: 16) {
                        SimpleCardView(
                            icon: "checkmark.circle.fill",
                            title: "Check Credential",
                            description: "Ensure your have verified credentials on your device",
                            action: {
                                
                            }
                        )
                        
                        SimpleCardView(
                            icon: "shield",
                            title: "Network Connection",
                            description: "Make sure you're still connected to the server",
                            action: {
                                
                            }
                        )
                        
                        SimpleCardView(
                            icon: "exclamationmark.circle",
                            title: "Server Connection",
                            description: "❌ Your actual email address will NOT be shared",
                            action: {
                                
                            })
                    }
                    
                }
                .padding(.horizontal, 20)
                CardView(icon: "exclamationmark.circle", iconColor: .red, borderColor: .red, title: "Credential Sharing", description: "You can try to sharing your credentials again")
            }
        }
    }
}

#Preview {
    VStack {
        ShareDocumentCredentialResultScreen(
            success: true,
            onContinue: {
                print("Preview: Continue (Success)")
            }
        )
        
        ShareDocumentCredentialResultScreen(
            success: false,
            onContinue: {
                print("Preview: Continue (Rejection)")
            }
        )
    }
} 
