package com.ussd.usddapp.dto;


import lombok.*;

@Data
public class MobileMoneyWithdrawalResponse {
    private String tnxCode;
    private String agentName;
    private String date;
    private String status;
    private String account;
    private String agentId;
    private String location;
    private String shopName;
    private String name;
    private String bankCode;
    private String message;
}

