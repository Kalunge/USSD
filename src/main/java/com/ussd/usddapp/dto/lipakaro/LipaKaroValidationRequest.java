package com.ussd.usddapp.dto.lipakaro;

import lombok.*;

@Data
public class LipaKaroValidationRequest {
    private String appID;
    private String type;
    private String version;
    private String terminalID;
    private String terminalUser;
    private String countryID;
    private String apiKey;
    private String terminalUserID;
    private String location;
    private String account;
    private String studentRef;
}