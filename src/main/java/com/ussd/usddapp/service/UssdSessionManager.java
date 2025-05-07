package com.ussd.usddapp.service;

import com.ussd.usddapp.dto.*;
import org.springframework.stereotype.*;

import java.util.*;

@Component
public class UssdSessionManager {

    private Map<String, UssdSession> sessions = new HashMap<>();

    public UssdSession getOrCreateSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new UssdSession());
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}