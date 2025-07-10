//
//  VerifyEmailResultScreen.swift
//  ios-client
//

import SwiftUI

public struct VerifyEmailResultScreen: View {
    let success: Bool
    let onContinue: () -> Void
    let onBack: () -> Void
    
    public init(success: Bool, onContinue: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.success = success
        self.onContinue = onContinue
        self.onBack = onBack
    }
    
    public var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Button {
                    onBack()
                } label: {
                    Image(systemName: ResourceHelper.ICON_BACK)
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
                        Image(systemName: success ? "checkmark.circle.fill" : "exclamationmark.circle")
                            .font(.system(size: 48, weight: .bold))
                            .foregroundColor(.primaryBlue)                    }
                    .padding(.top, 40)
                    
                    // Title and Subtitle
                    VStack(spacing: 12) {
                        Text("Email Verified Successfully!")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.black)
                            .multilineTextAlignment(.center)
                            .padding(.leading)
                            .padding(.trailing)
                        
                        CardView(icon: "checkmark.seal.fill", iconColor: .green, title: "Verification Complete", description: "Your email address has been verified and a secure credential has been created on your device.")
                    }
                }
                Spacer()
            }
            
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

#Preview {
    VStack {
        VerifyEmailResultScreen(success: true) {
            
        } onBack: {
            
        }
        
        VerifyEmailResultScreen(success: false) {
            
        } onBack: {
            
        }
    }
}
