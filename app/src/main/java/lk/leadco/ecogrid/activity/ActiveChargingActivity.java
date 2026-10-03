package lk.leadco.ecogrid.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import lk.leadco.ecogrid.databinding.ActivityActiveChargingBinding;
import lk.leadco.ecogrid.utils.EcoGridToast;
import lk.leadco.ecogrid.utils.EcoGridVirtualOBDManager;
import lk.leadco.ecogrid.utils.SharedPrefsManager;

public class ActiveChargingActivity extends AppCompatActivity {

    private ActivityActiveChargingBinding binding;
    private DatabaseReference hardwareRef;
    private ValueEventListener hardwareListener;
    private String stationId;
    private int targetPrecent;
    private int capacity;
    private EcoGridVirtualOBDManager obdManager;
    private boolean isChargingComplete = false;
    private boolean isChargingStarted = false;
    private int batteryLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivityActiveChargingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        stationId = getIntent().getStringExtra("STATION_ID");
        String targetPercentStr = getIntent().getStringExtra("TARGET_PRECENT");
        String energyRequired = getIntent().getStringExtra("ENERGY_REQUIRED");
        String estimatedTime = getIntent().getStringExtra("ESTIMATED_TIME");

        try {
            if (targetPercentStr != null) {
                targetPrecent = (int) Float.parseFloat(targetPercentStr);
            } else {
                targetPrecent = 100;
            }
        } catch (NumberFormatException e) {
            targetPrecent = 100;
        }

        capacity = getIntent().getIntExtra("CAPACITY",0);

        if(targetPrecent == 0) targetPrecent = 100;

        binding.tvStationId.setText("NODE :: "+ stationId);
        binding.tvTarget.setText("Charging to "+targetPrecent+" limit as requested.");
        binding.tvStatus.setText("CONNECTING....");


        binding.btnDone.setVisibility(View.GONE);

        hardwareRef = FirebaseDatabase.getInstance()
                .getReference("stations/"+stationId+"/current_session");

        obdManager = EcoGridVirtualOBDManager.getInstance();

        obdManager.setListener(((batteryLevel,
                                 voltage,
                                 currentAmp,
                                 temperatureCelsius,
                                 estimatedRangeKm,
                                 totalChargeTime,
                                 monthlyEnergyKwh,
                                 co2SavedKg) -> {

           if(isChargingComplete) return;



           if (isChargingStarted && !isChargingComplete) {
               hardwareRef.child("current_percent").setValue(batteryLevel);
           }

        }));

        startCharging();
        listenToChargingProgress();

        binding.btnStopCharging.setOnClickListener(v ->{ stopCharging(); });

        binding.gaugeNeedle.post(() ->{
            binding.gaugeNeedle.setPivotX(binding.gaugeNeedle.getWidth() / 2f);
            binding.gaugeNeedle.setPivotY(binding.gaugeNeedle.getHeight() + 80f);
        });
    }

    private void startCharging(){

        HashMap<String, Object> command = new HashMap<>();
        command.put("relay","ON");
        int initialBatteryLevel = obdManager.getCurrentBatteryLevel();
        command.put("current_percent", initialBatteryLevel);
        command.put("targetPercentage", String.valueOf(targetPrecent) + ".0");
        command.put("startTime", System.currentTimeMillis());


        String orderId = getIntent().getStringExtra("ORDER_ID");
        String userId = getIntent().getStringExtra("USER_ID");
        
        if(orderId != null) command.put("orderId", orderId);
        if(userId != null) command.put("userId", userId);

        hardwareRef.updateChildren(command).addOnSuccessListener(v ->{
            isChargingStarted = true;
            binding.tvStatus.setText("[ TRANSMITTING POWER ]");
            binding.tvStatus.setTextColor(Color.parseColor("#00FF66"));
            binding.tvStatusIcon.setImageTintList(ColorStateList.valueOf(
                    Color.parseColor("#00FF66")
            ));
            animateChargingText();

            obdManager.setChargingStatus(true);
            Log.d("HARDWARE", "System Online");

        }).addOnFailureListener( v -> {
            binding.tvStatus.setText("CONNECTION FAILED !");
            binding.tvStatus.setTextColor(Color.RED);
            binding.tvStatusIcon.setImageTintList(ColorStateList.valueOf(
                    Color.RED
            ));
            EcoGridToast.showToast(this, "Failed to connect to station!",
                    EcoGridToast.Type.ERROR);
        });
    }

    private void animateChargingText(){
        ObjectAnimator animator =
                ObjectAnimator.ofFloat(binding.tvStatus,"alpha",1f,0.3f,1f);
        animator.setDuration(1500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();

        ObjectAnimator animatorIcon =
                ObjectAnimator.ofFloat(binding.tvStatusIcon,"alpha",1f,0.3f,1f);
        animatorIcon.setDuration(1500);
        animatorIcon.setRepeatCount(ValueAnimator.INFINITE);
        animatorIcon.start();
    }

    private void listenToChargingProgress(){

        hardwareListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()){

                    String relayStatus = snapshot.child("relay").getValue(String.class);

                    Object liveRaw = snapshot.child("livePercentage").getValue();
                    Integer livePercent = null;
                    if (liveRaw instanceof Long) livePercent = ((Long) liveRaw).intValue();
                    else if (liveRaw instanceof Integer) livePercent = (Integer) liveRaw;
                    else if (liveRaw instanceof Double) livePercent = ((Double) liveRaw).intValue();

                    Double energyKwh = snapshot.child("energy_kwh").getValue(Double.class);
                    Object timeRaw = snapshot.child("time_left_mins").getValue();
                    Integer timeLeft = null;
                    if (timeRaw instanceof Long) timeLeft = ((Long) timeRaw).intValue();
                    else if (timeRaw instanceof Integer) timeLeft = (Integer) timeRaw;
                    Integer estimatedRangeKm = snapshot.child("estimated_range_km")
                            .getValue(Integer.class);

                    String chargingStatus = snapshot.child("status").getValue(String.class);


                    if(livePercent != null){
                        batteryLevel = livePercent;
                        

                        binding.tvPercentage.setText(String.valueOf(livePercent));
                        binding.chargingProgressBar.setProgress(livePercent);
                        
                        float angle = (livePercent / 100f) * 360f;
                        binding.gaugeNeedle.animate().rotation(angle).setDuration(800).start();
                    }

                    if(energyKwh != null){
                        binding.tvEnergy.setText(String.format("%.3f kWh", energyKwh));
                    }

                    if(timeLeft != null){
                        binding.tvTimeLeft.setText(timeLeft + " MIN");
                    }

                    if(estimatedRangeKm != null) {
                        binding.tvRange.setText(estimatedRangeKm + "KM");
                    }

                    if(!isChargingStarted || isChargingComplete) return;

                    boolean espCompleted = "COMPLETED".equals(chargingStatus) ||
                            "CANCELED".equals(chargingStatus);
                    boolean relayOff = "OFF".equals(relayStatus);
                    boolean targetReached = livePercent != null && livePercent >= targetPrecent;

                    if(espCompleted || targetReached){
                        Log.d("CHARGING", "Complete: espCompleted=" + espCompleted +
                                " targetReached=" + targetReached);

                        hardwareRef.child("status").setValue("COMPLETED");
                        hardwareRef.child("relay").setValue("OFF");
                        binding.tvStatus.setText("CHARGING COMPLETED");

                        handleChargingComplete();
                    } else if(relayOff){

                        Log.d("CHARGING", "Relay turned OFF externally - stopping");

                        hardwareRef.child("status").setValue("CANCELED");
                        hardwareRef.child("relay").setValue("OFF");
                        binding.tvStatus.setText("CHARGING CANCELLED");

                        handleChargingComplete();

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HARDWARE", "Error: " + error.getMessage());
            }
        };

        hardwareRef.addValueEventListener(hardwareListener);

    }

    private void stopCharging(){
        binding.btnStopCharging.setEnabled(false);
        binding.btnStopCharging.setText("OVERRIDING...");
        isChargingStarted = false;

        hardwareRef.child("relay").setValue("OFF");
        hardwareRef.child("status").setValue("CANCELED");
        binding.tvStatus.setText("CHARGING CANCELLED");

        handleChargingComplete();
    }

    private void handleChargingComplete(){
        if(isChargingComplete) return;
        isChargingComplete = true;

        binding.tvStatus.clearAnimation();
        binding.tvStatus.setAlpha(1f);

        binding.tvStatusIcon.clearAnimation();
        binding.tvStatusIcon.setAlpha(1f);

        binding.tvStatusIcon.setImageTintList(ColorStateList
                .valueOf(Color.parseColor("#2196F3")));
        binding.tvStatus.setTextColor(Color.parseColor("#2196F3"));

        binding.btnStopCharging.setVisibility(View.GONE);
        binding.btnDone.setVisibility(View.VISIBLE);

        binding.tvPercentage.setText(String.valueOf(batteryLevel));

        SharedPrefsManager.saveChargingStatus(this,false);
        SharedPrefsManager.saveCurrentBatteryLevel(this, batteryLevel);

        obdManager.stopSimulation();

        binding.btnDone.setOnClickListener( v ->{

            obdManager.setChargingStatus(false);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(hardwareRef != null && hardwareListener != null){
            hardwareRef.removeEventListener(hardwareListener);
        }
    }
}