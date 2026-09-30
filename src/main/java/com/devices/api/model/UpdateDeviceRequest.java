package com.devices.api.model;

import com.devices.domain.model.DeviceState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UpdateDeviceRequest", description = "Payload for partially updating an existing device")
public class UpdateDeviceRequest {

    @Size(max = 255, message = "name must be at most 255 characters")
    @Schema(example = "Industrial LTE Router Pro")
    private String name;

    @Size(max = 255, message = "brand must be at most 255 characters")
    @Schema(example = "Samsung")
    private String brand;

    @Schema(example = "INACTIVE", implementation = DeviceState.class)
    private DeviceState state;
}

