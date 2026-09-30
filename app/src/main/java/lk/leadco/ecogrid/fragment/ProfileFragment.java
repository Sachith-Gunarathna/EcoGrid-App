package lk.leadco.ecogrid.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.activity.PaymentsActivity;
import lk.leadco.ecogrid.activity.TermsActivity;
import lk.leadco.ecogrid.activity.VehiclesActivity;
import lk.leadco.ecogrid.databinding.FragmentProfileBinding;
import lk.leadco.ecogrid.model.User;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(firebaseUser != null){

            firebaseFirestore.collection("users")
                    .document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(db ->{

                        User user = db.toObject(User.class);

                        binding.tvUserName.setText(user.getName());
                        binding.tvUserEmail.setText(user.getEmail());


                        if(user.getProfilePicUrl() != null){
                            Glide.with(this)
                                    .load(user.getProfilePicUrl())
                                    .clone()
                                    .into(binding.imgProfile);
                        }else{
                            Glide.with(this)
                                    .load(R.drawable.placeholder)
                                    .clone()
                                    .into(binding.imgProfile);
                        }

                        if(user.getProfileBannerUrl() != null){
                            Glide.with(this)
                                    .load(user.getProfileBannerUrl())
                                    .clone()
                                    .into(binding.imgCover);
                        }else{
                            Glide.with(this)
                                    .load(R.drawable.placeholder)
                                    .clone()
                                    .into(binding.imgCover);
                        }

                    });
        }

        binding.btnVehicles.setOnClickListener( v ->{
            Intent intent = new Intent(requireContext(), VehiclesActivity.class);
            startActivity(intent);
        });

        binding.btnPayments.setOnClickListener( v ->{
            Intent intent = new Intent(requireContext(), PaymentsActivity.class);
            startActivity(intent);
        });

        binding.btnTerms.setOnClickListener( v ->{
            Intent intent = new Intent(requireContext(), TermsActivity.class);
            startActivity(intent);
        });
    }
}