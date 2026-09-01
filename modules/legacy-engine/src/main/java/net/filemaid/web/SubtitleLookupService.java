package net.filemaid.web;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.filemaid.web.Datasource;
import net.filemaid.web.SubtitleDescriptor;

public interface SubtitleLookupService
extends Datasource {
    public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] var1, Locale var2) throws Exception;

    public URI getLink();

    public CheckResult checkSubtitle(File var1, File var2) throws Exception;

    public void uploadSubtitle(Object var1, Locale var2, File[] var3, File[] var4) throws Exception;

    public boolean requireLogin();

    public static class CheckResult {
        public final boolean exists;
        public final Object identity;
        public final Locale language;

        public CheckResult(boolean bl, Object object, Locale locale) {
            this.exists = bl;
            this.identity = object;
            this.language = locale;
        }

        public String toString() {
            return String.format("%s [%s] => %s", this.identity, this.language, this.exists);
        }
    }
}

