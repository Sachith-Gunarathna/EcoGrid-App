package lk.leadco.ecogrid.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {


    private String uid;
    private String name;
    private String email;
    private String otp;
    private String phoneNumber;
    private long otpTime;
    private boolean isVerified;
    private String profilePicUrl;
    private String profileBannerUrl;


}
