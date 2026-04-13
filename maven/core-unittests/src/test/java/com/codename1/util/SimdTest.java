package com.codename1.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimdTest extends UITestBase {

    @FormTest
    void baseFallbackOpsWork() {
        Simd simd = new Simd();

        int[] a = new int[]{1, 2, 3, 4};
        int[] b = new int[]{4, 3, 2, 1};
        int[] out = new int[4];
        simd.add(a, b, out, 0, 4);
        assertEquals(5, out[0]);
        assertEquals(5, out[3]);

        float[] fa = new float[]{1f, -2f, 3f};
        float[] fb = new float[]{4f, 5f, -6f};
        float[] fo = new float[3];
        simd.mul(fa, fb, fo, 0, 3);
        assertEquals(4f, fo[0], 0.0001f);
        assertEquals(-18f, fo[2], 0.0001f);

        byte[] ba = new byte[]{120, 100, -128};
        byte[] bb = new byte[]{20, 100, -1};
        byte[] bo = new byte[3];
        simd.add(ba, bb, bo, 0, 3);
        assertEquals(127, bo[0]);
        assertEquals(127, bo[1]);
        assertEquals(-128, bo[2]);
    }

    @FormTest
    void javaseRegistryGuardInSimulator() {
        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }

        int[] regA = simd.allocInt(16);
        int[] regB = simd.allocInt(16);
        int[] regO = simd.allocInt(16);
        simd.add(regA, regB, regO, 0, 16);

        if (CN.isSimulator()) {
            int[] plainA = new int[16];
            int[] plainB = new int[16];
            int[] plainO = new int[16];
            Throwable t = assertThrows(IllegalArgumentException.class, () -> simd.add(plainA, plainB, plainO, 0, 16));
            assertTrue(t.getMessage().indexOf("Simd.alloc") >= 0);
        }
    }

    @FormTest
    void genericBitwiseShiftCompareSelectOpsWork() {
        Simd simd = new Simd();

        byte[] a = new byte[]{1, 2, 3, 4};
        byte[] b = new byte[]{3, 2, 1, 4};
        byte[] mask = new byte[4];
        byte[] outB = new byte[4];
        simd.cmpGt(a, b, mask, 0, 4);
        simd.select(mask, a, b, outB, 0, 4);
        assertEquals(3, outB[0]);
        assertEquals(2, outB[1]);
        assertEquals(3, outB[2]);
        assertEquals(4, outB[3]);

        int[] ia = new int[]{0x0f0f0f0f, 8, -16, 7};
        int[] ib = new int[]{0x00ff00ff, 1, 2, 9};
        int[] io = new int[4];
        simd.and(ia, ib, io, 0, 4);
        assertEquals(0x000f000f, io[0]);
        simd.shrLogical(ia, 1, io, 0, 4);
        assertEquals(4, io[1]);
        simd.shrArithmetic(ia, 1, io, 0, 4);
        assertEquals(-8, io[2]);

        byte[] intMask = new byte[4];
        simd.cmpLt(ia, ib, intMask, 0, 4);
        simd.select(intMask, ia, ib, io, 0, 4);
        assertEquals(0x00ff00ff, io[0]);
        assertEquals(1, io[1]);
        assertEquals(-16, io[2]);
        assertEquals(7, io[3]);

        int[] unpack = new int[4];
        simd.unpackUnsignedByteToInt(new byte[]{-1, 0, 1, 127}, unpack, 0, 4);
        assertEquals(255, unpack[0]);
        assertEquals(127, unpack[3]);

        byte[] packed = new byte[4];
        simd.packIntToByteSaturating(new int[]{-129, -128, 127, 1000}, packed, 0, 4);
        assertEquals(-128, packed[0]);
        assertEquals(-128, packed[1]);
        assertEquals(127, packed[2]);
        assertEquals(127, packed[3]);

        byte[] permuted = new byte[4];
        simd.permuteBytes(new byte[]{10, 20, 30, 40}, new byte[]{3, 2, 1, -1}, permuted, 0, 4);
        assertEquals(40, permuted[0]);
        assertEquals(30, permuted[1]);
        assertEquals(20, permuted[2]);
        assertEquals(0, permuted[3]);
    }

    @FormTest
    void base64EncodeDecodeViaSimdFallback() {
        Simd simd = new Simd();

        // Test basic encoding
        byte[] input = new byte[]{(byte)'H', (byte)'e', (byte)'l', (byte)'l', (byte)'o'};
        int encodedLen = ((input.length + 2) / 3) * 4;
        byte[] encoded = new byte[encodedLen];
        int written = simd.base64Encode(input, 0, input.length, encoded, 0);
        assertEquals(encodedLen, written);
        String encodedStr = new String(encoded, 0, written);
        assertEquals("SGVsbG8=", encodedStr);

        // Test decoding
        byte[] decoded = new byte[input.length];
        int decodedLen = simd.base64Decode(encoded, 0, written, decoded, 0);
        assertEquals(input.length, decodedLen);
        for (int i = 0; i < input.length; i++) {
            assertEquals(input[i], decoded[i]);
        }

        // Test round-trip with larger data
        byte[] largeInput = new byte[256];
        for (int i = 0; i < largeInput.length; i++) {
            largeInput[i] = (byte) i;
        }
        int largeEncodedLen = ((largeInput.length + 2) / 3) * 4;
        byte[] largeEncoded = new byte[largeEncodedLen];
        int largeWritten = simd.base64Encode(largeInput, 0, largeInput.length, largeEncoded, 0);
        assertEquals(largeEncodedLen, largeWritten);

        byte[] largeDecoded = new byte[largeInput.length];
        int largeDecodedLen = simd.base64Decode(largeEncoded, 0, largeWritten, largeDecoded, 0);
        assertEquals(largeInput.length, largeDecodedLen);
        for (int i = 0; i < largeInput.length; i++) {
            assertEquals(largeInput[i], largeDecoded[i], "Mismatch at index " + i);
        }
    }

    @FormTest
    void base64SimdMethodsMatchScalar() {
        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }

        // Test that SIMD encode matches scalar encode
        byte[] input = new byte[8192];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte)(i * 31 + 17);
        }

        int encodedLen = ((input.length + 2) / 3) * 4;
        byte[] scalarEncoded = new byte[encodedLen];
        int scalarWritten = Base64.encodeNoNewline(input, scalarEncoded);

        byte[] simdInput = simd.allocByte(input.length);
        System.arraycopy(input, 0, simdInput, 0, input.length);
        byte[] simdEncoded = simd.allocByte(encodedLen);
        int[] scratch = simd.allocInt(192);
        int simdWritten = Base64.encodeNoNewlineSimd(simdInput, 0, simdInput.length, simdEncoded, 0, scratch);

        assertEquals(scalarWritten, simdWritten);
        for (int i = 0; i < scalarWritten; i++) {
            assertEquals(scalarEncoded[i], simdEncoded[i], "Encode mismatch at index " + i);
        }

        // Test that SIMD decode matches scalar decode
        byte[] scalarDecoded = new byte[input.length];
        int scalarDecLen = Base64.decode(scalarEncoded, scalarDecoded);

        byte[] simdDecoded = simd.allocByte(input.length);
        int simdDecLen = Base64.decodeNoWhitespaceSimd(simdEncoded, 0, simdWritten, simdDecoded, 0, scratch);

        assertEquals(scalarDecLen, simdDecLen);
        for (int i = 0; i < scalarDecLen; i++) {
            assertEquals(scalarDecoded[i], simdDecoded[i], "Decode mismatch at index " + i);
        }
    }
}
