package com.urbanprint.backend.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final long SESSION_TTL_MS = 12L * 60 * 60 * 1000; // 12 hours

    public record UserAccount(String username, String password, String displayName) {
    }

    private record Session(String username, long expiresAt) {
    }

    // Shared password "urban" for the team accounts, as requested.
    // "admin" / "urbanprint" is kept as a fallback login.
    private static final List<UserAccount> USERS = List.of(
            new UserAccount("Luana Pinhanez", "urban", "Luana Pinhanez"),
            new UserAccount("Bruno Frede", "urban", "Bruno Frede"),
            new UserAccount("Miguel Custodio", "urban", "Miguel Custodio"),
            new UserAccount("admin", "urbanprint", "Administrator")
    );

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Optional<UserAccount> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        String cleanUsername = username.trim();

        return USERS.stream()
                .filter(user -> user.username().equalsIgnoreCase(cleanUsername)
                        && user.password().equals(password))
                .findFirst();
    }

    public String createSession(UserAccount user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(user.username(), System.currentTimeMillis() + SESSION_TTL_MS));
        return token;
    }

    public boolean isValidSession(String token) {
        if (token == null) {
            return false;
        }

        Session session = sessions.get(token);

        if (session == null) {
            return false;
        }

        if (session.expiresAt() < System.currentTimeMillis()) {
            sessions.remove(token);
            return false;
        }

        return true;
    }

    public String usernameOf(String token) {
        Session session = token == null ? null : sessions.get(token);
        return session == null ? null : session.username();
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}