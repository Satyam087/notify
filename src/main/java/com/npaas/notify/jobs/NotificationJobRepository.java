package com.npaas.notify.jobs;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {

    boolean existsByEventIdAndChannel(UUID eventId, NotificationChannel channel);
}
