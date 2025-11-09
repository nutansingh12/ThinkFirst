package com.thinkfirst.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConditionalOnExpression("'${DATABASE_URL:}'.length() > 0")
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        try {
            // Parse Railway's DATABASE_URL format: postgresql://user:password@host:port/database
            URI dbUri = new URI(databaseUrl);

            String username = null;
            String password = null;

            // Extract username and password from userInfo
            if (dbUri.getUserInfo() != null) {
                String[] userInfo = dbUri.getUserInfo().split(":");
                username = userInfo[0];
                if (userInfo.length > 1) {
                    password = userInfo[1];
                }
            }

            // Build JDBC URL: jdbc:postgresql://host:port/database
            String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();

            return DataSourceBuilder
                    .create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();

        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid DATABASE_URL format: " + databaseUrl, e);
        }
    }
}

