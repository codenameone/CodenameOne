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
    void testAllocaFallbackAndSimdOperations() {
        Simd fallback = new Simd();
        assertEquals(16, fallback.allocaByte(16).length);
        assertEquals(16, fallback.allocaInt(16).length);
        assertEquals(16, fallback.allocaFloat(16).length);
        assertEquals(0, fallback.allocaByteZeroed(16)[3]);
        assertEquals(0, fallback.allocaIntZeroed(16)[3]);
        assertEquals(0.0f, fallback.allocaFloatZeroed(16)[3], 0.0f);
        assertEquals(7, fallback.allocaByteFilled(16, (byte)7)[3]);
        assertEquals(7, fallback.allocaIntFilled(16, 7)[3]);
        assertEquals(7.5f, fallback.allocaFloatFilled(16, 7.5f)[3], 0.0f);
        assertThrows(IllegalArgumentException.class, () -> fallback.allocaByte(15));
        assertThrows(IllegalArgumentException.class, () -> fallback.allocaInt(15));
        assertThrows(IllegalArgumentException.class, () -> fallback.allocaFloat(15));

        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }

        int[] regA = simd.allocaInt(16);
        int[] regB = simd.allocaInt(16);
        int[] regO = simd.allocaInt(16);
        regA[0] = 3;
        regA[1] = -2;
        regB[0] = 4;
        regB[1] = 5;
        simd.add(regA, regB, regO, 0, 2);
        assertEquals(7, regO[0]);
        assertEquals(3, regO[1]);

        int[] zeroed = simd.allocaIntZeroed(16);
        int[] filled = simd.allocaIntFilled(16, 9);
        assertEquals(0, zeroed[5]);
        assertEquals(9, filled[5]);

        byte[] bytesA = simd.allocaByte(16);
        byte[] bytesB = simd.allocaByte(16);
        byte[] bytesO = simd.allocaByte(16);
        bytesA[0] = 120;
        bytesA[1] = 1;
        bytesA[2] = 127;
        bytesB[0] = 20;
        bytesB[1] = 2;
        bytesB[2] = 1;
        // Validate saturated byte addition for both near-limit and overflow inputs.
        simd.add(bytesA, bytesB, bytesO, 0, 3);
        assertEquals(127, bytesO[0]);
        assertEquals(3, bytesO[1]);
        assertEquals(127, bytesO[2]);
        assertEquals(11, simd.allocaByteFilled(16, (byte)11)[4]);
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

        byte[] unpacked0 = new byte[2];
        byte[] unpacked1 = new byte[2];
        byte[] unpacked2 = new byte[2];
        byte[] unpacked3 = new byte[2];
        int lookupOr = simd.unpackLookupBytesInterleaved4(
                new byte[]{10, 20, 30, 40, -1},
                new byte[]{3, 1, 0, 2, 4, 0, 1, 2},
                0,
                unpacked0,
                unpacked1,
                unpacked2,
                unpacked3,
                2);
        assertEquals(40, unpacked0[0]);
        assertEquals(20, unpacked1[0]);
        assertEquals(10, unpacked2[0]);
        assertEquals(30, unpacked3[0]);
        assertEquals(-1, unpacked0[1]);
        assertEquals(10, unpacked1[1]);
        assertEquals(20, unpacked2[1]);
        assertEquals(30, unpacked3[1]);
        assertTrue(lookupOr < 0);

        byte[] offsetLookup = new byte[8];
        simd.lookupBytes(new byte[]{11, 22, 33, 44}, new byte[]{9, 9, 3, 0, 2, 9, 9, 9}, 2, offsetLookup, 1, 4);
        assertEquals(44, offsetLookup[1]);
        assertEquals(11, offsetLookup[2]);
        assertEquals(33, offsetLookup[3]);
        assertEquals(0, offsetLookup[4]);

        byte[] offsetBitwise = new byte[8];
        simd.and(new byte[]{0, (byte)0xF3, (byte)0xCC, 0, 0}, 1, new byte[]{0, (byte)0x3F, (byte)0x0F, 0, 0}, 1, offsetBitwise, 2, 2);
        assertEquals((byte)0x33, offsetBitwise[2]);
        assertEquals((byte)0x0C, offsetBitwise[3]);
        simd.or(new byte[]{0, (byte)0xF0, (byte)0xC0, 0, 0}, 1, new byte[]{0, (byte)0x0F, (byte)0x0C, 0, 0}, 1, offsetBitwise, 4, 2);
        assertEquals((byte)0xFF, offsetBitwise[4]);
        assertEquals((byte)0xCC, offsetBitwise[5]);
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
        int simdWritten = Base64.encodeNoNewlineSimd(simdInput, 0, simdInput.length, simdEncoded, 0);

        assertEquals(scalarWritten, simdWritten);
        for (int i = 0; i < scalarWritten; i++) {
            assertEquals(scalarEncoded[i], simdEncoded[i], "Encode mismatch at index " + i);
        }

        // Test that SIMD decode matches scalar decode
        byte[] scalarDecoded = new byte[input.length];
        int scalarDecLen = Base64.decode(scalarEncoded, scalarDecoded);

        byte[] simdDecoded = simd.allocByte(input.length);
        int simdDecLen = Base64.decodeNoWhitespaceSimd(simdEncoded, 0, simdWritten, simdDecoded, 0);

        assertEquals(scalarDecLen, simdDecLen);
        for (int i = 0; i < scalarDecLen; i++) {
            assertEquals(scalarDecoded[i], simdDecoded[i], "Decode mismatch at index " + i);
        }

        byte[] legacyEncoded = simd.allocByte(encodedLen);
        byte[] legacyDecoded = simd.allocByte(input.length);
        int legacyWritten = Base64.encodeNoNewlineSimd(simdInput, 0, simdInput.length, legacyEncoded, 0, scratch);
        int legacyDecodedLen = Base64.decodeNoWhitespaceSimd(legacyEncoded, 0, legacyWritten, legacyDecoded, 0, scratch);
        assertEquals(simdWritten, legacyWritten);
        assertEquals(simdDecLen, legacyDecodedLen);
        for (int i = 0; i < legacyWritten; i++) {
            assertEquals(simdEncoded[i], legacyEncoded[i], "Legacy encode mismatch at index " + i);
        }
        for (int i = 0; i < legacyDecodedLen; i++) {
            assertEquals(simdDecoded[i], legacyDecoded[i], "Legacy decode mismatch at index " + i);
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

        byte[] offsetDst = new byte[8];
        simd.shl(new byte[]{0, (byte)0xAB, (byte)0x01, (byte)0xFF, (byte)0x80, 0}, 1, 4, offsetDst, 2, 4);
        assertEquals((byte)0xB0, offsetDst[2]);
        assertEquals((byte)0x10, offsetDst[3]);
        assertEquals((byte)0xF0, offsetDst[4]);
        assertEquals((byte)0x00, offsetDst[5]);

        simd.shrLogical(new byte[]{0, (byte)0xAB, (byte)0x01, (byte)0xFF, (byte)0x80, 0}, 1, 4, offsetDst, 0, 4);
        assertEquals((byte)0x0A, offsetDst[0]);
        assertEquals((byte)0x00, offsetDst[1]);
        assertEquals((byte)0x0F, offsetDst[2]);
        assertEquals((byte)0x08, offsetDst[3]);
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

        byte[] slab = new byte[20];
        simd.unpackBytesInterleaved3(
                new byte[]{
                        10, 11, 12,
                        20, 21, 22,
                        30, 31, 32,
                        40, 41, 42
                },
                0,
                slab,
                1,
                6,
                11,
                4);
        assertEquals((byte)10, slab[1]);
        assertEquals((byte)20, slab[2]);
        assertEquals((byte)31, slab[8]);
        assertEquals((byte)42, slab[14]);

        simd.unpackBytesInterleaved4(
                new byte[]{
                        1, 2, 3, 4,
                        5, 6, 7, 8,
                        9, 10, 11, 12,
                        13, 14, 15, 16
                },
                0,
                slab,
                0,
                4,
                8,
                12,
                4);
        assertEquals((byte)1, slab[0]);
        assertEquals((byte)5, slab[1]);
        assertEquals((byte)10, slab[6]);
        assertEquals((byte)15, slab[11]);
        assertEquals((byte)16, slab[15]);

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

        byte[] packedFromSlab3 = new byte[12];
        simd.packBytesInterleaved3(new byte[]{
                        1, 5, 9, 13,
                        2, 6, 10, 14,
                        3, 7, 11, 15
                },
                0,
                4,
                8,
                packedFromSlab3,
                0,
                4);
        assertEquals((byte)1, packedFromSlab3[0]);
        assertEquals((byte)2, packedFromSlab3[1]);
        assertEquals((byte)3, packedFromSlab3[2]);
        assertEquals((byte)13, packedFromSlab3[9]);
        assertEquals((byte)14, packedFromSlab3[10]);
        assertEquals((byte)15, packedFromSlab3[11]);

        byte[] packedFromSlab4 = new byte[16];
        simd.packBytesInterleaved4(new byte[]{
                        1, 5, 9, 13,
                        2, 6, 10, 14,
                        3, 7, 11, 15,
                        4, 8, 12, 16
                },
                0,
                4,
                8,
                12,
                packedFromSlab4,
                0,
                4);
        assertEquals((byte)1, packedFromSlab4[0]);
        assertEquals((byte)4, packedFromSlab4[3]);
        assertEquals((byte)9, packedFromSlab4[8]);
        assertEquals((byte)16, packedFromSlab4[15]);
    }

    @FormTest
    void blendByMaskTestNonzeroFusesAlphaReplace() {
        // Verify the fallback semantics on raw int[]s.
        Simd fallback = new Simd();
        int alphaMask = 0x80000000;
        int[] src = new int[]{
                0x00112233, // alpha 0  → unchanged
                0xff445566, // alpha non-zero → (& 0x00ffffff) | alphaMask
                0x12345678, // alpha non-zero → splice
                0x00000000  // alpha 0  → unchanged (and remains 0)
        };
        int[] dst = new int[4];
        fallback.blendByMaskTestNonzero(src, 0, 0xff000000, 0x00ffffff, alphaMask, dst, 0, 4);
        assertEquals(0x00112233, dst[0]);
        assertEquals(alphaMask | 0x00445566, dst[1]);
        assertEquals(alphaMask | 0x00345678, dst[2]);
        assertEquals(0x00000000, dst[3]);

        // Run the same test against the platform Simd (registered arrays where required).
        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }
        int n = 32; // exercise the vector loop and a tail
        int[] regSrc = simd.allocInt(n);
        int[] regDst = simd.allocInt(n);
        for (int i = 0; i < n; i++) {
            // Alternate transparent / opaque pixels with varied RGB content.
            regSrc[i] = ((i & 1) == 0 ? 0 : 0xff000000) | ((i * 0x010203) & 0x00ffffff);
        }
        simd.blendByMaskTestNonzero(regSrc, 0, 0xff000000, 0x00ffffff, alphaMask, regDst, 0, n);
        for (int i = 0; i < n; i++) {
            int v = regSrc[i];
            int expected = ((v & 0xff000000) != 0)
                    ? (v & 0x00ffffff) | alphaMask
                    : v;
            assertEquals(expected, regDst[i], "mismatch at index " + i);
        }
    }

    @FormTest
    void blendByMaskTestNonzeroSubstituteOnKeepEqFusesRemoveColor() {
        // Verify the fallback semantics on raw int[]s.
        Simd fallback = new Simd();
        int alphaMask = 0x80000000;
        int removeColor = 0x00abcdef; // RGB to be wiped to 0 after alpha replacement
        int[] src = new int[]{
                0x00abcdef, // alpha 0  → unchanged (even though RGB matches removeColor)
                0xff445566, // alpha non-zero, RGB no match → splice alpha
                0x12abcdef, // alpha non-zero, RGB matches → must become 0
                0x00000000  // alpha 0  → unchanged (and remains 0)
        };
        int[] dst = new int[4];
        fallback.blendByMaskTestNonzeroSubstituteOnKeepEq(
                src, 0, 0xff000000, 0x00ffffff, alphaMask, removeColor, 0, dst, 0, 4);
        assertEquals(0x00abcdef, dst[0]);
        assertEquals(alphaMask | 0x00445566, dst[1]);
        assertEquals(0, dst[2]);
        assertEquals(0x00000000, dst[3]);

        // Run the same test against the platform Simd (registered arrays where required).
        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }
        int n = 32; // exercise the vector loop and a tail
        int[] regSrc = simd.allocInt(n);
        int[] regDst = simd.allocInt(n);
        for (int i = 0; i < n; i++) {
            // Mix transparent / opaque pixels, with every 5th opaque pixel matching removeColor.
            int alpha = (i & 1) == 0 ? 0 : 0xff000000;
            int rgb = (i % 5 == 0) ? removeColor : ((i * 0x010203) & 0x00ffffff);
            regSrc[i] = alpha | rgb;
        }
        simd.blendByMaskTestNonzeroSubstituteOnKeepEq(
                regSrc, 0, 0xff000000, 0x00ffffff, alphaMask, removeColor, 0, regDst, 0, n);
        for (int i = 0; i < n; i++) {
            int v = regSrc[i];
            int expected;
            if ((v & 0xff000000) == 0) {
                expected = v;
            } else if ((v & 0x00ffffff) == removeColor) {
                expected = 0;
            } else {
                expected = (v & 0x00ffffff) | alphaMask;
            }
            assertEquals(expected, regDst[i], "mismatch at index " + i);
        }
    }

    @FormTest
    void replaceTopByteFromUnsignedBytesFusesApplyMask() {
        // Verify the fallback semantics on raw arrays.
        Simd fallback = new Simd();
        int[] rgb = new int[]{0xdeadbeef, 0x12345678, 0x00112233, 0xffffffff};
        byte[] alpha = new byte[]{(byte)0x00, (byte)0x80, (byte)0xff, (byte)0x7f};
        int[] dst = new int[4];
        fallback.replaceTopByteFromUnsignedBytes(rgb, 0, alpha, 0, dst, 0, 4);
        assertEquals(0x00adbeef, dst[0]);
        assertEquals(0x80345678, dst[1]);
        assertEquals(0xff112233, dst[2]);
        assertEquals(0x7fffffff, dst[3]);

        // Run the same test against the platform Simd (registered arrays where required).
        Simd simd = Simd.get();
        if (!simd.isSupported()) {
            return;
        }
        int n = 35; // exercise the 16-wide vector loop and a non-trivial tail
        int[] regRgb = simd.allocInt(n);
        byte[] regAlpha = simd.allocByte(n);
        int[] regDst = simd.allocInt(n);
        for (int i = 0; i < n; i++) {
            regRgb[i] = (i << 16) | ((i * 7) & 0xff) | (((i * 13) & 0xff) << 8) | (0x42 << 24);
            regAlpha[i] = (byte) (i * 11);
        }
        simd.replaceTopByteFromUnsignedBytes(regRgb, 0, regAlpha, 0, regDst, 0, n);
        for (int i = 0; i < n; i++) {
            int expected = (regRgb[i] & 0x00ffffff) | ((regAlpha[i] & 0xff) << 24);
            assertEquals(expected, regDst[i], "mismatch at index " + i);
        }
    }
}
