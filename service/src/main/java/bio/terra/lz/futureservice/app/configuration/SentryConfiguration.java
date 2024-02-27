package bio.terra.lz.futureservice.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "landingzone.sentry")
public record SentryConfiguration(String dsn, String environment) {}
