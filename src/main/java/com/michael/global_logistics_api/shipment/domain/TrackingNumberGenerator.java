package com.michael.global_logistics_api.shipment.domain;

import java.util.Locale;
import java.util.UUID;

public final class TrackingNumberGenerator {

    private static final String PREFIX = "TRK-";
    private static final int SUFFIX_LENGTH = 8;

    private TrackingNumberGenerator() {
    }

    public static String generate() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return PREFIX + raw.substring(0, SUFFIX_LENGTH);
    }
}
