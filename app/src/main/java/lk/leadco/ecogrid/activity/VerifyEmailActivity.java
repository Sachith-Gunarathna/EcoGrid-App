package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lk.leadco.ecogrid.R;

public class VerifyEmailActivity extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4;
    private MaterialButton verifyButton;
    private TextView verifySubtitle, resendTextView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private String userEmail;
    private String generatedOtp;
    private long otpTime;

    private boolean canResend = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        verifySubtitle = findViewById(R.id.verifySubtitle);
        resendTextView = findViewById(R.id.resendTextView);
        verifyButton = findViewById(R.id.verifyButton);

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        generatedOtp = getIntent().getStringExtra("GENERATED_OTP");
        otpTime = getIntent().getLongExtra("OTP_TIME", 0);

        if (userEmail != null && !userEmail.isEmpty()) {
            verifySubtitle.setText("Please enter the 4-digit code sent to\n" + userEmail);
        } else {
            verifySubtitle.setText("Please enter the 4-digit code sent to your email");
        }

        setupOtpInputs();

        verifyButton.setOnClickListener(view -> verifyCode());
        resendTextView.setOnClickListener(view -> resendOtp());
    }

    private void verifyCode() {
        String finalOtp = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString();

        if (finalOtp.length() == 4) {

            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - otpTime;
            long fiveMinutesInMillis = 5 * 60 * 1000;

            if (timeDifference > fiveMinutesInMillis) {
                Toast.makeText(this, "OTP Expired! Please try again.",
                        Toast.LENGTH_SHORT).show();
                clearOtpFields();
                return;
            }

            if (finalOtp.equals(generatedOtp)) {
                updateUserVerificationStatus();
            } else {
                Toast.makeText(this, "Invalid Code! Please try again.",
                        Toast.LENGTH_SHORT).show();
                clearOtpFields();
            }

        } else {
            Toast.makeText(this, "Please enter all 4 digits",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserVerificationStatus() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not found! Please login again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        Map<String, Object> update = new HashMap<>();
        update.put("isVerified", true);
        update.put("otp", null);

        firebaseFirestore.collection("users").document(uid)
                .update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Email Verified Successfully!",
                            Toast.LENGTH_SHORT).show();

                    Intent homeIntent = new Intent(VerifyEmailActivity.this,
                            MainActivity.class);

                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(homeIntent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Verification failed in Database",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void resendOtp() {
        if (canResend) {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser == null) return;

            int newOtp = (int) (Math.random() * 9000) + 1000;
            generatedOtp = String.valueOf(newOtp);
            otpTime = System.currentTimeMillis();

            String uid = currentUser.getUid();
            firebaseFirestore.collection("users").document(uid)
                    .update("otp", generatedOtp, "otpTime", otpTime);

            sendEmailWithOtp(userEmail, generatedOtp);
            startResendTimer();

            Toast.makeText(this, "A new code has been sent!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please wait before resending",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmailWithOtp(String userEmail, String otpCode) {
        String emailJsUrl = "https://api.emailjs.com/api/v1.0/email/send";
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JSONObject jsonBody = new JSONObject();

        try {
            jsonBody.put("service_id", "service_mq1utss");
            jsonBody.put("template_id", "template_ufhzdau");
            jsonBody.put("user_id", "iOddl06krjtfriX19");

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String expireTime = sdf.format(new Date(System.currentTimeMillis() + (5 * 60 * 1000)));

            JSONObject templateParams = new JSONObject();
            templateParams.put("user_email", userEmail);
            templateParams.put("passcode", otpCode);
            templateParams.put("time", expireTime);

            jsonBody.put("template_params", templateParams);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, emailJsUrl, jsonBody,
                response -> Toast.makeText(this, "OTP Sent Successfully!",
                        Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(this, "Failed to send OTP email",
                        Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(request);
    }

    private void startResendTimer() {
        canResend = false;
        resendTextView.setEnabled(false);


        new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {
                resendTextView.setText("Resend Code in " + l / 1000 + "s");
                resendTextView.setTextColor(Color.parseColor("#888888"));
            }

            @Override
            public void onFinish() {
                canResend = true;
                resendTextView.setEnabled(true);
                resendTextView.setText("Resend Code");
                resendTextView.setTextColor(Color.parseColor("#000000"));
            }
        }.start();
    }

    private void clearOtpFields() {
        otp1.setText("");
        otp2.setText("");
        otp3.setText("");
        otp4.setText("");
        otp1.requestFocus();
    }

    private void setupOtpInputs() {
        otp1.addTextChangedListener(new GenericTextWatcher(otp1, otp2));
        otp2.addTextChangedListener(new GenericTextWatcher(otp2, otp3));
        otp3.addTextChangedListener(new GenericTextWatcher(otp3, otp4));
        otp4.addTextChangedListener(new GenericTextWatcher(otp4, null));

        otp1.setOnKeyListener(new GenericKeyEvent(otp1, null));
        otp2.setOnKeyListener(new GenericKeyEvent(otp2, otp1));
        otp3.setOnKeyListener(new GenericKeyEvent(otp3, otp2));
        otp4.setOnKeyListener(new GenericKeyEvent(otp4, otp3));
    }

    private class GenericTextWatcher implements TextWatcher {
        private View currentView;
        private View nextView;

        public GenericTextWatcher(View currentView, View nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            String text = editable.toString();
            if (text.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }

    private class GenericKeyEvent implements View.OnKeyListener {
        private EditText currentView;
        private EditText previousView;

        public GenericKeyEvent(EditText currentView, EditText previousView) {
            this.currentView = currentView;
            this.previousView = previousView;
        }

        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_DEL &&
                    currentView.getText().toString().isEmpty()) {
                if (previousView != null) {
                    previousView.requestFocus();
                }
                return true;
            }
            return false;
        }
    }
}