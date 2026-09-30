package devices.ports.jpa.repository;

import com.devices.DevicesApiApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DevicesApiApplication.class)
@ActiveProfiles("test")
abstract class BaseRepositoryTest {
}

