package lk.leadco.ecogrid.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivityChargeSetupBinding;
import lk.leadco.ecogrid.model.EVStation;
import lk.leadco.ecogrid.utils.EcoGridToast;
import lk.leadco.ecogrid.utils.SharedPrefsManager;

public class ChargeSetupActivity extends AppCompatActivity {

    private ActivityChargeSetupBinding chargeSetupBinding;
    private TextView tvStationId ,tvTargetPercent, tvCurrentPercent,
            tvEstimatedCost, tvEnergyRequired, tvEstimatedTime;

    private MaterialButton btnProceedPayment;
    private LinearProgressIndicator currentBatteryProgress;
    private Slider batterySlider;

    private ImageView backBtn;

    private int CURRENT_BATTERY_CAPACITY ;
    private final double TOTAL_BATTERY_CAPACITY_KWH = 40.0;
    private double PRICE_PRE_KWH = 0;
    private final double CHARGER_POWER_KW = 7.4;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        chargeSetupBinding = ActivityChargeSetupBinding.inflate(getLayoutInflater());
        setContentView(chargeSetupBinding.getRoot());

        tvStationId = chargeSetupBinding.tvStationId;
        tvTargetPercent = chargeSetupBinding.tvTargetPercent;
        tvCurrentPercent = chargeSetupBinding.tvCurrentPercent;
        tvEstimatedCost = chargeSetupBinding.tvEstimatedCost;
        tvEnergyRequired = chargeSetupBinding.tvEnergyRequired;
        tvEstimatedTime = chargeSetupBinding.tvEstimatedTime;

        currentBatteryProgress = chargeSetupBinding.currentBatteryProgress;
        batterySlider = chargeSetupBinding.batterySlider;

        btnProceedPayment = chargeSetupBinding.btnProceedPayment;
        backBtn = chargeSetupBinding.btnBack;

        this.CURRENT_BATTERY_CAPACITY = SharedPrefsManager.getCurrentBatteryLevel(this);


        String station_id = getIntent().getStringExtra("STATION_ID");
        if(station_id != null){
            tvStationId.setText(station_id);
        }

        FirebaseFirestore.getInstance().collection("Stations")
                .whereEqualTo("basicInfo.name", station_id)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            EVStation evStation = document.toObject(EVStation.class);
                            if (evStation != null && evStation.getFinancial() != null) {
                                PRICE_PRE_KWH = evStation.getFinancial().getPrice_pre_kwh();
                                calculateEstimatedCost(batterySlider.getValue());
                            }
                        }
                    }
                });

        setupInitialUI();
        setupListeners();

        backBtn.setOnClickListener( v -> finish());

        btnProceedPayment.setOnClickListener( v ->{
            EcoGridToast.showToast(this,"Proceeding to Pay: "
                            +tvEstimatedCost.getText()+" for "
                            +tvEnergyRequired.getText()+" kWh",
                    EcoGridToast.Type.SUCCESS);

            Intent intent = new Intent(this,PaymentConfirmationActivity.class);

            intent.putExtra("STATION_ID",station_id);
            intent.putExtra("TARGET_PRECENT",tvTargetPercent.getText().toString());
            intent.putExtra("ESTIMATE_COST",tvEstimatedCost.getText().toString());
            intent.putExtra("ENERGY_REQUIRED",tvEnergyRequired.getText().toString());
            intent.putExtra("ESTIMATED_TIME",tvEstimatedTime.getText().toString());

            startActivity(intent);
            finish();
        });
    }

    private void setupInitialUI(){

        tvCurrentPercent.setText("Current: "+CURRENT_BATTERY_CAPACITY+ "%");
        currentBatteryProgress.setProgress((int) CURRENT_BATTERY_CAPACITY);

        DatabaseReference stationRef = FirebaseDatabase.getInstance()
                        .getReference("stations/"+tvStationId.getText()+"/current_session");
        stationRef.child("livePercentage").setValue(CURRENT_BATTERY_CAPACITY);

        batterySlider.setValue(80.0f);
        calculateEstimatedCost(80.0f);
    }

    private void setupListeners(){
        batterySlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if(value < CURRENT_BATTERY_CAPACITY){
                    slider.setValue((float) CURRENT_BATTERY_CAPACITY);
                    value = (float) CURRENT_BATTERY_CAPACITY;
                }

                calculateEstimatedCost(value);
            }
        });
    }

    private void calculateEstimatedCost(float targetPercent){

        tvTargetPercent.setText(String.valueOf(targetPercent));

        double requiredPrecent = targetPercent - CURRENT_BATTERY_CAPACITY;

        if(requiredPrecent <= 0){
            tvEnergyRequired.setText("0.0 kWh");
            tvEstimatedCost.setText("Rs. 0.00");
            tvEstimatedTime.setText("0 mins");
            return;
        }

        double energyRequired = (requiredPrecent / 100.0) * TOTAL_BATTERY_CAPACITY_KWH;
        double estimatedCost = energyRequired * PRICE_PRE_KWH;
        double estimatedTime = (energyRequired / CHARGER_POWER_KW) * 60.0;

        tvEnergyRequired.setText(String.format("%.1f kWh", energyRequired));
        tvEstimatedCost.setText(String.format("%.2f", estimatedCost));
        tvEstimatedTime.setText(String.format("%d mins", (int) estimatedTime));


    }
}