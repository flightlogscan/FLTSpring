package com.flt.fltspring.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
@EnableCaching
public class JwtFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        final String authHeader = request.getHeader("Authorization");
        System.out.println(authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Invalid authHeader: " + authHeader);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            final String uid = decodedToken.getUid();
            final String email = decodedToken.getEmail();

            // HttpServletRequest is a server side object so an attacker cannot set this attribute
            request.setAttribute("firebaseEmail", email);

            System.out.println("Uid: " + uid);
            System.out.println("Email: " + email);
        } catch (FirebaseAuthException e) {
            System.out.println("Firebase auth exception: " + e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(req, res);
    }
}
