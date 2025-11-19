package com.codename1.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class UtilBigNumberTest extends UITestBase {

    @FormTest
    void bigIntegerArithmeticAndBitLogic() {
        BigInteger large = new BigInteger("123456789012345678901234567890");
        BigInteger divisor = new BigInteger("987654321");
        BigInteger sum = large.add(divisor);
        assertEquals(new java.math.BigInteger(large.toString()).add(new java.math.BigInteger(divisor.toString())).toString(), sum.toString());

        BigInteger difference = large.subtract(divisor);
        assertEquals(new java.math.BigInteger(large.toString()).subtract(new java.math.BigInteger(divisor.toString())).toString(), difference.toString());

        BigInteger product = divisor.multiply(BigInteger.valueOf(42));
        assertEquals("41481481282", product.toString());

        BigInteger[] divRem = large.divideAndRemainder(divisor);
        java.math.BigInteger[] expected = new java.math.BigInteger(large.toString()).divideAndRemainder(new java.math.BigInteger(divisor.toString()));
        assertEquals(expected[0].toString(), divRem[0].toString());
        assertEquals(expected[1].toString(), divRem[1].toString());

        BigInteger shifted = divisor.shiftLeft(5).shiftRight(5);
        assertEquals(divisor, shifted);

        BigInteger toggled = divisor.setBit(0);
        assertTrue(toggled.testBit(0));
        assertFalse(toggled.clearBit(0).testBit(0));
        assertNotEquals(divisor, toggled.flipBit(1));

        BigInteger mask = BigInteger.valueOf(0xff);
        BigInteger logical = mask.and(BigInteger.valueOf(0xf0)).or(BigInteger.valueOf(0x0f));
        assertEquals(BigInteger.valueOf(0xff), logical);
        BigInteger xor = mask.xor(BigInteger.valueOf(0x0f));
        assertEquals(BigInteger.valueOf(0xf0), xor);
        assertTrue(mask.not().compareTo(BigInteger.ZERO) < 0);

        BigInteger modular = divisor.mod(BigInteger.valueOf(97));
        assertEquals(divisor.remainder(BigInteger.valueOf(97)), modular);

        byte[] serialized = divisor.toByteArray();
        assertEquals(divisor, new BigInteger(serialized));

        assertEquals(125, new BigInteger("5").pow(3).intValue());
        assertEquals(1, divisor.gcd(BigInteger.valueOf(2)).intValue());
        assertTrue(BigInteger.probablePrime(16, new Random(1)).isProbablePrime(10));
    }

    @FormTest
    void tBigDecimalRespectsMathContext() {
        TMathContext context = new TMathContext(5, TRoundingMode.HALF_UP);
        TBigDecimal value = new TBigDecimal("123.4567", new TMathContext("precision=7 roundingMode=HALF_EVEN"));
        TBigDecimal rounded = value.round(context);
        assertEquals(5, context.getPrecision());
        assertEquals(TRoundingMode.HALF_UP, context.getRoundingMode());
        assertEquals(TRoundingMode.HALF_EVEN, TRoundingMode.valueOf(TBigDecimal.ROUND_HALF_EVEN));
        assertEquals(TRoundingMode.HALF_EVEN, TRoundingMode.valueOf("HALF_EVEN"));

        // Ensure arithmetic methods with MathContext engage rounding helpers
        TBigDecimal augend = new TBigDecimal(new TBigInteger("5"), 1, context);
        TBigDecimal multiplicand = new TBigDecimal("3.33", context);
        assertEquals("8.83", augend.add(multiplicand, context).toString());
        assertEquals("1.67", multiplicand.subtract(augend, context).abs().toString());

        TBigDecimal product = multiplicand.multiply(augend, context);
        assertTrue(product.toString().startsWith("1.6"));

        TBigDecimal quotient = product.divide(augend, context);
        assertFalse(quotient.toString().isEmpty());

        TBigDecimal[] divRem = product.divideAndRemainder(augend, context);
        assertEquals(product.toString(), divRem[0].multiply(augend, context).add(divRem[1], context).round(context).toString());
    }

    @FormTest
    void bigDecimalInteropWithBigIntegerScale() {
        BigDecimal decimal = new BigDecimal(new BigInteger("12345"), 3);
        assertEquals(3, decimal.getScale());
        assertEquals("12.345", decimal.toString());

        BigDecimal adjusted = decimal.adjustScale(2);
        assertEquals("123.45", adjusted.toString());

        BigDecimal negated = decimal.negate();
        assertTrue(negated.compareTo(decimal) < 0);

        BigDecimal sum = decimal.add(new BigDecimal(new BigInteger("55"), 1));
        assertEquals("17.845", sum.toString());

        BigDecimal difference = decimal.subtract(new BigInteger("5"));
        assertEquals("7.345", difference.toString());

        BigDecimal product = decimal.multiply(new BigInteger("10"));
        assertEquals("123.45", product.toString());

        BigDecimal quotient = product.divide(new BigInteger("2"));
        assertEquals("61.725", quotient.toString());

        assertEquals(decimal.floor(), decimal.round());
        assertEquals(decimal.intValue(), decimal.floor().intValue());
        assertEquals(decimal.longValue(), decimal.floor().longValue());
    }
}
