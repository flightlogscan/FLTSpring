package com.flt.fltspring.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtFilterTest {
    private final JwtFilter filter = new JwtFilter();

    @Test
    void publicEndpoint_allowsWithoutAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET","/api/ping");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req,res,chain);
        verify(chain).doFilter(req,res);
    }

    @Test
    void missingAuthHeader_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET","/api/other");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req,res,chain);
        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(),any());
    }

    @Test
    void validToken_setsAttributes() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET","/api/other");
        req.addHeader("Authorization","Bearer token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid1");
        when(token.getEmail()).thenReturn("e@x.com");
        try (MockedStatic<FirebaseAuth> stat = Mockito.mockStatic(FirebaseAuth.class)) {
            stat.when(FirebaseAuth::getInstance).thenReturn(auth);
            when(auth.verifyIdToken("token")).thenReturn(token);

            filter.doFilter(req,res,chain);
            verify(chain).doFilter(req,res);
            assertThat(req.getAttribute(JwtFilter.FIREBASE_UID)).isEqualTo("uid1");
            assertThat(req.getAttribute(JwtFilter.FIREBASE_EMAIL_ATTR)).isEqualTo("e@x.com");
        }
    }
}
