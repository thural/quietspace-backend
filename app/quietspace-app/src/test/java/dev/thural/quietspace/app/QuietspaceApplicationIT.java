package dev.thural.quietspace;

import dev.thural.quietspace.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("testcontainers")
class QuietspaceApplicationIT {

	@Test
	void contextLoads() {
	}

}
