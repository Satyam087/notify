package com.npaas.notify.push;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

@Configuration
@ConditionalOnProperty(name = "notify.push.enabled", havingValue = "true")
public class FirebasePushConfiguration {

    @Bean
    FirebaseApp firebaseApp(
            @Value("${notify.push.firebase.service-account-base64:}") String serviceAccountBase64,
            @Value("${notify.push.firebase.service-account-json:}") String serviceAccountJson) throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        String json = resolveServiceAccountJson(serviceAccountBase64, serviceAccountJson);
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        );

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private String resolveServiceAccountJson(String serviceAccountBase64, String serviceAccountJson) {
        if (serviceAccountBase64 != null && !serviceAccountBase64.isBlank()) {
            return new String(Base64.getDecoder().decode(serviceAccountBase64.trim()), StandardCharsets.UTF_8);
        }
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return serviceAccountJson;
        }

        throw new IllegalStateException("Firebase service account is not configured");
    }
}
