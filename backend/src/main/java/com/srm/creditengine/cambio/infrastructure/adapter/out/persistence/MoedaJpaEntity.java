package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "moeda")
public class MoedaJpaEntity {

    @Id
    @Column(name = "codigo", nullable = false, length = 3)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 64)
    private String nome;

    @Column(name = "escala", nullable = false)
    private int escala;

    protected MoedaJpaEntity() {}

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public int getEscala() {
        return escala;
    }
}