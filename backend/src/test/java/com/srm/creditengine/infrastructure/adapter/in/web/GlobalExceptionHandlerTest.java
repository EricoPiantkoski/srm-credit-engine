package com.srm.creditengine.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
    void validationExceptionMapsToBadRequest() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new CodigoDto("brl"), "target");
        binding.rejectValue("codigo", "pattern", "must match [A-Z]{3}");
        var ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("codigo");
    }

    record CodigoDto(String codigo) {
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