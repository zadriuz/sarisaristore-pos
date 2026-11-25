# Ate Edna's Sari-Sari Store POS

Android point-of-sale and inventory management application built for daily sari-sari store operations, backed by Firebase Realtime Database.

> **Status:** Source code and sanitized Firebase configuration templates are preserved for reference. Live database rules are locked against public access.

<!--
### Interface Walkthrough

| Dashboard & Analytics | POS Checkout | Inventory & Sidebar |
| :---: | :---: | :---: |
| ![Dashboard](screenshots/dashboard.png) | ![POS](screenshots/pos.png) | ![Inventory](screenshots/inventory.png) |
| *Daily sales summary and store metrics* | *Itemized checkout with custom UI dialogs* | *Stock management via swipeable sidebar* |
-->

---

### Role & Context

- **Role:** Solo Developer
- **Client / Use Case:** Built for Ate Edna's Sari-Sari Store to replace manual notebook tracking of daily sales and stock counts
- **Platform:** Native Android (`com.example.sarisaristoreapp`)

---

### Key Features

- **Point-of-Sale Checkout:** Log retail item sales, compute transaction totals, and confirm orders through custom UI dialogs.
- **Inventory Management:** Add, edit, and monitor product prices and available stock quantities.
- **Dashboard Analytics:** View daily revenue summaries and store activity at a glance.
- **Swipeable Sidebar Navigation:** Switch between POS checkout, inventory, and analytics views using a gesture-friendly navigation drawer.
- **Cloud Data Sync:** Persists product records, stock updates, and transaction logs in Firebase Realtime Database.

---

### Tech Stack & Architecture

| Component | Technology |
| :--- | :--- |
| **Language** | Java |
| **Platform & UI** | Android SDK, Custom Dialogs, Swipeable Sidebar Drawer |
| **Backend & Database** | Firebase Realtime Database |
| **Build System** | Gradle (Kotlin DSL — `build.gradle.kts`) |

#### Repository Structure

```text
sarisaristore-pos/
├── app/
│   ├── src/main/                  # Activities, adapters, models, and XML layouts
│   └── google-services-example.json # Sanitized Firebase configuration template
├── gradle/                        # Gradle wrapper configuration
├── build.gradle.kts               # Top-level Gradle build script
└── settings.gradle.kts            # Project module settings
```

---

### Local Setup (Reference)

1. Clone this repository and open it in **Android Studio**.
2. Copy `app/google-services-example.json` to `app/google-services.json` and replace the placeholder fields with your own Firebase project credentials.
3. Enable **Realtime Database** in your Firebase Console.
4. Sync Gradle and run the `app` configuration on an Android emulator or physical device.
