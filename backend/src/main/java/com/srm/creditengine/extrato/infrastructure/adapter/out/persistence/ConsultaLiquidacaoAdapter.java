package com.srm.creditengine.extrato.infrastructure.adapter.out.persistence;

import com.srm.creditengine.extrato.domain.ConsultaLiquidacao;
import com.srm.creditengine.extrato.domain.ExtratoFiltros;
import com.srm.creditengine.extrato.domain.ExtratoLiquidacao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class ConsultaLiquidacaoAdapter implements ConsultaLiquidacao {

    private static final String SELECT =
        "SELECT li.id AS item_id, l.id AS liquidacao_id, l.chave_idempotencia, l.status, l.created_at, "
            + "li.recebivel_id, r.cedente, "
            + "li.valor_presente, li.spread_aplicado, li.prazo_meses, "
            + "li.valor_pagamento, li.codigo_moeda_pagamento, li.taxa_aplicada "
            + "FROM liquidacao_item li "
            + "JOIN liquidacao l ON l.id = li.liquidacao_id "
            + "JOIN recebivel r ON r.id = li.recebivel_id ";

    private final JdbcTemplate jdbcTemplate;

    public ConsultaLiquidacaoAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExtratoLiquidacao> consultar(ExtratoFiltros filtros) {
        StringBuilder sql = new StringBuilder(SELECT);
        appendFilters(sql, filtros, true);
        sql.append(" ORDER BY li.id LIMIT ?");
        return jdbcTemplate.query(sql.toString(),
            ps -> bind(ps, filtros, true), ROW_MAPPER);
    }

    private void appendFilters(StringBuilder sql, ExtratoFiltros filtros, boolean includeCursor) {
        sql.append(" WHERE 1 = 1");
        sql.append(" AND l.created_at >= ?");
        sql.append(" AND l.created_at <= ?");
        if (filtros.status() != null) {
            sql.append(" AND l.status = ?");
        }
        if (filtros.cedente() != null) {
            sql.append(" AND r.cedente = ?");
        }
        if (filtros.codigoMoedaPagamento() != null) {
            sql.append(" AND li.codigo_moeda_pagamento = ?");
        }
        if (includeCursor && filtros.lastId() != null) {
            sql.append(" AND li.id > ?");
        }
    }

    private void bind(java.sql.PreparedStatement ps, ExtratoFiltros filtros, boolean includeCursor)
            throws SQLException {
        int index = 1;
        ps.setObject(index++, filtros.dataInicial().atStartOfDay());
        ps.setObject(index++, filtros.dataFinal().atTime(23, 59, 59));
        if (filtros.status() != null) {
            ps.setString(index++, filtros.status());
        }
        if (filtros.cedente() != null) {
            ps.setString(index++, filtros.cedente());
        }
        if (filtros.codigoMoedaPagamento() != null) {
            ps.setString(index++, filtros.codigoMoedaPagamento());
        }
        if (includeCursor && filtros.lastId() != null) {
            ps.setLong(index++, filtros.lastId());
        }
        ps.setInt(index, filtros.limit());
    }

    private static final RowMapper<ExtratoLiquidacao> ROW_MAPPER = (rs, rowNum) -> map(rs);

    private static ExtratoLiquidacao map(ResultSet rs) throws SQLException {
        return new ExtratoLiquidacao(
            rs.getLong("item_id"),
            rs.getLong("liquidacao_id"),
            rs.getString("chave_idempotencia"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getLong("recebivel_id"),
            rs.getString("cedente"),
            rs.getBigDecimal("valor_presente"),
            rs.getBigDecimal("spread_aplicado"),
            rs.getBigDecimal("prazo_meses"),
            rs.getBigDecimal("valor_pagamento"),
            rs.getString("codigo_moeda_pagamento"),
            rs.getBigDecimal("taxa_aplicada"));
    }
}