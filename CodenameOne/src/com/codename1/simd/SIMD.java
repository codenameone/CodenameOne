/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.simd;

/**
 * Explicit SIMD helper API.
 * <p>
 * This API is intentionally tiny and designed to be bytecode-recognition friendly for
 * platform translators that can lower these calls into native SIMD instructions.
 * </p>
 * <p>
 * The default Java implementation is scalar and fully functional so code remains portable.
 * </p>
 */
public final class SIMD {

    private SIMD() {
        throw new AssertionError("SIMD should not be instantiated");
    }

    /**
     * Returns true when the current runtime+translator combo can map this API to a native SIMD backend.
     * The default implementation returns false.
     *
     * @return true if SIMD backend support is available.
     */
    public static boolean isSupported() {
        String explicit = System.getProperty("cn1.parparvm");
        if ("true".equalsIgnoreCase(explicit)) {
            return true;
        }
        String vmName = System.getProperty("java.vm.name", "");
        return vmName != null && vmName.toLowerCase().contains("parparvm");
    }

    /**
     * 4-lane float vector value type.
     */
    public static final class Float4 {
        public final float x;
        public final float y;
        public final float z;
        public final float w;

        public Float4(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }

    public static Float4 makeFloat4(float x, float y, float z, float w) {
        return new Float4(x, y, z, w);
    }

    public static Float4 load(float[] values, int offset) {
        return new Float4(
                values[offset],
                values[offset + 1],
                values[offset + 2],
                values[offset + 3]
        );
    }

    public static void store(float[] values, int offset, Float4 value) {
        values[offset] = value.x;
        values[offset + 1] = value.y;
        values[offset + 2] = value.z;
        values[offset + 3] = value.w;
    }

    public static Float4 add(Float4 a, Float4 b) {
        return new Float4(
                a.x + b.x,
                a.y + b.y,
                a.z + b.z,
                a.w + b.w
        );
    }

    public static Float4 mul(Float4 a, Float4 b) {
        return new Float4(
                a.x * b.x,
                a.y * b.y,
                a.z * b.z,
                a.w * b.w
        );
    }

    public static Float4 fma(Float4 a, Float4 b, Float4 c) {
        return add(mul(a, b), c);
    }

    /**
     * 4-lane integer vector.
     */
    public static final class Int4 {
        public final int x;
        public final int y;
        public final int z;
        public final int w;

        public Int4(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }

    public static Int4 makeInt4(int x, int y, int z, int w) {
        return new Int4(x, y, z, w);
    }

    public static Int4 add(Int4 a, Int4 b) {
        return new Int4(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w);
    }

    public static Int4 sub(Int4 a, Int4 b) {
        return new Int4(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w);
    }

    public static Int4 and(Int4 a, Int4 b) {
        return new Int4(a.x & b.x, a.y & b.y, a.z & b.z, a.w & b.w);
    }

    public static Int4 or(Int4 a, Int4 b) {
        return new Int4(a.x | b.x, a.y | b.y, a.z | b.z, a.w | b.w);
    }

    public static Int4 shl(Int4 a, int bits) {
        return new Int4(a.x << bits, a.y << bits, a.z << bits, a.w << bits);
    }

    public static Int4 ushr(Int4 a, int bits) {
        return new Int4(a.x >>> bits, a.y >>> bits, a.z >>> bits, a.w >>> bits);
    }

    /**
     * Unsigned byte 16-lane vector.
     */
    public static final class U8x16 {
        private final int[] lanes = new int[16];

        private U8x16() {
        }
    }

    public static U8x16 loadU8(byte[] values, int offset) {
        U8x16 out = new U8x16();
        for (int i = 0; i < 16; i++) {
            out.lanes[i] = values[offset + i] & 0xff;
        }
        return out;
    }

    public static int laneU8(U8x16 value, int lane) {
        return value.lanes[lane] & 0xff;
    }

    public static U8x16 and(U8x16 a, U8x16 b) {
        U8x16 out = new U8x16();
        for (int i = 0; i < 16; i++) {
            out.lanes[i] = (a.lanes[i] & b.lanes[i]) & 0xff;
        }
        return out;
    }

    public static U8x16 or(U8x16 a, U8x16 b) {
        U8x16 out = new U8x16();
        for (int i = 0; i < 16; i++) {
            out.lanes[i] = (a.lanes[i] | b.lanes[i]) & 0xff;
        }
        return out;
    }

    public static U8x16 xor(U8x16 a, U8x16 b) {
        U8x16 out = new U8x16();
        for (int i = 0; i < 16; i++) {
            out.lanes[i] = (a.lanes[i] ^ b.lanes[i]) & 0xff;
        }
        return out;
    }

    public static U8x16 shl(U8x16 a, int bits) {
        U8x16 out = new U8x16();
        for (int i = 0; i < 16; i++) {
            out.lanes[i] = ((a.lanes[i] << bits) & 0xff);
        }
        return out;
    }

    public static U8x16 ushr(U8x16 a, int bits) {
        U8x16 out = new U8x16();
        for (int i = 0; i < 16; i++) {
            out.lanes[i] = (a.lanes[i] >>> bits) & 0xff;
        }
        return out;
    }
}
