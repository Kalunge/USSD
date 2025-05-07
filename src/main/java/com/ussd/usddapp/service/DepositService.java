package com.ussd.usddapp.service;


import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.request.*;
import lombok.*;
import org.springframework.stereotype.*;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositApi depositApi;

    public String handlePinEntry(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String pin = inputParts[inputParts.length - 1];
            if (pin.matches("\\d{4}")) {
                session.setPin(pin);

                DepositRequest depositRequest = new DepositRequest();
                depositRequest.setAccount1(session.getAccountNumber());
                depositRequest.setAmount(session.getAmount());
                depositRequest.setPassword(pin);
                depositRequest.setApiKey(apiKey);
                depositRequest.setAccountName("User Name");
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
                    return "END Deposit successful. TnxCode: " + depositResponse.getTnxCode();
                }
                return "END Deposit failed. Status: " + depositResponse.getStatus();
            }
            return "END Invalid PIN. Must be 4 digits. Session ended.";
        }
        return "END No PIN provided. Session ended.";
    }
}