package com.devices.application.service;

import com.devices.api.model.CommandAcceptedResponse;
import com.devices.api.model.CreateDeviceRequest;
import com.devices.api.model.DeviceResponse;
import com.devices.api.model.UpdateDeviceRequest;
import com.devices.domain.exception.DeviceNotFoundException;
import com.devices.domain.mapper.DeviceMapper;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import com.devices.domain.service.DevicePolicyValidator;
import com.devices.ports.jpa.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceApplicationManager {

    private static final String CREATE_COMMAND = "CREATE_DEVICE";
    private static final String UPDATE_COMMAND = "UPDATE_DEVICE";
    private static final String DELETE_COMMAND = "DELETE_DEVICE";

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final DevicePolicyValidator devicePolicyValidator;

    @Transactional
    @CacheEvict(cacheNames = "devicesSearch", allEntries = true)
    public CommandAcceptedResponse createDevice(CreateDeviceRequest request) {
        Device newDevice = deviceMapper.toDomain(request);
        var savedEntity = deviceRepository.save(deviceMapper.toEntity(newDevice));

        return buildResponse(savedEntity.getId(), CREATE_COMMAND);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "devicesById", key = "#deviceId"),
        @CacheEvict(cacheNames = "devicesSearch", allEntries = true)
    })
    public CommandAcceptedResponse updateDevice(UUID deviceId, UpdateDeviceRequest request) {
        Device existingDevice = getDomainById(deviceId);
        devicePolicyValidator.validateUpdate(existingDevice, request.getName(), request.getBrand());

        Device updatedDevice = new Device(
            existingDevice.id(),
            request.getName() != null ? request.getName() : existingDevice.name(),
            request.getBrand() != null ? request.getBrand() : existingDevice.brand(),
            request.getState() != null ? request.getState() : existingDevice.state(),
            existingDevice.createdAt(),
            Instant.now()
        );

        deviceRepository.save(deviceMapper.toEntity(updatedDevice));

        return buildResponse(deviceId, UPDATE_COMMAND);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "devicesById", key = "#deviceId")
    public DeviceResponse getDevice(UUID deviceId) {
        log.trace("Cache miss for device {}", deviceId);
        return deviceMapper.toResponse(getDomainById(deviceId));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "devicesSearch", key = "(#brand ?: 'null') + '|' + (#state ?: 'null')")
    public List<DeviceResponse> getDevices(String brand, DeviceState state) {
        log.info("Cache miss for device search. brand={}, state={}", brand, state);

        record DeviceFilter(String brand, DeviceState state) {
        }

        var entities = switch (new DeviceFilter(brand, state)) {
            case DeviceFilter(var requestedBrand, var requestedState)
                when requestedBrand != null && requestedState != null ->
                deviceRepository.findAllByBrandIgnoreCaseAndStateOrderByCreatedAtDesc(requestedBrand, requestedState);
            case DeviceFilter(var requestedBrand, _)
                when requestedBrand != null ->
                deviceRepository.findAllByBrandIgnoreCaseOrderByCreatedAtDesc(requestedBrand);
            case DeviceFilter(_, var requestedState)
                when requestedState != null ->
                deviceRepository.findAllByStateOrderByCreatedAtDesc(requestedState);
            default -> deviceRepository.findAllByOrderByCreatedAtDesc();
        };

        return deviceMapper.toResponses(deviceMapper.toDomains(entities));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "devicesById", key = "#deviceId"),
        @CacheEvict(cacheNames = "devicesSearch", allEntries = true)
    })
    public CommandAcceptedResponse deleteDevice(UUID deviceId) {
        Device existingDevice = getDomainById(deviceId);
        devicePolicyValidator.validateDeletion(existingDevice);
        deviceRepository.deleteById(deviceId);

        return buildResponse(deviceId, DELETE_COMMAND);
    }

    private Device getDomainById(UUID deviceId) {
        return deviceRepository.findById(deviceId)
            .map(deviceMapper::toDomain)
            .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    private CommandAcceptedResponse buildResponse(UUID deviceId, String commandType) {
        UUID commandId = UUID.randomUUID();
        log.info("Executed command {} type={} deviceId={}", commandId, commandType, deviceId);

        return CommandAcceptedResponse.builder()
            .commandId(commandId)
            .deviceId(deviceId)
            .commandType(commandType)
            .acceptedAt(Instant.now())
            .build();
    }
}
