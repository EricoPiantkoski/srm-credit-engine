package com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "liquidacao")
public class LiquidacaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 64)
    private String chaveIdempotencia;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "liquidacao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LiquidacaoItemJpaEntity> itens = new ArrayList<>();

    protected LiquidacaoJpaEntity() {}

    public LiquidacaoJpaEntity(String chaveIdempotencia, String status, Instant createdAt) {
        this.chaveIdempotencia = chaveIdempotencia;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addItem(LiquidacaoItemJpaEntity item) {
        item.setLiquidacao(this);
        itens.add(item);
    }

    public Long getId() { return id; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<LiquidacaoItemJpaEntity> getItens() { return itens; }
}
