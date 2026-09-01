package net.filemaid.web;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.ResourceManager;
import net.filemaid.util.Digest;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.WebRequest;

public class ShooterSubtitles
implements SubtitleLookupService {
    @Override
    public String getIdentifier() {
        return "Shooter";
    }

    @Override
    public String getName() {
        return "\u5c04\u624b\u7f51";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.shooter");
    }

    @Override
    public boolean requireLogin() {
        return false;
    }

    @Override
    public URI getLink() {
        return URI.create("https://www.shooter.cn/");
    }

    public Cache getCache() {
        return Cache.getCache(this.getIdentifier(), CacheType.Daily);
    }

    @Override
    public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] fileArray, Locale locale) throws Exception {
        LinkedHashMap<File, List<SubtitleDescriptor>> linkedHashMap = new LinkedHashMap<File, List<SubtitleDescriptor>>(fileArray.length);
        for (File file : fileArray) {
            linkedHashMap.put(file, this.getSubtitleList(file, locale));
        }
        return linkedHashMap;
    }

    protected URL getSubApiUrl() {
        return WebRequest.parseURL(System.getProperty("net.filemaid.web.ShooterSubtitles.url", "https://www.shooter.cn/api/subapi.php"));
    }

    public synchronized List<SubtitleDescriptor> getSubtitleList(File file, Locale locale) throws Exception {
        if (Stream.of(Locale.CHINESE, Locale.ENGLISH).map(Locale::getLanguage).noneMatch(locale.getLanguage()::equals)) {
            throw new IllegalArgumentException("Language not supported: " + locale);
        }
        if (file.length() < 8192L) {
            return Collections.emptyList();
        }
        URL uRL = this.getSubApiUrl();
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        linkedHashMap.put("filehash", ShooterSubtitles.computeFileHash(file));
        linkedHashMap.put("pathinfo", file.getName());
        linkedHashMap.put("format", "json");
        linkedHashMap.put("lang", Locale.CHINESE.getLanguage().equals(locale.getLanguage()) ? "Chn" : "Eng");
        Duration duration = Duration.ofHours(1L);
        return this.getCache().castList(SubtitleDescriptor.class).computeIf(((Object)linkedHashMap).toString(), Cache.isStale(duration), element -> {
            ByteBuffer byteBuffer = WebRequest.post(uRL, linkedHashMap, null);
            if (byteBuffer.remaining() == 1 && byteBuffer.get(0) == -1) {
                return Collections.emptyList();
            }
            String string = FileUtilities.getNameWithoutExtension(file.getName());
            Object object = JsonUtilities.readJson(StandardCharsets.UTF_8.decode(byteBuffer));
            return JsonUtilities.streamJsonObjects(object).flatMap(map -> JsonUtilities.streamJsonObjects(map, "Files")).map(map -> {
                String string2 = JsonUtilities.getString(map, "Ext");
                String string3 = JsonUtilities.getString(map, "Link");
                return new ShooterSubtitleDescriptor(string, string2, string3, locale.getDisplayLanguage(Locale.ENGLISH));
            }).limit(1L).collect(Collectors.toList());
        });
    }

    @Override
    public SubtitleLookupService.CheckResult checkSubtitle(File file, File file2) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uploadSubtitle(Object object, Locale locale, File[] fileArray, File[] fileArray2) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected static String computeFileHash(File file) throws IOException {
        ArrayList<String> arrayList = new ArrayList<String>();
        long l = file.length();
        long[] lArray = new long[4];
        lArray[3] = l - 8192L;
        lArray[2] = l / 3L;
        lArray[1] = l / 3L * 2L;
        lArray[0] = 4096L;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");){
            byte[] byArray = new byte[4096];
            for (int i = 0; i < 4; ++i) {
                randomAccessFile.seek(lArray[i]);
                randomAccessFile.readFully(byArray);
                arrayList.add(Digest.md5(byArray));
            }
        }
        return String.join((CharSequence)";", arrayList);
    }

    public static class ShooterSubtitleDescriptor
    implements SubtitleDescriptor,
    Serializable {
        private String name;
        private String type;
        private String link;
        private String language;

        public ShooterSubtitleDescriptor(String string, String string2, String string3, String string4) {
            this.name = string;
            this.type = string2;
            this.link = string3;
            this.language = string4;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getLanguageName() {
            return this.language;
        }

        @Override
        public boolean isForced() {
            return false;
        }

        @Override
        public boolean isHI() {
            return false;
        }

        @Override
        public String getType() {
            return this.type;
        }

        @Override
        public ByteBuffer fetch() throws Exception {
            return WebRequest.fetch(WebRequest.newURL(this.link));
        }

        @Override
        public String getPath() {
            return this.getName() + "." + this.getType();
        }

        @Override
        public long getLength() {
            return -1L;
        }

        @Override
        public File toFile() {
            return new File(this.getPath());
        }

        public String toString() {
            return this.getName();
        }
    }
}

