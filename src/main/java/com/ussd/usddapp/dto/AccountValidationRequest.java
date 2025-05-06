package com.ussd.usddapp.dto;

import lombok.*;

@Data
public class AccountValidationRequest {
    private String bankCode;
    private String terminalID;
    private String type = "acc_validation";
    private String version;
    private String countryID;
    private String apiKey;
    private String terminalUserID;
    private String location;
    private String merchantID;
    private String account;
}