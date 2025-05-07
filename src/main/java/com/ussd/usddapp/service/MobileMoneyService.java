package com.ussd.usddapp.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.request.*;
import lombok.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MobileMoneyService {

    private final MobileMoneyApi mobileMoneyApi;

    public String handleMobileMoneyOption(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setMobileTransactionType("deposit");
                session.setState(UssdSession.State.ENTER_PHONE);
                return "CON Enter Phone Number";
            } else if ("2".equals(choice)) {
                session.setMobileTransactionType("withdraw");
                session.setState(UssdSession.State.ENTER_PHONE);
                return "CON Enter Phone Number";
            }
            return "END Invalid option. Session ended.";
        }
        return "CON Mobile Money Options:\n1. Deposit\n2. Withdraw";
    }

    public String handlePhoneEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String phone = inputParts[inputParts.length - 1];
            if (phone.matches("\\d{10,12}")) {
                session.setMobilePhone(phone);
                if ("deposit".equals(session.getMobileTransactionType())) {
                    session.setState(UssdSession.State.ENTER_MOBILE_AMOUNT);
                    return "CON Enter Amount";
                } else if ("withdraw".equals(session.getMobileTransactionType())) {
                    session.setState(UssdSession.State.ENTER_WITHDRAW_AMOUNT);
                    return "CON Enter Amount";
                }
            }
            return "END Invalid phone number. Session ended.";
        }
        return "END No phone number provided. Session ended.";
    }

    public String handleMobileAmountEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            try {
                double amount = Double.parseDouble(inputParts[inputParts.length - 1]);
                session.setAmount(amount);
                session.setState(UssdSession.State.CONFIRM_MOBILE);
                return String.format(
                        "CON Confirm Mobile Money Deposit\nPhone: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                        session.getMobilePhone(), amount
                );
            } catch (NumberFormatException e) {
                return "END Invalid amount. Session ended.";
            }
        }
        return "END No amount provided. Session ended.";
    }

    public String handleMobileConfirmation(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setState(UssdSession.State.ENTER_MOBILE_PIN);
                return "CON Enter 4-digit PIN";
            } else if ("2".equals(choice)) {
                return "END Transaction canceled.";
            }
        }
        return "END Invalid option. Session ended.";
    }

    public String handleMobilePinEntry(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String pin = inputParts[inputParts.length - 1];
            if (pin.matches("\\d{4}")) {
                session.setPin(pin);

                MobileMoneyRequest requestDto = new MobileMoneyRequest();
                requestDto.setApiKey(apiKey);
                requestDto.setType("lipa_karo_notification");
                requestDto.setPhoneNo(session.getMobilePhone());
                requestDto.setBillAmount(String.format("%.2f", session.getAmount()));

                MobileMoneyResponse responseDto = mobileMoneyApi.performMobileMoneyOperation(requestDto);
                if ("0".equals(responseDto.getStatus())) {
                    return String.format(
                            "END Mobile Money Deposit successful.\nTnxCode: %s\nAgent: %s\nBalance: %.2f",
                            responseDto.getTnxCode(), responseDto.getAgentName(), responseDto.getBalance()
                    );
                }
                return "END Mobile Money Deposit failed. Status: " + responseDto.getStatus();
            }
            return "END Invalid PIN. Must be 4 digits. Session ended.";
        }
        return "END No PIN provided. Session ended.";
    }

    public String handleWithdrawAmountEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            try {
                double amount = Double.parseDouble(inputParts[inputParts.length - 1]);
                session.setAmount(amount);
                session.setState(UssdSession.State.CONFIRM_WITHDRAW);
                return String.format(
                        "CON Confirm Mobile Money Withdrawal\nPhone: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                        session.getMobilePhone(), amount
                );
            } catch (NumberFormatException e) {
                return "END Invalid amount. Session ended.";
            }
        }
        return "END No amount provided. Session ended.";
    }

    public String handleWithdrawConfirmation(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setState(UssdSession.State.ENTER_WITHDRAW_PIN);
                return "CON Enter 4-digit PIN";
            } else if ("2".equals(choice)) {
                return "END Transaction canceled.";
            }
        }
        return "END Invalid option. Session ended.";
    }

    public String handleWithdrawPinEntry(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String pin = inputParts[inputParts.length - 1];
            if (pin.matches("\\d{4}")) {
                session.setPin(pin);

                MobileMoneyRequest requestDto = new MobileMoneyRequest();
                requestDto.setApiKey(apiKey);
                requestDto.setType("withdraw_notification");
                requestDto.setPhoneNo(session.getMobilePhone());
                requestDto.setBillAmount(String.format("%.2f", session.getAmount()));

                MobileMoneyResponse responseDto = mobileMoneyApi.performMobileMoneyOperation(requestDto);
                if ("0".equals(responseDto.getStatus())) {
                    return String.format(
                            "END Mobile Money Withdrawal successful.\nTnxCode: %s\nAgent: %s\nBalance: %.2f",
                            responseDto.getTnxCode(), responseDto.getAgentName(), responseDto.getBalance()
                    );
                }
                return "END Mobile Money Withdrawal failed. Status: " + responseDto.getStatus();
            }
            return "END Invalid PIN. Must be 4 digits. Session ended.";
        }
        return "END No PIN provided. Session ended.";
    }
}