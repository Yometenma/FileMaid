package net.filemaid.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.filemaid.core.model.MetadataCandidate;

/** Durable cache for normalized metadata provider responses. */
public interface MetadataCacheRepository {
    Optional<Entry> find(String key);
    void save(String key, String provider, List<MetadataCandidate> candidates, Instant createdAt, long ttlSeconds);
    int deleteOlderThan(Instant cutoff);
    boolean tryConsumeDaily(String provider, String credentialHash, LocalDate date, int limit);

    record Entry(List<MetadataCandidate> candidates, Instant createdAt, long ttlSeconds) {
        public Entry { candidates = List.copyOf(candidates); }
    }
}
