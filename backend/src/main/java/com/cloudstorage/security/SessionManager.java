package com.cloudstorage.security;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private static class Session {
        final UUID userId;
        final LocalDateTime expiresAt;

        Session(UUID userId) {
            this.userId = userId;
            this.expiresAt = LocalDateTime.now().plusHours(2);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public String createSession(UUID userId) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new Session(userId));
        return sessionId;
    }

    public boolean isValidSession(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null && !session.isExpired();
    }

    public UUID getUserId(String sessionId) {
        Session session = sessions.get(sessionId);
        return (session != null && !session.isExpired()) ? session.userId : null;
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public void invalidateAllUserSessions(UUID userId) {
        sessions.entrySet().removeIf(entry -> entry.getValue().userId.equals(userId));
    }
}