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
class ChequePreDatadoStrategyTest {

    @Mock
    private TipoRecebivelRepository repository;

    private final Recebivel recebivel = new Recebivel(null, "REF", "CHEQUE_PRE_DATADO",
        new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
        LocalDate.of(2026, 9, 15), "Cedente", 0L);

    @Test
    void resolvesSpreadFromRepository() {
        when(repository.obtainByCodigo("CHEQUE_PRE_DATADO"))
            .thenReturn(Optional.of(new TipoRecebivel("CHEQUE_PRE_DATADO", "Cheque Pré-datado", new BigDecimal("0.025"))));

        Spread spread = new ChequePreDatadoStrategy(repository).spreadFor(recebivel);

        assertThat(spread.valor()).isEqualByComparingTo("0.025000");
    }

    @Test
    void throwsWhenTipoMissing() {
        when(repository.obtainByCodigo("CHEQUE_PRE_DATADO")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ChequePreDatadoStrategy(repository).spreadFor(recebivel))
            .isInstanceOf(UnknownReceivableTypeException.class);
    }
}