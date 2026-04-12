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

    protected final void validateBinaryByte(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length) {
        validateNotNull(srcA, "srcA");
        validateNotNull(srcB, "srcB");
        validateNotNull(dst, "dst");
        validateRange(srcA.length, offset, length, "srcA");
        validateRange(srcB.length, offset, length, "srcB");
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
