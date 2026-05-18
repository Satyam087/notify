package com.npaas.notify.email;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EmailConfigurationValidator implements ApplicationRunner {

    private final boolean enabled;
    private final String fromEmail;
    private final String resendApiKey;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;

    public EmailConfigurationValidator(
            @Value("${notify.email.enabled:false}") boolean enabled,
            @Value("${notify.email.from:}") String fromEmail,
            @Value("${notify.email.resend.api-key:}") String resendApiKey,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword) {
        this.enabled = enabled;
        this.fromEmail = fromEmail;
        this.resendApiKey = resendApiKey;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        List<String> missing = new ArrayList<>();
        addIfBlank(missing, "NOTIFY_EMAIL_FROM", fromEmail);

        if (!hasText(resendApiKey) && !hasSmtpConfig()) {
            missing.add("NOTIFY_EMAIL_RESEND_API_KEY or complete SMTP configuration");
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Email delivery is enabled but required email configuration is missing: " + String.join(", ", missing)
            );
        }
    }

    private boolean hasSmtpConfig() {
        return hasText(mailHost) && hasText(mailUsername) && hasText(mailPassword);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void addIfBlank(List<String> missing, String name, String value) {
        if (!hasText(value)) {
            missing.add(name);
        }
    }
}
