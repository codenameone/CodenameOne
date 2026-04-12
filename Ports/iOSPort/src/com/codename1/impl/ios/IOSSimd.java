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

    private native byte[] allocByteNative(int size);
    private native int[] allocIntNative(int size);
    private native float[] allocFloatNative(int size);
}
