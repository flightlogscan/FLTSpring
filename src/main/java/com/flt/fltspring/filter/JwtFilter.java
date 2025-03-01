package com.flt.fltspring.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@Order(1)
@Slf4j
public class JwtFilter implements Filter {
    private static final String REQUEST_ID = "requestId";
    private static final String FIREBASE_EMAIL_ATTR = "firebaseEmail";
    private static final String BEARER_PREFIX = "Bearer ";
    
    // Endpoints that don't require authentication
    private static final Set<String> PUBLIC_ENDPOINTS = new HashSet<>(Arrays.asList(
            "/api/ping"
    ));
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        // Insert request id into context for tracking
        final String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        response.setHeader("X-Request-ID", requestId);

        try {
            // Skip JWT processing for public endpoints
            if (isPublicEndpoint(request.getRequestURI())) {
                filterChain.doFilter(req, res);
                return;
            }

            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("Missing or invalid Authorization header for request: {}", requestId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return;
            }

            // Extract token by removing prefix
            final String token = authHeader.substring(BEARER_PREFIX.length());

            try {
                final FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                final String uid = decodedToken.getUid();
                final String email = decodedToken.getEmail();

                // Add user information to request for use in controllers
                request.setAttribute(FIREBASE_EMAIL_ATTR, email);

                // Log authentication info with redacted token
                log.info("Authenticated user uid: {}, Request ID: {}", uid, requestId);
                log.debug("Authentication details - Email: {}, Request ID: {}", email, requestId);
            } catch (FirebaseAuthException e) {
                log.warn("Firebase authentication failed: {}, Request ID: {}", e.getMessage(), requestId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token");
                return;
            }

            filterChain.doFilter(req, res);
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }
    
    private boolean isPublicEndpoint(String uri) {
        return PUBLIC_ENDPOINTS.contains(uri);
    }
}
