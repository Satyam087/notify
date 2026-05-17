package com.npaas.notify.delivery;

import com.npaas.notify.events.NotificationEvent;
import com.npaas.notify.jobs.NotificationChannel;
import com.npaas.notify.jobs.NotificationJob;

public interface NotificationDeliveryHandler {

    NotificationChannel channel();

    DeliveryResult deliver(NotificationJob job, NotificationEvent event);
}
