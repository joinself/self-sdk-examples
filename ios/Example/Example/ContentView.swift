//
//  ContentView.swift
//  Example
//
//  Created by Long Pham on 8/5/25.
//

import SwiftUI
import self_ios_sdk

struct ContentView: View {
    
    @ObservedObject var viewModel: OnboardingViewModel = OnboardingViewModel()
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, SelfSDK!")
            
            Button {
                viewModel.lfcFlow()
            } label: {
                Text("LFC Flow")
            }

        }
        .padding()
    }
}

#Preview {
    ContentView()
}
