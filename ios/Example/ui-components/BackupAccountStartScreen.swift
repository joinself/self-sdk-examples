//
//  BackupAccountStartScreen.swift
//  ios-client
//

import SwiftUI

public struct BackupAccountStartScreen: View {
    @Binding private var isProcessing: Bool
    let onNext: () -> Void
    let onBack: () -> Void
    
    public init(isProcessing: Binding<Bool> = .constant(false), onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
        self._isProcessing = isProcessing
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
                            
                            Image(systemName: "icloud.and.arrow.up")
                                .font(.system(size: 40))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Secure Your Account")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("Created an encrypted backup of your account data, manage by the Self system, to prevent data loss and enable easy recovery.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                        
                        
                        CardView(icon: "info.circle.fill",
                                 iconColor: .accentColor,
                                 borderColor: .accentColor,
                                 title: "What is Account Backup?", description: "Backup up your account creates an encrypted copy of your essential data. The Self system securely manages the recovery mechanism, allowing you to restore your account on a new device after identity verification.")
                    }
                    
                    // What to Expect Section
                    VStack(alignment: .leading, spacing: 24) {
                        HStack {
                            Text("Backup Details")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.black)
                            Spacer()
                        }
                        
                        VStack(spacing: 20) {
                            ExpectationStepView(
                                stepNumber: 1,
                                title: "System-Managed Recovery",
                                description: "The Self system has secured your backup. Account recovery will involve idenity verification."
                            )
                            
                            ExpectationStepView(
                                stepNumber: 2,
                                title: "Data Encrypted",
                                description: "Your account data was encrypted for securely."
                            )
                            
                            ExpectationStepView(
                                stepNumber: 3,
                                title: "Secure Upload",
                                description: "Encrypted data was uploaded and store securely."
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    
                    Button(action: {
                        onNext()
                    }) {
                        HStack {
                            if isProcessing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                                Text("Backing up...")
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
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
                }
            }
        }
        .background(Color.white)
    }
}

#Preview {
    VStack {
        BackupAccountStartScreen {
            
        } onBack: {
            
        }
    }
}
