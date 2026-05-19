package com.npaas.notify.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.events.NotificationEventStatus;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;
import com.npaas.notify.jobs.NotificationJobRepository;
import com.npaas.notify.jobs.NotificationJobStatus;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationJobRepository notificationJobRepository;

    @Mock
    private NotificationDeliveryTransactionService transactionService;

    @Mock
    private NotificationDeliveryHandler handler;

    private NotificationDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        when(handler.channel()).thenReturn(NotificationChannel.EMAIL);
        deliveryService = new NotificationDeliveryService(
            notificationJobRepository,
            transactionService,
            List.of(handler),
            5
        );
    }

    @Test
    void deliversClaimedJobsAndFinalizesSuccessSeparately() {
        UUID jobId = UUID.randomUUID();
        NotificationJob job = job(jobId);
        NotificationEvent event = event(job.getEventId());
        ClaimedNotificationJob claimedJob = new ClaimedNotificationJob(job, event, 1);
        DeliveryResult result = new DeliveryResult("email", "message-1");

        when(notificationJobRepository.findByStatusAndNextAttemptAtIsNullOrderByCreatedAtAsc(
            NotificationJobStatus.PENDING,
            PageRequest.of(0, 1)
        )).thenReturn(List.of(job));
        when(transactionService.claim(jobId)).thenReturn(Optional.of(claimedJob));
        when(handler.deliver(job, event)).thenReturn(result);

        int delivered = deliveryService.deliverDueJobs(1);

        assertThat(delivered).isEqualTo(1);
        verify(transactionService).recordSuccess(claimedJob, result);
    }

    @Test
    void recordsRetryableFailureWhenHandlerThrowsUnexpectedError() {
        UUID jobId = UUID.randomUUID();
        NotificationJob job = job(jobId);
        NotificationEvent event = event(job.getEventId());
        ClaimedNotificationJob claimedJob = new ClaimedNotificationJob(job, event, 1);

        when(notificationJobRepository.findByStatusAndNextAttemptAtIsNullOrderByCreatedAtAsc(
            NotificationJobStatus.PENDING,
            PageRequest.of(0, 1)
        )).thenReturn(List.of(job));
        when(transactionService.claim(jobId)).thenReturn(Optional.of(claimedJob));
        when(handler.deliver(job, event)).thenThrow(new IllegalStateException("smtp down"));

        int delivered = deliveryService.deliverDueJobs(1);

        assertThat(delivered).isEqualTo(1);
        verify(transactionService).recordFailure(claimedJob, "email", "Unexpected delivery error", true);
    }

    private NotificationJob job(UUID jobId) {
        return new NotificationJob(
            jobId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "campuscritique",
            NotificationChannel.EMAIL,
            NotificationJobStatus.PENDING,
            "Subject",
            "Body"
        );
    }

    private NotificationEvent event(UUID eventId) {
        return new NotificationEvent(
            eventId,
            "campuscritique",
            "connect.requested",
            "connect-001",
            "{}",
            "{}",
            NotificationEventStatus.QUEUED
        );
    }
}
