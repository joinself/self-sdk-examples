# Android Examples

These are example applications that use SelfSDK, how to integrate across various use cases.

You can read the [full documentation here](https://docs.joinself.com/introduction/overview/)

1. Requirements
   - Java 17 SDK
   - Kotlin 2.0 and above
   - Gradle 8.11.1 and above
   - Android API 28 and above
   
2. Clone the repository   
```bash
git clone git@github.com:joinself/self-sdk-examples.git
cd self-sdk-examples/android/SelfDemo
```
 
3. Build

```bash
./gradlew :app:assembleDebug

adb -d install -r app/build/outputs/apk/debug/app-debug.apk
```