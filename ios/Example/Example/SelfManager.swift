//
//  SelfManager.swift
//  ExampleApp
//
//  Created by Long Pham on 29/5/25.
//


import Foundation
import SwiftUI
import self_ios_sdk

/// Minimal Self SDK manager for authentication validation demo
@MainActor
class SelfManager: ObservableObject {
    let account: Account
    
    @Published var currentStep: DemoStep = .registration
    @Published var registrationStatus: String = "Ready for registration"
    @Published var connectionStatus: String = "Not connected"
    @Published var authStatus: String = "Not started"
    @Published var isProcessing: Bool = false
    @Published var connectedServerAddress: String? = nil
    
    enum DemoStep {
        case registration
        case serverConnection
        case authentication
        case completed
    }
    
    init() {
        // Setup storage path
        let fileManager = FileManager.default
        let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let storageURL = documentsDirectory.appendingPathComponent("selfsdk_storage")
        
        // Create storage directory if needed
        if !fileManager.fileExists(atPath: storageURL.path) {
            do {
                try fileManager.createDirectory(at: storageURL, withIntermediateDirectories: true, attributes: nil)
                print("SelfManager: Created storage directory at: \(storageURL.path)")
            } catch {
                print("SelfManager: Error creating storage directory: \(error.localizedDescription)")
            }
        }
        
        // Build the account object
        self.account = Account.Builder()
            .withEnvironment(Environment.preview)
            .withSandbox(true)
            .withStoragePath(storageURL.path)
            .build()
        
        print("SelfManager: Account initialized")
        
        // Setup event listeners
        setupEventListeners()
        
        // Initialize SDK
        SelfSDK.initialize()
    }
    
    private func setupEventListeners() {
        // Status listener for connection state
        account.setOnStatusListener { [weak self] status in
            DispatchQueue.main.async {
                guard let self = self else { return }
                print("SelfManager: Account status: \(status)")
                
                if status == 0 {
                    let address = self.account.generateAddress()
                    print("SelfManager: Account ready. Address: \(address)")
                    
                    // Check if already registered
                    self.checkRegistrationStatus()
                } else {
                    print("SelfManager: Account initialization failed with status: \(status)")
                }
            }
        }
        
        // Request listener for authentication challenges - from documentation example
        account.setOnRequestListener { [weak self] requestMessage in
            guard let self = self else { return }
            
            let senderAddress = requestMessage.fromIdentifier()
            print("SelfManager: 🔔 REQUEST RECEIVED from: \(senderAddress)")
            
            if let credentialRequest = requestMessage as? CredentialRequest {
                print("SelfManager: ✅ Credential request received")
                
                // Check all requested credentials - from documentation example
                let allClaims: [Claim] = credentialRequest.details()
                let livenessCredentials = allClaims.filter { 
                    $0.types().contains(CredentialType.Liveness) && 
                    $0.types().contains(CredentialType.Verifiable)
                }
                let otherCredentials = allClaims.filter { 
                    !($0.types().contains(CredentialType.Liveness) && 
                      $0.types().contains(CredentialType.Verifiable))
                }
                
                print("SelfManager: Total credentials requested: \(allClaims.count)")
                print("SelfManager: Liveness credentials: \(livenessCredentials.count)")
                print("SelfManager: Other credentials: \(otherCredentials.count)")
                
                // Log other credential types for debugging
                for claim in otherCredentials {
                    print("SelfManager: Ignoring unsupported credential types: \(claim.types())")
                }
                
                if !livenessCredentials.isEmpty {
                    print("SelfManager: ✅ Processing liveness credentials only")
                    self.handleLivenessAuthentication(credentialRequest, livenessCredentials: livenessCredentials)
                } else {
                    print("SelfManager: ❌ No liveness credentials found - this app only supports liveness authentication")
                    DispatchQueue.main.async {
                        self.authStatus = "❌ No liveness credentials in request"
                    }
                }
            } else {
                print("SelfManager: ❌ Not a credential request")
            }
        }
        
        print("SelfManager: Event listeners configured")
    }
    
    // Check if account is already registered
    private func checkRegistrationStatus() {
        // For demo purposes, always start with registration
        // In real app, you'd check actual registration status
        DispatchQueue.main.async {
            self.registrationStatus = "Ready to register account"
        }
    }
    
    // Start registration flow using Self SDK
    func startRegistration() {
        DispatchQueue.main.async {
            self.isProcessing = true
            self.registrationStatus = "Starting registration..."
        }
        
        print("SelfManager: Starting registration with Self SDK")
        
        // Use Self SDK for registration - from documentation pattern
        SelfSDK.showLiveness(
            account: account,
            showIntroduction: false,
            autoDismiss: true
        ) { [weak self] selfieImageData, credentials, error in
            guard let self = self else { return }
            
            if let error = error {
                print("SelfManager: ❌ Registration liveness failed: \(error)")
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.registrationStatus = "❌ Registration failed"
                }
                return
            }
            
            guard !credentials.isEmpty else {
                print("SelfManager: ❌ No credentials from registration liveness")
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.registrationStatus = "❌ No credentials provided"
                }
                return
            }
            
            print("SelfManager: ✅ Registration liveness successful")
            
            // Register account with Self SDK
            Task {
                await self.registerAccount(selfieData: selfieImageData, credentials: credentials)
            }
        }
    }
    
    // Register account with Self SDK
    private func registerAccount(selfieData: Data, credentials: [Any]) async {
        DispatchQueue.main.async {
            self.registrationStatus = "Registering account..."
        }
        
        // Convert credentials to proper type
        let selfCredentials = credentials.compactMap { $0 as? Credential }
        
        guard !selfCredentials.isEmpty else {
            print("SelfManager: ❌ No valid credentials for registration")
            DispatchQueue.main.async {
                self.isProcessing = false
                self.registrationStatus = "❌ No valid credentials"
            }
            return
        }
        
        do {
            let success = try await account.register(selfieImage: selfieData, credentials: selfCredentials)
            
            DispatchQueue.main.async {
                self.isProcessing = false
                
                if success {
                    print("SelfManager: ✅ Account registration successful")
                    self.registrationStatus = "✅ Registration completed"
                    self.currentStep = .serverConnection
                    self.connectionStatus = "Ready for server connection"
                } else {
                    print("SelfManager: ❌ Account registration failed")
                    self.registrationStatus = "❌ Registration failed"
                }
            }
        } catch {
            print("SelfManager: ❌ Registration error: \(error)")
            DispatchQueue.main.async {
                self.isProcessing = false
                self.registrationStatus = "❌ Registration error: \(error.localizedDescription)"
            }
        }
    }
    
    // Connect to server
    func connectToServer(address: String) {
        guard !address.isEmpty else { return }
        
        DispatchQueue.main.async {
            self.isProcessing = true
            self.connectionStatus = "Connecting to server..."
            self.connectedServerAddress = nil
        }
        
        print("SelfManager: Connecting to server: \(address)")
        
        Task {
            do {
                let connectionResult = try await account.connectWith(address: address, info: [:])
                
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.connectionStatus = "✅ Connected to server"
                    self.connectedServerAddress = address
                    self.currentStep = .authentication
                    self.authStatus = "Ready for authentication"
                    print("SelfManager: Connected successfully. GroupAddress: \(connectionResult)")
                    
                    // Brief delay to let user see the connection success before starting auth
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        self.requestAuthentication()
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self.isProcessing = false
                    self.connectionStatus = "❌ Connection failed: \(error.localizedDescription)"
                    print("SelfManager: Connection error: \(error)")
                }
            }
        }
    }
    
    // Request authentication from server - from documentation example
    private func requestAuthentication() {
        guard let serverAddress = connectedServerAddress else { return }
        
        print("SelfManager: Requesting authentication from server")
        
        DispatchQueue.main.async {
            self.authStatus = "Requesting authentication..."
        }
        
        Task {
            do {
                let chatMessage = ChatMessage.Builder()
                    .toIdentifier(serverAddress)
                    .fromIdentifier(account.generateAddress())
                    .withMessage("REQUEST_NEW_AUTH")
                    .build()
                
                try await account.send(message: chatMessage, onAcknowledgement: { messageId, error in
                    if let error = error {
                        print("SelfManager: ❌ Failed to send auth request: \(error)")
                        DispatchQueue.main.async {
                            self.authStatus = "❌ Auth request failed"
                        }
                    } else {
                        print("SelfManager: ✅ Auth request sent successfully")
                        DispatchQueue.main.async {
                            self.authStatus = "Waiting for auth challenge..."
                        }
                    }
                })
            } catch {
                print("SelfManager: ❌ Error sending auth request: \(error)")
                DispatchQueue.main.async {
                    self.authStatus = "❌ Error sending auth request"
                }
            }
        }
    }
    
    // Handle liveness authentication - from documentation example
    private func handleLivenessAuthentication(
        _ credentialRequest: CredentialRequest, 
        livenessCredentials: [Claim]
    ) {
        // Analyze liveness requirements from filtered credentials
        for claim in livenessCredentials {
            let claimTypes = claim.types()
            let subject = claim.subject()
            let comparisonOperator = claim.comparisonOperator()
            print("SelfManager: Processing liveness claim: types=\(claimTypes), subject=\(subject), operator=\(comparisonOperator)")
        }
        
        DispatchQueue.main.async {
            self.authStatus = "Launching authentication liveness..."
        }
        
        // Present liveness check flow on main thread - from documentation example
        DispatchQueue.main.async {
            print("SelfManager: Launching authentication liveness on main thread")
            
            SelfSDK.showLiveness(
                account: self.account, 
                showIntroduction: false, 
                autoDismiss: true
            ) { [weak self] selfieImageData, credentials, error in
                guard let self = self else { return }
                
                if let error = error {
                    print("SelfManager: ❌ Authentication liveness failed: \(error)")
                    DispatchQueue.main.async {
                        self.authStatus = "❌ Authentication liveness failed"
                    }
                    return
                }
                
                guard !credentials.isEmpty else {
                    print("SelfManager: ❌ No credentials from authentication liveness")
                    DispatchQueue.main.async {
                        self.authStatus = "❌ No credentials from liveness"
                    }
                    return
                }
                
                print("SelfManager: ✅ Authentication liveness successful")
                DispatchQueue.main.async {
                    self.authStatus = "Sending credential response..."
                }
                
                // Send credential response - from documentation example
                Task {
                    await self.sendCredentialResponse(
                        to: credentialRequest,
                        credentials: credentials
                    )
                }
            }
        }
    }
    
    // Send credential response for successful liveness check - from documentation example
    private func sendCredentialResponse(
        to request: CredentialRequest, 
        credentials: [Any]
    ) async {
        // Convert credentials to proper type
        let selfCredentials = credentials.compactMap { $0 as? Credential }
        
        guard !selfCredentials.isEmpty else {
            print("SelfManager: ❌ No valid credentials to send")
            DispatchQueue.main.async {
                self.authStatus = "❌ No valid credentials"
            }
            return
        }
        
        do {
            let response = CredentialResponse.Builder()
                .withRequestId(request.id())
                .withTypes(request.types())
                .toIdentifier(request.toIdentifier())
                .withStatus(ResponseStatus.accepted)
                .withCredentials(selfCredentials)
                .build()
            
            try await account.send(message: response) { messageId, error in
                if let error = error {
                    print("SelfManager: ❌ Response failed: \(error)")
                } else {
                    print("SelfManager: ✅ Credentials sent successfully")
                }
            }
            
            DispatchQueue.main.async {
                self.authStatus = "✅ Authentication completed!"
                self.currentStep = .completed
            }
            
        } catch {
            print("SelfManager: ❌ Error building credential response: \(error)")
            DispatchQueue.main.async {
                self.authStatus = "❌ Response send failed"
            }
        }
    }
} 
