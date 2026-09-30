package com.devices.ports.jpa.repository;

import com.devices.domain.model.DeviceState;
import com.devices.ports.jpa.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, JpaSpecificationExecutor<DeviceEntity> {

    List<DeviceEntity> findAllByBrandIgnoreCaseOrderByCreatedAtDesc(String brand);

    List<DeviceEntity> findAllByStateOrderByCreatedAtDesc(DeviceState state);

    List<DeviceEntity> findAllByBrandIgnoreCaseAndStateOrderByCreatedAtDesc(String brand, DeviceState state);

    List<DeviceEntity> findAllByOrderByCreatedAtDesc();
}

