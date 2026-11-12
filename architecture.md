# Deep-Level Architecture Document: Stash Personal Finance Tracker

## 1. Directory Tree & File Inventory

Stash is an offline-first, high-performance personal finance and debt tracker engineered in Kotlin with Jetpack Compose. The architecture conforms to modern Android engineering principles: Unidirectional Data Flow (UDF), Room Object-Relational Mapping with strict non-destructive migrations, Jetpack DataStore Preferences, 120Hz LTPO display synchronization, biometric hardware authentication, and dynamic Material Design 3 (Monet) theming.

```
Stash/
├── build.gradle.kts                      # Root Gradle build script (AGP 8.7.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28)
├── settings.gradle.kts                   # Project configuration and Maven repository declarations
├── gradle.properties                     # JVM arguments, AndroidX flags, AAPT2 override
├── gradlew / gradlew.bat                 # Unix/Windows Gradle Wrapper execution entry points
├── gradle/wrapper/
│   ├── gradle-wrapper.properties         # Gradle distribution configuration (v8.11)
│   └── gradle-wrapper.jar                # Gradle wrapper execution jar
└── app/
    ├── build.gradle.kts                  # App build configuration (minSdk 31, targetSdk 35, compileSdk 35, Room schemas)
    ├── proguard-rules.pro                # R8 shrinking & keep rules for Room, Coroutines, Serialization
    ├── schemas/                          # Room exported JSON schemas for migration verification
    │   └── com.nan.stash.data.local.AppDatabase/
    │       ├── 1.json                    # Schema v1 (initial ledger without note or paymentMode)
    │       ├── 2.json                    # Schema v2 (added note column)
    │       ├── 3.json                    # Schema v3 (added paymentMode column)
    │       └── 4.json                    # Schema v4 (added loans table with borrower info, balances, due dates)
    └── src/
        ├── androidTest/
        │   └── java/com/nan/stash/
        │       └── MigrationTest.kt      # Room MigrationTestHelper verifying v1->v2, v2->v3, v3->v4, and v1->v4
        └── main/
            ├── AndroidManifest.xml       # App declaration, hardware acceleration, permissions, FileProvider
            ├── res/
            │   ├── drawable/             # XML vector assets and launcher bitmaps
            │   ├── mipmap-*/             # Multi-density legacy launcher icons
            │   ├── values/               # Strings, colors, XML themes
            │   └── xml/
            │       └── file_paths.xml    # FileProvider content mapping for CSV/JSON export
            └── java/com/nan/stash/
                ├── MainActivity.kt       # Root Activity: 120Hz display pinning, biometric gate, FAB & Modal orchestration
                ├── data/
                │   ├── model/
                │   │   ├── TransactionType.kt  # Discriminator enum (INCOME, EXPENSE, TRANSFER, LEND, REPAYMENT)
                │   │   └── PaymentMode.kt      # Wallet channel enum (ONLINE, CASH)
                │   └── local/
                │       ├── TransactionEntity.kt # Room SQL entity for financial transactions
                │       ├── TransactionDao.kt    # Reactive Room Data Access Object (Flow queries, insert, update, delete)
                │       ├── LoanEntity.kt        # Room SQL entity for tracked loans (borrower, phone, balance, dates, status)
                │       ├── LoanDao.kt           # Room Data Access Object for loans (Flow queries, insert, update, delete)
                │       ├── AppDatabase.kt       # RoomDatabase definition (exportSchema=true, non-destructive migrations 1->4)
                │       └── SettingsDataStore.kt # Proto-like Preferences DataStore for themes, currency, and biometric locks
                └── ui/
                    ├── MainViewModel.kt         # AndroidViewModel: StateFlow orchestration, transfers, loans, edits, backups
                    ├── navigation/
                    │   ├── NavRoutes.kt         # Sealed class Screen hierarchy (Home, History, Tags, Settings)
                    │   └── BottomNavBar.kt      # Material 3 NavigationBar component
                    ├── components/
                    │   ├── CommonComponents.kt  # Reusable UI primitives: CategoryIcon, Dynamic Color mapping
                    │   └── StashLogo.kt         # Procedural reactive Material 3 vector logo component
                    ├── screens/
                    │   ├── HomeScreen.kt        # Primary dashboard: dual-wallet balance, lent money banner, weekly graph, recent feed
                    │   ├── HistoryScreen.kt     # Filterable transaction ledger (search, chips including Lent, clickable for edit)
                    │   ├── TagsScreen.kt        # Category analytics: ranked spending cards, percentage shares, month comparisons
                    │   ├── SettingsScreen.kt    # Preferences: Theme, currency, biometric toggle, CSV/JSON backup
                    │   ├── AddMoneyBottomSheet.kt      # Native mobile keyboard decimal input modal for income logging
                    │   ├── SpendMoneyBottomSheet.kt    # Native mobile keyboard decimal input modal for expense logging
                    │   ├── TransferMoneyBottomSheet.kt # Native mobile keyboard transfer modal (Online ⇄ Cash)
                    │   ├── EditTransactionBottomSheet.kt # Full-feature transaction editor with decimal keyboard
                    │   ├── LendMoneyBottomSheet.kt     # Lending entry modal: person name, contact, reason, quick due dates, wallet source
                    │   └── LentMoneyManagerSheet.kt    # Full loan management: overview, filter chips, direct Call/WhatsApp triggers, repayments
                    ├── util/
                    │   └── DateUtils.kt         # Zero-allocation thread-safe DateTimeFormatter singleton for 120Hz scrolling
                    └── theme/
                        ├── Color.kt             # Legacy fallback color definitions
                        ├── Shape.kt             # Material 3 rounded corner shape tokens
                        ├── Type.kt              # Material 3 typography definitions
                        └── Theme.kt             # Dynamic Material You color scheme provider (API 31-35)
```

---

## 2. Diagram 1: System Context

```mermaid
flowchart TD
    User["End User (Touch & Biometric Input)"]
    
    subgraph AndroidPlatform["Android OS Platform (API 31 - 35)"]
        BiometricHAL["Android Keyguard / Biometric Subsystem (BiometricPrompt)"]
        SurfaceFlinger["SurfaceFlinger & WindowManager (120Hz Preferred Mode & FrameRateBoost)"]
        StorageFS["Scoped Internal Storage / App Sandboxed Cache"]
        ShareSheet["Android Sharesheet / Document Provider (SAF)"]
        DialerIntent["Native Phone Dialer Contract (Intent.ACTION_DIAL)"]
        WhatsAppBridge["WhatsApp Application / Web Gateway (Intent.ACTION_VIEW api.whatsapp.com)"]
    end

    subgraph StashProcess["Stash Application Boundary (Process: com.nan.stash)"]
        UI["Compose UI Layer (MainActivity, Screens & BottomSheets)"]
        VM["Business & State Coordinator (MainViewModel)"]
        RoomDB[("SQLite Database (Room: stash_database - Non-Destructive v4)")]
        DataStore[("Preferences DataStore (stash_settings.preferences_pb)")]
        FileProvider["Content Provider (androidx.core.content.FileProvider)"]
    end

    User -->|"Touch Gestures / 120Hz Swiping"| UI
    User -->|"Fingerprint / Device Credential"| BiometricHAL
    BiometricHAL -->|"Authentication Callback"| UI
    UI -->|"High Refresh Rate / HW Acceleration Params"| SurfaceFlinger
    UI -->|"Dispatches User Actions (Add, Spend, Transfer, Lend, Repay)"| VM
    UI -->|"Trigger Direct Phone Call"| DialerIntent
    UI -->|"Trigger Direct WhatsApp Chat"| WhatsAppBridge
    VM -->|"Reactive SQLite Queries (Flow: transactions & loans)"| RoomDB
    VM -->|"Configuration Mutators"| DataStore
    VM -->|"Writes JSON / CSV Exports"| StorageFS
    VM -->|"Generates Content URI"| FileProvider
    FileProvider -->|"Serves File Streams"| ShareSheet
    ShareSheet -->|"Transfers File to External App"| User
```

### Explanation
- **Purpose**: Defines the operational boundary of Stash, showing how the application interfaces with external Android OS subsystems (Biometrics, WindowManager, Scoped Storage, Native Dialer, and WhatsApp) without relying on any external remote cloud servers or proprietary third-party SDKs.
- **Key Files**: `MainActivity.kt`, `AndroidManifest.xml`, `file_paths.xml`.
- **Patterns Used**: Intent-based external service integration, ContentProvider delegation, Hardware abstraction.

---

## 3. Diagram 2: High-Level Layers & Architecture Modules

```mermaid
graph TB
    subgraph Presentation_Layer["Presentation Layer (Jetpack Compose & Material 3)"]
        MainActivity["MainActivity (FragmentActivity & Lifecycle Controller)"]
        Pager["HorizontalPager (beyondViewportPageCount = 1)"]
        Screens["Screens (Home, History, Tags, Settings)"]
        Modals["Bottom Sheets (Add, Spend, Transfer, Edit, LendMoney, LentMoneyManager)"]
        Theme["Theme & Dynamic Monet ColorScheme (StashTheme: API 31-35)"]
    end

    subgraph Domain_State_Layer["State & Business Logic Layer"]
        MainVM["MainViewModel (AndroidViewModel)"]
        StateHolders["StateFlow (transactions, loans, settings)"]
        AnalyticsEngine["Analytics Engine (WeeklyTrends, MonthComparison, CategorySummaries)"]
        TransferEngine["Wallet Transfer Calculator (Online ⇄ Cash Dual Allocation)"]
        LendingEngine["Lending & Repayment Engine (Asset tracking, balance deduction, repayment restoration)"]
        BackupEngine["Serialization Engine (CSV Generator, JSON Backup/Restore)"]
    end

    subgraph Data_Layer["Data & Persistence Layer"]
        subgraph Room_Persistence["Room Persistence (Explicit Migrations)"]
            DB["AppDatabase (RoomDatabase - exportSchema=true, version=4)"]
            TxDao["TransactionDao (Flow, Insert, Update, Delete)"]
            LoanDao["LoanDao (Flow, Insert, Update, Delete)"]
            TxEntity["TransactionEntity"]
            LoanEntity["LoanEntity"]
            Converters["Converters (TypeConverter for Enums)"]
            Migrations["Explicit Migrations (MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)"]
        end
        subgraph Preferences_Persistence["Preferences Persistence"]
            DS["SettingsDataStore"]
            PrefFlow["settingsFlow (StashSettings)"]
        end
    end

    Presentation_Layer -->|"Observes StateFlow / Dispatches Intents"| Domain_State_Layer
    Domain_State_Layer -->|"Dispatches Coroutine IO Operations"| Data_Layer
```

### Explanation
- **Purpose**: Establishes strict separation of concerns into Presentation, Domain/State, and Data Persistence layers.
- **Key Files**: `MainActivity.kt`, `MainViewModel.kt`, `AppDatabase.kt`, `TransactionDao.kt`, `LoanDao.kt`, `SettingsDataStore.kt`.
- **Patterns Used**: Clean Architecture separation, Unidirectional Data Flow (UDF), Repository-less Direct DAO Binding (ideal for embedded Android performance), Offline-First persistence.

---

## 4. Diagram 3: Module & Package Dependency Graph (Imports)

```mermaid
graph LR
    subgraph pkg_ui_theme["com.nan.stash.ui.theme"]
        Theme["Theme.kt"]
        Color["Color.kt"]
        Type["Type.kt"]
        Shape["Shape.kt"]
    end

    subgraph pkg_model["com.nan.stash.data.model"]
        TxType["TransactionType.kt"]
        PayMode["PaymentMode.kt"]
    end

    subgraph pkg_local["com.nan.stash.data.local"]
        TxEntity["TransactionEntity.kt"]
        LoanEntity["LoanEntity.kt"]
        TxDao["TransactionDao.kt"]
        LoanDao["LoanDao.kt"]
        AppDB["AppDatabase.kt"]
        SettingsDS["SettingsDataStore.kt"]
    end

    subgraph pkg_nav["com.nan.stash.ui.navigation"]
        NavRoutes["NavRoutes.kt"]
        BottomNav["BottomNavBar.kt"]
    end

    subgraph pkg_comp["com.nan.stash.ui.components"]
        Common["CommonComponents.kt"]
    end

    subgraph pkg_screens["com.nan.stash.ui.screens"]
        HomeSc["HomeScreen.kt"]
        HistSc["HistoryScreen.kt"]
        TagsSc["TagsScreen.kt"]
        SetSc["SettingsScreen.kt"]
        AddSh["AddMoneyBottomSheet.kt"]
        SpendSh["SpendMoneyBottomSheet.kt"]
        TransSh["TransferMoneyBottomSheet.kt"]
        EditSh["EditTransactionBottomSheet.kt"]
        LendSh["LendMoneyBottomSheet.kt"]
        LentMgr["LentMoneyManagerSheet.kt"]
    end

    subgraph pkg_root["com.nan.stash"]
        MainAct["MainActivity.kt"]
        MainVM["ui.MainViewModel.kt"]
    end

    TxEntity --> TxType
    TxEntity --> PayMode
    LoanEntity --> PayMode
    TxDao --> TxEntity
    LoanDao --> LoanEntity
    AppDB --> TxDao
    AppDB --> LoanDao
    AppDB --> TxEntity
    AppDB --> LoanEntity

    MainVM --> AppDB
    MainVM --> TxDao
    MainVM --> LoanDao
    MainVM --> SettingsDS
    MainVM --> TxType
    MainVM --> PayMode

    HomeSc --> TxEntity
    HomeSc --> LoanEntity
    HomeSc --> PayMode
    HomeSc --> TxType
    HomeSc --> Common
    HistSc --> TxEntity
    HistSc --> TxType
    HistSc --> PayMode
    HistSc --> Common
    LendSh --> PayMode
    LendSh --> Common
    LentMgr --> LoanEntity
    LentMgr --> PayMode
    LentMgr --> Common

    MainAct --> MainVM
    MainAct --> NavRoutes
    MainAct --> BottomNav
    MainAct --> HomeSc
    MainAct --> HistSc
    MainAct --> TagsSc
    MainAct --> SetSc
    MainAct --> AddSh
    MainAct --> SpendSh
    MainAct --> TransSh
    MainAct --> EditSh
    MainAct --> LendSh
    MainAct --> LentMgr
    MainAct --> Theme
```

### Explanation
- **Purpose**: Illustrates compile-time coupling and circular dependency avoidance across packages. Models and entities reside at the lowest dependency tier, consumed upwards by DAOs, ViewModel, Screens, and the root Activity.
- **Key Files**: Entire `com.nan.stash.*` package structure.
- **Patterns Used**: Acyclic Dependency Principle (ADP), Layered Inversion.

---

## 5. Diagram 4: Key End-to-End User Flow Sequences

### 5.1 Wallet Transfer Flow (Online ⇄ Cash Without Distortion)

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant FAB as FAB Menu
    participant TransferSheet as TransferMoneyBottomSheet
    participant VM as MainViewModel
    participant Dao as TransactionDao
    participant DB as SQLite DB
    participant UI as Dashboard

    User->>FAB: Tap "Transfer"
    FAB->>TransferSheet: Opens TransferMoneyBottomSheet
    User->>TransferSheet: Select "Online ➔ Cash" or "Cash ➔ Online"
    User->>TransferSheet: Enter Amount via NumericKeypad
    User->>TransferSheet: Tap "Confirm Transfer"
    TransferSheet->>VM: addTransfer(amount, fromMode, note)
    VM->>Dao: insertTransaction(TransactionEntity(type=TRANSFER, tag="Online → Cash", paymentMode=fromMode))
    Dao->>DB: INSERT INTO transactions
    DB-->>Dao: Invalidation Trigger
    Dao-->>VM: Emits updated Flow
    VM-->>UI: Updates Online (-$X) and Cash (+$X) balances
    Note over UI: Net Balance ($Online + $Cash) and Spending metrics remain completely unchanged
```

### 5.2 Interactive Transaction Editing & Deletion Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant History as HistoryScreen / HomeScreen
    participant EditSheet as EditTransactionBottomSheet
    participant VM as MainViewModel
    participant Dao as TransactionDao

    User->>History: Tap Transaction Item Card
    History->>EditSheet: Opens EditTransactionBottomSheet(item)
    User->>EditSheet: Modify Amount, Category, Note, or Wallet Mode
    alt Save Changes
        User->>EditSheet: Tap "Save Changes"
        EditSheet->>VM: updateTransaction(updatedEntity)
        VM->>Dao: updateTransaction(updatedEntity)
        Dao-->>History: Screen recomposes instantly
    else Delete Transaction
        User->>EditSheet: Tap "Delete"
        EditSheet->>User: Confirmation AlertDialog
        User->>EditSheet: Confirm Delete
        EditSheet->>VM: deleteTransaction(item)
        VM->>Dao: deleteTransaction(item)
        Dao-->>History: Item removed from view
    end
```

### 5.3 Lending Money, Contact Calling & WhatsApp Action Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant FAB as FAB Menu
    participant LendSheet as LendMoneyBottomSheet
    participant Manager as LentMoneyManagerSheet
    participant AndroidSystem as Android OS (Dialer & WhatsApp)
    participant VM as MainViewModel
    participant Room as Room Database

    User->>FAB: Tap "Lend Money"
    FAB->>LendSheet: Opens LendMoneyBottomSheet
    User->>LendSheet: Enter Borrower Name, Phone, Amount, Reason, Due Date, Mode (Cash)
    User->>LendSheet: Tap "Confirm & Record Loan"
    LendSheet->>VM: lendMoney(...)
    VM->>Room: INSERT INTO loans & INSERT INTO transactions (type=LEND, mode=CASH)
    Room-->>VM: Invalidation Tracker
    Note over VM: Cash wallet decreases by loan amount; Owed asset balance increases.
    
    User->>Manager: Tap "Money Lent" Card on Dashboard
    Manager->>User: Displays active borrowers with progress, status, and communication triggers
    alt Direct Phone Call
        User->>Manager: Tap Call Button
        Manager->>AndroidSystem: Intent(ACTION_DIAL, "tel:<clean_number>")
        AndroidSystem-->>User: Opens Native Phone Dialer
    else Direct WhatsApp Chat
        User->>Manager: Tap WhatsApp Button
        Manager->>AndroidSystem: Intent(ACTION_VIEW, "https://api.whatsapp.com/send?phone=...&text=...")
        AndroidSystem-->>User: Opens WhatsApp conversation with customized reminder
    else Partial or Full Repayment
        User->>Manager: Tap "Repay" -> Enter Amount received in Online
        Manager->>VM: recordRepayment(loan, amount, ONLINE, note)
        VM->>Room: UPDATE loans (balance reduced) & INSERT INTO transactions (type=REPAYMENT, mode=ONLINE)
        Note over VM: Online wallet increases; Loan marked settled when balance hits 0.
    end
```

---

## 6. Diagram 5: Entity-Relationship & Persistence Data Model

```mermaid
erDiagram
    TRANSACTIONS ||--o{ TRANSACTION_TYPE : categorized_by
    TRANSACTIONS ||--o{ PAYMENT_MODE : funded_via
    LOANS ||--o{ PAYMENT_MODE : initially_disbursed_via
    LOANS ||--o{ TRANSACTIONS : logs_lend_and_repayment_activity

    TRANSACTIONS {
        INTEGER id PK "autoGenerate = true"
        TEXT type "INCOME | EXPENSE | TRANSFER | LEND | REPAYMENT"
        REAL amount "Double precision financial amount"
        TEXT tag "Category, Direction, or Borrower reference"
        TEXT note "User annotation / description / contact details"
        INTEGER timestamp "Epoch milliseconds (System.currentTimeMillis())"
        TEXT paymentMode "ONLINE | CASH"
    }

    LOANS {
        INTEGER id PK "autoGenerate = true"
        TEXT personName "Borrower name (indexed search)"
        TEXT phoneNumber "Contact phone for direct Call & WhatsApp actions"
        REAL totalAmount "Total cumulative principal lent"
        REAL remainingAmount "Remaining unsettled balance"
        TEXT reason "Purpose or borrowing context"
        INTEGER lendDate "Disbursement timestamp"
        INTEGER dueDate "Target repayment deadline (Nullable)"
        TEXT initialPaymentMode "ONLINE | CASH"
        INTEGER isSettled "0 = Active, 1 = Fully Settled"
    }

    TRANSACTION_TYPE {
        string name "INCOME | EXPENSE | TRANSFER | LEND | REPAYMENT"
    }

    PAYMENT_MODE {
        string name "ONLINE | CASH"
        string displayName "Online Money | Cash Available"
    }

    STASH_SETTINGS {
        string theme_mode "SYSTEM | LIGHT | DARK"
        boolean dynamic_color "Monet dynamic theme toggle"
        string currency_symbol "Default: $"
        boolean biometric_enabled "Fingerprint lock flag"
    }
```

### Explanation
- **Purpose**: Defines SQLite physical schema (Room) and preferences store structure.
- **Key Files**: `TransactionEntity.kt`, `LoanEntity.kt`, `TransactionType.kt`, `PaymentMode.kt`, `SettingsDataStore.kt`.
- **Patterns Used**: Typed SQLite Entities with TypeConverters, Normalized tables, Explicit non-destructive schema migrations (v1 $\rightarrow$ v4).

---

## 7. Diagram 6: Internal API & Navigation Route Map

```mermaid
graph TD
    subgraph NavigationRoutes["Client Navigation Routes (HorizontalPager: beyondViewportPageCount=1)"]
        R0["Route: 'home' (Page 0) -> HomeScreen"]
        R1["Route: 'history' (Page 1) -> HistoryScreen"]
        R2["Route: 'tags' (Page 2) -> TagsScreen"]
        R3["Route: 'settings' (Page 3) -> SettingsScreen"]
    end

    subgraph ViewModelActions["MainViewModel Command Interface (MVI / UDF)"]
        A1["addTransaction(type, amount, tag, note, timestamp, mode)"]
        A2["addTransfer(amount, fromMode, note, timestamp)"]
        A3["lendMoney(person, phone, amount, reason, dueDate, mode, lendDate)"]
        A4["recordRepayment(loan, amountRepaid, mode, note)"]
        A5["addMoreToLoan(loan, additionalAmount, mode, note)"]
        A6["deleteLoan(loan)"]
        A7["updateTransaction(transaction)"]
        A8["deleteTransaction(transaction)"]
        A9["clearAllData()"]
        A10["exportToCsv(transactions): File"]
        A11["createBackupJson(transactions): File"]
        A12["restoreFromJson(jsonString, callback)"]
        A13["setThemeMode / setCurrencySymbol / setBiometricEnabled"]
    end

    subgraph SystemContracts["System Intent & Content Provider Endpoints"]
        I1["ACTION_DIAL -> tel:number"]
        I2["ACTION_VIEW -> https://api.whatsapp.com/send?phone=...&text=..."]
        I3["ACTION_SEND -> FileProvider content://com.nan.stash.provider/... (CSV / JSON)"]
        I4["ACTION_GET_CONTENT -> */* (SAF Document Picker for JSON Restore)"]
    end

    R0 -->|"Dashboard Actions"| A1
    R0 -->|"Dashboard Actions"| A2
    R0 -->|"Borrower Management"| A3
    R0 -->|"Repayments"| A4
    R1 -->|"Ledger Operations"| A7
    R1 -->|"Ledger Operations"| A8
    R3 -->|"Preferences"| A10
    R3 -->|"Preferences"| A11
    R3 -->|"Preferences"| A12
    R3 -->|"Preferences"| A13
    A3 --> I1
    A3 --> I2
    A10 --> I3
    A11 --> I3
    A12 --> I4
```

### Explanation
- **Purpose**: Maps application navigation destinations, ViewModel public action handlers, and system-level IPC intent contracts.
- **Key Files**: `NavRoutes.kt`, `BottomNavBar.kt`, `MainViewModel.kt`, `MainActivity.kt`.
- **Patterns Used**: MVI intent dispatching, SAF intent contracts, URI grant sharing via FileProvider.

---

## 8. Diagram 7: State Management & Reactive Data Flow (UDF)

```mermaid
flowchart TD
    subgraph UI_Layer["UI Layer (Compose Recomposition Tree)"]
        UI_Events["User Interaction (Keypad touch, chip select, swipe)"]
        UI_Observables["collectAsStateWithLifecycle / collectAsState"]
        UI_Widgets["HomeScreen, HistoryScreen, TagsScreen, SettingsScreen"]
    end

    subgraph ViewModel_Layer["ViewModel Layer (MainViewModel)"]
        Action_Receivers["Dispatched Action Methods"]
        Coroutine_Scope["viewModelScope (Dispatchers.IO)"]
        Flow_Pipelines["Combine & Transform Operators"]
        State_Holders["StateFlow (transactions, loans, settings)"]
    end

    subgraph Persistence_Layer["Persistence Layer"]
        Room_Invalidation["Room SQLite Invalidation Tracker"]
        Room_Tables["transactions & loans tables"]
        DataStore_Disk["stash_settings.preferences_pb"]
    end

    UI_Events -->|"Dispatches User Actions"| Action_Receivers
    Action_Receivers -->|"Launches Coroutines"| Coroutine_Scope
    Coroutine_Scope -->|"Writes (Insert / Update / Delete)"| Room_Tables
    Coroutine_Scope -->|"Writes (DataStore edit)"| DataStore_Disk
    Room_Tables -->|"Triggers On Table Modification"| Room_Invalidation
    Room_Invalidation -->|"Emits New List from SQLite"| Flow_Pipelines
    DataStore_Disk -->|"Emits Updated Preferences"| Flow_Pipelines
    Flow_Pipelines -->|"Updates Values"| State_Holders
    State_Holders -->|"Streamed As State"| UI_Observables
    UI_Observables -->|"Triggers Recomposition"| UI_Widgets
```

### Explanation
- **Purpose**: Visualizes the single source of truth reactive pipeline. The UI never mutates state directly; it issues intents to the ViewModel, which executes SQLite transactions on `Dispatchers.IO`. Room's invalidation tracker automatically re-emits updated data into `StateFlow`, driving atomic UI recompositions.
- **Key Files**: `MainViewModel.kt`, `TransactionDao.kt`, `LoanDao.kt`, `HomeScreen.kt`.
- **Patterns Used**: Unidirectional Data Flow (UDF), Observer Pattern via Kotlin Coroutines `Flow`, Reactive SQLite.

---

## 9. Diagram 8: Authentication & Security Flow (Biometric Lock)

```mermaid
stateDiagram-v2
    [*] --> ApplicationLaunch: User Opens Stash
    ApplicationLaunch --> ReadPreferences: Load Settings from DataStore

    state ReadPreferences <<choice>>
    ReadPreferences --> UnlockedState: isBiometricEnabled == false
    ReadPreferences --> LockedGate: isBiometricEnabled == true

    state LockedGate {
        [*] --> DisplayLockScreen: isAppUnlocked = false
        DisplayLockScreen --> TriggerPrompt: Auto-trigger promptBiometric()
        TriggerPrompt --> Authenticating: BiometricPrompt.authenticate()
        
        state AuthResult <<choice>>
        Authenticating --> AuthResult: User touches sensor / inputs PIN
        AuthResult --> Success: Authentication Succeeded
        AuthResult --> Retry: Authentication Failed (Soft Error)
        AuthResult --> Denied: Authentication Error (Hard Cancel / Fail)
        
        Retry --> Authenticating: Retry scan
        Denied --> DisplayLockScreen: Show "Unlock with Biometrics" button
    }

    Success --> UnlockedState: isAppUnlocked = true
    UnlockedState --> MainDashboard: Render Scaffold & HorizontalPager

    MainDashboard --> AppBackgrounded: User Minimizes App / Screen Off (onStop)
    AppBackgrounded --> ResetLock: isLaunchingPicker == false
    ResetLock --> DisplayLockScreen: isAppUnlocked = false (Re-arm lock)
```

### Explanation
- **Purpose**: Explains how biometric security protects sensitive financial records during app launch and resume cycles.
- **Key Files**: `MainActivity.kt`, `SettingsDataStore.kt`.
- **Patterns Used**: Activity Lifecycle state machine (`onStop`/`onResume`), Defensive Re-locking, Temporary Intent-Launcher Bypass (`isLaunchingPicker` prevents lock triggering when selecting system files for CSV/JSON).

---

## 10. Diagram 9: Build, Packaging & Deployment Pipeline

```mermaid
flowchart TD
    subgraph Source_Files["Source Artifacts"]
        KT["Kotlin 2.0.21 Source Files"]
        XML["Android XML (Manifest, Resources, file_paths)"]
        Schemas["Room Schemas (1.json -> 4.json)"]
    end

    subgraph Gradle_Pipeline["Gradle Build Process (AGP 8.7.2)"]
        KSP["KSP (Kotlin Symbol Processing) -> AppDatabase_Impl & DAOs"]
        Kotlinc["Kotlin Compiler (compileDebugKotlin)"]
        Javac["Java Compiler (compileDebugJavaWithJavac)"]
        AAPT2["AAPT2 (Android Asset Packaging Tool - Overridden for Termux)"]
        R8["R8 Minification & Dead-Code Stripping (isMinifyEnabled=true)"]
        PackageDebug["packageDebug -> Unaligned APK"]
    end

    subgraph Optimization_Signing["Packaging & Verification Tools"]
        ZipAlign["zipalign -f -p 4 (4-byte boundary alignment)"]
        ApkSigner["apksigner (V2 APK Signature Scheme with debug.keystore)"]
    end

    subgraph Target_Outputs["Shipped Deliverables"]
        APK["/storage/emulated/0/Download/Stash.apk (9.4 MB)"]
        ZIP["/storage/emulated/0/github/Stash-source.zip (115 KB)"]
        ARCH["/storage/emulated/0/Download/architecture.md"]
    end

    KT --> KSP
    Schemas --> KSP
    KSP --> Kotlinc
    KT --> Kotlinc
    Kotlinc --> Javac
    XML --> AAPT2
    Javac --> R8
    AAPT2 --> R8
    R8 --> PackageDebug
    PackageDebug --> ZipAlign
    ZipAlign --> ApkSigner
    ApkSigner --> APK
    KT --> ZIP
    XML --> ZIP
    Schemas --> ZIP
    APK --> ARCH
```

### Explanation
- **Purpose**: Details the complete compilation, resource optimization, dexing, alignment, and cryptographic signing pipeline executing entirely inside Termux on Android.
- **Key Files**: `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `proguard-rules.pro`.
- **Patterns Used**: Ahead-Of-Time (AOT) Symbol Processing via KSP, R8 Code Shrinking, Zero-padding Zip alignment, V2 signature verification.

---

## 11. Diagram 10: Class Diagrams for Core Business Logic

```mermaid
classDiagram
    class MainActivity {
        -MainViewModel viewModel
        -Boolean isAppUnlocked
        -Boolean isSettingsLoaded
        -Boolean showFabMenu
        -Boolean showAddSheet
        -Boolean showSpendSheet
        -Boolean showTransferSheet
        -Boolean showLendSheet
        -Boolean showLentManagerSheet
        -TransactionEntity editingTransaction
        +promptBiometric() Unit
        +onCreate(Bundle) Unit
        +onResume() Unit
        +onStop() Unit
    }

    class MainViewModel {
        -AppDatabase db
        -TransactionDao transactionDao
        -LoanDao loanDao
        -SettingsDataStore settingsDataStore
        +StateFlow~List~TransactionEntity~~ transactions
        +StateFlow~List~LoanEntity~~ loans
        +StateFlow~StashSettings~ settings
        +addTransaction(type, amount, tag, note, timestamp, mode) Unit
        +addTransfer(amount, fromMode, note, timestamp) Unit
        +lendMoney(person, phone, amount, reason, dueDate, mode, lendDate) Unit
        +recordRepayment(loan, amount, mode, note) Unit
        +addMoreToLoan(loan, amount, mode, note) Unit
        +deleteLoan(loan) Unit
        +updateTransaction(transaction) Unit
        +deleteTransaction(transaction) Unit
        +clearAllData() Unit
        +calculateWeeklyTrends(transactions) List~DayTrend~
        +calculateCategorySummaries(transactions) List~CategorySummary~
        +calculateMonthComparison(transactions) MonthComparison
    }

    class AppDatabase {
        <<abstract>>
        +transactionDao() TransactionDao
        +loanDao() LoanDao
        +$ALL_MIGRATIONS Array~Migration~
        +$MIGRATION_1_2 Migration
        +$MIGRATION_2_3 Migration
        +$MIGRATION_3_4 Migration
        +$getDatabase(Context) AppDatabase
    }

    class TransactionEntity {
        +Long id
        +TransactionType type
        +Double amount
        +String tag
        +String note
        +Long timestamp
        +PaymentMode paymentMode
    }

    class LoanEntity {
        +Long id
        +String personName
        +String phoneNumber
        +Double totalAmount
        +Double remainingAmount
        +String reason
        +Long lendDate
        +Long? dueDate
        +PaymentMode initialPaymentMode
        +Boolean isSettled
    }

    class TransactionType {
        <<enumeration>>
        INCOME
        EXPENSE
        TRANSFER
        LEND
        REPAYMENT
    }

    class PaymentMode {
        <<enumeration>>
        ONLINE
        CASH
        +String displayName
    }

    MainActivity --> MainViewModel : observes StateFlow & dispatches actions
    MainViewModel --> AppDatabase : reads DAOs & executes transactions
    MainViewModel --> TransactionEntity : queries/persists
    MainViewModel --> LoanEntity : queries/persists
    TransactionEntity --> TransactionType : discriminates
    TransactionEntity --> PaymentMode : categorizes
    LoanEntity --> PaymentMode : tracks disbursement mode
```

---

## 12. Tech Stack Inventory

| Component | Library / Tool | Version | Role in Architecture |
|---|---|---|---|
| **Language** | Kotlin | 2.0.21 | Primary language; coroutines, typed flows, data classes |
| **Compiler / Code Gen** | KSP | 2.0.21-1.0.28 | Generates Room database and DAO implementations |
| **Build System** | AGP (Android Gradle Plugin) | 8.7.2 | Builds, dexes, shrinks, and packages the Android artifact |
| **UI Framework** | Jetpack Compose (Compose BOM) | 2024.11.00 | Modern declarative reactive UI; zero XML view bindings |
| **Material System** | Material 3 (Material You) | 1.3.1 (via BOM) | Dynamic wallpaper-based palette theming (API 31-35) |
| **Persistence (SQL)** | AndroidX Room | 2.6.1 | Type-safe SQLite abstraction layer with explicit migrations (v1-v4) |
| **Database Testing** | AndroidX Room Testing | 2.6.1 | `MigrationTestHelper` verifying schema transitions (v1->v2, v2->v3, v3->v4) |
| **Preferences Store** | Jetpack DataStore Preferences | 1.1.1 | Asynchronous, transactional key-value store for app settings |
| **Authentication** | AndroidX Biometric | 1.2.0-alpha05 | Unified prompt for Fingerprint and Device Credentials |
| **External Communications** | Native Android Intent Contracts | Platform APIs | Standard `ACTION_DIAL` and `ACTION_VIEW` for Call & WhatsApp |
| **Display Optimization** | WindowManager API 34+ | Android Platform | 120Hz display mode pinning + FrameRateBoostOnTouch |

---

## 13. Deep-Dive Analysis: Risks, Coupling, Dead Code & Roadmap

### 13.1 Architectural Risks & Mitigation
1. **Financial Precision with `Double`**:
   - *Risk*: `TransactionEntity.amount` and `LoanEntity.remainingAmount` use 64-bit IEEE 754 floating point (`Double`). Repeated fraction additions (e.g. `$0.10 + $0.20 = 0.30000000000000004`) can produce subtle rounding inaccuracies over years of tracking.
   - *Mitigation*: Formatters strictly round to 2 decimal places with `Locale.US` and `String.format("%.2f")`. Future migration should transition balances to `Long` representing minor units (cents).
2. **Schema Evolution & Migration Safety**:
   - *Risk*: Room's `.fallbackToDestructiveMigration()` was historically a major hazard for users upon app updates.
   - *Mitigation*: Completely removed `.fallbackToDestructiveMigration()`. `exportSchema = true` is enabled, and all versions have explicit `Migration(from, to)` classes validated with `MigrationTestHelper`.

### 13.2 Coupling Analysis
- **Tight Coupling**: `MainActivity.kt` orchestrates both top-level navigation, biometric lifecycle listeners, and all modal bottom sheet presentations.
- *Recommendation*: As the feature set expands, extract modal sheets into a dedicated modal host coordinator or navigation graph receiver.
- **Loose Coupling**: Business analytics calculations (`calculateWeeklyTrends`, `calculateCategorySummaries`, `calculateMonthComparison`) are pure, side-effect-free functions inside `MainViewModel.kt`, making them easily unit-testable.

### 13.3 Dead Code Analysis
- Removed unused hardcoded color references in favor of Material 3 dynamic tokens.
- Deprecated static color pickers replaced with semantic `MaterialTheme.colorScheme` tokens (`primary`, `secondary`, `tertiary`, `surfaceContainer`, `outline`).

### 13.4 Native Keyboard & Input Optimization
- **System IME Migration**: Replaced custom `NumericKeypad` grid with native system decimal keyboard (`KeyboardOptions(keyboardType = KeyboardType.Decimal)`).
- **Auto-Focus & Smooth Scroll**: Added `FocusRequester` with delayed auto-request and wrapped form columns in `.verticalScroll(rememberScrollState())` to handle Android IME insets fluidly across all screen sizes and foldables.
- **Direct Currency Formatting**: Amount inputs now format live with dynamic currency prefixes (`-$`, `+$`, `$`) styled according to semantic financial token schemes.

### 13.5 Global Unified Navigation Transition
- **120Hz Transition Architecture**: Switched tab switching from instant discrete snaps (`scrollToPage`) to a unified `animateScrollToPage` with `tween(durationMillis = 280, easing = FastOutSlowInEasing)` globally across all tab transitions (Home ⇄ Tags, Home ⇄ Settings, History ⇄ Settings).
- **Zero-Jank Pre-rendering**: Enabled `beyondViewportPageCount = 1` so adjacent and intermediate pages are maintained in the composition tree, avoiding dropped frames during cross-tab navigation jumps.

### 13.6 Recommended Future Improvements
1. **Recurring Transactions / Reminders**: Scheduled WorkManager jobs to automatically notify users before loan repayment deadlines.
2. **Biometric Session Timeout**: Allow users to configure a lock timeout (e.g. "Lock immediately", "After 1 minute", "After 5 minutes").
3. **Multi-Currency Converter**: Real-time or cached currency conversion for cross-border expenses.
4. **Receipt Image Attachment**: Scoped storage photo picker to attach image proofs to transactions or loan records.
