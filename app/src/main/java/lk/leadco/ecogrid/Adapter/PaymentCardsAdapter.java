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
import lk.leadco.ecogrid.model.PaymentCard;

public class PaymentCardsAdapter extends RecyclerView.Adapter<PaymentCardsAdapter.CardViewHolder> {

    private List<PaymentCard> cardModelList;
    private OnCardClickListener listener;

    public interface OnCardClickListener{
        void onSetDefaultClick(PaymentCard card);
    }
    public PaymentCardsAdapter(List<PaymentCard> cardModelList, OnCardClickListener listener){
        this.cardModelList = cardModelList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_card,
                parent,false);

        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        PaymentCard card = cardModelList.get(position);

        holder.tvCardHolderName.setText(card.getCardHolderName().toUpperCase());
        holder.tvCardNumber.setText("**** **** **** "+card.getLast4());
        holder.tvExpiryDate.setText(card.getExpDate());
        holder.tvCardType.setText(card.getBrand().toUpperCase());

        if(card.isDefaultCard()){
            holder.tvDefaultBadge.setVisibility(View.VISIBLE);
            holder.btnSetDefault.setVisibility(View.GONE);
        }else{
            holder.tvDefaultBadge.setVisibility(View.GONE);
            holder.btnSetDefault.setVisibility(View.VISIBLE);
        }

        holder.btnSetDefault.setOnClickListener( v ->{
            if(listener != null){
                listener.onSetDefaultClick(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardModelList.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder{
        TextView tvCardHolderName, tvCardNumber, tvExpiryDate, tvCardType,tvDefaultBadge,btnSetDefault;
        ImageView imgChip;

        public CardViewHolder(@NonNull View itemView){
            super(itemView);

            tvCardHolderName = itemView.findViewById(R.id.tvCardHolderName);
            tvCardNumber = itemView.findViewById(R.id.tvCardNumber);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvCardType = itemView.findViewById(R.id.tvCardType);
            imgChip = itemView.findViewById(R.id.imgChip);
            tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
            btnSetDefault = itemView.findViewById(R.id.btnSetDefault);

        }

    }

}
