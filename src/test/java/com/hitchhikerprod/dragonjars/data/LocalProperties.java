package com.hitchhikerprod.dragonjars.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class LocalProperties {
    private static final LocalProperties INSTANCE = new LocalProperties();

    private static final String SYSTEM_FILE = "/com/hitchhikerprod/dragonjars/system.properties";
    private static final String USER_FILE = "/com/hitchhikerprod/dragonjars/personal.properties";

    private final Properties properties;

    private LocalProperties() {
        this.properties = new Properties();

        try (final InputStream input = this.getClass().getResourceAsStream(SYSTEM_FILE)) {
            if (Objects.isNull(input)) throw new IOException();
            properties.load(input);
        } catch (IOException e) {
            properties.setProperty("path.base", null);
        }

        try (final InputStream input = this.getClass().getResourceAsStream(USER_FILE)) {
            if (Objects.isNull(input)) throw new IOException();
            properties.load(input);
        } catch (IOException _) { }
    }

    public static String getBasePath() {
        return INSTANCE.properties.getProperty("path.base");
    }
}
