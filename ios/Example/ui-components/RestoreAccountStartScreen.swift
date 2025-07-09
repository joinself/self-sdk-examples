//
//  RestoreAccountStartScreen.swift
//  ios-client
//

import SwiftUI

public struct RestoreAccountStartScreen: View {
    @Binding private var isProcessing: Bool
    @State private var errorMessage: String? = nil
    let onNext: () -> Void
    let onBack: () -> Void
    public init(isProcessing: Binding<Bool> = .constant(false), errorMessage: String? = nil, onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
        self._isProcessing = isProcessing
        self.errorMessage = errorMessage
        self.onNext = onNext
        self.onBack = onBack
    }
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // DEBUG Header
                HStack {
                    Button {
                        onBack()
                    } label: {
                        Image(systemName: ResourceNames.ICON_BACK)
                            .foregroundStyle(Color.gray)
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity)
                
                VStack(spacing: 40) {
                    // User Icon and Title Section
                    VStack(spacing: 24) {
                        // User Icon
                        ZStack {
                            Circle()
                                .fill(Color.blue)
                                .frame(width: 80, height: 80)
                            
                            Image(systemName: "icloud.and.arrow.down")
                                .font(.system(size: 40))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Restore Your Account")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("Verify your identity through a liveness check and selfie to securely restore your account data.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                        
                        CardView(icon: "info.circle.fill", title: "How Account Restoration Works", description: "To ensure security, you'll first complete a liveness check and verify your identity with a selfie. On confirmed, the Self system will automatically retrieve your encryption backup.")
                    }
                    
                    // What to Expect Section
                    VStack(alignment: .leading, spacing: 24) {
                        HStack {
                            Text("Restoration Steps")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.black)
                            Spacer()
                        }
                        
                        VStack(spacing: 20) {
                            ExpectationStepView(
                                stepNumber: 1,
                                title: "Camera Access",
                                description: "We'll ask for camera permission when you start"
                            )
                            
                            ExpectationStepView(
                                stepNumber: 2,
                                title: "Position Your Face",
                                description: "Look directly at the camera and follow on-screen instructions"
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    
                    // Privacy Protection Section
                    
//                    Spacer(minLength: 40)
                    
                    // Start Registration Button
                    Button(action: {
                        onNext()
                    }) {
                        HStack {
                            if isProcessing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                                Text("Restoring...")
                            } else {
                                Text("Start Recover Your Account")
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
        }
        .background(Color.white)
    }
}

#Preview {
    RestoreAccountStartScreen(onNext: {
            
        }) {
            // restore
        }
}
