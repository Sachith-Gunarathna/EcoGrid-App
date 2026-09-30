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
public class Invoice {

    private String invoiceId;
    private String transactionId;
    private double amount;
    private String date;
    private String uId;
    private String evStation_id;
    private String vehicle_id;
    private String createdAt;
    private String energy_kWh;

}
