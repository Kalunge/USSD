package com.ussd.usddapp.dto.lipakaro;

import lombok.*;

@Data
public class LipaKaroValidationResponse {
    private String account;
    private String studentName;
    private String schoolName;
    private String status;
    private String paymentMode;
    private String billAmount;
    private String message;
}