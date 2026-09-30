package com.devices.domain.mapper;

import com.devices.domain.model.Device;
import com.devices.ports.jpa.entity.DeviceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class})
public interface DeviceMapper {

    DeviceResponse toResponse(Device device);

    List<DeviceResponse> toResponses(List<Device> devices);

    Device toDomain(DeviceEntity entity);

    List<Device> toDomains(List<DeviceEntity> entities);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    Device toDomain(CreateDeviceRequest request);

    @Mapping(target = "id", expression = "java(existingDevice.id())")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "brand", source = "request.brand")
    @Mapping(target = "state", source = "request.state")
    @Mapping(target = "createdAt", expression = "java(existingDevice.createdAt())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    Device toDomain(ReplaceDeviceRequest request, Device existingDevice);

    DeviceEntity toEntity(Device device);
}

