package com.flt.fltspring.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
@EnableCaching
@Slf4j
public class JwtFilter implements Filter {
    private static final String REQUEST_ID = "requestId";
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        // Insert request id into context (see application.yml for where it's used)
        final String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);

        try {
            // Skip JWT processing for /ping endpoint
            if (request.getRequestURI().equals("/api/ping")) {
                filterChain.doFilter(req, res);
                return;
            }

            final String authHeader = request.getHeader("Authorization");
            log.info(authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid authHeader: " + authHeader);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Remove "Bearer " which is 7 chars
            final String token = authHeader.substring(7);

            try {
                final FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                final String uid = decodedToken.getUid();
                final String email = decodedToken.getEmail();

                // HttpServletRequest is a server side object so an attacker cannot set this attribute
                request.setAttribute("firebaseEmail", email);

                log.info("Uid: " + uid);
                log.info("Email: " + email);
            } catch (FirebaseAuthException e) {
                log.error("Firebase auth exception: " + e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            filterChain.doFilter(req, res);
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }
}
