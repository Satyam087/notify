package com.npaas.notify.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

class EmailNotificationDeliveryServiceSenderTest {

    @SuppressWarnings("unchecked")
    private final EmailNotificationDeliveryService service = new EmailNotificationDeliveryService(
        (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class),
        new ObjectMapper(),
        RestClient.builder(),
        true,
        "connect@example.com",
        "CampusCritique",
        "connect@example.com",
        "",
        "https://api.resend.test"
    );

    @Test
    void fallsBackToConfiguredSenderWhenPayloadHasNoOverrides() {
        EmailNotificationDeliveryService.EmailSender sender = service.resolveSender("{\"name\":\"x\"}");

        assertThat(sender.fromName()).isEqualTo("CampusCritique");
        assertThat(sender.replyTo()).isEqualTo("connect@example.com");
    }

    @Test
    void usesPayloadDisplayNameAndReplyToWhenValid() {
        EmailNotificationDeliveryService.EmailSender sender = service.resolveSender(
            "{\"fromName\":\"satyamkumarsingh.com\",\"replyTo\":\"visitor@example.org\"}");

        assertThat(sender.fromName()).isEqualTo("satyamkumarsingh.com");
        assertThat(sender.replyTo()).isEqualTo("visitor@example.org");
    }

    @Test
    void stripsHeaderInjectionFromDisplayNameAndRejectsBadReplyTo() {
        EmailNotificationDeliveryService.EmailSender sender = service.resolveSender(
            "{\"fromName\":\"Eve <evil@example.org>\\r\\nBcc: x\",\"replyTo\":\"not an address\"}");

        assertThat(sender.fromName()).doesNotContain("<", ">", "\r", "\n");
        assertThat(sender.replyTo()).isEqualTo("connect@example.com");
    }
}
