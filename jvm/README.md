# JVM Examples

## Setup

- Clone the repo `https://github.com/joinself/self-mobile-embedded`
- Go go example dir  `cd self-mobile-embedded/self-sdk-kmp/examples/jvm`
- Then run the examples below

## Using the library

```gradle
repositories {
  maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
  implementation("com.joinself:sdk-jvm:1.0.0-SNAPSHOT")
}
```

## Run examples

### Discovery

Scan a QR code and set up an inbox

```bash
./gradlew :discovery:run
```

### Agreement

Send a agreement request to mobile and receive a response

```bash
./gradlew :agreement:run
```

### Chat

Send chat messages

```bash
./gradlew :chat:run
```

### Credential

Request email credentials from mobile

```bash
./gradlew :credential:run
```


## Notes

Run with debug, then attach debugger in Android Studio

```bash
./gradlew :agreement:run --debug-jvm
```
