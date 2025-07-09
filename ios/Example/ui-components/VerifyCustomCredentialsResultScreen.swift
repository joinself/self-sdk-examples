//
//  VerifyCustomCredentialsResultScreen.swift
//  ios-client
//

import SwiftUI

public struct VerifyCustomCredentialsResultScreen: View {
    let success: Bool
    let onContinue: () -> Void
    let onBack: () -> Void
    
    public init(success: Bool, onContinue: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.success = success
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
                            Text("Custom Credential Issued!")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                                .padding(.leading)
                                .padding(.trailing)
                            Text("Your custom credential has been successfully issued and added to your wallet.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .padding(.leading)
                                .padding(.trailing)
                            
                            CardView(icon: "shield.fill", iconColor: .green, title: "Credential Ready", description: "You can now use your Custom Credentials. It has been securely signed by our server and it is ready for use.")
                        }
                    }
                    Spacer()
                }
                
                VStack(alignment: .leading, spacing: 24) {
                    HStack {
                        Text("Next Steps")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.black)
                        Spacer()
                    }
                    
                    VStack(spacing: 16) {
                        ExpectationStepView(
                            stepNumber: 1,
                            title: "View Your Credential",
                            description: "You can find your new Custom Credentials in your digital wallet or credential list."
                        )
                    }
                }
                .padding(.horizontal, 20)
            
                Spacer(minLength: 40)
                
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
    VerifyCustomCredentialsResultScreen(success: true) {
        
    } onBack: {
        
    }
}
