/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.impl.ios;

import com.codename1.util.Simd;

/**
 * iOS SIMD implementation backed by NEON wrappers.
 */
public class IOSSimd extends Simd {
    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public byte[] allocByte(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return allocByteNative(size);
    }

    @Override
    public int[] allocInt(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return allocIntNative(size);
    }

    @Override
    public float[] allocFloat(int size) {
        if (size < 16) {
            throw new IllegalArgumentException("size must be >= 16");
        }
        return allocFloatNative(size);
    }

    @Override
    public native void lookupBytes(byte[] table, byte[] indices, byte[] dst, int offset, int length);

    @Override
    public native void lookupBytes(byte[] table, byte[] indices, int indicesOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void add(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void sub(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void mul(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void min(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void max(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void abs(byte[] src, byte[] dst, int offset, int length);

    @Override
    public native void clamp(byte[] src, byte[] dst, byte minValue, byte maxValue, int offset, int length);

    @Override
    public native void and(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void and(byte[] srcA, int srcAOffset, byte[] srcB, int srcBOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void or(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void or(byte[] srcA, int srcAOffset, byte[] srcB, int srcBOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void xor(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void not(byte[] src, byte[] dst, int offset, int length);

    @Override
    public native void cmpEq(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpEq(byte[] src, byte value, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpLt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpLt(byte[] src, byte value, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpGt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpRange(byte[] src, byte minValue, byte maxValue, byte[] dstMask, int offset, int length);

    @Override
    public native void select(byte[] mask, byte[] trueValues, byte[] falseValues, byte[] dst, int offset, int length);

    @Override
    public native void unpackUnsignedByteToInt(byte[] src, int[] dst, int offset, int length);

    @Override
    public native void packIntToByteSaturating(int[] src, byte[] dst, int offset, int length);

    @Override
    public native void packIntToByteTruncate(int[] src, byte[] dst, int offset, int length);

    @Override
    public native void packIntToByteTruncate(int[] src, int srcOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void permuteBytes(byte[] src, byte[] indices, byte[] dst, int offset, int length);

    @Override
    public native void add(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void sub(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void mul(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void min(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void max(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void abs(int[] src, int[] dst, int offset, int length);

    @Override
    public native void clamp(int[] src, int[] dst, int minValue, int maxValue, int offset, int length);

    @Override
    public native void and(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void and(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void or(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void or(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void xor(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void not(int[] src, int[] dst, int offset, int length);

    @Override
    public native void shl(int[] src, int bits, int[] dst, int offset, int length);

    @Override
    public native void shl(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length);

    @Override
    public native void shrLogical(int[] src, int bits, int[] dst, int offset, int length);

    @Override
    public native void shrLogical(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length);

    @Override
    public native void shrArithmetic(int[] src, int bits, int[] dst, int offset, int length);

    @Override
    public native void cmpEq(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpLt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length);

    @Override
    public native void cmpGt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length);

    @Override
    public native void select(byte[] mask, int[] trueValues, int[] falseValues, int[] dst, int offset, int length);

    @Override
    public native int sum(int[] src, int offset, int length);

    @Override
    public native int dot(int[] srcA, int[] srcB, int offset, int length);

    @Override
    public native void add(float[] srcA, float[] srcB, float[] dst, int offset, int length);

    @Override
    public native void sub(float[] srcA, float[] srcB, float[] dst, int offset, int length);

    @Override
    public native void mul(float[] srcA, float[] srcB, float[] dst, int offset, int length);

    @Override
    public native void min(float[] srcA, float[] srcB, float[] dst, int offset, int length);

    @Override
    public native void max(float[] srcA, float[] srcB, float[] dst, int offset, int length);

    @Override
    public native void abs(float[] src, float[] dst, int offset, int length);

    @Override
    public native void clamp(float[] src, float[] dst, float minValue, float maxValue, int offset, int length);

    @Override
    public native float sum(float[] src, int offset, int length);

    @Override
    public native float dot(float[] srcA, float[] srcB, int offset, int length);

    @Override
    public native void shl(byte[] src, int bits, byte[] dst, int offset, int length);

    @Override
    public native void shl(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length);

    @Override
    public native void shrLogical(byte[] src, int bits, byte[] dst, int offset, int length);

    @Override
    public native void shrLogical(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length);

    @Override
    public native void addWrapping(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void addWrapping(byte[] src, byte value, byte[] dst, int offset, int length);

    @Override
    public native void subWrapping(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void subWrapping(byte[] src, byte value, byte[] dst, int offset, int length);

    @Override
    public native void unpackUnsignedByteToInt(byte[] src, int srcOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void unpackUnsignedByteToIntInterleaved3(byte[] src, int srcOffset, int[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length);

    @Override
    public native void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, int length);

    @Override
    public native void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length);

    @Override
    public native void unpackBytesInterleaved4(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, byte[] dst3, int length);

    @Override
    public native void unpackBytesInterleaved4(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length);

    @Override
    public native int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, byte[] dst3, int length);

    @Override
    public native int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length);

    @Override
    public native void add(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void packIntToByteTruncateInterleaved4(int[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved3(byte[] src0, byte[] src1, byte[] src2, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved3(byte[] src, int src0Offset, int src1Offset, int src2Offset, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved4(byte[] src0, byte[] src1, byte[] src2, byte[] src3, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved4(byte[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length);

    @Override
    public native void cmpEq(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, byte[] dstMask, int dstOffset, int length);

    @Override
    public native void cmpLt(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, byte[] dstMask, int dstOffset, int length);

    @Override
    public native void select(byte[] mask, int maskOffset, int[] trueValues, int trueOffset, int[] falseValues, int falseOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void and(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length);

    @Override
    public native void or(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length);

    @Override
    public native void xor(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length);

    @Override
    public native void cmpEq(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length);

    @Override
    public native void cmpLt(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length);

    @Override
    public native void cmpGt(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length);

    @Override
    public native void not(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void select(byte[] mask, int maskOffset, int trueConstant, int falseConstant, int[] dst, int dstOffset, int length);

    @Override
    public native void select(byte[] mask, int maskOffset, int[] trueValues, int trueOffset, int falseConstant, int[] dst, int dstOffset, int length);

    @Override
    public native void select(byte[] mask, int maskOffset, int trueConstant, int[] falseValues, int falseOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void blendByMaskTestNonzero(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int[] dst, int dstOffset, int length);

    @Override
    public native void blendByMaskTestNonzeroSubstituteOnKeepEq(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int removeMatch, int removeValue, int[] dst, int dstOffset, int length);

    @Override
    public native void replaceTopByteFromUnsignedBytes(int[] rgbSrc, int rgbSrcOffset, byte[] alphaSrc, int alphaSrcOffset, int[] dst, int dstOffset, int length);

    private native byte[] allocByteNative(int size);
    private native int[] allocIntNative(int size);
    private native float[] allocFloatNative(int size);
}
