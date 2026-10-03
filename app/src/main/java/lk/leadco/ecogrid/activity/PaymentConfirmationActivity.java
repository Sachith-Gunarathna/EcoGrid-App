package lk.leadco.ecogrid.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivityPaymentConfirmationBinding;
import lk.leadco.ecogrid.model.Invoice;
import lk.leadco.ecogrid.model.Payment;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.model.Vehicle;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;
import lk.leadco.ecogrid.utils.SharedPrefsManager;
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;

import lk.payhere.androidsdk.model.StatusResponse;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentConfirmationActivity extends AppCompatActivity {
    private FirebaseFirestore firebaseFirestore;
    private ActivityPaymentConfirmationBinding binding;
    private TextView tvConfirmStation, tvConfirmTarget,tvConfirmCost,
            tvWalletBalance,tvWalletStatus,currencyType, energyRequired;
    private ImageView btnBack, walletIcon;
    private MaterialButton btnAuthorize;
    private double cost = 0.0;
    private User fUser;
    private FirebaseUser firebaseUser;

    private final int PAYHERE_REQUEST_CODE = 11001;

    private EcoGridLoadingDialog loading;
    private String vehicle_id;
    private String vehicleName;
    private int vehicleCapacity;
    private String savedCustomerToken = null;
    private String estimated_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
       firebaseUser = firebaseAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();

       firebaseFirestore.collection("users").document(firebaseUser.getUid()).get()
               .addOnSuccessListener(ds ->{

                   if(ds.exists()){
                       fUser = ds.toObject(User.class);
                   }

               });

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivityPaymentConfirmationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tvConfirmStation = binding.tvConfirmStation;
        tvConfirmTarget = binding.tvConfirmTarget;
        tvConfirmCost = binding.tvConfirmCost;
        tvWalletBalance = binding.tvWalletBalance;
        tvWalletStatus = binding.tvWalletStatus;
        btnBack = binding.btnBack;
        currencyType = binding.currencyType;
        energyRequired = binding.tvEnergy;

        btnAuthorize = binding.btnAuthorize;

        walletIcon = binding.walletIcon;

        loading = new EcoGridLoadingDialog(this);

        String station_id = getIntent().getStringExtra("STATION_ID");
        String target_precent = getIntent().getStringExtra("TARGET_PRECENT");
        String estimate_cost = getIntent().getStringExtra("ESTIMATE_COST");
        String energy_required = getIntent().getStringExtra("ENERGY_REQUIRED");
        estimated_time = getIntent().getStringExtra("ESTIMATED_TIME");

        vehicle_id = SharedPrefsManager.getActiveVehicle(this);

        firebaseFirestore.collection("vehicles").document(vehicle_id)
                .get()
                .addOnSuccessListener(ds -> {
                    if(ds.exists()){
                        Vehicle v = ds.toObject(Vehicle.class);
                        vehicleName = v.getBrand()+" "+v.getModel();
                        vehicleCapacity = v.getBattery_capacity_kwh();
                    }
                });

        if(station_id != null){
            tvConfirmStation.setText(station_id);
        }

        if(target_precent != null){
            tvConfirmTarget.setText(target_precent);
        }

        if(estimate_cost != null){
            tvConfirmCost.setText(estimate_cost);
        }

        if(energy_required != null){
            energyRequired.setText(energy_required);
        }

        tvWalletBalance.setText("00.00");
        tvWalletStatus.setText("Sufficient");

        finalizePayment();

        btnBack.setOnClickListener(v ->{
            finish();
        });

        btnAuthorize.setOnClickListener( v ->{
            paymentProcess();
        });

        firebaseFirestore.collection("users")
                .document(firebaseUser.getUid())
                .collection("paymentCards")
                .whereEqualTo("defaultCard",true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    if(!queryDocumentSnapshots.isEmpty()){
                        savedCustomerToken = queryDocumentSnapshots.getDocuments().get(0)
                                .getString("customerToken");
                    }
                });

    }

    private void finalizePayment(){
        try {
            double walletBalance = Double.parseDouble(tvWalletBalance.getText().toString());
            cost = Double.parseDouble(tvConfirmCost.getText().toString());

            if(walletBalance >= cost){
                currencyType.setTextColor(Color.parseColor("#FFFFFF"));
                tvWalletBalance.setText(String.valueOf(walletBalance - cost));
                walletIcon.setImageTintList(ColorStateList.valueOf(
                        Color.parseColor("#4CAF50")));

                walletIcon.setBackgroundTintList(ColorStateList.valueOf(
                        Color.parseColor("#334CAF50")));

            }else {
                currencyType.setTextColor(Color.parseColor("#ff0000"));
                tvWalletBalance.setTextColor(Color.parseColor("#ff0000"));
                tvWalletStatus.setBackgroundTintList(getColorStateList(
                        R.color.md_theme_error_mediumContrast));

                walletIcon.setImageTintList(
                        ColorStateList.valueOf(Color.parseColor("#ff0000")));

                walletIcon.setBackgroundTintList(
                        getColorStateList(R.color.md_theme_error_mediumContrast));

                tvWalletStatus.setTextColor(Color.parseColor("#ff0000"));
                tvWalletStatus.setText("Insufficient");

            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void paymentProcess(){

        btnAuthorize.setEnabled(false);
        btnAuthorize.setText("Processing...");

        if(savedCustomerToken != null){

            loading.show();

            String orderId = "EcoGrid-"+System.currentTimeMillis();

            Payment payments = new Payment();
            payments.setUid(firebaseUser.getUid());
            payments.setAmount(cost);
            payments.setCustomerToken(savedCustomerToken);
            payments.setStatus("PENDING");
            payments.setOrderId(orderId);
            payments.setStationId(tvConfirmStation.getText().toString());
            payments.setTargetPercentage(tvConfirmTarget.getText().toString());
            payments.setTimestamp(FieldValue.serverTimestamp());

           DocumentReference paymentRef =  firebaseFirestore.collection("payments")
                    .document();
            paymentRef.set(payments)
                    .addOnSuccessListener( v ->{

                        paymentRef.addSnapshotListener((snapshot,
                                                        e) ->{

                            if(snapshot != null && snapshot.exists()){

                                String status = snapshot.getString("status");

                                if(status.equals("SUCCESS")){

                                    String payhereTransactionId = snapshot.getString("transactionId");
                                    EcoGridToast.showToast(this,
                                            "Payment Successful!", EcoGridToast.Type.SUCCESS);

                                    SimpleDateFormat sdf = new
                                            SimpleDateFormat("MMM dd, yyyy, hh:mm a",
                                            Locale.ENGLISH);
                                    String formattedDate = sdf.format(System.currentTimeMillis());

                                    sendInvoice(payhereTransactionId != null ? payhereTransactionId
                                            : orderId, String.valueOf(cost), formattedDate);
                                    saveInvoice(payhereTransactionId != null ? payhereTransactionId
                                            : orderId, String.valueOf(cost), formattedDate);
                                }else if(status.equals("FAILED")){

                                    String reason = snapshot.getString("reason");

                                    loading.dismiss();
                                    btnAuthorize.setEnabled(true);
                                    btnAuthorize.setText("TRY AGAIN");

                                    EcoGridToast.showToast(this, "Payment Failed: "
                                            + (reason != null ? reason : "Card Declined"),
                                            EcoGridToast.Type.ERROR);

                                }

                            }

                        });

                    }).addOnFailureListener(e ->{
                        loading.dismiss();
                        btnAuthorize.setEnabled(true);
                        btnAuthorize.setText("PAY NOW");
                        EcoGridToast.showToast(this,
                                "Failed to start payment process", EcoGridToast.Type.ERROR);
                    });

        }else{

            InitRequest req = new InitRequest();

            req.setMerchantId("1220989");
            req.setCurrency("LKR");
            req.setAmount(cost);
            req.setOrderId("ECOGRID-"+System.currentTimeMillis());
            req.setItemsDescription("EV Charging at: "+ tvConfirmStation.getText());

            req.getCustomer().setFirstName(fUser.getName() != null ?
                    fUser.getName() : "User");

            req.getCustomer().setLastName("");
            req.getCustomer().setEmail(fUser.getEmail() != null ? fUser.getEmail() : "");
            req.getCustomer().setPhone(fUser.getPhoneNumber() != null ?
                    fUser.getPhoneNumber(): "");

            req.getCustomer().getAddress().setAddress("");
            req.getCustomer().getAddress().setCity("");
            req.getCustomer().getAddress().setCountry("Sri Lanka");

            Intent intent = new Intent(this, PHMainActivity.class);
            intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
            PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
            startActivityForResult(intent, PAYHERE_REQUEST_CODE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYHERE_REQUEST_CODE && data != null &&
                data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {

            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>)
                    data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

            if (resultCode == Activity.RESULT_OK) {

                if (response != null){
                    if (response.isSuccess()) {

                        StatusResponse status = response.getData();
                        String payhereTransactionId = String.valueOf(status.getPaymentNo());
                        String price = String.valueOf(status.getPrice());

                        EcoGridToast.showToast(this,
                                "Payment Successful!", EcoGridToast.Type.SUCCESS);

                        loading.show();

                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm a",
                                Locale.ENGLISH);
                        String formattedDate = sdf.format(System.currentTimeMillis());

                        sendInvoice(payhereTransactionId, price, formattedDate);
                        saveInvoice(payhereTransactionId, price, formattedDate);

                    }else{
                        EcoGridToast.showToast(this,
                                "Payment Failed! Rs." + cost, EcoGridToast.Type.ERROR);
                    }
                }else{
                    EcoGridToast.showToast(this,
                            "Payment Failed! Rs." + cost, EcoGridToast.Type.ERROR);
                    finish();
                }
            }else if(resultCode == Activity.RESULT_CANCELED){
                if(response != null){
                    EcoGridToast.showToast(this,
                            response.getData().getMessage(), EcoGridToast.Type.ERROR);
                    finish();
                }else{
                    EcoGridToast.showToast(this,
                            "Payment Failed or Canceled!", EcoGridToast.Type.ERROR);
                    finish();
                }
            }
        }
    }

    private void sendInvoice(String payhereTransactionId, String price, String formattedDate){

        OkHttpClient client = new OkHttpClient();

        String url = "https://www.nexcentauri.com/api/send-invoice";

        try {

            JSONObject object = new JSONObject();
            String stationStr = tvConfirmStation.getText().toString().trim();
            
            String userEmail = "";
            if (fUser.getEmail() != null ) {
                userEmail = fUser.getEmail();
            }

            object.put("transactionId", payhereTransactionId);
            object.put("amount", price);
            object.put("date", formattedDate);
            object.put("stationId", stationStr);
            object.put("email", userEmail);
            object.put("vehicle", vehicleName);

            RequestBody requestBody = RequestBody.create(object.toString(), MediaType.parse(
                    "application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    Log.e("EMAIL_API", "Failed to connect to backend: " + e.getMessage());
                    closeActivitySafe();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("EMAIL_API", "Success! Receipt sent via " +
                                "Nexcentauri Backend.");
                        finish();
                    } else {
                        Log.e("EMAIL_API", "Server Error: " + response.code());
                        finish();
                    }
                    response.close();
                    closeActivitySafe();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void saveInvoice(String payhereTransactionId, String price, String formattedDate){

        DocumentReference invoiceRef = firebaseFirestore.collection("users")
                .document(fUser.getUid())
                .collection("invoices").document();

        String invoiceId = invoiceRef.getId();

        Invoice invoice = Invoice.builder()
                .invoiceId(invoiceId)
                .transactionId(payhereTransactionId)
                .amount(Double.parseDouble(price))
                .date(formattedDate)
                .uId(fUser.getUid())
                .evStation_id(tvConfirmStation.getText().toString())
                .vehicle_id(vehicle_id)
                .createdAt(formattedDate)
                .energy_kWh(energyRequired.getText().toString())
                .build();

        firebaseFirestore.collection("invoices").document(invoiceId)
                .set(invoice)
                .addOnSuccessListener(v ->{
                 Log.d("INVOICE_SAVE", "Invoice saved successfully!");
                })
                .addOnFailureListener(v ->{
                    Log.e("INVOICE_SAVE", "Failed to save invoice!");
                });

    }
    private void closeActivitySafe(){
        runOnUiThread(() -> {
            loading.dismiss();

            String stationIdForCharging = tvConfirmStation.getText().toString();
            String orderIdForSession = "EcoGrid-" + System.currentTimeMillis();

            DatabaseReference sessionRef = FirebaseDatabase.getInstance()
                    .getReference("stations/" + stationIdForCharging + "/current_session");
            java.util.HashMap<String, Object> sessionData = new java.util.HashMap<>();
            sessionData.put("status", "CHARGING");
            sessionData.put("relay", "ON");
            sessionData.put("orderId", orderIdForSession);
            sessionData.put("userId", firebaseUser.getUid());
            sessionData.put("targetPercentage", tvConfirmTarget.getText().toString() + ".0");
            sessionData.put("startTime", System.currentTimeMillis());
            sessionData.put("energy_kwh", 0.0);
            sessionData.put("time_left_mins", 0);
            sessionData.put("estimated_range_km", 0);
            sessionData.put("livePercentage", 0);
            sessionRef.updateChildren(sessionData);

            Intent intent = new Intent(this, ActiveChargingActivity.class);
            intent.putExtra("STATION_ID", stationIdForCharging);
            intent.putExtra("TARGET_PRECENT", tvConfirmTarget.getText().toString());
            intent.putExtra("ESTIMATE_TIME", estimated_time);
            intent.putExtra("ENERGY_REQUIRED", energyRequired.getText().toString());
            intent.putExtra("ORDER_ID", orderIdForSession);
            intent.putExtra("USER_ID", firebaseUser.getUid());
            startActivity(intent);
            finish();
        });
    }
}