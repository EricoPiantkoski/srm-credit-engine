package com.srm.creditengine.precificacao.domain;

public record RecebivelQueryCriteria(String cedente, String codigoMoeda, String codigoTipo, int page, int size) {

    public RecebivelQueryCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative, but was: " + page);
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive, but was: " + size);
        }
    }
}