package com.ussd.usddapp.util;

import org.springframework.stereotype.Service;

import org.springframework.stereotype.Service;

@Service
public class MenuService {

    public String getMainMenu() {
        return "CON Welcome to USSD Banking\n1. Check Balance\n2. Transfer Money\n0. Exit";
    }

    public String getBalanceMessage(double balance) {
        return "CON Your balance is KSH " + balance + "\n1. Check Balance\n2. Transfer Money\n0. Exit";
    }

    public String getTransferAmountPrompt() {
        return "CON Enter amount to transfer:";
    }

    public String getTransferPhonePrompt() {
        return "CON Enter recipient phone number:";
    }

    public String getTransferSuccessMessage(double amount, String recipientPhone) {
        return "END Transfer of KSH " + amount + " to " + recipientPhone + " successful.";
    }

    public String getInsufficientFundsMessage() {
        return "END Insufficient funds.";
    }

    public String getInvalidOptionMessage() {
        return "CON Invalid option. Please try again.\n1. Check Balance\n2. Transfer Money\n0. Exit";
    }

    public String getInvalidAmountMessage() {
        return "END Invalid amount entered.";
    }

    public String getExitMessage() {
        return "END Thank you for using USSD Banking.";
    }

    public String getErrorMessage() {
        return "END An error occurred. Please try again.";
    }

    public String getEnterPinPrompt() {
        return "CON Please enter your 4-digit PIN:";
    }

    public String getInvalidPinMessage() {
        return "CON Invalid PIN. Please try again.\nEnter your 4-digit PIN:";
    }

    public String getSetPinPrompt() {
        return "CON Welcome! Please set a 4-digit PIN:";
    }

    public String getConfirmPinPrompt() {
        return "CON Please confirm your 4-digit PIN:";
    }

    public String getPinMismatchMessage() {
        return "CON PINs do not match. Please try again.\nSet a 4-digit PIN:";
    }

    public String getPinSetSuccessMessage() {
        return "CON PIN set successfully.";
    }
}