# ⚡ EcoGrid - Smart EV Charging Station & Grid Network System

[![Android Version](https://img.shields.io/badge/Android-7.0%2B%20%28API%2024%2B%29-brightgreen.svg)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28.svg)](https://firebase.google.com/)
[![ESP8266](https://img.shields.io/badge/IoT-ESP8266-red.svg)](https://www.espressif.com/)

**EcoGrid** is an end-to-end Smart Electric Vehicle (EV) Charging Station and Network Management System. It combines a feature-rich native Android mobile app with an IoT-enabled ESP8266 hardware station controller for real-time charging, remote relay actuation, live battery telemetry, and seamless payment processing.

---

## 🚀 Key Features

### 📱 Android Mobile Application

* 🔐 **User Authentication & Social Sign-In**:
  * Email & Password registration with OTP email verification.
  * Google Sign-In (Credential Manager) and Facebook SDK authentication.
  * FCM Token synchronization for personalized push notifications.

* 🗺️ **Interactive EV Station Navigation**:
  * Google Maps integration for locating nearby EV charging stations.
  * Route navigation, station status details, and location filtering.

* 📷 **QR Code Scanner**:
  * Integrated QR scanner for fast station scanning and session initiation.

* ⚡ **Live Charging Session Monitoring**:
  * Real-time sync with Firebase Realtime Database.
  * Visual live progress tracking (Battery percentage, kWh energy consumed, estimated time remaining).
  * Remote session termination / manual auto-stop controls.

* 💳 **Payments & Wallet Integration**:
  * Integrated **PayHere Payment Gateway SDK**.
  * Add & manage payment cards securely.
  * Digital wallet balance management, transaction history, and invoice records.

* 🚗 **Vehicle Management**:
  * Register multiple electric vehicles (Make, Model, License Plate, Battery Capacity).
  * Quick vehicle selection before initiating charging sessions.

* 🔔 **Push Notifications**:
  * Firebase Cloud Messaging (FCM) notifications for session start/completion, payment alerts, and updates.

---

### 🔌 IoT Station Hardware Firmware (`EcoGrid_ESP8266_Station.ino`)

* **ESP8266 Wi-Fi Station Controller**:
  * Real-time bidirectional communication with Firebase Realtime Database.
  * **Relay Switch Actuation**: Automatically powers ON/OFF charging relays based on station commands.
  * **Automated Charging Loop**: Simulates/calculates energy transfer (kWh) and auto-stops when target battery percentage is reached.
  * **Fail-Safe & Status Monitoring**: Listens to session status changes (`CHARGING`, `COMPLETED`, `CANCELED`, `IDLE`).

---

## 🛠️ Tech Stack & Architecture

### Mobile App
* **Language**: Java 11
* **UI & Architecture**: Material Design 3, View Binding, Custom Drawables & Fragments
* **Backend Services**: 
  * Firebase Auth (Email/Password, Google, Facebook)
  * Cloud Firestore (User profiles, vehicles, transactions, station metadata)
  * Firebase Realtime Database (Live charging session telemetry & hardware sync)
  * Firebase Cloud Messaging (FCM) & Firebase Storage
* **Maps & Location**: Google Maps SDK, Google Play Services Location
* **Payments**: PayHere Android SDK
* **Libraries & Tools**: Lombok, Glide, OkHttp, Volley, Code Scanner (ZXing wrapper), MPAndroidChart

### IoT Firmware
* **Hardware**: ESP8266 NodeMCU / WeMos D1
* **Language**: C++ (Arduino Framework)
* **Libraries**: `ESP8266WiFi`, `FirebaseESP8266`

---

## 📂 Project Structure

```text
EcoGrid-App/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/lk/leadco/ecogrid/
│   │   │   │   ├── activity/        # App Screens (SignIn, ActiveCharging, Payments, Vehicles, etc.)
│   │   │   │   ├── fragment/        # Home, Navigation, Scanner, Wallet, Profile, Settings
│   │   │   │   ├── model/           # Data Models (User, EVStation, Vehicle, Payment, Invoice, etc.)
│   │   │   │   ├── Adapter/         # RecyclerView Adapters (VehicleAdapter, TransactionAdapter, etc.)
│   │   │   │   └── utils/           # Dialogs, FCM Service, SharedPrefs, OBD Manager, Notifications
│   │   │   ├── res/                 # Layout XMLs, Drawables, Values, Colors
│   │   │   └── AndroidManifest.xml  # App Permissions & Declarations
│   └── build.gradle                 # Module dependencies & SDK config
├── EcoGrid_ESP8266_Station.ino      # ESP8266 Firmware Code
├── build.gradle                     # Top-level Gradle config
└── README.md                        # Documentation
```

---

## ⚙️ Getting Started & Setup

### Prerequisites

1. **Android Development**:
   * Android Studio Ladybug or newer.
   * JDK 11 or higher.
   * Android SDK with API Level 24+ support.

2. **Hardware / IoT Development**:
   * Arduino IDE (or PlatformIO).
   * ESP8266 Board Package installed in Arduino IDE.
   * `FirebaseESP8266` library installed.

---

### Step 1: Firebase Setup

1. Create a Firebase Project on the [Firebase Console](https://console.firebase.google.com/).
2. Add an **Android Application** with package name `lk.leadco.ecogrid`.
3. Download `google-services.json` and place it inside the `app/` folder.
4. Enable **Authentication** (Email/Password, Google, Facebook).
5. Provision **Cloud Firestore** and **Realtime Database**.
6. Enable **Firebase Cloud Messaging**.

---

### Step 2: API Keys Setup

Add your credentials in `app/src/main/res/values/strings.xml` (or `secrets.xml`):

```xml
<string name="google_maps_key">YOUR_GOOGLE_MAPS_API_KEY</string>
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
```

---

### Step 3: Hardware Firmware Setup (ESP8266)

1. Open `EcoGrid_ESP8266_Station.ino` in Arduino IDE.
2. Update the configuration constants:
   ```cpp
   #define WIFI_SSID       "YOUR_WIFI_SSID"
   #define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"
   #define FIREBASE_HOST   "YOUR_FIREBASE_RTDB_URL"
   #define FIREBASE_AUTH   "YOUR_FIREBASE_DATABASE_SECRET"
   #define STATION_ID      "YOUR_STATION_DOCUMENT_ID"
   ```
3. Connect relay signal pin to `D1` (GPIO5).
4. Flash the code to your ESP8266 board.

---

### Step 4: Build & Run Mobile App

1. Open the project in Android Studio.
2. Sync Gradle dependencies:
   ```bash
   ./gradlew build
   ```
3. Select an emulator or connected physical Android device (API 24+).
4. Click **Run (`Shift + F10`)**.

---

## 📄 License

This project is developed for **EcoGrid**. All rights reserved.
