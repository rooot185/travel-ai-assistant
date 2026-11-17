package com.wayfarer.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TravelApplication.Settings.class)
public class TravelApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelApplication.class, args);
    }

    @ConfigurationProperties("travel")
    public record Settings(boolean secureCookie, boolean allowRegistration, boolean aiEnabled,
                           String modelUrl, String modelName, String modelKey, String amapKey) {
        public boolean plannerReady() {
            return aiEnabled && !modelKey.isBlank() && !amapKey.isBlank() && !modelName.isBlank();
        }
    }
}
