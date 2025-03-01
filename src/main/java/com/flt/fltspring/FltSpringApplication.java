package com.flt.fltspring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Flight Logbook Table Scanner Spring Boot Application
 * 
 * This application processes flight logbook images, extracts table data,
 * and returns structured information for use in the Flight Log Tracer system.
 */
@SpringBootApplication
@ServletComponentScan
@EnableCaching
@EnableScheduling
@Slf4j
public class FltSpringApplication {
    public static void main(String[] args) {
        log.info("Starting Flight Logbook Table Scanner application");
        SpringApplication.run(FltSpringApplication.class, args);
    }
    
    /**
     * Configure CORS for the application
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "https://flightlogtracer.com", 
                        "https://www.flightlogtracer.com"
                    )
                    .allowedMethods("GET", "POST")
                    .maxAge(3600);
            }
        };
    }
}
