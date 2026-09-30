package lk.leadco.ecogrid.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private String vehicle_id;
    private String brand;
    private String model;
    private String plate_number;
    private int battery_capacity_kwh;
    private String user_id;
    private Date added_date;
    private String vehicle_type;
    private boolean isActive;
}
