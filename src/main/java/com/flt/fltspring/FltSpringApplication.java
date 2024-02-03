package com.flt.fltspring;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@SpringBootApplication
@ServletComponentScan
public class FltSpringApplication {

    public static void main(String[] args) throws IOException {

        Resource resource = new ClassPathResource("credentials/flightlogtracer-43695-firebase-adminsdk-1dyid-e1c32007d0.json");

        @SuppressWarnings("deprecation")
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .build();
        FirebaseApp.initializeApp(options);
        SpringApplication.run(FltSpringApplication.class, args);
    }
}
