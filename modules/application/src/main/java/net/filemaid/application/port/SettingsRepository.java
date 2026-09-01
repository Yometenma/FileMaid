package net.filemaid.application.port;

import java.util.Map;

/** Persistent application settings. Secret handling belongs to the API boundary. */
public interface SettingsRepository {
    Map<String, String> findAll();
    void saveAll(Map<String, String> values);
}
