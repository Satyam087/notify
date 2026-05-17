package com.npaas.notify.events;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationEventService {

    private final NotificationEventRepository notificationEventRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final ObjectMapper objectMapper;

    public NotificationEventService(
            NotificationEventRepository notificationEventRepository,
            NotificationEventPublisher notificationEventPublisher,
            ObjectMapper objectMapper) {
        this.notificationEventRepository = notificationEventRepository;
        this.notificationEventPublisher = notificationEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestEventResponse ingest(IngestEventRequest request) {
        return notificationEventRepository
            .findByTenantSlugAndIdempotencyKey(request.tenantId(), request.idempotencyKey())
            .map(existingEvent -> toResponse(existingEvent, true))
            .orElseGet(() -> createEvent(request));
    }

    private IngestEventResponse createEvent(IngestEventRequest request) {
        NotificationEvent event = new NotificationEvent(
            UUID.randomUUID(),
            request.tenantId(),
            request.eventType(),
            request.idempotencyKey(),
            serializeJson(request.recipient()),
            serializeJson(request.payload()),
            NotificationEventStatus.RECEIVED
        );

        NotificationEvent savedEvent = notificationEventRepository.save(event);
        savedEvent.markQueued();
        publishAfterCommit(savedEvent);
        return toResponse(savedEvent, false);
    }

    private void publishAfterCommit(NotificationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationEventPublisher.publish(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationEventPublisher.publish(event);
            }
        });
    }

    private IngestEventResponse toResponse(NotificationEvent event, boolean duplicate) {
        return new IngestEventResponse(
            event.getId(),
            event.getTenantSlug(),
            event.getEventType(),
            event.getStatus().name(),
            duplicate,
            event.getCreatedAt()
        );
    }

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid JSON payload", exception);
        }
    }
}
