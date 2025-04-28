package com.ussd.usddapp.util;

import org.springframework.stereotype.Service;

@Service
public class MenuService {

    public String getMainMenu() {
        return "CON Welcome to Mini Bank\n1. Check Balance\n2. Transfer Money\n0. Exit";
    }

    public String getTransferAmountPrompt() {
        return "CON Enter amount to transfer:";
    }

    public String getTransferPhonePrompt() {
        return "CON Enter recipient's phone number:";
    }

    public String getBalanceMessage(double balance) {
        return "END Your balance is $" + balance;
    }

    public String getTransferSuccessMessage(double amount, String recipientPhone) {
        return "END Transfer of $" + amount + " to " + recipientPhone + " successful.";
    }

    public String getExitMessage() {
        return "END Goodbye!";
    }

    public String getInvalidOptionMessage() {
        return "END Invalid option.";
    }

    public String getInvalidAmountMessage() {
        return "END Invalid amount.";
    }

    public String getInsufficientFundsMessage() {
        return "END Insufficient funds.";
    }

    public String getErrorMessage() {
        return "END Something went wrong.";
    }
}