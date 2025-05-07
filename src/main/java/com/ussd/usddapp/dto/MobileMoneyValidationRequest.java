package com.ussd.usddapp.dto;

import lombok.*;

@Data
public class MobileMoneyValidationRequest {
    private String terminalUserID = "8888";
    private String terminalID = "BKN52191100305";
    private String apiKey;
    private String countryID = "1";
    private String bankCode = "01";
    private String phoneNo;
    private String providerId = "AIRTEL";
    private String location = "";
    private String type = "hustler_account_validation";
}


