package devices.application.service;

import com.devices.api.model.CommandAcceptedResponse;
import com.devices.api.model.CreateDeviceRequest;
import com.devices.api.model.UpdateDeviceRequest;
import com.devices.application.port.DeviceCommandPublisher;
import com.devices.application.service.DeviceApplicationManager;
import com.devices.domain.event.DeviceEventMesssage;
import com.devices.domain.event.DeviceEventType;
import com.devices.domain.exception.DeviceDeletionNotAllowedException;
import com.devices.domain.exception.DeviceNotFoundException;
import com.devices.domain.exception.DeviceUpdateNotAllowedException;
import com.devices.domain.mapper.DeviceMapper;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import com.devices.domain.service.DevicePolicyValidator;
import com.devices.ports.jpa.entity.DeviceEntity;
import com.devices.ports.jpa.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceApplicationManagerTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceCommandPublisher deviceCommandPublisher;
    @Mock
    private DevicePolicyValidator devicePolicyValidator;

    private DeviceApplicationManager deviceApplicationManager;

    @Captor
    private ArgumentCaptor<DeviceEventMesssage> eventCaptor;

    @BeforeEach
    void setUp() {
        deviceApplicationManager = new DeviceApplicationManager(
            deviceRepository,
            deviceMapper,
            deviceCommandPublisher,
            devicePolicyValidator
        );
    }

    @Test
    void createDevice_acceptsAndPublishesCreateCommand() {
        CreateDeviceRequest request = new CreateDeviceRequest();
        request.setName("eSIM Gateway X1");
        request.setBrand("Motorola");
        request.setState(DeviceState.AVAILABLE);

        CommandAcceptedResponse actual = deviceApplicationManager.createDevice(request);

        assertEquals("DEVICE_CREATE_REQUESTED", actual.getCommandType());
        verify(deviceCommandPublisher).publish(eventCaptor.capture());
        DeviceEventMesssage envelope = eventCaptor.getValue();
        assertEquals("device", envelope.aggregateType());
        assertEquals(actual.getDeviceId(), envelope.aggregateId());
        assertEquals(actual.getCommandId(), envelope.eventId());
        assertEquals(DeviceEventType.DEVICE_CREATE_REQUESTED, envelope.eventType());
        assertEquals("eSIM Gateway X1", envelope.name());
        assertEquals("Motorola", envelope.brand());
        assertEquals(DeviceState.AVAILABLE, envelope.state());
        verifyNoInteractions(deviceRepository, deviceMapper);
    }

    @Test
    void updateDevice_acceptsAndPublishesUpdateCommand() {
        UUID deviceId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-28T09:00:00Z");
        DeviceEntity existingEntity = new DeviceEntity();
        existingEntity.setId(deviceId);
        Device existingDevice = Device.builder()
            .id(deviceId)
            .name("Remote SIM Hub M2M")
            .brand("Nokia")
            .state(DeviceState.AVAILABLE)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build();

        UpdateDeviceRequest request = new UpdateDeviceRequest();
        request.setState(DeviceState.INACTIVE);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingEntity));
        when(deviceMapper.toDomain(existingEntity)).thenReturn(existingDevice);

        CommandAcceptedResponse actual = deviceApplicationManager.updateDevice(deviceId, request);

        assertEquals(deviceId, actual.getDeviceId());
        assertEquals("DEVICE_UPDATE_REQUESTED", actual.getCommandType());
        verify(devicePolicyValidator).validateUpdate(existingDevice, null, null);
        verify(deviceCommandPublisher).publish(eventCaptor.capture());
        DeviceEventMesssage envelope = eventCaptor.getValue();
        assertEquals(DeviceEventType.DEVICE_UPDATE_REQUESTED, envelope.eventType());
        assertEquals(DeviceState.INACTIVE, envelope.state());
        assertEquals("Remote SIM Hub M2M", envelope.name());
        assertEquals("Nokia", envelope.brand());
    }

    @Test
    void deleteDevice_acceptsAndPublishesDeleteCommand() {
        UUID deviceId = UUID.randomUUID();
        DeviceEntity existingEntity = new DeviceEntity();
        existingEntity.setId(deviceId);
        Device existingDevice = Device.builder()
            .id(deviceId)
            .name("Industrial LTE Router Pro")
            .brand("Samsung")
            .state(DeviceState.AVAILABLE)
            .createdAt(Instant.parse("2026-09-28T09:00:00Z"))
            .updatedAt(Instant.parse("2026-09-28T09:05:00Z"))
            .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingEntity));
        when(deviceMapper.toDomain(existingEntity)).thenReturn(existingDevice);

        CommandAcceptedResponse actual = deviceApplicationManager.deleteDevice(deviceId);

        assertEquals(deviceId, actual.getDeviceId());
        assertEquals("DEVICE_DELETE_REQUESTED", actual.getCommandType());
        verify(devicePolicyValidator).validateDeletion(existingDevice);
        verify(deviceCommandPublisher).publish(eventCaptor.capture());
        DeviceEventMesssage envelope = eventCaptor.getValue();
        assertEquals(DeviceEventType.DEVICE_DELETE_REQUESTED, envelope.eventType());
        assertEquals(deviceId, envelope.aggregateId());
        verify(deviceRepository, never()).deleteById(deviceId);
    }

    @Test
    void updateDevice_notFound_failsBeforePublishing() {
        UUID deviceId = UUID.randomUUID();
        UpdateDeviceRequest request = new UpdateDeviceRequest();
        request.setState(DeviceState.INACTIVE);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class, () -> deviceApplicationManager.updateDevice(deviceId, request));

        verifyNoInteractions(deviceCommandPublisher, devicePolicyValidator);
    }

    @Test
    void updateDevice_policyFailure_failsBeforePublishing() {
        UUID deviceId = UUID.randomUUID();
        DeviceEntity existingEntity = new DeviceEntity();
        existingEntity.setId(deviceId);
        Device existingDevice = Device.builder()
            .id(deviceId)
            .name("Legacy eSIM Module")
            .brand("Nokia")
            .state(DeviceState.IN_USE)
            .createdAt(Instant.parse("2026-09-28T09:00:00Z"))
            .updatedAt(Instant.parse("2026-09-28T09:05:00Z"))
            .build();
        UpdateDeviceRequest request = new UpdateDeviceRequest();
        request.setName("5G Provisioning Gateway");

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingEntity));
        when(deviceMapper.toDomain(existingEntity)).thenReturn(existingDevice);
        doThrow(new DeviceUpdateNotAllowedException("Name and brand cannot be updated while the device is in use"))
            .when(devicePolicyValidator).validateUpdate(existingDevice, request.getName(), request.getBrand());

        assertThrows(DeviceUpdateNotAllowedException.class, () -> deviceApplicationManager.updateDevice(deviceId, request));

        verify(deviceCommandPublisher, never()).publish(any());
    }

    @Test
    void deleteDevice_policyFailure_failsBeforePublishing() {
        UUID deviceId = UUID.randomUUID();
        DeviceEntity existingEntity = new DeviceEntity();
        existingEntity.setId(deviceId);
        Device existingDevice = Device.builder()
            .id(deviceId)
            .name("Industrial LTE Router Pro")
            .brand("Samsung")
            .state(DeviceState.IN_USE)
            .createdAt(Instant.parse("2026-09-28T09:00:00Z"))
            .updatedAt(Instant.parse("2026-09-28T09:05:00Z"))
            .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingEntity));
        when(deviceMapper.toDomain(existingEntity)).thenReturn(existingDevice);
        doThrow(new DeviceDeletionNotAllowedException("Devices in use cannot be deleted"))
            .when(devicePolicyValidator).validateDeletion(eq(existingDevice));

        assertThrows(DeviceDeletionNotAllowedException.class, () -> deviceApplicationManager.deleteDevice(deviceId));

        verify(deviceCommandPublisher, never()).publish(any());
    }
}

