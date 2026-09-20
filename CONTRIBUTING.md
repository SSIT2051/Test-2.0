# Contributing to PumpkinMC Host

Thank you for your interest in contributing to **PumpkinMC Host**! We welcome bug reports, feature suggestions, documentation enhancements, and pull requests.

## 🚀 Alpha Notice
PumpkinMC Host is currently in **Early Alpha**. APIs, layout architectures, and configuration bindings may evolve rapidly. Please coordinate major feature proposals via an issue before opening large PRs.

## 🛠️ Development Setup

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer.
- **Java Development Kit (JDK) 17** (Temurin recommended).
- **Android SDK API Level 36** with build-tools.
- Physical ARM64 Android device running Android 7.0+ (API 24+) or an ARM64 system image.

### Building Locally
Clone the repository and build using Gradle:

```bash
# Clone the repository
git clone https://github.com/SSIT2051/pumpkinmc-host.git
cd pumpkinmc-host

# Ensure gradle wrapper has execution rights
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🏗️ Architecture Overview

The app is architected with modern Android best practices:
- **Jetpack Compose + Material 3**: Pitch-black Obsidian surface design system with Pumpkin Orange (`#FF6600`) highlights.
- **MVVM + Kotlin Coroutines & StateFlow**: Reactive UI state bindings with unbundled dirty-checking draft buffers (`draftServer`, `draftSettings`).
- **Tokio & Rayon Emulation Engine**: Background runtime managing virtualized ARM64 process cycles, thread allocations, and low-latency packet routing.
- **Playit.gg Zero-Port-Forward Tunneling**: Public TCP/UDP ingress point allowing friends on any network to join mobile-hosted servers.
- **WASM Plugin Marketplace**: Sandboxed WebAssembly extension model running alongside native Rust.

---

## 📋 Pull Request Guidelines

1. **Branch Naming**: Use descriptive branch names:
   - `feature/your-feature-name`
   - `bugfix/issue-description`
   - `docs/guide-update`
2. **Commit Messages**: Follow standard conventional commits:
   - `feat(dashboard): add dynamic core rebalancing`
   - `fix(network): handle wifi disconnect gracefully`
   - `docs: update user guide with bedrock crossplay`
3. **CI Pipeline**: Ensure all GitHub Actions workflows pass green before requesting review.

---

## 🤝 Attribution & Open Source

This project proudly credits the upstream **PumpkinMC** project:
- Upstream Core: [Pumpkin-MC/Pumpkin](https://github.com/Pumpkin-MC/Pumpkin)
- Native Rust Minecraft server implementation by the PumpkinMC organization and contributors.
- Licensed under open-source terms.
