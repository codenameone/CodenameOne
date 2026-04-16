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

        byte[] lookedUp = new byte[4];
        simd.lookupBytes(new byte[]{11, 22, 33, 44}, new byte[]{3, 0, 2, 9}, lookedUp, 0, 4);
        assertEquals(44, lookedUp[0]);
        assertEquals(11, lookedUp[1]);
        assertEquals(33, lookedUp[2]);
        assertEquals(0, lookedUp[3]);
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

    @FormTest
    void byteShlAndShrLogicalWork() {
        Simd simd = new Simd();
        byte[] src = new byte[]{(byte)0xAB, (byte)0x01, (byte)0xFF, (byte)0x80};
        byte[] dst = new byte[4];

        simd.shl(src, 4, dst, 0, 4);
        assertEquals((byte)0xB0, dst[0]);
        assertEquals((byte)0x10, dst[1]);
        assertEquals((byte)0xF0, dst[2]);
        assertEquals((byte)0x00, dst[3]);

        simd.shrLogical(src, 4, dst, 0, 4);
        assertEquals((byte)0x0A, dst[0]);
        assertEquals((byte)0x00, dst[1]);
        assertEquals((byte)0x0F, dst[2]);
        assertEquals((byte)0x08, dst[3]);
    }

    @FormTest
    void addWrappingAndSubWrappingWork() {
        Simd simd = new Simd();
        byte[] a = new byte[]{(byte)200, (byte)100, (byte)0, (byte)255};
        byte[] b = new byte[]{(byte)100, (byte)200, (byte)1, (byte)1};
        byte[] out = new byte[4];

        simd.addWrapping(a, b, out, 0, 4);
        assertEquals((byte)44, out[0]);   // 200+100=300 mod 256=44
        assertEquals((byte)44, out[1]);   // 100+200=300 mod 256=44
        assertEquals((byte)1, out[2]);    // 0+1=1
        assertEquals((byte)0, out[3]);    // 255+1=256 mod 256=0

        simd.subWrapping(a, b, out, 0, 4);
        assertEquals((byte)100, out[0]);  // 200-100=100
        assertEquals((byte)156, out[1]);  // 100-200=-100 mod 256=156
        assertEquals((byte)255, out[2]);  // 0-1=-1 mod 256=255
        assertEquals((byte)254, out[3]);  // 255-1=254
    }

    @FormTest
    void offsetBasedIntOpsWork() {
        Simd simd = new Simd();

        // Test offset-based unpack
        byte[] bytes = new byte[]{10, 20, (byte)200, (byte)255};
        int[] ints = new int[8];
        simd.unpackUnsignedByteToInt(bytes, 0, ints, 4, 4);
        assertEquals(10, ints[4]);
        assertEquals(20, ints[5]);
        assertEquals(200, ints[6]);
        assertEquals(255, ints[7]);

        // Test offset-based add
        int[] a = new int[]{0, 0, 5, 10, 15, 20};
        int[] b = new int[]{1, 2, 3, 4, 5, 6};
        int[] out = new int[6];
        simd.add(a, 2, b, 0, out, 1, 4);
        assertEquals(6, out[1]);   // a[2]+b[0] = 5+1
        assertEquals(12, out[2]);  // a[3]+b[1] = 10+2
        assertEquals(18, out[3]);  // a[4]+b[2] = 15+3
        assertEquals(24, out[4]);  // a[5]+b[3] = 20+4

        // Test offset-based cmpLt
        int[] vals = new int[]{5, 15, 25, 35};
        int[] thresh = new int[]{10, 10, 10, 10};
        byte[] mask = new byte[4];
        simd.cmpLt(vals, 0, thresh, 0, mask, 0, 4);
        assertEquals((byte)-1, mask[0]);
        assertEquals((byte)0, mask[1]);
        assertEquals((byte)0, mask[2]);
        assertEquals((byte)0, mask[3]);

        // Test offset-based cmpEq
        int[] vals2 = new int[]{10, 20, 10, 30};
        simd.cmpEq(vals2, 0, thresh, 0, mask, 0, 4);
        assertEquals((byte)-1, mask[0]);
        assertEquals((byte)0, mask[1]);
        assertEquals((byte)-1, mask[2]);
        assertEquals((byte)0, mask[3]);

        // Test offset-based select
        int[] trueV = new int[]{100, 200, 300, 400};
        int[] falseV = new int[]{-1, -2, -3, -4};
        int[] result = new int[4];
        mask[0] = -1; mask[1] = 0; mask[2] = -1; mask[3] = 0;
        simd.select(mask, 0, trueV, 0, falseV, 0, result, 0, 4);
        assertEquals(100, result[0]);
        assertEquals(-2, result[1]);
        assertEquals(300, result[2]);
        assertEquals(-4, result[3]);

        byte[] maskOut = new byte[4];
        simd.cmpEq(new byte[]{4, 5, 4, 6}, (byte)4, maskOut, 0, 4);
        assertEquals((byte)-1, maskOut[0]);
        assertEquals((byte)0, maskOut[1]);
        assertEquals((byte)-1, maskOut[2]);
        assertEquals((byte)0, maskOut[3]);

        simd.cmpLt(new byte[]{1, 2, 3, 4}, (byte)3, maskOut, 0, 4);
        assertEquals((byte)-1, maskOut[0]);
        assertEquals((byte)-1, maskOut[1]);
        assertEquals((byte)0, maskOut[2]);
        assertEquals((byte)0, maskOut[3]);

        byte[] wrapped = new byte[4];
        simd.addWrapping(new byte[]{1, 2, (byte)255, (byte)128}, (byte)2, wrapped, 0, 4);
        assertEquals((byte)3, wrapped[0]);
        assertEquals((byte)4, wrapped[1]);
        assertEquals((byte)1, wrapped[2]);
        assertEquals((byte)130, wrapped[3]);

        simd.subWrapping(new byte[]{1, 2, 0, (byte)128}, (byte)2, wrapped, 0, 4);
        assertEquals((byte)255, wrapped[0]);
        assertEquals((byte)0, wrapped[1]);
        assertEquals((byte)254, wrapped[2]);
        assertEquals((byte)126, wrapped[3]);

        int[] interleavedInts = new int[48];
        simd.unpackUnsignedByteToIntInterleaved3(
                new byte[]{
                        10, 11, 12,
                        20, 21, 22,
                        30, 31, 32,
                        40, 41, 42
                },
                0,
                interleavedInts,
                0,
                16,
                32,
                4);
        assertEquals(10, interleavedInts[0]);
        assertEquals(20, interleavedInts[1]);
        assertEquals(41, interleavedInts[16 + 3]);
        assertEquals(32, interleavedInts[32 + 2]);

        byte[] interleavedBytes = new byte[16];
        simd.packIntToByteTruncateInterleaved4(
                new int[]{
                        65, 66, 67, 68,
                        69, 70, 71, 72,
                        73, 74, 75, 76,
                        77, 78, 79, 80
                },
                0,
                4,
                8,
                12,
                interleavedBytes,
                0,
                4);
        assertEquals((byte)65, interleavedBytes[0]);
        assertEquals((byte)69, interleavedBytes[1]);
        assertEquals((byte)73, interleavedBytes[2]);
        assertEquals((byte)77, interleavedBytes[3]);
        assertEquals((byte)68, interleavedBytes[12]);
        assertEquals((byte)72, interleavedBytes[13]);
        assertEquals((byte)76, interleavedBytes[14]);
        assertEquals((byte)80, interleavedBytes[15]);

        byte[] stripe0 = new byte[4];
        byte[] stripe1 = new byte[4];
        byte[] stripe2 = new byte[4];
        byte[] stripe3 = new byte[4];
        simd.unpackBytesInterleaved3(
                new byte[]{
                        10, 11, 12,
                        20, 21, 22,
                        30, 31, 32,
                        40, 41, 42
                },
                0,
                stripe0,
                stripe1,
                stripe2,
                4);
        assertEquals((byte)10, stripe0[0]);
        assertEquals((byte)20, stripe0[1]);
        assertEquals((byte)31, stripe1[2]);
        assertEquals((byte)42, stripe2[3]);

        simd.unpackBytesInterleaved4(
                new byte[]{
                        1, 2, 3, 4,
                        5, 6, 7, 8,
                        9, 10, 11, 12,
                        13, 14, 15, 16
                },
                0,
                stripe0,
                stripe1,
                stripe2,
                stripe3,
                4);
        assertEquals((byte)1, stripe0[0]);
        assertEquals((byte)5, stripe0[1]);
        assertEquals((byte)10, stripe1[2]);
        assertEquals((byte)15, stripe2[3]);
        assertEquals((byte)16, stripe3[3]);

        byte[] packed3 = new byte[12];
        simd.packBytesInterleaved3(
                new byte[]{1, 5, 9, 13},
                new byte[]{2, 6, 10, 14},
                new byte[]{3, 7, 11, 15},
                packed3,
                0,
                4);
        assertEquals((byte)1, packed3[0]);
        assertEquals((byte)2, packed3[1]);
        assertEquals((byte)3, packed3[2]);
        assertEquals((byte)13, packed3[9]);
        assertEquals((byte)14, packed3[10]);
        assertEquals((byte)15, packed3[11]);

        byte[] packed4 = new byte[16];
        simd.packBytesInterleaved4(
                new byte[]{1, 5, 9, 13},
                new byte[]{2, 6, 10, 14},
                new byte[]{3, 7, 11, 15},
                new byte[]{4, 8, 12, 16},
                packed4,
                0,
                4);
        assertEquals((byte)1, packed4[0]);
        assertEquals((byte)4, packed4[3]);
        assertEquals((byte)9, packed4[8]);
        assertEquals((byte)16, packed4[15]);
    }
}
