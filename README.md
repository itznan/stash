# Stash — Minimalist Offline Finance & Debt Tracker

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-12%20--%2015%20(API%2031%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![F-Droid](https://img.shields.io/badge/F--Droid-Coming_Soon-1976D2.svg?logo=fdroid)](https://f-droid.org/packages/com.nan.stash/)
[![Material 3](https://img.shields.io/badge/Design-Material_You_(Monet)-7986CB.svg)](https://m3.material.io)

**Stash** is an ultra-fast, 100% offline personal finance, dual-wallet, and peer-to-peer loan tracker built with **Jetpack Compose** and **Material 3 Dynamic Theming (Monet)**.

---

## Features

- **100% Offline & Private**: Zero internet permissions, zero analytics, zero ads, zero accounts. Your financial data never leaves your device.
- **Dual-Wallet Architecture**: Track **Online Money** (bank/UPI) and **Cash Available** separately. Transfer between them with zero ledger distortion.
- **Lent Money & Debt Manager**: Record loans given to friends or colleagues with due dates, partial repayments, and direct Call/WhatsApp action shortcuts.
- **Native Keyboard Integration**: Ultra-fast decimal input with your mobile's native software keyboard.
- **120Hz LTPO Optimization**: Zero-allocation date parsing, graphicsLayer hardware transforms, and fluid tab switching animations.
- **Biometric Authentication**: Secure your transaction ledger behind fingerprint or device lock.
- **Monet Dynamic Theming**: Adapts in real-time to your Android wallpaper accents in Light and Dark modes.
- **Export & Backup**: Export CSVs for spreadsheet analysis and generate encrypted JSON backups with instant restore.

---

## Installation

Stash is distributed via F-Droid. F-Droid builds the app from source and signs it with its own key.

> [!IMPORTANT]
> **One-Time Upgrade Notice for v1.0.0 / v1.0.1 Testers**: If you installed an earlier APK from GitHub, export your data backup first (**Settings -> Backup Database**), uninstall the old app, install from F-Droid, and restore your backup. Android does not permit upgrading over an APK signed with a different key.

### F-Droid
- Install from F-Droid (pending merge of [Merge Request !57030](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/57030)):
  - [F-Droid Package Listing (Coming Soon)](https://f-droid.org/packages/com.nan.stash/)

---

## Building From Source

### Prerequisites
- JDK 17
- Android SDK (compileSdk 35, minSdk 31)

### Build Steps
```bash
# Clone the repository
git clone https://github.com/itznan/stash.git
cd Stash

# Assemble the release APK (unsigned — F-Droid applies its own signature)
./gradlew assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).