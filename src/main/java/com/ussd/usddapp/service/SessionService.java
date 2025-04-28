package com.ussd.usddapp.service;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SessionService {
    private final Map<String, String> sessions = new HashMap<>();
    private final Map<String, String> sessionData = new HashMap<>();

    public void setState(String sessionId, String state) {
        sessions.put(sessionId, state);
    }

    public String getState(String sessionId) {
        return sessions.getOrDefault(sessionId, "START");
    }

    public void setData(String sessionId, String key, String value) {
        sessionData.put(sessionId + "_" + key, value);
    }

    public String getData(String sessionId, String key, String defaultValue) {
        return sessionData.getOrDefault(sessionId + "_" + key, defaultValue);
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        sessionData.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId + "_"));
    }
}