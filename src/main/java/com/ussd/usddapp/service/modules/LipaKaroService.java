package com.ussd.usddapp.service.modules;

import com.fasterxml.jackson.databind.*;
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
public class LipaKaroService {

    private final MobileMoneyApi mobileMoneyApi;
    private final MobileMoneyValidationApi mobileMoneyValidationApi;
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    public String handleLipaKaroSelection(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                session.setTransactionType("lipa_karo");
                session.setState(UssdSession.State.ENTER_RECIPIENT_ACCOUNT);
                return "CON Enter Recipient Account Number";
            }
            return "END Invalid option. Session ended.";
        }
        return "CON Lipa Karo\n1. Proceed";
    }

    public String handleRecipientAccountEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String account = inputParts[inputParts.length - 1];
            if (account.matches("\\d+")) {
                session.setRecipientAccount(account);
                session.setState(UssdSession.State.ENTER_ADMISSION_NUMBER);
                return "CON Enter Admission Number";
            }
            return "END Invalid account number. Session ended.";
        }
        return "END No account number provided. Session ended.";
    }

    public String handleAdmissionNumberEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String admissionNumber = inputParts[inputParts.length - 1];
            if (admissionNumber.matches("[A-Za-z0-9]+")) {
                session.setAdmissionNumber(admissionNumber);
                session.setState(UssdSession.State.ENTER_DEPOSITED_BY);
                return "CON Enter Depositor Name";
            }
            return "END Invalid admission number. Session ended.";
        }
        return "END No admission number provided. Session ended.";
    }

    public String handleDepositedByEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            String depositedBy = inputParts[inputParts.length - 1];
            if (depositedBy.matches("[A-Za-z\\s]+")) {
                session.setDepositedBy(depositedBy);
                session.setState(UssdSession.State.ENTER_MOBILE_AMOUNT);
                return "CON Enter Amount";
            }
            return "END Invalid depositor name. Session ended.";
        }
        return "END No depositor name provided. Session ended.";
    }

    public String handleLipaKaroAmountEntry(UssdSession session, String[] inputParts) {
        if (inputParts.length > 1) {
            try {
                double amount = Double.parseDouble(inputParts[inputParts.length - 1]);
                session.setAmount(amount);
                session.setState(UssdSession.State.CONFIRM_LIPA_KARO);
                return String.format(
                        "CON Confirm Lipa Karo Payment\nAccount: %s\nAdmission: %s\nDepositor: %s\nAmount: %.2f\n1. Confirm\n2. Cancel",
                        session.getRecipientAccount(), session.getAdmissionNumber(), session.getDepositedBy(), amount
                );
            } catch (NumberFormatException e) {
                return "END Invalid amount. Session ended.";
            }
        }
        return "END No amount provided. Session ended.";
    }

    public String handleLipaKaroConfirmation(UssdSession session, String[] inputParts) {
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

    public String handleLipaKaroPinEntry(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String pin = inputParts[inputParts.length - 1];
            if (pin.matches("\\d{4}")) {
                session.setPin(pin);

                // Assuming Lipa Karo uses a similar API call to Mobile Money Deposit
                MobileMoneyDepositRequest requestDto = new MobileMoneyDepositRequest();
                requestDto.setApiKey(apiKey);
                requestDto.setPhoneNo(session.getMobilePhone() != null ? session.getMobilePhone() : "N/A");
                requestDto.setAmount(session.getAmount());
                requestDto.setProviderId("LIPA_KARO");
                requestDto.setOtp("");
                requestDto.setType("lipa_karo_payment");

                MobileMoneyDepositResponse responseDto = mobileMoneyApi.performMobileMoneyDeposit(requestDto);
                if ("0".equals(responseDto.getStatus())) {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionDate(responseDto.getDate() != null ?
                            LocalDateTime.parse(responseDto.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                            LocalDateTime.now());
                    transaction.setDepositorName(session.getDepositedBy() != null ? session.getDepositedBy() : "Unknown");
                    transaction.setTransactionType("lipa_karo");
                    transaction.setAmount(session.getAmount());
                    transaction.setMobileNumber(session.getMobilePhone() != null ? session.getMobilePhone() : "N/A");
                    transaction.setTransactionNumber(responseDto.getTnxCode());
                    transaction.setSigned(true);
                    transactionRepository.save(transaction);

                    return String.format(
                            "END Lipa Karo Payment successful.\nTnxCode: %s\nAccount: %s\nAdmission: %s\nDepositor: %s\nAmount: %.2f",
                            responseDto.getTnxCode(), session.getRecipientAccount(), session.getAdmissionNumber(), session.getDepositedBy(), session.getAmount()
                    );
                } else if ("104".equals(responseDto.getStatus())) {
                    return "END Transaction failed: " + responseDto.getMessage() + "\nPlease contact support.";
                }
                return "END Lipa Karo Payment failed. Status: " + responseDto.getStatus();
            }
            return "END Invalid PIN. Must be 4 digits. Session ended.";
        }
        return "END No PIN provided. Session ended.";
    }
}