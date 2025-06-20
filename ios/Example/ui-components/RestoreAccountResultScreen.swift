//
//  RestoreAccountResultScreen.swift
//  ios-client
//

import SwiftUI

public struct RestoreAccountResultScreen: View {
    @State private var showingRegistration = false
    @State private var isProcessing = false
    @State private var errorMessage: String? = nil
    let onNext: () -> Void
    let onBack: () -> Void
    public init(showingRegistration: Bool = false, isProcessing: Bool = false, errorMessage: String? = nil, onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.showingRegistration = showingRegistration
        self.isProcessing = isProcessing
        self.errorMessage = errorMessage
        self.onNext = onNext
        self.onBack = onBack
    }
    
    public var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Button {
                    onBack()
                } label: {
                    Image(systemName: "arrow.left")
                        .foregroundStyle(Color.white)
                }
                Text("DEBUG: RESTORATION_INTRO")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.blue)
            .frame(maxWidth: .infinity)
            
            VStack(spacing: 40) {
                // User Icon and Title Section
                VStack(spacing: 24) {
                    // User Icon
                    ZStack {
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 80, height: 80)
                        
                        Image(systemName: "checkmark.rectangle.portrait")
                            .font(.system(size: 40))
                            .foregroundColor(.white)
                    }
                    .padding(.top, 40)
                    
                    // Title and Subtitle
                    VStack(spacing: 12) {
                        Text("Restore Completed")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.black)
                            .multilineTextAlignment(.center)
                        
                        Text("Your account has been successfully and securely restored.")
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }
                    
                    CardView(icon: "checkmark.circle.fill", iconColor: .green, borderColor: .green, title: "Restoration Complete", description: "You can now access your account with all your previous information. Welcome back!")
                }
                
                Spacer()
                
                Button(action: {
                    onNext()
                }) {
                    HStack {
                        if isProcessing {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                            Text("Processing...")
                        } else {
                            Text("Done")
                        }
                    }
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(isProcessing ? Color.blue.opacity(0.7) : Color.blue)
                    .cornerRadius(12)
                }
                .disabled(isProcessing)
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
                
                // Error Message
                if let error = errorMessage {
                    Text("Error: \(error)")
                        .font(.system(size: 14))
                        .foregroundColor(.red)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 20)
                }
            }
        }
        .background(Color.white)
    }
}

#Preview {
    RestoreAccountResultScreen(onNext: {
            
        }) {
            // restore
        }
}
