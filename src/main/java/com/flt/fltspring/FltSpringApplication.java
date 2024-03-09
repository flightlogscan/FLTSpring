package com.flt.fltspring;

import com.flt.fltspring.secret.FirebaseSecretRetriever;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.io.IOException;

@SpringBootApplication
@ServletComponentScan
public class FltSpringApplication {

    public static void main(String[] args) throws IOException {

        @SuppressWarnings("deprecation")
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(FirebaseSecretRetriever.getSecretStream()))
                .build();
        FirebaseApp.initializeApp(options);
        SpringApplication.run(FltSpringApplication.class, args);
    }
}
