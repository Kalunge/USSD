package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import org.springframework.stereotype.*;

@Service
public class BankSelectionService {

    private static final String[] BANKS = {"KCB", "ABSA", "COOP"};

    public String handleBankSelection(UssdSession session, String[] inputParts) {
        if (inputParts.length > 0) {
            String choice = inputParts[0];
            int bankIndex;
            try {
                bankIndex = Integer.parseInt(choice) - 1;
                if (bankIndex >= 0 && bankIndex < BANKS.length) {
                    session.setBank(BANKS[bankIndex]);
                    session.setState(UssdSession.State.MENU);
                    return "CON Bank Selected: " + session.getBank() + "\n1. Deposit Money";
                }
            } catch (NumberFormatException e) {
                return "END Invalid input. Session ended.";
            }
        }
        return "END No bank selected. Session ended.";
    }
}