package lk.leadco.ecogrid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.leadco.ecogrid.databinding.ActivityAddCardBinding;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.utils.EcoGridToast;
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitPreapprovalRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class AddCardActivity extends AppCompatActivity {

    private static final String PAYHERE_MERCHANT_ID = "1220989";
    private static final int PAYHERE_REQUEST = 11010;
    private ActivityAddCardBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivityAddCardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        binding.btnProceedToPayHere.setOnClickListener(v ->{ startPayHerePreapproval(); });
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void startPayHerePreapproval(){

        if(firebaseUser == null) return;

        firebaseFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener( task ->{

                    if(task.exists()){

                        User user = task.toObject(User.class);

                        InitPreapprovalRequest req = new InitPreapprovalRequest();
                        req.setMerchantId(PAYHERE_MERCHANT_ID);
                        req.setCurrency("LKR");
                        req.setOrderId("Setup-" + System.currentTimeMillis());
                        req.setItemsDescription("EcoGrid Card Setup");
                        req.getCustomer().setFirstName(user.getName());
                        req.getCustomer().setLastName("");
                        req.getCustomer().setEmail(user.getEmail());
                        req.getCustomer().setPhone(user.getPhoneNumber());
                        req.getCustomer().getAddress().setAddress("Not Provided");
                        req.getCustomer().getAddress().setCity("Not Provided");
                        req.getCustomer().getAddress().setCountry("Sri Lanka");

                        req.setCustom1(firebaseUser.getUid());

                        req.setReturnUrl("https://nexcentauri.com/return");
                        req.setCancelUrl("https://nexcentauri.com/cancel");
                        req.setNotifyUrl("https://us-central1-echogrid-e13a5.cloudfunctions.net/payhereWebhook");

                        Intent intent = new Intent(this, PHMainActivity.class);
                        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
                        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);

                        payHereLauncher.launch(intent);

                        binding.btnProceedToPayHere.setEnabled(false);
                        binding.btnProceedToPayHere.setText("Processing...");


                    }

                });
    }

    private final ActivityResultLauncher<Intent> payHereLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    if (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response = (PHResponse<StatusResponse>)
                                data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                        if (response.isSuccess()) {

                            EcoGridToast.showToast(this,
                                    "Card added successfully via PayHere!",
                                    EcoGridToast.Type.SUCCESS);

                            finish();
                        } else {
                            EcoGridToast.showToast(this,
                                    "Failed: " + response.toString(),
                                    EcoGridToast.Type.ERROR);

                            binding.btnProceedToPayHere.setText("Proceed to PayHere");
                            binding.btnProceedToPayHere.setEnabled(true);
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    EcoGridToast.showToast(this,
                            "User canceled the payment process", EcoGridToast.Type.INFO);

                    binding.btnProceedToPayHere.setText("Proceed to PayHere");
                    binding.btnProceedToPayHere.setEnabled(true);
                }
            }
    );

}