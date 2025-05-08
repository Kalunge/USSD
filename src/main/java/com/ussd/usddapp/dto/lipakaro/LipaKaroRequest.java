package com.ussd.usddapp.dto.lipakaro;

import lombok.Data;

@Data
public class LipaKaroRequest {
    private String terminalUserID;
    private String terminalID;
    private String version;
    private String countryID;
    private String terminalUser;
    private String apiKey;
    private String type;
    private String account;
    private String studentRef;
    private String studentName;
    private String phoneNo;
    private String billAmount;
    private String amount;
}