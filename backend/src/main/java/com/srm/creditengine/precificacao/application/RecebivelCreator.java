package com.srm.creditengine.precificacao.application;

import com.srm.creditengine.precificacao.domain.MoedaCatalog;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.domain.TipoRecebivelRepository;
import com.srm.creditengine.precificacao.domain.exception.ReceivableConflictException;
import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RecebivelCreator {

    private final RecebivelRepository repository;
    private final TipoRecebivelRepository tipoRepository;
    private final MoedaCatalog moedaCatalog;

    public RecebivelCreator(RecebivelRepository repository,
                            TipoRecebivelRepository tipoRepository, MoedaCatalog moedaCatalog) {
        this.repository = repository;
        this.tipoRepository = tipoRepository;
        this.moedaCatalog = moedaCatalog;
    }

    public Recebivel create(CreateRecebivelInput input) {
        validateTipo(input.codigoTipo());
        int escala = moedaCatalog.scaleOf(new CodigoMoeda(input.codigoMoeda()));
        validateReferenciaAvailable(input.referenciaExterna());
        Recebivel recebivel = new Recebivel(null, input.referenciaExterna(), input.codigoTipo(),
            new Dinheiro(input.valorFace(), new CodigoMoeda(input.codigoMoeda()), escala),
            input.dataVencimento(), input.cedente(), 0L);
        repository.save(recebivel);
        return recebivel;
    }

    private void validateTipo(String codigoTipo) {
        if (tipoRepository.obtainByCodigo(codigoTipo).isEmpty()) {
            throw new UnknownReceivableTypeException(codigoTipo);
        }
    }

    private void validateReferenciaAvailable(String referenciaExterna) {
        if (repository.existsReferenciaExterna(referenciaExterna)) {
            throw new ReceivableConflictException(referenciaExterna);
        }
    }

    public record CreateRecebivelInput(
        String referenciaExterna, String codigoTipo, BigDecimal valorFace,
        String codigoMoeda, LocalDate dataVencimento, String cedente) {}
}