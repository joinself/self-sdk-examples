
# Self ExampleApp

**Self ExampleApp** is an iOS application showcasing the integration of the **SelfSDK**. It serves as an example for developers, demonstrating how to implement various use cases of the SelfSDK, such as authentication, verify document, sharing verified credentials like email addresses, ID numbers...  
The application is built using modern iOS development practices, featuring a user-friendly interface created with **SwiftUI**. It provides a practical guide for developers looking to incorporate decentralized identity features into their own iOS applications.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Build](#build)
- [Using the App](#using-the-app)

## Features
Main features of the app:

   - Setup account and connect with server
   - Authenticate with server using biometric authentication
   - Verify email address, passport/id card, and custom credentials
   - Share verified email credentials, verified document credentials, and verified custom credentials
   - Back up and restore account's data

## Prerequisites

Before you begin, ensure you have met the following requirements:

   - Xcode Version 16.2 and above

## Setup

To get a local copy up and running, follow these simple steps:   
Clone the repository:
```bash
git clone git@github.com:joinself/self-sdk-examples.git
```

## Build
You can open the iOS example from this below commands.

```bash
cd ios/Example/
open Example.xcodeproj
```

## Setup Server

For the Self Demo application to function fully (e.g., to connect for credential verification and sharing), a backend server component needs to be running. 

Follow these steps to set up and run the server from the command line, then copy the address for mobile app to connect.
  
```bash
cd self-sdk-examples/java

./gradlew :self-demo:run
```

Or run with docker image

```bash
docker run -it ghcr.io/joinself/self-sdk-demo:latest
```

## Using the App

This section guides you through the initial steps to get started with the Self Demo application.

1.  **Initialize the SDK:**  
   Upon first launch, the application will typically initialize the SelfSDK in the background. This process sets up the necessary components for the app to interact with the Self network.

2.  **Register an Account:**  
   Once the SDK is initialized, if you are a new user, you will be prompted to register an account or restore a backup file.

3.  **Connect to the Server:**  
   After successful registration (or if you are a returning user who has already registered), the app will attempt to connect to the above server.

4.  **Select an Option to Start:**  
   Once connected, you will typically land on a main screen.  
   Here, you'll find various options to explore the app's features:    

   - **Authenticate** with the connected server using liveness check.
   - **Verify Credentials**: The app guides you to verify your email address or an ID document. After successful verification, credentials are stored in a secured Self database.
   - **Provide Credentials**: Once the credentials are stored in the Self database, you can share them with the connected server.
   - **Sign Documents**: You receive a document from the connected server and respond with either accept or reject.
   - **Backup**: Self SDK creates a snapshot of the local database, which you need to store in the local file system.
   
   
