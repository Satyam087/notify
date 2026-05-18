package com.npaas.notify.email;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class EmailConfigurationValidatorTest {

    @Test
    void doesNothingWhenEmailIsDisabled() {
        EmailConfigurationValidator validator = new EmailConfigurationValidator(
            false,
            "",
            "",
            "",
            "",
            ""
        );

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
            .doesNotThrowAnyException();
    }

    @Test
    void failsFastWhenEmailIsEnabledWithoutResendOrSmtpConfig() {
        EmailConfigurationValidator validator = new EmailConfigurationValidator(
            true,
            "connect@campuscritique.in",
            "",
            "",
            "resend",
            ""
        );

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NOTIFY_EMAIL_RESEND_API_KEY")
            .hasMessageContaining("SMTP");
    }

    @Test
    void allowsStartupWhenResendApiKeyIsConfigured() {
        EmailConfigurationValidator validator = new EmailConfigurationValidator(
            true,
            "connect@campuscritique.in",
            "re_test_key",
            "",
            "",
            ""
        );

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
            .doesNotThrowAnyException();
    }

    @Test
    void allowsStartupWhenSmtpConfigIsComplete() {
        EmailConfigurationValidator validator = new EmailConfigurationValidator(
            true,
            "connect@campuscritique.in",
            "",
            "smtp.resend.com",
            "resend",
            "re_test_key"
        );

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
            .doesNotThrowAnyException();
    }
}
