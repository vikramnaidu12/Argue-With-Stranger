package com.arguewithstranger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration
 *                        + @ComponentScan (scans com.arguewithstranger.*)
 *
 * Spring Boot auto-configuration handles:
 *   - DataSource from application.properties
 *   - Hibernate SessionFactory
 *   - Jackson ObjectMapper
 *   - Embedded Tomcat on port 8080
 *   - Spring Security filter chain (configured in SecurityConfig)
 */
@Slf4j
@SpringBootApplication
public class ArgueWithStrangerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArgueWithStrangerApplication.class, args);
        log.info("═══════════════════════════════════════════");
        log.info("  Argue With Stranger started on port 8080 ");
        log.info("═══════════════════════════════════════════");
    }
}