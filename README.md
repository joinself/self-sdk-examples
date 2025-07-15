# Self SDK Examples

This repository provides example applications demonstrating the usage of Self SDKs across various platforms. Our SDKs enable you to integrate Self's identity and messaging capabilities into your applications.

**Note on Submodules:** This repository utilizes Git submodules to include some of the SDK examples (e.g. Golang). To ensure you clone everything correctly, use:

```bash
git clone --recurse-submodules https://github.com/joinself/self-sdk-examples.git
```

If you have already cloned the repository without the submodules, you can initialize and update them using:

```bash
git submodule update --init --recursive
```

## Mobile SDKs

Our mobile SDKs allow you to build secure and user-friendly applications for Android and iOS.

*   **Android:** Find examples for our Android SDK in the [android](./android/) directory
*   **Android Demo App:** See our demo application for a more in-depth example on Android [here](https://github.com/joinself/demo-android)
*   **iOS Demo App:** See our demo application for a more in-depth example on iOS [here](https://github.com/joinself/demo-ios)

## Server SDKs

Our server-side SDKs are designed for robust backend integrations.

*   **Golang:** Check out examples for our Golang SDK in the [`golang/examples/`](./golang/examples/) directory.
*   **Java:** Discover examples for our Java SDK in the [`java/`](./java/) directory.

We encourage you to explore these examples to understand how to best utilize Self SDKs in your projects.
