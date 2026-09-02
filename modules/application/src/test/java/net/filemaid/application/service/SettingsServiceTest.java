package net.filemaid.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.filemaid.application.port.SettingsRepository;
import org.junit.jupiter.api.Test;

class SettingsServiceTest {
    @Test
    void marksEveryRuntimeBackedSettingAsActive() {
        var values = new HashMap<String, String>();
        var service = new SettingsService(new SettingsRepository() {
            @Override public Map<String, String> findAll() { return Map.copyOf(values); }
            @Override public void saveAll(Map<String, String> updates) { values.putAll(updates); }
        });

        for (String key : new String[] { "metadata.defaultMatchMode", "system.databaseBackupRetention" }) {
            assertTrue(service.definitions().stream()
                    .filter(definition -> definition.key().equals(key))
                    .findFirst().orElseThrow().active(), key + " should be marked active");
        }
    }
}
