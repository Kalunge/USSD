package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.request.*;
import lombok.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final AccountValidationApi accountValidationApi;

    public AccountValidationResponse validateAccount(UssdSession session, String accountNumber, String apiKey) throws IOException {
        AccountValidationRequest validationRequest = new AccountValidationRequest();
        validationRequest.setAccount(accountNumber);
        validationRequest.setApiKey(apiKey);
        validationRequest.setBankCode("01");
        validationRequest.setTerminalID("BKN52191100305");
        validationRequest.setType("acc_validation");
        validationRequest.setVersion("1.1.10");
        validationRequest.setCountryID("1");
        validationRequest.setTerminalUserID("123678");
        validationRequest.setLocation("");
        validationRequest.setMerchantID("20");

        AccountValidationResponse response = accountValidationApi.validateAccount(validationRequest);
        if ("0".equals(response.getStatus())) {
            session.setAccountValidationResponse(response);
        }
        return response;
    }
}