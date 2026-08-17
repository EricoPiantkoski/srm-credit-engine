package com.srm.creditengine.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateConflictException;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateNotFoundException;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoConflictException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoNotFoundException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoVersionConflictException;
import com.srm.creditengine.precificacao.domain.exception.ExchangeRateUnavailableException;
import com.srm.creditengine.precificacao.domain.exception.ReceivableConflictException;
import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static final ParMoedas PAR =
        new ParMoedas(new com.srm.creditengine.shared.domain.model.CodigoMoeda("USD"),
            new com.srm.creditengine.shared.domain.model.CodigoMoeda("BRL"));

    @Test
    void domainExceptionMapsToUnprocessableEntity() {
        var ex = new IncompatibleCurrenciesException(
            new com.srm.creditengine.shared.domain.model.CodigoMoeda("BRL"),
            new com.srm.creditengine.shared.domain.model.CodigoMoeda("USD"));

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).contains("Cannot operate on amounts of different currencies");
    }

    @Test
    void exchangeRateConflictMapsToConflict() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleExchangeRateConflict(new ExchangeRateConflictException(PAR, Instant.parse("2026-08-14T16:00:00Z")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("already exists");
    }

    @Test
    void receivableConflictMapsToConflict() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleReceivableConflict(new ReceivableConflictException("REF-001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("REF-001");
    }

    @Test
    void liquidacaoConflictMapsToConflict() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleLiquidacaoConflict(new LiquidacaoConflictException("CHAVE-001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("CHAVE-001");
    }

    @Test
    void liquidacaoVersionConflictMapsToConflict() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleLiquidacaoVersionConflict(new LiquidacaoVersionConflictException(10L, 3L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("Reprocess");
    }

    @Test
    void liquidacaoNotFoundMapsToNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleLiquidacaoNotFound(new LiquidacaoNotFoundException(99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("99");
    }

    @Test
    void exchangeRateNotFoundMapsToNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleExchangeRateNotFound(new ExchangeRateNotFoundException(PAR));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("no exchange rate found");
    }

    @Test
    void exchangeRateProviderUnavailableMapsToServiceUnavailable() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleExchangeRateProviderUnavailable(new ExchangeRateProviderUnavailableException(PAR));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).contains("provider unavailable");
        assertThat(response.getBody().resolution()).contains("PUT /api/taxas-cambio");
    }

    @Test
    void exchangeRateUnavailableMapsToUnprocessableEntityWithResolution() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleExchangeRateUnavailable(new ExchangeRateUnavailableException(
                new com.srm.creditengine.shared.domain.model.CodigoMoeda("BRL"),
                new com.srm.creditengine.shared.domain.model.CodigoMoeda("USD")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).contains("no exchange rate available");
        assertThat(response.getBody().resolution()).contains("PUT /api/taxas-cambio");
    }

    @Test
    void genericDomainExceptionHasNullResolution() {
        var ex = new IncompatibleCurrenciesException(
            new com.srm.creditengine.shared.domain.model.CodigoMoeda("BRL"),
            new com.srm.creditengine.shared.domain.model.CodigoMoeda("USD"));

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleDomainException(ex);

        assertThat(response.getBody().resolution()).isNull();
    }

    @Test
    void constraintViolationMapsToBadRequest() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<CodigoDto>> violations = validator.validate(new CodigoDto(null));

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleConstraintViolation(new ConstraintViolationException(violations));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("codigo");
    }

    record CodigoDto(@NotNull String codigo) {
    }

    @Test
    void validationExceptionMapsToBadRequest() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new CodigoDto("brl"), "target");
        binding.rejectValue("codigo", "pattern", "must match [A-Z]{3}");
        var ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("codigo");
    }

    @Test
    void missingRequiredParameterMapsToBadRequest() {
        var ex = new MissingServletRequestParameterException("codigoCotacao", "String");

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleMissingOrMalformedParam(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("codigoCotacao");
    }

    @Test
    void noResourceFoundMapsToNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
            handler.handleNoResource(new NoResourceFoundException(HttpMethod.POST, "api/taxas-cambio/unknown"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("api/taxas-cambio/unknown");
    }

    @Test
    void unexpectedExceptionMapsToInternalServerError() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            ResponseEntity<GlobalExceptionHandler.ErrorBody> response =
                handler.handleUnexpected(new IllegalStateException("boom"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().message()).isEqualTo("Unexpected internal error.");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}