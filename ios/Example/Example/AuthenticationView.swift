//
//  AuthenticationView.swift
//  ExampleApp
//
//  Created by Long Pham on 2/6/25.
//
import SwiftUI
import SelfUI

// MARK: - SwiftUI Integration Example
struct AuthenticationView: View {
    @StateObject private var authManager = SelfAuthManager()
    @State private var showQRScanner = false
    @State private var isCodeValid = false
    
    var body: some View {
        VStack {
            Text("Self SDK Authentication")
                .font(.title)
            
            Button("Start Authentication") {
                // Your authentication trigger logic here
                showQRScanner = true
            }
            .buttonStyle(.borderedProminent)
        }
        .fullScreenCover(isPresented: $showQRScanner, onDismiss: {
            // dismissed view
        }, content: {
            QRReaderView(isCodeValid: $isCodeValid, onCode: { code in
                print("QRCode: \(code)")
            }) { codeData in
                print("QRCode: \(codeData)")
                authManager.handleAuthData(data: codeData) { error in
                    if error == nil {
                        showQRScanner = false
                    }
                }
            }
        })
        .padding()
    }
}
