package com.fiipractic.stocks.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFoundException(StockNotFoundException ex) {
        logError("stock_not_found", "StockNotFoundException", HttpStatus.NOT_FOUND, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(StockAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleStockAlreadyExistsException(StockAlreadyExistsException ex) {
        logError("stock_already_exists", "StockAlreadyExistsException", HttpStatus.CONFLICT, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(StockInUseException.class)
    public ResponseEntity<ErrorResponse> handleStockInUseException(StockInUseException ex) {
        logError("stock_in_use", "StockInUseException", HttpStatus.CONFLICT, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        logError("user_not_found", "UserNotFoundException", HttpStatus.NOT_FOUND, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PortfolioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioNotFoundException(PortfolioNotFoundException ex) {
        logError("portfolio_not_found", "PortfolioNotFoundException", HttpStatus.NOT_FOUND, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        logError("unauthorized", "UnauthorizedException", HttpStatus.FORBIDDEN, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(UserNotOwnerOfPortfolioException.class)
    public ResponseEntity<ErrorResponse> handleUserNotOwnerOfPortfolioException(UserNotOwnerOfPortfolioException ex) {
        logError("portfolio_access_denied", "UserNotOwnerOfPortfolioException", HttpStatus.NOT_FOUND, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PortfolioLimitException.class)
    public ResponseEntity<ErrorResponse> handlePortfolioLimitException(PortfolioLimitException ex) {
        logError("portfolio_limit_reached", "PortfolioLimitException", HttpStatus.FORBIDDEN, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MarketPriceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleMarketPriceUnavailableException(MarketPriceUnavailableException ex) {
        logError("market_price_unavailable", "MarketPriceUnavailableException", HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logError("invalid_argument", "IllegalArgumentException", HttpStatus.BAD_REQUEST, ex.getMessage(), ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                LocalDateTime.now(),
                errors
        );
        logError("validation_failed", "MethodArgumentNotValidException", HttpStatus.BAD_REQUEST, "Validation failed", ex, false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logError("data_integrity_violation", "DataIntegrityViolationException", HttpStatus.CONFLICT,
                "Operatia nu poate fi efectuata din cauza constrangerilor de date.", ex, false);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Operatia nu poate fi efectuata din cauza constrangerilor de date.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        logError("unexpected_error", ex.getClass().getSimpleName(), HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(), ex, true);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected error",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private void logError(String action,
                          String errorType,
                          HttpStatus status,
                          String message,
                          Exception ex,
                          boolean includeStackTrace) {
        try {
            MDC.put("action", action);
            MDC.put("errorType", errorType);
            MDC.put("httpStatus", String.valueOf(status.value()));

            if (includeStackTrace) {
                log.error(message, ex);
            } else {
                log.error(message);
            }
        } finally {
            MDC.clear();
        }
    }

    // records for error and validation responses

    public record ErrorResponse(int status, String message, LocalDateTime timestamp) {}

    public record ValidationErrorResponse(
            int status,
            String message,
            LocalDateTime timestamp,
            Map<String, String> errors
    ) {}
}