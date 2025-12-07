package com.MWS;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class FlywayConfig {

    public static void migrateDatabase(PostgreSQLContainer<?> postgres) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations("classpath:/migration")
                .load();

        flyway.migrate();
    }

    public static void cleanDatabase(PostgreSQLContainer<?> postgres) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .load();

        flyway.clean();
    }
}