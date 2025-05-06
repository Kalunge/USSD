package com.ussd.usddapp.controler;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/agent")
@Slf4j
@CrossOrigin
public class AgentWithdrawalController {

    private final Map<String, SessionData> sessionStore = new HashMap<>();

    @Data
    public static class SessionData {
        private String state = "START";
        private String phoneNumber;
        private double withdrawalAmount;
        private double balance;
    }

    @GetMapping("/ussd")
    public String handleUssd(
            @RequestParam("ORIG") String orig,
            @RequestParam("DEST") String dest,
            @RequestParam("SESSION_ID") String sessionId,
            @RequestParam(value = "USSD_PARAMS", defaultValue = "") String ussdParams) {

        //log.debug("Received USSD request: sessionId={}, orig={}, dest={}, ussdParams='{}'", sessionId, orig, dest, ussdParams);

        SessionData sessionData = sessionStore.computeIfAbsent(sessionId, k -> new SessionData());
        String state = sessionData.getState();
        String response;

        try {
            switch (state) {
                case "START" -> {
                    sessionData.setState("PHONE_NUMBER");
                    response = "CON Enter Agent Phone Number (e.g., 2547xxxxxxxx):";
                }
                case "PHONE_NUMBER" -> {
                    //  log.debug("Processing PHONE_NUMBER state with ussdParams='{}'", ussdParams);
                    if (ussdParams == null || ussdParams.trim().isEmpty()) {
                        // log.debug("ussdParams is null or empty after trimming");
                        response = "CON Please enter a valid phone number:";
                    } else {
                        String trimmedParams = ussdParams.trim();
                        // Validate phone number: must start with 2547 and be 12 digits long
                        if (!trimmedParams.matches("2547\\d{8}")) {
                            //  log.debug("Invalid phone number format: {}", trimmedParams);
                            response = "CON Please enter a valid Safaricom number (e.g., 2547xxxxxxxx):";
                        } else {
                            // Simulate checking if the agent exists
                            if (isValidAgent(trimmedParams)) {
                                sessionData.setPhoneNumber(trimmedParams);
                                sessionData.setBalance(getAgentBalance(trimmedParams)); // Simulated balance
                                sessionData.setState("MENU");
                                response = "CON Welcome Agent!\n1. Withdraw\n2. Check Balance\n0. Cancel";
                            } else {
                                sessionStore.remove(sessionId);
                                response = "END Agent not found. Please contact support.";
                            }
                        }
                    }
                }
                case "MENU" -> {
                    switch (ussdParams) {
                        case "1" -> {
                            sessionData.setState("WITHDRAW_AMOUNT");
                            response = "CON Enter Amount to Withdraw:";
                        }
                        case "2" -> {
                            double balance = sessionData.getBalance();
                            sessionStore.remove(sessionId);
                            response = String.format("END Your balance is %.2f KES.", balance);
                        }
                        case "0" -> {
                            sessionStore.remove(sessionId);
                            response = "END Session cancelled.";
                        }
                        default ->
                                response = "CON Invalid option. Please select:\n1. Withdraw\n2. Check Balance\n0. Cancel";
                    }
                }
                case "WITHDRAW_AMOUNT" -> {
                    log.debug("Processing WITHDRAW_AMOUNT state with ussdParams='{}'", ussdParams);
                    if (ussdParams == null || ussdParams.trim().isEmpty()) {
                        log.debug("ussdParams is null or empty after trimming");
                        response = "CON Please enter a valid amount:";
                    } else {
                        String trimmedParams = ussdParams.trim();
                        // Validate amount: must be a positive number
                        try {
                            double amount = Double.parseDouble(trimmedParams);
                            if (amount <= 0) {
                                response = "CON Amount must be greater than 0:";
                            } else {
                                double balance = sessionData.getBalance();
                                if (amount > balance) {
                                    response = String.format("CON Insufficient balance. Your balance is %.2f KES.\nEnter a lower amount:", balance);
                                } else {
                                    sessionData.setWithdrawalAmount(amount);
                                    sessionData.setState("CONFIRM_WITHDRAW");
                                    response = String.format("CON Withdraw %.2f KES\nRemaining Balance: %.2f KES\n1. Confirm\n0. Cancel", amount, balance - amount);
                                }
                            }
                        } catch (NumberFormatException e) {
                            // log.debug("Invalid amount format: {}", trimmedParams);
                            response = "CON Please enter a valid numeric amount:";
                        }
                    }
                }
                case "CONFIRM_WITHDRAW" -> {
                    if (ussdParams.equals("1")) {
                        sessionData.setState("PIN_AUTH");
                        response = "UPR Please enter your Service PIN to confirm withdrawal:";
                    } else if (ussdParams.equals("0")) {
                        sessionStore.remove(sessionId);
                        response = "END Withdrawal cancelled.";
                    } else {
                        response = "CON Invalid option. Please select:\n1. Confirm\n0. Cancel";
                    }
                }
                case "PIN_AUTH" -> {
                    //  log.debug("Processing PIN_AUTH state for phoneNumber={} and amount={}", sessionData.getPhoneNumber(), sessionData.getWithdrawalAmount());
                    double amount = sessionData.getWithdrawalAmount();
                    String phoneNumber = sessionData.getPhoneNumber();
                    double newBalance = sessionData.getBalance() - amount;
                    sessionStore.remove(sessionId);
                    response = String.format("END Withdrawal of %.2f KES successful for Agent %s.\nNew Balance: %.2f KES.", amount, phoneNumber, newBalance);
                }
                default -> {
                    // log.error("Invalid state: {}", state);
                    sessionStore.remove(sessionId);
                    response = "END An error occurred. Please try again.";
                }
            }
        } catch (Exception e) {
            // log.error("Error processing USSD request: {}", e.getMessage(), e);
            sessionStore.remove(sessionId);
            response = "END An error occurred. Please try again.";
        }

        // log.debug("Returning response: {}", response);
        return response;
    }

    // Simulated method to check if the agent exists
    private boolean isValidAgent(String phoneNumber) {
        // Simulate a few valid agent phone numbers
        return phoneNumber.equals("254799999999") || phoneNumber.equals("254712345678") || phoneNumber.equals("254713398918");
    }

    // Simulated method to get the agent's balance
    private double getAgentBalance(String phoneNumber) {
        // Simulate balance: 5000 KES for 254799999999, 3000 KES for 254712345678
        if (phoneNumber.equals("254799999999")) {
            return 5000.0;
        } else if (phoneNumber.equals("254712345678")) {
            return 3000.0;
        }
        return 1000.0;
    }
}
