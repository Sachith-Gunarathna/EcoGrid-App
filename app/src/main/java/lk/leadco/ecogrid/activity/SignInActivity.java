package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

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

import java.util.Arrays;
import java.util.Objects;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivitySignInBinding;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.utils.EcoGridDialog;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class SignInActivity extends AppCompatActivity {

    private CredentialManager credentialManager;
    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private CallbackManager mCallbackManager;
    private EcoGridLoadingDialog loadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(android.graphics.Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);
        loadingDialog = new EcoGridLoadingDialog(this);

        if (firebaseAuth.getCurrentUser() != null) {
            goToMainActivity();
            return;
        }

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
        binding.tvForgotPassword.setOnClickListener(v -> showPasswordResetDialog());

        binding.signUpLink.setOnClickListener(view -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            finish();
        });

        binding.signInBtn.setOnClickListener(view -> handleEmailSignIn());
    }

    private void handleEmailSignIn() {
        String email = binding.emailAddress.getText().toString().trim();
        String password = binding.password.getText().toString().trim();

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

        loadingDialog.show();
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fetchUserFromFirestoreAndLogin(firebaseAuth.getCurrentUser());
                    } else {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(this,
                                "Authentication failed", EcoGridToast.Type.ERROR);
                    }
                });
    }

    private void showPasswordResetDialog() {
        EcoGridDialog.showInputDialog(this, "RESET PASSWORD",
                "Enter your registered email address to receive a password reset link.",
                "Send Link", "Cancel",
                "Please enter your email!", email -> {

                    loadingDialog.show();

                    firebaseAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                dismissLoadingSafe();
                                if (task.isSuccessful()) {
                                    EcoGridToast.showToast(SignInActivity.this,
                                            "Reset link sent to your email!",
                                            EcoGridToast.Type.SUCCESS);
                                } else {
                                    EcoGridToast.showToast(SignInActivity.this,
                                            "No account found or failed to send email!",
                                            EcoGridToast.Type.ERROR);
                                }
                            });
                });
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
                        EcoGridToast.showToast(SignInActivity.this,
                                "Facebook Login Cancelled", EcoGridToast.Type.ERROR);
                    }

                    @Override
                    public void onError(@NonNull FacebookException e) {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(SignInActivity.this,
                                "Facebook Error: " + e.getMessage(), EcoGridToast.Type.ERROR);
                    }
                });

        binding.btnFacebookSignIn.setOnClickListener(v -> {
            loadingDialog.show();
            LoginManager.getInstance().logInWithReadPermissions(SignInActivity.this,
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
                .addOnSuccessListener(authResult -> handlePostLogin
                        (authResult.getAdditionalUserInfo().isNewUser()))
                .addOnFailureListener(e -> {
                    dismissLoadingSafe();
                    EcoGridToast.showToast(this,
                            "Microsoft Sign-In Failed: " +
                                    e.getMessage(), EcoGridToast.Type.ERROR);
                });
    }

    private void signInWithGoogleModern() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();
        loadingDialog.show();

        credentialManager.getCredentialAsync(this, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential &&
                                credential.getType()
                                        .equals(GoogleIdTokenCredential
                                                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            GoogleIdTokenCredential googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.getData());
                            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                        } else {
                            dismissLoadingSafe();
                            EcoGridToast.showToast(SignInActivity.this,
                                    "Unexpected type of credential", EcoGridToast.Type.ERROR);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        dismissLoadingSafe();
                        EcoGridToast.showToast(SignInActivity.this,
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
                        EcoGridToast.showToast(this,
                                "Firebase Auth Failed.", EcoGridToast.Type.ERROR);
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

    private void checkAndSaveUserInfo(FirebaseUser user){
        firebaseFirestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task ->{
                    if(task.isSuccessful()){
                        if(task.getResult().exists()){
                            EcoGridToast.showToast(SignInActivity.this,
                                    "Welcome Back! " +
                                            (user.getDisplayName() != null ?
                                                    user.getDisplayName() : ""),
                                    EcoGridToast.Type.SUCCESS);
                            goToMainActivity();
                        }else{
                            saveNewUserToFirestore(user);
                        }
                    }else{
                        dismissLoadingSafe();
                        EcoGridToast.showToast(SignInActivity.this,
                                "Failed to verify user data.", EcoGridToast.Type.ERROR);
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
                .email(finalEmail)
                .name(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User")
                .isVerified(true)
                .profilePicUrl(String.valueOf(firebaseUser.getPhotoUrl()))
                .build();

        firebaseFirestore.collection("users").document(firebaseUser.getUid())
                .set(userModel)
                .addOnSuccessListener(v -> {
                    EcoGridToast.showToast(this,
                            "Sign-In Success! " + firebaseUser.getDisplayName(),
                            EcoGridToast.Type.SUCCESS);
                    fetchUserFromFirestoreAndLogin(firebaseUser);
                })
                .addOnFailureListener(e -> {
                    dismissLoadingSafe();
                    EcoGridToast.showToast(this,
                            "Database Save Failed!", EcoGridToast.Type.ERROR);
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
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}