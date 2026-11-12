# Stash — Minimalist Offline Finance & Debt Tracker

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-12%20--%2015%20(API%2031%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Obtainium](https://img.shields.io/badge/Obtainium-Compatible-536DFE.svg)](https://github.com/ImranR98/Obtainium)
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

### Method 1: Obtainium (Recommended for Direct Updates)
1. Download and install [Obtainium](https://github.com/ImranR98/Obtainium).
2. Tap **Add App** in Obtainium.
3. Paste the URL of this repository:
   ```
   https://github.com/itznan/stash
   ```
4. Tap **Add**. Obtainium will automatically fetch the latest release APK and notify you of new updates!

### Method 2: Manual APK Download
Download `Stash.apk` directly from the [Releases](releases) tab.

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

# Assemble the release APK
./gradlew assembleRelease
```
The output APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
