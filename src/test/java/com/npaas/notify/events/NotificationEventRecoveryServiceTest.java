package com.npaas.notify.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationEventRecoveryServiceTest {

    @Mock
    private NotificationEventRepository notificationEventRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private NotificationEventRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new NotificationEventRecoveryService(
            notificationEventRepository,
            notificationEventPublisher
        );
    }

    @Test
    void republishesStaleQueuedEvents() {
        NotificationEvent event = new NotificationEvent(
            UUID.randomUUID(),
            "campuscritique",
            "connect.requested",
            "connect-001",
            "{}",
            "{}",
            NotificationEventStatus.QUEUED
        );

        when(notificationEventRepository.findByStatusAndUpdatedAtBeforeOrderByCreatedAtAsc(
            org.mockito.ArgumentMatchers.eq(NotificationEventStatus.QUEUED),
            org.mockito.ArgumentMatchers.any(Instant.class),
            org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 25))
        )).thenReturn(List.of(event));

        int republished = recoveryService.republishStaleQueuedEvents(Duration.ofMinutes(2), 25);

        assertThat(republished).isEqualTo(1);
        verify(notificationEventPublisher).publish(event);
    }
}
