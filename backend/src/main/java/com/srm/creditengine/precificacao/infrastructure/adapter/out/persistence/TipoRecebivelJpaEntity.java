package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "tipo_recebivel")
public class TipoRecebivelJpaEntity {

    @Id
    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 64)
    private String nome;

    @Column(name = "spread", nullable = false, precision = 9, scale = 6)
    private BigDecimal spread;

    protected TipoRecebivelJpaEntity() {}

    public TipoRecebivelJpaEntity(String codigo, String nome, BigDecimal spread) {
        this.codigo = codigo;
        this.nome = nome;
        this.spread = spread;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getSpread() {
        return spread;
    }
}