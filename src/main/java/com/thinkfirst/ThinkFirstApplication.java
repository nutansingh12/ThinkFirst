package com.thinkfirst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ThinkFirst - Educational AI Chat App for Kids
 * Main application entry point
 */
@SpringBootApplication
@EnableJpaAuditing
public class ThinkFirstApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThinkFirstApplication.class, args);
    }
}

