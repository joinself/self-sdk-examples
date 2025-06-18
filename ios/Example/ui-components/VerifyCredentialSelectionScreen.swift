//
//  VerifyCredentialSelectionScreen.swift
//  ios-client
//

import SwiftUI

public enum CredentialActionType {
    case emailAddress
    case identityDocument
}

public struct VerifyCredentialSelectionScreen: View {
    @State private var showSuccessToast: Bool
    
    let onActionSelected: (CredentialActionType) -> Void
    let onBack: () -> Void
    
    public init(showConnectionSuccess: Bool = false, onActionSelected: @escaping (CredentialActionType) -> Void, onBack: @escaping () -> Void) {
        self.onActionSelected = onActionSelected
        self.onBack = onBack
        self._showSuccessToast = State(initialValue: showConnectionSuccess)
    }
    
    public var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 0) {
                    // DEBUG Header
                    HStack {
                        Button {
                            onBack()
                        } label: {
                            Image(systemName: "arrow.left")
                                .foregroundStyle(Color.white)
                        }

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
                                Text("Verify Credentials")
                                    .font(.system(size: 32, weight: .bold))
                                    .foregroundColor(.black)
                                    .multilineTextAlignment(.center)
                                
                                Text("Choose the type of credential you want to verify. These verifications help establish trust and prove aspects of your identity.")
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
                                    icon: "envelope",
                                    title: "Verify Email Address",
                                    description: "Verify ownership of your email address",
                                    action: {
                                        onActionSelected(.emailAddress)
                                    }
                                )
                                
                                ActionCardView(
                                    icon: "person.text.rectangle",
                                    title: "Verify Identity Document",
                                    description: "Verify your goverment-issued identity documents",
                                    action: {
                                        onActionSelected(.identityDocument)
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
    VerifyCredentialSelectionScreen(
        showConnectionSuccess: true,
        onActionSelected: { actionType in
            print("Preview: Selected action: \(actionType)")
        }, onBack: {
            
        }
    )
} 
