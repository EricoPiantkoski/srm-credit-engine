package com.srm.creditengine.infrastructure.adapter.in.web;

import com.srm.creditengine.cambio.domain.exception.ExchangeRateConflictException;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateNotFoundException;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoConflictException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoNotFoundException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoVersionConflictException;
import com.srm.creditengine.precificacao.domain.exception.ExchangeRateUnavailableException;
import com.srm.creditengine.precificacao.domain.exception.ReceivableConflictException;
import com.srm.creditengine.shared.domain.exception.DomainException;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MANUAL_RATE_RESOLUTION = "insert the rate manually via PUT /api/taxas-cambio";

    @ExceptionHandler(ExchangeRateConflictException.class)
    public ResponseEntity<ErrorBody> handleExchangeRateConflict(ExchangeRateConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(ReceivableConflictException.class)
    public ResponseEntity<ErrorBody> handleReceivableConflict(ReceivableConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(LiquidacaoConflictException.class)
    public ResponseEntity<ErrorBody> handleLiquidacaoConflict(LiquidacaoConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(LiquidacaoVersionConflictException.class)
    public ResponseEntity<ErrorBody> handleLiquidacaoVersionConflict(LiquidacaoVersionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorBody> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest req) {
        String requestId = org.slf4j.MDC.get("requestId");
        log.warn("requestId={} unique constraint violation on {}", requestId, req.getRequestURI(), ex);

        String message = "Resource already exists";
        Throwable cause = ex.getCause();
        if (cause != null && "org.postgresql.util.PSQLException".equals(cause.getClass().getName())) {
            try {
                java.lang.reflect.Method getSQLState = cause.getClass().getMethod("getSQLState");
                String sqlState = (String) getSQLState.invoke(cause);
                if ("23505".equals(sqlState)) {
                    message = "Duplicate resource";
                }
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorBody(message));
    }

    @ExceptionHandler(LiquidacaoNotFoundException.class)
    public ResponseEntity<ErrorBody> handleLiquidacaoNotFound(LiquidacaoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<ErrorBody> handleExchangeRateNotFound(ExchangeRateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(ExchangeRateProviderUnavailableException.class)
    public ResponseEntity<ErrorBody> handleExchangeRateProviderUnavailable(ExchangeRateProviderUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorBody(ex.getMessage(), MANUAL_RATE_RESOLUTION));
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ResponseEntity<ErrorBody> handleExchangeRateUnavailable(ExchangeRateUnavailableException ex) {
        return ResponseEntity.unprocessableEntity()
            .body(new ErrorBody(ex.getMessage(), MANUAL_RATE_RESOLUTION));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorBody> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorBody(message));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorBody> handleDomainException(DomainException ex) {
        return ResponseEntity.unprocessableEntity()
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(com.srm.creditengine.auth.domain.exception.InvalidCredentialsException.class)
    public ResponseEntity<ErrorBody> handleInvalidCredentials(
            com.srm.creditengine.auth.domain.exception.InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorBody> handleInvalidRefreshToken(
            com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(com.srm.creditengine.auth.domain.exception.AccountLockedException.class)
    public ResponseEntity<ErrorBody> handleAccountLocked(
            com.srm.creditengine.auth.domain.exception.AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorBody("Account locked until " + ex.getLockedUntil()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorBody> handleMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorBody(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ErrorBody(message));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorBody> handleMissingOrMalformedParam(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorBody(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorBody> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorBody("Resource not found."));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorBody("Access denied."));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorBody> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorBody("Authentication required."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception ex, HttpServletRequest req) {
        String requestId = org.slf4j.MDC.get("requestId");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        log.error("requestId={} unhandled error on {}", requestId, req.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
            .body(new ErrorBody("Unexpected internal error."));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(String message, String resolution) {
        ErrorBody(String message) {
            this(message, null);
        }
    }
}
