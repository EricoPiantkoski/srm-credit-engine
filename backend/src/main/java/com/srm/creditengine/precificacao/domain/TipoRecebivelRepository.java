package com.srm.creditengine.precificacao.domain;

import java.util.Optional;

public interface TipoRecebivelRepository {
    Optional<TipoRecebivel> obtainByCodigo(String codigo);
}