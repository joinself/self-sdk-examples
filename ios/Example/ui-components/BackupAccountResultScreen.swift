//
//  BackupAccountStartScreen.swift
//  ios-client
//

import SwiftUI

public struct BackupAccountResultScreen: View {
    let success: Bool
    let onNext: () -> Void
    let onBack: () -> Void
    public init(success: Bool, onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.success = success
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
                    Text("DEBUG: BACKUP_ACCOUNT_RESULT")
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
                                .fill(success ? Color.blue : .gray)
                                .frame(width: 80, height: 80)
                            
                            Image(systemName: success ? "checkmark.icloud.fill" : "exclamationmark.icloud")
                                .font(.system(size: 40))
                                .foregroundColor(success ? .white : .red)
                        }
                        .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text(success ? "Backup Successful" : "Backup Failed!")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("Your account data has been securely backed up by the Self system.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                        
                        CardView(icon: "info.circle.fill", iconColor: .green, borderColor: .green, title: "Backup Complete & Secured", titleColor: .green, description: "Your information is now safely stored and managed by the Self system. You can restore your account through identity verification if needed.")
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
                        Text("Done")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.blue)
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
        BackupAccountResultScreen(success: true, onNext: {}, onBack: {})
        BackupAccountResultScreen(success: false, onNext: {}, onBack: {})
    }
}
