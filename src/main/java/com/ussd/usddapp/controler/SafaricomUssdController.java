package com.ussd.usddapp.controler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/safaricom")
@Slf4j
@CrossOrigin
public class SafaricomUssdController {

    private final Map<String, SessionData> sessionStore = new HashMap<>();

    @Data
    public static class SessionData {
        private String state = "START";
        private String billNumber;
        private double amount;
    }

    @GetMapping("/ussd")
    public String handleUssd(
            @RequestParam("ORIG") String orig,
            @RequestParam("DEST") String dest,
            @RequestParam(value = "SESSION_ID") String sessionId,
            @RequestParam(value = "USSD_PARAMS", defaultValue = "") String ussdParams) {

        log.debug("Received USSD request: sessionId={}, orig={}, dest={}, ussdParams='{}'", sessionId, orig, dest, ussdParams);

        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.error("SESSION_ID is missing or empty");
            return "END Missing SESSION_ID parameter.";
        }

        SessionData sessionData = sessionStore.computeIfAbsent(sessionId, k -> new SessionData());
        String state = sessionData.getState();
        String response;

        try {
            if (state.equals("START")) {
                sessionData.setState("BILL_NUMBER");
                response = "CON Enter Bill Number:";
            } else if (state.equals("BILL_NUMBER")) {
                log.debug("Processing BILL_NUMBER state with ussdParams='{}'", ussdParams);
                if (ussdParams == null || ussdParams.trim().isEmpty()) {
                    log.debug("ussdParams is empty or null");
                    response = "CON Please enter a valid bill number:";
                } else {
                    // Validate bill number (e.g., must be numeric and at least 6 digits)
                    if (!ussdParams.matches("\\d{6,}")) {
                        log.debug("Invalid bill number format: {}", ussdParams);
                        response = "CON Please enter a valid bill number (at least 6 digits):";
                    } else {
                        sessionData.setBillNumber(ussdParams);
                        double amount = ussdParams.equals("123456") ? 1500.0 : 1000.0;
                        sessionData.setAmount(amount);
                        sessionData.setState("CONFIRM");
                        response = String.format("CON Bill Number: %s\nAmount: %.2f KES\n1. Pay\n0. Cancel", ussdParams, amount);
                        log.debug("Transitioning to CONFIRM state with billNumber={} and amount={}", ussdParams, amount);
                    }
                }
            } else if (state.equals("CONFIRM")) {
                if (ussdParams.equals("1")) {
                    sessionData.setState("PIN_AUTH");
                    response = "UPR Please enter your Service PIN to confirm payment:";
                } else if (ussdParams.equals("0")) {
                    sessionStore.remove(sessionId);
                    response = "END Payment cancelled.";
                } else {
                    response = "CON Invalid option. Please select:\n1. Pay\n0. Cancel";
                }
            } else if (state.equals("PIN_AUTH")) {
                double amount = sessionData.getAmount();
                String billNumber = sessionData.getBillNumber();
                sessionStore.remove(sessionId);
                response = String.format("END Payment of %.2f KES for Bill %s successful.", amount, billNumber);
            } else {
                log.error("Invalid state: {}", state);
                sessionStore.remove(sessionId);
                response = "END An error occurred. Please try again.";
            }
        } catch (Exception e) {
            log.error("Error processing USSD request: {}", e.getMessage(), e);
            sessionStore.remove(sessionId);
            response = "END An error occurred. Please try again.";
        }

        log.debug("Returning response: {}", response);
        return response;
    }
}
