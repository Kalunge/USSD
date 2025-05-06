package com.ussd.usddapp.controler;


import com.ussd.usddapp.dto.*;

import com.ussd.usddapp.request.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


import java.io.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UssdController {
    private final AccountValidationApi accountValidationApi;
    private final DepositApi depositApi;

    @Value("${api.key}")
    private String apiKey;

    private Map<String, UssdSession> sessions = new HashMap<>();

    @PostMapping(value = "/ussd", consumes = "application/x-www-form-urlencoded", produces = "text/plain")
    public @ResponseBody String handleUssd(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        log.info("Received USSD request - sessionId: {}, phoneNumber: {}, text: {}", sessionId, phoneNumber, text);

        UssdSession session = sessions.computeIfAbsent(sessionId, k -> new UssdSession());
        String userInput = text.trim().isEmpty() ? "" : text;
        String[] inputParts = userInput.split("\\*");
        String response;

        try {
            switch (session.getState()) {
                case INIT:
                    response = "CON Welcome to Deposit Service\n1. Deposit Money";
                    session.setState(UssdSession.State.MENU);
                    break;

                case MENU:
                    if (inputParts.length > 0 && "1".equals(inputParts[0])) {
                        response = "CON Enter Account Number";
                        session.setState(UssdSession.State.ENTER_ACCOUNT);
                    } else {
                        response = "END Invalid option. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_ACCOUNT:
                    if (inputParts.length > 0) {
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
                            String accountName = validationResponse.getAccountDetails().split("\\^")[5]; // Extract name from accountDetails
                            response = String.format(
                                    "CON Confirm Account Details\nBank: KCB\nAccount Number: %s\nAccount Name: %s\n1. Confirm\n2. Cancel",
                                    validationResponse.getAccountNumber(), accountName
                            );
                            session.setState(UssdSession.State.CONFIRM_ACCOUNT);
                        } else {
                            response = "END Invalid account. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No account number provided. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case CONFIRM_ACCOUNT:
                    if (inputParts.length > 0) {
                        String choice = inputParts[inputParts.length - 1];
                        if ("1".equals(choice)) {
                            response = "CON Enter Amount";
                            session.setState(UssdSession.State.ENTER_AMOUNT);
                        } else if ("2".equals(choice)) {
                            response = "END Transaction canceled.";
                            sessions.remove(sessionId);
                        } else {
                            response = "END Invalid option. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No option selected. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_AMOUNT:
                    if (inputParts.length > 0) {
                        try {
                            double amount = Double.parseDouble(inputParts[inputParts.length - 1]);
                            session.setAmount(amount);
                            response = "CON Enter 4-digit PIN";
                            session.setState(UssdSession.State.ENTER_PIN);
                        } catch (NumberFormatException e) {
                            response = "END Invalid amount. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No amount provided. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_PIN:
                    if (inputParts.length > 0) {
                        String pin = inputParts[inputParts.length - 1];
                        if (pin.matches("\\d{4}")) {
                            session.setPin(pin);

                            DepositRequest depositRequest = new DepositRequest();
                            depositRequest.setAccount1(session.getAccountNumber());
                            depositRequest.setAmount(session.getAmount());
                            depositRequest.setPassword(pin);
                            depositRequest.setApiKey(apiKey);
                            depositRequest.setAccountName("User Name"); // Replace with actual name if available
                            depositRequest.setTerminalUserID("323");
                            depositRequest.setTerminalID("BKN52191100305");
                            depositRequest.setVersion("1.1.10.");
                            depositRequest.setRequestTime(System.currentTimeMillis());
                            depositRequest.setNarration("USSD Deposit");
                            depositRequest.setCountryID("2");
                            depositRequest.setCustomerName("Test User");
                            depositRequest.setLocation("");
                            depositRequest.setStartTime(java.time.LocalDateTime.now().toString());
                            depositRequest.setType("cash_deposit_cash_absent");

                            DepositResponse depositResponse = depositApi.performDeposit(depositRequest);
                            if ("0".equals(depositResponse.getStatus())) {
                                response = "END Deposit successful. TnxCode: " + depositResponse.getTnxCode();
                            } else {
                                response = "END Deposit failed. Status: " + depositResponse.getStatus();
                            }
                            sessions.remove(sessionId);
                        } else {
                            response = "END Invalid PIN. Must be 4 digits. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No PIN provided. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                default:
                    response = "END Invalid state. Session ended.";
                    sessions.remove(sessionId);
                    break;
            }
        } catch (IOException e) {
            log.error("Error processing USSD request: {}", e.getMessage(), e);
            response = "END An error occurred. Please try again later.";
            sessions.remove(sessionId);
        }

        log.info("Sending USSD response: {}", response);
        return response;
    }

    // Inner class to manage session state
    private static class UssdSession {
        enum State { INIT, MENU, ENTER_ACCOUNT, CONFIRM_ACCOUNT, ENTER_AMOUNT, ENTER_PIN }

        private State state = State.INIT;
        private String accountNumber;
        private double amount;
        private String pin;
        private AccountValidationResponse accountValidationResponse;

        public State getState() { return state; }
        public void setState(State state) { this.state = state; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
        public AccountValidationResponse getAccountValidationResponse() { return accountValidationResponse; }
        public void setAccountValidationResponse(AccountValidationResponse response) { this.accountValidationResponse = response; }
    }
}