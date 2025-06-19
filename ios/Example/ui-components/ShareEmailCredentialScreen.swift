//
//  ShareEmailCredentialScreen.swift
//  ios-client
//

import SwiftUI

public struct ShareEmailCredentialScreen: View {
    let credentialName: String
    let onApprove: () -> Void
    let onDeny: () -> Void
    let onBack: () -> Void
    
    public init(credentialName: String, onApprove: @escaping () -> Void, onDeny: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.onApprove = onApprove
        self.onDeny = onDeny
        self.onBack = onBack
        self.credentialName = credentialName
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
                                Text("Provide \(credentialName) Credentials?")
                                    .font(.system(size: 32, weight: .bold))
                                    .foregroundColor(.black)
                                    .multilineTextAlignment(.center)
                                    .padding(.leading)
                                    .padding(.trailing)
                                
                                Text("The server is requesting proof of your verified email address. Your actual email address will not be shared - only cryptographic proof that you own a verified email.")
                                    .font(.system(size: 12))
                                    .foregroundColor(.gray)
                                    .multilineTextAlignment(.center)
                                    .padding(16)
                                    .background(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                                            .background(Color.blue.opacity(0.05))
                                    )
                                    .padding(.horizontal, 20)
                            }
                        }
                        
                        // Available Actions Section
                        VStack(alignment: .leading, spacing: 24) {
                            HStack {
                                Text("What Will Be Shared")
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(.black)
                                Spacer()
                            }
                            
                            VStack(spacing: 16) {
                                SimpleCardView(
                                    icon: "checkmark.circle.fill",
                                    title: "Verification Status",
                                    description: "Proof that you have a verified email credential",
                                    action: {
                                        
                                    }
                                )
                                
                                SimpleCardView(
                                    icon: "shield.pattern.checkered",
                                    title: "Cryptographic Proof",
                                    description: "Zero-knowledge proof of credential ownership",
                                    action: {
                                        
                                    }
                                )
                                
                                SimpleCardView(
                                    icon: "exclamationmark.circle",
                                    title: "Personal Data",
                                    description: "❌ Your actual email address will NOT be shared",
                                    action: {
                                        
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        
                        Spacer()
                    }
                    
                    VStack(spacing: 12) {
                        Button(action: {
                            onApprove()
                        }) {
                            Text("Sign Document")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color.blue)
                                .cornerRadius(12)
                        }
                        
                        // Reject Button
                        Button(action: {
                            onDeny()
                        }) {
                            Text("Reject")
                                .font(.system(size: 18, weight: .medium))
                                .foregroundColor(.blue)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color.clear)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.blue, lineWidth: 2)
                                )
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                    .background(Color.white)
                }
            }
            .background(Color.white)
        }
    }
}

#Preview {
    ShareEmailCredentialScreen(credentialName: "Email") {
        
    } onDeny: {
        
    } onBack: {
        
    }

}
