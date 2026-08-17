package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recebivel")
public class RecebivelJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referencia_externa", nullable = false, unique = true, length = 64)
    private String referenciaExterna;

    @Column(name = "codigo_tipo", nullable = false, length = 32)
    private String codigoTipo;

    @Column(name = "valor_face", nullable = false, precision = 19, scale = 4)
    private BigDecimal valorFace;

    @Column(name = "codigo_moeda", nullable = false, length = 3)
    private String codigoMoeda;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "cedente", nullable = false, length = 128)
    private String cedente;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected RecebivelJpaEntity() {}

    public RecebivelJpaEntity(String referenciaExterna, String codigoTipo, BigDecimal valorFace,
                              String codigoMoeda, LocalDate dataVencimento, String cedente) {
        this.referenciaExterna = referenciaExterna;
        this.codigoTipo = codigoTipo;
        this.valorFace = valorFace;
        this.codigoMoeda = codigoMoeda;
        this.dataVencimento = dataVencimento;
        this.cedente = cedente;
    }

    public Long getId() {
        return id;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public String getCodigoTipo() {
        return codigoTipo;
    }

    public BigDecimal getValorFace() {
        return valorFace;
    }

    public String getCodigoMoeda() {
        return codigoMoeda;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public String getCedente() {
        return cedente;
    }

    public Long getVersion() {
        return version;
    }
}