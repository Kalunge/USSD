package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.service.modules.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UssdStateHandler {

    private final BankSelectionService bankSelectionService;
    private final TransactionService transactionService;
    private final ValidationService validationService;
    private final DepositService depositService;
    private final MobileMoneyTransactionService mobileMoneyTransactionService;
    private final LipaKaroService lipaKaroService;

    private static final String[] BANKS = {"KCB", "ABSA", "COOP"};

    public String handleState(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        log.debug("Handling state: {}, inputParts: {}", session.getState(), String.join(",", inputParts));
        String userInput = inputParts.length > 0 ? inputParts[inputParts.length - 1] : "";
        String response;

        switch (session.getState()) {
            case INIT:
                response = "CON Welcome to Agency Banking\nSelect Bank:\n1. KCB";
                session.setState(UssdSession.State.SELECT_BANK);
                break;

            case SELECT_BANK:
                response = bankSelectionService.handleBankSelection(session, inputParts);
                break;

            case MENU:
                response = transactionService.handleMenu(session, inputParts);
                break;

            case ENTER_ACCOUNT:
                response = transactionService.handleAccountEntry(session, inputParts, apiKey);
                break;

            case ENTER_AMOUNT:
                response = transactionService.handleAmountEntry(session, inputParts);
                break;

            case CONFIRM_ACCOUNT:
                response = transactionService.handleConfirmation(session, inputParts);
                break;

            case ENTER_PIN:
                response = depositService.handlePinEntry(session, inputParts, apiKey);
                break;

            case SELECT_MOBILE_MONEY_OPTION:
                response = mobileMoneyTransactionService.handleMobileMoneyOption(session, inputParts);
                break;

            case SELECT_TELCO:
                response = mobileMoneyTransactionService.handleTelcoSelection(session, inputParts);
                break;

            case ENTER_PHONE:
                response = mobileMoneyTransactionService.handlePhoneEntry(session, inputParts, apiKey);
                break;

            case ENTER_MOBILE_AMOUNT:
                log.info("Calling handleMobileAmountEntry or handleLipaKaroAmountEntry");
                if ("lipa_karo".equals(session.getTransactionType())) {
                    response = lipaKaroService.handleLipaKaroAmountEntry(session, inputParts);
                } else {
                    response = mobileMoneyTransactionService.handleMobileAmountEntry(session, inputParts);
                }
                break;

            case CONFIRM_MOBILE:
                response = mobileMoneyTransactionService.handleMobileConfirmation(session, inputParts);
                break;

            case ENTER_MOBILE_PIN:
                log.info("Calling handleMobilePinEntry or handleLipaKaroPinEntry");
                if ("lipa_karo".equals(session.getTransactionType())) {
                    response = lipaKaroService.handleLipaKaroPinEntry(session, inputParts, apiKey);
                } else {
                    response = mobileMoneyTransactionService.handleMobilePinEntry(session, inputParts, apiKey);
                }
                break;

            case ENTER_WITHDRAW_AMOUNT:
                response = mobileMoneyTransactionService.handleWithdrawAmountEntry(session, inputParts);
                break;

            case CONFIRM_WITHDRAW:
                response = mobileMoneyTransactionService.handleWithdrawConfirmation(session, inputParts);
                break;

            case ENTER_WITHDRAW_PIN:
                response = mobileMoneyTransactionService.handleWithdrawPinEntry(session, inputParts, apiKey);
                break;

            case SELECT_LIPA_KARO:
                response = lipaKaroService.handleLipaKaroSelection(session, inputParts);
                break;

            case ENTER_RECIPIENT_ACCOUNT:
                response = lipaKaroService.handleRecipientAccountEntry(session, inputParts);
                break;

            case ENTER_ADMISSION_NUMBER:
                response = lipaKaroService.handleAdmissionNumberEntry(session, inputParts);
                break;

            case ENTER_DEPOSITED_BY:
                response = lipaKaroService.handleDepositedByEntry(session, inputParts);
                break;

            case CONFIRM_LIPA_KARO:
                response = lipaKaroService.handleLipaKaroConfirmation(session, inputParts, apiKey);
                break;

            default:
                log.error("Invalid state encountered: {}. Session ID: {}, Input: {}", session.getState(), java.util.Arrays.toString(inputParts));
                response = "END Invalid state. Session ended.";
                session.setState(UssdSession.State.INIT);
                break;
        }

        if (response.startsWith("END")) {
            session.setState(UssdSession.State.INIT);
        }
        return response;
    }
}