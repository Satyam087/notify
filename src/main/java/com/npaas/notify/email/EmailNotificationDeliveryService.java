package com.npaas.notify.email;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

@Service
public class EmailNotificationDeliveryService implements NotificationDeliveryHandler {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final RestClient resendClient;
    private final boolean enabled;
    private final String fromEmail;
    private final String fromName;
    private final String replyToEmail;
    private final String resendApiKey;

    public EmailNotificationDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${notify.email.enabled:false}") boolean enabled,
            @Value("${notify.email.from}") String fromEmail,
            @Value("${notify.email.from-name:CampusCritique}") String fromName,
            @Value("${notify.email.reply-to:${notify.email.from}}") String replyToEmail,
            @Value("${notify.email.resend.api-key:}") String resendApiKey,
            @Value("${notify.email.resend.base-url:https://api.resend.com}") String resendBaseUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.objectMapper = objectMapper;
        this.resendClient = restClientBuilder.baseUrl(resendBaseUrl).build();
        this.enabled = enabled;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.replyToEmail = replyToEmail;
        this.resendApiKey = resendApiKey;
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

        String recipientEmail = extractRecipientEmail(event.getRecipient())
            .orElseThrow(() -> new DeliveryException("Missing recipient.email for email notification", false));
        EmailBody emailBody = resolveEmailBody(job, event);

        if (hasText(resendApiKey)) {
            return deliverWithResend(job, recipientEmail, emailBody);
        }

        return deliverWithSmtp(job, recipientEmail, emailBody);
    }

    private DeliveryResult deliverWithResend(NotificationJob job, String recipientEmail, EmailBody emailBody) {
        ResendEmailRequest request = new ResendEmailRequest(
            formatSender(),
            List.of(recipientEmail),
            job.getRenderedSubject(),
            emailBody.text(),
            emailBody.html(),
            replyToEmail
        );

        try {
            ResendEmailResponse response = resendClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ResendEmailResponse.class);

            return new DeliveryResult("resend", response == null ? null : response.id());
        } catch (RestClientResponseException exception) {
            throw new DeliveryException(
                "Resend delivery failed: " + rootMessage(exception),
                isRetryableHttpStatus(exception.getStatusCode()),
                exception
            );
        } catch (RestClientException exception) {
            throw new DeliveryException("Resend delivery failed: " + rootMessage(exception), true, exception);
        }
    }

    private DeliveryResult deliverWithSmtp(NotificationJob job, String recipientEmail, EmailBody emailBody) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new DeliveryException("Email sender is not configured", true);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setReplyTo(replyToEmail);
            helper.setTo(recipientEmail);
            helper.setSubject(job.getRenderedSubject());
            helper.setText(emailBody.body(), emailBody.isHtml());
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

    private String formatSender() {
        if (!hasText(fromName)) {
            return fromEmail;
        }

        return fromName + " <" + fromEmail + ">";
    }

    private boolean isRetryableHttpStatus(HttpStatusCode statusCode) {
        return statusCode.value() == 429 || statusCode.is5xxServerError();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private EmailBody resolveEmailBody(NotificationJob job, NotificationEvent event) {
        return extractEmailHtml(event.getPayload())
            .map(html -> new EmailBody(null, html, true))
            .orElseGet(() -> new EmailBody(job.getRenderedBody(), null, false));
    }

    private Optional<String> extractEmailHtml(String payloadJson) {
        JsonNode payload = parseJson(payloadJson);
        JsonNode emailHtml = payload.get("emailHtml");
        if (emailHtml == null || emailHtml.isNull() || !emailHtml.isTextual() || emailHtml.asText().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(emailHtml.asText());
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

    private record EmailBody(String text, String html, boolean isHtml) {

        String body() {
            return isHtml ? html : text;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ResendEmailRequest(
        String from,
        List<String> to,
        String subject,
        String text,
        String html,
        @JsonProperty("reply_to") String replyTo
    ) {
    }

    private record ResendEmailResponse(String id) {
    }
}
