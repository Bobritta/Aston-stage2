package util;

import config.AppConfig;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void setup() {
        HibernateUtil.reset();

        AppConfig.setDbProperty("url", postgres.getJdbcUrl());
        AppConfig.setDbProperty("username", postgres.getUsername());
        AppConfig.setDbProperty("password", postgres.getPassword());
        AppConfig.setDbProperty("driver", postgres.getDriverClassName());

        LiquibaseUtil.runMigrations();

        HibernateUtil.getSessionFactory();
    }

}
