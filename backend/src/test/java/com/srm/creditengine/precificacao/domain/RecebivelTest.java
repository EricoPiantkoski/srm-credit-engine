package com.srm.creditengine.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecebivelTest {

    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 9, 15);
    private static final Dinheiro VALOR = new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2);

    private Recebivel build() {
        return new Recebivel(1L, "REF-001", "DUPLICATA_MERCANTIL", VALOR, VENCIMENTO, "Empresa X", 0L);
    }

    @Test
    void exposesFields() {
        Recebivel recebivel = build();
        assertThat(recebivel.id()).isEqualTo(1L);
        assertThat(recebivel.referenciaExterna()).isEqualTo("REF-001");
        assertThat(recebivel.codigoTipo()).isEqualTo("DUPLICATA_MERCANTIL");
        assertThat(recebivel.valorFace()).isEqualTo(VALOR);
        assertThat(recebivel.dataVencimento()).isEqualTo(VENCIMENTO);
        assertThat(recebivel.cedente()).isEqualTo("Empresa X");
        assertThat(recebivel.version()).isEqualTo(0L);
    }

    @Test
    void computesPrazoInDays() {
        Recebivel recebivel = build();
        assertThat(recebivel.prazoInDays(LocalDate.of(2026, 8, 16))).isEqualTo(30L);
        assertThat(recebivel.prazoInDays(VENCIMENTO)).isEqualTo(0L);
    }

    @Test
    void rejectsBlankReferenciaExterna() {
        assertThatThrownBy(() -> new Recebivel(1L, "  ", "DUPLICATA_MERCANTIL", VALOR, VENCIMENTO, "X", 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("referenciaExterna");
    }

    @Test
    void rejectsBlankCodigoTipo() {
        assertThatThrownBy(() -> new Recebivel(1L, "REF-001", " ", VALOR, VENCIMENTO, "X", 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("codigoTipo");
    }

    @Test
    void rejectsNonPositiveValorFace() {
        Dinheiro zero = new Dinheiro(BigDecimal.ZERO, new CodigoMoeda("BRL"), 2);
        assertThatThrownBy(() -> new Recebivel(1L, "REF-001", "DUPLICATA_MERCANTIL", zero, VENCIMENTO, "X", 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valorFace must be positive");
    }

    @Test
    void rejectsBlankCedente() {
        assertThatThrownBy(() -> new Recebivel(1L, "REF-001", "DUPLICATA_MERCANTIL", VALOR, VENCIMENTO, " ", 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cedente");
    }

    @Test
    void rejectsNullVersion() {
        assertThatThrownBy(() -> new Recebivel(1L, "REF-001", "DUPLICATA_MERCANTIL", VALOR, VENCIMENTO, "X", null))
            .isInstanceOf(NullPointerException.class);
    }
}