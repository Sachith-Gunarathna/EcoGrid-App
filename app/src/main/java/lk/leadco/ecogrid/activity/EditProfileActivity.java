package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivityEditProfileBinding;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.utils.EcoGridLoadingDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;

    private StorageReference storageReference;
    private Uri profileImageUri = null;
    private Uri bannerImageUri = null;
    private EcoGridLoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            finish();
            return;
        }

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new EcoGridLoadingDialog(this);

        firebaseFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener( task -> {
                    if (!task.exists()) return;
                    User user = task.toObject(User.class);
                    if (user == null) return;

                    if(user.getProfileBannerUrl() != null && !user.getProfileBannerUrl().isEmpty()){
                        Glide.with(this)
                                .load(user.getProfileBannerUrl())
                                .placeholder(R.drawable.placeholder)
                                .into(binding.imgCover);
                    }else{
                        binding.imgCover.setImageResource(R.drawable.placeholder);
                    }

                    if(user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()){
                        Glide.with(this)
                                .load(user.getProfilePicUrl())
                                .placeholder(R.drawable.placeholder)
                                .into(binding.imgProfile);
                    }else{
                        binding.imgProfile.setImageResource(R.drawable.placeholder);
                    }

                    if(user.getEmail() != null){
                        binding.etEmail.setText(user.getEmail());
                    }

                    if(user.getName() != null){
                        binding.etFullName.setText(user.getName());
                    }

                    if(user.getPhoneNumber() != null){
                        binding.etPhone.setText(user.getPhoneNumber());
                    }

                });

        binding.btnEditProfilePic.setOnClickListener( v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            profilePickerLauncher.launch(intent);
        });

        binding.btnEditBanner.setOnClickListener(v ->{
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            bannerPickerLauncher.launch(intent);
        });

        binding.btnSave.setOnClickListener( v -> { saveProfile(); });
        binding.btnBack.setOnClickListener(v ->{ finish(); });
    }

    private final ActivityResultLauncher<Intent> profilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->{
                if(result.getResultCode() == RESULT_OK && result.getData() != null){
                    profileImageUri = result.getData().getData();
                    Glide.with(this)
                            .load(profileImageUri)
                            .into(binding.imgProfile);
                }
            }
    );

    private final ActivityResultLauncher<Intent> bannerPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->{
                if(result.getResultCode() == RESULT_OK && result.getData() != null){
                    bannerImageUri = result.getData().getData();
                   Glide.with(this)
                           .load(bannerImageUri)
                           .into(binding.imgCover);
                }
            }
    );

    private void saveProfile(){

        String fullName = binding.etFullName.getText().toString().trim();
        String mobileNumber = binding.etPhone.getText().toString().trim();

        if(fullName.isEmpty()){
            binding.etFullName.setError("Full name is required");
            binding.etFullName.requestFocus();
            return;
        }

        loadingDialog.show();
        EcoGridToast.showToast(this, "Saving changes...", EcoGridToast.Type.INFO);
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Saving...");

        uploadImagesAndSaveData(fullName, mobileNumber);

    }

    private void uploadImagesAndSaveData(String fullName, String mobileNumber){

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", fullName);
        updates.put("phoneNumber",mobileNumber);

        if(profileImageUri != null){

            StorageReference profileRef = storageReference.child("profile_images/"
                    +firebaseUser.getUid() +".jpg");
            profileRef.putFile(profileImageUri).addOnSuccessListener(taskSnapshot -> {
                profileRef.getDownloadUrl().addOnSuccessListener( uri ->{
                    updates.put("profilePicUrl", uri.toString());

                    checkAndUploadBannerImage(updates);
                });
            }).addOnFailureListener(e ->{
                handleFailure("Failed to upload profile image");
            });
        }else {
            checkAndUploadBannerImage(updates);
        }

    }

    private void checkAndUploadBannerImage(Map<String, Object> updates){

        if(bannerImageUri != null){

            StorageReference bannerRef = storageReference.child("banner_image/"
                                        +"bannerImageUrl"+ firebaseUser.getUid()+".jpg");

            bannerRef.putFile(bannerImageUri).addOnSuccessListener(taskSnapshot ->{
                bannerRef.getDownloadUrl().addOnSuccessListener(uri ->{
                    updates.put("profileBannerUrl", uri.toString());

                    saveToFirestore(updates);
                });
            }).addOnFailureListener(e ->{
                handleFailure("Failed to upload banner image");
            });
        }else{
            saveToFirestore(updates);
        }

    }

    private void saveToFirestore(Map<String, Object> updates){

        firebaseFirestore.collection("users").document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid ->{
                    loadingDialog.dismiss();
                    EcoGridToast.showToast(this, "Profile updated successfully!",
                            EcoGridToast.Type.SUCCESS);
                    finish();
                })
                .addOnFailureListener(aVoid ->{
                    handleFailure("Failed to update profile");
                });
    }

    private void handleFailure(String message){
        loadingDialog.dismiss();
        binding.btnSave.setEnabled(true);
        binding.btnSave.setText("Save Changes");
        EcoGridToast.showToast(this, message, EcoGridToast.Type.ERROR);

    }

}
