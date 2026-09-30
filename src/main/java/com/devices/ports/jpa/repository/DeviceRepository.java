package com.devices.ports.jpa.repository;

import com.devices.domain.model.DeviceState;
import com.devices.ports.jpa.entity.DeviceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, JpaSpecificationExecutor<DeviceEntity> {

    Slice<DeviceEntity> findAllByBrandIgnoreCaseOrderByCreatedAtDesc(String brand, Pageable pageable);

    Slice<DeviceEntity> findAllByStateOrderByCreatedAtDesc(DeviceState state, Pageable pageable);

    Slice<DeviceEntity> findAllByBrandIgnoreCaseAndStateOrderByCreatedAtDesc(String brand, DeviceState state, Pageable pageable);

    Slice<DeviceEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

