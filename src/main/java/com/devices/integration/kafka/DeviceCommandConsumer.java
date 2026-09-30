package com.devices.integration.kafka;

import com.devices.domain.event.DeviceEventMesssage;
import com.devices.domain.exception.DeviceNotFoundException;
import com.devices.domain.mapper.DeviceMapper;
import com.devices.domain.model.Device;
import com.devices.ports.jpa.repository.DeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class DeviceCommandConsumer {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${app.kafka.topics.device-commands}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(String message) {
        DeviceEventMesssage event = deserialize(message);
        try {
            switch (event.eventType()) {
                case DEVICE_CREATE_REQUESTED -> createDevice(event);
                case DEVICE_UPDATE_REQUESTED -> updateDevice(event);
                case DEVICE_DELETE_REQUESTED -> deleteDevice(event);
            }

            evictCaches(event.aggregateId());
            log.info(
                "Consumed Kafka device command id={} type={} aggregateId={} traceId={}",
                event.eventId(),
                event.eventType(),
                event.aggregateId(),
                event.traceId()
            );
        } catch (RuntimeException exception) {
            log.atError()
                .setCause(exception)
                .setMessage("Kafka device command processing failed")
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("eventType", event.eventType())
                .addKeyValue("aggregateId", event.aggregateId())
                .addKeyValue("traceId", event.traceId())
                .log();
            throw exception;
        }
    }

    private DeviceEventMesssage deserialize(String message) {
        try {
            return objectMapper.readValue(message, DeviceEventMesssage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize Kafka device command", exception);
        }
    }

    private void createDevice(DeviceEventMesssage event) {
        Device device = Device.builder()
            .id(event.aggregateId())
            .name(event.name())
            .brand(event.brand())
            .state(event.state())
            // created in the DB but we dont want to insert null instead
            .createdAt(Instant.now())
            .updatedAt(event.updatedAt() != null ? event.updatedAt() : Instant.now())
            .build();

        deviceRepository.save(deviceMapper.toEntity(device));
    }

    private void updateDevice(DeviceEventMesssage event) {
        Device existingDevice = getDomainById(event.aggregateId());
        Device updatedDevice = Device.builder()
            .id(event.aggregateId())
            .name(event.name())
            .brand(event.brand())
            .state(event.state())
            .createdAt(existingDevice.createdAt())
            .updatedAt(event.updatedAt() != null ? event.updatedAt() : Instant.now())
            .build();

        deviceRepository.save(deviceMapper.toEntity(updatedDevice));
    }

    private void deleteDevice(DeviceEventMesssage event) {
        deviceRepository.deleteById(event.aggregateId());
    }

    private Device getDomainById(UUID deviceId) {
        return deviceRepository.findById(deviceId)
            .map(deviceMapper::toDomain)
            .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }


    private void evictCaches(UUID deviceId) {
        Cache devicesById = cacheManager.getCache("devicesById");
        if (devicesById != null) {
            devicesById.evict(deviceId);
        }

        Cache devicesSearch = cacheManager.getCache("devicesSearch");
        if (devicesSearch != null) {
            devicesSearch.clear();
        }
    }
}


