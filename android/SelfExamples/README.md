# Android Examples

These are example applications that use SelfSDK, how to integrate across various use cases


### 1. Registration

Register for a Self account and display the registration status on the screen


### 2. Chat

This app connect to another app by an inbox address. You need to use server SDK (go-sdk, jvm-sdk) to configure an account, handle `onKeyPackage` callback and open the inbox.
And it sends and receives chat messages.

Note: update server inbox address in TODO

### 3. Chat - QRCode

This app connects another app by scanning the QRCode
And it sends and receives chat messages.

Need to scan QRCode from Demo app https://demo-sandbox.app.preview.joinself.com/


### 4. Verification

This simple app demonstrates how to integrate email and document verification processes.
It also displays all verified credentials in the account.

### 5. Credential

This app demonstrates how to handle incoming credential request, such as: liveness, email,...

Note: update server inbox address in TODO

Steps:
    1. Register
    2. Update server inbox address in MainActivity.kt
    3. Click Connect to set up connection with server
    4. Handle incoming credential request