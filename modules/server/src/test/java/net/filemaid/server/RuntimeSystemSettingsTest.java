package net.filemaid.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import net.filemaid.application.port.SettingsRepository;
import net.filemaid.application.service.SettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

class RuntimeSystemSettingsTest {
    private final TimeZone originalTimezone = TimeZone.getDefault();
    private final String originalProperty = System.getProperty("user.timezone");

    @AfterEach void restoreTimezone() {
        TimeZone.setDefault(originalTimezone);
        if (originalProperty == null) System.clearProperty("user.timezone");
        else System.setProperty("user.timezone", originalProperty);
    }

    @Test void appliesTimezoneAndRootLogLevelImmediately() {
        SettingsService settings = new SettingsService(new MemorySettingsRepository());
        LoggingSystem logging = mock(LoggingSystem.class);
        RuntimeSystemSettings runtime = new RuntimeSystemSettings(settings, logging);

        runtime.apply(Map.of("system.timezone", "UTC", "system.logLevel", "DEBUG"));

        assertEquals("UTC", TimeZone.getDefault().getID());
        assertEquals("UTC", System.getProperty("user.timezone"));
        verify(logging).setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, LogLevel.DEBUG);
    }

    @Test void systemSettingsAreMarkedAsRuntimeActive() {
        SettingsService settings = new SettingsService(new MemorySettingsRepository());
        assertEquals(true, settings.definitions().stream().filter(d -> d.key().equals("system.timezone")).findFirst().orElseThrow().active());
        assertEquals(true, settings.definitions().stream().filter(d -> d.key().equals("system.logLevel")).findFirst().orElseThrow().active());
    }

    private static final class MemorySettingsRepository implements SettingsRepository {
        private final Map<String, String> values = new LinkedHashMap<>();
        @Override public Map<String, String> findAll() { return Map.copyOf(values); }
        @Override public void saveAll(Map<String, String> updates) { values.putAll(updates); }
    }
}
