//
//  RegistrationIntroScreen.swift
//  ios-client
//

import SwiftUI

public struct RegistrationIntroScreen: View {
    @State private var showingRegistration = false
    @State private var isProcessing = false
    @State private var errorMessage: String? = nil
    
    let onRegistrationComplete: () -> Void
    let onNext: () -> Void
    public init(showingRegistration: Bool = false, isProcessing: Bool = false, errorMessage: String? = nil, onRegistrationComplete: @escaping () -> Void, onNext: @escaping () -> Void) {
        self.showingRegistration = showingRegistration
        self.isProcessing = isProcessing
        self.errorMessage = errorMessage
        self.onRegistrationComplete = onRegistrationComplete
        self.onNext = onNext
    }
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // DEBUG Header
                HStack {
                    Text("DEBUG: REGISTRATION_INTRO")
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
                            
                            Image(systemName: "person.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.white)
                        }
                        .padding(.top, 40)
                        
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
                    VStack(alignment: .leading, spacing: 24) {
                        HStack {
                            Text("What to Expect")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.black)
                            Spacer()
                        }
                        
                        VStack(spacing: 20) {
                            ExpectationStepView(
                                stepNumber: 1,
                                title: "Camera Access",
                                description: "We'll ask for camera permission when you start"
                            )
                            
                            ExpectationStepView(
                                stepNumber: 2,
                                title: "Position Your Face",
                                description: "Look directly at the camera and follow on-screen instructions"
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    
                    // Privacy Protection Section
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            Image(systemName: "lock.fill")
                                .font(.system(size: 24))
                                .foregroundColor(.blue)
                            
                            Text("Your Privacy is Protected")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.black)
                            
                            Spacer()
                        }
                        
                        Text("All biometric data is processed securely and never stored permanently. Your face data is used only for account registration and then discarded.")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                            .lineLimit(nil)
                    }
                    .padding(16)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                            .background(Color.blue.opacity(0.05))
                    )
                    .padding(.horizontal, 20)
                    
//                    Spacer(minLength: 40)
                    
                    // Start Registration Button
                    Button(action: {
                        startRegistration()
                    }) {
                        HStack {
                            if isProcessing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                                Text("Processing...")
                            } else {
                                Text("Start Registration")
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
        }
//        .ignoresSafeArea()
        .background(Color.white)
        .onAppear {
            checkRegistrationStatus()
        }
    }
    
    private func checkRegistrationStatus() {
//        guard let account = account else { return }
//        
//        if account.registered() {
//            print("🎬 RegistrationIntroScreen: Account already registered on appear, navigating to server connection")
//            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
//                onRegistrationComplete()
//            }
//        }
    }
    
    private func startRegistration() {
        onNext()
    }
}

struct ExpectationStepView: View {
    let stepNumber: Int
    let title: String
    let description: String
    
    var body: some View {
        HStack(spacing: 16) {
            // Step Number Circle
            ZStack {
                Circle()
                    .fill(Color.blue)
                    .frame(width: 40, height: 40)
                
                Text("\(stepNumber)")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
            }
            
            // Step Content
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.black)
                
                Text(description)
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .lineLimit(nil)
            }
            
            Spacer()
        }
    }
}

#Preview {
    RegistrationIntroScreen(
        onRegistrationComplete: {
            print("Preview: Registration complete")
        }, onNext: {
            
        }
    )
} 
