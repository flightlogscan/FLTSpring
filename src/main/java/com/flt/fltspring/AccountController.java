package com.flt.fltspring;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.flt.fltspring.filter.JwtFilter.FIREBASE_EMAIL_ATTR;
import static com.flt.fltspring.filter.JwtFilter.FIREBASE_UID;

@Slf4j
@RestController
@RequestMapping("/api/account")
public class AccountController {

    /**
     * Deletes a user account in firebase.
     *
     * History: Ability to delete an account is required by Apple.
     * We couldn't delete on the front end because deleting requires re-auth (achieved here in JwtFilter.java)
     * Apple doesn't like re-auth on front-end because they require delete account to "just delete", no extra steps.
     *
     * Local call:
     * curl -X POST http://localhost:8080/api/account/delete \
     *   -H "Authorization: Bearer {get token from ios app log}"
     *
     * @param request HttpServlet request giving Firebase user info (see: JwtFilter.java)
     * @return Whether the account deletion was successful or not.
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteAccount(final HttpServletRequest request) {

        final String firebaseUid = (String) request.getAttribute(FIREBASE_UID);
        final String firebaseEmail = (String) request.getAttribute(FIREBASE_EMAIL_ATTR);

        try {
            // JwtFilter verifies token on every request which allows us to delete here.
            log.info("Attempting to delete uid={} and email={}", firebaseUid, firebaseEmail);
            FirebaseAuth.getInstance().deleteUser(firebaseUid);
            log.info("Account deletion successful. uid={} and email={}", firebaseUid, firebaseEmail);
            return ResponseEntity.ok("Account deletion successful.");

        } catch (FirebaseAuthException exception) {
            log.error("Error deleting user: {}", firebaseEmail, exception);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user, please try again.");
        }
    }
}