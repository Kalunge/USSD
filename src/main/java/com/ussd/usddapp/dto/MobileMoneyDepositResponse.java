package com.ussd.usddapp.dto;

import lombok.*;

@Data
public class MobileMoneyDepositResponse {
    private String tnxCode;
    private String agentName;
    private String date;
    private String status;
    private String account;
    private String agentId;
    private String location;
    private String shopName;
    private String bankCode;
    private String accountNo;
    private String accName;
    private String message;
}
