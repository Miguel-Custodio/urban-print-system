package com.urbanprint.backend.config;

import com.urbanprint.backend.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String SESSION_COOKIE = "UP_SESSION";

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String route = request.getRequestURI().substring(request.getContextPath().length());

        boolean isAuthEndpoint = route.equals("/api/auth/login") || route.equals("/api/auth/logout");
        boolean isApi = route.startsWith("/api/");

        String sessionToken = extractSessionToken(request);
        boolean authenticated = authService.isValidSession(sessionToken);

        // Login/logout are always reachable
        if (isAuthEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        // API: no session -> 401
        if (isApi) {
            if (!authenticated) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Not authenticated\"}");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Login page: if already authenticated, go to the dashboard
        if (route.equals("/login.html")) {
            if (authenticated) {
                response.sendRedirect(request.getContextPath() + "/dashboard.html");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Any other page without session -> login page
        if (!authenticated) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        // Root goes to the dashboard when authenticated
        if (route.equals("/")) {
            response.sendRedirect(request.getContextPath() + "/dashboard.html");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractSessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}