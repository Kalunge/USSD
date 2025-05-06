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

    private static final String[] BANKS = {"KCB", "ABSA", "COOP"};

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
                    response = "CON Welcome to Deposit Service\nSelect Bank:\n" +
                            "1. KCB\n2. ABSA\n3. COOP";
                    session.setState(UssdSession.State.SELECT_BANK);
                    break;

                case SELECT_BANK:
                    if (inputParts.length > 0) {
                        String choice = inputParts[0];
                        int bankIndex;
                        try {
                            bankIndex = Integer.parseInt(choice) - 1;
                            if (bankIndex >= 0 && bankIndex < BANKS.length) {
                                session.setBank(BANKS[bankIndex]);
                                response = "CON Bank Selected: " + session.getBank() + "\n1. Deposit Money";
                                session.setState(UssdSession.State.MENU);
                            } else {
                                response = "END Invalid bank selection. Session ended.";
                                sessions.remove(sessionId);
                            }
                        } catch (NumberFormatException e) {
                            response = "END Invalid input. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No bank selected. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case MENU:
                    if (inputParts.length > 1 && "1".equals(inputParts[1])) {
                        response = "CON Enter Account Number";
                        session.setState(UssdSession.State.ENTER_ACCOUNT);
                    } else {
                        response = "END Invalid option. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_ACCOUNT:
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
                            response = "CON Enter Amount";
                            session.setState(UssdSession.State.ENTER_AMOUNT);
                        } else {
                            response = "END Invalid account. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No account number provided. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_AMOUNT:
                    if (inputParts.length > 1) {
                        try {
                            double amount = Double.parseDouble(inputParts[inputParts.length - 1]);
                            session.setAmount(amount);
                            AccountValidationResponse validationResponse = session.getAccountValidationResponse();
                            String accountName = validationResponse.getAccountDetails().split("\\^")[5]; // Extract name
                            response = String.format(
                                    "CON Confirm Transaction\nBank: %s\nAccount Number: %s\nAccount Name: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                                    session.getBank(), validationResponse.getAccountNumber(), accountName, amount
                            );
                            session.setState(UssdSession.State.CONFIRM_ACCOUNT);
                        } catch (NumberFormatException e) {
                            response = "END Invalid amount. Session ended.";
                            sessions.remove(sessionId);
                        }
                    } else {
                        response = "END No amount provided. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case CONFIRM_ACCOUNT:
                    if (inputParts.length > 1) {
                        String choice = inputParts[inputParts.length - 1];
                        if ("1".equals(choice)) {
                            response = "CON Enter 4-digit PIN";
                            session.setState(UssdSession.State.ENTER_PIN);
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

                case ENTER_PIN:
                    if (inputParts.length > 1) {
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
        enum State {INIT, SELECT_BANK, MENU, ENTER_ACCOUNT, ENTER_AMOUNT, CONFIRM_ACCOUNT, ENTER_PIN}

        private State state = State.INIT;
        private String bank;
        private String accountNumber;
        private double amount;
        private String pin;
        private AccountValidationResponse accountValidationResponse;

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public String getBank() {
            return bank;
        }

        public void setBank(String bank) {
            this.bank = bank;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public AccountValidationResponse getAccountValidationResponse() {
            return accountValidationResponse;
        }

        public void setAccountValidationResponse(AccountValidationResponse response) {
            this.accountValidationResponse = response;
        }
    }
}