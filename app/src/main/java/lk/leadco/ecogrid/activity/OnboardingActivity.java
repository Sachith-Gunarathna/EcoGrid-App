package lk.leadco.ecogrid.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import lk.leadco.ecogrid.Adapter.OnboardingAdapter;
import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.utils.OnboardingItem;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutOnboardingIndicators;
    private ExtendedFloatingActionButton buttonOnboardingAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        layoutOnboardingIndicators = findViewById(R.id.layoutDots);
        buttonOnboardingAction = findViewById(R.id.btnNext);
        TextView textSkip = findViewById(R.id.tvSkip);

        setupOnboardingItems();

        ViewPager2 onboardingViewPager = findViewById(R.id.viewPager);
        onboardingViewPager.setAdapter(onboardingAdapter);

        setupOnboardingIndicators();
        setCurrentOnboardingIndicator(0);

        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentOnboardingIndicator(position);

                if(position == onboardingAdapter.getItemCount() -1){
                    buttonOnboardingAction.setText("Get Started");
                }else{
                    buttonOnboardingAction.setText("Next");
                }
            }
        });

        buttonOnboardingAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(onboardingViewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()){
                    onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);

                }else{
                    navigateToSignIn();
                }
            }
        });

        textSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToSignIn();
            }
        });
    }

    public void setupOnboardingItems(){
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        OnboardingItem itemFast = new OnboardingItem(
                R.drawable.scooter,
                "Find Charging Stations",
                "Locate the nearest EcoGrid charging stations easily with our smart map network."
        );

        OnboardingItem itemPay = new OnboardingItem(
                R.drawable.scooter,
                "Smart Eco Charging",
                "Monitor your battery life and charge seamlessly with intelligent power-saving modes."
        );

        OnboardingItem itemJourney = new OnboardingItem(
                R.drawable.car,
                "Limitless Journey",
                "Enjoy an uninterrupted ride with ultra-fast charging and extended driving range."
        );

        onboardingItems.add(itemFast);
        onboardingItems.add(itemPay);
        onboardingItems.add(itemJourney);

        onboardingAdapter = new OnboardingAdapter(onboardingItems);

    }

    private void setupOnboardingIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8,0,8,0);

        for(int i=0;i<indicators.length;i++){
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    android.R.drawable.presence_invisible
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutOnboardingIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentOnboardingIndicator(int index) {
        int childCount = layoutOnboardingIndicators.getChildCount();
        for(int i=0;i<childCount;i++){

            ImageView imageView = (ImageView) layoutOnboardingIndicators.getChildAt(i);

            if(i == index){
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), android.R.drawable.presence_online
                ));

            }else{

                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), android.R.drawable.presence_invisible
                ));

            }
        }
    }

    private void navigateToSignIn(){

        SharedPreferences preferences = getSharedPreferences("EcoGridPrefs",MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isFirstTime",false);
        editor.apply();

        Intent intent = new Intent(OnboardingActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }
}
