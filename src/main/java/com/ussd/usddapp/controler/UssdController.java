package com.ussd.usddapp.controler;


import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class UssdController {
    private Map<String, String> sessions = new HashMap<>();
    private Map<String, Double> balances = new HashMap<>();

    @PostMapping(value = "/ussd", consumes = "application/x-www-form-urlencoded")
    public String handleUssd(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        // Log incoming request for debugging
        System.out.println("Request: sessionId=" + sessionId + ", phoneNumber=" + phoneNumber + ", text=" + text);

        // Initialize balance for new users
        balances.putIfAbsent(phoneNumber, 100.0);

        // Split the text input by '*' to handle concatenated inputs
        String[] inputs = text.isEmpty() ? new String[0] : text.split("\\*");
        String latestInput = inputs.length > 0 ? inputs[inputs.length - 1] : "";
        int inputLevel = inputs.length; // Tracks the number of inputs (depth in the menu)

        // Retrieve session state
        String state = sessions.getOrDefault(sessionId, "START");
        System.out.println("Current state: " + state + ", Input level: " + inputLevel + ", Latest input: " + latestInput);

        // Handle the initial request (text is empty)
        if (text.isEmpty()) {
            sessions.put(sessionId, "MENU");
            System.out.println("Transition to MENU");
            return "CON Welcome to Mini Bank\n1. Check Balance\n2. Transfer Money\n0. Exit";
        }

        // Process based on state and input level
        if (state.equals("MENU")) {
            switch (latestInput) {
                case "1":
                    sessions.put(sessionId, "CHECK_BALANCE");
                    System.out.println("Transition to CHECK_BALANCE");
                    return "END Your balance is $" + balances.get(phoneNumber);
                case "2":
                    sessions.put(sessionId, "TRANSFER_AMOUNT");
                    System.out.println("Transition to TRANSFER_AMOUNT");
                    return "CON Enter amount to transfer:";
                case "0":
                    sessions.remove(sessionId);
                    System.out.println("Session ended");
                    return "END Goodbye!";
                default:
                    return "END Invalid option.";
            }
        } else if (state.equals("CHECK_BALANCE")) {
            // Reset state to MENU after showing balance
            sessions.put(sessionId, "MENU");
            System.out.println("Transition back to MENU after CHECK_BALANCE");
            switch (latestInput) {
                case "1":
                    sessions.put(sessionId, "CHECK_BALANCE");
                    System.out.println("Transition to CHECK_BALANCE");
                    return "END Your balance is $" + balances.get(phoneNumber);
                case "2":
                    sessions.put(sessionId, "TRANSFER_AMOUNT");
                    System.out.println("Transition to TRANSFER_AMOUNT");
                    return "CON Enter amount to transfer:";
                case "0":
                    sessions.remove(sessionId);
                    System.out.println("Session ended");
                    return "END Goodbye!";
                default:
                    return "END Invalid option.";
            }
        } else if (state.equals("TRANSFER_AMOUNT")) {
            try {
                double amount = Double.parseDouble(latestInput);
                sessions.put(sessionId, "TRANSFER_PHONE");
                sessions.put(sessionId + "_amount", String.valueOf(amount));
                System.out.println("Amount: " + amount + ", Transition to TRANSFER_PHONE");
                return "CON Enter recipient's phone number:";
            } catch (NumberFormatException e) {
                sessions.remove(sessionId);
                return "END Invalid amount.";
            }
        } else if (state.equals("TRANSFER_PHONE")) {
            double amount = Double.parseDouble(sessions.getOrDefault(sessionId + "_amount", "0"));
            double balance = balances.get(phoneNumber);
            if (amount <= balance) {
                balances.put(phoneNumber, balance - amount);
                sessions.remove(sessionId);
                System.out.println("Transfer successful, session ended");
                return "END Transfer of $" + amount + " to " + latestInput + " successful.";
            } else {
                sessions.remove(sessionId);
                System.out.println("Insufficient funds, session ended");
                return "END Insufficient funds.";
            }
        } else {
            System.out.println("Invalid state: " + state);
            return "END Something went wrong.";
        }
    }
}
