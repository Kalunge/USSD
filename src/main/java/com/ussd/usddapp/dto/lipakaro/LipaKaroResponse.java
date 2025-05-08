package com.ussd.usddapp.dto.lipakaro;


import lombok.*;

@Data
public class LipaKaroResponse {
    private String tnxCode;
    private String agentName;
    private String date;
    private String status;
    private String agentId;
    private String location;
    private String shopName;
    private double customerCharge;
    private double balance;
    private String message;
}
