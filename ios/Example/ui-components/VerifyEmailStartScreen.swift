//
//  VerifyEmailStartScreen.swift
//  ios-client
//

import SwiftUI

public struct VerifyEmailStartScreen: View {
    let onContinue: () -> Void
    let onBack: () -> Void
    
    public init(onContinue: @escaping () -> Void, onBack: @escaping () -> Void) {
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
                        Image(systemName: "envelope")
                            .font(.system(size: 80))
                            .foregroundColor(.blue)
                        
                        Image(systemName: "checkmark")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .padding(.top, 40)
                    
                    // Title and Subtitle
                    VStack(spacing: 12) {
                        Text("Email Address Verification")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.black)
                            .multilineTextAlignment(.center)
                            .padding(.leading)
                            .padding(.trailing)
                        
                        Text("Verify ownship of your email address to create a trusted credential.")
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                            .padding(.leading)
                            .padding(.trailing)
                        
                        CardView(icon: "checkmark.seal.fill", iconColor: .accentColor, title: "Email Verification Required", description: "You will need to enter your email address and confirm it by clicking a verification link sent to your inbox. Keep your email app handy during this process.")
                    }
                }
                Spacer()
            }
            
            Spacer()
            
            VStack(spacing: 12) {
                Button(action: {
                    onContinue()
                }) {
                    Text("Start Email Verification")
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
    VerifyEmailStartScreen {
        
    } onBack: {
        
    }
}
