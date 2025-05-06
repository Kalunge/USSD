package com.ussd.usddapp.dto;

import lombok.Data;

@Data
public class DepositRequest {
    private String password; // To be replaced with 4-digit PIN
    private String account1;
    private String accountName;
    private double amount;
    private String terminalUserID;
    private String terminalID;
    private String version;
    private long requestTime;
    private String narration;
    private String countryID;
    private String customerName;
    private String location;
    private String startTime;
    private String type = "cash_deposit_cash_absent";
    private String apiKey;
}