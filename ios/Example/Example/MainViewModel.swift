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
    init() {
        account = Account.Builder()
            .withEnvironment(Environment.preview)
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
            self.accountRegistered = self.account.registered()
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

            case is SigningRequest:
                let signingRequest = message as! SigningRequest

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
    }
    
    @Published var accountRegistered: Bool = false
    
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

