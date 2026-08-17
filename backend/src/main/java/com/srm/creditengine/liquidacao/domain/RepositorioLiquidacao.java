package com.srm.creditengine.liquidacao.domain;

import java.util.Optional;
import java.util.List;

public interface RepositorioLiquidacao {

    boolean existsChaveIdempotencia(String chaveIdempotencia);

    Liquidacao save(Liquidacao liquidacao);

    Optional<Liquidacao> obtainById(Long id);

    PageResult list(int page, int size);

    record PageResult(List<Liquidacao> content, long totalElements, int page, int size, int totalPages) {}

}
