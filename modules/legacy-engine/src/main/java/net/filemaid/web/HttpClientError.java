package net.filemaid.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import net.filemaid.web.WebRequest;

public class HttpClientError
extends FileNotFoundException {
    private final int code;
    private final String message;
    private final Object url;
    private final String contentType;
    private final ByteBuffer responseContent;

    public HttpClientError(int n, String string, Object object, String string2, ByteBuffer byteBuffer) {
        this.code = n;
        this.message = string;
        this.url = object;
        this.contentType = string2;
        this.responseContent = byteBuffer;
    }

    public HttpClientError(HttpURLConnection httpURLConnection, ByteBuffer byteBuffer) throws IOException {
        this.code = httpURLConnection.getResponseCode();
        this.message = httpURLConnection.getResponseMessage();
        this.url = httpURLConnection.getURL();
        this.contentType = httpURLConnection.getContentType();
        this.responseContent = byteBuffer;
    }

    public boolean isPermanent() {
        return this.code == 404;
    }

    public boolean isUnauthorized() {
        return this.code == 401;
    }

    public boolean isRateLimited() {
        return this.code == 429;
    }

    public String getStatus() {
        return this.code + " " + this.message;
    }

    public Object getResource() {
        return WebRequest.getFilePath(this.url);
    }

    @Override
    public String getMessage() {
        return this.getResource() + " [" + this.getStatus() + "]";
    }

    public boolean isErrorResponse() {
        return this.responseContent.hasRemaining() && this.contentType != null && (this.contentType.startsWith("text") || this.contentType.endsWith("json") || this.contentType.endsWith("charset=utf-8"));
    }

    public String getResponseContent() {
        return WebRequest.getTextContent(this.responseContent, this.contentType).toString();
    }
}

