package com.srm.creditengine.precificacao.application;

import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelQueryCriteria;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import java.util.List;

public class RecebivelQuery {

    private final RecebivelRepository repository;

    public RecebivelQuery(RecebivelRepository repository) {
        this.repository = repository;
    }

    public List<Recebivel> list(RecebivelQueryCriteria criteria) {
        return repository.list(criteria);
    }
}