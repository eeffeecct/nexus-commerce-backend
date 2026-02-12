package ru.nexus.inventory.exception;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        log.info("Validation failed: {}", ex.getBindingResult());

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());

        return createResponseEntity(problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock failure: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource has been updated by another user. Please refresh and try again."
        );
        problem.setTitle("Resource Conflict");
        problem.setType(URI.create("https://api.nexus.ru/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ProblemDetail handleProductNotFound(InventoryNotFoundException ex) {
        log.warn("Inventory not found: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Inventory Not Found");
        problem.setType(URI.create("https://api.nexus.ru/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleProductNotFound(InsufficientStockException ex) {
        log.warn("Not enough in stock: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Not enough in stock");
        problem.setType(URI.create("https://api.nexus.ru/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());

        return problem;
    }
}
