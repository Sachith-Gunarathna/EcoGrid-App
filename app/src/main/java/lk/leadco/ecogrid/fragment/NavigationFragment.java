package lk.leadco.ecogrid.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.model.EVStation;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class NavigationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap myMap;
    private Polyline currentPolyline;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private List<LatLng> currentRoutePoint = new ArrayList<>();
    private List<LatLng> stations = new ArrayList<>();
    private boolean isRouteDrawn = false;
    private Marker userMarker;
    private BottomSheetDialog bottomSheetDialog;
    private LatLng currentUserLocation = null;
    private List<Marker> evMarkersList = new ArrayList<>();
    private TextToSpeech textToSpeech;
    private boolean isTsReady = false;
    private FirebaseFirestore firebaseFirestore;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    
    private View loadingLayout;
    private boolean isFirstLocationUpdate = true;

    public NavigationFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseFirestore = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(requireContext());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted ->{
                    if(isGranted){
                        enableUserLocation();
                    }else{
                        EcoGridToast.showToast(requireActivity(),
                                "Location permission is required to show your position.",
                                EcoGridToast.Type.ERROR);
                    }
                }
        );

        textToSpeech = new TextToSpeech(requireContext(), status -> {
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

        View view = inflater.inflate(R.layout.fragment_navigation,container,false);
        
        loadingLayout = view.findViewById(R.id.layoutLoadingMap);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        myMap.getUiSettings().setCompassEnabled(true);

        if(ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            enableUserLocation();
        }else{
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        loadEvLocations();

        myMap.setOnCameraMoveListener(() -> {
            float currentZoom = myMap.getCameraPosition().zoom;
            boolean shouldShowMarkers = currentZoom >= 14.0f;

            for(Marker marker : evMarkersList){
                if(marker != null){
                    marker.setVisible(shouldShowMarkers);
                }
            }
        });
    }

    private void enableUserLocation(){
        if(ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        myMap.setMyLocationEnabled(false);

        LocationRequest locationRequest =
                new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,5000)
                .setMinUpdateIntervalMillis(2000)
                .setMinUpdateDistanceMeters(5.0f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if(locationResult == null) return;

                for(Location location : locationResult.getLocations()){
                    if(location != null){

                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        currentUserLocation = userLatLng;

                        if(userMarker == null){
                            userMarker = myMap.addMarker(new MarkerOptions()
                                    .position(userLatLng)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.drawable.icons8_navigation_48))
                                    .anchor(0.5f,0.5f));
                        }else{
                            userMarker.setPosition(userLatLng);
                        }

                        if(location.hasBearing()){
                            userMarker.setRotation(location.getBearing());
                        }

                        if (isFirstLocationUpdate) {
                            myMap.animateCamera(CameraUpdateFactory
                                    .newLatLngZoom(userLatLng, 16.0f));
                            isFirstLocationUpdate = false;
                            

                            if (loadingLayout != null) {
                                loadingLayout.setVisibility(View.GONE);
                            }
                        } else {
                             myMap.animateCamera(CameraUpdateFactory.newLatLng(userLatLng));
                        }


                        if(isRouteDrawn && !currentRoutePoint.isEmpty()){
                            if(isOffRoute(userLatLng)){
                                LatLng nearest = findNearestStation(userLatLng);
                                if(nearest != null) {
                                    drawRouteToStation(userLatLng, nearest);
                                }
                            }
                        }
                    }
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void loadEvLocations(){
        if(myMap == null) return;

        BitmapDescriptor evIcon = BitmapDescriptorFactory
                .fromResource(R.drawable.icons8_charging_50);

        firebaseFirestore.collection("Stations")
                .get().addOnSuccessListener(queryDocumentSnapshots ->{

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots){
                        EVStation station = document.toObject(EVStation.class);

                        if(station != null && station.getBasicInfo() != null
                                && station.getTechSpecs() != null){

                            boolean isFastCharging = "Fast Charging"
                                    .equals(station.getTechSpecs().getConnection_type());

                            LatLng stationLocation = new LatLng(
                                    station.getBasicInfo().getLatitude(),
                                    station.getBasicInfo().getLongitude());

                            Marker marker = myMap.addMarker(new MarkerOptions()
                                    .position(stationLocation)
                                    .title(station.getBasicInfo().getName())
                                    .snippet("Available Ports:" + station.getTechSpecs().getPorts()
                                            +" | Fast Charging: " +isFastCharging+ " ")
                                    .icon(evIcon));

                            if(marker != null){
                                marker.setTag(station);
                                evMarkersList.add(marker);
                            }

                            stations.add(stationLocation);
                        }
                    }

                    myMap.setOnMarkerClickListener(clickedMarker ->{
                        EVStation clickedStation = (EVStation) clickedMarker.getTag();
                        if(clickedStation != null){
                            showBottomSheet(clickedStation);
                        }
                        return false;
                    });
                });
    }

    private LatLng findNearestStation(LatLng userLocation){
        LatLng nearest = null;
        float smallestDistance = -1;

        for(LatLng station : stations){
            float[] results = new float[1];
            Location.distanceBetween(
                    userLocation.latitude,
                    userLocation.longitude,
                    station.latitude,
                    station.longitude, results);

            float distanceInMeters = results[0];

            if(smallestDistance == -1 || distanceInMeters < smallestDistance){
                smallestDistance = distanceInMeters;
                nearest = station;
            }
        }
        return nearest;
    }

    private void drawRouteToStation(LatLng origin, LatLng dest){
        String url = "https://maps.googleapis.com/maps/api/directions/json?"+
                "origin="+origin.latitude+","+origin.longitude+
                "&destination="+dest.latitude+","+dest.longitude+
                "&key="+getString(R.string.google_map_polyline_key);

        new Thread(() -> {
            try {
                URL requestUrl = new URL(url);
                Scanner sc = new Scanner(requestUrl.openStream());
                StringBuilder sb = new StringBuilder();

                while (sc.hasNext()) sb.append(sc.next());
                sc.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray routes = json.getJSONArray("routes");

                if (routes.length() == 0) return;

                JSONObject route = routes.getJSONObject(0);
                JSONObject legs = route.getJSONArray("legs").getJSONObject(0);

                String distance = legs.getJSONObject("distance").getString("text");
                String duration = legs.getJSONObject("duration").getString("text");

                String encodedString =
                        route.getJSONObject("overview_polyline").getString("points");
                currentRoutePoint = decodePoly(encodedString);

                requireActivity().runOnUiThread(() -> {
                    if(!isAdded() || getActivity() == null) return;

                    if(currentPolyline != null){
                        currentPolyline.remove();
                    }

                    PolylineOptions options = new PolylineOptions()
                            .addAll(currentRoutePoint)
                            .width(15)
                            .color(Color.parseColor("#007AFF"))
                            .geodesic(true);

                    currentPolyline = myMap.addPolyline(options);
                    isRouteDrawn = true;

                    EcoGridToast.showToast(requireActivity(),
                            "Nearest Station Distance: " + distance + " | Time: " + duration,
                            EcoGridToast.Type.SUCCESS);
                });

            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    private List<LatLng> decodePoly(String encoded){
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len){
            int b, shift = 0, result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; }
            while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0; result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; }
            while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    private boolean isOffRoute(LatLng currentLocation){
        if(currentRoutePoint == null || currentRoutePoint.isEmpty()) return false;

        for(LatLng point : currentRoutePoint){
            float[] result = new float[1];

            Location.distanceBetween(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    point.latitude,
                    point.longitude, result);

            if(result[0] < 50) return false;
        }
        return true;
    }

    private void showBottomSheet(EVStation station) {
        bottomSheetDialog = new BottomSheetDialog(requireContext());

        View view = getLayoutInflater().inflate(R.layout.layout_station_details, null);


        ImageView imageView = view.findViewById(R.id.tvSheetImgStation);
        TextView tvName = view.findViewById(R.id.tvSheetStationName);
        TextView tvConnector = view.findViewById(R.id.tvSheetConnector);
        TextView tvPorts = view.findViewById(R.id.tvPorts);
        TextView tvPrice = view.findViewById(R.id.tvSheetPrice);
        Button btnNavigate = view.findViewById(R.id.btnSheetNavigateNow);


        String stationName = (station.getBasicInfo() != null && station.getBasicInfo().getName() != null) ?
                station.getBasicInfo().getName() : "Unknown Station";

        String ports = (station.getTechSpecs() != null) ?
                String.valueOf(station.getTechSpecs().getPorts()) : "0";

        String connType = (station.getTechSpecs() != null && station.getTechSpecs().getConnection_type() != null) ?
                station.getTechSpecs().getConnection_type() : "N/A";

        String powerOutput = (station.getTechSpecs() != null) ?
                String.valueOf(station.getTechSpecs().getPower_output_kw()) : "0";

        String priceValue = (station.getFinancial() != null) ?
                String.valueOf(station.getFinancial().getPrice_pre_kwh()) : "0.00";


        tvName.setText(stationName);
        tvPrice.setText("Rs. " + priceValue + " / kWh");


        tvConnector.setText("🔌 " + connType + " (" + powerOutput + "kW)");


        tvPorts.setText("⚡ " + ports + " Ports");


        if(station.getVerification() != null && station.getVerification().getPhoto_url() != null) {
            Glide.with(this)
                    .load(station.getVerification().getPhoto_url())
                    .placeholder(R.drawable.placeholder)
                    .into(imageView);
        }

        btnNavigate.setOnClickListener(v -> {
            if (currentUserLocation != null) {
                if (station.getBasicInfo() != null) {
                    LatLng position = new LatLng(
                            station.getBasicInfo().getLatitude(),
                            station.getBasicInfo().getLongitude());

                    speakBatteryAlert("Routing to " + stationName);
                    drawRouteToStation(currentUserLocation, position);
                    bottomSheetDialog.dismiss();
                }
            } else {
                EcoGridToast.showToast(requireActivity(), "Location not available yet!",
                        EcoGridToast.Type.ERROR);
            }
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void speakBatteryAlert(String message){
        if(isTsReady && textToSpeech != null){
            textToSpeech.speak(message,TextToSpeech.LANG_AVAILABLE,null,null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(fusedLocationProviderClient != null && locationCallback != null){
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
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

    private void triggerVibration(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            VibratorManager vibratorManager = (VibratorManager) requireContext()
                    .getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if(vibratorManager != null){
                Vibrator vibrator = vibratorManager.getDefaultVibrator();
                if(vibrator != null && vibrator.hasVibrator()){
                    vibrator.vibrate(VibrationEffect.createOneShot(500,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }
        }else {
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(
                    Context.VIBRATOR_SERVICE);

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(1000,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(1000);
                }
            }
        }
    }
}
