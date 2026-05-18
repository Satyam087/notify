package com.npaas.notify.email;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.npaas.notify.delivery.DeliveryException;
import com.npaas.notify.delivery.DeliveryResult;
import com.npaas.notify.delivery.NotificationDeliveryHandler;
import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailNotificationDeliveryService implements NotificationDeliveryHandler {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String fromEmail;
    private final String fromName;
    private final String replyToEmail;

    public EmailNotificationDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            ObjectMapper objectMapper,
            @Value("${notify.email.enabled:false}") boolean enabled,
            @Value("${notify.email.from}") String fromEmail,
            @Value("${notify.email.from-name:CampusCritique}") String fromName,
            @Value("${notify.email.reply-to:${notify.email.from}}") String replyToEmail) {
        this.mailSenderProvider = mailSenderProvider;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.replyToEmail = replyToEmail;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult deliver(NotificationJob job, NotificationEvent event) {
        if (!enabled) {
            throw new DeliveryException("Email delivery is disabled", false);
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new DeliveryException("Email sender is not configured", true);
        }

        String recipientEmail = extractRecipientEmail(event.getRecipient())
            .orElseThrow(() -> new DeliveryException("Missing recipient.email for email notification", false));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setReplyTo(replyToEmail);
            helper.setTo(recipientEmail);
            helper.setSubject(job.getRenderedSubject());
            helper.setText(job.getRenderedBody(), false);
            mailSender.send(message);
            return DeliveryResult.delivered("smtp");
        } catch (MessagingException exception) {
            throw new DeliveryException("Could not build email message", false, exception);
        } catch (MailException exception) {
            throw new DeliveryException("SMTP delivery failed: " + rootMessage(exception), true, exception);
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new DeliveryException("Invalid email sender name", false, exception);
        }
    }

    private String rootMessage(Throwable exception) {
        Throwable cursor = exception;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }

        String message = cursor.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private Optional<String> extractRecipientEmail(String recipientJson) {
        JsonNode recipient = parseJson(recipientJson);
        JsonNode email = recipient.get("email");
        if (email == null || email.isNull() || !email.isTextual() || email.asText().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(email.asText());
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return objectMapper.valueToTree(Map.of());
        }
    }
}
