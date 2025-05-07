package com.ussd.usddapp.dto;


import lombok.*;

@Data
public class MobileMoneyWithdrawalRequest {
    private String apiKey;
    private String countryID = "1";
    private String bankCode = "01";
    private String depositorPhoneNo;
    private String depositorName;
    private String phoneNo;
    private String terminalUserID = "12345678";
    private double amount;
    private long reqID;
    private String location = "";
    private String version = "1.1.10";
    private String mposSerial = "BKN52191100305";
    private String terminalID = "BKN52191100305";
    private String type = "hustler_cash_deposit";
    private long requestTime;
    private String customerName;
    private String providerId = "AIRTEL";
    private String message;

}
