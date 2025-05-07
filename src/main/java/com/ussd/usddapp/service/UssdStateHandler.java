package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import lombok.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UssdStateHandler {

    private final BankSelectionService bankSelectionService;
    private final TransactionService transactionService;
    private final ValidationService validationService;
    private final DepositService depositService;

    private static final String[] BANKS = {"KCB", "ABSA", "COOP"};

    public String handleState(UssdSession session, String[] inputParts, String apiKey) throws IOException {
        String userInput = inputParts.length > 0 ? inputParts[inputParts.length - 1] : "";
        String response;

        switch (session.getState()) {
            case INIT:
                response = "CON Welcome to Agency Banking\nSelect Bank:\n" +
                        "1. KCB\n2. ABSA\n3. COOP";
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

            default:
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