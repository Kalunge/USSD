package com.ussd.usddapp.dto;


import lombok.Data;

@Data
public class MobileMoneyResponse {
    private String tnxCode;
    private String agentName;
    private String date;
    private String status;
    private String agentId;
    private String location;
    private String shopName;
    private int customerCharge;
    private double balance;
    private String message;
}