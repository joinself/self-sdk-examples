//
//  DocSignResultScreen.swift
//  ios-client
//

import SwiftUI

public struct DocSignResultScreen: View {
    let success: Bool
    let onContinue: () -> Void
    
    public init(success: Bool, onContinue: @escaping () -> Void) {
        self.success = success
        self.onContinue = onContinue
    }
    
    public var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            
            // Scrollable content
            ScrollView {
                VStack(spacing: 40) {
                    if success {
                        // Success content
                        successContent
                    } else {
                        // Rejection content
                        rejectionContent
                    }
                }
                .padding(.bottom, 20) // Space above button
            }
            
            // Fixed button at bottom
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
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
            .background(Color.white)
        }
        .background(Color.white)
    }
    
    // MARK: - Success Content
    
    private var successContent: some View {
        VStack(spacing: 40) {
            // Checkmark Icon and Title Section
            VStack(spacing: 24) {
                // Blue Checkmark Circle
                ZStack {
                    Circle()
                        .fill(Color.blue)
                        .frame(width: 48, height: 48)
                    
                    Image(systemName: "checkmark")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                }
                .padding(.top, 40)
                
                // Title and Subtitle
                VStack(spacing: 12) {
                    Text("Document Signed Successfully")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .multilineTextAlignment(.center)
                    
                    Text("Your digital signature has been added to the document.")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
            }
            
            // Signature Complete Info Box
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.green)
                    
                    Text("Signature Complete")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.black)
                    
                    Spacer()
                }
                
                Text("Your cryptographic signature has been successfully applied to the document. The document is now legally enforceable and cannot be modified.")
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .lineLimit(nil)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.green.opacity(0.3), lineWidth: 1)
                    .background(Color.green.opacity(0.05))
            )
            .padding(.horizontal, 20)
            
            // Security Verification Section
            VStack(alignment: .leading, spacing: 16) {
                Text("Security Verification")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.black)
                    .padding(.horizontal, 20)
                
                HStack(spacing: 12) {
                    Image(systemName: "shield.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.green)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Tamper-Proof Signature")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.black)
                        
                        Text("Your signature is cryptographically secure and cannot be forged")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                }
                .padding(.horizontal, 20)
            }
        }
    }
    
    // MARK: - Rejection Content
    
    private var rejectionContent: some View {
        VStack(spacing: 40) {
            // Exclamation Icon and Title Section
            VStack(spacing: 24) {
                // Blue Exclamation Circle
                ZStack {
                    Circle()
                        .fill(Color.blue)
                        .frame(width: 48, height: 48)
                    
                    Image(systemName: "exclamationmark")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                }
                .padding(.top, 40)
                
                // Title and Subtitle
                VStack(spacing: 12) {
                    Text("Document Signing Rejected")
                        .font(.system(size: 32, weight: .bold))
                        .foregroundColor(.black)
                        .multilineTextAlignment(.center)
                    
                    Text("You rejected the document signing request.")
                        .font(.system(size: 16))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
            }
        }
    }
}

#Preview {
    VStack {
        DocSignResultScreen(
            success: true,
            onContinue: {
                print("Preview: Continue (Success)")
            }
        )
        
        DocSignResultScreen(
            success: false,
            onContinue: {
                print("Preview: Continue (Rejection)")
            }
        )
    }
} 
