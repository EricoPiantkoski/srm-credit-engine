package com.srm.creditengine.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrecificacaoStrategyResolverTest {

    private final PrecificacaoStrategy duplicata = new DuplicataMercantilStrategy(null);
    private final PrecificacaoStrategyResolver resolver =
        new PrecificacaoStrategyResolver(Map.of("DUPLICATA_MERCANTIL", duplicata));

    @Test
    void resolvesByCodigo() {
        assertThat(resolver.resolveFor("DUPLICATA_MERCANTIL")).isSameAs(duplicata);
    }

    @Test
    void throwsForUnknownCodigo() {
        assertThatThrownBy(() -> resolver.resolveFor("DESCONHECIDO"))
            .isInstanceOf(UnknownReceivableTypeException.class);
    }
}