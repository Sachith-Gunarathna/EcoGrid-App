package lk.leadco.ecogrid.model;

import com.google.firebase.firestore.FieldValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private String uid;
    private double amount;
    private String customerToken;
    private String status;
    private String orderId;
    private FieldValue timestamp;
    private String stationId;
    private String targetPercentage;


}
