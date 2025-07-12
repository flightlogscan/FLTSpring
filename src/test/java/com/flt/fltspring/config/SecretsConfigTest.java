package com.flt.fltspring.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretsConfigTest {
    @Test
    void initializeSecrets_usesFallback() {
        SecretsConfig cfg = new SecretsConfig();
        setField(cfg, "fallbackFirebaseAdminSecret", "fb");
        setField(cfg, "fallbackDocumentAiSecret", "doc");
        cfg.initializeSecrets();
        assertThat(cfg.firebaseAdminSecret()).isEqualTo("fb");
        assertThat(cfg.flsDocumentAiSecret()).isEqualTo("doc");
    }

    private static void setField(Object obj, String name, Object val) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
