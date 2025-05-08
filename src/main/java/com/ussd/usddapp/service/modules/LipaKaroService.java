package com.ussd.usddapp.service.modules;

import com.fasterxml.jackson.databind.*;
import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.dto.lipakaro.*;
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

    private final LipaKaroApi lipaKaroApi;
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

    public String handleLipaKaroConfirmation(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        if (inputParts.length > 1) {
            String choice = inputParts[inputParts.length - 1];
            if ("1".equals(choice)) {
                // Perform validation before proceeding
                LipaKaroValidationRequest validationRequest = new LipaKaroValidationRequest();
                validationRequest.setAppID("39"); // Example value, adjust as needed
                validationRequest.setType("lipa_karo_validation");
                validationRequest.setVersion("1.1.16"); // Example value
                validationRequest.setTerminalID("BKN52191100307"); // Example value
                validationRequest.setTerminalUser("1111"); // Example value
                validationRequest.setCountryID("1"); // Example value
                validationRequest.setApiKey(apiKey);
                validationRequest.setTerminalUserID("1111"); // Example value
                validationRequest.setLocation("yaya center nairobi"); // Example value
                validationRequest.setAccount(session.getRecipientAccount());
                validationRequest.setStudentRef(session.getAdmissionNumber());

                LipaKaroValidationResponse validationResponse = lipaKaroApi.validateLipaKaroAccount(validationRequest);

                if ("0".equals(validationResponse.getStatus())) {
                    double billAmount = Double.parseDouble(validationResponse.getBillAmount());
                    if (session.getAmount() <= billAmount) {
                        session.setAccountName(validationResponse.getStudentName()); // Store student name
                        session.setState(UssdSession.State.ENTER_MOBILE_PIN);
                        return String.format(
                                "CON Validation Successful\nAccount: %s\nStudent: %s\nSchool: %s\nBill Amount: %.2f\nYour Amount: %.2f\nEnter 4-digit PIN",
                                validationResponse.getAccount(),
                                validationResponse.getStudentName(),
                                validationResponse.getSchoolName(),
                                billAmount,
                                session.getAmount()
                        );
                    } else {
                        return "END Amount exceeds bill amount (%.2f). Session ended.";
                    }
                } else {
                    return "END Invalid account or student reference. Status: " + validationResponse.getStatus();
                }
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

                LipaKaroRequest request = new LipaKaroRequest();
                request.setTerminalUserID("8888"); // Example value, adjust as needed
                request.setTerminalID("BKN52191100305"); // Example value, adjust as needed
                request.setVersion("1.1.12"); // Example value, adjust as needed
                request.setCountryID("1"); // Example value, adjust as needed
                request.setTerminalUser("brian"); // Example value, adjust as needed
                request.setApiKey(apiKey);
                request.setType("lipa_karo_notification");
                request.setAccount(session.getRecipientAccount());
                request.setStudentRef(session.getAdmissionNumber());
                request.setStudentName(session.getDepositedBy());
                request.setPhoneNo(session.getMobilePhone() != null ? session.getMobilePhone() : "254700000000"); // Fallback value
                request.setBillAmount(String.valueOf(session.getAmount()));
                request.setAmount("1"); // Example value, adjust logic if needed

                LipaKaroResponse response = lipaKaroApi.payFees(request);
                if ("0".equals(response.getStatus())) {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionDate(response.getDate() != null ?
                            LocalDateTime.parse(response.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) :
                            LocalDateTime.now());
                    transaction.setDepositorName(session.getDepositedBy() != null ? session.getDepositedBy() : "Unknown");
                    transaction.setTransactionType("lipa_karo");
                    transaction.setAmount(session.getAmount());
                    transaction.setMobileNumber(session.getMobilePhone() != null ? session.getMobilePhone() : "N/A");
                    transaction.setTransactionNumber(response.getTnxCode());
                    transaction.setSigned(true);
                    transactionRepository.save(transaction);

                    return String.format(
                            "END Lipa Karo Payment successful.\nTnxCode: %s\nAccount: %s\nAdmission: %s\nDepositor: %s\nAmount: %.2f",
                            response.getTnxCode(), session.getRecipientAccount(), session.getAdmissionNumber(), session.getDepositedBy(), response.getBalance()
                    );
                } else if ("104".equals(response.getStatus())) {
                    return "END Transaction failed: Please contact support.";
                }
                return "END Lipa Karo Payment failed. Status: " + response.getStatus();
            }
            return "END Invalid PIN. Must be 4 digits. Session ended.";
        }
        return "END No PIN provided. Session ended.";
    }
}