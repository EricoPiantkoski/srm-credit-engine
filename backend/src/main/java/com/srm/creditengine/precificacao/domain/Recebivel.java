package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Recebivel {

    private final Long id;
    private final String referenciaExterna;
    private final String codigoTipo;
    private final Dinheiro valorFace;
    private final LocalDate dataVencimento;
    private final String cedente;
    private final Long version;

    public Recebivel(Long id, String referenciaExterna, String codigoTipo,
                     Dinheiro valorFace, LocalDate dataVencimento, String cedente, Long version) {
        validate(referenciaExterna, codigoTipo, valorFace, dataVencimento, cedente, version);
        this.id = id;
        this.referenciaExterna = referenciaExterna;
        this.codigoTipo = codigoTipo;
        this.valorFace = valorFace;
        this.dataVencimento = dataVencimento;
        this.cedente = cedente;
        this.version = version;
    }

    private void validate(String referenciaExterna, String codigoTipo, Dinheiro valorFace,
                          LocalDate dataVencimento, String cedente, Long version) {
        Objects.requireNonNull(referenciaExterna, "referenciaExterna must not be null");
        if (referenciaExterna.isBlank()) {
            throw new IllegalArgumentException("referenciaExterna must not be blank");
        }
        Objects.requireNonNull(codigoTipo, "codigoTipo must not be null");
        if (codigoTipo.isBlank()) {
            throw new IllegalArgumentException("codigoTipo must not be blank");
        }
        Objects.requireNonNull(valorFace, "valorFace must not be null");
        if (valorFace.valor().signum() <= 0) {
            throw new IllegalArgumentException("valorFace must be positive, but was: " + valorFace.valor());
        }
        Objects.requireNonNull(dataVencimento, "dataVencimento must not be null");
        Objects.requireNonNull(cedente, "cedente must not be null");
        if (cedente.isBlank()) {
            throw new IllegalArgumentException("cedente must not be blank");
        }
        Objects.requireNonNull(version, "version must not be null");
    }

    public long prazoInDays(LocalDate dataPrecificacao) {
        return ChronoUnit.DAYS.between(dataPrecificacao, dataVencimento);
    }

    public Long id() { return id; }
    public String referenciaExterna() { return referenciaExterna; }
    public String codigoTipo() { return codigoTipo; }
    public Dinheiro valorFace() { return valorFace; }
    public LocalDate dataVencimento() { return dataVencimento; }
    public String cedente() { return cedente; }
    public Long version() { return version; }
}