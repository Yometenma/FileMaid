package net.filemaid.web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import net.filemaid.web.WebRequest;

public class HttpServerError
extends IOException {
    private final int code;
    private final String message;
    private final Object url;

    public HttpServerError(int n, String string, Object object) {
        this.code = n;
        this.message = string;
        this.url = object;
    }

    public HttpServerError(HttpURLConnection httpURLConnection, ByteBuffer byteBuffer) throws IOException {
        this.code = httpURLConnection.getResponseCode();
        this.message = httpURLConnection.getResponseMessage();
        this.url = httpURLConnection.getURL();
    }

    public String getStatus() {
        return this.code + " " + this.message;
    }

    @Override
    public String getMessage() {
        return WebRequest.getFilePath(this.url) + " [" + this.getStatus() + "]";
    }
}

