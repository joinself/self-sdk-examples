//
//  ContentView.swift
//  Example
//
//  Created by Long Pham on 16/6/25.
//


//
//  ContentView.swift
//  ios-client
//
//  Created by Jason Reid on 06/06/2025.
//

import SwiftUI
import ui_components
import self_ios_sdk

struct ContentView: View {
    
    @EnvironmentObject var viewModel: MainViewModel
    @State private var currentScreen: AppScreen = .initialization
    @State private var initializedAccount: Account? = nil
    // Note: UserDefaults are automatically cleared when app is uninstalled (iOS sandbox behavior)
    @State private var isServerConnected: Bool = UserDefaults.standard.bool(forKey: "isServerConnected")
    @State private var connectedServerAddress: String? = UserDefaults.standard.string(forKey: "connectedServerAddress")
    
    // Track whether to show connection success toast on ActionSelectionScreen
    @State private var showConnectionSuccessToast: Bool = false
    
    // Overlay state for server requests (auth, doc signing, etc.)
    @State private var showServerRequestOverlay: Bool = false
    @State private var overlayMessage: String = ""
    @State private var serverRequestTimeoutTask: Task<Void, Never>? = nil
    @State private var showToast: Bool = false
    @State private var toastMessage: String = ""
    
    // Current credential request (needed to send response back)
    @State private var currentCredentialRequest: CredentialRequest? = nil
    
    // Current verification request (needed to send response back)
    @State private var currentVerificationRequest: VerificationRequest? = nil
    
    @State private var showVerifyEmail: Bool = false
    @State private var showVerifyDocument: Bool = false
    
    // backup & restore
    @State private var showDocumentPicker = false
    @State private var selectedFileName: String?
    @State private var selectedFileURLs: [URL] = []
    @State private var fileToShareURLs: [URL] = []
    @State private var showShareSheet = false
    
    
    enum AppScreen {
        case initialization
        case registrationIntro
        case serverConnection
        case serverConnectionProcessing(serverAddress: String)
        case actionSelection
        case verifyCredential
        case verifyEmailStart
        case verifyEmailResult
        case verifyDocumentStart
        case verifyDocumentResult
        case shareCredential
        case shareEmailStart
        case shareEmailResult(success: Bool)
        case shareDocumentStart
        case shareDocumentResult(success: Bool)
        case authStart
        case authResult
        case docSignStart
        case docSignResult(success: Bool)
        case backupStart
        case backupResult(success: Bool)
        case restoreStart
        case restoreResult(success: Bool)
    }
    
    var body: some View {
        ZStack {
            Group {
                switch currentScreen {
                case .initialization:
                    InitializeSDKScreen(isInitialized: $viewModel.isInitialized, onInitializationComplete: {
                        initializedAccount = viewModel.account
                        determineNextScreen(account: viewModel.account)
                    })
                case .registrationIntro:
                    RegistrationIntroScreen {
                        // start registration
                        viewModel.registerAccount { success in
                            viewModel.accountRegistered = success
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .serverConnection
                            }
                        }
                    } onRestore: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .restoreStart
                        }
                    }
                    
                case .serverConnection:
                    ServerConnectionScreen(
                        onConnectToServer: { serverAddress in
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .serverConnectionProcessing(serverAddress: serverAddress)
                            }
                        }
                    )
                case .serverConnectionProcessing(let serverAddress):
                    ServerConnectionProcessingScreen(
                        isConnecting: $viewModel.isConnecting,
                        connectionError: $viewModel.connectionError,
                        hasTimedOut: $viewModel.hasTimedOut,
                        serverAddress: serverAddress,
                        onConnectionStart: { serverAddress in
                            print("onConnectionStart: \(serverAddress)")
                            Task {
                                await viewModel.connectToSelfServer(serverAddress: serverAddress) { success in
                                    // connection completion
                                    if success {
                                        // Update server connection state and navigate to action selection
                                        isServerConnected = true
                                        connectedServerAddress = serverAddress
                                        // UserDefaults are app-sandboxed and automatically cleared on app uninstall
                                        UserDefaults.standard.set(true, forKey: "isServerConnected")
                                        UserDefaults.standard.set(serverAddress, forKey: "connectedServerAddress")
                                        // Set up message listener for incoming requests
                                        setupMessageListener()
                                        // Show success toast since this is first visit after connection
                                        showConnectionSuccessToast = true
                                        withAnimation(.easeInOut(duration: 0.5)) {
                                            currentScreen = .actionSelection
                                        }
                                    }
                                }
                            }
                            
                        },
                        onConnectionComplete: {
                            // Update server connection state and navigate to action selection
                            isServerConnected = true
                            connectedServerAddress = serverAddress
                            // UserDefaults are app-sandboxed and automatically cleared on app uninstall
                            UserDefaults.standard.set(true, forKey: "isServerConnected")
                            UserDefaults.standard.set(serverAddress, forKey: "connectedServerAddress")
                            // Set up message listener for incoming requests
                            setupMessageListener()
                            // Show success toast since this is first visit after connection
                            showConnectionSuccessToast = true
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .actionSelection
                            }
                        },
                        onGoBack: {
                            // Reset connection state and go back to server connection screen
                            isServerConnected = false
                            connectedServerAddress = nil
                            UserDefaults.standard.set(false, forKey: "isServerConnected")
                            UserDefaults.standard.removeObject(forKey: "connectedServerAddress")
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .serverConnection
                            }
                        }
                    )
                case .actionSelection:
                    ActionSelectionScreen(
                        showConnectionSuccess: showConnectionSuccessToast,
                        onActionSelected: { actionType in
                            print("🎯 ContentView: User selected action: \(actionType)")
                            // Reset the connection success toast flag after first visit
                            showConnectionSuccessToast = false
                            handleActionSelection(actionType)
                        }
                    )
                    
                case .verifyCredential:
                    VerifyCredentialSelectionScreen { credentialActionType in
                        if credentialActionType == .emailAddress {
                            // show verify email flow
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .verifyEmailStart
                            }
                        } else if credentialActionType == .identityDocument {
                            // show verify document flow
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .verifyDocumentStart
                            }
                        }
                        
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                    
                case .verifyEmailStart:
                    VerifyEmailStartScreen {
                        showVerifyEmail = true
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                    .fullScreenCover(isPresented: $showVerifyEmail, onDismiss: {
                        
                    }, content: {
                        EmailFlow(account: viewModel.account, autoDismiss: false, onResult: { success in
                            print("Verify email finished = \(success)")
                            self.showVerifyEmail = false
                            if success {
                                withAnimation(.easeInOut(duration: 0.5)) {
                                    currentScreen = .verifyEmailResult
                                }
                            } else {
                                withAnimation(.easeInOut(duration: 0.5)) {
                                    currentScreen = .actionSelection
                                }
                            }
                        })
                    })
                    
                case .verifyEmailResult:
                    VerifyEmailResultScreen {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }

                case .verifyDocumentStart:
                    VerifyDocumentStartScreen {
                        showVerifyDocument = true
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                    .fullScreenCover(isPresented: $showVerifyDocument, onDismiss: {
                        // dismiss view
                    }, content: {
                        // MARK: - Verify Documents
                        DocumentFlow(account: viewModel.account, devMode: true, autoCaptureImage: false, onResult:  { success in
                            print("Verify document finished: \(success)")
                            showVerifyDocument = false
                            if success {
                                withAnimation(.easeInOut(duration: 0.5)) {
                                    currentScreen = .verifyDocumentResult
                                }
                            } else {
                                withAnimation(.easeInOut(duration: 0.5)) {
                                    currentScreen = .actionSelection
                                }
                            }
                        })
                    })
                    
                case .verifyDocumentResult:
                    VerifyDocumentResultScreen {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }

                    
                case .shareCredential:
                    ProvideCredentialSelectionScreen { credentialActionType in
                        if credentialActionType == .emailAddress {
                            self.sendEmailCredentialRequest()
                        } else if credentialActionType == .identityDocument {
                            self.sendIDNumberCredentialRequest()
                        } else if credentialActionType == .customCredential {
                            // TODO: GET Custom Credential
                            
                        }
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                case .shareEmailStart:
                    ShareEmailCredentialScreen(credentialName: "Email") {
                        // approve
                        self.sendEmailCredentialResponse(responseStatus: .accepted)
                        // Navigate to result screen
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .shareEmailResult(success: true)
                        }
                    } onDeny: {
                        self.sendEmailCredentialResponse(responseStatus: .rejected)
                        // Navigate to result screen
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .shareEmailResult(success: false)
                        }
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }

                case .shareEmailResult(let success):
                    ShareEmailCredentialResultScreen(success: success) {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                
                case .shareDocumentStart:
                    ShareDocumentCredentialStartScreen(credentialName: "") {
                        viewModel.responseToCredentialRequest(credentialRequest: currentCredentialRequest, responseStatus: .accepted)
                    } onDeny: {
                        viewModel.responseToCredentialRequest(credentialRequest: currentCredentialRequest, responseStatus: .rejected)
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }

                    
                case .shareDocumentResult(let success):
                    ShareDocumentCredentialResultScreen(success: success) {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                    
                case .authStart:
                    AuthStartScreen(
                        onStartAuthentication: {
                            startAuthenticationLivenessCheck()
                        }
                    )
                case .authResult:
                    AuthResultScreen(
                        onContinue: {
                            // Return to action selection (don't show connection success toast)
                            showConnectionSuccessToast = false
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .actionSelection
                            }
                        }
                    )
                case .docSignStart:
                    DocSignStartScreen(
                        onSignDocument: {
                            startDocumentSigning()
                        },
                        onRejectDocument: {
                            rejectDocumentSigning()
                        }
                    )
                case .docSignResult(let success):
                    DocSignResultScreen(
                        success: success,
                        onContinue: {
                            // Return to action selection (don't show connection success toast)
                            showConnectionSuccessToast = false
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .actionSelection
                            }
                        }
                    )
                    
                    // MARK: Backup & Restore
                case .backupStart:
                    BackupAccountStartScreen {
                        viewModel.backup { backupFile in
                            // open share extension to save file
                            if let url = backupFile {
                                fileToShareURLs = [url]
                            }
                            
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .backupResult(success: backupFile != nil)
                            }
                        }
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                
                case .backupResult(let success):
                    BackupAccountResultScreen(success: success) {
                        // share backup file
                        showShareSheet = true
                    } onBack: {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            currentScreen = .actionSelection
                        }
                    }
                    .sheet(isPresented: $showShareSheet) {
                        ShareSheet(items: fileToShareURLs) {
                            self.showShareSheet = false
                            withAnimation(.easeInOut(duration: 0.5)) {
                                currentScreen = .actionSelection
                            }
                        }
                    }
                    
                case .restoreStart:
                    Text("RestoreStart")
                
                case .restoreResult(let success):
                    Text("RestoreResult: \(success)")
                }
            }
            
            // Server request overlay (blocks interaction while waiting for server response)
            if showServerRequestOverlay {
                Color.black.opacity(0.4)
                    .edgesIgnoringSafeArea(.all)
                    .onTapGesture {
                        // Block all interactions
                    }
                
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.5)
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    
                    Text(overlayMessage)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.white)
                }
            }
            
            // Toast notification
            if showToast {
                ToastMessageView(message: toastMessage)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }
    
    private func determineNextScreen(account: Account) {
        print("🎯 ContentView: Determining next screen based on account status...")
        
        // Check if account is registered
        let isRegistered = account.registered()
        print("🎯 ContentView: Account registered: \(isRegistered)")
        
        // Check stored connection state and validate it
        let storedConnectionState = UserDefaults.standard.bool(forKey: "isServerConnected")
        let storedServerAddress = UserDefaults.standard.string(forKey: "connectedServerAddress")
        print("🎯 ContentView: Stored server connection state: \(storedConnectionState)")
        print("🎯 ContentView: Stored server address: \(storedServerAddress ?? "nil")")
        
        // Validate connection state - both must be present for a valid connection
        let hasValidConnection = storedConnectionState && storedServerAddress != nil
        if storedConnectionState && storedServerAddress == nil {
            print("🎯 ContentView: ⚠️ Inconsistent state: connection marked as true but no server address. Resetting connection state.")
            // Reset inconsistent state
            UserDefaults.standard.set(false, forKey: "isServerConnected")
            UserDefaults.standard.removeObject(forKey: "connectedServerAddress")
        }
        
        isServerConnected = hasValidConnection
        connectedServerAddress = storedServerAddress
        print("🎯 ContentView: Final server connected state: \(isServerConnected)")
        print("🎯 ContentView: Final server address: \(connectedServerAddress ?? "nil")")
        
        withAnimation(.easeInOut(duration: 0.5)) {
            if isRegistered && isServerConnected {
                print("🎯 ContentView: Account registered AND server connected, navigating to ACTION_SELECTION")
                // Set up message listener if we have a stored server connection
                setupMessageListener()
                // Don't show connection success toast since user is already connected
                showConnectionSuccessToast = false
                currentScreen = .actionSelection
            } else if isRegistered {
                print("🎯 ContentView: Account registered but not connected to server, navigating to SERVER_CONNECTION")
                currentScreen = .serverConnection
            } else {
                print("🎯 ContentView: Account not registered, navigating to REGISTRATION_INTRO")
                currentScreen = .registrationIntro
            }
        }
    }
    
    // MARK: - Action Handling
    
    private func handleActionSelection(_ actionType: ActionType) {
        switch actionType {
        case .authenticate:
            handleAuthenticateAction()
        case .verifyCredentials:
            handleVerifyCredentials()
        case .provideCredentials:
//            print("🎯 ContentView: Provide Credentials selected (not implemented yet)")
//            showToastMessage("Provide Credentials feature coming soon!")
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .shareCredential
            }
        case .signDocuments:
            handleSignDocumentsAction()
            
        case .backup:
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .backupStart
            }
        @unknown default:
            fatalError()
        }
    }
    
    private func handleAuthenticateAction() {
        print("🔐 ContentView: Starting authentication flow...")
        
        // Show overlay and spinner
        showServerRequestOverlay = true
        overlayMessage = "Waiting for authentication request..."
        
        // Send authentication request message to server
        sendAuthenticationRequest()
        
        // Start 5-second timeout
        serverRequestTimeoutTask = Task {
            try? await Task.sleep(nanoseconds: 5_000_000_000) // 5 seconds
            
            await MainActor.run {
                if showServerRequestOverlay {
                    // Timeout occurred
                    print("🔐 ContentView: Authentication request timed out")
                    showServerRequestOverlay = false
                    showToastMessage("Authentication request timed out. Please try again.")
                }
            }
        }
    }
    
    private func sendAuthenticationRequest() {
        print("🔐 ContentView: Sending authentication request message to server...")
        
        guard let serverAddress = connectedServerAddress else {
            print("🔐 ContentView: ❌ Cannot send message - no server connected")
            showServerRequestOverlay = false
            showToastMessage("No server connected. Please connect to a server first.")
            return
        }
        
        // FIXME: Set server address to view model
        viewModel.serverAddress = serverAddress
        viewModel.notifyServerForRequest(message: SERVER_REQUESTS.REQUEST_CREDENTIAL_AUTH) { messageId, error in
            Task { @MainActor in
                if let error = error {
                    print("🔐 ContentView: ❌ Authentication request send failed: \(error)")
                    showServerRequestOverlay = false
                    showToastMessage("Failed to send authentication request: \(error.localizedDescription)")
                } else {
                    print("🔐 ContentView: ✅ Authentication request sent successfully with ID: \(messageId)")
                    // Message sent successfully, now waiting for server response via message listener
                }
            }
        }
    }
    
    private func sendEmailCredentialRequest() {
        print("🔐 ContentView: Sending email request message to server...")
        
        guard let serverAddress = connectedServerAddress else {
            print("🔐 ContentView: ❌ Cannot send message - no server connected")
            showServerRequestOverlay = false
            showToastMessage("No server connected. Please connect to a server first.")
            return
        }
        
        // FIXME: Set server address to view model
        viewModel.serverAddress = serverAddress
        viewModel.notifyServerForRequest(message: SERVER_REQUESTS.REQUEST_CREDENTIAL_EMAIL) { messageId, error in
            Task { @MainActor in
                if let error = error {
                    print("🔐 ContentView: ❌ Email credential request send failed: \(error)")
                    showServerRequestOverlay = false
                    showToastMessage("Failed to send email credential request: \(error.localizedDescription)")
                } else {
                    print("🔐 ContentView: ✅ Email credential request sent successfully with ID: \(messageId)")
                    // Message sent successfully, now waiting for server response via message listener
                }
            }
        }
    }
    
    private func sendIDNumberCredentialRequest() {
        print("🔐 ContentView: Sending ID Number credential request message to server...")
        
        guard let serverAddress = connectedServerAddress else {
            print("🔐 ContentView: ❌ Cannot send message - no server connected")
            showServerRequestOverlay = false
            showToastMessage("No server connected. Please connect to a server first.")
            return
        }
        
        // FIXME: Set server address to view model
        viewModel.serverAddress = serverAddress
        viewModel.notifyServerForRequest(message: SERVER_REQUESTS.REQUEST_CREDENTIAL_DOCUMENT) { messageId, error in
            Task { @MainActor in
                if let error = error {
                    print("🔐 ContentView: ❌ Authentication request send failed: \(error)")
                    showServerRequestOverlay = false
                    showToastMessage("Failed to send document credential request: \(error.localizedDescription)")
                } else {
                    print("🔐 ContentView: ✅ Document credential request sent successfully with ID: \(messageId)")
                    // Message sent successfully, now waiting for server response via message listener
                }
            }
        }
    }
    
    private func startAuthenticationLivenessCheck() {
        print("🔐 ContentView: Starting authentication liveness check with SelfUI")
        
        guard let account = initializedAccount else {
            showToastMessage("Authentication requires an active account")
            return
        }
        
        // Use SelfUI to perform liveness check
        SelfSDK.showLiveness(account: account) { data, credentials, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("🔐 ContentView: ❌ Authentication liveness check failed: \(error)")
                    showToastMessage("Authentication failed. Please try again.")
                    // Stay on current screen to allow retry
                } else {
                    print("🔐 ContentView: ✅ Authentication liveness check successful")
                    print("🔐 ContentView: Received \(credentials.count) credentials")
                    
                    // Send credential response back to server
                    sendCredentialResponse(account: account, credentials: credentials)
                    
                    // Navigate to result screen
                    withAnimation(.easeInOut(duration: 0.5)) {
                        currentScreen = .authResult
                    }
                }
            }
        }
    }
    
    // MARK: - Verify Credentials
    
    private func handleVerifyCredentials() {
        withAnimation(.easeInOut(duration: 0.5)) {
            currentScreen = .verifyCredential
        }
    }
    
    // MARK: - Document Signing Methods
    
    private func handleSignDocumentsAction() {
        print("📄 ContentView: Starting document signing flow...")
        
        guard let account = initializedAccount else {
            showToastMessage("Document signing requires an active account")
            return
        }
        
        // Show overlay and spinner
        showServerRequestOverlay = true
        overlayMessage = "Waiting for document signing request..."
        
        // Send document signing request message to server
        sendDocumentSigningRequest(account: account)
        
        // Start 5-second timeout
        serverRequestTimeoutTask = Task {
            try? await Task.sleep(nanoseconds: 5_000_000_000) // 5 seconds
            
            await MainActor.run {
                if showServerRequestOverlay {
                    // Timeout occurred
                    print("📄 ContentView: Document signing request timed out")
                    showServerRequestOverlay = false
                    showToastMessage("Document signing request timed out. Please try again.")
                }
            }
        }
    }
    
    private func sendDocumentSigningRequest(account: Account) {
        print("📄 ContentView: Sending document signing request message to server...")
        
        guard let serverAddress = connectedServerAddress else {
            print("📄 ContentView: ❌ Cannot send message - no server connected")
            showServerRequestOverlay = false
            showToastMessage("No server connected. Please connect to a server first.")
            return
        }
        
        Task {
            do {
                let chatMessage = ChatMessage.Builder()
                    .toIdentifier(serverAddress)
                    .fromIdentifier(account.generateAddress())
                    .withMessage("REQUEST_DOCUMENT_SIGNING")
                    .build()
                
                try await account.send(message: chatMessage, onAcknowledgement: { messageId, error in
                    Task { @MainActor in
                        if let error = error {
                            print("📄 ContentView: ❌ Document signing request send failed: \(error)")
                            showServerRequestOverlay = false
                            showToastMessage("Failed to send document signing request: \(error.localizedDescription)")
                        } else {
                            print("📄 ContentView: ✅ Document signing request sent successfully with ID: \(messageId)")
                            // Message sent successfully, now waiting for server response via message listener
                        }
                    }
                })
            } catch {
                print("📄 ContentView: ❌ Failed to build document signing request: \(error)")
                showServerRequestOverlay = false
                showToastMessage("Failed to build document signing request: \(error.localizedDescription)")
            }
        }
    }
    
    private func startDocumentSigning() {
        print("📄 ContentView: Starting document signing with Self SDK")
        
        guard let account = initializedAccount else {
            showToastMessage("Document signing requires an active account")
            return
        }
        
        // For now, simulate signing process
        // TODO: Implement actual document signing using Self SDK
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            print("📄 ContentView: ✅ Document signed successfully")
            
            // Send verification response back to server
            sendVerificationResponse(account: account, accepted: true)
            
            // Navigate to success result screen
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .docSignResult(success: true)
            }
        }
    }
    
    private func rejectDocumentSigning() {
        print("📄 ContentView: User rejected document signing")
        
        guard let account = initializedAccount else {
            showToastMessage("Document signing requires an active account")
            return
        }
        
        // Send rejection to server
        sendVerificationResponse(account: account, accepted: false)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            print("📄 ContentView: ✅ Document signing rejection sent to server")
            // Navigate to rejection result screen
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .docSignResult(success: false)
            }
        }
    }
    
    private func sendEmailCredentialResponse(responseStatus: ResponseStatus = .accepted) {
        guard let account = initializedAccount else {
            print("Account is nil.")
            return
        }
        
        guard let credentialRequest = currentCredentialRequest else {
            print("🔐 ContentView: ❌ Cannot send credential response - no stored credential request")
            return
        }
        
        
        let storedCredentials = account.lookUpCredentials(claims: credentialRequest.details())
        
        let credentialResponse = CredentialResponse.Builder()
            .withRequestId(credentialRequest.id())
            .withTypes(credentialRequest.types())
            .toIdentifier(credentialRequest.toIdentifier())
            .withStatus(responseStatus)
            .withCredentials(storedCredentials)
            .build()
        viewModel.sendKMPMessage(message: credentialResponse) { messageId, error in
        }
    }
    
    private func sendCredentialResponse(account: Account, credentials: [Credential]) {
        print("🔐 ContentView: Sending credential response back to server...")
        
        guard let credentialRequest = currentCredentialRequest else {
            print("🔐 ContentView: ❌ Cannot send credential response - no stored credential request")
            return
        }
        
        Task {
            do {
                let credentialResponse = CredentialResponse.Builder()
                    .withRequestId(credentialRequest.id())
                    .withTypes(credentialRequest.types())
                    .toIdentifier(credentialRequest.toIdentifier())
                    .withStatus(ResponseStatus.accepted)
                    .withCredentials(credentials)
                    .build()
                
                try await account.send(message: credentialResponse, onAcknowledgement: { messageId, error in
                    Task { @MainActor in
                        if let error = error {
                            print("🔐 ContentView: ❌ Credential response send failed: \(error)")
                            showToastMessage("Failed to send authentication response: \(error.localizedDescription)")
                        } else {
                            print("🔐 ContentView: ✅ Credential response sent successfully with ID: \(messageId)")
                            // Clear the stored request since we've responded to it
                            currentCredentialRequest = nil
                        }
                    }
                })
            } catch {
                print("🔐 ContentView: ❌ Failed to build credential response: \(error)")
                showToastMessage("Failed to build authentication response: \(error.localizedDescription)")
            }
        }
    }
    
    private func sendVerificationResponse(account: Account, accepted: Bool) {
        print("📄 ContentView: Sending verification response back to server...")
        
        guard let verificationRequest = currentVerificationRequest else {
            print("📄 ContentView: ❌ Cannot send verification response - no stored verification request")
            return
        }
        
        Task {
            do {
                let status = accepted ? ResponseStatus.accepted : ResponseStatus.rejected
                let verificationResponse = VerificationResponse.Builder()
                    .withRequestId(verificationRequest.id())
                    .withTypes(verificationRequest.types())
                    .toIdentifier(verificationRequest.toIdentifier())
                    .fromIdentifier(verificationRequest.fromIdentifier())
                    .withStatus(status)
                    .build()
                
                try await account.send(message: verificationResponse, onAcknowledgement: { messageId, error in
                    Task { @MainActor in
                        if let error = error {
                            print("📄 ContentView: ❌ Verification response send failed: \(error)")
                            showToastMessage("Failed to send document signing response: \(error.localizedDescription)")
                        } else {
                            print("📄 ContentView: ✅ Verification response sent successfully with ID: \(messageId)")
                            // Clear the stored request since we've responded to it
                            currentVerificationRequest = nil
                        }
                    }
                })
            } catch {
                print("📄 ContentView: ❌ Failed to build verification response: \(error)")
                showToastMessage("Failed to build document signing response: \(error.localizedDescription)")
            }
        }
    }
    
    // MARK: - Message Handling
    
    private func setupMessageListener() {
        guard let account = initializedAccount else {
            print("🎯 ContentView: ⚠️ Cannot setup message listener - no account available")
            return
        }
        
        print("🎯 ContentView: 📚 Setting up message listener...")
        
        // Set up request listener for credential and verification requests
        account.setOnRequestListener { request in
            Task { @MainActor in
                handleIncomingRequest(request)
            }
        }
        
        // Also set up message listener for other message types
        account.setOnMessageListener { message in
            Task { @MainActor in
                handleIncomingMessage(message)
            }
        }
        
        print("🎯 ContentView: ✅ Message listeners configured successfully")
    }
    
    private func handleIncomingRequest(_ request: Any) {
        print("🎯 ContentView: 📥 Received incoming request of type: \(type(of: request))")
        
        if let credentialRequest = request as? CredentialRequest {
            handleIncomingCredentialRequest(credentialRequest)
        } else if let verificationRequest = request as? VerificationRequest {
            handleIncomingVerificationRequest(verificationRequest)
        } else {
            print("🎯 ContentView: ❓ Unknown request type: \(type(of: request))")
        }
    }
    
    private func handleIncomingMessage(_ message: Any) {
        print("🎯 ContentView: 📥 Received incoming message of type: \(type(of: message))")
        
        if let chatMessage = message as? ChatMessage {
            handleIncomingChatMessage(chatMessage)
        } else {
            print("🎯 ContentView: ❓ Unknown message type: \(type(of: message))")
        }
    }
    
    private func handleIncomingCredentialRequest(_ credentialRequest: CredentialRequest) {
        let fromAddress = credentialRequest.fromIdentifier()
        print("🎯 ContentView: 🎫 Credential request from \(fromAddress)")
        
        // Store the credential request so we can respond to it later
        currentCredentialRequest = credentialRequest
        
        // check credential request type
        
        // Cancel timeout if waiting for auth request
        serverRequestTimeoutTask?.cancel()
        
        // Hide overlay and navigate to AUTH_START
        showServerRequestOverlay = false
        let emailCredential = credentialRequest.details().first?.types().contains(CredentialType.Email) ?? false
        let documentCredential = credentialRequest.details().first?.types().contains(CredentialType.Passport) ?? false
        
        if emailCredential {
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .shareEmailStart
            }
        } else if documentCredential {
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .shareDocumentStart
            }
        }else {
            withAnimation(.easeInOut(duration: 0.5)) {
                currentScreen = .authStart
            }
        }
    }
    
    
    
    private func handleIncomingVerificationRequest(_ verificationRequest: VerificationRequest) {
        let fromAddress = verificationRequest.fromIdentifier()
        print("🎯 ContentView: 📝 Verification request from \(fromAddress)")
        
        // Store the verification request so we can respond to it later
        currentVerificationRequest = verificationRequest
        
        // Cancel timeout if waiting for doc signing request
        serverRequestTimeoutTask?.cancel()
        
        // Hide overlay and navigate to DOC_SIGN_START
        showServerRequestOverlay = false
        withAnimation(.easeInOut(duration: 0.5)) {
            currentScreen = .docSignStart
        }
    }
    
    private func handleIncomingChatMessage(_ chatMessage: ChatMessage) {
        let messageContent = chatMessage.message()
        let fromAddress = chatMessage.fromIdentifier()
        print("🎯 ContentView: 💬 Chat message from \(fromAddress): '\(messageContent)'")
        // Chat messages are informational, no specific action needed
    }
    
    private func showToastMessage(_ message: String) {
        toastMessage = message
        showToast = true
        
        // Auto-hide toast after 3 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            withAnimation(.easeOut(duration: 0.5)) {
                showToast = false
            }
        }
    }
    
    // MARK: - State Management & Data Persistence
    
    /// Clears all app-specific state.
    ///
    /// **Data Persistence & App Uninstall Behavior:**
    /// - UserDefaults: Automatically cleared on app uninstall (iOS sandbox behavior)
    /// - Self SDK storage: Uses app Documents directory, automatically cleared on uninstall
    /// - Self SDK Keychain items: Managed by Self SDK with appropriate accessibility settings
    ///   (typically cleared on uninstall, but may depend on SDK configuration)
    ///
    /// This method can be used for development/testing or explicit reset functionality.
    /// It only clears app-level state, not Self SDK internal state (keys, credentials).
    private func clearAllAppState() {
        print("🧹 ContentView: Clearing all app-specific persistent state")
        UserDefaults.standard.removeObject(forKey: "isServerConnected")
        UserDefaults.standard.removeObject(forKey: "connectedServerAddress")
        isServerConnected = false
        connectedServerAddress = nil
        
        // Reset to initial screen
        currentScreen = .initialization
        initializedAccount = nil
    }
}

//#Preview {
//    ContentView()
//}
