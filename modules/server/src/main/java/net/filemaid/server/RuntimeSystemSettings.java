package net.filemaid.server;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;
import net.filemaid.application.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;

/** Applies the small set of process-wide settings that can safely change without a restart. */
@Component
public final class RuntimeSystemSettings {
    private static final String TIMEZONE = "system.timezone";
    private static final String LOG_LEVEL = "system.logLevel";
    private final SettingsService settings;
    private final LoggingSystem loggingSystem;

    @Autowired
    public RuntimeSystemSettings(SettingsService settings) {
        this(settings, LoggingSystem.get(RuntimeSystemSettings.class.getClassLoader()));
    }

    RuntimeSystemSettings(SettingsService settings, LoggingSystem loggingSystem) {
        this.settings = settings;
        this.loggingSystem = loggingSystem;
    }

    @PostConstruct
    void applyPersistedSettings() {
        apply(settings.findAll());
    }

    public void apply(Map<String, String> changed) {
        if (changed.containsKey(TIMEZONE)) {
            String zone = changed.get(TIMEZONE);
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)));
            System.setProperty("user.timezone", zone);
        }
        if (changed.containsKey(LOG_LEVEL)) {
            loggingSystem.setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, LogLevel.valueOf(changed.get(LOG_LEVEL)));
        }
    }
}
