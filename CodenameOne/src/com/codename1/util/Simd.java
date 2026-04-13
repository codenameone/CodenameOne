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

import com.codename1.ui.CN;

/**
 * Portable SIMD API with Java fallback implementations.
 */
public class Simd {

    public static Simd get() {
        return CN.getSimd();
    }

    public boolean isSupported() {
        return false;
    }

    public byte[] allocByte(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return new byte[size];
    }

    public int[] allocInt(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return new int[size];
    }

    public float[] allocFloat(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return new float[size];
    }

    public void add(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(srcA[i] + srcB[i]);
        }
    }

    public void sub(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(srcA[i] - srcB[i]);
        }
    }

    public void mul(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(srcA[i] * srcB[i]);
        }
    }

    public void min(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] < srcB[i] ? srcA[i] : srcB[i];
        }
    }

    public void max(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] > srcB[i] ? srcA[i] : srcB[i];
        }
    }

    public void abs(byte[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            if (v == Byte.MIN_VALUE) {
                dst[i] = Byte.MAX_VALUE;
            } else {
                dst[i] = (byte)Math.abs(v);
            }
        }
    }

    public void clamp(byte[] src, byte[] dst, byte minValue, byte maxValue, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            if (v < minValue) {
                dst[i] = minValue;
            } else if (v > maxValue) {
                dst[i] = maxValue;
            } else {
                dst[i] = (byte)v;
            }
        }
    }

    public void and(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte)(srcA[i] & srcB[i]);
        }
    }

    public void or(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte)(srcA[i] | srcB[i]);
        }
    }

    public void xor(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte)(srcA[i] ^ srcB[i]);
        }
    }

    public void not(byte[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte)(~src[i]);
        }
    }

    public void cmpEq(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] == srcB[i] ? (byte)-1 : (byte)0;
        }
    }

    public void cmpLt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] < srcB[i] ? (byte)-1 : (byte)0;
        }
    }

    public void cmpGt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] > srcB[i] ? (byte)-1 : (byte)0;
        }
    }

    public void cmpRange(byte[] src, byte minValue, byte maxValue, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            dstMask[i] = v >= minValue && v <= maxValue ? (byte)-1 : (byte)0;
        }
    }

    public void select(byte[] mask, byte[] trueValues, byte[] falseValues, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = mask[i] != 0 ? trueValues[i] : falseValues[i];
        }
    }

    public void unpackUnsignedByteToInt(byte[] src, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] & 0xff;
        }
    }

    public void packIntToByteSaturating(int[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = clampByte(src[i]);
        }
    }

    public void packIntToByteTruncate(int[] src, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = (byte)src[i];
        }
    }

    public void packIntToByteTruncate(int[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = (byte)src[srcOffset + i];
        }
    }

    public void permuteBytes(byte[] src, byte[] indices, byte[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int idx = indices[i];
            dst[i] = idx >= 0 && idx < src.length ? src[idx] : 0;
        }
    }

    public void add(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] + srcB[i];
        }
    }

    public void sub(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] - srcB[i];
        }
    }

    public void mul(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] * srcB[i];
        }
    }

    public void min(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] < srcB[i] ? srcA[i] : srcB[i];
        }
    }

    public void max(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] > srcB[i] ? srcA[i] : srcB[i];
        }
    }

    public void abs(int[] src, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            int v = src[i];
            dst[i] = v == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(v);
        }
    }

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

    public void and(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] & srcB[i];
        }
    }

    public void and(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = srcA[srcAOffset + i] & srcB[srcBOffset + i];
        }
    }

    public void or(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] | srcB[i];
        }
    }

    public void or(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = srcA[srcAOffset + i] | srcB[srcBOffset + i];
        }
    }

    public void xor(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] ^ srcB[i];
        }
    }

    public void not(int[] src, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = ~src[i];
        }
    }

    public void shl(int[] src, int bits, int[] dst, int offset, int length) {
        int shift = bits & 31;
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] << shift;
        }
    }

    public void shl(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length) {
        int shift = bits & 31;
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] << shift;
        }
    }

    public void shrLogical(int[] src, int bits, int[] dst, int offset, int length) {
        int shift = bits & 31;
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] >>> shift;
        }
    }

    public void shrLogical(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length) {
        int shift = bits & 31;
        for (int i = 0; i < length; i++) {
            dst[dstOffset + i] = src[srcOffset + i] >>> shift;
        }
    }

    public void shrArithmetic(int[] src, int bits, int[] dst, int offset, int length) {
        int shift = bits & 31;
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = src[i] >> shift;
        }
    }

    public void cmpEq(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] == srcB[i] ? (byte)-1 : (byte)0;
        }
    }

    public void cmpLt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] < srcB[i] ? (byte)-1 : (byte)0;
        }
    }

    public void cmpGt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dstMask[i] = srcA[i] > srcB[i] ? (byte)-1 : (byte)0;
        }
    }

    public void select(byte[] mask, int[] trueValues, int[] falseValues, int[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = mask[i] != 0 ? trueValues[i] : falseValues[i];
        }
    }

    public int sum(int[] src, int offset, int length) {
        int out = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += src[i];
        }
        return out;
    }

    public int dot(int[] srcA, int[] srcB, int offset, int length) {
        int out = 0;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += srcA[i] * srcB[i];
        }
        return out;
    }

    public void add(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] + srcB[i];
        }
    }

    public void sub(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] - srcB[i];
        }
    }

    public void mul(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = srcA[i] * srcB[i];
        }
    }

    public void min(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = Math.min(srcA[i], srcB[i]);
        }
    }

    public void max(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = Math.max(srcA[i], srcB[i]);
        }
    }

    public void abs(float[] src, float[] dst, int offset, int length) {
        for (int i = offset, end = offset + length; i < end; i++) {
            dst[i] = Math.abs(src[i]);
        }
    }

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

    public float sum(float[] src, int offset, int length) {
        float out = 0f;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += src[i];
        }
        return out;
    }

    public float dot(float[] srcA, float[] srcB, int offset, int length) {
        float out = 0f;
        for (int i = offset, end = offset + length; i < end; i++) {
            out += srcA[i] * srcB[i];
        }
        return out;
    }

    /**
     * Encodes binary data to Base64 without line breaks.
     *
     * <p>Both {@code src} and {@code dst} must be allocated with
     * {@link #allocByte(int)}.  The destination must have room for at least
     * {@code ((srcLen + 2) / 3) * 4} bytes starting at {@code dstOffset}.</p>
     *
     * @param src       source bytes
     * @param srcOffset offset into {@code src}
     * @param srcLen    number of bytes to encode
     * @param dst       destination buffer for Base64 output
     * @param dstOffset offset into {@code dst}
     * @return number of Base64 bytes written
     */
    public int base64Encode(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset) {
        return Base64.encodeNoNewline(src, srcOffset, srcLen, dst, dstOffset);
    }

    /**
     * Decodes Base64 data (no whitespace) to binary.
     *
     * <p>Both {@code src} and {@code dst} must be allocated with
     * {@link #allocByte(int)}.  {@code srcLen} must be a multiple of 4.
     * The destination must have room for at least
     * {@code (srcLen / 4) * 3} bytes starting at {@code dstOffset}.</p>
     *
     * @param src       Base64 bytes (no whitespace)
     * @param srcOffset offset into {@code src}
     * @param srcLen    number of bytes to decode (must be multiple of 4)
     * @param dst       destination buffer for decoded output
     * @param dstOffset offset into {@code dst}
     * @return number of decoded bytes written, or -1 for invalid input
     */
    public int base64Decode(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset) {
        return Base64.decodeNoWhitespace(src, srcOffset, srcLen, dst, dstOffset);
    }

    protected final void validateBinaryByte(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateMaskBinaryByte(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dstMask, "dstMask");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dstMask.length, offset, length, "dstMask");
    }

    protected final void validateRangeMaskByte(byte[] src, byte[] dstMask, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dstMask, "dstMask");
        validateRange(src.length, offset, length, "src");
        validateRange(dstMask.length, offset, length, "dstMask");
    }

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

    protected final void validateByteToInt(byte[] src, int[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateIntToByte(int[] src, byte[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validatePermuteByte(byte[] src, byte[] indices, byte[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(indices, "indices");
        validateNotNull(dst, "dst");
        validateRange(indices.length, offset, length, "indices");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateUnaryByte(byte[] src, byte[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateBinaryInt(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateUnaryInt(int[] src, int[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateMaskBinaryInt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dstMask, "dstMask");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dstMask.length, offset, length, "dstMask");
    }

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

    protected final void validateReductionInt(int[] src, int offset, int length) {
        validateNotNull(src, "src");
        validateRange(src.length, offset, length, "src");
    }

    protected final void validateDotInt(int[] srcA, int[] srcB, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
    }

    protected final void validateBinaryFloat(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateUnaryFloat(float[] src, float[] dst, int offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, offset, length, "src");
        validateRange(dst.length, offset, length, "dst");
    }

    protected final void validateReductionFloat(float[] src, int offset, int length) {
        validateNotNull(src, "src");
        validateRange(src.length, offset, length, "src");
    }

    protected final void validateDotFloat(float[] srcA, float[] srcB, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
    }


    protected final void validateNotNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " is null");
        }
    }

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
        return (byte)value;
    }
}
