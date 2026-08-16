package com.srm.creditengine.cambio.domain;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;

public interface MoedaRepository {
    boolean exists(CodigoMoeda moeda);
}