package com.srm.creditengine.precificacao.domain;

import java.util.List;
import java.util.Optional;

public interface RecebivelRepository {

    void save(Recebivel recebivel);

    boolean existsReferenciaExterna(String referenciaExterna);

    Optional<Recebivel> obtainById(Long id);

    List<Recebivel> list(RecebivelQueryCriteria criteria);
}