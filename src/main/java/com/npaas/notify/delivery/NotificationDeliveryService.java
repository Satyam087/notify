package com.npaas.notify.delivery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;
import com.npaas.notify.jobs.NotificationJobRepository;
import com.npaas.notify.jobs.NotificationJobStatus;

import jakarta.annotation.PreDestroy;

@Service
public class NotificationDeliveryService {

    private static final AtomicInteger DELIVERY_THREAD_COUNTER = new AtomicInteger();

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationDeliveryTransactionService transactionService;
    private final Map<NotificationChannel, NotificationDeliveryHandler> handlers;
    private final ExecutorService deliveryExecutor;
    private final long handlerTimeoutSeconds;

    public NotificationDeliveryService(
            NotificationJobRepository notificationJobRepository,
            NotificationDeliveryTransactionService transactionService,
            List<NotificationDeliveryHandler> deliveryHandlers,
            @Value("${notify.delivery.handler-timeout-seconds:20}") long handlerTimeoutSeconds) {
        this.notificationJobRepository = notificationJobRepository;
        this.transactionService = transactionService;
        this.handlers = toHandlerMap(deliveryHandlers);
        this.handlerTimeoutSeconds = Math.max(1, handlerTimeoutSeconds);
        this.deliveryExecutor = Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setName("notification-delivery-" + DELIVERY_THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    void shutdown() {
        deliveryExecutor.shutdownNow();
    }

    public int deliverDueJobs(int batchSize) {
        transactionService.recoverStaleProcessingJobs(batchSize);
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
            DeliveryResult result = deliverWithTimeout(handler, claimedJob);
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

    private DeliveryResult deliverWithTimeout(
            NotificationDeliveryHandler handler,
            ClaimedNotificationJob claimedJob) {
        Future<DeliveryResult> delivery = deliveryExecutor.submit(
            () -> handler.deliver(claimedJob.job(), claimedJob.event())
        );

        try {
            return delivery.get(handlerTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            delivery.cancel(true);
            throw new DeliveryException(
                "Delivery handler timed out after " + handlerTimeoutSeconds + " seconds",
                true,
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DeliveryException("Delivery handler interrupted", true, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof DeliveryException deliveryException) {
                throw deliveryException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new DeliveryException("Unexpected delivery error", true, exception);
        }
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
