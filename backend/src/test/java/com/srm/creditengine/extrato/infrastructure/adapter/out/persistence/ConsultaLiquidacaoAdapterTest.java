package com.srm.creditengine.extrato.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.extrato.domain.ExtratoFiltros;
import com.srm.creditengine.extrato.domain.ExtratoLiquidacao;
import com.srm.creditengine.liquidacao.domain.ItemLiquidacao;
import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.StatusLiquidacao;
import com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence.LiquidacaoRepositoryAdapter;
import com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence.LiquidacaoJpaRepository;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence.RecebivelJpaRepository;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class ConsultaLiquidacaoAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ConsultaLiquidacaoAdapter adapter;

    @Autowired
    private LiquidacaoRepositoryAdapter liquidacaoAdapter;

    @Autowired
    private LiquidacaoJpaRepository liquidacaoJpaRepository;

    @Autowired
    private RecebivelRepository recebivelRepository;

    @Autowired
    private RecebivelJpaRepository recebivelJpaRepository;

    @BeforeEach
    void clean() {
        liquidacaoJpaRepository.deleteAll();
        recebivelJpaRepository.deleteAll();
    }

    private Long criarRecebivel(String ref, String cedente) {
        Recebivel recebivel = new Recebivel(null, ref, "DUPLICATA_MERCANTIL",
            new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
            LocalDate.of(2026, 9, 15), cedente, 0L);
        recebivelRepository.save(recebivel);
        return recebivelJpaRepository.findAll().stream()
            .filter(e -> e.getReferenciaExterna().equals(ref))
            .findFirst().orElseThrow().getId();
    }

    private void liquidar(String chave, Long recebivelId, Instant createdAt, String status) {
        liquidacaoAdapter.save(new Liquidacao(null, chave, StatusLiquidacao.valueOf(status),
            List.of(new ItemLiquidacao(recebivelId, new BigDecimal("985.2200"),
                new BigDecimal("0.015000"), new BigDecimal("1.000000"),
                new BigDecimal("985.22"), "BRL", null)), createdAt));
    }

    @Test
    void consultsByPeriodAndFilters() {
        Long recebivelA = criarRecebivel("REF-A", "Cedente A");
        Long recebivelB = criarRecebivel("REF-B", "Cedente B");
        liquidar("CHAVE-A", recebivelA, Instant.parse("2026-08-10T10:00:00Z"), "LIQUIDADA");
        liquidar("CHAVE-B", recebivelB, Instant.parse("2026-08-20T10:00:00Z"), "FALHOU");

        List<ExtratoLiquidacao> all = adapter.consultar(
            new ExtratoFiltros(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null, null, null, 50));
        assertThat(all).hasSize(2);

        List<ExtratoLiquidacao> byPeriod = adapter.consultar(
            new ExtratoFiltros(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 31), null, null, null, null, 50));
        assertThat(byPeriod).hasSize(1);
        assertThat(byPeriod.get(0).chaveIdempotencia()).isEqualTo("CHAVE-B");

        List<ExtratoLiquidacao> byStatus = adapter.consultar(
            new ExtratoFiltros(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "LIQUIDADA", null, null, null, 50));
        assertThat(byStatus).hasSize(1);
        assertThat(byStatus.get(0).chaveIdempotencia()).isEqualTo("CHAVE-A");

        List<ExtratoLiquidacao> byCedente = adapter.consultar(
            new ExtratoFiltros(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, "Cedente B", null, null, 50));
        assertThat(byCedente).hasSize(1);
        assertThat(byCedente.get(0).recebivelId()).isEqualTo(recebivelB);
    }

    @Test
    void paginatesByCursor() {
        Long recebivelA = criarRecebivel("REF-A", "Cedente A");
        Long recebivelB = criarRecebivel("REF-B", "Cedente A");
        Long recebivelC = criarRecebivel("REF-C", "Cedente A");
        liquidar("CHAVE-A", recebivelA, Instant.parse("2026-08-10T10:00:00Z"), "LIQUIDADA");
        liquidar("CHAVE-B", recebivelB, Instant.parse("2026-08-11T10:00:00Z"), "LIQUIDADA");
        liquidar("CHAVE-C", recebivelC, Instant.parse("2026-08-12T10:00:00Z"), "LIQUIDADA");

        List<ExtratoLiquidacao> page1 = adapter.consultar(
            new ExtratoFiltros(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null, null, null, 2));
        assertThat(page1).hasSize(2);

        Long lastId = page1.get(page1.size() - 1).itemId();
        List<ExtratoLiquidacao> page2 = adapter.consultar(
            new ExtratoFiltros(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null, null, lastId, 2));
        assertThat(page2).hasSize(1);
    }

    @Test
    void validatesFiltros() {
        assertThatThrownBy(() -> new ExtratoFiltros(
            LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1), null, null, null, null, 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dataFinal");
        assertThatThrownBy(() -> new ExtratoFiltros(
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null, null, null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("limit");
        assertThatThrownBy(() -> new ExtratoFiltros(
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null, null, null, null, ExtratoFiltros.MAX_LIMIT + 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not exceed");
    }
}