package com.marvin.nutrition.configuration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Flyway configuration for the nutrition module. */
@Configuration("FlywayConfigNutrition")
public class FlywayConfig {

    /**
     * Configures and runs Flyway migrations for the nutrition schema.
     *
     * @param dataSource the datasource to use for migrations
     * @return the configured Flyway instance
     */
    @Bean(initMethod = "migrate")
    public Flyway flywayNutrition(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/nutrition")
                .schemas("nutrition")
                .table("flyway_schema_history_nutrition")
                .baselineOnMigrate(true)
                .load();
    }
}
