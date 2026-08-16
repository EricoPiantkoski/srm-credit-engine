package com.srm.creditengine.cambio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public class TaxaCambio {

    private final ParMoedas par;
    private final BigDecimal taxa;
    private final Instant vigencia;

    public TaxaCambio(ParMoedas par, BigDecimal taxa, Instant vigencia) {
        validate(par, taxa, vigencia);
        this.par = par;
        this.taxa = taxa.setScale(8, RoundingMode.HALF_EVEN);
        this.vigencia = vigencia;
    }

    private void validate(ParMoedas par, BigDecimal taxa, Instant vigencia) {
        Objects.requireNonNull(par, "par must not be null");
        Objects.requireNonNull(taxa, "taxa must not be null");
        Objects.requireNonNull(vigencia, "vigencia must not be null");
        if (taxa.signum() <= 0) {
            throw new IllegalArgumentException("taxa must be positive, but was: " + taxa);
        }
    }

    public ParMoedas par() {
        return par;
    }

    public BigDecimal taxa() {
        return taxa;
    }

    public Instant vigencia() {
        return vigencia;
    }
}