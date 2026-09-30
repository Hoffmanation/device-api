package com.devices.api.model;

import com.devices.domain.model.DeviceState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateDeviceRequest", description = "Payload for creating a new device")
public class CreateDeviceRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 255, message = "name must be at most 255 characters")
    @Schema(example = "eSIM Gateway X1")
    private String name;

    @NotBlank(message = "brand must not be blank")
    @Size(max = 255, message = "brand must be at most 255 characters")
    @Schema(example = "Motorola")
    private String brand;

    @NotNull(message = "state must not be null")
    @Schema(example = "AVAILABLE", implementation = DeviceState.class)
    private DeviceState state;
}

