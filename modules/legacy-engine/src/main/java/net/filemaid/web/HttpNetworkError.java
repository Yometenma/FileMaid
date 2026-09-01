package net.filemaid.web;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import javax.net.ssl.SSLHandshakeException;

public class HttpNetworkError
extends IOException {
    private final String host;

    public HttpNetworkError(URI uRI, Exception exception) {
        super(exception);
        this.host = uRI.getHost();
    }

    public HttpNetworkError(HttpURLConnection httpURLConnection, Exception exception) {
        super(exception);
        this.host = httpURLConnection.getURL().getHost();
    }

    @Override
    public String getMessage() {
        Throwable throwable = this.getCause();
        if (throwable instanceof SocketTimeoutException) {
            return throwable.getMessage() + ": Unable to connect to " + this.host + " at this time. Please try again later.";
        }
        if (throwable instanceof SSLHandshakeException) {
            return "SSL handshake failed: Unable to establish a secure connection to " + this.host + " at this time. Please check your network configuration.";
        }
        if (throwable instanceof ConnectException) {
            return "Unable to establish a connection to " + this.host + " at this time. Please check your network configuration.";
        }
        return throwable.toString();
    }
}

