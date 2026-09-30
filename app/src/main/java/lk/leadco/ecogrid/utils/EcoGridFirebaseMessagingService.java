package lk.leadco.ecogrid.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class EcoGridFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "EcoGrid_FCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);

        // Save updated token to Firestore
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(v -> Log.d(TAG, "Token saved"))
                    .addOnFailureListener(e -> Log.e(TAG, "Token save failed: " + e.getMessage()));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        boolean isPaused = SharedPrefsManager.getPauseNotification(getApplicationContext());
        if (isPaused) return;

        Map<String, String> data = message.getData();
        String type = data.get("type");

        Log.d(TAG, "Message received, type: " + type);

        if ("admin_broadcast".equals(type)) {
            // General notification from admin panel
            String title = data.get("title");
            String body  = data.get("body");
            if (title == null && message.getNotification() != null)
                title = message.getNotification().getTitle();
            if (body == null && message.getNotification() != null)
                body = message.getNotification().getBody();

            if (title != null && body != null) {
                EcoGridNotificationHelper.showGeneralNotification(
                        getApplicationContext(), title, body);
            }

        } else if (data.containsKey("stationId") || data.containsKey("percentage")) {
            // Charging complete notification
            String stationId  = data.getOrDefault("stationId", "EcoGrid Station");
            String percentage = data.getOrDefault("percentage", "100%");
            EcoGridNotificationHelper.showChargingCompleteNotification(
                    getApplicationContext(), stationId, percentage);

        } else {
            // Fallback: use notification payload if present
            if (message.getNotification() != null) {
                String title = message.getNotification().getTitle();
                String body  = message.getNotification().getBody();
                if (title != null && body != null) {
                    EcoGridNotificationHelper.showGeneralNotification(
                            getApplicationContext(), title, body);
                }
            }
        }
    }
}
