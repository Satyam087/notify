package com.npaas.notify.delivery;

import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.jobs.NotificationJob;

record ClaimedNotificationJob(NotificationJob job, NotificationEvent event, int attemptNumber) {
}
