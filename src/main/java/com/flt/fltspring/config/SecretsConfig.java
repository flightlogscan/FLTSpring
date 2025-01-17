package com.flt.fltspring.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SecretsConfig {
    private static final String FLIGHT_LOG_SCAN_INFISICAL_PROJECT_ID = "34457db8-92b6-4785-af7d-aff07b776299";
    private String firebaseAdminSecret;
    private String flsDocumentAiSecret;

    @Value("${api.token:#{null}}")
    private String dotenvToken;

    @PostConstruct
    public void initializeSecrets() throws Exception {

        String token = System.getenv("API_TOKEN");
        if (token == null) {
            if (dotenvToken == null) {
                throw new IllegalStateException("Neither API_TOKEN env var nor api.token property found");
            }
            token = dotenvToken;
        }

        var sdk = new InfisicalSdk(new SdkConfig.Builder().build());
        sdk.Auth().SetAccessToken(token);

        this.firebaseAdminSecret = sdk.Secrets().GetSecret(
       "FIREBASE_ADMIN",
       FLIGHT_LOG_SCAN_INFISICAL_PROJECT_ID,
        "prod",
        null,
        null,
        null,
        null
        ).getSecretValue();

        this.flsDocumentAiSecret = sdk.Secrets().GetSecret(
                "FLS_DOCUMENT_AI",
                FLIGHT_LOG_SCAN_INFISICAL_PROJECT_ID,
                "prod",
                null,
                null,
                null,
                null
        ).getSecretValue();
    }

    @Bean
    public String firebaseAdminSecret() {
        return firebaseAdminSecret;
    }

    @Bean
    public String flsDocumentAiSecret() {
        return flsDocumentAiSecret;
    }
}
