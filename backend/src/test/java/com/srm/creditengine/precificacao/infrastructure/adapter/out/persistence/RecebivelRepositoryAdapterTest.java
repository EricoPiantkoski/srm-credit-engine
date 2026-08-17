package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelQueryCriteria;
import com.srm.creditengine.precificacao.domain.TipoRecebivel;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
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
class RecebivelRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RecebivelRepositoryAdapter adapter;

    @Autowired
    private RecebivelJpaRepository jpaRepository;

    @Autowired
    private TipoRecebivelRepositoryAdapter tipoAdapter;

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    private Recebivel recebivel(String ref, String cedente, String codigoMoeda, String codigoTipo) {
        return new Recebivel(null, ref, codigoTipo,
            new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda(codigoMoeda), 2),
            LocalDate.of(2026, 9, 15), cedente, 0L);
    }

    @Test
    void savesAndLoadsRecebivel() {
        adapter.save(recebivel("REF-001", "Cedente A", "BRL", "DUPLICATA_MERCANTIL"));

        assertThat(adapter.existsReferenciaExterna("REF-001")).isTrue();
        assertThat(adapter.existsReferenciaExterna("REF-002")).isFalse();

        Long savedId = jpaRepository.findAll().get(0).getId();
        Optional<Recebivel> loaded = adapter.obtainById(savedId);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().referenciaExterna()).isEqualTo("REF-001");
        assertThat(loaded.get().codigoTipo()).isEqualTo("DUPLICATA_MERCANTIL");
        assertThat(loaded.get().valorFace().valor()).isEqualByComparingTo("1000.00");
        assertThat(loaded.get().valorFace().moeda().codigo()).isEqualTo("BRL");
        assertThat(loaded.get().dataVencimento()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(loaded.get().cedente()).isEqualTo("Cedente A");
        assertThat(loaded.get().version()).isZero();
    }

    @Test
    void listsWithFiltersAndPagination() {
        adapter.save(recebivel("REF-001", "Cedente A", "BRL", "DUPLICATA_MERCANTIL"));
        adapter.save(recebivel("REF-002", "Cedente A", "USD", "DUPLICATA_MERCANTIL"));
        adapter.save(recebivel("REF-003", "Cedente B", "BRL", "CHEQUE_PRE_DATADO"));

        List<Recebivel> byCedente = adapter.list(new RecebivelQueryCriteria("Cedente A", null, null, 0, 20));
        assertThat(byCedente).hasSize(2);

        List<Recebivel> byMoeda = adapter.list(new RecebivelQueryCriteria(null, "USD", null, 0, 20));
        assertThat(byMoeda).hasSize(1);
        assertThat(byMoeda.get(0).referenciaExterna()).isEqualTo("REF-002");

        List<Recebivel> byTipo = adapter.list(new RecebivelQueryCriteria(null, null, "CHEQUE_PRE_DATADO", 0, 20));
        assertThat(byTipo).hasSize(1);
        assertThat(byTipo.get(0).referenciaExterna()).isEqualTo("REF-003");

        List<Recebivel> byAll = adapter.list(
            new RecebivelQueryCriteria("Cedente A", "BRL", "DUPLICATA_MERCANTIL", 0, 20));
        assertThat(byAll).hasSize(1);

        List<Recebivel> page2 = adapter.list(new RecebivelQueryCriteria(null, null, null, 1, 2));
        assertThat(page2).hasSize(1);
    }

    @Test
    void tipoRecebivelSeededFromV2() {
        Optional<TipoRecebivel> duplicata = tipoAdapter.obtainByCodigo("DUPLICATA_MERCANTIL");
        assertThat(duplicata).isPresent();
        assertThat(duplicata.get().nome()).isEqualTo("Duplicata Mercantil");
        assertThat(duplicata.get().spread()).isEqualByComparingTo("0.015000");

        Optional<TipoRecebivel> cheque = tipoAdapter.obtainByCodigo("CHEQUE_PRE_DATADO");
        assertThat(cheque).isPresent();
        assertThat(cheque.get().spread()).isEqualByComparingTo("0.025000");

        assertThat(tipoAdapter.obtainByCodigo("DESCONHECIDO")).isEmpty();
    }
}