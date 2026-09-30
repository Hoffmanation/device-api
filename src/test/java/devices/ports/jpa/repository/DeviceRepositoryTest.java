package devices.ports.jpa.repository;

import com.devices.domain.model.DeviceState;
import com.devices.ports.jpa.entity.DeviceEntity;
import com.devices.ports.jpa.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
    }

    @Test
    void findAllByBrandIgnoreCaseOrderByCreatedAtDesc_returnsMatchingDevices() {
        deviceRepository.save(device("eSIM Gateway X1", "Motorola", DeviceState.AVAILABLE, Instant.parse("2026-09-28T08:00:00Z")));
        deviceRepository.save(device("Industrial LTE Router Pro", "Samsung", DeviceState.IN_USE, Instant.parse("2026-09-28T09:00:00Z")));
        deviceRepository.save(device("Remote SIM Hub M2M", "motorola", DeviceState.INACTIVE, Instant.parse("2026-09-28T10:00:00Z")));

        List<DeviceEntity> result = deviceRepository.findAllByBrandIgnoreCaseOrderByCreatedAtDesc("MOTOROLA", PageRequest.of(0, 10)).getContent();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Remote SIM Hub M2M");
        assertThat(result.get(1).getName()).isEqualTo("eSIM Gateway X1");
    }

    @Test
    void findAllByStateOrderByCreatedAtDesc_returnsOnlyRequestedState() {
        deviceRepository.save(device("eSIM Gateway X1", "Motorola", DeviceState.AVAILABLE, Instant.parse("2026-09-28T08:00:00Z")));
        deviceRepository.save(device("NB-IoT Tracker One", "Motorola", DeviceState.AVAILABLE, Instant.parse("2026-09-28T10:00:00Z")));
        deviceRepository.save(device("Industrial LTE Router Pro", "Samsung", DeviceState.IN_USE, Instant.parse("2026-09-28T09:00:00Z")));

        List<DeviceEntity> result = deviceRepository.findAllByStateOrderByCreatedAtDesc(DeviceState.AVAILABLE, PageRequest.of(0, 10)).getContent();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DeviceEntity::getName).containsExactly("NB-IoT Tracker One", "eSIM Gateway X1");
    }

    @Test
    void findAllByBrandIgnoreCaseAndStateOrderByCreatedAtDesc_returnsIntersection() {
        deviceRepository.save(device("eSIM Gateway X1", "Motorola", DeviceState.AVAILABLE, Instant.parse("2026-09-28T08:00:00Z")));
        deviceRepository.save(device("Remote SIM Hub M2M", "Motorola", DeviceState.IN_USE, Instant.parse("2026-09-28T09:00:00Z")));
        deviceRepository.save(device("5G Edge Router X2", "Nokia", DeviceState.AVAILABLE, Instant.parse("2026-09-28T10:00:00Z")));

        List<DeviceEntity> result = deviceRepository.findAllByBrandIgnoreCaseAndStateOrderByCreatedAtDesc("motorola", DeviceState.AVAILABLE, PageRequest.of(0, 10)).getContent();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("eSIM Gateway X1");
    }

    private DeviceEntity device(String name, String brand, DeviceState state, Instant createdAt) {
        DeviceEntity entity = new DeviceEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setBrand(brand);
        entity.setState(state);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt.plusSeconds(60));
        return entity;
    }
}
