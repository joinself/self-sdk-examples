//
//  RegistrationIntroScreen.swift
//  ios-client
//

import SwiftUI

public struct RegistrationIntroScreen: View {
    @Binding private var isProcessing: Bool
    @State private var errorMessage: String? = nil
    
    let onNext: () -> Void
    let onRestore: () -> Void
    public init(isProcessing: Binding<Bool> = .constant(false), errorMessage: String? = nil, onNext: @escaping () -> Void, onRestore: @escaping () -> Void) {
        self._isProcessing = isProcessing
        self.errorMessage = errorMessage
        self.onNext = onNext
        self.onRestore = onRestore
    }
    
    public var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Button {
                    onRestore()
                } label: {
                    Text("Restore Account")
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
                        Image(systemName: "plus")
                            .font(.system(size: 40))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                        Image(systemName: "person")
                            .font(.system(size: 40))
                            .foregroundColor(.white)
                    }
                    .frame(width: 100, height: 100, alignment: .topLeading)
                    
                    // Title and Subtitle
                    VStack(spacing: 12) {
                        Text("Register Your Account")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(.black)
                            .multilineTextAlignment(.center)
                        
                        Text("Complete a quick liveness check to securely register your Self account")
                            .font(.system(size: 16))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }
                }
                
                // What to Expect Section
                CardView(icon: ResourceNames.ICON_LIVENESS, title: "Liveness Check Required", description: "You will be asked for camera permission when you start. Look directly at the camera and follow the on-screen instructions.")
                
                Spacer()
                
                // Start Registration Button
                Button(action: {
                    startRegistration()
                }) {
                    HStack {
                        if isProcessing {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(0.8)
                            Text("Registering...")
                        } else {
                            Text("Start")
                        }
                    }
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(isProcessing ? Color.blue.opacity(0.7) : Color.blue)
                    .cornerRadius(12)
                }
                .disabled(isProcessing)
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
                
                // Error Message
                if let error = errorMessage {
                    Text("Error: \(error)")
                        .font(.system(size: 14))
                        .foregroundColor(.red)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 20)
                }
            }
        }
        .background(Color.white)
    }
    
    private func startRegistration() {
        onNext()
    }
}

#Preview {
    RegistrationIntroScreen(onNext: {
            
        }) {
            // restore
        }
}
