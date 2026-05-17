package com.npaas.notify.delivery;

public record DeliveryResult(String provider, String providerMessageId) {

    public static DeliveryResult delivered(String provider) {
        return new DeliveryResult(provider, null);
    }
}
