package lk.leadco.ecogrid.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.databinding.ActivityMainBinding;
import lk.leadco.ecogrid.databinding.SideNavHeaderBinding;
import lk.leadco.ecogrid.fragment.HomeFragment;
import lk.leadco.ecogrid.fragment.NavigationFragment;
import lk.leadco.ecogrid.fragment.ProfileFragment;
import lk.leadco.ecogrid.fragment.ScannerFragment;
import lk.leadco.ecogrid.fragment.SettingsFragment;
import lk.leadco.ecogrid.fragment.WalletFragment;
import lk.leadco.ecogrid.model.User;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;

    private SideNavHeaderBinding sideNavHeaderBinding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private  FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));

        View decor = window.getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.sideNavigationView.getHeaderView(0);

        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigation;

        setSupportActionBar(toolbar);

        ImageView btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if(savedInstanceState == null){
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

         currentUser = firebaseAuth.getCurrentUser();

        if(currentUser != null){

            updateUserProfileUI();

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener( task ->{

                if(task.isSuccessful() && task.getResult() != null){

                    String token = task.getResult();
                    firebaseFirestore.collection("users")
                            .document(currentUser.getUid())
                            .update("fcmToken",token)
                            .addOnSuccessListener( v ->{
                                Log.d("FCM_TEST","SUCCESS");
                            })
                            .addOnFailureListener( v ->{
                                Log.e("FCM_TEST","FAILED");
                            });

                }

            });

            navigationView.getMenu().findItem(R.id.side_nav_login).setVisible(false);

            navigationView.getMenu().findItem(R.id.side_nav_home).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_navigation).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_scanner).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_wallet).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_settings).setVisible(true);
            navigationView.getMenu().findItem(R.id.side_nav_logout).setVisible(true);
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        Menu navMenu = navigationView.getMenu();
        Menu bottomNavMenu = bottomNavigationView.getMenu();

        if(currentUser != null){

            for(int i=0 ; i<navMenu.size(); i++){
                navMenu.getItem(i).setChecked(false);
            }

            for(int i=0; i< bottomNavMenu.size();i++){
                bottomNavMenu.getItem(i).setChecked(false);
            }

            if(itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home){
                loadFragment(new HomeFragment());
                navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);

            }else if(itemId == R.id.side_nav_navigation || itemId == R.id.bottom_nav_navigation){
                loadFragment(new NavigationFragment());
                navigationView.getMenu().findItem(R.id.side_nav_navigation).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_navigation).setChecked(true);

            }else if(itemId == R.id.side_nav_scanner || itemId == R.id.bottom_nav_scanner){
                loadFragment(new ScannerFragment());
                navigationView.getMenu().findItem(R.id.side_nav_scanner).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_scanner).setChecked(true);

            }else if(itemId == R.id.side_nav_wallet || itemId == R.id.bottom_nav_wallet){
                loadFragment(new WalletFragment());
                navigationView.getMenu().findItem(R.id.side_nav_wallet).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_wallet).setChecked(true);

            }else if(itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile){
                loadFragment(new ProfileFragment());
                navigationView.getMenu().findItem(R.id.side_nav_profile).setChecked(true);
                bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile).setChecked(true);

            }else if(itemId == R.id.side_nav_settings){
                loadFragment(new SettingsFragment());
                navigationView.getMenu().findItem(R.id.side_nav_settings).setChecked(true);
                bottomNavigationView.getMenu().close();

            }else if(itemId == R.id.side_nav_logout){
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signOut();
                Intent intent = new Intent(MainActivity.this,SignInActivity.class);
                startActivity(intent);
                finish();
            }

            if(drawerLayout.isDrawerOpen(GravityCompat.START)){
                drawerLayout.closeDrawer(GravityCompat.START);
            }

        }else{
            Intent intent = new Intent(MainActivity.this,SignInActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }

    private void loadFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container,fragment);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserProfileUI();
    }

    private void updateUserProfileUI() {
        if (currentUser != null) {
            firebaseFirestore.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(ds -> {
                        if (ds.exists()) {
                            User user = ds.toObject(User.class);
                            if (user != null) {
                                sideNavHeaderBinding.headerUserEmail.setText(user.getEmail());
                                sideNavHeaderBinding.headerUserName.setText(user.getName());
                                binding.tvUserName.setText(user.getName());

                                if (user.getProfilePicUrl() != null) {
                                    Glide.with(MainActivity.this)
                                            .load(user.getProfilePicUrl())
                                            .clone()
                                            .into(sideNavHeaderBinding.imgProfile);

                                    Glide.with(MainActivity.this)
                                            .load(user.getProfilePicUrl())
                                            .clone()
                                            .into(binding.imgProfile);
                                } else {
                                    sideNavHeaderBinding.imgProfile.setImageResource(R.drawable.icons8_user_100);
                                    binding.imgProfile.setImageResource(R.drawable.icons8_user_100);
                                }

                                if (user.getProfileBannerUrl() != null) {
                                    Glide.with(MainActivity.this)
                                            .load(user.getProfileBannerUrl())
                                            .clone()
                                            .into(sideNavHeaderBinding.bannerImage);
                                } else {
                                    Glide.with(MainActivity.this)
                                            .load(R.drawable.placeholder)
                                            .clone()
                                            .into(sideNavHeaderBinding.bannerImage);
                                }
                            }
                        }
                    });
        }
    }
}