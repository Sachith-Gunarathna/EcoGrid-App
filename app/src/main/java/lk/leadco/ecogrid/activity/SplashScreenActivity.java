package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import lk.leadco.ecogrid.R;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logo = findViewById(R.id.logoImageView);
        LinearLayout poweredByLayout = findViewById(R.id.poweredByLayout);
        TextView appName3 = findViewById(R.id.appNameTextView3);
        TextView appName4 = findViewById(R.id.appNameTextView4);

        logo.setAlpha(0f);
        logo.setTranslationY(50f);

        poweredByLayout.setAlpha(0f);
        poweredByLayout.setTranslationY(50f);

        appName3.setAlpha(0f);
        appName3.setTranslationY(50f);

        appName4.setAlpha(0f);
        appName4.setTranslationY(50f);


        logo.animate().alpha(1f).translationY(0f).setDuration(1200)
                .start();

        poweredByLayout.animate().alpha(1f).translationY(0f).setDuration(1000)
                .setStartDelay(500).start();

        appName3.animate().alpha(1f).translationY(0f).setDuration(1000)
                .setStartDelay(700).start();

        appName4.animate().alpha(1f).translationY(0f).setDuration(1000)
                .setStartDelay(900).start();


        new Handler().postDelayed(() -> {
            SharedPreferences preferences = getSharedPreferences("EcoGridPrefs",
                    MODE_PRIVATE);
            boolean isFirstTime = preferences.getBoolean("isFirstTime", true);

            Intent intent;
            if (isFirstTime) {
                intent = new Intent(SplashScreenActivity.this,
                        OnboardingActivity.class);
            } else {
                intent = new Intent(SplashScreenActivity.this,
                        SignInActivity.class);
            }
            startActivity(intent);
            finish();
        }, 3000);
    }
}