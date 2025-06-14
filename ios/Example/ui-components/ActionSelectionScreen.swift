//
//  ActionSelectionScreen.swift
//  ios-client
//

import SwiftUI

enum ActionType {
    case authenticate
    case verifyCredentials
    case provideCredentials
    case signDocuments
}

struct ActionSelectionScreen: View {
    @State private var showSuccessToast: Bool
    
    let onActionSelected: (ActionType) -> Void
    
    init(showConnectionSuccess: Bool = false, onActionSelected: @escaping (ActionType) -> Void) {
        self.onActionSelected = onActionSelected
        self._showSuccessToast = State(initialValue: showConnectionSuccess)
    }
    
    var body: some View {
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
                                    icon: "checkmark.circle.fill",
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

struct ActionCardView: View {
    let icon: String
    let title: String
    let description: String
    let action: () -> Void
    @State private var isPressed = false
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                // Icon
                Image(systemName: icon)
                    .font(.system(size: 28))
                    .foregroundColor(.blue)
                    .frame(width: 40, height: 40)
                
                // Content
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.black)
                        .multilineTextAlignment(.leading)
                    
                    Text(description)
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.leading)
                        .lineLimit(nil)
                }
                
                Spacer()
                
                // Arrow
                Image(systemName: "chevron.right")
                    .font(.system(size: 16))
                    .foregroundColor(.gray)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.white)
                    .shadow(
                        color: Color.black.opacity(0.08),
                        radius: 8,
                        x: 0,
                        y: 2
                    )
                    .shadow(
                        color: Color.black.opacity(0.04),
                        radius: 2,
                        x: 0,
                        y: 1
                    )
            )
            .scaleEffect(isPressed ? 0.98 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: isPressed)
        }
        .buttonStyle(PlainButtonStyle())
        .onTapGesture {
            // Add haptic feedback
            let impactFeedback = UIImpactFeedbackGenerator(style: .light)
            impactFeedback.impactOccurred()
            action()
        }
        .onLongPressGesture(minimumDuration: 0, maximumDistance: .infinity, pressing: { pressing in
            isPressed = pressing
        }, perform: {})
    }
}

#Preview {
    ActionSelectionScreen(
        showConnectionSuccess: true,
        onActionSelected: { actionType in
            print("Preview: Selected action: \(actionType)")
        }
    )
} 
