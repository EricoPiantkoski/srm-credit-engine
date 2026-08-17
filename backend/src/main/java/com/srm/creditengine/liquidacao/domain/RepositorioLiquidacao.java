package com.srm.creditengine.liquidacao.domain;

import java.util.Optional;

public interface RepositorioLiquidacao {

    boolean existsChaveIdempotencia(String chaveIdempotencia);

    Liquidacao save(Liquidacao liquidacao);

    Optional<Liquidacao> obtainById(Long id);
}