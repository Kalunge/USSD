package com.ussd.usddapp.dto;

import lombok.Data;

@Data
public class MobileMoneyRequest {
    private String terminalUserID = "8888";
    private String terminalID = "BKN52191100305";
    private String version = "1.1.12";
    private String countryID = "1";
    private String terminalUser = "brian";
    private String apiKey;
    private String type;
    private String account = "1135101116";
    private String studentRef = "ss001";
    private String studentName = "CHEPTOO KAREN";
    private String phoneNo;
    private String billAmount;
    private String amount;
}