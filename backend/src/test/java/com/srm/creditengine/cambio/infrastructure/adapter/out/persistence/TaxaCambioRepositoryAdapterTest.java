package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class TaxaCambioRepositoryAdapterTest {

    private static final ParMoedas USD_BRL = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TaxaCambioRepositoryAdapter adapter;

    @Autowired
    private TaxaCambioJpaRepository jpaRepository;

    @Autowired
    private MoedaRepositoryAdapter moedaAdapter;

    @Autowired
    private MoedaJpaRepository moedaJpaRepository;

    @BeforeEach
    void clean() {
        jpaRepository.deleteAll();
    }

    @Test
    void savesAndDetectsVigencia() {
        Instant vigencia = Instant.parse("2026-08-14T16:00:00Z");
        adapter.save(new TaxaCambio(USD_BRL, new BigDecimal("5.25"), vigencia));

        assertThat(adapter.existsVigencia(USD_BRL, vigencia)).isTrue();
        assertThat(adapter.existsVigencia(USD_BRL, vigencia.plusSeconds(1))).isFalse();
    }

    @Test
    void obtainsVigenteUpToReference() {
        Instant earlier = Instant.parse("2026-08-01T16:00:00Z");
        Instant later = Instant.parse("2026-08-14T16:00:00Z");
        adapter.save(new TaxaCambio(USD_BRL, new BigDecimal("5.10"), earlier));
        adapter.save(new TaxaCambio(USD_BRL, new BigDecimal("5.25"), later));

        assertThat(adapter.obtainVigente(USD_BRL, later).orElseThrow().taxa()).isEqualByComparingTo("5.25");
        assertThat(adapter.obtainVigente(USD_BRL, later.minusSeconds(1)).orElseThrow().taxa()).isEqualByComparingTo("5.10");
        assertThat(adapter.obtainVigente(USD_BRL, earlier).orElseThrow().taxa()).isEqualByComparingTo("5.10");
        assertThat(adapter.obtainVigente(USD_BRL, earlier.minusSeconds(1))).isEmpty();
    }

    @Test
    void moedaExistsPerSeed() {
        assertThat(moedaAdapter.exists(new CodigoMoeda("BRL"))).isTrue();
        assertThat(moedaAdapter.exists(new CodigoMoeda("EUR"))).isFalse();

        MoedaJpaEntity brl = moedaJpaRepository.findById("BRL").orElseThrow();
        assertThat(brl.getCodigo()).isEqualTo("BRL");
        assertThat(brl.getNome()).isEqualTo("Real Brasileiro");
        assertThat(brl.getEscala()).isEqualTo(2);
    }

    @Test
    void roundsTripDomainTaxa() {
        Instant vigencia = Instant.parse("2026-08-14T16:00:00Z");
        adapter.save(new TaxaCambio(USD_BRL, new BigDecimal("5.250000001"), vigencia));

        Optional<TaxaCambio> loaded = adapter.obtainVigente(USD_BRL, vigencia);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().par()).isEqualTo(USD_BRL);
        assertThat(loaded.get().taxa()).isEqualByComparingTo("5.25000000");
        assertThat(loaded.get().vigencia()).isEqualTo(vigencia);
    }
}