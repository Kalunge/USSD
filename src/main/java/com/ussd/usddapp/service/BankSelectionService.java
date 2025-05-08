package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankSelectionService {

    private static final String[] BANKS = {"KCB", "ABSA", "COOP"};
    private final TransactionService transactionService;

    public String handleBankSelection(UssdSession session, String[] inputParts) {
        log.info("Processing bank selection with inputParts: {}", java.util.Arrays.toString(inputParts));
        if (inputParts.length > 0 && !inputParts[inputParts.length - 1].isEmpty()) {
            String choice = inputParts[inputParts.length - 1];
            int bankIndex;
            try {
                bankIndex = Integer.parseInt(choice) - 1;
                if (bankIndex >= 0 && bankIndex < BANKS.length) {
                    session.setBank(BANKS[bankIndex]);
                    session.setState(UssdSession.State.MENU); // Set state to MENU after bank selection
                    log.info("Bank selected: {}, transitioning to MENU", BANKS[bankIndex]);
                    return String.format("CON Bank Selected: %s\n1. Deposit Money\n2. Mobile Money\n3. Lipa Karo", session.getBank());
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid bank selection input: {}", choice);
            }
            return "END Invalid bank option. Session ended.";
        }
        // No input or empty input, show the bank selection prompt
        StringBuilder bankOptions = new StringBuilder("CON Select Bank:\n");
        for (int i = 0; i < BANKS.length; i++) {
            bankOptions.append(i + 1).append(". ").append(BANKS[i]).append("\n");
        }
        return bankOptions.toString();
    }
}