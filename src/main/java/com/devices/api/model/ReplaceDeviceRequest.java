package com.devices.api.model;

import com.devices.domain.model.DeviceState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "ReplaceDeviceRequest", description = "Payload for fully replacing an existing device")
public class ReplaceDeviceRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 255, message = "name must be at most 255 characters")
    @Schema(example = "5G Edge Router X2")
    private String name;

    @NotBlank(message = "brand must not be blank")
    @Size(max = 255, message = "brand must be at most 255 characters")
    @Schema(example = "Nokia")
    private String brand;

    @NotNull(message = "state must not be null")
    @Schema(example = "IN_USE", implementation = DeviceState.class)
    private DeviceState state;
}

