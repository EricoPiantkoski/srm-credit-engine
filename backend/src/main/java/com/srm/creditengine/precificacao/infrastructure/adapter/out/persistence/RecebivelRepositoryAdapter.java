package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import com.srm.creditengine.cambio.infrastructure.adapter.out.persistence.MoedaJpaRepository;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelQueryCriteria;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.domain.StatusRecebivel;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RecebivelRepositoryAdapter implements RecebivelRepository {

    private final RecebivelJpaRepository jpaRepository;
    private final MoedaJpaRepository moedaJpaRepository;

    public RecebivelRepositoryAdapter(RecebivelJpaRepository jpaRepository,
                                      MoedaJpaRepository moedaJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.moedaJpaRepository = moedaJpaRepository;
    }

    @Override
    public Recebivel save(Recebivel recebivel) {
        RecebivelJpaEntity entity = new RecebivelJpaEntity(
            recebivel.referenciaExterna(), recebivel.codigoTipo(), recebivel.valorFace().valor(),
            recebivel.valorFace().moeda().codigo(), recebivel.dataVencimento(), recebivel.cedente(),
            recebivel.status().name());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public boolean existsReferenciaExterna(String referenciaExterna) {
        return jpaRepository.existsByReferenciaExterna(referenciaExterna);
    }

    @Override
    public Optional<Recebivel> obtainById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Recebivel> list(RecebivelQueryCriteria criteria) {
        PageRequest pageable = PageRequest.of(criteria.page(), criteria.size());
        Page<RecebivelJpaEntity> page = find(criteria, pageable);
        return page.getContent().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean marcarLiquidado(Long id, Long expectedVersion) {
        return jpaRepository.marcarLiquidado(id, expectedVersion) > 0;
    }

    private Page<RecebivelJpaEntity> find(RecebivelQueryCriteria criteria, PageRequest pageable) {
        String cedente = criteria.cedente();
        String codigoMoeda = criteria.codigoMoeda();
        String codigoTipo = criteria.codigoTipo();
        boolean hasCedente = cedente != null;
        boolean hasMoeda = codigoMoeda != null;
        boolean hasTipo = codigoTipo != null;
        if (hasCedente && hasMoeda && hasTipo) {
            return jpaRepository.findByCedenteAndCodigoMoedaAndCodigoTipo(cedente, codigoMoeda, codigoTipo, pageable);
        }
        if (hasCedente && hasMoeda) {
            return jpaRepository.findByCedenteAndCodigoMoeda(cedente, codigoMoeda, pageable);
        }
        if (hasCedente && hasTipo) {
            return jpaRepository.findByCedenteAndCodigoTipo(cedente, codigoTipo, pageable);
        }
        if (hasMoeda && hasTipo) {
            return jpaRepository.findByCodigoMoedaAndCodigoTipo(codigoMoeda, codigoTipo, pageable);
        }
        if (hasCedente) {
            return jpaRepository.findByCedente(cedente, pageable);
        }
        if (hasMoeda) {
            return jpaRepository.findByCodigoMoeda(codigoMoeda, pageable);
        }
        if (hasTipo) {
            return jpaRepository.findByCodigoTipo(codigoTipo, pageable);
        }
        return jpaRepository.findAll(pageable);
    }

    private Recebivel toDomain(RecebivelJpaEntity entity) {
        int escala = moedaJpaRepository.findById(entity.getCodigoMoeda())
            .orElseThrow()
            .getEscala();
        return new Recebivel(
            entity.getId(), entity.getReferenciaExterna(), entity.getCodigoTipo(),
            new Dinheiro(entity.getValorFace(), new CodigoMoeda(entity.getCodigoMoeda()), escala),
            entity.getDataVencimento(), entity.getCedente(), entity.getVersion(),
            StatusRecebivel.valueOf(entity.getStatus()));
    }
}