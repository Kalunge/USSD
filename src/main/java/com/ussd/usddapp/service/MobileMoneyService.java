package com.ussd.usddapp.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.repository.*;
import com.ussd.usddapp.request.*;
import lombok.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.*;
import java.time.format.*;

@Service
@RequiredArgsConstructor
public class MobileMoneyService {

    private final MobileMoneyApi mobileMoneyApi;
    private final MobileMoneyValidationApi mobileMoneyValidationApi;
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    public String handleMobileMoneyOption(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setMobileTransactionType("deposit");
                session.setState(UssdSession.State.SELECT_TELCO);
                return "CON Select Telco:\n1. Airtel\n2. Telkom";
            } else if ("2".equals(choice)) {
                session.setMobileTransactionType("withdraw");
                session.setState(UssdSession.State.SELECT_TELCO);
                return "CON Select Telco:\n1. Airtel\n2. Telkom";
            }
            return "END Invalid option. Session ended.";
        }
        return "CON Mobile Money Options:\n1. Deposit\n2. Withdraw";
    }

    public String handleTelcoSelection(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setTelco("AIRTEL");
            } else if ("2".equals(choice)) {
                session.setTelco("TELKOM");
            } else {
                return "END Invalid telco option. Session ended.";
            }
            session.setState(UssdSession.State.ENTER_PHONE);
            return "CON Enter Phone Number";
        }
        return "CON Select Telco:\n1. Airtel\n2. Telkom";
    }

    public String handlePhoneEntry(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String phone = inputParts[inputParts.length - 1];
            if (phone.matches("\\d{10,12}")) {
                session.setMobilePhone(phone);

                MobileMoneyValidationRequest validationRequest = new MobileMoneyValidationRequest();
                validationRequest.setApiKey(apiKey);
                validationRequest.setPhoneNo(phone);
                MobileMoneyValidationResponse validationResponse = mobileMoneyValidationApi.validateMobileAccount(validationRequest);

                if ("00".equals(validationResponse.getStatus())) {
                    session.setAccountName(validationResponse.getAccountName());
                    if ("deposit".equals(session.getMobileTransactionType())) {
                        session.setState(UssdSession.State.ENTER_MOBILE_AMOUNT);
                        return "CON Enter Amount";
                    } else if ("withdraw".equals(session.getMobileTransactionType())) {
                        session.setState(UssdSession.State.ENTER_WITHDRAW_AMOUNT);
                        return "CON Enter Amount";
                    }
                } else {
                    return "END Invalid account. Status: " + validationResponse.getStatus();
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
                        "CON Confirm Mobile Money Deposit\nTelco: %s\nPhone: %s\nAccount Name: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                        session.getTelco(), session.getMobilePhone(), session.getAccountName(), amount
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

                MobileMoneyDepositRequest requestDto = new MobileMoneyDepositRequest();
                requestDto.setApiKey(apiKey);
                requestDto.setPhoneNo(session.getMobilePhone());
                requestDto.setAmount(session.getAmount());
                requestDto.setProviderId(session.getTelco());
                requestDto.setOtp("");
                requestDto.setType("hustler_cash_withdrawal");

                MobileMoneyDepositResponse responseDto = mobileMoneyApi.performMobileMoneyDeposit(requestDto);
                if ("0".equals(responseDto.getStatus())) {

                    Transaction transaction = new Transaction();
                    transaction.setTransactionDate(responseDto.getDate() != null ?
                            LocalDateTime.parse(responseDto.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                            LocalDateTime.now());
                    transaction.setDepositorName(session.getAccountName() != null ? session.getAccountName() : "Unknown");
                    //TODO add prompt for depositor Id transaction.setDepositorId(session.getDepositorId() != null ? session.getDepositorId() : "N/A");
                    transaction.setTransactionType("deposit");
                    transaction.setAmount(session.getAmount());
                    transaction.setMobileNumber(session.getMobilePhone());
                    transaction.setTransactionNumber(responseDto.getTnxCode());
                    transaction.setSigned(true);
                    transactionRepository.save(transaction);

                    return String.format(
                            "END Mobile Money Deposit successful.\nTelco: %s\nTnxCode: %s\nAccount: %s\nAccount Name: %s\nBalance: %.2f",
                            session.getTelco(), responseDto.getTnxCode(), responseDto.getAccountNo(), responseDto.getAccName(), session.getAmount() - 0.01
                    );
                } else if ("104".equals(responseDto.getStatus())) {
                    return "END Transaction failed: " + responseDto.getMessage() + "\nPlease contact support.";
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
                        "CON Confirm Mobile Money Withdrawal\nTelco: %s\nPhone: %s\nAccount Name: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                        session.getTelco(), session.getMobilePhone(), session.getAccountName(), amount
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

                MobileMoneyWithdrawalRequest requestDto = new MobileMoneyWithdrawalRequest();
                requestDto.setApiKey(apiKey);
                requestDto.setPhoneNo(session.getMobilePhone());
                requestDto.setAmount(session.getAmount());
                requestDto.setProviderId(session.getTelco());
                requestDto.setDepositorPhoneNo(session.getMobilePhone());
                requestDto.setDepositorName(session.getAccountName());
                requestDto.setCustomerName(session.getAccountName());
                requestDto.setReqID(System.currentTimeMillis() / 1000);
                requestDto.setRequestTime(System.currentTimeMillis());
                requestDto.setType("hustler_cash_deposit");

                MobileMoneyWithdrawalResponse responseDto = mobileMoneyApi.performMobileMoneyWithdrawal(requestDto);
                if ("0".equals(responseDto.getStatus())) {

                    Transaction transaction = new Transaction();
                    transaction.setTransactionDate(responseDto.getDate() != null ?
                            LocalDateTime.parse(responseDto.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                            LocalDateTime.now());
                    transaction.setDepositorName(session.getAccountName() != null ? session.getAccountName() : "Unknown");
                    // TODO prompt for depositor Id
                    //transaction.setDepositorId(session.getDepositorId() != null ? session.getDepositorId() : "N/A");
                    transaction.setTransactionType("withdraw");
                    transaction.setAmount(session.getAmount());
                    transaction.setMobileNumber(session.getMobilePhone());
                    transaction.setTransactionNumber(responseDto.getTnxCode());
                    transaction.setSigned(true);
                    transactionRepository.save(transaction);
                    return String.format(
                            "END Mobile Money Withdrawal successful.\nTelco: %s\nTnxCode: %s\nAccount: %s\nName: %s\nBalance: %.2f",
                            session.getTelco(), responseDto.getTnxCode(), responseDto.getAccount(), responseDto.getName(), session.getAmount() - 0.01
                    );
                } else if ("104".equals(responseDto.getStatus())) {
                    return "END Transaction failed: " + responseDto.getMessage() + "\nPlease contact support.";
                }
                return "END Mobile Money Withdrawal failed. Status: " + responseDto.getStatus();
            }
            return "END Invalid PIN. Must be 4 digits. Session ended.";
        }
        return "END No PIN provided. Session ended.";
    }
}