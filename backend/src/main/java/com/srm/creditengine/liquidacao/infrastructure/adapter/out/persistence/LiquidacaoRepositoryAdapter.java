package com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence;

import com.srm.creditengine.liquidacao.domain.ItemLiquidacao;
import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.liquidacao.domain.StatusLiquidacao;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LiquidacaoRepositoryAdapter implements RepositorioLiquidacao {

    private final LiquidacaoJpaRepository jpaRepository;

    public LiquidacaoRepositoryAdapter(LiquidacaoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsChaveIdempotencia(String chaveIdempotencia) {
        return jpaRepository.existsByChaveIdempotencia(chaveIdempotencia);
    }

    @Override
    @Transactional
    public Liquidacao save(Liquidacao liquidacao) {
        LiquidacaoJpaEntity entity = new LiquidacaoJpaEntity(
            liquidacao.chaveIdempotencia(), liquidacao.status().name(), liquidacao.createdAt());
        for (ItemLiquidacao item : liquidacao.itens()) {
            entity.addItem(toJpa(item));
        }
        return toDomain(jpaRepository.save(entity));
    }

    private LiquidacaoItemJpaEntity toJpa(ItemLiquidacao item) {
        return new LiquidacaoItemJpaEntity(
            item.recebivelId(), item.valorPresente(), item.spreadAplicado(),
            item.prazoMeses(), item.valorPagamento(), item.codigoMoedaPagamento(), item.taxaAplicada());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Liquidacao> obtainById(Long id) {
        return jpaRepository.findWithItensById(id).map(this::toDomain);
    }

    private Liquidacao toDomain(LiquidacaoJpaEntity entity) {
        List<ItemLiquidacao> itens = entity.getItens().stream().map(this::toDomainItem).toList();
        return new Liquidacao(entity.getId(), entity.getChaveIdempotencia(),
            StatusLiquidacao.valueOf(entity.getStatus()), itens, entity.getCreatedAt());
    }

    private ItemLiquidacao toDomainItem(LiquidacaoItemJpaEntity entity) {
        return new ItemLiquidacao(
            entity.getRecebivelId(), entity.getValorPresente(), entity.getSpreadAplicado(),
            entity.getPrazoMeses(), entity.getValorPagamento(), entity.getCodigoMoedaPagamento(),
            entity.getTaxaAplicada());
    }
}