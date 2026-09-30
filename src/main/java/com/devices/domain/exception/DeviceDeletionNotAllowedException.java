package com.devices.domain.exception;

public class DeviceDeletionNotAllowedException extends RuntimeException {

    public DeviceDeletionNotAllowedException(String message) {
        super(message);
    }
}

