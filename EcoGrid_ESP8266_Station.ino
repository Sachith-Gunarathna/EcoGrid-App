#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>

#define WIFI_SSID ""
#define WIFI_PASSWORD ""

#define FIREBASE_HOST ""
#define FIREBASE_AUTH ""

#define RELAY_PIN D5

FirebaseData fbData;
FirebaseData fbWrite;
FirebaseAuth auth;
FirebaseConfig config;

String basePath = "/stations/ECOGRID-ALAWWA-STATION-01/current_session";

bool isBatteryInit = false;
int currentBatt = 0;
unsigned long prevMillis = 0;
const unsigned long INTERVAL = 5000;

void setRelay(bool isOn) {

  if (isOn) {
    digitalWrite(RELAY_PIN, HIGH);
  } else {
    digitalWrite(RELAY_PIN, LOW);
  }
}

void writeStatus(String newStatus) {
  Firebase.setString(fbWrite, basePath + "/status", newStatus);
}

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  setRelay(false);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  String status = "";


  if (Firebase.getString(fbData, basePath + "/status")) {
    status = fbData.stringData();

    Serial.println("Firebase DB Status: " + status);
  } else {

    Serial.println("Firebase Error: " + fbData.errorReason());
  }

  if (status == "CHARGING") {
    setRelay(true);
    int targetPct = 100;

    if (Firebase.getString(fbData, basePath + "/targetPercentage")) {
      String t = fbData.stringData();
      t.replace("%", "");
      t.trim();
      int parsed = (int)t.toFloat();
      if (parsed > 0) targetPct = parsed;
    }

    if (!isBatteryInit) {
      if (Firebase.getInt(fbData, basePath + "/livePercentage")) {
        currentBatt = fbData.intData();
        isBatteryInit = true;
        Serial.println("[INIT] Starting battery: " + String(currentBatt));
      } else if (Firebase.getInt(fbData, basePath + "/current_percent")) {
        currentBatt = fbData.intData();
        isBatteryInit = true;
      }
    }

    unsigned long now = millis();
    if (now - prevMillis > INTERVAL) {
      prevMillis = now;
      currentBatt++;

      float batteryCapacityKwh = 40.0;
      float chargerPowerKw = 7.4;
      float chargedPct = (float)(currentBatt);
      float energyKwh = (chargedPct / 100.0) * batteryCapacityKwh;
      int timeLeftMins = (int)(((targetPct - currentBatt) / 100.0) *
              batteryCapacityKwh / chargerPowerKw * 60.0);

      if (timeLeftMins < 0) timeLeftMins = 0;

      Serial.println("[CHARGING] " + String(currentBatt) + "/" +
              String(targetPct) + "% " + String(timeLeftMins) +
              " min left");

      Firebase.setInt(fbWrite, basePath + "/livePercentage", currentBatt);
      Firebase.setInt(fbWrite, basePath + "/current_percent", currentBatt);
      Firebase.setDouble(fbWrite, basePath + "/energy_kwh", energyKwh);
      Firebase.setInt(fbWrite, basePath + "/time_left_mins", timeLeftMins);

      if (currentBatt >= targetPct) {
        Serial.println("[DONE] Target reached! Stopping...");
        setRelay(false);
        writeStatus("COMPLETED");
        currentBatt = 0;
        isBatteryInit = false;
      }
    }
  } else if (status == "COMPLETED" || status == "CANCELED") {
    setRelay(false);
    currentBatt = 0;
    isBatteryInit = false;
  }
}