//
//  RestoreAccountResultScreen.swift
//  ios-client
//

import SwiftUI

public struct RestoreAccountResultScreen: View {
    let success: Bool
    let onNext: () -> Void
    let onBack: () -> Void
    public init(success: Bool, onNext: @escaping () -> Void, onBack: @escaping () -> Void) {
        self.success = success
        self.onNext = onNext
        self.onBack = onBack
    }
    public var body: some View {
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
                // User Icon and Title Section
                VStack(spacing: 24) {
                    // User Icon
                    ZStack {
                        Circle()
                            .fill(Color.blue)
                            .frame(width: 80, height: 80)
                        
                        Image(systemName: "checkmark.rectangle.portrait")
                            .font(.system(size: 40))
                            .foregroundColor(.white)
                    }
                    .padding(.top, 40)
                    
                    // Title and Subtitle
                    VStack(spacing: 12) {
                        Text("Restore Completed")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.black)
                            .multilineTextAlignment(.center)
                        
                        Text("Your account has been successfully and securely restored.")
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }
                    
                    CardView(icon: "checkmark.circle.fill", iconColor: .green, borderColor: .green, title: "Restoration Complete", description: "You can now access your account with all your previous information. Welcome back!")
                }
                
                Spacer()
                
                Button(action: {
                    onNext()
                }) {
                    HStack {
                        Text("Done")
                    }
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
        .background(Color.white)
    }
}

#Preview {
    RestoreAccountResultScreen(success: true) {
        
    } onBack: {
        
    }

}
