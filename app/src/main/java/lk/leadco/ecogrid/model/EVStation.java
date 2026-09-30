package lk.leadco.ecogrid.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EVStation {

    private BasicInfo basicInfo;
    private TechSpecs techSpecs;
    private Financial financial;
    private Verification verification;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfo{
        private String name;
        private String address;
        private double latitude;
        private double longitude;
        private String owner_id;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechSpecs{
        private String hardware_id;
        private String connection_type;
        private double power_output_kw;
        private int ports;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Financial{
        private double price_pre_kwh;
        private String payhere_merchant_id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Verification{
        private boolean is_verified;
        private String photo_url;
    }
}
