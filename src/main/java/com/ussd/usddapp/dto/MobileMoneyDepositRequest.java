package com.ussd.usddapp.dto;

import lombok.Data;

@Data
public class MobileMoneyDepositRequest {
    private String terminalUserID = "8888";
    private String terminalID = "BKN52191100305";
    private String version = "1.1.12";
    private String countryID = "1";
    private String terminalUser = "brian";
    private String phoneNo;
    private double amount;
    private String providerId;
    private String location = "";
    private String otp = "";
    private String type = "hustler_cash_withdrawal";
    private String apiKey;
}


