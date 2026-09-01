package net.filemaid.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.invoke.CallSite;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import net.filemaid.Logging;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.Digest;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.HttpClientError;
import net.filemaid.web.HttpNetworkError;
import net.filemaid.web.HttpServerError;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public final class WebRequest {
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_HEAD = "HEAD";
    public static final String HTTP_DELETE = "DELETE";
    private static final boolean LEGACY_HTTP_CLIENT = Boolean.parseBoolean(System.getProperty("net.filemaid.web.WebRequest.v1"));
    private static final boolean LOG_RESPONSE_CONTENT = Boolean.parseBoolean(System.getProperty("net.filemaid.web.WebRequest.log.response"));
    private static final String HTTP_USER_AGENT = System.getProperty("http.agent");
    private static final String HTTP_ENCODING_GZIP = "gzip";
    private static final String HTTP_CHARSET_UTF8 = "UTF-8";
    private static final Duration TIMEOUT = Duration.ofSeconds(60L);
    private static HttpClient httpClient;

    public static ByteBuffer fetch(URL uRL) throws IOException {
        return WebRequest.fetch(uRL, 0L, null, null, null);
    }

    public static ByteBuffer fetch(URL uRL, long l, String string, Map<String, String> map, BiConsumer<String, String> biConsumer) throws IOException {
        if (LEGACY_HTTP_CLIENT) {
            return WebRequest.httpGetV1(uRL, l, string, map, biConsumer);
        }
        return WebRequest.httpRequestV2(HTTP_GET, uRL, l, string, null, null, map, biConsumer);
    }

    public static ByteBuffer post(URL uRL, Map<String, ?> map, Map<String, String> map2) throws IOException {
        byte[] byArray = WebRequest.encodeParameters(map).getBytes(StandardCharsets.UTF_8);
        if (map2 != null && HTTP_ENCODING_GZIP.equals(map2.get("Content-Encoding"))) {
            byArray = WebRequest.gzip(byArray);
        }
        return WebRequest.post(HTTP_POST, uRL, byArray, "application/x-www-form-urlencoded", map2, null);
    }

    public static ByteBuffer post(URL uRL, byte[] byArray, String string, Map<String, String> map) throws IOException {
        return WebRequest.post(HTTP_POST, uRL, byArray, string, map, null);
    }

    public static ByteBuffer post(String string, URL uRL, byte[] byArray, String string2, Map<String, String> map, BiConsumer<String, String> biConsumer) throws IOException {
        if (LEGACY_HTTP_CLIENT) {
            return WebRequest.httpPostV1(string, uRL, byArray, string2, map, biConsumer);
        }
        return WebRequest.httpRequestV2(string, uRL, 0L, null, byArray, string2, map, biConsumer);
    }

    public static int status(String string, URL uRL, Map<String, String> map) throws IOException {
        if (LEGACY_HTTP_CLIENT) {
            return WebRequest.httpStatusV1(string, uRL, map);
        }
        return WebRequest.httpStatusV2(string, uRL, map);
    }

    private static void consumeResponseHeader(BiConsumer<String, String> biConsumer, Supplier<Map<String, List<String>>> supplier) {
        if (biConsumer != null) {
            supplier.get().forEach((string, list) -> {
                if (string != null) {
                    list.forEach(string2 -> biConsumer.accept(string.toLowerCase(Locale.ROOT), (String)string2));
                }
            });
        }
    }

    private static String encodeParameter(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

    public static String encodeParameters(Map<?, ?> map) {
        return map.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null).map(entry -> entry.getKey() + "=" + WebRequest.encodeParameter(entry.getValue().toString())).collect(Collectors.joining("&"));
    }

    public static String encodeParameters(String ... stringArray) {
        return WebRequest.encodeParameters(WebRequest.mapParameters(stringArray));
    }

    public static Map<String, String> mapParameters(String ... stringArray) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(stringArray.length / 2);
        for (int i = 0; i < stringArray.length; i += 2) {
            String string = stringArray[i];
            String string2 = stringArray[i + 1];
            if (string == null || string2 == null) continue;
            linkedHashMap.put(string.toString(), string2.toString());
        }
        return linkedHashMap;
    }

    public static byte[] gzip(byte[] byArray) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(byArray.length);
        try (GZIPOutputStream gZIPOutputStream = new GZIPOutputStream(byteBufferOutputStream);){
            gZIPOutputStream.write(byArray);
        }
        return byteBufferOutputStream.getByteArray();
    }

    public static byte[] gunzip(byte[] byArray) throws IOException {
        if (byArray.length >= 2 && byArray[0] == 31 && byArray[1] == -117) {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
            try (GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(byArray));){
                byteBufferOutputStream.transferFully(gZIPInputStream);
            }
            return byteBufferOutputStream.getByteArray();
        }
        return byArray;
    }

    public static ByteBuffer gunzip(ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer.remaining() >= 2 && byteBuffer.get(byteBuffer.position()) == 31 && byteBuffer.get(byteBuffer.position() + 1) == -117) {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
            try (GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteBufferInputStream(byteBuffer));){
                byteBufferOutputStream.transferFully(gZIPInputStream);
            }
            return byteBufferOutputStream.getByteBuffer();
        }
        return byteBuffer;
    }

    public static ByteBuffer gunzip(InputStream inputStream) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        byteBufferOutputStream.transferFully(inputStream);
        ByteBuffer byteBuffer = byteBufferOutputStream.getByteBuffer();
        int n = byteBuffer.limit() - 1;
        for (int i = byteBuffer.position(); i < n; ++i) {
            if (byteBuffer.get(i) != 31 || byteBuffer.get(i + 1) != -117) continue;
            byteBuffer.position(i);
            return WebRequest.gunzip(byteBuffer);
        }
        return ByteBuffer.allocate(0);
    }

    public static Document getDocument(String string) throws Exception {
        if (string.isEmpty()) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }
        return WebRequest.getDocument(new InputSource(new StringReader(string)));
    }

    public static Document getDocument(InputStream inputStream) throws Exception {
        return WebRequest.getDocument(new InputSource(inputStream));
    }

    public static Document getDocument(InputSource inputSource) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setValidating(false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
        documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
        return documentBuilderFactory.newDocumentBuilder().parse(inputSource);
    }

    public static void validateXml(InputStream inputStream) throws SAXException, ParserConfigurationException, IOException {
        WebRequest.validateXml(new InputSource(inputStream));
    }

    public static void validateXml(InputSource inputSource) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory sAXParserFactory = SAXParserFactory.newInstance();
        sAXParserFactory.setValidating(false);
        sAXParserFactory.setNamespaceAware(false);
        XMLReader xMLReader = sAXParserFactory.newSAXParser().getXMLReader();
        xMLReader.setErrorHandler(new DefaultHandler());
        xMLReader.parse(inputSource);
    }

    public static Supplier<String> log(URL uRL, long l, String string) {
        return () -> {
            ArrayList<CallSite> arrayList = new ArrayList<CallSite>(2);
            if (string != null) {
                arrayList.add((CallSite)((Object)("If-None-Match: " + string)));
            }
            if (l > 0L) {
                arrayList.add((CallSite)((Object)("If-Modified-Since: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC)))));
            }
            return "Fetch resource: " + WebRequest.getFilePath(uRL) + " " + arrayList;
        };
    }

    public static Supplier<String> log(ByteBuffer byteBuffer) {
        return () -> {
            if (byteBuffer == null) {
                return "Received 0 bytes";
            }
            if (LOG_RESPONSE_CONTENT) {
                return "Received " + FileUtilities.formatSize(byteBuffer.remaining()) + "\n" + WebRequest.getTextContent(byteBuffer, "application/octet-stream") + "\n";
            }
            return "Received " + FileUtilities.formatSize(byteBuffer.remaining());
        };
    }

    public static CharSequence getTextContent(ByteBuffer byteBuffer, String string) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(byteBuffer.duplicate());
        }
        catch (Exception exception) {
            if ((long)byteBuffer.remaining() < 4000L) {
                return WebRequest.getDataURI(byteBuffer, string);
            }
            return "[" + Digest.md5(byteBuffer.duplicate()) + "]";
        }
    }

    public static String getDataURI(ByteBuffer byteBuffer, String string) {
        return "data:" + string + ";base64," + StandardCharsets.UTF_8.decode(Base64.getUrlEncoder().encode(byteBuffer.duplicate()));
    }

    public static String getFilePath(Object object) {
        if (LOG_RESPONSE_CONTENT) {
            return object.toString();
        }
        String string = object.toString();
        int n = string.indexOf(63);
        if (n > 0) {
            return string.substring(0, n);
        }
        return string;
    }

    public static URL newURL(String string) throws MalformedURLException, URISyntaxException {
        return new URI(string).toURL();
    }

    public static URL newURL(URI uRI, String string) throws MalformedURLException {
        return uRI.resolve(string).toURL();
    }

    public static URL parseURL(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        try {
            return WebRequest.newURL(string);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Invalid URL", string, exception));
            return null;
        }
    }

    private static ByteBuffer httpGetV1(URL uRL, long l, String string, Map<String, String> map, BiConsumer<String, String> biConsumer) throws IOException, HttpServerError {
        ByteBufferOutputStream byteBufferOutputStream;
        HttpURLConnection httpURLConnection;
        block18: {
            httpURLConnection = WebRequest.newRequest(uRL);
            if (l > 0L) {
                httpURLConnection.setIfModifiedSince(l);
            } else if (string != null) {
                httpURLConnection.addRequestProperty("If-None-Match", string);
            }
            try {
                httpURLConnection.addRequestProperty("Accept-Encoding", HTTP_ENCODING_GZIP);
                httpURLConnection.addRequestProperty("Accept-Charset", HTTP_CHARSET_UTF8);
            }
            catch (IllegalStateException illegalStateException) {
                Logging.debug.warning(Logging.cause(httpURLConnection.getRequestMethod(), httpURLConnection.getURL(), illegalStateException));
            }
            if (map != null) {
                map.forEach(httpURLConnection::addRequestProperty);
            }
            WebRequest.checkErrorCode(httpURLConnection);
            if (WebRequest.notModified(httpURLConnection)) {
                if (l > 0L || string != null) {
                    return null;
                }
                throw new HttpServerError(520, "Unexpected HTTP response [304 Not Modified] even though HTTP request header did not specify [If-Modified-Since] or [If-None-Match]", uRL);
            }
            int n = httpURLConnection.getContentLength();
            String string2 = httpURLConnection.getContentEncoding();
            byteBufferOutputStream = new ByteBufferOutputStream(n);
            try (InputStream inputStream = httpURLConnection.getInputStream();){
                if (HTTP_ENCODING_GZIP.equalsIgnoreCase(string2)) {
                    byteBufferOutputStream.transferFully(new GZIPInputStream(inputStream));
                } else {
                    byteBufferOutputStream.transferFully(inputStream);
                }
            }
            catch (IOException iOException) {
                if (n < 0) break block18;
                throw iOException;
            }
        }
        WebRequest.consumeResponseHeader(biConsumer, httpURLConnection::getHeaderFields);
        return byteBufferOutputStream.getByteBuffer();
    }

    private static ByteBuffer httpPostV1(String string, URL uRL, byte[] byArray, String string2, Map<String, String> map, BiConsumer<String, String> biConsumer) throws IOException, ProtocolException {
        ByteBufferOutputStream byteBufferOutputStream;
        HttpURLConnection httpURLConnection;
        block11: {
            httpURLConnection = WebRequest.newRequest(uRL);
            httpURLConnection.addRequestProperty("Content-Length", String.valueOf(byArray.length));
            httpURLConnection.addRequestProperty("Content-Type", string2);
            httpURLConnection.setRequestMethod(string);
            httpURLConnection.setDoOutput(true);
            if (map != null) {
                map.forEach(httpURLConnection::addRequestProperty);
            }
            WebRequest.postData(httpURLConnection, byArray);
            WebRequest.checkErrorCode(httpURLConnection);
            int n = httpURLConnection.getContentLength();
            String string3 = httpURLConnection.getContentEncoding();
            byteBufferOutputStream = new ByteBufferOutputStream(n);
            try (InputStream inputStream = httpURLConnection.getInputStream();){
                if (HTTP_ENCODING_GZIP.equalsIgnoreCase(string3)) {
                    byteBufferOutputStream.transferFully(new GZIPInputStream(inputStream));
                } else {
                    byteBufferOutputStream.transferFully(inputStream);
                }
            }
            catch (IOException iOException) {
                if (n < 0) break block11;
                throw iOException;
            }
        }
        WebRequest.consumeResponseHeader(biConsumer, httpURLConnection::getHeaderFields);
        return byteBufferOutputStream.getByteBuffer();
    }

    private static int httpStatusV1(String string, URL uRL, Map<String, String> map) throws IOException {
        HttpURLConnection httpURLConnection = WebRequest.newRequest(uRL);
        httpURLConnection.setRequestMethod(string);
        if (map != null) {
            map.forEach(httpURLConnection::addRequestProperty);
        }
        return httpURLConnection.getResponseCode();
    }

    private static HttpURLConnection newRequest(URL uRL) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection)uRL.openConnection();
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setAllowUserInteraction(false);
        if (uRL.getUserInfo() != null) {
            String string = Base64.getEncoder().encodeToString(uRL.getUserInfo().getBytes(StandardCharsets.UTF_8));
            httpURLConnection.setRequestProperty("Authorization", "Basic " + string);
        }
        return httpURLConnection;
    }

    private static boolean notModified(HttpURLConnection httpURLConnection) throws IOException {
        return httpURLConnection.getResponseCode() == 304;
    }

    private static void checkErrorCode(HttpURLConnection httpURLConnection) throws IOException {
        try {
            Logging.debug.finest(WebRequest.log(httpURLConnection));
            if (WebRequest.notModified(httpURLConnection)) {
                return;
            }
            if (httpURLConnection.getResponseCode() >= 500) {
                throw new HttpServerError(httpURLConnection, WebRequest.consume(httpURLConnection));
            }
            if (httpURLConnection.getResponseCode() >= 300) {
                throw new HttpClientError(httpURLConnection, WebRequest.consume(httpURLConnection));
            }
        }
        catch (ConnectException | SocketTimeoutException | SSLHandshakeException iOException) {
            throw new HttpNetworkError(httpURLConnection, (Exception)iOException);
        }
    }

    private static void postData(HttpURLConnection httpURLConnection, byte[] byArray) throws IOException {
        try {
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(byArray);
            outputStream.close();
        }
        catch (ConnectException | SocketTimeoutException | SSLHandshakeException iOException) {
            throw new HttpNetworkError(httpURLConnection, (Exception)iOException);
        }
    }

    private static ByteBuffer consume(HttpURLConnection httpURLConnection) {
        InputStream inputStream = httpURLConnection.getErrorStream();
        try {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
            byteBufferOutputStream.transferFully(inputStream);
            Logging.debug.finest(WebRequest.log(byteBufferOutputStream.getByteBuffer()));
            return byteBufferOutputStream.getByteBuffer();
        } catch (Exception exception) {
            Logging.debug.warning(Logging.cause(httpURLConnection.getRequestMethod(), httpURLConnection.getURL(), exception));
            return ByteBuffer.allocate(0);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Supplier<String> log(HttpURLConnection httpURLConnection) {
        return () -> {
            Formatter formatter = new Formatter();
            try {
                formatter.format("[%s %s] => [%s %s]", httpURLConnection.getRequestMethod(), WebRequest.getFilePath(httpURLConnection.getURL()), httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage());
            }
            catch (Exception exception) {
                formatter.format("[%s] => [%s]", WebRequest.getFilePath(httpURLConnection.getURL()), exception);
            }
            if (LOG_RESPONSE_CONTENT) {
                httpURLConnection.getHeaderFields().forEach((string, list) -> {
                    for (String string2 : list) {
                        if (string == null) {
                            formatter.format("\n%s", string2);
                            continue;
                        }
                        formatter.format("\n%s: %s", string, string2);
                    }
                });
            }
            return formatter.toString();
        };
    }

    private static ByteBuffer httpRequestV2(String string, URL uRL, long l, String string2, byte[] byArray, String string3, Map<String, String> map, BiConsumer<String, String> biConsumer) throws IOException {
        try {
            HttpResponse<byte[]> httpResponse;
            HttpRequest.Builder builder = WebRequest.newRequest(uRL.toURI());
            builder.header("Accept-Encoding", HTTP_ENCODING_GZIP);
            builder.header("Accept-Charset", HTTP_CHARSET_UTF8);
            if (l > 0L) {
                builder.header("If-Modified-Since", DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(l).atZone(ZoneOffset.UTC)));
            } else if (string2 != null) {
                builder.header("If-None-Match", string2);
            }
            if (map != null) {
                map.forEach(builder::header);
            }
            if (byArray != null) {
                builder.method(string, HttpRequest.BodyPublishers.ofByteArray(byArray));
                builder.header("Content-Type", string3);
            }
            if (WebRequest.notModified(httpResponse = WebRequest.doRequest(builder.build()))) {
                if (l > 0L || string2 != null) {
                    return null;
                }
                throw new HttpServerError(520, "Unexpected HTTP response [304 Not Modified] even though HTTP request header did not specify [If-Modified-Since] or [If-None-Match]", uRL);
            }
            HttpHeaders httpHeaders = httpResponse.headers();
            byte[] byArray2 = httpResponse.body();
            if (httpHeaders == null || byArray2 == null) {
                throw new HttpServerError(httpResponse.statusCode(), "Invalid HTTP Response: " + httpResponse + " | " + httpHeaders + " | " + byArray2, uRL);
            }
            if (HTTP_ENCODING_GZIP.equals(WebRequest.getContentEncoding(httpResponse))) {
                byArray2 = WebRequest.gunzip(byArray2);
            }
            WebRequest.consumeResponseHeader(biConsumer, httpHeaders::map);
            return ByteBuffer.wrap(byArray2);
        }
        catch (URISyntaxException uRISyntaxException) {
            throw new IOException(uRISyntaxException);
        }
        catch (InterruptedException interruptedException) {
            throw new CancellationException("[" + string + " " + uRL + "] has been cancelled");
        }
    }

    private static int httpStatusV2(String string, URL uRL, Map<String, String> map) throws IOException {
        try {
            HttpRequest.Builder builder = WebRequest.newRequest(uRL.toURI());
            builder.method(string, HttpRequest.BodyPublishers.noBody());
            if (map != null) {
                map.forEach(builder::header);
            }
            HttpResponse<Void> httpResponse = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.discarding());
            return httpResponse.statusCode();
        }
        catch (URISyntaxException uRISyntaxException) {
            throw new IOException(uRISyntaxException);
        }
        catch (InterruptedException interruptedException) {
            throw new CancellationException("[" + string + " " + uRL + "] has been cancelled");
        }
    }

    private static HttpRequest.Builder newRequest(URI uRI) throws URISyntaxException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uRI);
        builder.timeout(TIMEOUT);
        if (HTTP_USER_AGENT != null) {
            builder.headers("User-Agent", HTTP_USER_AGENT);
        }
        if (uRI.getUserInfo() != null) {
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(uRI.getUserInfo().getBytes(StandardCharsets.UTF_8)));
            builder.uri(new URI(uRI.getScheme(), null, uRI.getHost(), uRI.getPort(), uRI.getPath(), uRI.getQuery(), uRI.getFragment()));
        }
        return builder;
    }

    private static synchronized HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(TIMEOUT).cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        }
        return httpClient;
    }

    private static HttpResponse<byte[]> doRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        try {
            HttpResponse<byte[]> httpResponse = WebRequest.httpClient().send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            Logging.debug.finest(WebRequest.log(httpResponse));
            if (WebRequest.notModified(httpResponse)) {
                return httpResponse;
            }
            if (httpResponse.statusCode() >= 500) {
                throw new HttpServerError(httpResponse.statusCode(), WebRequest.getStatusMessage(httpResponse), httpResponse.uri());
            }
            if (httpResponse.statusCode() >= 300) {
                throw new HttpClientError(httpResponse.statusCode(), WebRequest.getStatusMessage(httpResponse), httpResponse.uri(), WebRequest.getContentType(httpResponse), ByteBuffer.wrap(httpResponse.body()));
            }
            return httpResponse;
        }
        catch (ConnectException | SocketTimeoutException | HttpTimeoutException | SSLHandshakeException iOException) {
            throw new HttpNetworkError(httpRequest.uri(), (Exception)iOException);
        }
    }

    private static Supplier<String> log(HttpResponse<?> httpResponse) {
        return () -> {
            Formatter formatter = new Formatter();
            try {
                formatter.format("[%s %s] => [%s %s]", httpResponse.request().method(), WebRequest.getFilePath(httpResponse.uri()), httpResponse.statusCode(), WebRequest.getStatusMessage(httpResponse));
            }
            catch (Exception exception) {
                formatter.format("[%s] => [%s]", WebRequest.getFilePath(httpResponse.uri()), exception);
            }
            if (LOG_RESPONSE_CONTENT) {
                httpResponse.headers().map().forEach((string, list) -> {
                    for (String string2 : list) {
                        if (string == null) {
                            formatter.format("\n%s", string2);
                            continue;
                        }
                        formatter.format("\n%s: %s", string, string2);
                    }
                });
            }
            return formatter.toString();
        };
    }

    private static String getContentEncoding(HttpResponse<?> httpResponse) {
        return httpResponse.headers().firstValue("content-encoding").orElse(null);
    }

    private static String getContentType(HttpResponse<?> httpResponse) {
        return httpResponse.headers().firstValue("content-type").orElse(null);
    }

    private static boolean notModified(HttpResponse<?> httpResponse) {
        return httpResponse.statusCode() == 304;
    }

    private static String getStatusMessage(HttpResponse<?> httpResponse) {
        return WebRequest.getStatusMessage(httpResponse.statusCode());
    }

    private static String getStatusMessage(int n) {
        switch (n) {
            case 200: {
                return "OK";
            }
            case 301: {
                return "Moved Permanently";
            }
            case 302: {
                return "Found";
            }
            case 304: {
                return "Not Modified";
            }
            case 400: {
                return "Bad Request";
            }
            case 401: {
                return "Unauthorized Request";
            }
            case 403: {
                return "Forbidden";
            }
            case 406: {
                return "Not Acceptable";
            }
            case 410: {
                return "Gone";
            }
            case 404: {
                return "Not Found";
            }
            case 429: {
                return "Too Many Requests";
            }
            case 500: {
                return "Internal Server Error";
            }
            case 501: {
                return "Not Implemented";
            }
            case 502: {
                return "Bad Gateway";
            }
            case 503: {
                return "Service Unavailable";
            }
            case 504: {
                return "Gateway Timeout";
            }
            case 520: {
                return "Web Server Returned an Unknown Error";
            }
            case 521: {
                return "Web Server Is Down";
            }
            case 522: {
                return "Connection Timed Out";
            }
            case 523: {
                return "Origin Is Unreachable";
            }
            case 524: {
                return "A Timeout Occurred";
            }
            case 527: {
                return "Railgun Error";
            }
        }
        if (n >= 400 && n < 500) {
            return "Client Error " + n;
        }
        if (n >= 500 && n < 600) {
            return "Server Error " + n;
        }
        return "Status Code " + n;
    }

    private WebRequest() {
        throw new UnsupportedOperationException();
    }
}

