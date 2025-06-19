//
//  VerifyDocumentResultScreen.swift
//  ios-client
//

import SwiftUI

public struct VerifyDocumentResultScreen: View {
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
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 80))
                                .foregroundColor(.blue)
                            
                            Image(systemName: "checkmark")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Document Verified Successfully!")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                                .padding(.leading)
                                .padding(.trailing)
                            Text("Your identity document has been successfully verified and a secure credential has been created on your device.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .padding(.leading)
                                .padding(.trailing)
                            
                            CardView(icon: "checkmark.seal.fill", iconColor: .green, title: "Verification Complete", description: "Your document has been authenticated and a verifiable credential has been securely stored on your device. You can now use this credential to prove your identity.")
                        }
                    }
                    Spacer()
                }
                
                VStack(alignment: .leading, spacing: 24) {
                    HStack {
                        Text("What You Can Do Now")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.black)
                        Spacer()
                    }
                    
                    VStack(spacing: 16) {
                        SimpleCardView(
                            icon: "shield",
                            title: "Share Your Credentials",
                            description: "Use your verified credentials to authenticate with services",
                            action: {
                                
                            }
                        )
                        
                        SimpleCardView(
                            icon: "person.text.rectangle.fill",
                            title: "Prove Your Identity",
                            description: "Your verified document can be used as proof of your identity",
                            action: {
                                
                            }
                        )
                        
                        SimpleCardView(
                            icon: "checkmark.circle.fill",
                            title: "Access Protected Service",
                            description: "Many service accept verified credentials for enhanced security",
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
                        Text("Continue")
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
    VerifyDocumentResultScreen {
        
    } onBack: {
        
    }
}
