package com.flt.fltspring;

import com.flt.fltspring.secret.FirebaseSecretRetriever;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.io.IOException;

@SpringBootApplication
@ServletComponentScan
@Slf4j
public class FltSpringApplication {

    public static void main(String[] args) throws IOException {

        @SuppressWarnings("deprecation")
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(FirebaseSecretRetriever.getSecretStream()))
                .build();
        FirebaseApp.initializeApp(options);
        log.info("Finished initializing firebase app");
        SpringApplication.run(FltSpringApplication.class, args);
    }
}
