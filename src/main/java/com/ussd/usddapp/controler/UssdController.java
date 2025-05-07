package com.ussd.usddapp.controler;

import com.ussd.usddapp.dto.*;
import com.ussd.usddapp.service.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UssdController {

    private final UssdSessionManager sessionManager;
    private final UssdStateHandler stateHandler;

    @Value("${api.key}")
    private String apiKey;

    @PostMapping(value = "/ussd", consumes = "application/x-www-form-urlencoded", produces = "text/plain")
    public @ResponseBody String handleUssd(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", required = false, defaultValue = "") String text) {

        log.info("Received USSD request - sessionId: {}, phoneNumber: {}, text: {}", sessionId, phoneNumber, text);
        UssdSession session = sessionManager.getOrCreateSession(sessionId);
        String response;

        try {
            response = stateHandler.handleState(session, text.split("\\*"), apiKey);
        } catch (IOException e) {
            log.error("Error processing USSD request: {}", e.getMessage(), e);
            response = "END An error occurred. Please try again later.";
            sessionManager.removeSession(sessionId);
        }

        log.info("Sending USSD response: {}", response);
        return response;
    }
}