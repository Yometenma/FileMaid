package net.filemaid.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Movie;
import net.filemaid.web.OpenSubtitlesXmlRpcSubtitleDescriptor;
import net.filemaid.web.SubtitleSearchResult;
import net.filemaid.web.WebRequest;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.util.Base64;

public class OpenSubtitlesXmlRpc {
    private final String useragent;
    private String token;

    public OpenSubtitlesXmlRpc(String string) {
        this.useragent = string;
    }

    public String getUserAgent() {
        return this.useragent;
    }

    public synchronized String token() {
        return Objects.requireNonNull(this.token, "token");
    }

    public synchronized void login(String string, String string2, String string3) throws Exception {
        Map<?, ?> map = this.invoke("LogIn", string, string2, string3, this.useragent);
        this.token = map.get("token").toString();
    }

    public synchronized void logout() throws Exception {
        try {
            this.invoke("LogOut", this.token());
        }
        finally {
            this.token = null;
        }
    }

    public synchronized boolean isLoggedOn() {
        return this.token != null;
    }

    public Map<String, String> getServerInfo() throws Exception {
        return (Map)this.invoke("ServerInfo", this.token());
    }

    public List<OpenSubtitlesXmlRpcSubtitleDescriptor> searchSubtitles(Collection<Query> collection) throws Exception {
        OpenSubtitlesXmlRpcSubtitleDescriptor.checkDownloadQuota();
        ArrayList<OpenSubtitlesXmlRpcSubtitleDescriptor> arrayList = new ArrayList<OpenSubtitlesXmlRpcSubtitleDescriptor>();
        Map<?, ?> map = this.invoke("SearchSubtitles", this.token(), collection);
        try {
            List<Map> list = (List)map.get("data");
            for (Map map2 : list) {
                arrayList.add(new OpenSubtitlesXmlRpcSubtitleDescriptor(OpenSubtitlesXmlRpcSubtitleDescriptor.Property.asEnumMap(map2)));
            }
        }
        catch (ClassCastException classCastException) {
            // empty catch block
        }
        return arrayList;
    }

    public List<SubtitleSearchResult> searchMoviesOnIMDB(String string) throws Exception {
        try {
            Map<?, ?> map = this.invoke("SearchMoviesOnIMDB", this.token(), string);
            List<Map> list = (List)map.get("data");
            ArrayList<SubtitleSearchResult> arrayList = new ArrayList<SubtitleSearchResult>();
            Pattern pattern = Pattern.compile("(.+)[(](\\d{4})([/]I+)?[)]");
            for (Map map2 : list) {
                try {
                    String string2 = (String)map2.get("id");
                    if (!string2.matches("\\d{1,11}")) {
                        throw new IllegalArgumentException("Illegal IMDb movie ID: Must be a 7-digit number");
                    }
                    Matcher matcher = pattern.matcher((CharSequence)map2.get("title"));
                    if (!matcher.find()) {
                        throw new IllegalArgumentException("Illegal title: Must be in 'name (year)' format");
                    }
                    String string3 = matcher.group(1).replaceAll("\"", "").trim();
                    int n = Integer.parseInt(matcher.group(2));
                    arrayList.add(new SubtitleSearchResult(Integer.parseInt(string2), string3, n, null));
                }
                catch (Exception exception) {
                    Logging.debug.fine(Logging.cause("Ignore movie", map2, exception));
                }
            }
            return arrayList;
        }
        catch (ClassCastException classCastException) {
            throw new XmlRpcException("Illegal XMLRPC response on searchMoviesOnIMDB");
        }
    }

    public Movie getIMDBMovieDetails(int n) throws Exception {
        Map<?, ?> map = this.invoke("GetIMDBMovieDetails", this.token(), n);
        try {
            Map map2 = (Map)map.get("data");
            String string = (String)map2.get("title");
            int n2 = Integer.parseInt((String)map2.get("year"));
            return Movie.IMDB(string, n2, n);
        }
        catch (RuntimeException runtimeException) {
            Logging.debug.warning(Logging.cause("Failed to lookup movie by imdbid", n, runtimeException));
            return null;
        }
    }

    private Map<String, Object> getUploadStruct(BaseInfo baseInfo, SubFile ... subFileArray) {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        if (baseInfo != null) {
            linkedHashMap.put("baseinfo", baseInfo);
        }
        for (int i = 0; i < subFileArray.length; ++i) {
            linkedHashMap.put("cd" + (i + 1), subFileArray[i]);
        }
        return linkedHashMap;
    }

    public TryUploadResponse tryUploadSubtitles(SubFile ... subFileArray) throws Exception {
        Map<String, Object> map = this.getUploadStruct(null, subFileArray);
        Map<?, ?> map2 = this.invoke("TryUploadSubtitles", this.token(), map);
        boolean bl = map2.get("alreadyindb").toString().equals("0");
        ArrayList<Map<String, String>> arrayList = new ArrayList<Map<String, String>>();
        if (map2.get("data") instanceof Map) {
            arrayList.add((Map)map2.get("data"));
        } else if (map2.get("data") instanceof List) {
            arrayList.addAll((List)map2.get("data"));
        }
        return new TryUploadResponse(bl, arrayList);
    }

    public void uploadSubtitles(BaseInfo baseInfo, SubFile ... subFileArray) throws Exception {
        Map<String, Object> map = this.getUploadStruct(baseInfo, subFileArray);
        Map<?, ?> map2 = this.invoke("UploadSubtitles", this.token(), map);
        if (Integer.valueOf(1).equals(map2.get("alreadyindb"))) {
            throw new FileAlreadyExistsException("Subtitle already exists in database: " + map2.get("data"));
        }
    }

    public List<String> detectLanguage(byte[] byArray) throws Exception {
        String string = OpenSubtitlesXmlRpc.encodeData(byArray);
        Map<?, ?> map = this.invoke("DetectLanguage", this.token(), Collections.singleton(string));
        ArrayList<String> arrayList = new ArrayList<String>(2);
        if (map.containsKey("data")) {
            arrayList.addAll(((Map)map.get("data")).values());
        }
        return arrayList;
    }

    public Map<String, Integer> checkSubHash(Collection<String> collection) throws Exception {
        Map<?, ?> map = this.invoke("CheckSubHash", this.token(), collection);
        Map<?, ?> map2 = (Map)map.get("data");
        HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        for (Map.Entry<?, ?> entry : map2.entrySet()) {
            hashMap.put((String)entry.getKey(), Integer.parseInt(entry.getValue().toString()));
        }
        return hashMap;
    }

    public Map<String, List<SubtitleSearchResult>> guessMovie(Collection<String> collection) throws Exception {
        HashMap<String, List<SubtitleSearchResult>> hashMap = new HashMap<String, List<SubtitleSearchResult>>();
        Map<?, ?> map = this.invoke("GuessMovieFromString", this.token(), collection);
        Object obj = map.get("data");
        if (obj instanceof Map) {
            Map map2 = (Map)obj;
            for (String string : collection) {
                Map map3;
                ArrayList<SubtitleSearchResult> arrayList = new ArrayList<SubtitleSearchResult>();
                Map map4 = (Map)map2.get(string);
                if (map4 != null && (map3 = (Map)map4.get("BestGuess")) != null) {
                    String string2 = String.valueOf(map3.get("MovieName"));
                    String string3 = String.valueOf(map3.get("MovieKind"));
                    int n = Integer.parseInt(String.valueOf(map3.get("IDMovieIMDB")));
                    int n2 = Integer.parseInt(String.valueOf(map3.get("MovieYear")));
                    arrayList.add(new SubtitleSearchResult(n, string2, n2, string3));
                }
                hashMap.put(string, arrayList);
            }
        }
        return hashMap;
    }

    public Map<String, Movie> checkMovieHash(Collection<String> collection, int n) throws Exception {
        HashMap<String, Movie> hashMap = new HashMap<String, Movie>();
        Map<?, ?> map = this.invoke("CheckMovieHash2", this.token(), collection);
        Object obj = map.get("data");
        if (obj instanceof Map) {
            Map<?, ?> map2 = (Map)obj;
            for (Map.Entry<?, ?> entry : map2.entrySet()) {
                if (!(entry.getValue() instanceof List)) continue;
                String string = (String)entry.getKey();
                ArrayList<Movie> arrayList = new ArrayList<Movie>();
                List list = (List)entry.getValue();
                for (Object e : list) {
                    Map map3;
                    int n2;
                    if (!(e instanceof Map) || (n2 = Integer.parseInt((String)(map3 = (Map)e).get("SeenCount"))) < n) continue;
                    String string2 = (String)map3.get("MovieName");
                    int n3 = Integer.parseInt((String)map3.get("MovieYear"));
                    int n4 = Integer.parseInt((String)map3.get("MovieImdbID"));
                    arrayList.add(Movie.IMDB(string2, n3, n4));
                }
                if (arrayList.size() == 1) {
                    hashMap.put(string, (Movie)arrayList.get(0));
                    continue;
                }
                if (arrayList.size() <= 1) continue;
                Logging.debug.warning(Logging.message("Ignore hash match due to hash collision", arrayList));
            }
        }
        return hashMap;
    }

    public Map<String, String> getSubLanguages() throws Exception {
        return this.getSubLanguages("en");
    }

    public Map<String, String> getSubLanguages(String string) throws Exception {
        Map<?, ?> map = this.invoke("GetSubLanguages", string);
        HashMap<String, String> hashMap = new HashMap<String, String>();
        for (Map map2 : (List<Map>)map.get("data")) {
            hashMap.put((String)map2.get("SubLanguageID"), (String)map2.get("ISO639"));
        }
        return hashMap;
    }

    public void noOperation() throws Exception {
        this.invoke("NoOperation", this.token());
    }

    protected Map<?, ?> invoke(final String string, final Object ... objectArray) throws Exception {
        try {
            XmlRpcClient xmlRpcClient = new XmlRpcClient(this.getXmlRpcUrl(), false){

                public void parse(InputStream inputStream) {
                    ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
                    try {
                        byteBufferOutputStream.transferFully(new GZIPInputStream(inputStream, 65536));
                    }
                    catch (IOException iOException) {
                        throw new IllegalStateException("Network Error", iOException);
                    }
                    Logging.debug.finest(() -> String.format("[RPC] %s %s", string, Arrays.deepToString(objectArray)));
                    Logging.debug.finest(WebRequest.log(byteBufferOutputStream.getByteBuffer()));
                    try {
                        WebRequest.validateXml(new ByteBufferInputStream(byteBufferOutputStream.getByteBuffer()));
                    }
                    catch (Exception exception) {
                        throw new IllegalStateException("Bad Response", new InvalidResponseException("Invalid XML", WebRequest.getTextContent(byteBufferOutputStream.getByteBuffer(), "application/xml"), exception));
                    }
                    super.parse((InputStream)new ByteBufferInputStream(byteBufferOutputStream.getByteBuffer()));
                }
            };
            xmlRpcClient.setRequestProperty("Accept-Encoding", "gzip");
            Map map = (Map)xmlRpcClient.invoke(string, objectArray);
            this.checkResponse(map);
            return map;
        }
        catch (XmlRpcFault xmlRpcFault) {
            if (xmlRpcFault.getErrorCode() == 406) {
                OpenSubtitlesXmlRpc openSubtitlesXmlRpc = this;
                synchronized (openSubtitlesXmlRpc) {
                    this.token = null;
                }
            }
            throw xmlRpcFault;
        }
        catch (Exception exception) {
            throw (Exception)Logging.getRootCause(exception);
        }
    }

    protected URL getXmlRpcUrl() {
        return WebRequest.parseURL(System.getProperty("net.filemaid.OpenSubtitlesXmlRpc.url", "https://api.opensubtitles.org/xml-rpc"));
    }

    protected static String encodeData(byte[] byArray) {
        DeflaterInputStream deflaterInputStream = new DeflaterInputStream(new ByteArrayInputStream(byArray));
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(byArray.length);
        try {
            byteBufferOutputStream.transferFully(deflaterInputStream);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return new String(Base64.encode((byte[])byteBufferOutputStream.getByteArray()));
    }

    protected void checkResponse(Map<?, ?> map) throws XmlRpcFault {
        Object obj;
        Object obj2 = map.get("message");
        if (obj2 != null) {
            Logging.debug.finest(Logging.message("[RPC] Status Message", obj2));
        }
        if ((obj = map.get("status")) == null || "200 OK".equals(obj)) {
            return;
        }
        Integer n = StringUtilities.matchInteger(obj.toString());
        throw new XmlRpcFault(n == null ? 0 : n, obj.toString());
    }

    public static final class BaseInfo
    extends HashMap<String, Object> {
        public void setIDMovieImdb(int n) {
            this.put("idmovieimdb", Integer.toString(n));
        }

        public void setSubLanguageID(String string) {
            this.put("sublanguageid", string);
        }

        public void setMovieReleaseName(String string) {
            this.put("moviereleasename", string);
        }

        public void setMovieAka(String string) {
            this.put("movieaka", string);
        }

        public void setSubAuthorComment(String string) {
            this.put("subauthorcomment", string);
        }
    }

    public static final class SubFile
    extends HashMap<String, Object> {
        public void setSubHash(String string) {
            this.put("subhash", string);
        }

        public void setSubFileName(String string) {
            this.put("subfilename", string);
        }

        public void setMovieHash(String string) {
            this.put("moviehash", string);
        }

        public void setMovieByteSize(long l) {
            this.put("moviebytesize", Long.toString(l));
        }

        public void setMovieFileName(String string) {
            this.put("moviefilename", string);
        }

        public void setSubContent(byte[] byArray) {
            this.put("subcontent", OpenSubtitlesXmlRpc.encodeData(byArray));
        }

        public void setMovieTimeMS(Duration duration) {
            if (duration.toMillis() > 0L) {
                this.put("movietimems", duration.toMillis());
            }
        }

        public void setMovieFPS(double d) {
            if (d > 0.0) {
                this.put("moviefps", Double.toString(d));
            }
        }

        public void setMovieFrames(String string) {
            if (string.length() > 0) {
                this.put("movieframes", string);
            }
        }

        @Override
        public String toString() {
            return String.format("(%s, %s)", this.get("moviefilename"), this.get("subfilename"));
        }
    }

    public static final class TryUploadResponse {
        private final boolean uploadRequired;
        private final List<Map<String, String>> subtitleData;

        private TryUploadResponse(boolean bl, List<Map<String, String>> list) {
            this.uploadRequired = bl;
            this.subtitleData = list;
        }

        public boolean isUploadRequired() {
            return this.uploadRequired;
        }

        public List<Map<String, String>> getSubtitleData() {
            return this.subtitleData;
        }

        public String toString() {
            return String.format("TryUploadResponse: %s => %s", this.uploadRequired, this.subtitleData);
        }
    }

    public static final class Query
    extends HashMap<String, Object>
    implements Serializable {
        private Query(String ... stringArray) {
            this.put("sublanguageid", StringUtilities.join(stringArray, (CharSequence)","));
        }

        public static Query forHash(String string, long l, String ... stringArray) {
            Query query = new Query(stringArray);
            query.put("moviehash", string);
            query.put("moviebytesize", Long.toString(l));
            return query;
        }

        public static Query forTag(String string, String ... stringArray) {
            Query query = new Query(stringArray);
            query.put("tag", string);
            return query;
        }

        public static Query forImdbId(int n, int n2, int n3, String ... stringArray) {
            Query query = new Query(stringArray);
            query.put("imdbid", Integer.toString(n));
            if (n2 >= 0) {
                query.put("season", Integer.toString(n2));
            }
            if (n3 >= 0) {
                query.put("episode", Integer.toString(n3));
            }
            return query;
        }
    }
}

