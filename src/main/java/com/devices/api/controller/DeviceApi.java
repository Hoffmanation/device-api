package com.devices.api.controller;

import com.devices.api.model.CommandAcceptedResponse;
import com.devices.api.model.CreateDeviceRequest;
import com.devices.api.model.DeviceResponse;
import com.devices.api.model.UpdateDeviceRequest;
import com.devices.domain.model.DeviceState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Devices", description = "CRUD API for managing device resources")
@RequestMapping("/api/v1/devices")
public interface DeviceApi {

    @PostMapping
    @Operation(summary = "Create a device")
    @ApiResponse(responseCode = "202", description = "Create command accepted", content = @Content(schema = @Schema(implementation = CommandAcceptedResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)))
    @ResponseStatus(HttpStatus.ACCEPTED)
    ResponseEntity<CommandAcceptedResponse> createDevice(@Valid @RequestBody CreateDeviceRequest request);


    @PatchMapping("/{deviceId}")
    @Operation(summary = "Partially update a device")
    @ApiResponse(responseCode = "202", description = "Update command accepted", content = @Content(schema = @Schema(implementation = CommandAcceptedResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)))
    @ResponseStatus(HttpStatus.ACCEPTED)
    ResponseEntity<CommandAcceptedResponse> updateDevice(
        @Parameter(description = "Device identifier") @PathVariable UUID deviceId,
        @Valid @RequestBody UpdateDeviceRequest request
    );

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete a device")
    @ApiResponse(responseCode = "202", description = "Delete command accepted", content = @Content(schema = @Schema(implementation = CommandAcceptedResponse.class)))
    @ResponseStatus(HttpStatus.ACCEPTED)
    ResponseEntity<CommandAcceptedResponse> deleteDevice(@Parameter(description = "Device identifier") @PathVariable UUID deviceId);

    @GetMapping("/{deviceId}")
    @Operation(summary = "Fetch a single device")
    @ApiResponse(responseCode = "200", description = "Device found", content = @Content(schema = @Schema(implementation = DeviceResponse.class)))
    @ApiResponse(responseCode = "404", description = "Device not found", content = @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)))
    ResponseEntity<DeviceResponse> getDevice(@Parameter(description = "Device identifier") @PathVariable UUID deviceId);

    @GetMapping
    @Operation(summary = "Fetch all devices or filter by brand and state, with pagination")
    @ApiResponse(responseCode = "200", description = "Devices found", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviceResponse.class))))
    ResponseEntity<List<DeviceResponse>> getDevices(
        @Parameter(description = "Filter by brand") @RequestParam(required = false) String brand,
        @Parameter(description = "Filter by state") @RequestParam(required = false) DeviceState state,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size);
}

