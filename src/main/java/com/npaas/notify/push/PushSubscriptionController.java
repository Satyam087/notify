package com.npaas.notify.push;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/push-subscriptions")
@Validated
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    public PushSubscriptionController(PushSubscriptionService pushSubscriptionService) {
        this.pushSubscriptionService = pushSubscriptionService;
    }

    @PostMapping
    public PushSubscriptionResponse upsert(@Valid @RequestBody UpsertPushSubscriptionRequest request) {
        return pushSubscriptionService.upsert(request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(@Valid @RequestBody DeletePushSubscriptionRequest request) {
        pushSubscriptionService.deactivate(request);
        return ResponseEntity.noContent().build();
    }
}
