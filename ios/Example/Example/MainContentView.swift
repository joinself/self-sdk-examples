//
//  ContentView.swift
//  Example
//
//  Created by Long Pham on 8/5/25.
//

import SwiftUI
import self_ios_sdk

struct MainContentView: View {
    
    @ObservedObject var viewModel: MainViewModel = MainViewModel()
    @State private var showVerifyDocument: Bool = false
    
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, SelfSDK!")
            
            Button {
                viewModel.registerAccount { success in
                    print("Register account finished: \(success)")
                }
            } label: {
                Text("Register Account")
            }
            .disabled(viewModel.accountRegistered)
            .buttonStyle(.borderedProminent)
            
            Button {
                viewModel.lfcFlow()
            } label: {
                Text("LFC Flow")
            }
            .buttonStyle(.borderedProminent)
            
            Button {
                showVerifyDocument = true
            } label: {
                Text("Verify Document Flow")
            }
            .buttonStyle(.borderedProminent)
            List {
                // display credentials here!
                ForEach(viewModel.credentialItems) { credentialItem in
                    if let value = credentialItem.claims.first?.value() {
                        Text(value)
                            .foregroundStyle(Color.white)
                    }
                    
                }
            }

        }
        .fullScreenCover(isPresented: $showVerifyDocument, onDismiss: {
            // dismiss view
        }, content: {
            DocumentFlow(account: viewModel.account, autoCaptureImage: false, onResult:  { success in
                print("Verify document finished: \(success)")
                showVerifyDocument = false
                // reload view to display document's credential
                
            })
        })
        .padding()
    }
}

#Preview {
    MainContentView()
}
