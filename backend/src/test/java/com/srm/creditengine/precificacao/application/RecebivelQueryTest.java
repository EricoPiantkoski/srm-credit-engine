package com.srm.creditengine.precificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelQueryCriteria;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecebivelQueryTest {

    @Mock
    private RecebivelRepository repository;

    @Test
    void delegatesToRepository() {
        RecebivelQueryCriteria criteria = new RecebivelQueryCriteria("Cedente", "BRL", "DUPLICATA_MERCANTIL", 0, 20);
        Recebivel recebivel = new Recebivel(1L, "REF", "DUPLICATA_MERCANTIL",
            new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
            LocalDate.of(2026, 9, 15), "Cedente", 0L);
        when(repository.list(criteria)).thenReturn(List.of(recebivel));

        List<Recebivel> result = new RecebivelQuery(repository).list(criteria);

        assertThat(result).containsExactly(recebivel);
        verify(repository).list(criteria);
    }
}