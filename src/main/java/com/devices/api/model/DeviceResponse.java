package com.devices.api.model;

import com.devices.domain.model.DeviceState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DeviceResponse", description = "Representation of a device resource")
public class DeviceResponse {

    @Schema(example = "fd6b6e70-8d0b-4dda-bab5-2320d2ad4d39", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(example = "eSIM Gateway X1")
    private String name;

    @Schema(example = "Motorola")
    private String brand;

    @Schema(example = "AVAILABLE", implementation = DeviceState.class)
    private DeviceState state;

    @Schema(example = "2026-09-28T09:00:00Z", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;

    @Schema(example = "2026-09-28T09:05:00Z", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
}

