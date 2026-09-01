package net.filemaid.application.service;

import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;

/** Thin use-case facade over the operation history repository. */
public final class OperationHistoryService {
    private final OperationHistoryRepository repository;

    public OperationHistoryService(OperationHistoryRepository repository) {
        this.repository = repository;
    }

    public List<OperationRecord> append(List<OperationResult> results) {
        return repository.append(results);
    }

    public List<OperationRecord> findAll() {
        return repository.findAll();
    }

    public Optional<OperationRecord> findById(long id) {
        return repository.findById(id);
    }
}
