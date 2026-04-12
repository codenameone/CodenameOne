/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.impl.javase;

import com.codename1.ui.CN;
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

    private void validateRegistered(Object... arrays) {
        if (!CN.isSimulator()) {
            return;
        }
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
