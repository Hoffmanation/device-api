package com.devices.kafka;

import com.devices.application.port.DeviceCommandPublisher;
import com.devices.domain.event.DeviceEventMesssage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceCommandPublisher implements DeviceCommandPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.device-commands}")
    private String deviceCommandsTopic;

    @Override
    public void publish(DeviceEventMesssage command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            kafkaTemplate.send(deviceCommandsTopic, command.aggregateId().toString(), payload).join();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize device command for Kafka publication", exception);
        }
        log.info(
            "Published Kafka device command id={} type={} aggregateId={} topic={}",
            command.eventId(),
            command.eventType(),
            command.aggregateId(),
            deviceCommandsTopic
        );
    }
}

