package be.garagepilot.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<?> notFound(ResourceNotFoundException e, HttpServletRequest r) {
    return error(HttpStatus.NOT_FOUND, e.getMessage(), r);
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<?> business(BusinessException e, HttpServletRequest r) {
    return error(HttpStatus.CONFLICT, e.getMessage(), r);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request");
    return error(HttpStatus.BAD_REQUEST, message, r);
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus s, String m, HttpServletRequest r) {
    return ResponseEntity.status(s)
        .body(
            Map.of(
                "timestamp",
                Instant.now(),
                "status",
                s.value(),
                "error",
                s.name(),
                "message",
                m,
                "path",
                r.getRequestURI()));
  }
}
