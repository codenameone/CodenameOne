package com.codename1.tools.translator;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CNumberTest {
    @Test void finiteLiteralsRoundTripEveryBit() {
        long[] doubles = {0, Long.MIN_VALUE, 1, 0xfffffffffffffL, 0x10000000000000L,
                0x7fefffffffffffffL, Double.doubleToLongBits(1.99584030953472E292)};
        for (long bits : doubles) checkDouble(bits);
        int[] floats = {0, Integer.MIN_VALUE, 1, 0x7fffff, 0x800000, 0x7f7fffff};
        for (int bits : floats) checkFloat(bits);
        Random random = new Random(42);
        for (int i = 0; i < 10000; i++) { checkDouble(random.nextLong()); checkFloat(random.nextInt()); }
    }
    private void checkDouble(long bits) {
        double value = Double.longBitsToDouble(bits);
        if (Double.isNaN(value) || Double.isInfinite(value)) return;
        assertEquals(bits, Double.doubleToRawLongBits(Double.parseDouble(CNumber.literal(value))));
    }
    private void checkFloat(int bits) {
        float value = Float.intBitsToFloat(bits);
        if (Float.isNaN(value) || Float.isInfinite(value)) return;
        assertEquals(bits, Float.floatToRawIntBits(Float.parseFloat(CNumber.literal(value))));
    }
}
