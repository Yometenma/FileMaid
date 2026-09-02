package net.filemaid.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;

/** Persists executed file operations to support history and undo. */
public interface OperationHistoryRepository {
    List<OperationRecord> append(List<OperationResult> results);

    default List<OperationRecord> append(String batchId, List<OperationResult> results) { return append(results); }

    List<OperationRecord> findAll();

    Optional<OperationRecord> findById(long id);

    /** Deletes entries whose timestamp predates {@code cutoff}; returns the removed count. */
    default int deleteOlderThan(Instant cutoff) { return 0; }
}
