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
    private static final String DEFAULT_ENVIRONMENT = "prod";
    
    @Value("${infisical.project-id:13c16c80-a03a-44c3-8bca-92ae4e5efc47}")
    private String projectId;
    
    @Value("${infisical.environment:prod}")
    private String environment;
    
    @Value("${firebase.admin.secret:}")
    private String fallbackFirebaseAdminSecret;
    
    @Value("${azure.document-intelligence.secret:}")
    private String fallbackDocumentAiSecret;
    
    private String firebaseAdminSecret;
    private String flsDocumentAiSecret;

    @PostConstruct
    public void initializeSecrets() {
        try {
            String token = System.getenv("API_TOKEN");
            if (token == null || token.isBlank()) {
                handleMissingToken();
                return;
            }

            var sdk = new InfisicalSdk(new SdkConfig.Builder().build());
            sdk.Auth().SetAccessToken(token);

            // Get Firebase Admin secret
            try {
                this.firebaseAdminSecret = sdk.Secrets().GetSecret(
                        "FIREBASE_ADMIN",
                        projectId,
                        environment,
                        null, null, null, null
                ).getSecretValue();
                log.info("Successfully loaded Firebase Admin secret from Infisical");
            } catch (Exception e) {
                handleSecretError("Firebase Admin", e);
            }

            // Get Document AI secret
            try {
                this.flsDocumentAiSecret = sdk.Secrets().GetSecret(
                        "FLS_DOCUMENT_AI",
                        projectId,
                        environment,
                        null, null, null, null
                ).getSecretValue();
                log.info("Successfully loaded Document AI secret from Infisical");
            } catch (Exception e) {
                handleSecretError("Document AI", e);
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize Infisical SDK", e);
            useLocalSecrets();
        }
    }
    
    private void handleMissingToken() {
        log.warn("API_TOKEN environment variable is missing. Using fallback secrets if available.");
        useLocalSecrets();
    }
    
    private void handleSecretError(String secretName, Exception e) {
        log.error("Failed to retrieve {} secret from Infisical: {}", secretName, e.getMessage());
        useLocalSecrets();
    }
    
    private void useLocalSecrets() {
        if (firebaseAdminSecret == null || firebaseAdminSecret.isBlank()) {
            firebaseAdminSecret = fallbackFirebaseAdminSecret;
            log.info("Using local fallback for Firebase Admin secret");
        }
        
        if (flsDocumentAiSecret == null || flsDocumentAiSecret.isBlank()) {
            flsDocumentAiSecret = fallbackDocumentAiSecret;
            log.info("Using local fallback for Document AI secret");
        }
    }

    @Bean
    public String firebaseAdminSecret() {
        if (firebaseAdminSecret == null || firebaseAdminSecret.isBlank()) {
            log.warn("Firebase Admin secret is not available");
        }
        return firebaseAdminSecret;
    }

    @Bean
    public String flsDocumentAiSecret() {
        if (flsDocumentAiSecret == null || flsDocumentAiSecret.isBlank()) {
            log.warn("Document AI secret is not available");
        }
        return flsDocumentAiSecret;
    }
}
