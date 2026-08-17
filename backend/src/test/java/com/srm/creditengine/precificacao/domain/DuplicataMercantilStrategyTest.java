package com.srm.creditengine.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicataMercantilStrategyTest {

    @Mock
    private TipoRecebivelRepository repository;

    private final Recebivel recebivel = new Recebivel(null, "REF", "DUPLICATA_MERCANTIL",
        new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
        LocalDate.of(2026, 9, 15), "Cedente", 0L);

    @Test
    void resolvesSpreadFromRepository() {
        when(repository.obtainByCodigo("DUPLICATA_MERCANTIL"))
            .thenReturn(Optional.of(new TipoRecebivel("DUPLICATA_MERCANTIL", "Duplicata Mercantil", new BigDecimal("0.015"))));

        Spread spread = new DuplicataMercantilStrategy(repository).spreadFor(recebivel);

        assertThat(spread.valor()).isEqualByComparingTo("0.015000");
    }

    @Test
    void throwsWhenTipoMissing() {
        when(repository.obtainByCodigo("DUPLICATA_MERCANTIL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DuplicataMercantilStrategy(repository).spreadFor(recebivel))
            .isInstanceOf(UnknownReceivableTypeException.class);
    }
}