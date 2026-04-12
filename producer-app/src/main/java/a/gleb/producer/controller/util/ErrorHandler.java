package a.gleb.producer.controller.util;

import a.gleb.producer.exception.ProducerAppException;
import a.gleb.producer.model.response.ErrorResponse;
import a.gleb.producer.model.response.ErrorResponse.FieldErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the producer application.
 * <p>
 * This class intercepts exceptions thrown by controllers and provides
 * a consistent error response format ({@link ErrorResponse}) across the entire application.
 * </p>
 *
 * <p>Handled exception types:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} – triggered when request validation fails.
 *       Produces an HTTP 400 response with a list of field-specific errors.</li>
 *   <li>{@link ProducerAppException} – custom application exception.
 *       Produces a response using the HTTP status and message contained in the exception.</li>
 *   <li>{@link Exception} – any other unhandled exception.
 *       Produces an HTTP 500 response with a generic error message and the original exception details.</li>
 * </ul>
 *
 * <p>All error responses are wrapped in a {@link ResponseEntity}
 * with the appropriate HTTP status code and a body of type {@link ErrorResponse}.</p>
 *
 * @author [a.gleb]
 * @see ErrorResponse
 * @see FieldErrorResponse
 * @since 1.0
 */
@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Object> details = Map.of(
                "validationErrors",
                ex.getFieldErrors()
                        .stream()
                        .map(it -> new FieldErrorResponse(it.getField(), it.getDefaultMessage()))
                        .toList()
        );

        log.error("MethodArgumentNotValidException, [message={}]", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation error", details);
    }

    @ExceptionHandler(ProducerAppException.class)
    public ResponseEntity<ErrorResponse> producerAppException(ProducerAppException ex) {
        log.error("ProducerAppException, [message={}]", ex.getMessage(), ex);
        return buildErrorResponse(ex.getHttpStatus(), ex.getMessage(), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolationException(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorResponse(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.error("ConstraintViolationException, [message={}]", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation error", Map.of("validationErrors", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exception(Exception ex) {
        log.error("UnhandledException, [message={}]", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", Map.of("details", ex.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> missingRequestHeaderException(MissingRequestHeaderException ex) {
        log.error("MissingRequestHeaderException: [message={}]", ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation error",
                Map.of("details", List.of(new FieldErrorResponse(ex.getHeaderName(), "Missing header")))
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            Map<String, Object> details
    ) {
        var response = new ErrorResponse(message, details);

        return ResponseEntity.status(status)
                .body(response);
    }
}
