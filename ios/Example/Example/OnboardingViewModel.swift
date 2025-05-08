//
//  OnboardingViewModel.swift
//  Example
//
//  Created by Long Pham on 8/5/25.
//

import Foundation
import Combine
import self_ios_sdk

final class OnboardingViewModel: ObservableObject {
    @Published var isOnboardingCompleted: Bool = false
    
    let account = Account.Builder()
        .withEnvironment(Environment.preview)
        .withSandbox(true) // if true -> production
        .withGroupId("") // ex: com.example.app.your_app_group
        .withStoragePath(FileManager.storagePath)
        .build()
    
    func lfcFlow() {
        SelfSDK.showLiveness(account: account, showIntroduction: true, autoDismiss: true, onResult: { selfieImageData, credentials, error in
            print("showLivenessCheck credentials: \(credentials)")
            self.register(selfieImageData: selfieImageData, credentials: credentials)
        })
    }
    
    private func register(selfieImageData: Data, credentials: [Credential], completion: ((Bool) -> Void)? = nil) {
        Task(priority: .background) {
            do {
                let success = try await account.register(selfieImage: selfieImageData, credentials: credentials)
                print("Register account: \(success)")
            } catch let error {
                print("Register Error: \(error)")
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

