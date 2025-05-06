package com.ussd.usddapp.controler;


import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.request.*;
import jakarta.servlet.http.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ussd")
@Slf4j
@RequiredArgsConstructor
public class UssdController {

    @Value("${api.key}")
    private String apiKey;

    private Map<String, UssdSession> sessions = new HashMap<>();
    private final AccountValidationApi accountValidationApi;
    private final DepositApi depositApi;

    @PostMapping
    public ResponseEntity<String> handleUssd(@RequestParam String sessionId,
                                             @RequestParam String phoneNumber,
                                             @RequestParam String text,
                                             HttpServletRequest request) {
        log.info("Received USSD request - sessionId: {}, phoneNumber: {}, text: {}", sessionId, phoneNumber, text);

        UssdSession session = sessions.computeIfAbsent(sessionId, k -> new UssdSession());
        String userInput = text.trim();
        String response;

        try {
            switch (session.getState()) {
                case INIT:
                    response = "CON Welcome to Deposit Service\n1. Deposit Money";
                    session.setState(UssdSession.State.MENU);
                    break;

                case MENU:
                    if ("1".equals(userInput)) {
                        response = "CON Enter Account Number";
                        session.setState(UssdSession.State.ENTER_ACCOUNT);
                    } else {
                        response = "END Invalid option. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_ACCOUNT:
                    session.setAccountNumber(userInput);
                    AccountValidationRequest validationRequest = new AccountValidationRequest();
                    validationRequest.setAccount(userInput);
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
                        response = "CON Account validated. Enter Amount";
                        session.setState(UssdSession.State.ENTER_AMOUNT);
                    } else {
                        response = "END Invalid account. Session ended.";
                        sessions.remove(sessionId);
                    }
                    break;

                case ENTER_AMOUNT:
                    session.setAmount(Double.parseDouble(userInput));
                    response = "CON Enter 4-digit PIN";
                    session.setState(UssdSession.State.ENTER_PIN);
                    break;

                case ENTER_PIN:
                    if (userInput.matches("\\d{4}")) {
                        session.setPin(userInput);
                        DepositRequest depositRequest = new DepositRequest();
                        depositRequest.setAccount1(session.getAccountNumber());
                        depositRequest.setAmount(session.getAmount());
                        depositRequest.setPassword(session.getPin());
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
                    break;

                default:
                    response = "END Invalid state. Session ended.";
                    sessions.remove(sessionId);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing USSD request: {}", e.getMessage(), e);
            response = "END An error occurred. Please try again later.";
            sessions.remove(sessionId);
        }

        return ResponseEntity.ok(response);
    }

    // Inner class to manage session state
    private static class UssdSession {
        enum State {INIT, MENU, ENTER_ACCOUNT, ENTER_AMOUNT, ENTER_PIN}

        private State state = State.INIT;
        private String accountNumber;
        private double amount;
        private String pin;

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
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
    }
}