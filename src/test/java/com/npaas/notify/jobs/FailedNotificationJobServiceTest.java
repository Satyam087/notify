package com.npaas.notify.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.npaas.notify.delivery.NotificationDeliveryAttempt;
import com.npaas.notify.delivery.NotificationDeliveryAttemptRepository;

@ExtendWith(MockitoExtension.class)
class FailedNotificationJobServiceTest {

    @Mock
    private NotificationJobRepository notificationJobRepository;

    @Mock
    private NotificationDeliveryAttemptRepository deliveryAttemptRepository;

    @Test
    void listsFailedJobsWithLatestErrorAndCapsLimit() {
        UUID jobId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        NotificationJob job = new NotificationJob(
            jobId,
            eventId,
            UUID.randomUUID(),
            "campuscritique",
            NotificationChannel.EMAIL,
            NotificationJobStatus.FAILED,
            "New connect request",
            "Body"
        );
        NotificationDeliveryAttempt failedAttempt = NotificationDeliveryAttempt.failed(
            jobId,
            "campuscritique",
            NotificationChannel.EMAIL,
            2,
            "smtp",
            "Mailbox unavailable"
        );
        FailedNotificationJobService service = new FailedNotificationJobService(
            notificationJobRepository,
            deliveryAttemptRepository
        );
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(notificationJobRepository.findByTenantSlugAndStatusOrderByUpdatedAtDesc(
            org.mockito.ArgumentMatchers.eq("campuscritique"),
            org.mockito.ArgumentMatchers.eq(NotificationJobStatus.FAILED),
            pageableCaptor.capture()
        )).thenReturn(List.of(job));
        when(deliveryAttemptRepository.findFirstByJobIdOrderByAttemptNumberDesc(jobId))
            .thenReturn(Optional.of(failedAttempt));

        List<FailedNotificationJobResponse> responses = service.listFailed("campuscritique", 500);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(jobId);
        assertThat(responses.getFirst().eventId()).isEqualTo(eventId);
        assertThat(responses.getFirst().channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(responses.getFirst().lastProvider()).isEqualTo("smtp");
        assertThat(responses.getFirst().lastError()).isEqualTo("Mailbox unavailable");
        verify(deliveryAttemptRepository).findFirstByJobIdOrderByAttemptNumberDesc(jobId);
    }
}
