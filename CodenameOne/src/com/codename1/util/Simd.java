/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.util;

import com.codename1.annotations.Concrete;
import com.codename1.ui.CN;

/// Portable SIMD API with Java fallback implementations.
@Concrete(name = "com.codename1.impl.ios.IOSSimd")
public class Simd {
    /// Returns the singleton instance of the Simd class. Equivalent to `CN.getSimd();`
    public static Simd get() {
        return CN.getSimd();
    }

    /// Returns true if SIMD instructions are natively supported
    /// if this returns false the APIs in this class would still work
    /// using fallback loop code
    public boolean isSupported() {
        return false;
    }

    /// Allocates an aligned memory block for efficient SIMD
    /// operations. All operations MUST be performed on aligned
    /// arrays and shouldn't use arrays created with `new`. Operations
    /// on unaligned arrays might produce undefined results.
    public byte[] allocByte(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return new byte[size];
    }

    /// Allocates an aligned memory block for efficient SIMD
    /// operations. All operations MUST be performed on aligned
    /// arrays and shouldn't use arrays created with `new`. Operations
    /// on unaligned arrays might produce undefined results.
    public int[] allocInt(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return new int[size];
    }

    /// Allocates an aligned memory block for efficient SIMD
    /// operations. All operations MUST be performed on aligned
    /// arrays and shouldn't use arrays created with `new`. Operations
    /// on unaligned arrays might produce undefined results.
    public float[] allocFloat(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return new float[size];
    }

    /// @deprecated This is a special scratch-allocation API. On ParparVM this may be lowered to a
    /// stack-backed faux array, so callers MUST keep it method-local, MUST be extremely cautious
    /// when using it, and MUST treat its contents as undefined until written. Use `allocByte(int)`
    /// for heap-backed arrays.
    public byte[] allocaByte(int size) {
        return allocByte(size);
    }

    /// @deprecated This is a special scratch-allocation API. On ParparVM this may be lowered to a
    /// stack-backed faux array, so callers MUST keep it method-local, MUST be extremely cautious
    /// when using it, and MUST treat its contents as undefined until written. Use `allocInt(int)`
    /// for heap-backed arrays.
    public int[] allocaInt(int size) {
        return allocInt(size);
    }

    /// @deprecated This is a special scratch-allocation API. On ParparVM this may be lowered to a
    /// stack-backed faux array, so callers MUST keep it method-local, MUST be extremely cautious
    /// when using it, and MUST treat its contents as undefined until written. Use `allocFloat(int)`
    /// for heap-backed arrays.
    public float[] allocaFloat(int size) {
        return allocFloat(size);
    }

    /// Special scratch-allocation API that guarantees a zero-initialized byte array while retaining
    /// the same method-local constraints as `allocaByte(int)`.
    public byte[] allocaByteZeroed(int size) {
        byte[] out = allocaByte(size);
        fillByte(out, (byte) 0);
        return out;
    }

    /// Special scratch-allocation API that guarantees a zero-initialized int array while retaining
    /// the same method-local constraints as `allocaInt(int)`.
    public int[] allocaIntZeroed(int size) {
        int[] out = allocaInt(size);
        fillInt(out, 0);
        return out;
    }

    /// Special scratch-allocation API that guarantees a zero-initialized float array while retaining
    /// the same method-local constraints as `allocaFloat(int)`.
    public float[] allocaFloatZeroed(int size) {
        float[] out = allocaFloat(size);
        fillFloat(out, 0.0f);
        return out;
    }

    /// Special scratch-allocation API that guarantees every byte starts with the same value while
    /// retaining the same method-local constraints as `allocaByte(int)`.
    public byte[] allocaByteFilled(int size, byte value) {
        byte[] out = allocaByte(size);
        fillByte(out, value);
        return out;
    }

    /// Special scratch-allocation API that guarantees every int starts with the same value while
    /// retaining the same method-local constraints as `allocaInt(int)`.
    public int[] allocaIntFilled(int size, int value) {
        int[] out = allocaInt(size);
        fillInt(out, value);
        return out;
    }

    /// Special scratch-allocation API that guarantees every float starts with the same value while
    /// retaining the same method-local constraints as `allocaFloat(int)`.
    public float[] allocaFloatFilled(int size, float value) {
        float[] out = allocaFloat(size);
        fillFloat(out, value);
        return out;
    }

    private static void fillByte(byte[] arr, byte value) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = value;
        }
    }

    private static void fillInt(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = value;
        }
    }

    private static void fillFloat(float[] arr, float value) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = value;
        }
    }

    /// Looks up values from a table using unsigned byte indices.
    public void lookupBytes(byte[] table, byte[] indices, byte[] dst, int offset, int length) {
        lookupBytes(table, indices, offset, dst, offset, length);
    }

    /// Looks up values from a table using unsigned byte indices.
    public void lookupBytes(byte[] table, byte[] indices, int indicesOffset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int idx = indices[indicesOffset + i] & 0xff;
            dst[dstOffset + i] = idx < table.length ? table[idx] : 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void and(byte[] srcA, int srcAOffset, byte[] srcB, int srcBOffset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte) (srcA[srcAOffset + i] & srcB[srcBOffset + i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void or(byte[] srcA, int srcAOffset, byte[] srcB, int srcBOffset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte) (srcA[srcAOffset + i] | srcB[srcBOffset + i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shl(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length) {
        int shift = bits & 7;
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte) ((src[srcOffset + i] & 0xff) << shift);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shrLogical(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length) {
        int shift = bits & 7;
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte) ((src[srcOffset + i] & 0xff) >>> shift);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length) {
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 3;
            dst[dst0Offset + i] = src[srcIndex];
            dst[dst1Offset + i] = src[srcIndex + 1];
            dst[dst2Offset + i] = src[srcIndex + 2];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void add(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(srcA[i] + srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void sub(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(srcA[i] - srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void mul(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(srcA[i] * srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void min(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] < srcB[i] ? srcA[i] : srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void max(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] > srcB[i] ? srcA[i] : srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void abs(byte[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            if (v == Byte.MIN_VALUE) {
                dst[i] = Byte.MAX_VALUE;
            } else {
                dst[i] = (byte) Math.abs(v);
            }
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void clamp(byte[] src, byte[] dst, byte minValue, byte maxValue, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            if (v < minValue) {
                dst[i] = minValue;
            } else if (v > maxValue) {
                dst[i] = maxValue;
            } else {
                dst[i] = (byte) v;
            }
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void and(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        and(srcA, offset, srcB, offset, dst, offset, length);
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void or(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        or(srcA, offset, srcB, offset, dst, offset, length);
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void xor(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) (srcA[i] ^ srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void not(byte[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) (~src[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpEq(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] == srcB[i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpEq(byte[] src, byte value, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = src[i] == value ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpLt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] < srcB[i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpLt(byte[] src, byte value, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = src[i] < value ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpGt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] > srcB[i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpRange(byte[] src, byte minValue, byte maxValue, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            dstMask[i] = v >= minValue && v <= maxValue ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void select(byte[] mask, byte[] trueValues, byte[] falseValues, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = mask[i] != 0 ? trueValues[i] : falseValues[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shl(byte[] src, int bits, byte[] dst, int offset, int length) {
        shl(src, offset, bits, dst, offset, length);
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shrLogical(byte[] src, int bits, byte[] dst, int offset, int length) {
        shrLogical(src, offset, bits, dst, offset, length);
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void addWrapping(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) (srcA[i] + srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void addWrapping(byte[] src, byte value, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) (src[i] + value);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void subWrapping(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) (srcA[i] - srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void subWrapping(byte[] src, byte value, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) (src[i] - value);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackUnsignedByteToInt(byte[] src, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] & 0xff;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackUnsignedByteToInt(byte[] src, int srcOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] & 0xff;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackUnsignedByteToIntInterleaved3(byte[] src, int srcOffset, int[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length) {
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 3;
            dst[dst0Offset + i] = src[srcIndex] & 0xff;
            dst[dst1Offset + i] = src[srcIndex + 1] & 0xff;
            dst[dst2Offset + i] = src[srcIndex + 2] & 0xff;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, int length) {
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 3;
            dst0[i] = src[srcIndex];
            dst1[i] = src[srcIndex + 1];
            dst2[i] = src[srcIndex + 2];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackBytesInterleaved4(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, byte[] dst3, int length) {
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 4;
            dst0[i] = src[srcIndex];
            dst1[i] = src[srcIndex + 1];
            dst2[i] = src[srcIndex + 2];
            dst3[i] = src[srcIndex + 3];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void unpackBytesInterleaved4(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length) {
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 4;
            dst[dst0Offset + i] = src[srcIndex];
            dst[dst1Offset + i] = src[srcIndex + 1];
            dst[dst2Offset + i] = src[srcIndex + 2];
            dst[dst3Offset + i] = src[srcIndex + 3];
        }
    }

    /// Unpacks interleaved bytes, looks each byte up in the provided table, stores the
    /// looked-up values into separate lane arrays, and returns the bitwise OR of all
    /// written values.
    public int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, byte[] dst3, int length) {
        int or = 0;
        int tableLength = table.length;
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 4;
            int idx0 = src[srcIndex] & 0xff;
            int idx1 = src[srcIndex + 1] & 0xff;
            int idx2 = src[srcIndex + 2] & 0xff;
            int idx3 = src[srcIndex + 3] & 0xff;
            byte v0 = idx0 < tableLength ? table[idx0] : 0;
            byte v1 = idx1 < tableLength ? table[idx1] : 0;
            byte v2 = idx2 < tableLength ? table[idx2] : 0;
            byte v3 = idx3 < tableLength ? table[idx3] : 0;
            dst0[i] = v0;
            dst1[i] = v1;
            dst2[i] = v2;
            dst3[i] = v3;
            or |= v0 | v1 | v2 | v3;
        }
        return or;
    }

    /// Unpacks interleaved bytes, looks each byte up in the provided table, stores the
    /// looked-up values into virtual lane ranges in a destination array, and returns
    /// the bitwise OR of all written values.
    public int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length) {
        int or = 0;
        int tableLength = table.length;
        for (int i = 0; i < length; i++) {
            int srcIndex = srcOffset + i * 4;
            int idx0 = src[srcIndex] & 0xff;
            int idx1 = src[srcIndex + 1] & 0xff;
            int idx2 = src[srcIndex + 2] & 0xff;
            int idx3 = src[srcIndex + 3] & 0xff;
            byte v0 = idx0 < tableLength ? table[idx0] : 0;
            byte v1 = idx1 < tableLength ? table[idx1] : 0;
            byte v2 = idx2 < tableLength ? table[idx2] : 0;
            byte v3 = idx3 < tableLength ? table[idx3] : 0;
            dst[dst0Offset + i] = v0;
            dst[dst1Offset + i] = v1;
            dst[dst2Offset + i] = v2;
            dst[dst3Offset + i] = v3;
            or |= v0 | v1 | v2 | v3;
        }
        return or;
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packIntToByteSaturating(int[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(src[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packIntToByteTruncate(int[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte) src[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packIntToByteTruncate(int[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte) src[srcOffset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packIntToByteTruncateInterleaved4(int[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int dstIndex = dstOffset + i * 4;
            dst[dstIndex] = (byte) src[src0Offset + i];
            dst[dstIndex + 1] = (byte) src[src1Offset + i];
            dst[dstIndex + 2] = (byte) src[src2Offset + i];
            dst[dstIndex + 3] = (byte) src[src3Offset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packBytesInterleaved3(byte[] src0, byte[] src1, byte[] src2, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int dstIndex = dstOffset + i * 3;
            dst[dstIndex] = src0[i];
            dst[dstIndex + 1] = src1[i];
            dst[dstIndex + 2] = src2[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packBytesInterleaved3(byte[] src, int src0Offset, int src1Offset, int src2Offset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int dstIndex = dstOffset + i * 3;
            dst[dstIndex] = src[src0Offset + i];
            dst[dstIndex + 1] = src[src1Offset + i];
            dst[dstIndex + 2] = src[src2Offset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packBytesInterleaved4(byte[] src0, byte[] src1, byte[] src2, byte[] src3, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int dstIndex = dstOffset + i * 4;
            dst[dstIndex] = src0[i];
            dst[dstIndex + 1] = src1[i];
            dst[dstIndex + 2] = src2[i];
            dst[dstIndex + 3] = src3[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void packBytesInterleaved4(byte[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int dstIndex = dstOffset + i * 4;
            dst[dstIndex] = src[src0Offset + i];
            dst[dstIndex + 1] = src[src1Offset + i];
            dst[dstIndex + 2] = src[src2Offset + i];
            dst[dstIndex + 3] = src[src3Offset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void permuteBytes(byte[] src, byte[] indices, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int idx = indices[i];
            dst[i] = idx >= 0 && idx < src.length ? src[idx] : 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void add(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] + srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void add(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = srcA[srcAOffset + i] + srcB[srcBOffset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void sub(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] - srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void mul(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] * srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void min(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] < srcB[i] ? srcA[i] : srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void max(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] > srcB[i] ? srcA[i] : srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void abs(int[] src, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            dst[i] = v == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(v);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void clamp(int[] src, int[] dst, int minValue, int maxValue, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            if (v < minValue) {
                dst[i] = minValue;
            } else if (v > maxValue) {
                dst[i] = maxValue;
            } else {
                dst[i] = v;
            }
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void and(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] & srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void and(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = srcA[srcAOffset + i] & srcB[srcBOffset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void or(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] | srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void or(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = srcA[srcAOffset + i] | srcB[srcBOffset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void xor(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] ^ srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void not(int[] src, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = ~src[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shl(int[] src, int bits, int[] dst, int offset, int length) {
        int shift = bits & 31;
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] << shift;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shl(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length) {
        int shift = bits & 31;
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] << shift;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shrLogical(int[] src, int bits, int[] dst, int offset, int length) {
        int shift = bits & 31;
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] >>> shift;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shrLogical(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length) {
        int shift = bits & 31;
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] >>> shift;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void shrArithmetic(int[] src, int bits, int[] dst, int offset, int length) {
        int shift = bits & 31;
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] >> shift;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpEq(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] == srcB[i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpEq(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, byte[] dstMask, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dstMask[dstOffset + i] = srcA[srcAOffset + i] == srcB[srcBOffset + i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpLt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] < srcB[i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpLt(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, byte[] dstMask, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dstMask[dstOffset + i] = srcA[srcAOffset + i] < srcB[srcBOffset + i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpGt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] > srcB[i] ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void select(byte[] mask, int[] trueValues, int[] falseValues, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = mask[i] != 0 ? trueValues[i] : falseValues[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void select(byte[] mask, int maskOffset, int[] trueValues, int trueOffset, int[] falseValues, int falseOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = mask[maskOffset + i] != 0 ? trueValues[trueOffset + i] : falseValues[falseOffset + i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public int sum(int[] src, int offset, int length) {
        int out = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += src[i];
        }
        return out;
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public int dot(int[] srcA, int[] srcB, int offset, int length) {
        int out = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += srcA[i] * srcB[i];
        }
        return out;
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void add(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] + srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void sub(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] - srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void mul(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] * srcB[i];
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void min(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = Math.min(srcA[i], srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void max(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = Math.max(srcA[i], srcB[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void abs(float[] src, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = Math.abs(src[i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void clamp(float[] src, float[] dst, float minValue, float maxValue, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            float v = src[i];
            if (v < minValue) {
                dst[i] = minValue;
            } else if (v > maxValue) {
                dst[i] = maxValue;
            } else {
                dst[i] = v;
            }
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public float sum(float[] src, int offset, int length) {
        float out = 0f;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += src[i];
        }
        return out;
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public float dot(float[] srcA, float[] srcB, int offset, int length) {
        float out = 0f;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += srcA[i] * srcB[i];
        }
        return out;
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void and(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] & constant;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void or(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] | constant;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void xor(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] ^ constant;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpEq(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dstMask[dstOffset + i] = src[srcOffset + i] == constant ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpLt(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dstMask[dstOffset + i] = src[srcOffset + i] < constant ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void cmpGt(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dstMask[dstOffset + i] = src[srcOffset + i] > constant ? (byte) -1 : (byte) 0;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void not(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte) (~src[srcOffset + i]);
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void select(byte[] mask, int maskOffset, int trueConstant, int falseConstant, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = mask[maskOffset + i] != 0 ? trueConstant : falseConstant;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void select(byte[] mask, int maskOffset, int[] trueValues, int trueOffset, int falseConstant, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = mask[maskOffset + i] != 0 ? trueValues[trueOffset + i] : falseConstant;
        }
    }

    /// Exposes SIMD APIs directly **all arrays MUST be aligned arrays**
    public void select(byte[] mask, int maskOffset, int trueConstant, int[] falseValues, int falseOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = mask[maskOffset + i] != 0 ? trueConstant : falseValues[falseOffset + i];
        }
    }

    /// Fused single-pass conditional bit-blend driven by a masked-non-zero test of `src` against
    /// `testMask`. For every element:
    ///
    /// `dst[i] = (src[i] & testMask) != 0 ? (src[i] & trueKeepMask) | trueOrValue : src[i]`
    ///
    /// This collapses a "test-then-modify-or-keep-source" pattern that would otherwise require
    /// three or more separate primitive calls (and three or more passes over `src`) into a single
    /// pass. It maps directly to one NEON / SSE vector loop on platforms that ship a vectorized
    /// implementation. **All arrays MUST be aligned/registered arrays.**
    public void blendByMaskTestNonzero(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int v = src[srcOffset + i];
            dst[dstOffset + i] = (v & testMask) != 0 ? (v & trueKeepMask) | trueOrValue : v;
        }
    }

    /// Fused single-pass extension of `blendByMaskTestNonzero` that additionally substitutes
    /// `removeValue` whenever the post-mask result would equal `removeMatch`. For every element:
    ///
    /// ```
    /// v = src[i]
    /// if ((v & testMask) == 0)            dst[i] = v
    /// else if ((v & trueKeepMask) == removeMatch) dst[i] = removeValue
    /// else                                dst[i] = (v & trueKeepMask) | trueOrValue
    /// ```
    ///
    /// This collapses a `blendByMaskTestNonzero` + `cmpEq` + `select` chain (three passes over the
    /// buffer plus two scratch allocations) into a single vector pass. It maps to one NEON / SSE
    /// loop on platforms that ship a vectorized implementation. **All arrays MUST be aligned /
    /// registered arrays.**
    public void blendByMaskTestNonzeroSubstituteOnKeepEq(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int removeMatch, int removeValue, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int v = src[srcOffset + i];
            if ((v & testMask) == 0) {
                dst[dstOffset + i] = v;
            } else {
                int kept = v & trueKeepMask;
                dst[dstOffset + i] = (kept == removeMatch) ? removeValue : kept | trueOrValue;
            }
        }
    }

    /// Fused single-pass replacement of the top byte of every int in `rgbSrc` with the
    /// corresponding unsigned byte from `alphaSrc`. For every element:
    ///
    /// `dst[i] = (rgbSrc[i] & 0x00ffffff) | ((alphaSrc[i] & 0xff) << 24)`
    ///
    /// Designed for the `Image.applyMask` hot path, which previously required four separate
    /// primitive calls (`unpackUnsignedByteToInt`, `shl`, `and`, `or`) and two scratch
    /// allocations. Maps to a single NEON / SSE vector loop (`vmovl_u8` =&gt; `vshlq_n_u32(24)`
    /// =&gt; `vorrq(vandq, ...)`) on platforms that ship a vectorized implementation.
    /// **All arrays MUST be aligned / registered arrays.**
    public void replaceTopByteFromUnsignedBytes(int[] rgbSrc, int rgbSrcOffset, byte[] alphaSrc, int alphaSrcOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            int rgb = rgbSrc[rgbSrcOffset + i] & 0x00ffffff;
            int alpha = (alphaSrc[alphaSrcOffset + i] & 0xff) << 24;
            dst[dstOffset + i] = rgb | alpha;
        }
    }


    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateBinaryByte(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateMaskBinaryByte(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dstMask, "dstMask");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dstMask.length, offset, length, "dstMask");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateRangeMaskByte(byte[] src, byte[] dstMask, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dstMask, "dstMask");
        validateRange(src.length, offset, length, "src");
        validateRange(dstMask.length, offset, length, "dstMask");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateSelectByte(byte[] mask, byte[] trueValues, byte[] falseValues, byte[] dst, int offset, int length) {
        validateNotNull(mask, "mask");
        validateNotNull(trueValues, "trueValues");
        validateNotNull(falseValues, "falseValues");
        validateNotNull(dst, "dst");
        validateRange(mask.length, offset, length, "mask");
        validateRange(trueValues.length, offset, length, "trueValues");
        validateRange(falseValues.length, offset, length, "falseValues");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateByteToInt(byte[] src, int[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateIntToByte(int[] src, byte[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validatePermuteByte(byte[] src, byte[] indices, byte[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(indices, "indices");
        validateNotNull(dst, "dst");
        validateRange(indices.length, offset, length, "indices");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateUnaryByte(byte[] src, byte[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateBinaryInt(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateUnaryInt(int[] src, int[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateMaskBinaryInt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dstMask, "dstMask");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dstMask.length, offset, length, "dstMask");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateSelectInt(byte[] mask, int[] trueValues, int[] falseValues, int[] dst, int offset, int length) {
        validateNotNull(mask, "mask");
        validateNotNull(trueValues, "trueValues");
        validateNotNull(falseValues, "falseValues");
        validateNotNull(dst, "dst");
        validateRange(mask.length, offset, length, "mask");
        validateRange(trueValues.length, offset, length, "trueValues");
        validateRange(falseValues.length, offset, length, "falseValues");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateReductionInt(int[] src, int offset, int length) {
        validateNotNull(src, "src");
        validateRange(src.length, offset, length, "src");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateDotInt(int[] srcA, int[] srcB, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateBinaryFloat(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateUnaryFloat(float[] src, float[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateReductionFloat(float[] src, int offset, int length) {
        validateNotNull(src, "src");
        validateRange(src.length, offset, length, "src");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateDotFloat(float[] srcA, float[] srcB, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateNotNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " is null");
        }
    }

    /// This API is used internally to verify valid array arguments in the simulator
    /// notice that no validation occurs on the devices.
    protected final void validateRange(int arrayLength, int offset, int length, String name) {
        if (offset < 0 || length < 0 || offset > arrayLength || arrayLength - offset < length) {
            throw new ArrayIndexOutOfBoundsException(name + " invalid range offset=" + offset + " length=" + length + " size=" + arrayLength);
        }
    }

    private byte clampByte(int value) {
        if (value > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        }
        if (value < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        }
        return (byte) value;
    }
}
