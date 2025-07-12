package com.flt.fltspring.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;

import static org.mockito.Mockito.mockStatic;
import static org.assertj.core.api.Assertions.assertThat;

class FirebaseConfigTest {
    @Test
    void initialize_invokesFirebaseApp() throws Throwable {
        FirebaseConfig cfg = new FirebaseConfig();
        java.lang.reflect.Field field = FirebaseConfig.class.getDeclaredField("firebaseAdminSecret");
        field.setAccessible(true);
        field.set(cfg, "{}" );

        GoogleCredentials creds = Mockito.mock(GoogleCredentials.class);
        try (MockedStatic<GoogleCredentials> gc = Mockito.mockStatic(GoogleCredentials.class); 
             MockedStatic<FirebaseApp> app = Mockito.mockStatic(FirebaseApp.class)) {
            gc.when(() -> GoogleCredentials.fromStream(Mockito.any(ByteArrayInputStream.class))).thenReturn(creds);
            app.when(() -> FirebaseApp.initializeApp(Mockito.any(FirebaseOptions.class))).thenReturn(null);

            cfg.initialize();
            app.verify(() -> FirebaseApp.initializeApp(Mockito.any(FirebaseOptions.class)));
        }
    }
}
