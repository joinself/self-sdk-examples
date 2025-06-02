//
//  SelfAuthManager.swift
//  ExampleApp
//
//  Created by Long Pham on 29/5/25.
//

import SwiftUI
import self_ios_sdk

// Production-ready Self SDK authentication manager
@MainActor
class SelfAuthManager: ObservableObject {
    private let account: Account
    
    init() {
        // Setup secure storage path
        let fileManager = FileManager.default
        let documentsDirectory = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let storageURL = documentsDirectory.appendingPathComponent("selfsdk_storage")
        
        // Ensure storage directory exists
        try? fileManager.createDirectory(at: storageURL, withIntermediateDirectories: true)
        
        // Initialize account
        self.account = Account.Builder()
            .withEnvironment(Environment.preview) // Use .production for live apps
            .withSandbox(true) // Set to false for production
            .withStoragePath(storageURL.path)
            .build()
        
        setupEventListeners()
        SelfSDK.initialize()
    }
    
    private func setupEventListeners() {
        // 1. Handle credential request in your request listener
        account.setOnRequestListener { [weak self] requestMessage in
            guard let self = self else { return }
            
            if let credentialRequest = requestMessage as? CredentialRequest {
                print("Credential request received from: \(credentialRequest.fromIdentifier())")
                
                Task { @MainActor in
                    await self.handleCredentialRequest(credentialRequest)
                }
            }
        }
    }
    
    // 2. Handle credential request with proper filtering and thread safety
    private func handleCredentialRequest(_ credentialRequest: CredentialRequest) async {
        // Check all requested credentials - filter for liveness credentials
        let allClaims: [Claim] = credentialRequest.details()
        let livenessCredentials = allClaims.filter {
            $0.types().contains(CredentialType.Liveness) &&
            $0.types().contains(CredentialType.Verifiable)
        }
        let otherCredentials = allClaims.filter {
            !($0.types().contains(CredentialType.Liveness) &&
              $0.types().contains(CredentialType.Verifiable))
        }
        
        print("Total credentials requested: \(allClaims.count)")
        print("Liveness credentials: \(livenessCredentials.count)")
        print("Other credentials: \(otherCredentials.count)")
        
        // Log other credential types for debugging
        for claim in otherCredentials {
            print("Ignoring unsupported credential types: \(claim.types())")
        }
        
        if !livenessCredentials.isEmpty {
            print("✅ Processing liveness credentials only")
            await handleLivenessAuthentication(credentialRequest, livenessCredentials: livenessCredentials)
        } else {
            print("❌ No liveness credentials found - this app only supports liveness authentication")
        }
    }
    
    // 3. Handle liveness authentication with proper thread management
    private func handleLivenessAuthentication(
        _ credentialRequest: CredentialRequest,
        livenessCredentials: [Claim]
    ) async {
        // Analyze liveness requirements from filtered credentials
        for claim in livenessCredentials {
            let claimTypes = claim.types()
            let subject = claim.subject()
            let comparisonOperator = claim.comparisonOperator()
            print("Processing liveness claim: types=\(claimTypes), subject=\(subject), operator=\(comparisonOperator)")
        }
        
        // Present liveness check flow on main thread (critical for UI)
        SelfSDK.showLiveness(
            account: account,
            showIntroduction: false,
            autoDismiss: true
        ) { [weak self] selfieImageData, credentials, error in
            guard let self = self else { return }
            
            if let error = error {
                print("Liveness check failed: \(error)")
                return
            }
            
            guard !credentials.isEmpty else {
                print("No credentials provided from liveness check")
                return
            }
            
            // Send credential response
            Task { @MainActor in
                await self.sendCredentialResponse(
                    to: credentialRequest,
                    credentials: credentials
                )
            }
        }
    }
    
    // 4. Send credential response with proper type conversion
    private func sendCredentialResponse(
        to request: CredentialRequest,
        credentials: [Any]
    ) async {
        // Convert credentials to proper type (critical for Swift type safety)
        let selfCredentials = credentials.compactMap { $0 as? Credential }
        
        guard !selfCredentials.isEmpty else {
            print("No valid credentials to send")
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
                    print("❌ Response failed: \(error)")
                } else {
                    print("✅ Credentials sent successfully")
                }
            }
        } catch {
            print("Error building credential response: \(error)")
        }
    }
}
