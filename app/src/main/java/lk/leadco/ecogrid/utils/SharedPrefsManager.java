package lk.leadco.ecogrid.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {

    private static final String PREF_NAME = "EcoGridPrefs";
    
    private static final String KEY_ACTIVE_VEHICLE = "activeVehicle";
    private static final String KEY_CHARGING_STATUS = "chargingStatus";
    private static final String KEY_CURRENT_BATTERY_LEVEL = "currentBatteryLevel";
    private static final String KEY_PAUSE_NOTIFICATION = "pauseNotification";
    private static final String KEY_DEFAULT_TARGET_LIMIT = "defaultTargetLimit";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveActiveVehicle(Context context, String vehicleNo) {
        getPrefs(context).edit().putString(KEY_ACTIVE_VEHICLE, vehicleNo).apply();
    }

    public static String getActiveVehicle(Context context) {
        return getPrefs(context).getString(KEY_ACTIVE_VEHICLE, "Unknown Vehicle");
    }

    public static void saveChargingStatus(Context context, boolean status){
        getPrefs(context).edit().putBoolean(KEY_CHARGING_STATUS, status).apply();
    }

    public static void saveCurrentBatteryLevel(Context context, int batteryLevel){
        getPrefs(context).edit().putInt(KEY_CURRENT_BATTERY_LEVEL, batteryLevel).apply();
    }

    public static int getCurrentBatteryLevel(Context context) {
        return getPrefs(context).getInt(KEY_CURRENT_BATTERY_LEVEL, 0);
    }

    public static void savePauseNotification(Context context, boolean status) {
         getPrefs(context).edit().putBoolean(KEY_PAUSE_NOTIFICATION, status).apply();
    }

    public static boolean getPauseNotification(Context context){
        return getPrefs(context).getBoolean(KEY_PAUSE_NOTIFICATION, false);
    }

    public static void saveDefaultTargetLimit(Context context, int limit) {
        getPrefs(context).edit().putInt(KEY_DEFAULT_TARGET_LIMIT, limit).apply();
    }

    public static int getDefaultTargetLimit(Context context) {
        return getPrefs(context).getInt(KEY_DEFAULT_TARGET_LIMIT, 80);
    }

}
