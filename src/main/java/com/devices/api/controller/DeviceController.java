package com.devices.api.controller;

import com.devices.api.model.CommandAcceptedResponse;
import com.devices.api.model.CreateDeviceRequest;
import com.devices.api.model.DeviceResponse;
import com.devices.api.model.UpdateDeviceRequest;
import com.devices.application.service.DeviceApplicationManager;
import com.devices.domain.model.DeviceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceApi {

    private final DeviceApplicationManager deviceApplicationManager;

    @Override
    public ResponseEntity<CommandAcceptedResponse> createDevice(CreateDeviceRequest request) {
        CommandAcceptedResponse accepted = deviceApplicationManager.createDevice(request);
        log.info("Accepted create command {} for device {}", accepted.getCommandId(), accepted.getDeviceId());
        return ResponseEntity.accepted()
            .location(URI.create("/api/v1/devices/" + accepted.getDeviceId()))
            .body(accepted);
    }

    @Override
    public ResponseEntity<CommandAcceptedResponse> updateDevice(UUID deviceId, UpdateDeviceRequest request) {
        log.info("Accepting update command for device {}", deviceId);
        var response = deviceApplicationManager.updateDevice(deviceId, request);
        return ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<DeviceResponse> getDevice(UUID deviceId) {
        log.info("Fetching device {}", deviceId);
        var response = deviceApplicationManager.getDevice(deviceId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<DeviceResponse>> getDevices(String brand, DeviceState state) {
        log.info("Fetching devices brand={} state={}", brand, state);
        var responses = deviceApplicationManager.getDevices(brand, state);
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<CommandAcceptedResponse> deleteDevice(UUID deviceId) {
        log.info("Accepting delete command for device {}", deviceId);
        var response = deviceApplicationManager.deleteDevice(deviceId);
        return ResponseEntity.accepted().body(response);
    }
}

