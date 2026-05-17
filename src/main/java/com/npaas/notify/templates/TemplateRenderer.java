package com.npaas.notify.templates;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final ObjectMapper objectMapper;

    public TemplateRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RenderedTemplate render(NotificationTemplate template, String payloadJson) {
        JsonNode payload = parsePayload(payloadJson);
        return new RenderedTemplate(
            renderText(template.getSubjectTemplate(), payload),
            renderText(template.getBodyTemplate(), payload)
        );
    }

    private String renderText(String templateText, JsonNode payload) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateText);
        StringBuffer rendered = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = extractValue(payload, key);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String extractValue(JsonNode payload, String key) {
        JsonNode value = payload.get(key);
        if (value == null || value.isNull()) {
            return "";
        }

        if (value.isTextual()) {
            return value.asText();
        }

        return value.toString();
    }

    private JsonNode parsePayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            return objectMapper.valueToTree(Map.of());
        }
    }
}
