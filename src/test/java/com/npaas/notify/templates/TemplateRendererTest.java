package com.npaas.notify.templates;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.npaas.notify.jobs.NotificationChannel;

class TemplateRendererTest {

    private final TemplateRenderer templateRenderer = new TemplateRenderer(new ObjectMapper());

    @Test
    void rendersSubjectAndBodyFromPayloadPlaceholders() {
        NotificationTemplate template = new NotificationTemplate(
            UUID.randomUUID(),
            "campuscritique",
            "connect.requested",
            NotificationChannel.EMAIL,
            "connect_email",
            "New connect for {{ collegeName }}",
            "Student {{studentName}} requested {{collegeName}}. Missing: {{unknown}}",
            true
        );

        RenderedTemplate renderedTemplate = templateRenderer.render(
            template,
            """
            {
              "studentName": "Aarav",
              "collegeName": "Newton ADYPU"
            }
            """
        );

        assertThat(renderedTemplate.subject()).isEqualTo("New connect for Newton ADYPU");
        assertThat(renderedTemplate.body())
            .isEqualTo("Student Aarav requested Newton ADYPU. Missing: ");
    }

    @Test
    void usesEmptyPayloadWhenJsonIsInvalid() {
        NotificationTemplate template = new NotificationTemplate(
            UUID.randomUUID(),
            "campuscritique",
            "connect.requested",
            NotificationChannel.IN_APP,
            "connect_in_app",
            "{{collegeName}}",
            "Body {{studentName}}",
            true
        );

        RenderedTemplate renderedTemplate = templateRenderer.render(template, "{not-json");

        assertThat(renderedTemplate.subject()).isEmpty();
        assertThat(renderedTemplate.body()).isEqualTo("Body ");
    }
}
