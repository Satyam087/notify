package com.npaas.notify.delivery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;
import com.npaas.notify.jobs.NotificationJobRepository;
import com.npaas.notify.jobs.NotificationJobStatus;

@Service
public class NotificationDeliveryService {

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationDeliveryTransactionService transactionService;
    private final Map<NotificationChannel, NotificationDeliveryHandler> handlers;

    public NotificationDeliveryService(
            NotificationJobRepository notificationJobRepository,
            NotificationDeliveryTransactionService transactionService,
            List<NotificationDeliveryHandler> deliveryHandlers) {
        this.notificationJobRepository = notificationJobRepository;
        this.transactionService = transactionService;
        this.handlers = toHandlerMap(deliveryHandlers);
    }

    public int deliverDueJobs(int batchSize) {
        List<UUID> jobIds = findDueJobIds(batchSize);
        int claimedJobs = 0;

        for (UUID jobId : jobIds) {
            claimedJobs += deliver(jobId);
        }

        return claimedJobs;
    }

    private List<UUID> findDueJobIds(int batchSize) {
        List<NotificationJob> jobs = new ArrayList<>();
        jobs.addAll(notificationJobRepository.findByStatusAndNextAttemptAtIsNullOrderByCreatedAtAsc(
            NotificationJobStatus.PENDING,
            PageRequest.of(0, batchSize)
        ));

        int remaining = batchSize - jobs.size();
        if (remaining > 0) {
            jobs.addAll(notificationJobRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                NotificationJobStatus.PENDING,
                Instant.now(),
                PageRequest.of(0, remaining)
            ));
        }

        return jobs.stream().map(NotificationJob::getId).toList();
    }

    private int deliver(UUID jobId) {
        ClaimedNotificationJob claimedJob = transactionService.claim(jobId).orElse(null);
        if (claimedJob == null) {
            return 0;
        }

        NotificationDeliveryHandler handler = handlers.get(claimedJob.job().getChannel());
        if (handler == null) {
            transactionService.recordFailure(
                claimedJob,
                "none",
                "No delivery handler configured for channel " + claimedJob.job().getChannel(),
                false
            );
            return 1;
        }

        try {
            DeliveryResult result = handler.deliver(claimedJob.job(), claimedJob.event());
            transactionService.recordSuccess(claimedJob, result);
        } catch (DeliveryException exception) {
            transactionService.recordFailure(
                claimedJob,
                providerName(handler),
                exception.getMessage(),
                exception.isRetryable()
            );
        } catch (RuntimeException exception) {
            transactionService.recordFailure(
                claimedJob,
                providerName(handler),
                "Unexpected delivery error",
                true
            );
        }

        return 1;
    }

    private String providerName(NotificationDeliveryHandler handler) {
        return handler == null ? "none" : handler.channel().name().toLowerCase();
    }

    private Map<NotificationChannel, NotificationDeliveryHandler> toHandlerMap(
            List<NotificationDeliveryHandler> deliveryHandlers) {
        Map<NotificationChannel, NotificationDeliveryHandler> handlerMap = new EnumMap<>(NotificationChannel.class);
        for (NotificationDeliveryHandler handler : deliveryHandlers) {
            handlerMap.put(handler.channel(), handler);
        }

        return handlerMap;
    }
}
