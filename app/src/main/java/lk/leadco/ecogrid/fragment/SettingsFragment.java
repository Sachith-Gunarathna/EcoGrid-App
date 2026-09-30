
package lk.leadco.ecogrid.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.activity.AddVehicleActivity;
import lk.leadco.ecogrid.activity.EditProfileActivity;
import lk.leadco.ecogrid.activity.PaymentMethodsActivity;
import lk.leadco.ecogrid.activity.SignInActivity;
import lk.leadco.ecogrid.activity.VehiclesActivity;
import lk.leadco.ecogrid.databinding.FragmentSettingsBinding;
import lk.leadco.ecogrid.model.User;
import lk.leadco.ecogrid.utils.EcoGridDialog;
import lk.leadco.ecogrid.utils.EcoGridToast;
import lk.leadco.ecogrid.utils.SharedPrefsManager;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;
    private int currentLimit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseFirestore.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener( task ->{

                    User user = task.toObject(User.class);

                    if(user.getName() != null){
                        binding.tvUserName.setText(user.getName());
                    }else{
                        binding.tvUserName.setText("User");
                    }

                    if(user.getEmail() != null){
                        binding.tvUserEmail.setText(user.getEmail());
                    }else{
                        binding.tvUserEmail.setText("user@example.com");
                    }

                    if(user.getProfilePicUrl() != null){
                        Glide.with(this)
                                .load(user.getProfilePicUrl())
                                .clone()
                                .into(binding.profilePic);
                    }else {
                        Glide.with(this)
                                .load(R.drawable.placeholder)
                                .clone()
                                .into(binding.profilePic);
                    }

                });

        boolean isPushEnable = SharedPrefsManager.getPauseNotification(requireContext());
        binding.switchNotifications.setChecked(!isPushEnable);

        binding.switchNotifications.setOnCheckedChangeListener((buttonView,
                                                                isChecked) ->{
            boolean shouldPause = !isChecked;
            SharedPrefsManager.savePauseNotification(requireContext(), shouldPause);

            if(isChecked){
                EcoGridToast.showToast(requireContext(), "Notifications are enabled",
                        EcoGridToast.Type.SUCCESS);
            }else{
                EcoGridToast.showToast(requireContext(), "Notifications are paused",
                        EcoGridToast.Type.SUCCESS);
            }
        });

         currentLimit = SharedPrefsManager.getDefaultTargetLimit(requireContext());
        binding.tvTargetValue.setText(currentLimit+"%");

        binding.editProfile.setOnClickListener(v ->{ editProfile(); });

        binding.btnMyVehicles.setOnClickListener( v ->{ myVehicles(); });

        binding.btnPaymentMethods.setOnClickListener( v ->{ paymentMethods(); });

        binding.btnDefaultTarget.setOnClickListener( v ->{ defaultTarget(); });

        binding.btnHelpCenter.setOnClickListener( v ->{ helpCenter(); });

        binding.btnLogout.setOnClickListener( v ->{ logout(); });
    }

    private void editProfile(){
        Intent intent = new Intent(requireActivity(), EditProfileActivity.class);
        startActivity(intent);
    }

    private void myVehicles(){
        Intent intent = new Intent(requireActivity(), VehiclesActivity.class);
        startActivity(intent);
    }

    private void paymentMethods(){
        Intent intent = new Intent(requireActivity(), PaymentMethodsActivity.class);
        startActivity(intent);
    }

    private void defaultTarget(){


        String[] options = {"50%", "60%", "70%", "80%", "90%", "100%"};
        int[] values = {50, 60, 70, 80, 90, 100};

        int selectedIndex = 0;
        for(int i = 0; i < values.length; i++){
            if(values[i] == currentLimit){
                selectedIndex = i;
                break;
            }

        }

        EcoGridDialog.showSingleChoiceDialog(requireContext(),
                "Set Default Target Limit",
                options,
                selectedIndex,
                (index, selectedText) -> {

                    int newLimit = values[index];

                    binding.tvTargetValue.setText(newLimit + "%");

                    SharedPrefsManager.saveDefaultTargetLimit(requireContext(), newLimit);
                });

    }

    private void helpCenter(){

        EcoGridDialog.showConfirmDialog(requireContext(),
                "Contact Support",
                "Do you want to call Nexcentauri Support center?",
                "Call Now",
                "Cancel",
                ()->{
                    String phoneNumber = "tel:+94763145020";
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(phoneNumber));
                    startActivity(intent);
                });

    }

    private void logout(){
        firebaseAuth.signOut();

        Intent intent = new Intent(requireActivity(), SignInActivity.class);
        startActivity(intent);
    }
}