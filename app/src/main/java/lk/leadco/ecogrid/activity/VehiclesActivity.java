package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.leadco.ecogrid.Adapter.VehicleAdapter;
import lk.leadco.ecogrid.databinding.ActivityVehiclesBinding;
import lk.leadco.ecogrid.model.Vehicle;
import lk.leadco.ecogrid.utils.EcoGridDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class VehiclesActivity extends AppCompatActivity {

    private ActivityVehiclesBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;

    private List<Vehicle> myVehicles = new ArrayList<>();
    private VehicleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVehiclesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        setupRecyclerView();
        loadVehicles();

        binding.btnBack.setOnClickListener( v ->{
            finish();
        });

        binding.btnAddVehicle.setOnClickListener( v ->{
            Intent intent = new Intent(VehiclesActivity.this, AddVehicleActivity.class);
            startActivity(intent);
            finish();
        });

    }

    private void setupRecyclerView() {
        adapter = new VehicleAdapter(myVehicles, new VehicleAdapter.OnVehicleClickListener() {
            @Override
            public void onVehicleClick(Vehicle vehicle) {

            }

            @Override
            public void onDeleteClick(Vehicle vehicle) {
                deleteVehicle(vehicle);
            }
        });
        binding.rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVehicles.setAdapter(adapter);
    }

    private void loadVehicles(){
        if(firebaseUser != null){
            firebaseFirestore.collection("vehicles")
                    .whereEqualTo("user_id",firebaseUser.getUid())
                    .whereEqualTo("active",true)
                    .get()
                    .addOnSuccessListener( queryDocumentSnapshots -> {
                        myVehicles.clear();
                        for(QueryDocumentSnapshot document : queryDocumentSnapshots){
                            Vehicle v = document.toObject(Vehicle.class);
                            if(v != null){
                                myVehicles.add(v);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if(myVehicles.isEmpty()){
                            binding.tvNoVehicles.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvNoVehicles.setVisibility(View.GONE);
                        }

                    }) .addOnFailureListener(e -> {
                        EcoGridToast.showToast(this,
                                "Error loading vehicles: " + e.getMessage(),
                                EcoGridToast.Type.ERROR);
                    });
        }
    }

    private void deleteVehicle(Vehicle vehicle){
        EcoGridDialog.showConfirmDialog( VehiclesActivity.this,
                "Remove Vehicle",
                "Are you sure you want to remove this vehicle from your profile? " +
                        "Your past charging history will still be saved.",
                "Yes, Remove",
                "Cancel",
                () -> {
                    softDelete(vehicle);
                }
                );
    }

    private void softDelete(Vehicle vehicle){
        if (vehicle == null) return;

        firebaseFirestore.collection("vehicles").document(vehicle.getVehicle_id())
                .update("active",false)
                .addOnSuccessListener( v ->{
                    EcoGridToast.showToast(VehiclesActivity.this,
                            "Vehicle removed successfully!",
                            EcoGridToast.Type.SUCCESS);
                    loadVehicles();
                })
                .addOnFailureListener(v ->{
                    EcoGridToast.showToast(VehiclesActivity.this,
                            "Failed to remove vehicle!",
                            EcoGridToast.Type.ERROR);
                });
    }
}