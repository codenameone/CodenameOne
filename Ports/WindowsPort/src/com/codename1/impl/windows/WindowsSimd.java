/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.windows;

import com.codename1.util.Simd;

/**
 * Native Windows SIMD implementation, the x86/ARM analog of the iOS {@code
 * IOSSimd}. The hot-path vector operations are overridden with {@code native}
 * methods backed by SSE2 (x64) / NEON (arm64) intrinsics in
 * {@code nativeSources/cn1_windows_simd.c}; every other operation is inherited
 * from the portable {@link Simd} base (a correct scalar loop), so the API is
 * complete whether or not a given op is vectorized.
 *
 * <p>Unlike iOS this class does <em>not</em> override the {@code alloc*}
 * methods: the native kernels use unaligned loads/stores ({@code _mm_loadu_*} /
 * {@code vld1q_*}), so a plain {@code new int[]} works and there is no 16-byte
 * alignment requirement. {@link #isSupported()} returns {@code true} to signal
 * that native acceleration is present.</p>
 */
public class WindowsSimd extends Simd {
    @Override
    public boolean isSupported() {
        return true;
    }

    /// False on both Windows arches: x86-64 has no 3-way SSE2 interleave and its
    /// /O2 autovectorizer already matches scalar; arm64 has the NEON interleave but
    /// the codec's table lookups (unpackLookupBytesInterleaved4 / lookupBytes) are
    /// scalar here (iOS uses NEON vqtbl), so base64 DECODE loses even though encode
    /// wins -- net not a clear win. So keep the Base64 codec scalar on Windows;
    /// only iOS/Mac (full NEON, 75-83% faster) enable it. The fused image kernels
    /// are unaffected (gated on isSupported(), and they win on every arch).
    @Override
    public boolean isByteShuffleAccelerated() {
        return false;
    }

    /* ----------------------------------------------------------------- int */

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
    public native void and(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void or(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native void xor(int[] srcA, int[] srcB, int[] dst, int offset, int length);

    @Override
    public native int sum(int[] src, int offset, int length);

    @Override
    public native int dot(int[] srcA, int[] srcB, int offset, int length);

    /* --------------------------------------------------------------- float */

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
    public native float sum(float[] src, int offset, int length);

    @Override
    public native float dot(float[] srcA, float[] srcB, int offset, int length);

    /* ---------------------------------------------------------------- byte */

    @Override
    public native void add(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void sub(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void and(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void or(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    @Override
    public native void xor(byte[] srcA, byte[] srcB, byte[] dst, int offset, int length);

    /* ----------------------------------------------- fused image hot paths */

    @Override
    public native void replaceTopByteFromUnsignedBytes(int[] rgbSrc, int rgbSrcOffset, byte[] alphaSrc, int alphaSrcOffset, int[] dst, int dstOffset, int length);

    @Override
    public native void blendByMaskTestNonzero(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int[] dst, int dstOffset, int length);

    // Image.modifyAlpha(alpha, removeColor) -- the only image op still on the slow
    // scalar fallback; now a fused native blend like its sibling above.
    @Override
    public native void blendByMaskTestNonzeroSubstituteOnKeepEq(int[] src, int srcOffset, int testMask, int trueKeepMask, int trueOrValue, int removeMatch, int removeValue, int[] dst, int dstOffset, int length);

    /* ------------------------------------------------ byte codec hot paths */
    // Used by the Base64 SIMD codec and Image.createMask. Without these natives
    // they fell through to the generic Simd scalar defaults (lane-scratch loops),
    // which were slower than straight scalar -- so "SIMD on" lost. cn1_windows_simd.c
    // implements them in C (NEON-vectorized on arm64, SSE2 byte-shifts on x64,
    // scalar table lookups like IOSSimd).

    @Override
    public native void shl(byte[] src, int bits, byte[] dst, int offset, int length);

    @Override
    public native void shl(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length);

    @Override
    public native void shrLogical(byte[] src, int bits, byte[] dst, int offset, int length);

    @Override
    public native void shrLogical(byte[] src, int srcOffset, int bits, byte[] dst, int dstOffset, int length);

    @Override
    public native void lookupBytes(byte[] table, byte[] indices, byte[] dst, int offset, int length);

    @Override
    public native void lookupBytes(byte[] table, byte[] indices, int indicesOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void packIntToByteTruncate(int[] src, byte[] dst, int offset, int length);

    @Override
    public native void packIntToByteTruncate(int[] src, int srcOffset, byte[] dst, int dstOffset, int length);

    @Override
    public native void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, int length);

    @Override
    public native void unpackBytesInterleaved3(byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int length);

    @Override
    public native void packBytesInterleaved3(byte[] src0, byte[] src1, byte[] src2, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved3(byte[] src, int src0Offset, int src1Offset, int src2Offset, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved4(byte[] src0, byte[] src1, byte[] src2, byte[] src3, byte[] dst, int dstOffset, int length);

    @Override
    public native void packBytesInterleaved4(byte[] src, int src0Offset, int src1Offset, int src2Offset, int src3Offset, byte[] dst, int dstOffset, int length);

    @Override
    public native int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst0, byte[] dst1, byte[] dst2, byte[] dst3, int length);

    @Override
    public native int unpackLookupBytesInterleaved4(byte[] table, byte[] src, int srcOffset, byte[] dst, int dst0Offset, int dst1Offset, int dst2Offset, int dst3Offset, int length);
}
