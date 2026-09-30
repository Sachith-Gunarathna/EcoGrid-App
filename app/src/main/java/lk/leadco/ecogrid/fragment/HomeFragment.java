package lk.leadco.ecogrid.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.activity.AddVehicleActivity;
import lk.leadco.ecogrid.model.Vehicle;
import lk.leadco.ecogrid.utils.EcoGridToast;
import lk.leadco.ecogrid.utils.EcoGridVirtualOBDManager;
import lk.leadco.ecogrid.utils.SharedPrefsManager;

public class HomeFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private TextView tvSelectedVehicleName, tvSelectedVehiclePlate, tvVehicleNameBottom,
            tvKm, tvObdVoltage, tvObdCurrent, tvObdTemp,
            tvTotalTime, tvTotalEnergy, tvCo2Saved, tvBatteryPercent;
    private ImageView imgSelectedVehicle, imgVehicle;

    private List<Vehicle> myVehicles = new ArrayList<>();
    private Vehicle currentSelectedVehicle = null;
    private MaterialCardView cardVehicleSelector;
    private FirebaseUser firebaseUser;
    private ProgressBar pbBatteryVisual;

    private LineChart liveHealthChart;
    private BarChart historyBarChart;
    private TextToSpeech textToSpeech;
    private boolean isTsReady = false;
    private boolean isBatteryAlertSpoken = false;

    private ArrayList<Entry> voltageEntries = new ArrayList<>();
    private int timeIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        textToSpeech = new TextToSpeech(requireContext(),status ->{
            if(status == TextToSpeech.SUCCESS){
                int result = textToSpeech.setLanguage(Locale.US);
                isTsReady = !(result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED);

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ImageView addVehicle = view.findViewById(R.id.addVehicleBtn);
        tvSelectedVehicleName = view.findViewById(R.id.tvSelectedVehicleName);
        tvSelectedVehiclePlate = view.findViewById(R.id.tvSelectedVehiclePlate);
        tvVehicleNameBottom = view.findViewById(R.id.vehicleName);
        cardVehicleSelector = view.findViewById(R.id.cardVehicleSelector);
        imgSelectedVehicle = view.findViewById(R.id.imgSelectedVehicle);
        imgVehicle = view.findViewById(R.id.imgVehicle);

        pbBatteryVisual = view.findViewById(R.id.pbBatteryVisual);
        tvBatteryPercent = view.findViewById(R.id.tvBatteryPercent);
        tvKm = view.findViewById(R.id.tvKm);
        tvObdVoltage = view.findViewById(R.id.tvObdVoltage);
        tvObdCurrent = view.findViewById(R.id.tvObdCurrent);
        tvObdTemp = view.findViewById(R.id.tvObdTemp);

        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        tvTotalEnergy = view.findViewById(R.id.tvTotalEnergy);
        tvCo2Saved = view.findViewById(R.id.tvCo2Saved);

        liveHealthChart = view.findViewById(R.id.liveHealthChart);
        historyBarChart = view.findViewById(R.id.historyBarChart);

        setupHealthChart();
        setupHistoryChart();
        setupOBDSimulationListener();
        fetchMyVehicles();

        addVehicle.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddVehicleActivity.class);
            startActivity(intent);
        });

        cardVehicleSelector.setOnClickListener(v -> {
            if (myVehicles.isEmpty()) {
                EcoGridToast.showToast(requireActivity(),
                        "No vehicles found. Please add one.", EcoGridToast.Type.ERROR);
            } else {
                showVehicleSelectorSheet();
            }
        });

        return view;
    }

    private void fetchMyVehicles() {
        if (firebaseUser.getUid() == null) return;

        firebaseFirestore.collection("vehicles")
                .whereEqualTo("user_id", firebaseUser.getUid())
                .whereEqualTo("active",true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myVehicles.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Vehicle v = document.toObject(Vehicle.class);
                        if (v != null) {
                            myVehicles.add(v);
                        }
                    }

                    if (!myVehicles.isEmpty()) {
                        updateDashboardWithVehicle(myVehicles.get(0));
                    } else {
                        tvSelectedVehicleName.setText("No Vehicles Found");
                        tvSelectedVehiclePlate.setText("Click '+' to add");
                        tvVehicleNameBottom.setText("No Vehicle Selected");
                    }
                })
                .addOnFailureListener(e -> {
                    EcoGridToast.showToast(requireActivity(),
                            "Error loading vehicles: " + e.getMessage(),
                            EcoGridToast.Type.ERROR);
                });
    }

    private void updateDashboardWithVehicle(Vehicle vehicle) {
        currentSelectedVehicle = vehicle;
        String fullName = vehicle.getBrand() + " " + vehicle.getModel();

        tvSelectedVehicleName.setText(fullName);
        tvSelectedVehiclePlate.setText(vehicle.getPlate_number());
        tvVehicleNameBottom.setText(fullName);

        if ("EV CAR".equals(vehicle.getVehicle_type())) {
            imgSelectedVehicle.setImageResource(R.drawable.car);
            imgVehicle.setImageResource(R.drawable.car);
        } else if ("EV BIKE".equals(vehicle.getVehicle_type())) {
            imgSelectedVehicle.setImageResource(R.drawable.scooter);
            imgVehicle.setImageResource(R.drawable.scooter);
        } else if ("EV TUK-TUK".equals(vehicle.getVehicle_type())) {
            imgSelectedVehicle.setImageResource(R.drawable.tuktuk);
            imgVehicle.setImageResource(R.drawable.tuktuk);
        } else if ("EV MINI SCOOTER".equals(vehicle.getVehicle_type())) {
            imgSelectedVehicle.setImageResource(R.drawable.mini_scooter);
            imgVehicle.setImageResource(R.drawable.mini_scooter);
        }

        SharedPrefsManager.saveActiveVehicle(requireContext(), vehicle.getVehicle_id());

        EcoGridVirtualOBDManager obdManager = EcoGridVirtualOBDManager.getInstance();
        obdManager.connectToVehicle(requireContext(), vehicle);
        obdManager.startSimulation(requireContext());
    }

    private void showVehicleSelectorSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_vehicle_list, null);
        LinearLayout container = sheetView.findViewById(R.id.vehicleListContainer);

        for (Vehicle v : myVehicles) {
            View vehicleView = getLayoutInflater().inflate(R.layout.item_vehicle_selector, null);

            TextView tvName = vehicleView.findViewById(R.id.tvItemName);
            TextView tvPlate = vehicleView.findViewById(R.id.tvItemPlate);
            tvName.setText(v.getBrand() + " " + v.getModel());
            tvPlate.setText(v.getPlate_number());

            ImageView imgSheetVehicle = vehicleView.findViewById(R.id.imgItemVehicle);

            if ("EV CAR".equals(v.getVehicle_type())) {
                imgSheetVehicle.setImageResource(R.drawable.car);
            } else if ("EV BIKE".equals(v.getVehicle_type())) {
                imgSheetVehicle.setImageResource(R.drawable.scooter);
            } else if ("EV TUK-TUK".equals(v.getVehicle_type())) {
                imgSheetVehicle.setImageResource(R.drawable.tuktuk);
            } else if ("EV MINI SCOOTER".equals(v.getVehicle_type())) {
                imgSheetVehicle.setImageResource(R.drawable.mini_scooter);
            }

            vehicleView.setOnClickListener(click -> {
                updateDashboardWithVehicle(v);
                dialog.dismiss();
            });

            container.addView(vehicleView);
        }

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void setupOBDSimulationListener() {



        EcoGridVirtualOBDManager.getInstance().setListener((
                batteryLevel, voltage, currentAmp, temperatureCelsius,
                estimatedRangeKm, totalChargeTime, monthlyEnergyKwh, co2SavedKg) -> {

            if (!isAdded() || getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (batteryLevel > 50) {

                    pbBatteryVisual.setProgressTintList(ColorStateList
                            .valueOf(Color.parseColor("#4CAF50")));
                    isBatteryAlertSpoken = false;
                } else if (batteryLevel > 20) {

                    pbBatteryVisual.setProgressTintList(ColorStateList
                            .valueOf(Color.parseColor("#FF9800")));
                    isBatteryAlertSpoken = true;
                } else {

                    pbBatteryVisual.setProgressTintList(ColorStateList
                            .valueOf(Color.parseColor("#F44336")));

                    if (!isBatteryAlertSpoken) {
                        speakBatteryAlert("Warning: Battery critical. " +
                                "Please route to the nearest EcoGrid station.");
                        isBatteryAlertSpoken = true;
                    }
                }

                pbBatteryVisual.setProgress(batteryLevel);
                tvBatteryPercent.setText(batteryLevel + " %");
                tvKm.setText(estimatedRangeKm + " km");
                tvObdVoltage.setText(String.format(Locale.getDefault(), "%.1f V", voltage));
                tvObdCurrent.setText(String.format(Locale.getDefault(), "%.1f A", currentAmp));
                tvObdTemp.setText(temperatureCelsius + " °C");

                if (tvTotalTime != null) tvTotalTime.setText(totalChargeTime);
                if (tvTotalEnergy != null) tvTotalEnergy.setText(monthlyEnergyKwh + " kWh");
                if (tvCo2Saved != null) tvCo2Saved.setText(co2SavedKg + " kg");

                voltageEntries.add(new Entry(timeIndex++, (float) voltage));

                if (voltageEntries.size() > 20) {
                    voltageEntries.remove(0);
                }

                LineDataSet lineDataSet = new LineDataSet(voltageEntries, "Live Voltage (V)");
                lineDataSet.setColor(Color.parseColor("#4CAF50"));
                lineDataSet.setLineWidth(2f);
                lineDataSet.setDrawCircles(false);
                lineDataSet.setDrawValues(false);
                lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                LineData lineData = new LineData(lineDataSet);
                liveHealthChart.setData(lineData);
                liveHealthChart.notifyDataSetChanged();
                liveHealthChart.invalidate();
            });
        });
    }

    private void setupHistoryChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1f, 12f));
        entries.add(new BarEntry(2f, 18f));
        entries.add(new BarEntry(3f, 5f));
        entries.add(new BarEntry(4f, 22f));
        entries.add(new BarEntry(5f, 14f));
        entries.add(new BarEntry(6f, 30f));
        entries.add(new BarEntry(7f, 8f));

        BarDataSet dataSet = new BarDataSet(entries, "Energy Usage (kWh)");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        historyBarChart.setData(barData);

        historyBarChart.getDescription().setEnabled(false);
        historyBarChart.getAxisRight().setEnabled(false);
        historyBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        historyBarChart.getXAxis().setDrawGridLines(false);
        historyBarChart.animateY(1000);
    }

    private void setupHealthChart() {
        liveHealthChart.getDescription().setEnabled(false);
        liveHealthChart.getAxisRight().setEnabled(false);

        liveHealthChart.getAxisLeft().setTextColor(Color.WHITE);
        liveHealthChart.getXAxis().setTextColor(Color.WHITE);
        liveHealthChart.getLegend().setTextColor(Color.WHITE);

        liveHealthChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        liveHealthChart.getXAxis().setDrawGridLines(false);
    }

    private void speakBatteryAlert(String message){
        if(isTsReady && textToSpeech != null){
            textToSpeech.speak(message,TextToSpeech.QUEUE_FLUSH,null,null);
        }
    }

    @Override
    public void onDestroy() {
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}