package lk.leadco.ecogrid.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.activity.ActiveChargingActivity;
import lk.leadco.ecogrid.activity.MainActivity;

public class EcoGridNotificationHelper {

    private static final String CHANNEL_CHARGING = "charging_alerts";
    private static final String CHANNEL_GENERAL  = "ecogrid_general";

    private static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);


        NotificationChannel chargingCh = new NotificationChannel(
                CHANNEL_CHARGING, "Charging Alerts", NotificationManager.IMPORTANCE_HIGH);
        chargingCh.setDescription("EV charging status notifications");
        chargingCh.enableLights(true);
        chargingCh.setLightColor(Color.GREEN);
        chargingCh.enableVibration(true);
        nm.createNotificationChannel(chargingCh);


        NotificationChannel generalCh = new NotificationChannel(
                CHANNEL_GENERAL, "EcoGrid Updates", NotificationManager.IMPORTANCE_DEFAULT);
        generalCh.setDescription("General EcoGrid announcements and updates");
        generalCh.enableLights(true);
        generalCh.setLightColor(Color.parseColor("#00C853"));
        nm.createNotificationChannel(generalCh);
    }

    public static void showChargingCompleteNotification(Context context,
                                                        String stationName, String percentage) {
        ensureChannels(context);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, ActiveChargingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CHARGING)
                .setSmallIcon(R.drawable.icons8_notification_24)
                .setContentTitle("⚡ Charging Complete")
                .setContentText("Your vehicle at " + stationName + " is now " + percentage + " charged.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#4CAF50"))
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    public static void showGeneralNotification(Context context, String title, String body) {
        ensureChannels(context);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 1, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.icons8_notification_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#00C853"))
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
