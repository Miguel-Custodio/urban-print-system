package com.urbanprint.backend.controller;

import com.urbanprint.backend.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_COOKIE = "UP_SESSION";
    private static final long SESSION_TTL_SECONDS = 12L * 60 * 60; // 12 hours

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResult(boolean ok, String username, String displayName, String error) {
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@RequestBody LoginRequest request) {
        return authService.authenticate(request.username(), request.password())
                .map(user -> {
                    String token = authService.createSession(user);

                    ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, token)
                            .httpOnly(true)
                            .path("/")
                            .maxAge(SESSION_TTL_SECONDS)
                            .sameSite("Lax")
                            .build();

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, cookie.toString())
                            .body(new LoginResult(true, user.username(), user.displayName(), null));
                })
                .orElse(ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResult(false, null, null, "Invalid username or password.")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken) {

        authService.logout(sessionToken);

        ResponseCookie clearedCookie = ResponseCookie.from(SESSION_COOKIE, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }
}