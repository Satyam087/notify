package com.npaas.notify.jobs;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationJobWriter {

    private final NotificationJobRepository notificationJobRepository;

    NotificationJobWriter(NotificationJobRepository notificationJobRepository) {
        this.notificationJobRepository = notificationJobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewJob(NotificationJob job) {
        notificationJobRepository.saveAndFlush(job);
    }
}
