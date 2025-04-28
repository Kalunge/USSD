package com.ussd.usddapp.service;

import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "ussd:session:";
    private static final String SESSION_DATA_PREFIX = "ussd:session:data:";
    private static final long SESSION_TTL_SECONDS = 180; // 3 minutes

    private final RedisTemplate<String, String> redisTemplate;

    public void setState(String sessionId, String state) {
        String key = SESSION_KEY_PREFIX + sessionId;
        log.debug("Setting state: key={}, state={}", key, state);
        try {
            redisTemplate.opsForValue().set(key, state, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("State set successfully: key={}", key);
        } catch (Exception e) {
            log.error("Failed to set state in Redis: key={}, state={}, error={}", key, state, e.getMessage(), e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public String getState(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        log.debug("Retrieving state: key={}", key);
        try {
            String state = redisTemplate.opsForValue().get(key);
            log.debug("State retrieved: key={}, state={}", key, state);
            return state != null ? state : "START";
        } catch (Exception e) {
            log.error("Failed to retrieve state from Redis: key={}, error={}", key, e.getMessage(), e);
            return "START";
        }
    }

    public void setData(String sessionId, String key, String value) {
        String dataKey = SESSION_DATA_PREFIX + sessionId + ":" + key;
        log.debug("Setting data: key={}, value={}", dataKey, value);
        try {
            redisTemplate.opsForValue().set(dataKey, value, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Data set successfully: key={}", dataKey);
        } catch (Exception e) {
            log.error("Failed to set data in Redis: key={}, value={}, error={}", dataKey, value, e.getMessage(), e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    public String getData(String sessionId, String key, String defaultValue) {
        String dataKey = SESSION_DATA_PREFIX + sessionId + ":" + key;
        log.debug("Retrieving data: key={}", dataKey);
        try {
            String value = redisTemplate.opsForValue().get(dataKey);
            log.debug("Data retrieved: key={}, value={}", dataKey, value);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.error("Failed to retrieve data from Redis: key={}, error={}", dataKey, e.getMessage(), e);
            return defaultValue;
        }
    }

    public void clearSession(String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        String dataKeyPrefix = SESSION_DATA_PREFIX + sessionId + ":";
        log.debug("Clearing session: sessionId={}", sessionId);
        try {
            // Delete the session state
            redisTemplate.delete(sessionKey);
            log.debug("Deleted session state: key={}", sessionKey);

            // Delete session data keys (use a more efficient approach)
            redisTemplate.delete(dataKeyPrefix + "amount"); // Since we only store "amount" in this app
            log.debug("Deleted session data: key={}", dataKeyPrefix + "amount");
        } catch (Exception e) {
            log.error("Failed to clear session in Redis: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
        log.debug("Session cleared: sessionId={}", sessionId);
    }

    // manage session in app
//    private final Map<String, String> sessions = new HashMap<>();
//    private final Map<String, String> sessionData = new HashMap<>();

//    public void setState(String sessionId, String state) {
//        sessions.put(sessionId, state);
//    }
//
//    public String getState(String sessionId) {
//        return sessions.getOrDefault(sessionId, "START");
//    }
//
//    public void setData(String sessionId, String key, String value) {
//        sessionData.put(sessionId + "_" + key, value);
//    }
//
//    public String getData(String sessionId, String key, String defaultValue) {
//        return sessionData.getOrDefault(sessionId + "_" + key, defaultValue);
//    }
//
//    public void clearSession(String sessionId) {
//        sessions.remove(sessionId);
//        sessionData.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId + "_"));
//    }
}