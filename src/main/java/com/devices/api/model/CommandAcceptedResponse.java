package com.devices.api.model;

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
@Schema(name = "CommandAcceptedResponse", description = "Acknowledgement returned when a device write command was accepted for asynchronous processing")
public class CommandAcceptedResponse {

    @Schema(example = "3e7c5d5c-7b24-4b6a-8d5d-fdff826ff825", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID commandId;

    @Schema(example = "fd6b6e70-8d0b-4dda-bab5-2320d2ad4d39", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID deviceId;

    @Schema(example = "DEVICE_CREATE_REQUESTED", accessMode = Schema.AccessMode.READ_ONLY)
    private String commandType;

    @Schema(example = "2026-09-28T09:00:00Z", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant acceptedAt;
}

