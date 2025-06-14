//
//  AuthResultScreen.swift
//  ios-client
//

import SwiftUI

struct AuthResultScreen: View {
    @State private var showSuccessToast = true
    let onContinue: () -> Void
    
    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // DEBUG Header
                HStack {
                    Text("DEBUG: AUTH_RESULT")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.white)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color.blue)
                .frame(maxWidth: .infinity)
                
                // Scrollable content
                ScrollView {
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
                                Text("Authentication Successful")
                                    .font(.system(size: 32, weight: .bold))
                                    .foregroundColor(.black)
                                    .multilineTextAlignment(.center)
                                
                                Text("Your identity has been verified successfully. Your biometric credentials were validated by the server.")
                                    .font(.system(size: 16))
                                    .foregroundColor(.gray)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 20)
                            }
                        }
                        
                        // Identity Verified Info Box
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 12) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(.green)
                                
                                Text("Identity Verified")
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(.black)
                                
                                Spacer()
                            }
                            
                            Text("Your liveness check was completed successfully and your credentials have been validated by the server. You can now continue with other actions.")
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
                    }
                    .padding(.bottom, 20) // Space above button
                }
                
                // Fixed button at bottom
                Button(action: {
                    onContinue()
                }) {
                    Text("Continue")
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
            
            // Success Toast Notification
            if showSuccessToast {
                VStack {
                    Spacer()
                    
                    HStack(spacing: 12) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.green)
                        
                        Text("Authentication response sent!")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white)
                        
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                    .background(Color.black.opacity(0.8))
                    .cornerRadius(12)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 80) // Position above Continue button
                }
                .onAppear {
                    // Auto-hide success toast after 4 seconds
                    DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
                        withAnimation(.easeOut(duration: 0.5)) {
                            showSuccessToast = false
                        }
                    }
                }
            }
        }
    }
}



#Preview {
    AuthResultScreen(
        onContinue: {
            print("Preview: Continue pressed")
        }
    )
} 
