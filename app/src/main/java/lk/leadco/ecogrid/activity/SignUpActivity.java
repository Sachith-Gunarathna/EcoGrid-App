package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivitySignUpBinding;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.utils.EcoGridDialog;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class SignUpActivity extends AppCompatActivity {

    private CredentialManager credentialManager;
    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private CallbackManager mCallbackManager;
    private EcoGridLoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new EcoGridLoadingDialog(this);
        credentialManager = CredentialManager.create(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setupClickListeners();

        setupFacebookLogin();
    }

    private void dismissLoadingSafe() {
        if (!isFinishing() && !isDestroyed() && loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    private void setupClickListeners() {

        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogleModern());
        binding.btnMicrosoftSignIn.setOnClickListener(v -> signInWithMicrosoftModern());

        binding.signInLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signUpBtn.setOnClickListener(v -> handleEmailSignUp());
    }

    private void handleEmailSignUp() {
        String email = binding.emailAddress.getText().toString().trim();
        String password = binding.password.getText().toString().trim();
        String re_enter_password = binding.reEnterPassword.getText().toString().trim();

        if (email.isEmpty()) {
            binding.emailAddress.setError("Email is required");
            binding.emailAddress.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailAddress.setError("Invalid email");
            binding.emailAddress.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.password.setError("Password is required");
            binding.password.requestFocus();
            return;
        }

        if (re_enter_password.isEmpty()) {
            binding.reEnterPassword.setError("Please re-enter password");
            binding.reEnterPassword.requestFocus();
            return;
        }

        if (!password.equals(re_enter_password)) {
            binding.reEnterPassword.setError("Password does not match");
            binding.reEnterPassword.requestFocus();
            return;
        }

        loadingDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        int randomOtp = (int) (Math.random() * 9000) + 1000;
                        String generatedOtpString = String.valueOf(randomOtp);
                        long otpGenerationTime = System.currentTimeMillis();

                        User user = User.builder().uid(uid).email(email).otp(generatedOtpString)
                                .otpTime(otpGenerationTime).isVerified(false).build();

                        firebaseFirestore.collection("users").document(uid).set(user)
                                .addOnSuccessListener(unused -> {
                                    dismissLoadingSafe();
                                    Toast.makeText(getApplicationContext(), "Saved success",
                                            Toast.LENGTH_SHORT).show();

                                    sendEmailWithOtp(email, generatedOtpString);

                                    Intent intent = new Intent(SignUpActivity.this,
                                            VerifyEmailActivity.class);
                                    intent.putExtra("USER_EMAIL", email);
                                    intent.putExtra("GENERATED_OTP", generatedOtpString);
                                    intent.putExtra("OTP_TIME", otpGenerationTime);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    dismissLoadingSafe();
                                    Toast.makeText(getApplicationContext(),
                                            "Authentication failed in Database",
                                            Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        dismissLoadingSafe();
                        Toast.makeText(getApplicationContext(),
                                "User already registered or error occurred!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
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

        StringRequest request = new StringRequest(Request.Method.POST, emailJsUrl,
                response -> Toast.makeText(SignUpActivity.this,
                        "OTP Sent Successfully!", Toast.LENGTH_SHORT).show(),
                error -> {
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        String errorResponseBody = new String(error.networkResponse.data);
                        Log.e("EMAILJS_DEBUG", "Error Body: " + errorResponseBody);
                    }
                    Toast.makeText(SignUpActivity.this, "Failed to send OTP",
                            Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return jsonBody.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        requestQueue.add(request);
    }

    private void setupFacebookLogin() {
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(SignUpActivity.this,
                                "Facebook Login Cancelled", EcoGridToast.Type.ERROR);
                    }

                    @Override
                    public void onError(@NonNull FacebookException e) {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(SignUpActivity.this,
                                "Facebook Error: " + e.getMessage(), EcoGridToast.Type.ERROR);
                    }
                });

        binding.btnFacebookSignIn.setOnClickListener(v -> {
            loadingDialog.show();
            LoginManager.getInstance().logInWithReadPermissions(SignUpActivity.this,
                    Arrays.asList("email", "public_profile"));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this,
                task -> {
                    if (task.isSuccessful()) {
                        handlePostLogin(task.getResult().getAdditionalUserInfo().isNewUser());
                    } else {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(this,
                                "Facebook Firebase Auth Failed.", EcoGridToast.Type.ERROR);
                    }
                });
    }

    private void signInWithMicrosoftModern() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("microsoft.com");
        provider.addCustomParameter("prompt", "select_account");

        loadingDialog.show();
        firebaseAuth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult ->
                        handlePostLogin(authResult.getAdditionalUserInfo().isNewUser()))
                .addOnFailureListener(e -> {
                    dismissLoadingSafe();
                    EcoGridToast.showToast(this, "Microsoft Sign-In Failed: "
                            + e.getMessage(), EcoGridToast.Type.ERROR);
                });
    }

    private void signInWithGoogleModern() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption).build();
        loadingDialog.show();

        credentialManager.getCredentialAsync(this, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential &&
                                credential.getType().equals(GoogleIdTokenCredential
                                        .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            GoogleIdTokenCredential googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.getData());
                            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                        } else {
                            dismissLoadingSafe();
                            EcoGridToast.showToast(SignUpActivity.this,
                                    "Unexpected type of credential", EcoGridToast.Type.ERROR);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(SignUpActivity.this,
                                "Google Sign-In Failed", EcoGridToast.Type.ERROR);
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this,
                task -> {
                    if (task.isSuccessful()) {
                        handlePostLogin(Objects.requireNonNull(task.getResult()
                                .getAdditionalUserInfo()).isNewUser());
                    } else {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(this, "Firebase Auth Failed.",
                                EcoGridToast.Type.ERROR);
                    }
                });
    }

    private void handlePostLogin(boolean isNewUser) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            dismissLoadingSafe();
            return;
        }
        checkAndSaveUserInfo(user);
    }

    private void checkAndSaveUserInfo(FirebaseUser user) {
        firebaseFirestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            EcoGridToast.showToast(this, "Welcome Back! " +
                                    (user.getDisplayName() != null ? user.getDisplayName() : ""),
                                    EcoGridToast.Type.SUCCESS);
                            goToMainActivity();
                        } else {
                            saveNewUserToFirestore(user);
                        }
                    } else {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(this, "Failed to verify user data.",
                                EcoGridToast.Type.ERROR);
                    }
                });
    }

    private void saveNewUserToFirestore(FirebaseUser firebaseUser) {
        String fetchedEmail = firebaseUser.getEmail();

        if (fetchedEmail == null || fetchedEmail.isEmpty()) {

            dismissLoadingSafe();

            EcoGridDialog.showInputDialog(this, "Email Required",
                    "We couldn't get your email address. Please enter it to continue.",
                    "Save & Continue", "Cancel",
                    "Email cannot be empty!",
                    emailInput -> {

                        loadingDialog.show();
                        proceedToSaveUser(firebaseUser, emailInput);
                    });
        } else {
            proceedToSaveUser(firebaseUser, fetchedEmail);
        }


    }

    private void proceedToSaveUser(FirebaseUser firebaseUser, String finalEmail) {
        User userModel = User.builder()
                .uid(firebaseUser.getUid())
                .email(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "")
                .name(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User")
                .isVerified(true)
                .profilePicUrl(String.valueOf(firebaseUser.getPhotoUrl()))
                .build();

        firebaseFirestore.collection("users").document(firebaseUser.getUid())
                .set(userModel)
                .addOnSuccessListener(v -> {
                    EcoGridToast.showToast(this, "Sign-In Success! " +
                            firebaseUser.getDisplayName(), EcoGridToast.Type.SUCCESS);
                    fetchUserFromFirestoreAndLogin(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    dismissLoadingSafe();
                    EcoGridToast.showToast(this, "Database Save Failed!",
                            EcoGridToast.Type.ERROR);
                });
    }

    private void fetchUserFromFirestoreAndLogin(FirebaseUser firebaseUser) {
        firebaseFirestore.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        goToMainActivity();
                    } else {
                        dismissLoadingSafe();
                    }
                });
    }

    private void goToMainActivity() {
        dismissLoadingSafe();
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}