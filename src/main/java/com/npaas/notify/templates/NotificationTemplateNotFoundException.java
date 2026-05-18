package com.npaas.notify.templates;

public class NotificationTemplateNotFoundException extends RuntimeException {

    public NotificationTemplateNotFoundException(String tenantId, String templateKey) {
        super("Template not found for tenant " + tenantId + " and key " + templateKey);
    }
}
