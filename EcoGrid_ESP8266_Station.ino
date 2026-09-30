#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>

// ─────────────────────────────────────────
//  CONFIG — change these per station
// ─────────────────────────────────────────
#define WIFI_SSID       "4G-MIFI-3DD7"
#define WIFI_PASSWORD   "Sachith@Nethmi@"

#define FIREBASE_HOST   "echogrid-e13a5-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH   "kV50kKmGE8sO7l0aLHFgOAVhfoZ61DE4f5Y7iyAZ"

// ⚠️  Change this to match the station document ID in Firebase RTDB
//     e.g. the key under /stations/ in your DB
#define STATION_ID      "Xn6U6LAKg16R4BLy9RXz"

#define RELAY_PIN D1

// ─────────────────────────────────────────
//  Firebase objects
// ─────────────────────────────────────────
FirebaseData   fbData;
FirebaseData   fbWrite;       // separate object for writes (avoids stream conflicts)
FirebaseAuth   auth;
FirebaseConfig config;

// ─────────────────────────────────────────
//  Session state
// ─────────────────────────────────────────
String  basePath        = "/stations/" STATION_ID "/current_session";
bool    isBatteryInit   = false;
int     currentBatt     = 0;
unsigned long prevMillis = 0;
const long    INTERVAL   = 1000;   // update Firebase every 1 second

// ─────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────
void setRelay(bool on) {
  digitalWrite(RELAY_PIN, on ? HIGH : LOW);
  Serial.println(on ? "[RELAY] ON" : "[RELAY] OFF");
}

void writeStatus(String status) {
  Firebase.setString(fbWrite, basePath + "/status", status);
  Firebase.setString(fbWrite, basePath + "/relay",  status == "CHARGING" ? "ON" : "OFF");
}

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  setRelay(false);

  // ── Wi-Fi ──
  Serial.println("\nConnecting to Wi-Fi...");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500); Serial.print(".");
  }
  Serial.println("\nWiFi connected!  IP: " + WiFi.localIP().toString());

  // ── Firebase ──
  config.database_url                = FIREBASE_HOST;
  config.signer.tokens.legacy_token  = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  Serial.println("[Firebase] Ready.  Station: " STATION_ID);
}

void loop() {
  // ── Read current status ──────────────────────────
  if (!Firebase.getString(fbData, basePath + "/status")) {
    Serial.println("[ERROR] status read: " + fbData.errorReason());
    delay(2000);
    return;
  }

  String status = fbData.stringData();
  Serial.println("[STATUS] " + status);

  // ── CHARGING ─────────────────────────────────────
  if (status == "CHARGING") {
    setRelay(true);

    // Read target percentage (written by app as "80.0")
    int targetPct = 100;
    if (Firebase.getString(fbData, basePath + "/targetPercentage")) {
      String t = fbData.stringData();
      t.replace("%", ""); t.trim();
      int parsed = (int) t.toFloat();
      if (parsed > 0) targetPct = parsed;
    }

    // Initialise battery from what the app wrote (livePercentage)
    if (!isBatteryInit) {
      if (Firebase.getInt(fbData, basePath + "/livePercentage")) {
        currentBatt   = fbData.intData();
        isBatteryInit = true;
        Serial.println("[INIT] Starting battery: " + String(currentBatt) + "%");
      } else if (Firebase.getInt(fbData, basePath + "/current_percent")) {
        currentBatt   = fbData.intData();
        isBatteryInit = true;
      }
    }

    // ── 1-second tick ──
    unsigned long now = millis();
    if (now - prevMillis >= INTERVAL) {
      prevMillis = now;
      currentBatt++;

      // Calculate energy & time remaining
      // Assume 40 kWh battery, 7.4 kW charger  (adjust if needed)
      float batteryCapacityKwh = 40.0;
      float chargerPowerKw     = 7.4;
      float chargedPct         = (float)(currentBatt - 0);   // simplification
      float energyKwh          = (chargedPct / 100.0) * batteryCapacityKwh;
      int   timeLeftMins       = (int)(((targetPct - currentBatt) / 100.0) *
                                        batteryCapacityKwh / chargerPowerKw * 60.0);
      if (timeLeftMins < 0) timeLeftMins = 0;

      Serial.println("[CHARGING] " + String(currentBatt) + "% / " +
                     String(targetPct) + "% | " +
                     String(energyKwh, 3) + " kWh | " +
                     String(timeLeftMins) + " min left");

      // ── Push live data to Firebase ──
      Firebase.setInt(fbWrite,    basePath + "/livePercentage",  currentBatt);
      Firebase.setInt(fbWrite,    basePath + "/current_percent", currentBatt);
      Firebase.setDouble(fbWrite, basePath + "/energy_kwh",      energyKwh);
      Firebase.setInt(fbWrite,    basePath + "/time_left_mins",  timeLeftMins);

      // ── Target reached → auto stop ──
      if (currentBatt >= targetPct) {
        Serial.println("[DONE] Target reached! Stopping...");
        setRelay(false);
        writeStatus("COMPLETED");
        currentBatt   = 0;
        isBatteryInit = false;
      }
    }

  // ── COMPLETED / CANCELED ─────────────────────────
  } else if (status == "COMPLETED" || status == "CANCELED") {
    setRelay(false);
    currentBatt   = 0;
    isBatteryInit = false;

  // ── IDLE / unknown ───────────────────────────────
  } else {
    setRelay(false);
  }

  delay(800);  // slightly under 1s so the millis() tick doesn't drift
}
