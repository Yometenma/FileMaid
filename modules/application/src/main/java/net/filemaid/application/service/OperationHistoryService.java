package net.filemaid.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;

/** Thin use-case facade over the operation history repository. */
public final class OperationHistoryService {
    private final OperationHistoryRepository repository;
    private final SettingsService settings;

    public OperationHistoryService(OperationHistoryRepository repository) {
        this(repository, null);
    }

    public OperationHistoryService(OperationHistoryRepository repository, SettingsService settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public List<OperationRecord> append(List<OperationResult> results) {
        List<OperationRecord> records = repository.append(results);
        cleanupExpired();
        return records;
    }

    public List<OperationRecord> findAll() {
        return repository.findAll();
    }

    public Optional<OperationRecord> findById(long id) {
        return repository.findById(id);
    }

    private void cleanupExpired() {
        int days = settings == null ? 0 : parseDays(settings.value("files.historyRetentionDays", "90"));
        if (days > 0) repository.deleteOlderThan(Instant.now().minus(days, ChronoUnit.DAYS));
    }

    static int parseDays(String value) {
        try { return Math.max(0, Integer.parseInt(value == null ? "" : value.trim())); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
