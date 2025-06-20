//
//  BackupAccountStartScreen.swift
//  ios-client
//

import SwiftUI

public struct BackupAccountStartScreen: View {
    let onNext: () -> Void
    let onBack: () -> Void
    
    public init(onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
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
                            
                            Image(systemName: "clock.arrow.trianglehead.counterclockwise.rotate.90")
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
                        
                        CardView(icon: "info.circle.fill", iconColor: .green, borderColor: .green, title: "Backup Complete & Secured", titleColor: .green, description: "Your information is now safely stored and managed by the Self system. You can restore your account through identity verification if needed.")
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
                    
                    // Privacy Protection Section
                    
//                    Spacer(minLength: 40)
                    
                    // Start Registration Button
                    Button(action: {
                        onNext()
                    }) {
                        Text("Start Backup Process")
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
        BackupAccountStartScreen {
            
        } onBack: {
            
        }
    }
}
