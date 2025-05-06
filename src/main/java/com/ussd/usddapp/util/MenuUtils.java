package com.ussd.usddapp.util;

public class MenuUtils {

    public static String getStartMenu() {
        return "CON Select Bank:\n1. KCB Bank";
    }

    public static String getBankSelectMenu() {
        return "CON Invalid option. Select:\n1. KCB Bank";
    }

    public static String getDescriptionMenu() {
        return "CON Enter Description:";
    }

    public static String getNameIdMenu() {
        return "CON Enter Name/ID Number:";
    }

    public static String getAccountNumberMenu() {
        return "CON Enter Account Number:";
    }

    public static String getAmountMenu() {
        return "CON Enter Amount:";
    }

    public static String getInvalidAmountMenu() {
        return "CON Amount must be greater than 0. Enter Amount:";
    }

    public static String getConfirmMenu(String accountNumber, double amount, String description, String nameId) {
        return String.format("CON Confirm Deposit:\nBank: KCB Bank\nDescription: %s\nName/ID: %s\nAmount: %.2f KES\nAccount: %s\n1. Proceed\n0. Cancel", description, nameId, amount, accountNumber);
    }

    public static String getInvalidConfirmMenu() {
        return "CON Invalid option. Select:\n1. Proceed\n0. Cancel";
    }

    public static String getPinAuthMenu() {
        return "UPR Enter 4-digit PIN to confirm:";
    }

    public static String getInvalidPinMenu() {
        return "CON Invalid PIN. Enter 4-digit PIN:";
    }

    public static String getSuccessMenu(String tnxCode) {
        return String.format("END Deposit successful.\nTransaction Code: %s", tnxCode);
    }

    public static String getFailureMenu() {
        return "END Deposit failed. Please try again.";
    }

    public static String getErrorMenu() {
        return "END An error occurred. Please try again.";
    }

    public static String getCancelMenu() {
        return "END Deposit cancelled.";
    }

    public static String getInvalidAccountMenu() {
        return "END Invalid account. Please try again.";
    }
}