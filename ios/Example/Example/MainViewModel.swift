//
//  MainViewModel.swift
//  Example
//
//  Created by Long Pham on 8/5/25.
//

import Foundation
import Combine
import self_ios_sdk

struct CredentialItem: Identifiable {
    var id: String = UUID().uuidString
    var claims: [Claim]
}

final class MainViewModel: ObservableObject {
    @Published var isOnboardingCompleted: Bool = false
    
    let account: Account
    @Published var accountRegistered: Bool = false
    @Published var isInitialized: Bool = false
    
    @Published var isConnecting = true
    @Published var connectionError: String? = nil
    @Published var hasTimedOut = false
    
    init() {
        // Initialize SDK
        SelfSDK.initialize()
        
        account = Account.Builder()
            .withEnvironment(Environment.production)
            .withSandbox(true) // if true -> production
            .withGroupId("") // ex: com.example.app.your_app_group
            .withStoragePath(FileManager.storagePath)
            .build()
        
        // add listener
        account.setOnInfoRequest { (key: String) in
            print("setOnInfoRequest: \(key)")
        }

        account.setOnInfoResponse { (address: String, data: [String: Any]) in
            print("setOnInfoResponse: \(address)/\(data)")
        }

        account.setOnStatusListener { status in
            print("init account status:\(status)")
            // reload credentials view
            Task { @MainActor in
                self.accountRegistered = self.account.registered()
            }
            self.reloadCredentialItems()
        }

        account.setOnRelayConnectListener {
            print("onRelayConnect connected.")
        }

        account.setOnMessageListener { message in
            print("Message received: \(message.id())")
            switch message {
            case is ChatMessage:
                let chatMessage = message as! ChatMessage

            case is Receipt:
                let receipt = message as! Receipt

            default:
                print("TODO: Handle For Message: \(message)")
                break
            }
        }

        account.setOnRequestListener { message in
            print("setOnRequestListener: \(message)")
            switch message {
            case is CredentialRequest:
                let credentialRequest = message as! CredentialRequest

            case is VerificationRequest:
                let verificationRequest = message as! VerificationRequest
                self.handleVerificationRequest(verificationRequest: verificationRequest)

            case is SigningRequest:
                let signingRequest = message as! SigningRequest
                print("Received signing request: \(signingRequest.id())")
                self.handleSigningRequest(signingRequest: signingRequest)

            default:
                print("TODO: Handle For Request: \(message)")
                break
            }
        }

        account.setOnResponseListener { message in
            print("setOnResponseListener: \(message)")
            switch message {
            case is CredentialResponse:
                let response = message as! CredentialResponse

            default:
                print("TODO: Handle For Response: \(message)")
                break;
            }
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            self.isInitialized = true
        }
    }
    
    // transform credential into credential item to perform identifiable to display inside a List
    @Published var credentialItems: [CredentialItem] = []
    func reloadCredentialItems() {
        Task { @MainActor in
            credentialItems = account.credentials().map { credential in
                CredentialItem(claims: credential.claims())
            }
        }
    }
    
    func registerAccount(completion: ((Bool) -> Void)? = nil) {
        SelfSDK.showLiveness(account: account, showIntroduction: true, autoDismiss: true, onResult: { selfieImageData, credentials, error in
            print("showLivenessCheck credentials: \(credentials)")
            self.register(selfieImageData: selfieImageData, credentials: credentials) { success in
                Task { @MainActor in
                    completion?(success)
                }
            }
        })
    }
    
    func handleAuthData(data: Data, completion: ((Error?) -> Void)? = nil) {
        Task(priority: .background) {
            do {
                let discoveryData = try await Account.qrCode(data: data)
                print("Discovery Data: \(discoveryData)")
                try await account.connectWith(qrCode: data)
                Task { @MainActor in
                    completion?(nil)
                }
            } catch {
                print("Handle data error: \(error)")
                Task { @MainActor in
                    completion?(error)
                }
            }
        }
    }
    
    func handleSigningRequest(signingRequest: SigningRequest) {
        // assume we will accept the request immediately
        
        self.respondToSigningRequest(signingRequest: signingRequest, status: .accepted, credentials: [])
    }
    
    func respondToSigningRequest(signingRequest: SigningRequest, status: ResponseStatus, credentials: [Credential]) {
        print("respondToSigningRequest: \(signingRequest.id()) -> status: \(status)")
        
        let signingResponse = SigningResponse.Builder()
            .withRequestId(signingRequest.id())
            .toIdentifier(signingRequest.toIdentifier())
            .fromIdentifier(signingRequest.fromIdentifier())
            .withStatus(status)
            .withCredentials(credentials)
            .build()

        Task(priority: .background, operation: {
            try await self.account.send(message: signingResponse, onAcknowledgement: {msgId, error in
                print("sent signing response with id: \(msgId) error: \(error)")
            })
        })
    }
    
    func handleVerificationRequest(verificationRequest: VerificationRequest) {
        // assume we will accept the request immediately
        
        self.respondToVerificationRequest(verificationRequest: verificationRequest, status: .accepted, credentials: [])
    }
    
    func respondToVerificationRequest(verificationRequest: VerificationRequest, status: ResponseStatus, credentials: [Credential]) {
        print("respondToVerificationRequest: \(verificationRequest.id()) -> status: \(status)")
        
        let verificationResponse = VerificationResponse.Builder()
            .withRequestId(verificationRequest.id())
            .toIdentifier(verificationRequest.toIdentifier())
            .fromIdentifier(verificationRequest.fromIdentifier())
            .withTypes(verificationRequest.types())
            .withStatus(status)
            .withCredentials(credentials)
            .build()

        Task(priority: .background, operation: {
            try await self.account.send(message: verificationResponse, onAcknowledgement: {msgId, error in
                print("sent verification response with id: \(msgId) error: \(error)")
            })
        })
    }
    
    func lfcFlow() {
        SelfSDK.showLiveness(account: account, showIntroduction: true, autoDismiss: true, onResult: { selfieImageData, credentials, error in
            print("showLivenessCheck credentials: \(credentials)")
            //self.register(selfieImageData: selfieImageData, credentials: credentials)
        })
    }
    
    private func register(selfieImageData: Data, credentials: [Credential], completion: ((Bool) -> Void)? = nil) {
        Task(priority: .background) {
            do {
                let success = try await account.register(selfieImage: selfieImageData, credentials: credentials)
                print("Register account: \(success)")
                completion?(success)
            } catch let error {
                print("Register Error: \(error)")
                completion?(false)
            }
        }
    }
    
    // MARK: - Server connection
    func connectToSelfServer(serverAddress: String, completion: @escaping((Bool) -> Void)) async {
        print("🌐 ServerConnectionProcessing: Connecting to Self server with address: \(serverAddress)")
        
        // Check if we already timed out
        if !isConnecting {
            print("🌐 ServerConnectionProcessing: Connection attempt cancelled due to timeout")
            return
        }
        
        do {
            let connectionResult = try await account.connectWith(address: serverAddress, info: [:])

            DispatchQueue.main.async {
                // Only proceed if we haven't timed out
                if self.isConnecting {
                    print("🌐 ServerConnectionProcessing: ✅ Successfully connected to server")
                    print("🌐 ServerConnectionProcessing: Connection result: \(connectionResult)")

                    //currentStep = 4 // Completed
                    self.isConnecting = false

                    // Wait a moment to show completion, then navigate
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                      //  onConnectionComplete()
                        completion(true)
                    }
                }
            }

        } catch {
            print("🌐 ServerConnectionProcessing: ❌ Failed to connect to server: \(error)")

            DispatchQueue.main.async {
                // Only set error if we haven't already timed out
                if self.isConnecting {
                    self.connectionError = "Failed to connect: \(error.localizedDescription)"
                    self.isConnecting = false
                }
            }
        }
    }
    
    
    // MARK: - Backup & Restore
    func backup(completion: ((URL?) -> Void)? = nil) {
        Task (priority: .background) {
            guard let backupFile = await account.backup() else {
                completion?(nil)
                return
            }
            print("Backup file: \(backupFile)")
            Task { @MainActor in
                completion?(backupFile)
            }
        }
    }
    
    
    func restore(selfieData: Data, backupFile: URL, completion: ((Bool) -> Void)? = nil) {
        Task (priority: .background) {
            do {
                let credentials = try await account.restore(backupFile: backupFile, selfieImage: selfieData)
                print("Restore complete with error: \(credentials.count)")
                if credentials.count > 0  {
                    // register sandbox
                } else {
                    Task { @MainActor in
                        completion?(false)
                    }
                }
            } catch {
                
            }
            
        }
    }
    
    func accountIsRegistered() -> Bool {
        return account.registered()
    }
}

extension FileManager {
    static var storagePath: String {
        let storagePath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("account1")
        createFolderAtURL(url: storagePath) // create folder if needed
        print("StoragePath: \(storagePath)")
        return storagePath.path()
    }
    
    static func createFolderAtURL(url: URL) {
        if FileManager.default.fileExists(atPath: url.path()) {
            print("Folder already exists: \(url)")
            
        } else {
            // Create the folder
            do {
                try FileManager.default.createDirectory(at: url, withIntermediateDirectories: true, attributes: nil)
                print("Folder created successfully: \(url)")
            } catch {
                print("Error creating folder: \(error.localizedDescription)")
            }
        }
    }
}

