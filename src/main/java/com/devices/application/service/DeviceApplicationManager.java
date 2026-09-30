package com.devices.application.service;

import com.devices.api.model.CommandAcceptedResponse;
import com.devices.api.model.CreateDeviceRequest;
import com.devices.api.model.UpdateDeviceRequest;
import com.devices.application.port.DeviceCommandPublisher;
import com.devices.domain.event.DeviceEventMesssage;
import com.devices.domain.event.DeviceEventType;
import com.devices.domain.exception.DeviceNotFoundException;
import com.devices.domain.mapper.DeviceMapper;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import com.devices.domain.service.DevicePolicyValidator;
import com.devices.ports.jpa.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceApplicationManager {

    private static final String TRACE_ID = "traceId";
    private static final String AGGREGATE_TYPE = "device";

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final DeviceCommandPublisher deviceCommandPublisher;
    private final DevicePolicyValidator devicePolicyValidator;

    public CommandAcceptedResponse createDevice(CreateDeviceRequest request) {
        Instant acceptedAt = Instant.now();
        return publishDeviceEvent(
            UUID.randomUUID(),
            DeviceEventType.DEVICE_CREATE_REQUESTED,
            request.getName(),
            request.getBrand(),
            request.getState(),
            acceptedAt,
            acceptedAt
        );
    }

    public CommandAcceptedResponse updateDevice(UUID deviceId, UpdateDeviceRequest request) {
        Device existingDevice = getDomainById(deviceId);
        devicePolicyValidator.validateUpdate(existingDevice, request.getName(), request.getBrand());

        Instant acceptedAt = Instant.now();
        return publishDeviceEvent(
            deviceId,
            DeviceEventType.DEVICE_UPDATE_REQUESTED,
            request.getName() != null ? request.getName() : existingDevice.name(),
            request.getBrand() != null ? request.getBrand() : existingDevice.brand(),
            request.getState() != null ? request.getState() : existingDevice.state(),
            acceptedAt,
            acceptedAt
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "devicesById", key = "#deviceId")
    public Device getDevice(UUID deviceId) {
        log.trace("Cache miss for device {}", deviceId);
        return getDomainById(deviceId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "devicesSearch", key = "(#brand ?: 'null') + '|' + (#state ?: 'null') + '|' + #page + '|' + #size")
    public List<Device> getDevices(String brand, DeviceState state, int page, int size) {
        log.info("Cache miss for device search. brand={}, state={}", brand, state);

        record DeviceFilter(String brand, DeviceState state) {
        }

        var entities = switch (new DeviceFilter(brand, state)) {
            case DeviceFilter(var requestedBrand, var requestedState)
                when requestedBrand != null && requestedState != null ->
                deviceRepository.findAllByBrandIgnoreCaseAndStateOrderByCreatedAtDesc(requestedBrand, requestedState, PageRequest.of(page, size));
            case DeviceFilter(var requestedBrand, _)
                when requestedBrand != null ->
                deviceRepository.findAllByBrandIgnoreCaseOrderByCreatedAtDesc(requestedBrand, PageRequest.of(page, size));
            case DeviceFilter(_, var requestedState)
                when requestedState != null ->
                deviceRepository.findAllByStateOrderByCreatedAtDesc(requestedState, PageRequest.of(page, size));
            default -> deviceRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        };

        return deviceMapper.toDomains(entities.getContent());
    }

    public CommandAcceptedResponse deleteDevice(UUID deviceId) {
        Device existingDevice = getDomainById(deviceId);
        devicePolicyValidator.validateDeletion(existingDevice);
        Instant acceptedAt = Instant.now();
        return publishDeviceEvent(deviceId, DeviceEventType.DEVICE_DELETE_REQUESTED, null, null, null, null, acceptedAt);
    }

    private Device getDomainById(UUID deviceId) {
        return deviceRepository.findById(deviceId)
            .map(deviceMapper::toDomain)
            .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    private CommandAcceptedResponse publishDeviceEvent(
        UUID deviceId,
        DeviceEventType eventType,
        String name,
        String brand,
        DeviceState state,
        Instant updatedAt,
        Instant acceptedAt
    ) {
        UUID commandId = UUID.randomUUID();

        DeviceEventMesssage envelope = DeviceEventMesssage.builder()
            .eventId(commandId)
            .aggregateType(AGGREGATE_TYPE)
            .aggregateId(deviceId)
            .eventType(eventType)
            .occurredAt(acceptedAt)
            .traceId(MDC.get(TRACE_ID))
            .name(name)
            .brand(brand)
            .state(state)
            .updatedAt(updatedAt)
            .build();

        deviceCommandPublisher.publish(envelope);
        log.info("Accepted Kafka device command {} type={} aggregateId={}", commandId, eventType, deviceId);

        return CommandAcceptedResponse.builder()
            .commandId(commandId)
            .deviceId(deviceId)
            .commandType(eventType.name())
            .acceptedAt(acceptedAt)
            .build();
    }
}

