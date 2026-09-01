package net.filemaid.application.port;

import java.util.List;
import java.util.Locale;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

public interface MetadataProvider {
    String id();
    boolean available();
    String status();
    List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception;
}
