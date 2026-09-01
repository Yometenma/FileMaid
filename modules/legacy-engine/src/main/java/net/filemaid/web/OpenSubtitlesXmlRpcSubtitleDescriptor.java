package net.filemaid.web;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import net.filemaid.Logging;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.WebRequest;

public class OpenSubtitlesXmlRpcSubtitleDescriptor
implements SubtitleDescriptor,
Serializable {
    private final Map<Property, String> properties;
    private static int DOWNLOAD_QUOTA = 1000;

    public OpenSubtitlesXmlRpcSubtitleDescriptor(Map<Property, String> map) {
        this.properties = map;
    }

    public Map<Property, String> getProperties() {
        return this.properties;
    }

    public String getProperty(Property property) {
        return this.properties.get((Object)property);
    }

    @Override
    public String getPath() {
        StringBuilder stringBuilder = new StringBuilder(64);
        String string = this.getProperty(Property.MovieName);
        if (string != null) {
            stringBuilder.append(string);
            stringBuilder.append('/');
        }
        String string2 = this.getProperty(Property.SeriesSeason);
        String string3 = this.getProperty(Property.SeriesEpisode);
        if (string2 != null && string3 != null) {
            stringBuilder.append('S').append((String)(string2.length() > 1 ? string2 : "0" + string2));
            stringBuilder.append('E').append((String)(string3.length() > 1 ? string3 : "0" + string3));
            stringBuilder.append('/');
        }
        stringBuilder.append(this.getName()).append('.').append(this.getType());
        return stringBuilder.toString();
    }

    @Override
    public String getName() {
        return FileUtilities.getNameWithoutExtension(this.getProperty(Property.SubFileName));
    }

    @Override
    public String getLanguageName() {
        return this.getProperty(Property.LanguageName);
    }

    @Override
    public boolean isForced() {
        return "0".equals(this.getProperty(Property.SubForeignPartsOnly));
    }

    @Override
    public boolean isHI() {
        return "0".equals(this.getProperty(Property.SubHearingImpaired));
    }

    @Override
    public String getType() {
        return this.getProperty(Property.SubFormat);
    }

    @Override
    public long getLength() {
        return Long.parseLong(this.getProperty(Property.SubSize));
    }

    public String getMovieHash() {
        return this.getProperty(Property.MovieHash);
    }

    public long getMovieByteSize() {
        return Long.parseLong(this.getProperty(Property.MovieByteSize));
    }

    public String getMovieReleaseName() {
        return this.getProperty(Property.MovieReleaseName);
    }

    public int getQueryNumber() {
        return Integer.parseInt(this.getProperty(Property.QueryNumber));
    }

    public float getMovieFPS() {
        return Float.parseFloat(this.getProperty(Property.MovieFPS));
    }

    public long getMovieTimeMS() {
        return Long.parseLong(this.getProperty(Property.MovieTimeMS));
    }

    public int getSubActualCD() {
        return Integer.parseInt(this.getProperty(Property.SubActualCD));
    }

    public int getSubSumCD() {
        return Integer.parseInt(this.getProperty(Property.SubSumCD));
    }

    public static synchronized void checkDownloadQuota() throws IllegalStateException {
        if (DOWNLOAD_QUOTA <= 0) {
            throw new IllegalStateException("Download-Quota has been exceeded");
        }
    }

    private static synchronized void setAndCheckDownloadQuota(int n) throws IllegalStateException {
        DOWNLOAD_QUOTA = n;
        OpenSubtitlesXmlRpcSubtitleDescriptor.checkDownloadQuota();
    }

    @Override
    public ByteBuffer fetch() throws Exception {
        OpenSubtitlesXmlRpcSubtitleDescriptor.checkDownloadQuota();
        URL uRL = WebRequest.newURL(this.getProperty(Property.SubDownloadLink));
        ByteBuffer byteBuffer = WebRequest.fetch(uRL, 0L, null, null, (string, string2) -> {
            if ("download-quota".equals(string)) {
                Logging.debug.finest("Download-Quota: " + string2);
                OpenSubtitlesXmlRpcSubtitleDescriptor.setAndCheckDownloadQuota(Integer.parseInt(string2));
            }
        });
        return WebRequest.gunzip(byteBuffer);
    }

    public int hashCode() {
        return this.getProperty(Property.IDSubtitle).hashCode();
    }

    public boolean equals(Object object) {
        if (object instanceof OpenSubtitlesXmlRpcSubtitleDescriptor) {
            OpenSubtitlesXmlRpcSubtitleDescriptor openSubtitlesXmlRpcSubtitleDescriptor = (OpenSubtitlesXmlRpcSubtitleDescriptor)object;
            return this.getProperty(Property.IDSubtitle).equals(openSubtitlesXmlRpcSubtitleDescriptor.getProperty(Property.IDSubtitle));
        }
        return false;
    }

    public String toString() {
        return this.getProperty(Property.SubFileName);
    }

    @Override
    public File toFile() {
        return new File(this.getPath());
    }

    public static enum Property {
        IDSubtitle,
        IDSubtitleFile,
        IDSubMovieFile,
        IDMovie,
        IDMovieImdb,
        SubFileName,
        SubLastTS,
        SubFormat,
        SubEncoding,
        SubHash,
        SubSize,
        MovieHash,
        MovieByteSize,
        MovieName,
        MovieNameEng,
        MovieYear,
        MovieReleaseName,
        MovieTimeMS,
        MovieFPS,
        MovieImdbRating,
        MovieKind,
        SeriesSeason,
        SeriesEpisode,
        SeriesIMDBParent,
        SubLanguageID,
        ISO639,
        LanguageName,
        UserID,
        UserRank,
        UserNickName,
        SubAddDate,
        SubAuthorComment,
        SubFeatured,
        SubComments,
        SubDownloadsCnt,
        SubHearingImpaired,
        SubForeignPartsOnly,
        SubRating,
        SubHD,
        SubBad,
        SubActualCD,
        SubSumCD,
        MatchedBy,
        QueryNumber,
        SubtitlesLink,
        SubDownloadLink,
        ZipDownloadLink;


        public static <V> EnumMap<Property, V> asEnumMap(Map<String, V> map) {
            EnumMap<Property, V> enumMap = new EnumMap<Property, V>(Property.class);
            for (Map.Entry<String, V> entry : map.entrySet()) {
                try {
                    enumMap.put(Property.valueOf(entry.getKey()), entry.getValue());
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
            return enumMap;
        }
    }
}

