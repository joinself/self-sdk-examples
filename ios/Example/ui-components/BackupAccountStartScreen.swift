//
//  BackupAccountStartScreen.swift
//  ios-client
//

import SwiftUI

public struct BackupAccountStartScreen: View {
    @State private var isProcessing = false
    @State private var errorMessage: String? = nil
    let onNext: () -> Void
    let onBack: () -> Void
    public init(isProcessing: Bool = false, errorMessage: String? = nil, onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.isProcessing = isProcessing
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
                        Image(systemName: "arrow.left")
                            .foregroundStyle(Color.white)
                    }
                    Text("DEBUG: BACKUP_ACCOUNT_INTRO")
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
                            
                            Image(systemName: "checkmark.icloud.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Backup Successful")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("Your account data has been securely backed up by the Self system.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                        
                        CardView(icon: "info.circle.fill",
                                 iconColor: .green,
                                 borderColor: .green,
                                 title: "What is Account Backup?", description: "Backup up your account creates an encrypted copy of your essential data. The Self system securely manages the recovery mechanism, allowing you to restore your account on a new device after identity verification.")
                    }
                    
                    // What to Expect Section
                    VStack(alignment: .leading, spacing: 24) {
                        HStack {
                            Text("How Backup Works")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.black)
                            Spacer()
                        }
                        
                        VStack(spacing: 20) {
                            ExpectationStepView(
                                stepNumber: 1,
                                title: "Prepare Data",
                                description: "Your essestial account data is prepared for backup."
                            )
                            
                            ExpectationStepView(
                                stepNumber: 2,
                                title: "Encrypt & Secure",
                                description: "The data is strongly encrypted, and the Self system sets up secure recovery protocols."
                            )
                            
                            ExpectationStepView(
                                stepNumber: 3,
                                title: "Backup Complete",
                                description: "Your encrypted data is backed up. You can restore it later through idenity verification."
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
                                Text("Processing...")
                            } else {
                                Text("Start Backup Process")
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
    BackupAccountStartScreen(onNext: {
            
        }) {
            // restore
        }
}
