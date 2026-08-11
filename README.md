# LS Pass

**100% local, client-side, zero-knowledge Bitwarden-style password manager for Android.**

LS Pass is a fully offline password manager built with Kotlin and Jetpack Compose. All vault data — passwords, TOTP secrets, card numbers, identities, SSH keys, and passkeys — is encrypted on-device with **AES-256-GCM** using keys derived from your master password via **Argon2id**. Nothing ever leaves your device. No accounts, no servers, no cloud sync, no telemetry.

---

## Table of Contents

- [Highlights](#highlights)
- [Feature Overview](#feature-overview)
- [Supported Item Types](#supported-item-types)
- [Security Architecture](#security-architecture)
- [Autofill Service](#autofill-service)
- [Import & Export](#import--export)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Building & Signing](#building--signing)
- [Environment Configuration](#environment-configuration)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Security Disclaimer](#security-disclaimer)
- [License](#license)

---

## Highlights

- **Zero-knowledge by design** — Your master password is never stored, transmitted, or known by anyone. The vault is encrypted with a key derived exclusively from it.
- **100% offline** — The Room database lives only on your device. There is no network access for vault data. The single optional network capability (Gemini AI) is unused by default.
- **AES-256-GCM encryption** — Every field is encrypted with a random 12-byte IV and a 128-bit authentication tag.
- **Argon2id key derivation** — Memory-hard KDF with 64 MB memory, 3 iterations, and 4 lanes, resistant to GPU/ASIC brute-force attacks.
- **Biometric unlock** — Master key backed up to the Android Keystore behind a hardware-bound AES key; unlock with fingerprint/face without typing your password.
- **Android Autofill service** — Autofill usernames and passwords directly in any app or browser, with domain/package-aware ranking.
- **Bitwarden import** — Drop in an unencrypted Bitwarden CSV export and migrate everything, including folders and favorites.
- **Encrypted backups** — Export your entire vault as a single AES-256-GCM encrypted JSON file protected by an export password of your choice.

---

## Feature Overview

### Vault Lifecycle

| State | Description |
| --- | --- |
| `NOT_SETUP` | First launch. Create your master password, an optional hint, and choose whether to enable biometric unlock. |
| `LOCKED` | Vault is encrypted at rest. Unlock with the master password or biometrics. |
| `UNLOCKED` | Decrypted master key is held in memory only; all items are available. |

### Unlock Options

- **Master password** — Derived via Argon2id and verified against an encrypted verification token without exposing the key.
- **Biometric unlock** — Your derived master key is encrypted with a Keystore-backed AES key and decrypted only after a successful `BiometricPrompt` authentication.

### Session Security

- **Auto-lock** — Choose from *Immediate*, *1/5/15/30 minutes*, *On app background*, or *Never*. Inactivity is tracked globally via pointer events.
- **Clipboard auto-clear** — Copied secrets are wiped from the clipboard after *10s / 20s / 30s / 1 min*, or never. The timer resets on every copy.
- **Background locking** — The vault locks immediately (or after the timeout) when the app goes to the background, depending on the chosen option.

### Vault Management

- **Search** — Instant full-text filtering across item names, usernames, emails, URIs, cardholder names, passkey RP IDs, secure-note contents, and SSH key names.
- **Folders** — Organize items into named folders; filter the list by folder.
- **Collections** — Color-coded collections with multi-item membership; filter by collection.
- **Favorites & Hidden items** — Star items for quick access, or hide sensitive items from the default list (with a "show hidden only" filter).
- **Recently accessed** — A smart "Recently Accessed" rail combining explicit open history with recently edited items.
- **Quick copy** — One-tap copy of usernames, passwords, TOTP codes, and other secrets with automatic clipboard clearing.

### Built-in Password Generator

- **Random passwords** — 5–128 characters with toggles for upper/lowercase, numbers, and specials, guaranteed minimum counts, and an "avoid ambiguous characters" option (`iI1lLo0O8`).
- **Passphrases** — 3–20 words from a curated 48-word list, configurable separator, capitalization, and optional appended digit (e.g. `Mountain-Ocean-Quartz-7`).
- **Entropy estimation** — Live bit-entropy calculation shown in the generator and used by the health audit.

### Vault Health Audit (offline)

Runs entirely on-device and produces a 0–100 health score:

- **Weak password detection** — Flags logins with entropy < 45 bits; < 28 bits or < 8 chars is `CRITICAL`, otherwise `HIGH`.
- **Reused password detection** — Groups logins sharing the same password; 3+ accounts is `CRITICAL`.
- **Missing 2FA** — Counts logins with no TOTP secret attached.
- **Empty password detection** — Logins with no password are flagged `CRITICAL`.

### TOTP (2FA) Codes

RFC 6238-compliant TOTP generator (HMAC-SHA1, 30-second window, 6 digits) with Base32 secret decoding and a live countdown of remaining seconds.

---

## Supported Item Types

| Type | Encrypted fields (stored as JSON) |
| --- | --- |
| **Login** | username, password, TOTP secret, URIs, notes, custom fields |
| **Card** | cardholder name, number, brand, expiry month/year, CVV, notes, custom fields |
| **Identity** | title, names, username, company, SSN, passport, license, email, phone, address, notes, custom fields |
| **Secure Note** | free-form notes, custom fields |
| **SSH Key** | key name, private key, public key, fingerprint, notes, custom fields |
| **Passkey** | relying-party ID, user handle, credential ID, public key, notes, custom fields |

Custom fields support text, hidden, and boolean values and round-trip through Bitwarden CSV imports.

---

## Security Architecture

### Threat Model

LS Pass assumes the device itself is the only trust boundary. The app protects against:

- **Physical device theft** — At-rest vault data is unreadable without the master password or biometrics.
- **Brute-force offline attacks** — Argon2id's memory-hardness makes parallel GPU cracking prohibitively expensive; the export format uses a fresh random salt per export.
- **Accidental secret exposure** — Clipboard auto-clear, hidden items, and full-disk encryption of secrets in the database.

### Encryption Pipeline

```
Master Password ──> Argon2id (t=3, m=64MiB, p=4, v1.3) ──> 256-bit AES key (memory only)
                                                        │
                       ┌────────────────────────────────┤
                       ▼                                ▼
              Room database                        Android Keystore
              (AES-256-GCM per field)              (hardware-bound AES-256
                                                   backup of the master key
                                                   for biometric unlock)
```

- **Key derivation:** `Argon2id` (v1.3, 3 iterations, 64 MiB memory, 4 lanes) producing a 256-bit AES key from the master password and a 16-byte random salt. A `PBKDF2WithHmacSHA256` (100,000 iterations) implementation is retained as a legacy fallback.
- **Field encryption:** `AES/GCM/NoPadding` with a fresh 12-byte random IV per encryption and a 128-bit GCM tag. Ciphertext is stored as `Base64(IV ‖ ciphertext ‖ tag)`.
- **Verification token:** On setup, the fixed string `LS_PASS_VALID_VAULT_TOKEN` is encrypted with the derived key and stored; a password is accepted only if it decrypts correctly — no password hash is ever stored.
- **Biometric key backup:** The derived master key is encrypted with a Keystore-generated, non-exportable AES-256 key and stored in DataStore. The Keystore key is deleted when biometric unlock is disabled.
- **In-memory only:** The active master key lives in a process-wide holder (`VaultSessionManager.getSharedMasterKey()`) and is nulled on every lock, background timeout, and process death.
- **Secure random:** All salts, IVs, and generated passwords use `java.security.SecureRandom`.

### What Is Not Stored

- Your master password (derived key only, in memory while unlocked)
- Plaintext secrets at rest (every sensitive field is AES-256-GCM encrypted)
- Any data off-device (no network calls are made for vault functionality)

---

## Autofill Service

`LsPassAutofillService` is an `android.service.autofill.AutofillService` that works entirely offline:

- **Field detection** — Parses the `AssistStructure` for username/email/password fields using autofill hints, HTML attributes, `InputType` flags, and view id/hint heuristics (including WebView fields).
- **Smart ranking** — Candidate logins are scored and sorted by: direct URI domain match (+100), app package match (+80), name/domain similarity (+50), name/package match (+30).
- **Locked behavior** — If the vault is locked, the service offers an "LS Pass (Locked)" dataset that deep-links into the app to unlock, keeping credentials unreachable from the fill UI.
- **Save requests** — Acknowledged locally with zero network access.

To use it: `Settings > Passwords & accounts > Autofill service > LS Pass`.

---

## Import & Export

### Bitwarden CSV Import

Paste (or provide) a standard Bitwarden **unencrypted** CSV export:

- Header auto-detection (`folder`, `type`, `name`, `login_username`, `login_password`, `notes`, `fields`, `favorite`, card/identity columns, etc.)
- Automatic type mapping (`login`, `secure note`, `card`, `identity`) with content-aware fallback detection
- Folders are created on the fly (reused when names match, case-insensitively)
- Favorites preserved, custom fields parsed from `fields` blocks (`Name: Value` or `Name=Value` lines)
- A summary toast reports imported count and created folders

### Encrypted Backup (JSON)

- **Export** — The full vault (items, folders, collections) is serialized to JSON, encrypted with AES-256-GCM under a key derived (Argon2id, fresh random salt) from an **export password you choose**, and written as a portable `LSPASS_ENCRYPTED_V1` container.
- **Import** — The container is detected by its `LSPASS_ENCRYPTED_V1` format marker, decrypted with the provided export password, and restored.

---

## Project Structure

```
ls-pass/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                 # App entry, screen routing, session wiring
│   │   │   │   ├── autofill/
│   │   │   │   │   └── LsPassAutofillService.kt    # Android Autofill service
│   │   │   │   ├── crypto/
│   │   │   │   │   ├── CryptoManager.kt            # AES-256-GCM, Argon2id, PBKDF2, generators
│   │   │   │   │   ├── BiometricManager.kt         # Keystore + BiometricPrompt integration
│   │   │   │   │   └── TotpGenerator.kt            # RFC 6238 TOTP
│   │   │   │   ├── data/
│   │   │   │   │   ├── dao/VaultDao.kt             # Room DAO
│   │   │   │   │   ├── db/LsPassDatabase.kt        # Room database
│   │   │   │   │   ├── importer/BitwardenCsvImporter.kt
│   │   │   │   │   ├── models/                     # Entities + DTOs + health/export models
│   │   │   │   │   └── repository/VaultRepository.kt  # Encrypt/decrypt on save/load
│   │   │   │   ├── session/VaultSessionManager.kt  # Auth state, auto-lock, clipboard, DataStore
│   │   │   │   └── ui/
│   │   │   │       ├── components/                 # Animated icons, strength meter
│   │   │   │       ├── screens/                    # Setup/Unlock/Main/Detail/Edit/Health
│   │   │   │       ├── theme/                      # Material 3 theme
│   │   │   │       └── viewmodel/                  # VaultViewModel, GeneratorViewModel
│   │   │   └── res/                                # Icons, themes, autofill config
│   │   ├── test/                                   # Robolectric + Roborazzi unit/screenshot tests
│   │   └── androidTest/                            # Instrumented tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/libs.versions.toml                       # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .env.example                                    # Secrets plugin template (Gemini key)
├── metadata.json
└── .gitignore
```

---

## Tech Stack

| Layer | Technology | Version |
| --- | --- | --- |
| Language | Kotlin | 2.2.10 |
| UI | Jetpack Compose (Material 3) | BOM 2024.09.00 |
| Android Gradle Plugin | AGP | 9.1.1 |
| `compileSdk` / `targetSdk` / `minSdk` | 36 (API 36.1) / 36 / 24 | — |
| DI / Architecture | Manual DI + MVVM + Repository | — |
| Database | Room (with KSP) | 2.7.0 |
| Preferences | DataStore Preferences | 1.1.7 |
| Navigation | Navigation Compose | 2.8.9 |
| Serialization | Moshi (+ KSP codegen) | 1.15.2 |
| Networking (unused by default) | Retrofit / OkHttp (logging) | 2.12.0 / 4.10.0 |
| Crypto | Bouncy Castle (`bcprov-jdk18on`) | 1.78.1 |
| Biometrics | `androidx.biometric` | 1.1.0 |
| Lifecycle | Lifecycle Compose / ViewModel | 2.8.7 |
| Coroutines | kotlinx-coroutines | 1.10.2 |
| Firebase | BOM (AI + App Check reCAPTCHA, optional auth/firestore) | 34.15.0 |
| Testing | JUnit 4, Robolectric, Roborazzi, Espresso, Compose UI test | — |
| Codegen | KSP | 2.3.5 |

---

## Architecture

- **Single-module, unidirectional data flow:** UI (Compose) → ViewModel (`StateFlow`) → Repository → Room.
- **Encryption boundary:** The repository encrypts every field before persisting and decrypts only while the vault is unlocked. Ciphertext-only entities are stored in Room; decrypted DTOs (`DecryptedVaultItem`) exist solely in memory.
- **Session orchestration:** `VaultSessionManager` owns the `VaultAuthState` machine (`NOT_SETUP → LOCKED → UNLOCKED`), the in-memory master key, auto-lock timing, clipboard clearing, and all user preferences.
- **Offline-first:** All vault operations complete locally; the only network dependencies are optional and disabled by default (see below).

---

## Getting Started

### Prerequisites

- Android Studio (latest stable, with JDK 17+)
- Android SDK Platform 36 (`compileSdk 36`) — AGP will prompt to install it
- A device or emulator running Android 7.0 (API 24) or higher

### Run (Android Studio)

1. `File > Open` and select the project root (`ls-pass/`).
2. Let Gradle sync complete (first sync downloads AGP 9.1.1, Kotlin 2.2.10, and dependencies).
3. Select an emulator or connected device and press **Run**.
4. On first launch you will be guided through vault setup.

### Run (Command Line)

```bash
# Unit tests
./gradlew test

# Build a debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Install on a connected device
./gradlew installDebug
```

> **Note:** The debug build signs with the bundled `debug.keystore`. On Windows use `gradlew.bat`.

---

## Building & Signing

The release build type reads signing configuration from environment variables:

| Variable | Purpose |
| --- | --- |
| `KEYSTORE_PATH` | Path to your `.jks` keystore (defaults to `<project>/my-upload-key.jks`) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password (alias `upload`) |

```bash
# Example: create a release APK
KEYSTORE_PATH=/path/to/keystore.jks \
STORE_PASSWORD=xxx \
KEY_PASSWORD=xxx \
./gradlew assembleRelease
```

Both `my-upload-key.jks` and `debug.keystore` are git-ignored — never commit keystores or passwords.

---

## Environment Configuration

Optional secrets are configured through the **Secrets Gradle Plugin**, which reads `.env` (with `.env.example` as the fallback template):

| Key | Purpose |
| --- | --- |
| `GEMINI_API_KEY` | Required only if the app is extended to call the Gemini API. Commented out by default so the key is **not** packaged into the APK. |

The Google Services plugin runs in **WARN** mode, so the project builds without a `google-services.json`. Firebase dependencies (`firebase-ai`, `firebase-appcheck-recaptcha`) are included and unused; `firebase-auth` / Firestore / Credential Manager remain commented out until a sync feature is deliberately enabled.

---

## Testing

The project includes three test layers:

1. **Robolectric unit tests** — JVM-based Android tests (`app/src/test/java/com/example/ExampleRobolectricTest.kt`) with `testOptions.unitTests.isIncludeAndroidResources = true`.
2. **Roborazzi screenshot tests** — Golden-image Compose rendering on the JVM (`GreetingScreenshotTest.kt`, output under `app/src/test/screenshots/`).
3. **Instrumented tests** — Espresso + Compose UI tests on device/emulator (`app/src/androidTest/`).

```bash
./gradlew test                      # Robolectric + unit tests
./gradlew connectedDebugAndroidTest # Instrumented tests (needs device)
./gradlew recordRoborazziDebug     # Regenerate screenshot goldens
```

---

## Roadmap

- Wire-up of the (already dependency-listed) optional Firebase Auth + Google Sign-In and Firestore sync, behind explicit user opt-in
- Autofill **save** flow (creating new logins from the autofill UI)
- Real WebAuthn passkey attestation flow (currently stored as encrypted passkey records)
- Argon2id parameter upgrade and database migrations (currently `fallbackToDestructiveMigration` at v1)
- Camera-based card scanning (dependencies already listed in the catalog)
- Material You dynamic color theme support

---

## Security Disclaimer

LS Pass is a security-sensitive application provided **as-is**, without warranty. While it follows established cryptography practices (Argon2id, AES-256-GCM, Android Keystore, offline storage), it has **not** undergone an independent third-party security audit. Use it at your own risk; for production or high-value credential storage, consider a commercially audited solution until this project receives professional review. Always keep offline backups of your vault encrypted with a strong export password.

---

## License

No license has been specified for this project. All rights reserved by the author until a license (e.g., MIT or GPL) is added.

---

Made with Kotlin and Jetpack Compose.
