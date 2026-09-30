package com.devices.domain.service;

import com.devices.domain.exception.DeviceDeletionNotAllowedException;
import com.devices.domain.exception.DeviceUpdateNotAllowedException;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import org.springframework.stereotype.Component;

@Component
public class DevicePolicyValidator {

    public void validateUpdate(Device existingDevice, String newName, String newBrand) {
        if (existingDevice.state() == DeviceState.IN_USE) {
            boolean nameChanged = newName != null && !existingDevice.name().equals(newName);
            boolean brandChanged = newBrand != null && !existingDevice.brand().equals(newBrand);
            if (nameChanged || brandChanged) {
                throw new DeviceUpdateNotAllowedException("Name and brand cannot be updated while the device is in use");
            }
        }
    }

    public void validateDeletion(Device existingDevice) {
        if (existingDevice.state() == DeviceState.IN_USE) {
            throw new DeviceDeletionNotAllowedException("Devices in use cannot be deleted");
        }
    }
}

