package com.ussd.usddapp.dto;

import lombok.Data;

@Data
public class AccountValidationResponse {
    private String accountNumber;
    private String accountDetails;
    private String status;
    private String message;

}