package com.caresync.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getResourceAsStream("/database.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing database.properties in src/main/resources");
            }
            PROPERTIES.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read database.properties", ex);
        }
    }

    private DatabaseConfig() {
    }

    public static String getUrl() {
        return firstPresent("CARESYNC_DB_URL", "caresync.db.url", "db.url");
    }

    public static String getUsername() {
        return firstPresent("CARESYNC_DB_USERNAME", "caresync.db.username", "db.username");
    }

    public static String getPassword() {
        return firstPresent("CARESYNC_DB_PASSWORD", "caresync.db.password", "db.password");
    }

    private static String firstPresent(String environmentKey, String propertyKey, String fileKey) {
        String environmentValue = System.getenv(environmentKey);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        return PROPERTIES.getProperty(fileKey);
    }
}
