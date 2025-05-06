package com.ussd.usddapp.emulators;


import com.ussd.usddapp.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
@Slf4j
public class DummyUssdEmulator implements CommandLineRunner {

    @Data
    public static class SessionData {
        private String state = "START";
        private String accountNumber;
        private double amount;
        private String description;
        private String nameId;
    }

    private final Map<String, SessionData> sessionStore = new HashMap<>();
    private static final String SESSION_ID = "dummy-session-001";

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        SessionData sessionData = sessionStore.computeIfAbsent(SESSION_ID, k -> new SessionData());

        System.out.println("=== Dummy USSD Emulator ===");
        System.out.println("Simulating USSD session. Enter inputs or 'exit' to quit.");

        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting emulator...");
                break;
            }

            String response = processInput(sessionData, input);
            System.out.println(response);

            if (response.startsWith("END ")) {
                sessionStore.remove(SESSION_ID);
                break;
            }
        }

        scanner.close();
    }

    private String processInput(SessionData sessionData, String input) {
        String state = sessionData.getState();
        log.debug("Processing state: {}, input: {}", state, input);

        switch (state) {
            case "START":
                sessionData.setState("BANK_SELECT");
                return MenuUtils.getStartMenu();
            case "BANK_SELECT":
                if (input.equals("1")) {
                    sessionData.setState("DESCRIPTION");
                    return MenuUtils.getDescriptionMenu();
                } else {
                    return MenuUtils.getBankSelectMenu();
                }

            case "NAME_ID":
                if (!input.trim().isEmpty()) {
                    sessionData.setNameId(input);
                    sessionData.setState("ACCOUNT_NUMBER");
                    return MenuUtils.getAccountNumberMenu();
                } else {
                    return "CON Please enter a valid Name/ID Number:";
                }
            case "ACCOUNT_NUMBER":
                if (!input.trim().isEmpty()) {
                    sessionData.setAccountNumber(input);
                    sessionData.setState("AMOUNT");
                    return MenuUtils.getAmountMenu();
                } else {
                    return "CON Please enter a valid account number:";
                }
            case "AMOUNT":
                try {
                    double amount = Double.parseDouble(input.trim());
                    if (amount > 0) {
                        sessionData.setAmount(amount);
                        sessionData.setState("CONFIRM");
                        return MenuUtils.getConfirmMenu(sessionData.getAccountNumber(), sessionData.getAmount(), sessionData.getDescription(), sessionData.getNameId());
                    } else {
                        return MenuUtils.getInvalidAmountMenu();
                    }
                } catch (NumberFormatException e) {
                    return "CON Please enter a valid amount:";
                }

            case "DESCRIPTION":
                if (!input.trim().isEmpty()) {
                    sessionData.setDescription(input);
                    sessionData.setState("NAME_ID");
                    return MenuUtils.getNameIdMenu();
                } else {
                    return "CON Please enter a valid description:";
                }
            case "CONFIRM":
                if (input.equals("1")) {
                    sessionData.setState("PIN_AUTH");
                    return MenuUtils.getPinAuthMenu();
                } else if (input.equals("0")) {
                    sessionStore.remove(SESSION_ID);
                    return MenuUtils.getCancelMenu();
                } else {
                    return MenuUtils.getInvalidConfirmMenu();
                }
            case "PIN_AUTH":
                if (input.matches("\\d{4}")) {
                    sessionStore.remove(SESSION_ID);
                    return MenuUtils.getSuccessMenu("DUMMY-TNX-123");
                } else {
                    return MenuUtils.getInvalidPinMenu();
                }
            default:
                sessionStore.remove(SESSION_ID);
                return MenuUtils.getErrorMenu();
        }
    }
}

// description not before