package devices.domain.service;

import com.devices.domain.exception.DeviceDeletionNotAllowedException;
import com.devices.domain.exception.DeviceUpdateNotAllowedException;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import com.devices.domain.service.DevicePolicyValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevicePolicyValidatorTest {

    private final DevicePolicyValidator policy = new DevicePolicyValidator();

    @Test
    void validateUpdate_allowsNameAndBrandChange_whenDeviceNotInUse() {
        Device device = device(DeviceState.AVAILABLE, "Legacy eSIM Module", "Motorola");

        assertDoesNotThrow(() -> policy.validateUpdate(device, "5G Provisioning Gateway", "Nokia"));
    }

    @Test
    void validateUpdate_rejectsNameChange_whenDeviceInUse() {
        Device device = device(DeviceState.IN_USE, "Legacy eSIM Module", "Motorola");

        assertThrows(DeviceUpdateNotAllowedException.class, () -> policy.validateUpdate(device, "5G Provisioning Gateway", "Motorola"));
    }

    @Test
    void validateUpdate_rejectsBrandChange_whenDeviceInUse() {
        Device device = device(DeviceState.IN_USE, "Legacy eSIM Module", "Motorola");

        assertThrows(DeviceUpdateNotAllowedException.class, () -> policy.validateUpdate(device, "Legacy eSIM Module", "Nokia"));
    }

    @Test
    void validateDeletion_rejectsInUseDevice() {
        Device device = device(DeviceState.IN_USE, "Industrial SIM Gateway", "Nokia");

        assertThrows(DeviceDeletionNotAllowedException.class, () -> policy.validateDeletion(device));
    }

    @Test
    void validateDeletion_allowsNonInUseDevice() {
        Device device = device(DeviceState.INACTIVE, "Industrial SIM Gateway", "Nokia");

        assertDoesNotThrow(() -> policy.validateDeletion(device));
    }

    private Device device(DeviceState state, String name, String brand) {
        return Device.builder()
            .id(UUID.randomUUID())
            .name(name)
            .brand(brand)
            .state(state)
            .createdAt(Instant.now().minusSeconds(60))
            .updatedAt(Instant.now())
            .build();
    }
}

