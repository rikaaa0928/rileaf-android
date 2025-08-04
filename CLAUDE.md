# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Rileaf is an Android VPN application built using the Leaf proxy framework. It consists of:

- **Android App**: A Kotlin/Compose UI that provides VPN control interface
- **Rust Core**: The Leaf proxy framework with FFI bindings for Android integration
- **Build System**: Gradle for Android, Cargo for Rust, with cross-compilation support

## Architecture

### Core Components

1. **MainActivity.kt** (`app/src/main/java/moe/rikaaa0928/rileaf/MainActivity.kt`): Compose UI for VPN control with SOCKS5 address configuration
2. **LeafVpnService.kt** (`app/src/main/java/moe/rikaaa0928/rileaf/LeafVpnService.kt`): Android VPN service that integrates with Leaf via FFI
3. **Leaf FFI** (`lib-src/leaf/leaf-uniffi/`): UniFFI-generated Kotlin bindings for the Rust Leaf library
4. **Native Libraries** (`app/src/main/jniLibs/`): Compiled Rust libraries for different Android architectures

### FFI Integration

The project uses UniFFI to generate Kotlin bindings from Rust. The main FFI functions used:
- `leafRunWithConfigString()`: Starts the Leaf proxy with configuration
- `leafShutdown()`: Stops the proxy runtime

## Common Development Commands

### Building the Android App
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Building Rust Components
```bash
# Build all Rust components (from lib-src/leaf/)
cd lib-src/leaf
make cli-dev    # Debug build
make cli        # Release build

# Build Android FFI libraries (requires NDK_HOME)
cd lib-src/leaf
./scripts/build_android_uniffi.sh

# Test Rust components
make test
```

### Android Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Build Requirements

### Android Development
- Android SDK (compileSdk 36, minSdk 29, targetSdk 36)
- Kotlin 2.0.21
- Compose BOM 2024.09.00
- JNA 5.13.0 for native library loading

### Rust Development
- Rust toolchain with Android targets:
  - `aarch64-linux-android`
  - `armv7-linux-androideabi` 
  - `x86_64-linux-android`
  - `i686-linux-android`
- Android NDK (for cross-compilation)
- UniFFI for generating bindings

## Development Workflow

1. **Modify Rust Code**: Make changes in `lib-src/leaf/`
2. **Rebuild FFI**: Run `./scripts/build_android_uniffi.sh` to regenerate bindings and native libraries
3. **Copy Artifacts**: The script automatically copies:
   - Native libraries to `app/src/main/jniLibs/`
   - Kotlin bindings to `app/src/main/java/uniffi/leafuniffi/`
4. **Build Android**: Use Gradle commands to build and install the app

## Key Configuration

### Leaf Proxy Configuration
The VPN service uses INI-format configuration with:
- TUN interface via file descriptor
- DNS server configuration
- Proxy outbound definitions
- Host rules for routing

Example configuration structure in `LeafVpnService.kt:95-104`.

### VPN Network Setup
- Virtual IP: 10.9.28.2/24
- Routes all traffic (0.0.0.0/0)
- Excludes own package from VPN to prevent loops
- Uses Android VPN service builder pattern

## Important Files

- `lib-src/leaf/Makefile`: Rust build commands
- `lib-src/leaf/scripts/build_android_uniffi.sh`: Cross-compilation script for Android
- `gradle/libs.versions.toml`: Android dependency versions
- `lib-src/leaf/leaf-uniffi/Cargo.toml`: FFI library configuration