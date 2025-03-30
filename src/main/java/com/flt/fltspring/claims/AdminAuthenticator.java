package com.flt.fltspring.claims;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class AdminAuthenticator {

    private final Set<String> adminEmails;

    public AdminAuthenticator(@Value("${app.admin-emails:}") String configuredAdmins) {
        Set<String> emails = new HashSet<>();
        
        // Add default admins
        emails.add("flightlogtracer@gmail.com");
        emails.add("flightlogscan@flightlogscan.com");
        
        // Add any configured admins from properties
        if (configuredAdmins != null && !configuredAdmins.isEmpty()) {
            Arrays.stream(configuredAdmins.split(","))
                  .map(String::trim)
                  .filter(email -> !email.isEmpty())
                  .forEach(emails::add);
        }
        
        this.adminEmails = ImmutableSet.copyOf(emails);
    }

    public boolean isAdmin(final String emailToAuthenticate) {
        if (emailToAuthenticate == null || emailToAuthenticate.isBlank()) {
            return false;
        }
        return adminEmails.contains(emailToAuthenticate);
    }
}
