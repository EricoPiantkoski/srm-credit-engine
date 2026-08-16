package com.srm.creditengine.shared.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record CodigoMoeda(String codigo) {

    private static final Pattern ISO_ALPHA3 = Pattern.compile("[A-Z]{3}");

    public CodigoMoeda {
        validar(codigo);
    }

    private static void validar(String codigo) {
        Objects.requireNonNull(codigo, "codigo must not be null");
        if (!ISO_ALPHA3.matcher(codigo).matches()) {
            throw new IllegalArgumentException(
                "currency code must be 3 uppercase letters, but was: " + codigo);
        }
    }
}