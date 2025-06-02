# Android Examples

These are example applications that use SelfSDK, how to integrate across various use cases.

You can read the [full documentation here](https://docs.joinself.com/introduction/overview/){target="_blank"}

1. Requirements
   - Java 17 SDK
   - Kotlin 2.0 and above
   - Gradle 8.11.1 and above
   - Android API 28 and above
   
2. Clone the repository   
```bash
git clone git@github.com:joinself/self-sdk-examples.git
cd self-sdk-examples/android/SelfExamples
```

3. Open `SelfExamples` in Android Studio, and choose the following example apps to run.   
Or build each app in command line

## Examples

#### 1. Registration

Register for a Self account and display the registration status on the screen

Build & install command  
```bash
./gradlew :registration:assembleDebug
adb -d install -r registration/build/outputs/apk/debug/registration-debug.apk
```

#### 2. Chat

This app connect to another app by an inbox address. You need to use server SDK (go-sdk, jvm-sdk) to configure an account, handle `onKeyPackage` callback and open the inbox.
And it sends and receives chat messages.

Note: update server inbox address in TODO

Build & install command   
```bash
./gradlew :chat:assembleDebug
adb -d install -r chat/build/outputs/apk/debug/chat-debug.apk
```

#### 3. Chat - QRCode

This app connects another app by scanning the QRCode
And it sends and receives chat messages.

Need to scan QRCode from Demo app https://demo-sandbox.app.preview.joinself.com/

Build & install command   
```bash
./gradlew :chat-qrcode:assembleDebug
adb -d install -r chat-qrcode/build/outputs/apk/debug/chat-qrcode-debug.apk
```

#### 4. Verification

This simple app demonstrates how to integrate email and document verification processes.
It also displays all verified credentials in the account.


Build & install command   
```bash
./gradlew :verification:assembleDebug
adb -d install -r verification/build/outputs/apk/debug/verification-debug.apk
```

#### 5. Credential

This app demonstrates how to handle incoming credential request, such as: liveness, email,...

Note: update server inbox address in TODO

Steps:
  - Update server inbox address in MainActivity.kt
  - Run app and Register
  - Click Connect to set up connection with server
  - Handle incoming credential request

Build & install command   
```bash
./gradlew :credential:assembleDebug
adb -d install -r credential/build/outputs/apk/debug/credential-debug.apk
```