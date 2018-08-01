package org.logevents.util;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Properties;

public class Configuration {

    private Properties properties;
    private String prefix;

    public Configuration(Properties properties, String prefix) {
        this.properties = properties;
        this.prefix = prefix;
    }

    public Duration getDuration(String key) {
        try {
            return Duration.parse(getString(key));
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(fullKey(key) + " value " + getString(key) + ": " + e.getMessage());
        }
    }

    private String getString(String key) {
        return optionalString(key)
                .orElseThrow(() -> new IllegalStateException("Missing required key <" + fullKey(key) + "> in <" + properties.keySet() + ">"));
    }

    private Optional<String> optionalString(String key) {
        return Optional.ofNullable(properties.getProperty(fullKey(key)));
    }

    private String fullKey(String key) {
        return prefix + "." + key;
    }

    public <T> T createInstance(String key, Class<T> clazz) {
        optionalString(key)
            .orElseThrow(() -> new IllegalStateException("Missing configuration for " + clazz.getSimpleName() + " in " + fullKey(key)));
        return ConfigUtil.create(fullKey(key), clazz.getPackage().getName(), properties);
    }
}