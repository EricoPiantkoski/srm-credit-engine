package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "taxa_cambio")
public class TaxaCambioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_base", nullable = false, length = 3)
    private String codigoBase;

    @Column(name = "codigo_cotacao", nullable = false, length = 3)
    private String codigoCotacao;

    @Column(name = "taxa", nullable = false, precision = 19, scale = 8)
    private BigDecimal taxa;

    @Column(name = "vigencia", nullable = false)
    private Instant vigencia;

    protected TaxaCambioJpaEntity() {}

    public TaxaCambioJpaEntity(String codigoBase, String codigoCotacao, BigDecimal taxa, Instant vigencia) {
        this.codigoBase = codigoBase;
        this.codigoCotacao = codigoCotacao;
        this.taxa = taxa;
        this.vigencia = vigencia;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoBase() {
        return codigoBase;
    }

    public String getCodigoCotacao() {
        return codigoCotacao;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public Instant getVigencia() {
        return vigencia;
    }
}