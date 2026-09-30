package lk.leadco.ecogrid.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.utils.OnboardingItem;

public class OnboardingAdapter extends
        RecyclerView.Adapter<OnboardingAdapter.onBoardingViewHolder> {

    private List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems){
        this.onboardingItems = onboardingItems;


    }

    @NonNull
    @Override
    public onBoardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new onBoardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding,parent,false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull onBoardingViewHolder holder, int position) {
        holder.setOnBoardingData(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    class onBoardingViewHolder extends RecyclerView.ViewHolder{

        private TextView textTile;
        private TextView textDescription;
        private ImageView imageOnBoarding;

        onBoardingViewHolder(@NonNull View itemView){
            super(itemView);
            textTile = itemView.findViewById(R.id.tvTitle);
            textDescription = itemView.findViewById(R.id.tvDescription);
            imageOnBoarding = itemView.findViewById(R.id.imgOnboarding);
        }

        void setOnBoardingData(OnboardingItem onBoardingItem){
            textTile.setText(onBoardingItem.getTitle());
            textDescription.setText(onBoardingItem.getDescription());
            imageOnBoarding.setImageResource(onBoardingItem.getImage());
        }
    }
}