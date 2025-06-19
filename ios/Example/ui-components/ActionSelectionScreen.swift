//
//  ActionSelectionScreen.swift
//  ios-client
//

import SwiftUI

public enum ActionType {
    case authenticate
    case verifyCredentials
    case provideCredentials
    case signDocuments
}

public struct ActionSelectionScreen: View {
    @State private var showSuccessToast: Bool
    
    let onActionSelected: (ActionType) -> Void
    
    public init(showConnectionSuccess: Bool = false, onActionSelected: @escaping (ActionType) -> Void) {
        self.onActionSelected = onActionSelected
        self._showSuccessToast = State(initialValue: showConnectionSuccess)
    }
    
    public var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 0) {
                    // DEBUG Header
                    HStack {
                        Text("DEBUG: ACTION_SELECTION")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white)
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .frame(maxWidth: .infinity)
                    
                    VStack(spacing: 40) {
                        // Shield Icon and Title Section
                        VStack(spacing: 24) {
                            // Shield with Checkmark Icon
                            ZStack {
                                Image(systemName: "shield.fill")
                                    .font(.system(size: 80))
                                    .foregroundColor(.blue)
                                
                                Image(systemName: "checkmark")
                                    .font(.system(size: 32, weight: .bold))
                                    .foregroundColor(.white)
                            }
                            .padding(.top, 40)
                            
                            // Title and Subtitle
                            VStack(spacing: 12) {
                                Text("Server Connection Ready")
                                    .font(.system(size: 32, weight: .bold))
                                    .foregroundColor(.black)
                                    .multilineTextAlignment(.center)
                                
                                Text("Your Self account is connected to the server and ready to use. Choose an action below to get started with secure authentication and verification.")
                                    .font(.system(size: 16))
                                    .foregroundColor(.gray)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 20)
                            }
                        }
                        
                        // Available Actions Section
                        VStack(alignment: .leading, spacing: 24) {
                            HStack {
                                Text("Available Actions")
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(.black)
                                Spacer()
                            }
                            
                            VStack(spacing: 16) {
                                ActionCardView(
                                    icon: "shield.fill",
                                    title: "Authenticate",
                                    description: "Use your biometric credentials to securely log in to services",
                                    action: {
                                        onActionSelected(.authenticate)
                                    }
                                )
                                
                                ActionCardView(
                                    icon: "checkmark.shield.fill",
                                    title: "Verify Credentials",
                                    description: "Verify information about you such as email and government issued ID",
                                    action: {
                                        onActionSelected(.verifyCredentials)
                                    }
                                )
                                
                                ActionCardView(
                                    icon: "square.and.arrow.up",
                                    title: "Provide Credentials",
                                    description: "Securely share verified information about you",
                                    action: {
                                        onActionSelected(.provideCredentials)
                                    }
                                )
                                
                                ActionCardView(
                                    icon: "pencil",
                                    title: "Sign Documents",
                                    description: "Securely review, sign, and share documents",
                                    action: {
                                        onActionSelected(.signDocuments)
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        
                        Spacer(minLength: 100) // Extra space for toast
                    }
                }
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
                        
                        Text("Connected to server successfully!")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white)
                        
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                    .background(Color.black.opacity(0.8))
                    .cornerRadius(12)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
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
    ActionSelectionScreen(
        showConnectionSuccess: false,
        onActionSelected: { actionType in
            print("Preview: Selected action: \(actionType)")
        }
    )
} 
