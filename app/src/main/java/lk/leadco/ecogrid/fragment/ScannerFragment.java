package lk.leadco.ecogrid.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.activity.ChargeSetupActivity;
import lk.leadco.ecogrid.databinding.FragmentScannerBinding;
import lk.leadco.ecogrid.model.EVStation;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class ScannerFragment extends Fragment {

    private FragmentScannerBinding scannerBinding;
    private CodeScanner mCodeScanner;
    private ImageView flashBtn;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private FirebaseFirestore firebaseFirestore;
    private EcoGridLoadingDialog loadingDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseFirestore = FirebaseFirestore.getInstance();
        loadingDialog = new EcoGridLoadingDialog(requireActivity());

        requestPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if(isGranted){
                                EcoGridToast.showToast(requireContext(),
                                        "Camera Access Granted!",
                                        EcoGridToast.Type.SUCCESS);

                                View view = getView();
                                if(view != null){
                                    CodeScannerView scannerView =
                                            view.findViewById(R.id.cameraPreviewFrame);
                                    startScanning(scannerView, flashBtn);

                                    if(mCodeScanner != null){
                                        mCodeScanner.startPreview();
                                    }
                                }
                            }else {
                                EcoGridToast.showToast(requireContext(),
                                        "Camera permission is required for scanning!",
                                        EcoGridToast.Type.ERROR);
                            }
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        scannerBinding = FragmentScannerBinding.inflate(inflater,container,false);

        CodeScannerView scannerView = scannerBinding.cameraPreviewFrame;
        flashBtn = scannerBinding.btnFlash;
        MaterialCardView cardManualEntry = scannerBinding.cardManualEntry;

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){

            requestPermissionLauncher.launch(Manifest.permission.CAMERA);

        }else{
            startScanning(scannerView, flashBtn);
        }

        cardManualEntry.setOnClickListener(v ->{
            EcoGridToast.showToast(requireContext(), "Switching to Manual Entry...",
                    EcoGridToast.Type.SUCCESS);
        });

        return scannerBinding.getRoot();
    }

    private void startScanning(CodeScannerView scannerView, ImageView flashBtn){

        mCodeScanner = new CodeScanner(requireContext(),scannerView);

        mCodeScanner.setAutoFocusEnabled(true);
        mCodeScanner.setFormats(CodeScanner.ALL_FORMATS);

        flashBtn.setOnClickListener( v ->{
            if(mCodeScanner != null){
                if(mCodeScanner.isPreviewActive()){
                    try {

                        boolean currentFlashState = mCodeScanner.isFlashEnabled();
                        mCodeScanner.setFlashEnabled(!currentFlashState);

                        boolean newFlashState = mCodeScanner.isFlashEnabled();

                        flashBtn.setColorFilter(newFlashState ?
                                Color.parseColor("#4CAF50") :
                                Color.WHITE);

                    }catch (Exception e){
                        EcoGridToast.showToast(requireContext(),
                                "Flash control failed!", EcoGridToast.Type.ERROR);
                    }
                }
            }

        });

        mCodeScanner.setDecodeCallback(result -> requireActivity().runOnUiThread(() -> {
            String scannedId = result.getText();
            validateStationAndProceed(scannedId);
        }));

        scannerView.setOnClickListener( v -> mCodeScanner.startPreview());
    }

    private void validateStationAndProceed(String scannedId) {
        if (scannedId == null || scannedId.isEmpty()) {
            EcoGridToast.showToast(requireContext(), "Invalid QR Code", EcoGridToast.Type.ERROR);
            mCodeScanner.startPreview();
            return;
        }

        loadingDialog.show();

        firebaseFirestore.collection("Stations")
                .document(scannedId)
                .get()
                .addOnCompleteListener(task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            EVStation stationName = document.toObject(EVStation.class);
                            EcoGridToast.showToast(requireContext(), "Station Verified!",
                                    EcoGridToast.Type.SUCCESS);

                            Intent intent = new Intent(requireActivity(), ChargeSetupActivity.class);
                            intent.putExtra("STATION_ID", stationName.getBasicInfo().getName());
                            startActivity(intent);
                        } else {
                            EcoGridToast.showToast(requireContext(),
                                    "Unauthorized Station! Please scan a valid EcoGrid QR.",
                                    EcoGridToast.Type.ERROR);
                            mCodeScanner.startPreview();
                        }
                    } else {
                        EcoGridToast.showToast(requireContext(),
                                "Error verifying station. Please try again.",
                                EcoGridToast.Type.ERROR);
                        mCodeScanner.startPreview();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mCodeScanner != null){
            mCodeScanner.startPreview();
        }
    }

    @Override
    public void onPause() {
        if(mCodeScanner != null){
            mCodeScanner.releaseResources();
            if(flashBtn != null) {
                flashBtn.setColorFilter(Color.WHITE);
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        scannerBinding = null;
    }
}
