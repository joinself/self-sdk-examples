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

        }
        .padding()
    }
}

#Preview {
    MainContentView()
}
