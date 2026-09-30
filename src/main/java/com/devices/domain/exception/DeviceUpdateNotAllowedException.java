package com.devices.domain.exception;

public class DeviceUpdateNotAllowedException extends RuntimeException {

    public DeviceUpdateNotAllowedException(String message) {
        super(message);
    }
}

