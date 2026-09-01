package net.filemaid.application.port;

import java.util.List;
import java.util.Optional;
import net.filemaid.core.model.MetadataSelection;

/** Persists user-confirmed metadata matches so later scans can reuse them. */
public interface MatchDecisionRepository {
    void save(MetadataSelection selection);

    Optional<MetadataSelection> findBySource(String source);

    List<MetadataSelection> findAll();
}
