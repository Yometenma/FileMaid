package net.filemaid.core.model;

public record MediaGroupMember(String path, MediaKind kind, String companionOf, ParsedMediaName media) {}
