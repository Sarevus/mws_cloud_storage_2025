package com.MWS.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "subscription")
public class SubscriptionProperties {

    private Map<String, PlanConfig> plans;

    @Data
    public static class PlanConfig {
        private String name;
        private Long storageLimitBytes;
        private Double pricePerMonth;
        private Double pricePerYear;
        private Long maxFileSizeBytes;
        private Integer maxFilesCount;
        private Integer priority;
        private Boolean canShareFiles;
        private Map<String, String> description;
        private Map<String, Object> features;
    }
}