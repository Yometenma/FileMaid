package net.filemaid.server.api;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import net.filemaid.application.service.SearchMetadataService.MetadataProviderUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<Map<String, String>> ioFailure(IOException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to access the requested storage path"));
    }

    @ExceptionHandler(MetadataProviderUnavailableException.class)
    ResponseEntity<Map<String, String>> providerUnavailable(MetadataProviderUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", exception.getMessage()));
    }
}
