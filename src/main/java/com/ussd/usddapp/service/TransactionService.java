package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.request.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountValidationApi accountValidationApi;

    public String handleMenu(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1 && "1".equals(inputParts[1])) {
            session.setState(UssdSession.State.ENTER_ACCOUNT);
            return "CON Enter Account Number";
        }
        return "END Invalid option. Session ended.";
    }

    public String handleAccountEntry(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String accountNumber = inputParts[inputParts.length - 1];
            session.setAccountNumber(accountNumber);

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

            AccountValidationResponse validationResponse = accountValidationApi.validateAccount(validationRequest);
            if ("0".equals(validationResponse.getStatus())) {
                session.setAccountValidationResponse(validationResponse);
                session.setState(UssdSession.State.ENTER_AMOUNT);
                return "CON Enter Amount";
            }
            return "END Invalid account. Session ended.";
        }
        return "END No account number provided. Session ended.";
    }

    public String handleAmountEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            try {
                double amount = Double.parseDouble(inputParts[inputParts.length - 1]);
                session.setAmount(amount);
                AccountValidationResponse validationResponse = session.getAccountValidationResponse();
                String accountName = validationResponse.getAccountDetails().split("\\^")[5];
                session.setState(UssdSession.State.CONFIRM_ACCOUNT);
                return String.format(
                        "CON Confirm Transaction\nBank: %s\nAccount Number: %s\nAccount Name: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                        session.getBank(), validationResponse.getAccountNumber(), accountName, amount
                );
            } catch (NumberFormatException e) {
                return "END Invalid amount. Session ended.";
            }
        }
        return "END No amount provided. Session ended.";
    }

    public String handleConfirmation(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setState(UssdSession.State.ENTER_PIN);
                return "CON Enter 4-digit PIN";
            } else if ("2".equals(choice)) {
                return "END Transaction canceled.";
            }
        }
        return "END Invalid option. Session ended.";
    }
}