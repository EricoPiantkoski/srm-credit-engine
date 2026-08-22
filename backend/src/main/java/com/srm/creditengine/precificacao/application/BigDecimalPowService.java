package com.srm.creditengine.precificacao.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class BigDecimalPowService {

    private static final int PRECISION = 30;
    private static final MathContext MATH_CONTEXT = new MathContext(PRECISION, RoundingMode.HALF_EVEN);
    private static final int MAX_ITERATIONS = 100;
    private static final BigDecimal EPSILON = new BigDecimal("1E-25");

    public BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        if (base.signum() <= 0) {
            throw new IllegalArgumentException("base must be positive, but was: " + base);
        }
        if (exponent.stripTrailingZeros().scale() <= 0) {
            return base.pow(exponent.intValueExact(), MATH_CONTEXT);
        }
        BigDecimal lnBase = ln(base);
        BigDecimal product = lnBase.multiply(exponent, MATH_CONTEXT);
        return exp(product);
    }

    private BigDecimal ln(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ln undefined for non-positive value: " + x);
        }
        BigDecimal xMinus1 = x.subtract(BigDecimal.ONE, MATH_CONTEXT);
        BigDecimal xPlus1 = x.add(BigDecimal.ONE, MATH_CONTEXT);
        BigDecimal ratio = xMinus1.divide(xPlus1, MATH_CONTEXT);
        BigDecimal ratioSquared = ratio.multiply(ratio, MATH_CONTEXT);
        BigDecimal term = ratio;
        BigDecimal sum = ratio;
        int n = 1;
        while (n < MAX_ITERATIONS) {
            term = term.multiply(ratioSquared, MATH_CONTEXT);
            BigDecimal nextTerm = term.divide(BigDecimal.valueOf(2 * n + 1), MATH_CONTEXT);
            if (nextTerm.abs().compareTo(EPSILON) < 0) {
                break;
            }
            sum = sum.add(nextTerm, MATH_CONTEXT);
            n++;
        }
        return sum.multiply(BigDecimal.valueOf(2), MATH_CONTEXT);
    }

    private BigDecimal exp(BigDecimal x) {
        if (x.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }
        BigDecimal term = BigDecimal.ONE;
        BigDecimal sum = BigDecimal.ONE;
        int n = 1;
        while (n < MAX_ITERATIONS) {
            term = term.multiply(x, MATH_CONTEXT).divide(BigDecimal.valueOf(n), MATH_CONTEXT);
            if (term.abs().compareTo(EPSILON) < 0) {
                break;
            }
            sum = sum.add(term, MATH_CONTEXT);
            n++;
        }
        return sum;
    }
}