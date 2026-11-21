# Stash - Minimalist Offline Finance & Debt Tracker

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-12%20--%2015%20(API%2031%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![GitHub Release](https://img.shields.io/github/v/release/itznan/stash?logo=github)](https://github.com/itznan/stash/releases)
[![F-Droid](https://img.shields.io/badge/F--Droid-Package-1976D2.svg?logo=fdroid)](https://f-droid.org/packages/com.nan.stash/)
[![Material 3](https://img.shields.io/badge/Design-Material_You_(Monet)-7986CB.svg)](https://m3.material.io)

Stash is an ultra-fast, 100% offline personal finance, dual-wallet, and peer-to-peer loan tracker built for Android with Jetpack Compose and Material 3 Dynamic Theming (Monet).

---

## Features

- **100% Offline & Private**: Zero internet permissions, zero analytics, zero ads, zero accounts. Your financial data never leaves your device.
- **Dual-Wallet Tracking**: Manage Online Money (bank/UPI) and Cash Available independently, with seamless zero-distortion transfers.
- **Low Balance Warnings**: Configure custom warning thresholds for Online Money and Cash in Settings. Get instant visual warning badges on the dashboard and alerts before confirming expenses that exceed limits.
- **Lent Money & Debt Manager**: Track money lent to friends or colleagues with due dates, partial repayments, and direct Call or WhatsApp shortcuts.
- **Recurring Subscriptions**: Monitor monthly bills and recurring services with renewal dates, burn-rate summaries, and one-tap payment recording.
- **Printable PDF Statements**: Generate clean, formatted A4 financial reports with net cash flow, income, expenses, and category breakdowns ready for printing or sharing.
- **Home Screen Widget**: View live Online and Cash balances on your launcher, with quick-action shortcuts for Add Income, Spend, and Transfer.
- **Interactive Analytics**: 7-day cash flow bar chart, ranked expense categories, visual donut breakdown, and Month-over-Month comparison metrics.
- **Biometric Security**: Protect your financial data behind fingerprint authentication or device screen lock.
- **Monet Dynamic Theming**: Fully responsive to Android wallpaper color accents in both Light and Dark modes.
- **Export & Backup**: Export transaction ledgers to CSV for spreadsheets, or create complete local JSON snapshots (transactions, loans, subscriptions) with one-tap restore.
- **120Hz LTPO Performance**: Built with zero-allocation date formatting, graphicsLayer hardware transforms, and fluid tab animations.

---

## Installation

### GitHub Releases
Download the signed APK directly from the [GitHub Releases](https://github.com/itznan/stash/releases) page.

### F-Droid
Stash is available on F-Droid, built directly from source:
- [F-Droid Package Listing](https://f-droid.org/packages/com.nan.stash/)

---

## Building From Source

### Prerequisites
- JDK 17
- Android SDK (compileSdk 35, minSdk 31)

### Build Steps
```bash
# Clone the repository
git clone https://github.com/itznan/stash.git
cd stash

# Assemble the release APK (unsigned)
./gradlew assembleRelease
```

The output APK will be generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
