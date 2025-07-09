//
//  VerifyCustomCredentialsStartScreen.swift
//  ios-client
//

import SwiftUI

public struct VerifyCustomCredentialsStartScreen: View {
    let onContinue: () -> Void
    let onBack: () -> Void
    
    public init(onContinue: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.onContinue = onContinue
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
                    // Shield Icon and Title Section
                    VStack(spacing: 24) {
                        // Shield with Checkmark Icon
                        ZStack {
                            Image(systemName: "list.bullet.rectangle.portrait.fill")
                                .font(.system(size: 80))
                                .foregroundColor(.blue)
                            
//                            Image(systemName: "checkmark")
//                                .font(.system(size: 32, weight: .bold))
//                                .foregroundColor(.white)
                        }
                        .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Receive Your Digital Credential")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                                .padding(.leading)
                                .padding(.trailing)
                            Text("A new Digital Credential will be generated and securely signed by our server, then delivered to your device.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .padding(.leading)
                                .padding(.trailing)
                            
                            CardView(icon: "checkmark.icloud.fill", iconColor: .accentColor, title: "Server-Generated Credentials", description: "For your securiry and convenience, your Digital Credential is created and digitally signed by our trusted server. This ensures its authenticity and integrity.")
                            
                            CardView(icon: "shield.fill", iconColor: .accentColor, title: "What To Expect", description: "The process quick and secure. Once initiated, the server will prepare your Digital Credential. No complex steps are required from your side during generation.")
                        }
                    }
                    Spacer()
                }
                
                VStack(alignment: .leading, spacing: 24) {
                    HStack {
                        Text("Benefits")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.black)
                        Spacer()
                    }
                    
                    VStack(spacing: 16) {
                        SimpleCardView(
                            icon: "checkmark.seal",
                            title: "Authentic & Verified",
                            description: "Receive a credential that is verifiably authentic.",
                            action: {
                                
                            }
                        )
                        
                        SimpleCardView(
                            icon: "shield.lefthalf.filled",
                            title: "Security Delivered",
                            description: "Your credential is created and delivered through a secure channel.",
                            action: {
                                
                            }
                        )
                    }
                }
                .padding(.horizontal, 20)
            
                Spacer()
                
                VStack(spacing: 12) {
                    Button(action: {
                        onContinue()
                    }) {
                        Text("Get My Digital Credential")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.blue)
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
                .background(Color.white)
            }
            .background(Color.white)
        }
    }
}

#Preview {
    VerifyCustomCredentialsStartScreen {
        
    } onBack: {
        
    }
}
