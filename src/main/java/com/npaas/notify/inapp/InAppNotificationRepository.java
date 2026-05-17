package com.npaas.notify.inapp;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    boolean existsByJobId(UUID jobId);
}
