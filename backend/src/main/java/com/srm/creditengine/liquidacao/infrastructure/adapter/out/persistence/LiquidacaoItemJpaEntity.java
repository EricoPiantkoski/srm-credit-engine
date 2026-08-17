package com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "liquidacao_item")
public class LiquidacaoItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liquidacao_id", nullable = false)
    private LiquidacaoJpaEntity liquidacao;

    @Column(name = "recebivel_id", nullable = false)
    private Long recebivelId;

    @Column(name = "valor_presente", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorPresente;

    @Column(name = "spread_aplicado", nullable = false, precision = 9, scale = 6)
    private BigDecimal spreadAplicado;

    @Column(name = "prazo_meses", nullable = false, precision = 9, scale = 6)
    private BigDecimal prazoMeses;

    @Column(name = "valor_pagamento", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorPagamento;

    @Column(name = "codigo_moeda_pagamento", nullable = false, length = 3)
    private String codigoMoedaPagamento;

    @Column(name = "taxa_aplicada", precision = 19, scale = 8)
    private BigDecimal taxaAplicada;

    protected LiquidacaoItemJpaEntity() {}

    public LiquidacaoItemJpaEntity(Long recebivelId, BigDecimal valorPresente, BigDecimal spreadAplicado,
                                   BigDecimal prazoMeses, BigDecimal valorPagamento,
                                   String codigoMoedaPagamento, BigDecimal taxaAplicada) {
        this.recebivelId = recebivelId;
        this.valorPresente = valorPresente;
        this.spreadAplicado = spreadAplicado;
        this.prazoMeses = prazoMeses;
        this.valorPagamento = valorPagamento;
        this.codigoMoedaPagamento = codigoMoedaPagamento;
        this.taxaAplicada = taxaAplicada;
    }

    void setLiquidacao(LiquidacaoJpaEntity liquidacao) {
        this.liquidacao = liquidacao;
    }

    public Long getId() {
        return id;
    }

    public Long getRecebivelId() {
        return recebivelId;
    }

    public BigDecimal getValorPresente() {
        return valorPresente;
    }

    public BigDecimal getSpreadAplicado() {
        return spreadAplicado;
    }

    public BigDecimal getPrazoMeses() {
        return prazoMeses;
    }

    public BigDecimal getValorPagamento() {
        return valorPagamento;
    }

    public String getCodigoMoedaPagamento() {
        return codigoMoedaPagamento;
    }

    public BigDecimal getTaxaAplicada() {
        return taxaAplicada;
    }
}