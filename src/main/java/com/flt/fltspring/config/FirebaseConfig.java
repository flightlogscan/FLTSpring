package com.flt.fltspring.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {
    @Autowired
    private String firebaseAdminSecret;

    @PostConstruct
    public void initialize() throws Throwable {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                        new ByteArrayInputStream(firebaseAdminSecret.getBytes(StandardCharsets.UTF_8))))
                .build();
        FirebaseApp.initializeApp(options);
        log.info("Finished initializing firebase app");
    }
}
