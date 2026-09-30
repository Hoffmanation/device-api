package com.devices.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record Device(
    UUID id,
    String name,
    String brand,
    DeviceState state,
    Instant createdAt,
    Instant updatedAt
) {
}

