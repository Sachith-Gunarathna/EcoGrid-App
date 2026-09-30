package lk.leadco.ecogrid.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCard {

    private String cardId;
    private String uId;
    private String brand;
    private String last4;
    private String expDate;
    private String pmId;
    private String cardHolderName;
    private Date created_at;
    private boolean defaultCard;

}
