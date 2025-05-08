//
//  ExampleApp.swift
//  Example
//
//  Created by Long Pham on 8/5/25.
//

import SwiftUI
import self_ios_sdk

@main
struct ExampleApp: App {
    
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
        }
    }
}
