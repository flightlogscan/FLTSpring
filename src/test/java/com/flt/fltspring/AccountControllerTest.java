package com.flt.fltspring;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.flt.fltspring.filter.JwtFilter.FIREBASE_EMAIL_ATTR;
import static com.flt.fltspring.filter.JwtFilter.FIREBASE_UID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @Test
    void deleteAccount_success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(FIREBASE_UID, "uid1");
        request.setAttribute(FIREBASE_EMAIL_ATTR, "a@b.com");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> mocked = Mockito.mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            AccountController controller = new AccountController();
            ResponseEntity<String> resp = controller.deleteAccount(request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).contains("successful");
            verify(mockAuth).deleteUser("uid1");
        }
    }

    @Test
    void deleteAccount_firebaseError_returns500() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(FIREBASE_UID, "uid1");
        request.setAttribute(FIREBASE_EMAIL_ATTR, "a@b.com");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        doThrow(new FirebaseAuthException(new FirebaseException(ErrorCode.INTERNAL, "error", null)))
                .when(mockAuth).deleteUser("uid1");

        try (MockedStatic<FirebaseAuth> mocked = Mockito.mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            AccountController controller = new AccountController();
            ResponseEntity<String> resp = controller.deleteAccount(request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(mockAuth).deleteUser("uid1");
        }
    }
}
