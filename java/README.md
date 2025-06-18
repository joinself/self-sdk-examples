# JVM Examples

## Setup

- Clone the repo `https://github.com/joinself/self-mobile-embedded`
- Go go example dir  `cd self-mobile-embedded/self-sdk-kmp/examples/jvm`
- Then run the examples below

## Using the library

```gradle
repositories {
    maven {
        name = "Central Portal Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")        
        content {
            includeGroup("com.joinself")
        }
    }
    mavenCentral()
}

dependencies {
  implementation("com.joinself:sdk-jvm:1.0.0")
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

## Distribution

- Build executable distribution 
```bash
./gradlew :self-demo:distZip # output build/distributions/self-demo.zip
```

- Unzip `self-demo.zip` & run
```bash
cd self-demo
./bin/self-demo
```
