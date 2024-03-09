package com.flt.fltspring.secret;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FirebaseSecretRetriever {
    public static InputStream getSecretStream() {
        String firebaseAdminSecretName = "firebase_admin";
        String secret = AWSSecretRetriever.getSecretString(firebaseAdminSecretName);

        return new ByteArrayInputStream(secret.getBytes(StandardCharsets.UTF_8));
    }
}
