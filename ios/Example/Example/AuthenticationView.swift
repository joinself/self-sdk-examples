//
//  AuthenticationView.swift
//  ExampleApp
//
//  Created by Long Pham on 2/6/25.
//
import SwiftUI
import SelfUI
import self_ios_sdk

// MARK: - SwiftUI Integration Example
struct AuthenticationView: View {
    @StateObject private var authManager = SelfAuthManager()
    @State private var showQRScanner = false
    @State private var showVerifyDocument = false
    @State private var isCodeValid = false
    @StateObject private var viewModel: MainViewModel = MainViewModel()
    
    var body: some View {
        VStack {
            Text("Self SDK Authentication")
                .font(.title)
            
            Button("Start Authentication") {
                // Your authentication trigger logic here
                showQRScanner = true
            }
            .buttonStyle(.borderedProminent)
            
            Button("Document Verification") {
                showVerifyDocument = true
            }
            .buttonStyle(.borderedProminent)
            
            Button("Document Verification in SDK") {
                
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
        .fullScreenCover(isPresented: $showVerifyDocument, onDismiss: {
            // document dismissed
        }, content: {
            DocumentFlow(account: authManager.getAccount(), autoCaptureImage: false, onResult:  { success in
                print("Verify document flow: \(success)")
            })
        })
        .padding()
    }
}
