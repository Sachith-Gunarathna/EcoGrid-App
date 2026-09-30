package lk.leadco.ecogrid.Adapter;

import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.leadco.ecogrid.R;
import lk.leadco.ecogrid.fragment.WalletFragment;
import lk.leadco.ecogrid.model.Invoice;

public class TransactionAdapter extends
        RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Invoice> invoiceList;

    public TransactionAdapter(List<Invoice> invoiceList){
        this.invoiceList = invoiceList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);

        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {

        Invoice invoice = invoiceList.get(position);
        holder.tvStationName.setText(invoice.getEvStation_id());
        holder.tvAmount.setText(String.format("- Rs. %.2f", invoice.getAmount()));
        holder.tvKwh.setText(String.valueOf(invoice.getEnergy_kWh()));
        holder.tvDate.setText(invoice.getDate() + " Ref: "+ invoice.getTransactionId());
    }

    @Override
    public int getItemCount() {
        return invoiceList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder{

        TextView tvStationName, tvAmount, tvKwh, tvDate;
        ImageView station_img;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStationName = itemView.findViewById(R.id.tvStationName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvKwh = itemView.findViewById(R.id.tvKwh);
            tvDate = itemView.findViewById(R.id.tvDate);
            station_img = itemView.findViewById(R.id.station_img);
        }

    }
}
