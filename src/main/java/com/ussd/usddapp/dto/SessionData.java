package com.ussd.usddapp.dto;

import lombok.Data;

@Data
public class SessionData {
    private String state = "START";
    private String accountNumber;
    private String accountName;
    private double amount;
    private String bankCode = "01";
    private String terminalID = "BKN52191100305";
    private String terminalUserID = "123678";
    private String merchantID = "20";
    private String description;
    private String nameId;
}