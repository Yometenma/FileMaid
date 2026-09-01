package net.filemaid.server;

import java.util.function.Supplier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

/**
 * Always materializes the deferred CSRF token so CookieCsrfTokenRepository
 * writes the XSRF-TOKEN cookie on every request. The plain attribute handler
 * only stores a lazy supplier for view rendering, which never happens in this
 * JSON-only API, so after login consumes the token the cookie would otherwise
 * stay missing and every subsequent write would fail with 403.
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final CsrfTokenRequestAttributeHandler delegate = new CsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        this.delegate.handle(request, response, csrfToken);
        csrfToken.get(); // materialize so CookieCsrfTokenRepository writes the cookie
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
