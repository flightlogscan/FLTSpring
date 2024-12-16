package com.flt.fltspring.claims;

import com.google.common.collect.ImmutableSet;

public final class AdminAuthenticator {

    private AdminAuthenticator() {}

    // Not the most scalable solution since it requires hardcoding and deploying
    // But good enough for now! We should use Firebase Claims.
    private static final ImmutableSet<String> ADMIN_EMAILS = ImmutableSet.of(
                    "will.janis@gmail.com",
                    "flightlogtracer@gmail.com",
                    "flightlogtracer@flightlogtracer.com",
                    "lancedesi@msn.com");

    public static boolean isAdmin(final String emailToAuthenticate) {
        return ADMIN_EMAILS.contains(emailToAuthenticate);
    }
}
