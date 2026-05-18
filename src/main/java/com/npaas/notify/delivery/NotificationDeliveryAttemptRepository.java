package com.npaas.notify.delivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, UUID> {

    List<NotificationDeliveryAttempt> findByJobIdOrderByAttemptNumberAsc(UUID jobId);

    Optional<NotificationDeliveryAttempt> findFirstByJobIdOrderByAttemptNumberDesc(UUID jobId);
}
