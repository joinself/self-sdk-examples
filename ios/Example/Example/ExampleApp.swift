//
//  ExampleApp.swift
//  Example
//
//  Created by Long Pham on 8/5/25.
//

import SwiftUI
import self_ios_sdk
import ui_components

@main
struct ExampleApp: App {
    
    @ObservedObject var viewModel: MainViewModel = MainViewModel()
    
    init () {
        SelfSDK.initialize {
            return "FILL_YOUR_PUSH_TOKEN_HERE"
        } log: { log in
            print("Log: \(log)")
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
//            if viewModel.accountRegistered {
//                MainContentView()
//                    .environmentObject(viewModel)
//            } else if !viewModel.isInitialized {
//                                
//                InitializeSDKScreen(isInitialized: $viewModel.isInitialized) {
//                    print("onInitializationComplete....")
//                }
//                
//            } else {
//                RegistrationIntroScreen(
//                    onRegistrationComplete: {
//                        print("Preview: Registration complete")
//                    }) {
//                        // start registration
//                        viewModel.registerAccount { success in
//                            viewModel.accountRegistered = success
//                        }
//                    }
//            }
        }
    }
}
