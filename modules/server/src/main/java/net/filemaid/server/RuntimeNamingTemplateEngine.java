package net.filemaid.server;

import java.util.LinkedHashMap;
import java.util.Map;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.application.service.SettingsService;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.infrastructure.naming.SafeNamingTemplateEngine;

/** Reads the naming settings at call time so template changes apply without a restart. */
public final class RuntimeNamingTemplateEngine implements NamingTemplateEngine {
    private static final String DEFAULT_PRESET = "JELLYFIN";

    /** Built-in naming presets; CUSTOM falls back to the user-defined templates. */
    private static final Map<String, Preset> PRESETS = Map.of(
            "JELLYFIN", new Preset(
                    "TV Shows/{title}/Season {season:02}/{title} - S{season:02}{episodes}{extension}",
                    "Movies/{title} ({year})/{title} ({year}){extension}",
                    "Unsorted/{original}"),
            "EMBY", new Preset(
                    "TV Shows/{title}/Season {season:02}/{title} - S{season:02}E{episode:02}{extension}",
                    "Movies/{title} ({year})/{title} ({year}){extension}",
                    "Unsorted/{original}"),
            "PLEX", new Preset(
                    "TV Shows/{title} ({year})/Season {season:02}/{title} - S{season:02}E{episode:02}{extension}",
                    "Movies/{title} ({year})/{title} ({year}){extension}",
                    "Unsorted/{original}"));

    private final FileMaidProperties properties;
    private final SettingsService settings;

    public RuntimeNamingTemplateEngine(FileMaidProperties properties, SettingsService settings) {
        this.properties = properties;
        this.settings = settings;
    }

    private SafeNamingTemplateEngine delegate() {
        String preset = settings.value("naming.preset", DEFAULT_PRESET);
        String unknownTitle = settings.value("naming.unknownTitle", "Unknown");
        if (!"CUSTOM".equals(preset)) {
            Preset builtin = PRESETS.getOrDefault(preset, PRESETS.get(DEFAULT_PRESET));
            return new SafeNamingTemplateEngine(builtin.series(), builtin.movie(), builtin.unknown(), unknownTitle);
        }
        return new SafeNamingTemplateEngine(
                settings.value("naming.seriesTemplate", properties.naming().series()),
                settings.value("naming.movieTemplate", properties.naming().movie()),
                settings.value("naming.unknownTemplate", properties.naming().unknown()),
                unknownTitle);
    }

    @Override public String format(ParsedMediaName media) { return delegate().format(media); }
    @Override public String format(ParsedMediaName media, MediaInfo info) { return delegate().format(media, info); }

    @Override public Map<String, String> templates() {
        Map<String, String> result = new LinkedHashMap<>(delegate().templates());
        result.put("preset", settings.value("naming.preset", DEFAULT_PRESET));
        result.put("titlePreference", settings.value("naming.titlePreference", "LOCALIZED"));
        result.put("unknownTitle", settings.value("naming.unknownTitle", "Unknown"));
        return result;
    }

    private record Preset(String series, String movie, String unknown) { }
}
