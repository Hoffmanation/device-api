package devices.integration.kafka;

import com.devices.domain.event.DeviceEventMesssage;
import com.devices.domain.event.DeviceEventType;
import com.devices.domain.mapper.DeviceMapper;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import com.devices.integration.kafka.DeviceCommandConsumer;
import com.devices.ports.jpa.entity.DeviceEntity;
import com.devices.ports.jpa.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandConsumerTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache devicesByIdCache;
    @Mock
    private Cache devicesSearchCache;

    private DeviceCommandConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DeviceCommandConsumer(deviceRepository, deviceMapper, cacheManager, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void handle_createRequested_persistsDeviceAndEvictsCaches() {
        UUID deviceId = UUID.randomUUID();
        DeviceEntity entity = new DeviceEntity();
        entity.setId(deviceId);

        when(cacheManager.getCache("devicesById")).thenReturn(devicesByIdCache);
        when(cacheManager.getCache("devicesSearch")).thenReturn(devicesSearchCache);

        DeviceEventMesssage event = DeviceEventMesssage.builder()
            .eventId(UUID.randomUUID())
            .aggregateType("device")
            .aggregateId(deviceId)
            .eventType(DeviceEventType.DEVICE_CREATE_REQUESTED)
            .occurredAt(Instant.now())
            .traceId("trace-123")
            .name("eSIM Gateway X1")
            .brand("Motorola")
            .state(DeviceState.AVAILABLE)
            .updatedAt(Instant.parse("2026-09-28T09:00:00Z"))
            .build();

        when(deviceMapper.toEntity(argThat(device ->
            device != null
                && deviceId.equals(device.id())
                && "eSIM Gateway X1".equals(device.name())
                && "Motorola".equals(device.brand())
                && DeviceState.AVAILABLE == device.state()
        ))).thenReturn(entity);

        assertDoesNotThrow(() -> consumer.consume(json(event)));

        verify(deviceRepository).save(entity);
        verify(devicesByIdCache).evict(deviceId);
        verify(devicesSearchCache).clear();
    }

    @Test
    void handle_updateRequested_updatesOnlyProvidedFields() {
        UUID deviceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-28T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-28T10:00:00Z");
        DeviceEntity savedEntity = inactiveEntity(deviceId, createdAt);
        Device existing = Device.builder()
            .id(deviceId)
            .name("Remote SIM Hub M2M")
            .brand("Nokia")
            .state(DeviceState.AVAILABLE)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build();

        when(cacheManager.getCache("devicesById")).thenReturn(devicesByIdCache);
        when(cacheManager.getCache("devicesSearch")).thenReturn(devicesSearchCache);

        DeviceEventMesssage event = DeviceEventMesssage.builder()
            .eventId(UUID.randomUUID())
            .aggregateType("device")
            .aggregateId(deviceId)
            .eventType(DeviceEventType.DEVICE_UPDATE_REQUESTED)
            .occurredAt(Instant.now())
            .traceId("trace-456")
            .name("Remote SIM Hub M2M")
            .brand("Nokia")
            .state(DeviceState.INACTIVE)
            .updatedAt(updatedAt)
            .build();

        when(deviceRepository.findById(deviceId)).thenReturn(java.util.Optional.of(savedEntity));
        when(deviceMapper.toDomain(savedEntity)).thenReturn(existing);

        when(deviceMapper.toEntity(argThat(device ->
            device != null
                && deviceId.equals(device.id())
                && "Remote SIM Hub M2M".equals(device.name())
                && "Nokia".equals(device.brand())
                && DeviceState.INACTIVE == device.state()
                && createdAt.equals(device.createdAt())
                && updatedAt.equals(device.updatedAt())
        ))).thenReturn(savedEntity);

        assertDoesNotThrow(() -> consumer.consume(json(event)));

        verify(deviceRepository).save(savedEntity);
        verify(devicesByIdCache).evict(deviceId);
        verify(devicesSearchCache).clear();
    }

    @Test
    void handle_deleteRequested_deletesDeviceAndEvictsCaches() {
        UUID deviceId = UUID.randomUUID();

        when(cacheManager.getCache("devicesById")).thenReturn(devicesByIdCache);
        when(cacheManager.getCache("devicesSearch")).thenReturn(devicesSearchCache);

        DeviceEventMesssage event = DeviceEventMesssage.builder()
            .eventId(UUID.randomUUID())
            .aggregateType("device")
            .aggregateId(deviceId)
            .eventType(DeviceEventType.DEVICE_DELETE_REQUESTED)
            .occurredAt(Instant.now())
            .traceId("trace-789")
            .build();

        assertDoesNotThrow(() -> consumer.consume(json(event)));

        verify(deviceRepository).deleteById(deviceId);
        verify(devicesByIdCache).evict(deviceId);
        verify(devicesSearchCache).clear();
    }

    private DeviceEntity inactiveEntity(UUID id, Instant createdAt) {
        DeviceEntity entity = new DeviceEntity();
        entity.setId(id);
        entity.setName("Remote SIM Hub M2M");
        entity.setBrand("Nokia");
        entity.setState(DeviceState.INACTIVE);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private String json(DeviceEventMesssage event) {
        try {
            return new ObjectMapper().findAndRegisterModules().writeValueAsString(event);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}


