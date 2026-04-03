package com.adobe.romannumeral.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application-specific configuration properties.
 *
 * <p>Bound to the {@code app.*} namespace in {@code application.yml}.
 * Externalizing these values allows tuning without code changes.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Maximum value accepted for conversion (default: 3999). */
    private int maxValue = 3999;

    /** Maximum range size for range queries (default: 3999). */
    private int maxRangeSize = 3999;

    /**
     * Valid API keys for authentication.
     * Production note: use a secrets manager (Vault, AWS Secrets Manager) instead of config files.
     */
    private List<String> apiKeys = List.of();
}
