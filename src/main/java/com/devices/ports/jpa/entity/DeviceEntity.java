package com.devices.ports.jpa.entity;

import com.devices.domain.model.DeviceState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_devices_brand", columnList = "brand"),
    @Index(name = "idx_devices_state", columnList = "state"),
    @Index(name = "idx_devices_brand_state", columnList = "brand,state")
})
public class DeviceEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeviceState state;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

