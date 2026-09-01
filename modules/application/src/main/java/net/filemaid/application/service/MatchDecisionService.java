package net.filemaid.application.service;

import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.MatchDecisionRepository;
import net.filemaid.core.model.MetadataSelection;

/** Stores and retrieves confirmed metadata matches. */
public final class MatchDecisionService {
    private final MatchDecisionRepository repository;

    public MatchDecisionService(MatchDecisionRepository repository) {
        this.repository = repository;
    }

    public void save(MetadataSelection selection) {
        repository.save(selection);
    }

    public Optional<MetadataSelection> findBySource(String source) {
        return repository.findBySource(source);
    }

    public List<MetadataSelection> findAll() {
        return repository.findAll();
    }
}
