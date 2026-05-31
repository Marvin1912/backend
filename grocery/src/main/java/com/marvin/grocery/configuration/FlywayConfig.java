package com.marvin.grocery.configuration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Flyway configuration for the grocery module. */
@Configuration("FlywayConfigGrocery")
public class FlywayConfig {

    /**
     * Configures and runs Flyway migrations for the grocery schema.
     *
     * @param dataSource the datasource to use for migrations
     * @return the configured Flyway instance
     */
    @Bean(initMethod = "migrate")
    public Flyway flywayGrocery(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/grocery")
                .schemas("grocery")
                .table("flyway_schema_history_grocery")
                .baselineOnMigrate(true)
                .load();
    }
}
