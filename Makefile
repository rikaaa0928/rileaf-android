.PHONY: help clean setup-env \
        rust-ffi android-debug android-release \
        test lint typecheck \
        install-debug install-release \
        all debug release

# Configuration
CFG_COMMIT_HASH := $(shell git rev-parse HEAD | cut -c 1-7)
CFG_COMMIT_DATE := $(shell git log --format="%ci" -n 1)
export CFG_COMMIT_HASH := $(CFG_COMMIT_HASH)
export CFG_COMMIT_DATE := $(CFG_COMMIT_DATE)

# Android NDK configuration
NDK_VERSION := r25c
ANDROID_API := 21

# Rust targets for Android
ANDROID_TARGETS := aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

# Build directories
RUST_DIR := lib-src/leaf
JNI_LIBS_DIR := app/src/main/jniLibs
KOTLIN_BINDINGS_DIR := app/src/main/java/uniffi/leafuniffi

help: ## Show this help
	@echo "Rileaf Android Build System"
	@echo ""
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'

clean: ## Clean all build artifacts
	@echo "Cleaning build artifacts..."
	./gradlew clean
	cd $(RUST_DIR) && cargo clean
	rm -rf $(JNI_LIBS_DIR)
	rm -rf $(KOTLIN_BINDINGS_DIR)

setup-env: ## Setup development environment
	@echo "Setting up development environment..."
	@echo "Checking Rust installation..."
	@which rustc || (echo "Rust not found. Please install Rust: https://rustup.rs/" && exit 1)
	@echo "Adding Android targets..."
	@for target in $(ANDROID_TARGETS); do \
		rustup target add $$target; \
	done
	@echo "Checking NDK_HOME..."
	@test -n "$(NDK_HOME)" || (echo "NDK_HOME not set. Please set it to your Android NDK path" && exit 1)
	@test -d "$(NDK_HOME)" || (echo "NDK_HOME directory does not exist: $(NDK_HOME)" && exit 1)
	@echo "Environment setup complete"

rust-ffi: setup-env ## Build Rust FFI libraries for Android
	@echo "Building Rust FFI libraries..."
	cd $(RUST_DIR) && ./scripts/build_android_uniffi.sh
	@echo "Copying FFI artifacts to Android project..."
	mkdir -p $(JNI_LIBS_DIR)
	cp -r $(RUST_DIR)/jniLibs/* $(JNI_LIBS_DIR)/
	mkdir -p $(KOTLIN_BINDINGS_DIR)
	cp -r $(RUST_DIR)/kotlin/uniffi/leafuniffi/* $(KOTLIN_BINDINGS_DIR)/

rust-ffi-debug: setup-env ## Build Rust FFI libraries for Android (debug mode)
	@echo "Building Rust FFI libraries (debug)..."
	cd $(RUST_DIR) && ./scripts/build_android_uniffi.sh debug
	@echo "Copying FFI artifacts to Android project..."
	mkdir -p $(JNI_LIBS_DIR)
	cp -r $(RUST_DIR)/jniLibs/* $(JNI_LIBS_DIR)/
	mkdir -p $(KOTLIN_BINDINGS_DIR)
	cp -r $(RUST_DIR)/kotlin/uniffi/leafuniffi/* $(KOTLIN_BINDINGS_DIR)/

android-debug: rust-ffi-debug ## Build debug APK
	@echo "Building debug APK..."
	./gradlew assembleDebug

android-release: rust-ffi ## Build release APK
	@echo "Building release APK..."
	./gradlew assembleRelease

test: ## Run tests
	@echo "Running Rust tests..."
	cd $(RUST_DIR) && cargo test -p leaf
	@echo "Running Android tests..."
	./gradlew test

lint: ## Run linting
	@echo "Running Android lint..."
	./gradlew lint

typecheck: ## Run type checking
	@echo "Running Kotlin compilation check..."
	./gradlew compileDebugKotlin

install-debug: android-debug ## Install debug APK to connected device
	@echo "Installing debug APK..."
	./gradlew installDebug

install-release: android-release ## Install release APK to connected device
	@echo "Installing release APK..."
	./gradlew installRelease

debug: android-debug ## Alias for android-debug

release: android-release ## Alias for android-release

all: android-release ## Build everything (release mode)

# Development workflow targets
dev-install: install-debug ## Quick development build and install

dev-test: rust-ffi-debug test ## Quick development test cycle

# Rust-only targets (for development in lib-src/leaf)
rust-cli: ## Build Rust CLI (release)
	cd $(RUST_DIR) && make cli

rust-cli-dev: ## Build Rust CLI (debug)
	cd $(RUST_DIR) && make cli-dev

rust-test: ## Run Rust tests only
	cd $(RUST_DIR) && make test

rust-proto-gen: ## Regenerate protocol buffers
	cd $(RUST_DIR) && make proto-gen

# Verification targets
verify: typecheck lint test ## Run all verification steps

check-env: ## Check environment setup
	@echo "Checking environment..."
	@echo "Rust version: $$(rustc --version)"
	@echo "Cargo version: $$(cargo --version)"
	@echo "Java version: $$(java -version 2>&1 | head -n 1)"
	@echo "NDK_HOME: $(NDK_HOME)"
	@echo "Android targets:"
	@for target in $(ANDROID_TARGETS); do \
		echo "  $$target: $$(rustup target list --installed | grep $$target >/dev/null && echo 'installed' || echo 'NOT INSTALLED')"; \
	done

# Quick aliases
build: android-debug ## Alias for android-debug
install: install-debug ## Alias for install-debug