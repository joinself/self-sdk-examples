//
//  AuthenticationView.swift
//  ExampleApp
//
//  Created by Long Pham on 2/6/25.
//
import SwiftUI

// MARK: - SwiftUI Integration Example
struct AuthenticationView: View {
    @StateObject private var authManager = SelfAuthManager()
    
    var body: some View {
        VStack {
            Text("Self SDK Authentication")
                .font(.title)
            
            Button("Start Authentication") {
                // Your authentication trigger logic here
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
