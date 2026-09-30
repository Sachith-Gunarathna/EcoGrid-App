package lk.leadco.ecogrid.activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.model.Vehicle;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class AddVehicleActivity extends AppCompatActivity {
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String uid;

    private EcoGridLoadingDialog loadingDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        firebaseFirestore = FirebaseFirestore.getInstance();
        loadingDialog = new EcoGridLoadingDialog(AddVehicleActivity.this);

        FirebaseUser currentUser = firebaseAuth.getInstance().getCurrentUser();

        if(currentUser != null){
            uid = currentUser.getUid();
        }else{
            EcoGridToast.showToast(this, "Please login first",
                    EcoGridToast.Type.ERROR);
            finish();
            return;
        }

        MaterialCardView carCard = findViewById(R.id.cardCar);
        MaterialCardView bikeCard = findViewById(R.id.cardBike);
        MaterialCardView tukTukCard = findViewById(R.id.cardTukTuk);
        MaterialCardView miniScooterCard = findViewById(R.id.cardMiniScooter);

        ImageView backBtn = findViewById(R.id.btnBack);

        backBtn.setOnClickListener( v ->{
            finish();
        });

        carCard.setOnClickListener(v -> {
            selectCard(carCard, bikeCard, tukTukCard, miniScooterCard);
            showBottomSheet("EV CAR");
        });

        bikeCard.setOnClickListener(v -> {
            selectCard(bikeCard, carCard, tukTukCard, miniScooterCard);
            showBottomSheet("EV BIKE");
        });

        tukTukCard.setOnClickListener(v -> {
            selectCard(tukTukCard, carCard, bikeCard, miniScooterCard);
            showBottomSheet("EV TUK-TUK");
        });

        miniScooterCard.setOnClickListener(v -> {
            selectCard(miniScooterCard, carCard, bikeCard, tukTukCard);
            showBottomSheet("EV MINI SCOOTER");
        });
    }

    private void selectCard(MaterialCardView selected,
                            MaterialCardView un1, MaterialCardView un2, MaterialCardView un3){

        selected.setStrokeColor(Color.parseColor("#4CAF50"));
        selected.setCardBackgroundColor(Color.parseColor("#E8F5E9"));

        int defaultBg = Color.parseColor("#F5F7FA");
        un1.setStrokeColor(Color.TRANSPARENT);
        un1.setCardBackgroundColor(defaultBg);

        un2.setStrokeColor(Color.TRANSPARENT);
        un2.setCardBackgroundColor(defaultBg);

        un3.setStrokeColor(Color.TRANSPARENT);
        un3.setCardBackgroundColor(defaultBg);

    }

    private void showBottomSheet(String VehicleType){

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_vehicle, null);

        TextView tvSheetTitle = sheetView.findViewById(R.id.tvSheetTitle);
        tvSheetTitle.setText("Register Your "+VehicleType);

        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveVehicle);

        TextInputEditText evBrand = sheetView.findViewById(R.id.etBrand);
        TextInputEditText evModel = sheetView.findViewById(R.id.etModel);
        TextInputEditText evPlateNo = sheetView.findViewById(R.id.etPlateNo);
        TextInputEditText evCapacity = sheetView.findViewById(R.id.etCapacity);


        btnSave.setOnClickListener( v ->{

            String brand = evBrand.getText().toString().trim();
            String model = evModel.getText().toString().trim();
            String plateNo = evPlateNo.getText().toString().trim();
            String capacityStr = evCapacity.getText() != null ?
                    evCapacity.getText().toString().trim() : "";

            if(brand.isEmpty()){
                evBrand.setError("Brand is required");
                evBrand.requestFocus();
                return;
            }

            if(model.isEmpty()) {
                evModel.setError("Model is required");
                evModel.requestFocus();
                return;
            }

            if(plateNo.isEmpty()){
                evPlateNo.setError("Plate Number is required");
                evPlateNo.requestFocus();
                return;
            }

            int capacity = Integer.parseInt(capacityStr);
            if(capacity <= 0){
                evCapacity.setError("Capacity cannot be zero or negative");
                evCapacity.requestFocus();
                return;
            }

            loadingDialog.show();

            saveVehicle(brand, model, plateNo, capacity, VehicleType);

            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();

    }

    private void saveVehicle(String evBrand, String evModel,
                             String evPlateNo, int evCapacity,String vehicleType){

        DocumentReference vehicleRef = firebaseFirestore.collection("users")
                .document(uid)
                .collection("vehicles").document();

        String vehicleId = vehicleRef.getId();

        Vehicle vehicle = Vehicle.builder()
                .vehicle_id(vehicleId)
                .brand(evBrand)
                .model(evModel)
                .plate_number(evPlateNo)
                .battery_capacity_kwh(evCapacity)
                .user_id(uid)
                .added_date(new Date())
                .vehicle_type(vehicleType)
                .isActive(true)
                .build();

        firebaseFirestore.collection("vehicles").document(vehicleId)
                .set(vehicle)
                .addOnSuccessListener(v -> {
                    loadingDialog.dismiss();
                    showConnectionAnimation();

                    EcoGridToast.showToast(AddVehicleActivity.this,
                            "Vehicle Added Successfully! \uD83D\uDE97",
                            EcoGridToast.Type.SUCCESS);
                })
                .addOnFailureListener( e -> {
                    loadingDialog.dismiss();
                    EcoGridToast.showToast(AddVehicleActivity.this,
                            "Error: "+e.getMessage(),
                            EcoGridToast.Type.ERROR);
                });
    }

    private void showConnectionAnimation(){

        Dialog connectDialog = new Dialog(this);
        connectDialog.setContentView(R.layout.dialog_connecting_module);
        connectDialog.setCancelable(false);

        if(connectDialog.getWindow() != null){
            connectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ProgressBar pbConnecting = connectDialog.findViewById(R.id.pbConnection);
        ImageView imageSuccess = connectDialog.findViewById(R.id.imgSuccess);
        TextView tvStatus = connectDialog.findViewById(R.id.tvStatus);
        TextView tvSubStatus = connectDialog.findViewById(R.id.tvSubStatus);

        connectDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() ->{

            pbConnecting.setVisibility(View.GONE);
            imageSuccess.setVisibility(View.VISIBLE);

            tvStatus.setText("Module Connected! \uD83D\uDE97");
            tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            tvSubStatus.setText("Receiving live OBD diagnostics...");

            new Handler(Looper.getMainLooper()).postDelayed(()->{
                connectDialog.dismiss();

                Intent intent =
                        new Intent(AddVehicleActivity.this , MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            },1500);

        },3000);
    }
}