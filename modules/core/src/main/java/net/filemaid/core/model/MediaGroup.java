package net.filemaid.core.model;

import java.util.List;

public record MediaGroup(
        String key,
        MediaGroupType type,
        String title,
        Integer year,
        List<MediaGroupMember> members,
        List<String> warnings) {
    public MediaGroup {
        members = members == null ? List.of() : List.copyOf(members);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
