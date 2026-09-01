package net.filemaid.server;

import java.util.Map;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.application.service.SettingsService;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.infrastructure.naming.SafeNamingTemplateEngine;

public final class RuntimeNamingTemplateEngine implements NamingTemplateEngine {
    private final FileMaidProperties properties;
    private final SettingsService settings;
    public RuntimeNamingTemplateEngine(FileMaidProperties properties, SettingsService settings) { this.properties = properties; this.settings = settings; }
    private SafeNamingTemplateEngine delegate() {
        return new SafeNamingTemplateEngine(
                settings.value("naming.seriesTemplate", properties.naming().series()),
                settings.value("naming.movieTemplate", properties.naming().movie()),
                settings.value("naming.unknownTemplate", properties.naming().unknown()));
    }
    @Override public String format(ParsedMediaName media) { return delegate().format(media); }
    @Override public String format(ParsedMediaName media, MediaInfo info) { return delegate().format(media, info); }
    @Override public Map<String, String> templates() { return delegate().templates(); }
}
