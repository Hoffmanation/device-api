package com.devices.application.port;

import com.devices.domain.event.DeviceEventMesssage;

public interface DeviceCommandPublisher {

    void publish(DeviceEventMesssage command);
}

