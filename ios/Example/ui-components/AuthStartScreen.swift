//
//  AuthStartScreen.swift
//  ios-client
//

import SwiftUI

public struct AuthStartScreen: View {
    let onStartAuthentication: () -> Void
    
    public init(onStartAuthentication: @escaping () -> Void) {
        self.onStartAuthentication = onStartAuthentication
    }
    
    public var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Text("DEBUG: AUTH_START")
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
                    // Shield Icon and Title Section
                    VStack(spacing: 24) {
                        // Shield Icon
                        Image(systemName: "shield.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.blue)
                            .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Authentication Request")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("The server has requested you to authenticate using your biometric credentials. Complete the liveness check to verify your identity.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                    }
                    
                    // Biometric Verification Info Box
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            Image(systemName: "faceid")
                                .font(.system(size: 24))
                                .foregroundColor(.blue)
                            
                            Text("Biometric Verification Required")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.black)
                            
                            Spacer()
                        }
                        
                        Text("You will be asked to take a selfie to verify your liveness and identity. This process is secure and your biometric data stays on your device.")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                            .lineLimit(nil)
                    }
                    .padding(16)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                            .background(Color.blue.opacity(0.05))
                    )
                    .padding(.horizontal, 20)
                }
                .padding(.bottom, 20) // Space above button
            }
            
            // Fixed button at bottom
            Button(action: {
                onStartAuthentication()
            }) {
                Text("Start Authentication")
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
}



#Preview {
    AuthStartScreen(
        onStartAuthentication: {
            print("Preview: Start Authentication")
        }
    )
} 
