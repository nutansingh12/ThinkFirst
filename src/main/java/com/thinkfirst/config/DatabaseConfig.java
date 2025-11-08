package com.thinkfirst.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        // If DATABASE_URL is provided (Railway format), convert it to JDBC format
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            // Railway provides: postgresql://user:password@host:port/database
            // We need: jdbc:postgresql://user:password@host:port/database
            if (databaseUrl.startsWith("postgresql://") && !databaseUrl.startsWith("jdbc:")) {
                databaseUrl = "jdbc:" + databaseUrl;
            }
            
            return DataSourceBuilder
                    .create()
                    .url(databaseUrl)
                    .build();
        }
        
        // Fall back to default Spring Boot configuration
        return DataSourceBuilder
                .create()
                .build();
    }
}

