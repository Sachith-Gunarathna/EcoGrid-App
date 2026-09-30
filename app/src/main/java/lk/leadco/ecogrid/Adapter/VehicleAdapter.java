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
import lk.leadco.ecogrid.model.Vehicle;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private List<Vehicle> vehicleList;
    private OnVehicleClickListener listener;

    public interface OnVehicleClickListener {
        void onVehicleClick(Vehicle vehicle);
        void onDeleteClick(Vehicle vehicle);
    }

    public VehicleAdapter(List<Vehicle> vehicleList, OnVehicleClickListener listener) {
        this.vehicleList = vehicleList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle_selector,
                parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicleList.get(position);
        holder.bind(vehicle, listener);
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPlate;
        ImageView imgVehicle, btnDelete;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPlate = itemView.findViewById(R.id.tvItemPlate);
            imgVehicle = itemView.findViewById(R.id.imgItemVehicle);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }

        void bind(Vehicle vehicle, OnVehicleClickListener listener) {
            tvName.setText(vehicle.getBrand() + " " + vehicle.getModel());
            tvPlate.setText(vehicle.getPlate_number());

            if ("EV CAR".equals(vehicle.getVehicle_type())) {
                imgVehicle.setImageResource(R.drawable.car);
            } else if ("EV BIKE".equals(vehicle.getVehicle_type())) {
                imgVehicle.setImageResource(R.drawable.scooter);
            } else if ("EV TUK-TUK".equals(vehicle.getVehicle_type())) {
                imgVehicle.setImageResource(R.drawable.tuktuk);
            } else if ("EV MINI SCOOTER".equals(vehicle.getVehicle_type())) {
                imgVehicle.setImageResource(R.drawable.mini_scooter);
            }

            itemView.setOnClickListener(v -> listener.onVehicleClick(vehicle));
            
            if (btnDelete != null) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> listener.onDeleteClick(vehicle));
            }
        }
    }
}