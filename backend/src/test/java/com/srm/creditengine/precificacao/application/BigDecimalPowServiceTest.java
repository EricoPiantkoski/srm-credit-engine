package com.srm.creditengine.precificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class BigDecimalPowServiceTest {

    private final BigDecimalPowService service = new BigDecimalPowService();

    @Test
    void integerExponentReturnsExactResult() {
        BigDecimal result = service.pow(new BigDecimal("2"), new BigDecimal("10"));
        assertThat(result).isEqualByComparingTo("1024");
    }

    @Test
    void fractionalExponentComputesCorrectly() {
        BigDecimal result = service.pow(new BigDecimal("4"), new BigDecimal("0.5"));
        assertThat(result).isCloseTo(new BigDecimal("2"), org.assertj.core.data.Offset.offset(new BigDecimal("0.0001")));
    }

    @Test
    void fractionalExponentPrecision() {
        BigDecimal base = new BigDecimal("1.025");
        BigDecimal exponent = new BigDecimal("0.5");
        BigDecimal result = service.pow(base, exponent);
        assertThat(result).isCloseTo(new BigDecimal("1.01242284"), org.assertj.core.data.Offset.offset(new BigDecimal("0.00001")));
    }

    @Test
    void highPrecisionComparisonWithDouble() {
        BigDecimal base = new BigDecimal("1.015");
        BigDecimal exponent = new BigDecimal("1.0");
        BigDecimal exact = service.pow(base, exponent);
        BigDecimal doubleBased = BigDecimal.valueOf(Math.pow(base.doubleValue(), exponent.doubleValue()));
        assertThat(exact.subtract(doubleBased).abs())
            .isLessThan(new BigDecimal("1E-15"));
    }

    @Test
    void complexFractionalExponent() {
        BigDecimal base = BigDecimal.ONE.add(new BigDecimal("0.01")).add(new BigDecimal("0.015"));
        BigDecimal exponent = new BigDecimal("1.0");
        BigDecimal result = service.pow(base, exponent);
        assertThat(result).isEqualByComparingTo("1.025");
    }

    @Test
    void exponentZeroReturnsOne() {
        BigDecimal result = service.pow(new BigDecimal("5"), new BigDecimal("0"));
        assertThat(result).isEqualByComparingTo("1");
    }

    @Test
    void baseOneReturnsOne() {
        BigDecimal result = service.pow(BigDecimal.ONE, new BigDecimal("100"));
        assertThat(result).isEqualByComparingTo("1");
    }

    @Test
    void rejectsNonPositiveBase() {
        assertThatThrownBy(() -> service.pow(BigDecimal.ZERO, new BigDecimal("1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base must be positive");
        assertThatThrownBy(() -> service.pow(new BigDecimal("-1"), new BigDecimal("1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base must be positive");
    }
}