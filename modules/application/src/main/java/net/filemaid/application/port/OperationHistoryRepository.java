package net.filemaid.application.port;

import java.util.List;
import java.util.Optional;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;

/** Persists executed file operations to support history and undo. */
public interface OperationHistoryRepository {
    List<OperationRecord> append(List<OperationResult> results);

    List<OperationRecord> findAll();

    Optional<OperationRecord> findById(long id);
}
