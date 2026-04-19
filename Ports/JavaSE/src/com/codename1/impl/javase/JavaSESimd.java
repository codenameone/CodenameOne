/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.impl.javase;

import com.codename1.util.Simd;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JavaSE SIMD implementation used for simulator validation and fallback execution.
 */
public class JavaSESimd extends Simd {
    private final Set<Integer> allocatedIds = Collections.synchronizedSet(new HashSet<Integer>());

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public byte[] allocByte(int size) {
        byte[] out = super.allocByte(size);
        allocatedIds.add(Integer.valueOf(System.identityHashCode(out)));
        return out;
    }

    @Override
    public int[] allocInt(int size) {
        int[] out = super.allocInt(size);
        allocatedIds.add(Integer.valueOf(System.identityHashCode(out)));
        return out;
    }

    @Override
    public float[] allocFloat(int size) {
        float[] out = super.allocFloat(size);
        allocatedIds.add(Integer.valueOf(System.identityHashCode(out)));
        return out;
    }

    @Override
    public void lookupBytes(byte[] table, byte[] indices, int indicesOffset, byte[] dst, int dstOffset, int length) {
        validateNotNull(table, "table");
        validateNotNull(indices, "indices");
        validateNotNull(dst, "dst");
        validateRange(indices.length, indicesOffset, length, "indices");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(table, indices, dst);
        super.lookupBytes(table, indices, indicesOffset, dst, dstOffset, length);
    }

    @Override
    public void add(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.add(srcA, srcB, dst, offset, length);
    }

    @Override
    public void sub(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.sub(srcA, srcB, dst, offset, length);
    }

    @Override
    public void mul(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.mul(srcA, srcB, dst, offset, length);
    }

    @Override
    public void min(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.min(srcA, srcB, dst, offset, length);
    }

    @Override
    public void max(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.max(srcA, srcB, dst, offset, length);
    }

    @Override
    public void abs(byte[] src, byte[] dst, int offset, int length) {
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.abs(src, dst, offset, length);
    }

    @Override
    public void clamp(byte[] src, byte[] dst, byte minValue, byte maxValue, int offset, int length) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue > maxValue");
        }
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.clamp(src, dst, minValue, maxValue, offset, length);
    }

    @Override
    public void and(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.and(srcA, srcB, dst, offset, length);
    }

    @Override
    public void and(byte[] srcA, int srcAOffset, byte[] srcB, int srcBOffset, byte[] dst, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(srcA, srcB, dst);
        super.and(srcA, srcAOffset, srcB, srcBOffset, dst, dstOffset, length);
    }

    @Override
    public void or(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.or(srcA, srcB, dst, offset, length);
    }

    @Override
    public void or(byte[] srcA, int srcAOffset, byte[] srcB, int srcBOffset, byte[] dst, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(srcA, srcB, dst);
        super.or(srcA, srcAOffset, srcB, srcBOffset, dst, dstOffset, length);
    }

    @Override
    public void xor(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.xor(srcA, srcB, dst, offset, length);
    }

    @Override
    public void not(byte[] src, byte[] dst, int offset, int length) {
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.not(src, dst, offset, length);
    }

    @Override
    public void cmpEq(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryByte(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpEq(srcA, srcB, dstMask, offset, length);
    }

    @Override
    public void cmpEq(byte[] src, byte value, byte[] dstMask, int offset, int length) {
        validateRangeMaskByte(src, dstMask, offset, length);
        validateRegistered(src, dstMask);
        super.cmpEq(src, value, dstMask, offset, length);
    }

    @Override
    public void cmpLt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryByte(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpLt(srcA, srcB, dstMask, offset, length);
    }

    @Override
    public void cmpLt(byte[] src, byte value, byte[] dstMask, int offset, int length) {
        validateRangeMaskByte(src, dstMask, offset, length);
        validateRegistered(src, dstMask);
        super.cmpLt(src, value, dstMask, offset, length);
    }

    @Override
    public void cmpGt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryByte(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpGt(srcA, srcB, dstMask, offset, length);
    }

    @Override
    public void cmpRange(byte[] src, byte minValue, byte maxValue, byte[] dstMask, int offset, int length) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue > maxValue");
        }
        validateRangeMaskByte(src, dstMask, offset, length);
        validateRegistered(src, dstMask);
        super.cmpRange(src, minValue, maxValue, dstMask, offset, length);
    }

    @Override
    public void select(byte[] mask, byte[] trueValues, byte[] falseValues, byte[] dst, int offset, int length) {
        validateSelectByte(mask, trueValues, falseValues, dst, offset, length);
        validateRegistered(mask, trueValues, falseValues, dst);
        super.select(mask, trueValues, falseValues, dst, offset, length);
    }

    @Override
    public void unpackUnsignedByteToInt(byte[] src, int[] dst, int offset, int length) {
        validateByteToInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.unpackUnsignedByteToInt(src, dst, offset, length);
    }

    @Override
    public void packIntToByteSaturating(int[] src, byte[] dst, int offset, int length) {
        validateIntToByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.packIntToByteSaturating(src, dst, offset, length);
    }

    @Override
    public void packIntToByteTruncate(int[] src, byte[] dst, int offset, int length) {
        validateIntToByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.packIntToByteTruncate(src, dst, offset, length);
    }

    @Override
    public void packIntToByteTruncate(int[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.packIntToByteTruncate(src, srcOffset, dst, dstOffset, length);
    }

    @Override
    public void permuteBytes(byte[] src, byte[] indices, byte[] dst, int offset, int length) {
        validatePermuteByte(src, indices, dst, offset, length);
        validateRegistered(src, indices, dst);
        super.permuteBytes(src, indices, dst, offset, length);
    }

    @Override
    public void add(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.add(srcA, srcB, dst, offset, length);
    }

    @Override
    public void sub(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.sub(srcA, srcB, dst, offset, length);
    }

    @Override
    public void mul(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.mul(srcA, srcB, dst, offset, length);
    }

    @Override
    public void min(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.min(srcA, srcB, dst, offset, length);
    }

    @Override
    public void max(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.max(srcA, srcB, dst, offset, length);
    }

    @Override
    public void abs(int[] src, int[] dst, int offset, int length) {
        validateUnaryInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.abs(src, dst, offset, length);
    }

    @Override
    public void clamp(int[] src, int[] dst, int minValue, int maxValue, int offset, int length) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue > maxValue");
        }
        validateUnaryInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.clamp(src, dst, minValue, maxValue, offset, length);
    }

    @Override
    public void and(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.and(srcA, srcB, dst, offset, length);
    }

    @Override
    public void and(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(srcA, srcB, dst);
        super.and(srcA, srcAOffset, srcB, srcBOffset, dst, dstOffset, length);
    }

    @Override
    public void or(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.or(srcA, srcB, dst, offset, length);
    }

    @Override
    public void or(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(srcA, srcB, dst);
        super.or(srcA, srcAOffset, srcB, srcBOffset, dst, dstOffset, length);
    }

    @Override
    public void xor(int[] srcA, int[] srcB, int[] dst, int offset, int length) {
        validateBinaryInt(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.xor(srcA, srcB, dst, offset, length);
    }

    @Override
    public void not(int[] src, int[] dst, int offset, int length) {
        validateUnaryInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.not(src, dst, offset, length);
    }

    @Override
    public void shl(int[] src, int bits, int[] dst, int offset, int length) {
        validateUnaryInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.shl(src, bits, dst, offset, length);
    }

    @Override
    public void shl(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.shl(src, srcOffset, bits, dst, dstOffset, length);
    }

    @Override
    public void shrLogical(int[] src, int bits, int[] dst, int offset, int length) {
        validateUnaryInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.shrLogical(src, bits, dst, offset, length);
    }

    @Override
    public void shrLogical(int[] src, int srcOffset, int bits, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.shrLogical(src, srcOffset, bits, dst, dstOffset, length);
    }

    @Override
    public void shrArithmetic(int[] src, int bits, int[] dst, int offset, int length) {
        validateUnaryInt(src, dst, offset, length);
        validateRegistered(src, dst);
        super.shrArithmetic(src, bits, dst, offset, length);
    }

    @Override
    public void cmpEq(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryInt(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpEq(srcA, srcB, dstMask, offset, length);
    }

    @Override
    public void cmpLt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryInt(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpLt(srcA, srcB, dstMask, offset, length);
    }

    @Override
    public void cmpGt(int[] srcA, int[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryInt(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpGt(srcA, srcB, dstMask, offset, length);
    }

    @Override
    public void select(byte[] mask, int[] trueValues, int[] falseValues, int[] dst, int offset, int length) {
        validateSelectInt(mask, trueValues, falseValues, dst, offset, length);
        validateRegistered(mask, trueValues, falseValues, dst);
        super.select(mask, trueValues, falseValues, dst, offset, length);
    }

    @Override
    public int sum(int[] src, int offset, int length) {
        validateReductionInt(src, offset, length);
        validateRegistered(src);
        return super.sum(src, offset, length);
    }

    @Override
    public int dot(int[] srcA, int[] srcB, int offset, int length) {
        validateDotInt(srcA, srcB, offset, length);
        validateRegistered(srcA, srcB);
        return super.dot(srcA, srcB, offset, length);
    }

    @Override
    public void add(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateBinaryFloat(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.add(srcA, srcB, dst, offset, length);
    }

    @Override
    public void sub(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateBinaryFloat(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.sub(srcA, srcB, dst, offset, length);
    }

    @Override
    public void mul(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateBinaryFloat(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.mul(srcA, srcB, dst, offset, length);
    }

    @Override
    public void min(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateBinaryFloat(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.min(srcA, srcB, dst, offset, length);
    }

    @Override
    public void max(float[] srcA, float[] srcB, float[] dst, int offset, int length) {
        validateBinaryFloat(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.max(srcA, srcB, dst, offset, length);
    }

    @Override
    public void abs(float[] src, float[] dst, int offset, int length) {
        validateUnaryFloat(src, dst, offset, length);
        validateRegistered(src, dst);
        super.abs(src, dst, offset, length);
    }

    @Override
    public void clamp(float[] src, float[] dst, float minValue, float maxValue, int offset, int length) {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue > maxValue");
        }
        validateUnaryFloat(src, dst, offset, length);
        validateRegistered(src, dst);
        super.clamp(src, dst, minValue, maxValue, offset, length);
    }

    @Override
    public float sum(float[] src, int offset, int length) {
        validateReductionFloat(src, offset, length);
        validateRegistered(src);
        return super.sum(src, offset, length);
    }

    @Override
    public float dot(float[] srcA, float[] srcB, int offset, int length) {
        validateDotFloat(srcA, srcB, offset, length);
        validateRegistered(srcA, srcB);
        return super.dot(srcA, srcB, offset, length);
    }

    @Override
    public void shl(byte[] src, int bits, byte[] dst, int offset, int length) {
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.shl(src, bits, dst, offset, length);
    }

    @Override
    public void shl(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.shl(src, srcOffset, bits, dst, dstOffset, length);
    }

    @Override
    public void shrLogical(byte[] src, int bits, byte[] dst, int offset, int length) {
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.shrLogical(src, bits, dst, offset, length);
    }

    @Override
    public void shrLogical(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.shrLogical(src, srcOffset, bits, dst, dstOffset, length);
    }

    @Override
    public void addWrapping(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.addWrapping(srcA, srcB, dst, offset, length);
    }

    @Override
    public void addWrapping(byte[] src, byte value, byte[] dst, int offset, int length) {
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.addWrapping(src, value, dst, offset, length);
    }

    @Override
    public void subWrapping(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.subWrapping(srcA, srcB, dst, offset, length);
    }

    @Override
    public void subWrapping(byte[] src, byte value, byte[] dst, int offset, int length) {
        validateUnaryByte(src, dst, offset, length);
        validateRegistered(src, dst);
        super.subWrapping(src, value, dst, offset, length);
    }

    @Override
    public void unpackUnsignedByteToInt(byte[] src, int srcOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.unpackUnsignedByteToInt(src, srcOffset, dst, dstOffset, length);
    }

    @Override
    public void unpackUnsignedByteToIntInterleaved3(byte[] src, int srcOffset, int[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length * 3, "src");
        validateRange(dst.length, dst0Offset, length, "dst");
        validateRange(dst.length, dst1Offset, length, "dst");
        validateRange(dst.length, dst2Offset, length, "dst");
        validateRegistered(src, dst);
        super.unpackUnsignedByteToIntInterleaved3(src, srcOffset, dst, dst0Offset, dst1Offset, dst2Offset, length);
    }

    @Override
    public void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst0, "dst0");
        validateNotNull(dst1, "dst1");
        validateNotNull(dst2, "dst2");
        validateRange(src.length, srcOffset, length * 3, "src");
        validateRange(dst0.length, 0, length, "dst0");
        validateRange(dst1.length, 0, length, "dst1");
        validateRange(dst2.length, 0, length, "dst2");
        validateRegistered(src, dst0, dst1, dst2);
        super.unpackBytesInterleaved3(src, srcOffset, dst0, dst1, dst2, length);
    }

    @Override
    public void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length * 3, "src");
        validateRange(dst.length, dst0Offset, length, "dst");
        validateRange(dst.length, dst1Offset, length, "dst");
        validateRange(dst.length, dst2Offset, length, "dst");
        validateRegistered(src, dst);
        super.unpackBytesInterleaved3(src, srcOffset, dst, dst0Offset, dst1Offset, dst2Offset, length);
    }

    @Override
    public void unpackBytesInterleaved4(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, byte[] dst3, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst0, "dst0");
        validateNotNull(dst1, "dst1");
        validateNotNull(dst2, "dst2");
        validateNotNull(dst3, "dst3");
        validateRange(src.length, srcOffset, length * 4, "src");
        validateRange(dst0.length, 0, length, "dst0");
        validateRange(dst1.length, 0, length, "dst1");
        validateRange(dst2.length, 0, length, "dst2");
        validateRange(dst3.length, 0, length, "dst3");
        validateRegistered(src, dst0, dst1, dst2, dst3);
        super.unpackBytesInterleaved4(src, srcOffset, dst0, dst1, dst2, dst3, length);
    }

    @Override
    public void unpackBytesInterleaved4(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length * 4, "src");
        validateRange(dst.length, dst0Offset, length, "dst");
        validateRange(dst.length, dst1Offset, length, "dst");
        validateRange(dst.length, dst2Offset, length, "dst");
        validateRange(dst.length, dst3Offset, length, "dst");
        validateRegistered(src, dst);
        super.unpackBytesInterleaved4(src, srcOffset, dst, dst0Offset, dst1Offset, dst2Offset, dst3Offset, length);
    }

    @Override
    public int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length) {
        validateNotNull(table, "table");
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length * 4, "src");
        validateRange(dst.length, dst0Offset, length, "dst");
        validateRange(dst.length, dst1Offset, length, "dst");
        validateRange(dst.length, dst2Offset, length, "dst");
        validateRange(dst.length, dst3Offset, length, "dst");
        validateRegistered(table, src, dst);
        return super.unpackLookupBytesInterleaved4(table, src, srcOffset, dst, dst0Offset, dst1Offset, dst2Offset, dst3Offset, length);
    }

    @Override
    public void add(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(srcA, srcB, dst);
        super.add(srcA, srcAOffset, srcB, srcBOffset, dst, dstOffset, length);
    }

    @Override
    public void cmpEq(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, byte[] dstMask, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dstMask, "dstMask");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dstMask.length, dstOffset, length, "dstMask");
        validateRegistered(srcA, srcB, dstMask);
        super.cmpEq(srcA, srcAOffset, srcB, srcBOffset, dstMask, dstOffset, length);
    }

    @Override
    public void cmpLt(int[] srcA, int srcAOffset, int[] srcB, int srcBOffset, byte[] dstMask, int dstOffset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dstMask, "dstMask");
        validateRange(srcA.length, srcAOffset, length, "srcA");
        validateRange(srcB.length, srcBOffset, length, "srcB");
        validateRange(dstMask.length, dstOffset, length, "dstMask");
        validateRegistered(srcA, srcB, dstMask);
        super.cmpLt(srcA, srcAOffset, srcB, srcBOffset, dstMask, dstOffset, length);
    }

    @Override
    public void select(byte[] mask, int maskOffset, int[] trueValues, int trueOffset, int[] falseValues, int falseOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(mask, "mask");
        validateNotNull(trueValues, "trueValues");
        validateNotNull(falseValues, "falseValues");
        validateNotNull(dst, "dst");
        validateRange(mask.length, maskOffset, length, "mask");
        validateRange(trueValues.length, trueOffset, length, "trueValues");
        validateRange(falseValues.length, falseOffset, length, "falseValues");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(mask, trueValues, falseValues, dst);
        super.select(mask, maskOffset, trueValues, trueOffset, falseValues, falseOffset, dst, dstOffset, length);
    }

    @Override
    public void packIntToByteTruncateInterleaved4(int[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, src0Offset, length, "src");
        validateRange(src.length, src1Offset, length, "src");
        validateRange(src.length, src2Offset, length, "src");
        validateRange(src.length, src3Offset, length, "src");
        validateRange(dst.length, dstOffset, length * 4, "dst");
        validateRegistered(src, dst);
        super.packIntToByteTruncateInterleaved4(src, src0Offset, src1Offset, src2Offset, src3Offset, dst, dstOffset, length);
    }

    @Override
    public void packBytesInterleaved3(byte[] src0, byte[] src1, byte[] src2, byte[] dst, int dstOffset, int length) {
        validateNotNull(src0, "src0");
        validateNotNull(src1, "src1");
        validateNotNull(src2, "src2");
        validateNotNull(dst, "dst");
        validateRange(src0.length, 0, length, "src0");
        validateRange(src1.length, 0, length, "src1");
        validateRange(src2.length, 0, length, "src2");
        validateRange(dst.length, dstOffset, length * 3, "dst");
        validateRegistered(src0, src1, src2, dst);
        super.packBytesInterleaved3(src0, src1, src2, dst, dstOffset, length);
    }

    @Override
    public void packBytesInterleaved3(byte[] src, int src0Offset, int src1Offset, int src2Offset, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, src0Offset, length, "src");
        validateRange(src.length, src1Offset, length, "src");
        validateRange(src.length, src2Offset, length, "src");
        validateRange(dst.length, dstOffset, length * 3, "dst");
        validateRegistered(src, dst);
        super.packBytesInterleaved3(src, src0Offset, src1Offset, src2Offset, dst, dstOffset, length);
    }

    @Override
    public void packBytesInterleaved4(byte[] src0, byte[] src1, byte[] src2, byte[] src3, byte[] dst, int dstOffset, int length) {
        validateNotNull(src0, "src0");
        validateNotNull(src1, "src1");
        validateNotNull(src2, "src2");
        validateNotNull(src3, "src3");
        validateNotNull(dst, "dst");
        validateRange(src0.length, 0, length, "src0");
        validateRange(src1.length, 0, length, "src1");
        validateRange(src2.length, 0, length, "src2");
        validateRange(src3.length, 0, length, "src3");
        validateRange(dst.length, dstOffset, length * 4, "dst");
        validateRegistered(src0, src1, src2, src3, dst);
        super.packBytesInterleaved4(src0, src1, src2, src3, dst, dstOffset, length);
    }

    @Override
    public void packBytesInterleaved4(byte[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, src0Offset, length, "src");
        validateRange(src.length, src1Offset, length, "src");
        validateRange(src.length, src2Offset, length, "src");
        validateRange(src.length, src3Offset, length, "src");
        validateRange(dst.length, dstOffset, length * 4, "dst");
        validateRegistered(src, dst);
        super.packBytesInterleaved4(src, src0Offset, src1Offset, src2Offset, src3Offset, dst, dstOffset, length);
    }

    @Override
    public void and(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.and(src, srcOffset, constant, dst, dstOffset, length);
    }

    @Override
    public void or(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.or(src, srcOffset, constant, dst, dstOffset, length);
    }

    @Override
    public void xor(int[] src, int srcOffset, int constant, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.xor(src, srcOffset, constant, dst, dstOffset, length);
    }

    @Override
    public void cmpEq(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dstMask, "dstMask");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dstMask.length, dstOffset, length, "dstMask");
        validateRegistered(src, dstMask);
        super.cmpEq(src, srcOffset, constant, dstMask, dstOffset, length);
    }

    @Override
    public void cmpLt(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dstMask, "dstMask");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dstMask.length, dstOffset, length, "dstMask");
        validateRegistered(src, dstMask);
        super.cmpLt(src, srcOffset, constant, dstMask, dstOffset, length);
    }

    @Override
    public void cmpGt(int[] src, int srcOffset, int constant, byte[] dstMask, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dstMask, "dstMask");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dstMask.length, dstOffset, length, "dstMask");
        validateRegistered(src, dstMask);
        super.cmpGt(src, srcOffset, constant, dstMask, dstOffset, length);
    }

    @Override
    public void not(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.not(src, srcOffset, dst, dstOffset, length);
    }

    @Override
    public void select(byte[] mask, int maskOffset, int trueConstant, int falseConstant, int[] dst, int dstOffset, int length) {
        validateNotNull(mask, "mask");
        validateNotNull(dst, "dst");
        validateRange(mask.length, maskOffset, length, "mask");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(mask, dst);
        super.select(mask, maskOffset, trueConstant, falseConstant, dst, dstOffset, length);
    }

    @Override
    public void select(byte[] mask, int maskOffset, int[] trueValues, int trueOffset, int falseConstant, int[] dst, int dstOffset, int length) {
        validateNotNull(mask, "mask");
        validateNotNull(trueValues, "trueValues");
        validateNotNull(dst, "dst");
        validateRange(mask.length, maskOffset, length, "mask");
        validateRange(trueValues.length, trueOffset, length, "trueValues");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(mask, trueValues, dst);
        super.select(mask, maskOffset, trueValues, trueOffset, falseConstant, dst, dstOffset, length);
    }

    @Override
    public void select(byte[] mask, int maskOffset, int trueConstant, int[] falseValues, int falseOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(mask, "mask");
        validateNotNull(falseValues, "falseValues");
        validateNotNull(dst, "dst");
        validateRange(mask.length, maskOffset, length, "mask");
        validateRange(falseValues.length, falseOffset, length, "falseValues");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(mask, falseValues, dst);
        super.select(mask, maskOffset, trueConstant, falseValues, falseOffset, dst, dstOffset, length);
    }

    @Override
    public void blendByMaskTestNonzero(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.blendByMaskTestNonzero(src, srcOffset, testMask, trueKeepMask, trueOrValue, dst, dstOffset, length);
    }

    @Override
    public void blendByMaskTestNonzeroSubstituteOnKeepEq(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int removeMatch, int removeValue, int[] dst, int dstOffset, int length) {
        validateNotNull(src, "src");
        validateNotNull(dst, "dst");
        validateRange(src.length, srcOffset, length, "src");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(src, dst);
        super.blendByMaskTestNonzeroSubstituteOnKeepEq(src, srcOffset, testMask, trueKeepMask, trueOrValue, removeMatch, removeValue, dst, dstOffset, length);
    }

    @Override
    public void replaceTopByteFromUnsignedBytes(int[] rgbSrc, int rgbSrcOffset, byte[] alphaSrc, int alphaSrcOffset, int[] dst, int dstOffset, int length) {
        validateNotNull(rgbSrc, "rgbSrc");
        validateNotNull(alphaSrc, "alphaSrc");
        validateNotNull(dst, "dst");
        validateRange(rgbSrc.length, rgbSrcOffset, length, "rgbSrc");
        validateRange(alphaSrc.length, alphaSrcOffset, length, "alphaSrc");
        validateRange(dst.length, dstOffset, length, "dst");
        validateRegistered(rgbSrc, alphaSrc, dst);
        super.replaceTopByteFromUnsignedBytes(rgbSrc, rgbSrcOffset, alphaSrc, alphaSrcOffset, dst, dstOffset, length);
    }

    private void validateRegistered(Object... arrays) {
        for (int i = 0; i < arrays.length; i++) {
            Object arr = arrays[i];
            Integer id = Integer.valueOf(System.identityHashCode(arr));
            if (!allocatedIds.contains(id)) {
                throw new IllegalArgumentException(
                        "SIMD array argument was not allocated using Simd.alloc*(). objectId=" + id.intValue());
            }
        }
    }
}
