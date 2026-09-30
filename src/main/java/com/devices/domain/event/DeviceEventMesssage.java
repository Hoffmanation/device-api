package com.devices.domain.event;

import com.devices.domain.model.DeviceState;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record DeviceEventMesssage(
    UUID eventId,
    String aggregateType,
    UUID aggregateId,
    DeviceEventType eventType,
    Instant occurredAt,
    String traceId,
    String name,
    String brand,
    DeviceState state,
    Instant updatedAt
) {
}

