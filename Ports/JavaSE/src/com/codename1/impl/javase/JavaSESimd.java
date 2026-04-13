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
    public void or(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateBinaryByte(srcA, srcB, dst, offset, length);
        validateRegistered(srcA, srcB, dst);
        super.or(srcA, srcB, dst, offset, length);
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
    public void cmpLt(byte[] srcA, byte[] srcB, byte[] dstMask, int offset, int length) {
        validateMaskBinaryByte(srcA, srcB, dstMask, offset, length);
        validateRegistered(srcA, srcB, dstMask);
        super.cmpLt(srcA, srcB, dstMask, offset, length);
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
    public int base64Encode(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset) {
        validateRegistered(src, dst);
        return super.base64Encode(src, srcOffset, srcLen, dst, dstOffset);
    }

    @Override
    public int base64Decode(byte[] src, int srcOffset, int srcLen, byte[] dst, int dstOffset) {
        validateRegistered(src, dst);
        return super.base64Decode(src, srcOffset, srcLen, dst, dstOffset);
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
