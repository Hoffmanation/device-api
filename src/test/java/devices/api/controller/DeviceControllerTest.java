package devices.api.controller;

import com.devices.api.controller.ApiExceptionHandler;
import com.devices.api.controller.DeviceController;
import com.devices.api.model.CommandAcceptedResponse;
import com.devices.api.model.CreateDeviceRequest;
import com.devices.application.service.DeviceApplicationManager;
import com.devices.domain.exception.DeviceNotFoundException;
import com.devices.domain.mapper.DeviceMapper;
import com.devices.domain.mapper.DeviceMapperImpl;
import com.devices.domain.model.Device;
import com.devices.domain.model.DeviceState;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DeviceControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final DeviceApplicationManager deviceApplicationManager = mock(DeviceApplicationManager.class);
    private final DeviceMapper deviceMapper = new DeviceMapperImpl();
    private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    private final MockMvc mockMvc;

    DeviceControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        this.mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceApplicationManager, deviceMapper))
            .setControllerAdvice(new ApiExceptionHandler(httpServletRequest))
            .setValidator(validator)
            .build();
    }

    @Test
    void createDevice_returns202() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        CreateDeviceRequest request = new CreateDeviceRequest();
        request.setName("eSIM Gateway X1");
        request.setBrand("Motorola");
        request.setState(DeviceState.AVAILABLE);

        CommandAcceptedResponse response = accepted(commandId, deviceId, "DEVICE_CREATE_REQUESTED");
        when(deviceApplicationManager.createDevice(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Location", "/api/v1/devices/" + deviceId))
            .andExpect(jsonPath("$.commandId").value(commandId.toString()))
            .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
            .andExpect(jsonPath("$.commandType").value("DEVICE_CREATE_REQUESTED"));
    }

    @Test
    void getDevice_returns200() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(deviceApplicationManager.getDevice(deviceId)).thenReturn(device(deviceId, "Industrial LTE Router Pro", "Samsung", DeviceState.IN_USE));

        mockMvc.perform(get("/api/v1/devices/{deviceId}", deviceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.brand").value("Samsung"))
            .andExpect(jsonPath("$.state").value("IN_USE"));
    }

    @Test
    void getDevice_notFound_returns404() throws Exception {
        UUID deviceId = UUID.randomUUID();
        when(deviceApplicationManager.getDevice(deviceId)).thenThrow(new DeviceNotFoundException(deviceId));

        mockMvc.perform(get("/api/v1/devices/{deviceId}", deviceId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("DEVICE_NOT_FOUND"));
    }


    @Test
    void patchDevice_validationFailure_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/devices/{deviceId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "x".repeat(256) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
    }

    @Test
    void patchDevice_invalidState_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/devices/{deviceId}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"INACTIVEEE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_DEVICE_STATE"));
    }

    @Test
    void deleteDevice_returns202() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        when(deviceApplicationManager.deleteDevice(deviceId)).thenReturn(accepted(commandId, deviceId, "DEVICE_DELETE_REQUESTED"));

        mockMvc.perform(delete("/api/v1/devices/{deviceId}", deviceId))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.commandId").value(commandId.toString()))
            .andExpect(jsonPath("$.commandType").value("DEVICE_DELETE_REQUESTED"));

        verify(deviceApplicationManager, times(1)).deleteDevice(deviceId);
    }

    private CommandAcceptedResponse accepted(UUID commandId, UUID deviceId, String commandType) {
        return CommandAcceptedResponse.builder()
            .commandId(commandId)
            .deviceId(deviceId)
            .commandType(commandType)
            .acceptedAt(Instant.parse("2026-09-28T09:00:00Z"))
            .build();
    }

    private Device device(UUID id, String name, String brand, DeviceState state) {
        return Device.builder()
            .id(id)
            .name(name)
            .brand(brand)
            .state(state)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}

