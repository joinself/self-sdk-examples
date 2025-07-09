//
//  ServerConnectionScreen.swift
//  ios-client
//

import SwiftUI

public struct ServerConnectionScreen: View {
    @State private var serverAddress = ""
    @State private var showSuccessMessage = true // Show the success message from registration
    @State private var isConnecting = false
    
    let onConnectToServer: (String) -> Void
    
    private let maxCharacters = 66
    
    private var isValidServerAddress: Bool {
        return serverAddress.count == maxCharacters && serverAddress.allSatisfy { $0.isHexDigit }
    }
    
    public init(serverAddress: String = "", showSuccessMessage: Bool = true, isConnecting: Bool = false, onConnectToServer: @escaping (String) -> Void) {
        self.serverAddress = serverAddress
        self.showSuccessMessage = showSuccessMessage
        self.isConnecting = isConnecting
        self.onConnectToServer = onConnectToServer
    }
    
    public var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // DEBUG Header
                HStack {
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity)
                
                VStack(spacing: 40) {
                    // Cloud Icon and Title Section
                    VStack(spacing: 24) {
                        // Cloud Icon
                        Image(systemName: "cloud")
                            .font(.system(size: 80))
                            .foregroundColor(.blue)
                            .padding(.top, 40)
                        
                        // Title and Subtitle
                        VStack(spacing: 12) {
                            Text("Connect to Server")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(.black)
                                .multilineTextAlignment(.center)
                            
                            Text("Enter the server address to connect your Self account to authentication servers.")
                                .font(.system(size: 16))
                                .foregroundColor(.gray)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 20)
                        }
                    }
                    
                    // Secure Connection Info Box
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            Image(systemName: "shield.fill")
                                .font(.system(size: 24))
                                .foregroundColor(.blue)
                            
                            Text("Secure Connection")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(.black)
                            
                            Spacer()
                        }
                        
                        Text("Your connection will be encrypted and your biometric data remains secure on your device. Only verification results are shared.")
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
                    
                    // Server Address Section
                    VStack(alignment: .leading, spacing: 16) {
                        HStack {
                            Text("Server Address")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.black)
                            Spacer()
                        }
                        
                        Text("Enter the 66-character hexadecimal server address/ID provided by your administrator or service provider.")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                            .lineLimit(nil)
                        
                        VStack(alignment: .leading, spacing: 8) {
                            TextField("Server Address/ID", text: $serverAddress)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 12)
                                .background(Color.white)
                                .foregroundColor(.black)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                )
                                .font(.system(size: 16))
                                .autocapitalization(.none)
                                .disableAutocorrection(true)
                                .onChange(of: serverAddress) { newValue in
                                    // Limit to 66 characters and ensure hex
                                    let filtered = newValue.filter { $0.isHexDigit }
                                    if filtered.count > maxCharacters {
                                        serverAddress = String(filtered.prefix(maxCharacters))
                                    } else {
                                        serverAddress = filtered
                                    }
                                }
                            
                            HStack {
                                Text("\(serverAddress.count)/\(maxCharacters) characters")
                                    .font(.system(size: 12))
                                    .foregroundColor(.gray)
                                Spacer()
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    
                    Spacer(minLength: 60)
                    
                    // Connect to Server Button
                    Button(action: {
                        connectToServer()
                    }) {
                        HStack {
                            if isConnecting {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: isValidServerAddress ? .white : .black))
                                    .scaleEffect(0.8)
                                Text("Connecting...")
                            } else {
                                Text("Connect to Server")
                            }
                        }
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(isValidServerAddress ? .white : .black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(isValidServerAddress ? Color.blue : Color.gray.opacity(0.2))
                        .cornerRadius(12)
                    }
                    .disabled(!isValidServerAddress || isConnecting)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 40)
                }
                
                // Success Message (shown after registration)
                if showSuccessMessage {
                    VStack {
                        Spacer()
                        
                        HStack(spacing: 12) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 20))
                                .foregroundColor(.green)
                            
                            Text("Account registered successfully!")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.white)
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 16)
                        .background(Color.black.opacity(0.8))
                        .cornerRadius(12)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 40)
                    }
                    .onAppear {
                        // Auto-hide success message after 4 seconds
                        DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
                            withAnimation(.easeOut(duration: 0.5)) {
                                showSuccessMessage = false
                            }
                        }
                    }
                }
            }
        }
        .background(Color.white)
    }
    
    private func connectToServer() {
        guard isValidServerAddress else {
            return
        }
        
        print("🌐 ServerConnectionScreen: Navigating to server connection processing with address: \(serverAddress)")
        
        // Navigate to the processing screen
        onConnectToServer(serverAddress)
    }
}

#Preview {
    ServerConnectionScreen(
        onConnectToServer: { serverAddress in
            print("Preview: Connect to server: \(serverAddress)")
        }
    )
}

extension Character {
    var isHexDigit: Bool {
        return self.isASCII && (self.isNumber || ("a"..."f").contains(self.lowercased().first!) || ("A"..."F").contains(self))
    }
} 
