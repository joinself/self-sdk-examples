//
//  DocSignStartScreen.swift
//  ios-client
//

import SwiftUI

struct DocSignStartScreen: View {
    let onSignDocument: () -> Void
    let onRejectDocument: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            // DEBUG Header
            HStack {
                Text("DEBUG: DOC_SIGN_START")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.blue)
            .frame(maxWidth: .infinity)
            
            // Scrollable content
            ScrollView {
                VStack(spacing: 40) {
                    // PDF Icon and Title Section
                    VStack(spacing: 24) {
                        // PDF Icon
                        Image(systemName: "doc.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.blue)
                            .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("PDF Document Signing")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("The server has requested you to sign a PDF document. Review the details below and choose whether to sign or reject.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                    }
                    
                    // PDF Agreement Document Info Box
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            Image(systemName: "doc.text.fill")
                                .font(.system(size: 24))
                                .foregroundColor(.blue)
                            
                            Text("PDF Agreement Document")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.black)
                            
                            Spacer()
                        }
                        
                        Text("You are being asked to sign a PDF document. This creates a verifiable digital signature using your cryptographic credentials.")
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
                    
                    // Security & Legal Notice Section
                    VStack(alignment: .leading, spacing: 24) {
                        Text("Security & Legal Notice")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.black)
                            .padding(.horizontal, 20)
                        
                        // Cryptographic Authentication
                        VStack(spacing: 16) {
                            HStack(spacing: 12) {
                                Image(systemName: "shield.fill")
                                    .font(.system(size: 20))
                                    .foregroundColor(.green)
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Cryptographic Authentication")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(.black)
                                    
                                    Text("Your signature is created using secure cryptographic keys unique to you")
                                        .font(.system(size: 14))
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                            }
                            
                            // Non-Repudiation
                            HStack(spacing: 12) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 20))
                                    .foregroundColor(.green)
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Non-Repudiation")
                                        .font(.system(size: 16, weight: .semibold))
                                        .foregroundColor(.black)
                                    
                                    Text("The signature provides legal proof of your agreement to the terms of the document.")
                                        .font(.system(size: 14))
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                }
                .padding(.bottom, 20) // Space above buttons
            }
            
            // Fixed buttons at bottom
            VStack(spacing: 12) {
                // Sign Document Button
                Button(action: {
                    onSignDocument()
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
                    onRejectDocument()
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
        .background(Color.white)
    }
}

#Preview {
    DocSignStartScreen(
        onSignDocument: {
            print("Preview: Sign Document")
        },
        onRejectDocument: {
            print("Preview: Reject Document")
        }
    )
} 
