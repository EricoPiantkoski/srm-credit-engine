package com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.liquidacao.application.LiquidarLote;
import com.srm.creditengine.liquidacao.domain.ItemLiquidacao;
import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.StatusLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoVersionConflictException;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence.RecebivelJpaRepository;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class LiquidacaoRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private LiquidacaoRepositoryAdapter adapter;

    @Autowired
    private LiquidacaoJpaRepository liquidacaoJpaRepository;

    @Autowired
    private RecebivelRepository recebivelRepository;

    @Autowired
    private LiquidarLote liquidarLote;

    @Autowired
    private RecebivelJpaRepository recebivelJpaRepository;

    @BeforeEach
    void clean() {
        liquidacaoJpaRepository.deleteAll();
        recebivelJpaRepository.deleteAll();
    }

    private Long criarRecebivel() {
        Recebivel recebivel = new Recebivel(null, "REF-100", "DUPLICATA_MERCANTIL",
            new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
            LocalDate.of(2026, 9, 15), "Cedente A", 0L);
        recebivelRepository.save(recebivel);
        return recebivelJpaRepository.findAll().get(0).getId();
    }

    private Liquidacao liquidacao(String chave, Long recebivelId) {
        return new Liquidacao(null, chave, StatusLiquidacao.LIQUIDADA,
            List.of(new ItemLiquidacao(recebivelId, new BigDecimal("985.2200"),
                new BigDecimal("0.015000"), new BigDecimal("1.000000"),
                new BigDecimal("985.22"), "BRL", null)),
            Instant.parse("2026-08-16T12:00:00Z"));
    }

    @Test
    void savesAndLoadsLiquidacaoWithItens() {
        Long recebivelId = criarRecebivel();

        Liquidacao saved = adapter.save(liquidacao("CHAVE-001", recebivelId));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.chaveIdempotencia()).isEqualTo("CHAVE-001");
        assertThat(adapter.existsChaveIdempotencia("CHAVE-001")).isTrue();
        assertThat(adapter.existsChaveIdempotencia("CHAVE-002")).isFalse();

        Optional<Liquidacao> loaded = adapter.obtainById(saved.id());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().chaveIdempotencia()).isEqualTo("CHAVE-001");
        assertThat(loaded.get().status()).isEqualTo(StatusLiquidacao.LIQUIDADA);
        assertThat(loaded.get().itens()).hasSize(1);
        assertThat(loaded.get().itens().get(0).recebivelId()).isEqualTo(recebivelId);
        assertThat(loaded.get().itens().get(0).valorPresente()).isEqualByComparingTo("985.2200");
        assertThat(loaded.get().itens().get(0).valorPagamento()).isEqualByComparingTo("985.22");
    }

    @Test
    void marcarLiquidadoLiquidaSomenteUmaVez() {
        Long recebivelId = criarRecebivel();

        assertThat(recebivelRepository.marcarLiquidado(recebivelId, 0L)).isTrue();
        assertThat(recebivelRepository.marcarLiquidado(recebivelId, 0L)).isFalse();
        assertThat(recebivelRepository.marcarLiquidado(recebivelId, 1L)).isFalse();
    }

    @Test
    void duplicateChaveIdempotenciaIsRejectedByUniqueConstraint() {
        Long recebivelId = criarRecebivel();
        adapter.save(liquidacao("CHAVE-001", recebivelId));

        assertThatThrownBy(() -> adapter.save(liquidacao("CHAVE-001", recebivelId)))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void liquidacaoSequencialDoMesmoRecebivelEhRejeitada() {
        Long recebivelId = criarRecebivel();

        liquidarLote.liquidar(new LiquidarLote.LiquidarLoteInput(
            "9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", "BRL", List.of(recebivelId)));

        assertThatThrownBy(() -> liquidarLote.liquidar(new LiquidarLote.LiquidarLoteInput(
            "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee", "BRL", List.of(recebivelId))))
            .isInstanceOf(LiquidacaoVersionConflictException.class);
        assertThat(adapter.obtainById(liquidacaoJpaRepository.findAll().get(0).getId())).isPresent();
        assertThat(liquidacaoJpaRepository.findAll()).hasSize(1);
    }

}
