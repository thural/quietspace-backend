package dev.thural.quietspace.db;

import dev.thural.quietspace.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Validates the Flyway-managed production schema against the modularized
 * JPA entities: context startup runs {@code migrate} and Hibernate
 * {@code validate}. Catches schema drift introduced by entity decoupling
 * (ID foreign keys, collection-table mappings, enum mappings).
 *
 * <p>Uses its own MySQL container as {@code root} on database
 * {@code quietspace_flyway_test} because V1 contains {@code CREATE DATABASE} /
 * {@code USE} statements that the shared {@code test} user cannot execute.</p>
 */
@SpringBootTest
@Import(FlywayMigrationIT.Containers.class)
@ActiveProfiles("testcontainers")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/structure",
    "spring.flyway.clean-disabled=false",
    "spring.flyway.clean-on-validation-error=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayMigrationIT {

    @Test
    void contextLoads() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {

        @Bean
        @ServiceConnection
        MySQLContainer<?> mysqlContainer() {
            return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("quietspace_flyway_test")
                    .withUsername("root")
                    .withPassword("test");
        }

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitMQContainer() {
            return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
        }
    }
}
