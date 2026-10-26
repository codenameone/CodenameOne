/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security;

// Pure-Java implementations of MD5 / SHA-1 / SHA-2 (224, 256, 384, 512). These
// are the well-known reference algorithms (RFC 1321, RFC 3174, FIPS 180-4).
//
// One class with a small dispatch is more compact than five separate classes
// and keeps all the round constants close to the rounds that use them.
@com.codename1.impl.SharedWithBackend
abstract class MessageDigestImpl {

    abstract void update(byte[] data, int offset, int length);
    abstract void update(byte b);
    abstract byte[] digest();
    abstract int digestLength();
    abstract void reset();

    static MessageDigestImpl create(String algorithm) {
        if (algorithm == null) {
            throw new CryptoException("algorithm must not be null");
        }
        String a = normalise(algorithm);
        if ("MD5".equals(a)) {
            return new Md5();
        }
        if ("SHA1".equals(a)) {
            return new Sha1();
        }
        if ("SHA224".equals(a)) {
            return new Sha256Family(true);
        }
        if ("SHA256".equals(a)) {
            return new Sha256Family(false);
        }
        if ("SHA384".equals(a)) {
            return new Sha512Family(true);
        }
        if ("SHA512".equals(a)) {
            return new Sha512Family(false);
        }
        if ("KECCAK256".equals(a)) {
            return new Keccak(32, (byte) 0x01);
        }
        if ("RIPEMD160".equals(a)) {
            return new Ripemd160();
        }
        if ("SHA3256".equals(a)) {
            return new Keccak(32, (byte) 0x06);
        }
        throw new CryptoException("unsupported hash algorithm: " + algorithm);
    }

    static String normalise(String algorithm) {
        StringBuilder b = new StringBuilder(algorithm.length());
        for (int i = 0; i < algorithm.length(); i++) {
            char c = algorithm.charAt(i);
            if (c == '-' || c == '_' || c == ' ') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 'a' + 'A');
            }
            b.append(c);
        }
        return b.toString();
    }

    // ===============================================================
    // shared 64-byte (512-bit) block engine used by MD5 and SHA-1/SHA-256
    abstract static class Block64 extends MessageDigestImpl {
        final byte[] buffer = new byte[64];
        int bufferLen;
        long byteCount;

        abstract void processBlock(byte[] block, int offset);
        abstract void writeStateBigEndian(byte[] out);

        @Override
        void update(byte[] data, int offset, int length) {
            byteCount += length;
            if (bufferLen > 0) {
                int copy = 64 - bufferLen;
                if (copy > length) {
                    copy = length;
                }
                System.arraycopy(data, offset, buffer, bufferLen, copy);
                bufferLen += copy;
                offset += copy;
                length -= copy;
                if (bufferLen == 64) {
                    processBlock(buffer, 0);
                    bufferLen = 0;
                }
            }
            while (length >= 64) {
                processBlock(data, offset);
                offset += 64;
                length -= 64;
            }
            if (length > 0) {
                System.arraycopy(data, offset, buffer, 0, length);
                bufferLen = length;
            }
        }

        @Override
        void update(byte b) {
            byteCount++;
            buffer[bufferLen++] = b;
            if (bufferLen == 64) {
                processBlock(buffer, 0);
                bufferLen = 0;
            }
        }

        final byte[] finishCommon(boolean bigEndianLength) {
            long bits = byteCount * 8L;
            buffer[bufferLen++] = (byte) 0x80;
            if (bufferLen > 56) {
                while (bufferLen < 64) {
                    buffer[bufferLen++] = 0;
                }
                processBlock(buffer, 0);
                bufferLen = 0;
            }
            while (bufferLen < 56) {
                buffer[bufferLen++] = 0;
            }
            if (bigEndianLength) {
                buffer[56] = (byte) (bits >>> 56);
                buffer[57] = (byte) (bits >>> 48);
                buffer[58] = (byte) (bits >>> 40);
                buffer[59] = (byte) (bits >>> 32);
                buffer[60] = (byte) (bits >>> 24);
                buffer[61] = (byte) (bits >>> 16);
                buffer[62] = (byte) (bits >>> 8);
                buffer[63] = (byte) bits;
            } else {
                buffer[56] = (byte) bits;
                buffer[57] = (byte) (bits >>> 8);
                buffer[58] = (byte) (bits >>> 16);
                buffer[59] = (byte) (bits >>> 24);
                buffer[60] = (byte) (bits >>> 32);
                buffer[61] = (byte) (bits >>> 40);
                buffer[62] = (byte) (bits >>> 48);
                buffer[63] = (byte) (bits >>> 56);
            }
            processBlock(buffer, 0);
            byte[] out = new byte[digestLength()];
            writeStateBigEndian(out);
            reset();
            return out;
        }
    }

    // ===============================================================
    // MD5 -- RFC 1321 (little-endian length, little-endian word loads)
    static final class Md5 extends Block64 {
        int a;
        int b;
        int c;
        int d;

        Md5() {
            reset();
        }

        @Override
        public void reset() {
            a = 0x67452301;
            b = 0xefcdab89;
            c = 0x98badcfe;
            d = 0x10325476;
            bufferLen = 0;
            byteCount = 0;
        }

        @Override
        public int digestLength() {
            return 16;
        }

        @Override
        public byte[] digest() {
            return finishCommon(false);
        }

        @Override
        void writeStateBigEndian(byte[] out) {
            // MD5 actually writes its state little-endian; we reuse the name
            // for the shared finish path.
            writeLE(out, 0, a);
            writeLE(out, 4, b);
            writeLE(out, 8, c);
            writeLE(out, 12, d);
        }

        private static void writeLE(byte[] out, int o, int v) {
            out[o] = (byte) v;
            out[o + 1] = (byte) (v >>> 8);
            out[o + 2] = (byte) (v >>> 16);
            out[o + 3] = (byte) (v >>> 24);
        }

        private static int readLE(byte[] src, int o) {
            return  (src[o] & 0xff)
                  | (src[o + 1] & 0xff) << 8
                  | (src[o + 2] & 0xff) << 16
                  | (src[o + 3] & 0xff) << 24;
        }

        private static int rol(int v, int s) {
            return (v << s) | (v >>> (32 - s));
        }

        @Override
        void processBlock(byte[] block, int o) {
            int x0  = readLE(block, o);
            int x1  = readLE(block, o + 4);
            int x2  = readLE(block, o + 8);
            int x3  = readLE(block, o + 12);
            int x4  = readLE(block, o + 16);
            int x5  = readLE(block, o + 20);
            int x6  = readLE(block, o + 24);
            int x7  = readLE(block, o + 28);
            int x8  = readLE(block, o + 32);
            int x9  = readLE(block, o + 36);
            int x10 = readLE(block, o + 40);
            int x11 = readLE(block, o + 44);
            int x12 = readLE(block, o + 48);
            int x13 = readLE(block, o + 52);
            int x14 = readLE(block, o + 56);
            int x15 = readLE(block, o + 60);

            int aa = a;
            int bb = b;
            int cc = c;
            int dd = d;

            // round 1
            aa = bb + rol(aa + ((bb & cc) | (~bb & dd)) + x0  + 0xd76aa478, 7);
            dd = aa + rol(dd + ((aa & bb) | (~aa & cc)) + x1  + 0xe8c7b756, 12);
            cc = dd + rol(cc + ((dd & aa) | (~dd & bb)) + x2  + 0x242070db, 17);
            bb = cc + rol(bb + ((cc & dd) | (~cc & aa)) + x3  + 0xc1bdceee, 22);
            aa = bb + rol(aa + ((bb & cc) | (~bb & dd)) + x4  + 0xf57c0faf, 7);
            dd = aa + rol(dd + ((aa & bb) | (~aa & cc)) + x5  + 0x4787c62a, 12);
            cc = dd + rol(cc + ((dd & aa) | (~dd & bb)) + x6  + 0xa8304613, 17);
            bb = cc + rol(bb + ((cc & dd) | (~cc & aa)) + x7  + 0xfd469501, 22);
            aa = bb + rol(aa + ((bb & cc) | (~bb & dd)) + x8  + 0x698098d8, 7);
            dd = aa + rol(dd + ((aa & bb) | (~aa & cc)) + x9  + 0x8b44f7af, 12);
            cc = dd + rol(cc + ((dd & aa) | (~dd & bb)) + x10 + 0xffff5bb1, 17);
            bb = cc + rol(bb + ((cc & dd) | (~cc & aa)) + x11 + 0x895cd7be, 22);
            aa = bb + rol(aa + ((bb & cc) | (~bb & dd)) + x12 + 0x6b901122, 7);
            dd = aa + rol(dd + ((aa & bb) | (~aa & cc)) + x13 + 0xfd987193, 12);
            cc = dd + rol(cc + ((dd & aa) | (~dd & bb)) + x14 + 0xa679438e, 17);
            bb = cc + rol(bb + ((cc & dd) | (~cc & aa)) + x15 + 0x49b40821, 22);

            // round 2
            aa = bb + rol(aa + ((bb & dd) | (cc & ~dd)) + x1  + 0xf61e2562, 5);
            dd = aa + rol(dd + ((aa & cc) | (bb & ~cc)) + x6  + 0xc040b340, 9);
            cc = dd + rol(cc + ((dd & bb) | (aa & ~bb)) + x11 + 0x265e5a51, 14);
            bb = cc + rol(bb + ((cc & aa) | (dd & ~aa)) + x0  + 0xe9b6c7aa, 20);
            aa = bb + rol(aa + ((bb & dd) | (cc & ~dd)) + x5  + 0xd62f105d, 5);
            dd = aa + rol(dd + ((aa & cc) | (bb & ~cc)) + x10 + 0x02441453, 9);
            cc = dd + rol(cc + ((dd & bb) | (aa & ~bb)) + x15 + 0xd8a1e681, 14);
            bb = cc + rol(bb + ((cc & aa) | (dd & ~aa)) + x4  + 0xe7d3fbc8, 20);
            aa = bb + rol(aa + ((bb & dd) | (cc & ~dd)) + x9  + 0x21e1cde6, 5);
            dd = aa + rol(dd + ((aa & cc) | (bb & ~cc)) + x14 + 0xc33707d6, 9);
            cc = dd + rol(cc + ((dd & bb) | (aa & ~bb)) + x3  + 0xf4d50d87, 14);
            bb = cc + rol(bb + ((cc & aa) | (dd & ~aa)) + x8  + 0x455a14ed, 20);
            aa = bb + rol(aa + ((bb & dd) | (cc & ~dd)) + x13 + 0xa9e3e905, 5);
            dd = aa + rol(dd + ((aa & cc) | (bb & ~cc)) + x2  + 0xfcefa3f8, 9);
            cc = dd + rol(cc + ((dd & bb) | (aa & ~bb)) + x7  + 0x676f02d9, 14);
            bb = cc + rol(bb + ((cc & aa) | (dd & ~aa)) + x12 + 0x8d2a4c8a, 20);

            // round 3
            aa = bb + rol(aa + (bb ^ cc ^ dd) + x5  + 0xfffa3942, 4);
            dd = aa + rol(dd + (aa ^ bb ^ cc) + x8  + 0x8771f681, 11);
            cc = dd + rol(cc + (dd ^ aa ^ bb) + x11 + 0x6d9d6122, 16);
            bb = cc + rol(bb + (cc ^ dd ^ aa) + x14 + 0xfde5380c, 23);
            aa = bb + rol(aa + (bb ^ cc ^ dd) + x1  + 0xa4beea44, 4);
            dd = aa + rol(dd + (aa ^ bb ^ cc) + x4  + 0x4bdecfa9, 11);
            cc = dd + rol(cc + (dd ^ aa ^ bb) + x7  + 0xf6bb4b60, 16);
            bb = cc + rol(bb + (cc ^ dd ^ aa) + x10 + 0xbebfbc70, 23);
            aa = bb + rol(aa + (bb ^ cc ^ dd) + x13 + 0x289b7ec6, 4);
            dd = aa + rol(dd + (aa ^ bb ^ cc) + x0  + 0xeaa127fa, 11);
            cc = dd + rol(cc + (dd ^ aa ^ bb) + x3  + 0xd4ef3085, 16);
            bb = cc + rol(bb + (cc ^ dd ^ aa) + x6  + 0x04881d05, 23);
            aa = bb + rol(aa + (bb ^ cc ^ dd) + x9  + 0xd9d4d039, 4);
            dd = aa + rol(dd + (aa ^ bb ^ cc) + x12 + 0xe6db99e5, 11);
            cc = dd + rol(cc + (dd ^ aa ^ bb) + x15 + 0x1fa27cf8, 16);
            bb = cc + rol(bb + (cc ^ dd ^ aa) + x2  + 0xc4ac5665, 23);

            // round 4
            aa = bb + rol(aa + (cc ^ (bb | ~dd)) + x0  + 0xf4292244, 6);
            dd = aa + rol(dd + (bb ^ (aa | ~cc)) + x7  + 0x432aff97, 10);
            cc = dd + rol(cc + (aa ^ (dd | ~bb)) + x14 + 0xab9423a7, 15);
            bb = cc + rol(bb + (dd ^ (cc | ~aa)) + x5  + 0xfc93a039, 21);
            aa = bb + rol(aa + (cc ^ (bb | ~dd)) + x12 + 0x655b59c3, 6);
            dd = aa + rol(dd + (bb ^ (aa | ~cc)) + x3  + 0x8f0ccc92, 10);
            cc = dd + rol(cc + (aa ^ (dd | ~bb)) + x10 + 0xffeff47d, 15);
            bb = cc + rol(bb + (dd ^ (cc | ~aa)) + x1  + 0x85845dd1, 21);
            aa = bb + rol(aa + (cc ^ (bb | ~dd)) + x8  + 0x6fa87e4f, 6);
            dd = aa + rol(dd + (bb ^ (aa | ~cc)) + x15 + 0xfe2ce6e0, 10);
            cc = dd + rol(cc + (aa ^ (dd | ~bb)) + x6  + 0xa3014314, 15);
            bb = cc + rol(bb + (dd ^ (cc | ~aa)) + x13 + 0x4e0811a1, 21);
            aa = bb + rol(aa + (cc ^ (bb | ~dd)) + x4  + 0xf7537e82, 6);
            dd = aa + rol(dd + (bb ^ (aa | ~cc)) + x11 + 0xbd3af235, 10);
            cc = dd + rol(cc + (aa ^ (dd | ~bb)) + x2  + 0x2ad7d2bb, 15);
            bb = cc + rol(bb + (dd ^ (cc | ~aa)) + x9  + 0xeb86d391, 21);

            a += aa;
            b += bb;
            c += cc;
            d += dd;
        }
    }

    // ===============================================================
    // SHA-1 -- RFC 3174
    static final class Sha1 extends Block64 {
        int h0;
        int h1;
        int h2;
        int h3;
        int h4;
        final int[] w = new int[80];

        Sha1() {
            reset();
        }

        @Override
        public void reset() {
            h0 = 0x67452301;
            h1 = 0xEFCDAB89;
            h2 = 0x98BADCFE;
            h3 = 0x10325476;
            h4 = 0xC3D2E1F0;
            bufferLen = 0;
            byteCount = 0;
        }

        @Override
        public int digestLength() {
            return 20;
        }

        @Override
        public byte[] digest() {
            return finishCommon(true);
        }

        @Override
        void writeStateBigEndian(byte[] out) {
            writeBE(out, 0, h0);
            writeBE(out, 4, h1);
            writeBE(out, 8, h2);
            writeBE(out, 12, h3);
            writeBE(out, 16, h4);
        }

        @Override
        void processBlock(byte[] block, int o) {
            for (int i = 0; i < 16; i++) {
                w[i] = (block[o + i * 4] & 0xff) << 24
                     | (block[o + i * 4 + 1] & 0xff) << 16
                     | (block[o + i * 4 + 2] & 0xff) << 8
                     | (block[o + i * 4 + 3] & 0xff);
            }
            for (int i = 16; i < 80; i++) {
                int t = w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16];
                w[i] = (t << 1) | (t >>> 31);
            }
            int a = h0;
            int b = h1;
            int c = h2;
            int d = h3;
            int e = h4;
            for (int i = 0; i < 80; i++) {
                int f;
                int k;
                if (i < 20) {
                    f = (b & c) | (~b & d);
                    k = 0x5A827999;
                } else if (i < 40) {
                    f = b ^ c ^ d;
                    k = 0x6ED9EBA1;
                } else if (i < 60) {
                    f = (b & c) | (b & d) | (c & d);
                    k = 0x8F1BBCDC;
                } else {
                    f = b ^ c ^ d;
                    k = 0xCA62C1D6;
                }
                int t = ((a << 5) | (a >>> 27)) + f + e + k + w[i];
                e = d;
                d = c;
                c = (b << 30) | (b >>> 2);
                b = a;
                a = t;
            }
            h0 += a;
            h1 += b;
            h2 += c;
            h3 += d;
            h4 += e;
        }
    }

    // ===============================================================
    // SHA-224 / SHA-256 -- FIPS 180-4 (32-bit word version)
    static final class Sha256Family extends Block64 {
        private static final int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
        };

        private final boolean truncated; // sha-224 if true
        private int h0;
        private int h1;
        private int h2;
        private int h3;
        private int h4;
        private int h5;
        private int h6;
        private int h7;
        private final int[] w = new int[64];
        private final int[] state = new int[8];

        Sha256Family(boolean truncated) {
            this.truncated = truncated;
            reset();
        }

        @Override
        public void reset() {
            if (truncated) {
                h0 = 0xc1059ed8; h1 = 0x367cd507; h2 = 0x3070dd17; h3 = 0xf70e5939;
                h4 = 0xffc00b31; h5 = 0x68581511; h6 = 0x64f98fa7; h7 = 0xbefa4fa4;
            } else {
                h0 = 0x6a09e667; h1 = 0xbb67ae85; h2 = 0x3c6ef372; h3 = 0xa54ff53a;
                h4 = 0x510e527f; h5 = 0x9b05688c; h6 = 0x1f83d9ab; h7 = 0x5be0cd19;
            }
            bufferLen = 0;
            byteCount = 0;
        }

        @Override
        public int digestLength() {
            return truncated ? 28 : 32;
        }

        @Override
        public byte[] digest() {
            return finishCommon(true);
        }

        @Override
        void writeStateBigEndian(byte[] out) {
            writeBE(out, 0, h0);
            writeBE(out, 4, h1);
            writeBE(out, 8, h2);
            writeBE(out, 12, h3);
            writeBE(out, 16, h4);
            writeBE(out, 20, h5);
            writeBE(out, 24, h6);
            if (!truncated) {
                writeBE(out, 28, h7);
            }
        }

        @Override
        void processBlock(byte[] block, int o) {
            for (int i = 0; i < 16; i++) {
                w[i] = (block[o + i * 4] & 0xff) << 24
                     | (block[o + i * 4 + 1] & 0xff) << 16
                     | (block[o + i * 4 + 2] & 0xff) << 8
                     | (block[o + i * 4 + 3] & 0xff);
            }
            int[] st = state;
            st[0] = h0; st[1] = h1; st[2] = h2; st[3] = h3;
            st[4] = h4; st[5] = h5; st[6] = h6; st[7] = h7;
            compress(st, w);
            h0 = st[0]; h1 = st[1]; h2 = st[2]; h3 = st[3];
            h4 = st[4]; h5 = st[5]; h6 = st[6]; h7 = st[7];
        }

        /// One SHA-256 compression of the block whose sixteen big-endian
        /// words are in `w[0..15]` (w has room for 64) into `st[0..7]`.
        static void compress(int[] st, int[] w) {
            for (int i = 16; i < 64; i++) {
                int v15 = w[i - 15];
                int s0 = ((v15 >>> 7) | (v15 << 25)) ^ ((v15 >>> 18) | (v15 << 14)) ^ (v15 >>> 3);
                int v2 = w[i - 2];
                int s1 = ((v2 >>> 17) | (v2 << 15)) ^ ((v2 >>> 19) | (v2 << 13)) ^ (v2 >>> 10);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }
            int a = st[0];
            int b = st[1];
            int c = st[2];
            int d = st[3];
            int e = st[4];
            int f = st[5];
            int g = st[6];
            int h = st[7];
            for (int i = 0; i < 64; i++) {
                int s1 = ((e >>> 6) | (e << 26)) ^ ((e >>> 11) | (e << 21)) ^ ((e >>> 25) | (e << 7));
                int ch = (e & f) ^ (~e & g);
                int t1 = h + s1 + ch + K[i] + w[i];
                int s0 = ((a >>> 2) | (a << 30)) ^ ((a >>> 13) | (a << 19)) ^ ((a >>> 22) | (a << 10));
                int mj = (a & b) ^ (a & c) ^ (b & c);
                int t2 = s0 + mj;
                h = g; g = f; f = e;
                e = d + t1;
                d = c; c = b; b = a;
                a = t1 + t2;
            }
            st[0] += a; st[1] += b; st[2] += c; st[3] += d;
            st[4] += e; st[5] += f; st[6] += g; st[7] += h;
        }
    }

    // ===============================================================
    // SHA-384 / SHA-512 -- FIPS 180-4 (64-bit word version, 128-byte blocks,
    // 128-bit length field). Uses a 128-byte block engine.
    static final class Sha512Family extends MessageDigestImpl {
        private static final long[] K = {
            0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL,
            0x3956c25bf348b538L, 0x59f111f1b605d019L, 0x923f82a4af194f9bL, 0xab1c5ed5da6d8118L,
            0xd807aa98a3030242L, 0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L,
            0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L, 0xc19bf174cf692694L,
            0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L,
            0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L, 0x76f988da831153b5L,
            0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL, 0xbf597fc7beef0ee4L,
            0xc6e00bf33da88fc2L, 0xd5a79147930aa725L, 0x06ca6351e003826fL, 0x142929670a0e6e70L,
            0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL,
            0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L, 0x92722c851482353bL,
            0xa2bfe8a14cf10364L, 0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L,
            0xd192e819d6ef5218L, 0xd69906245565a910L, 0xf40e35855771202aL, 0x106aa07032bbd1b8L,
            0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L,
            0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L, 0x682e6ff3d6b2b8a3L,
            0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL,
            0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L, 0xc67178f2e372532bL,
            0xca273eceea26619cL, 0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L,
            0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L, 0x113f9804bef90daeL, 0x1b710b35131c471bL,
            0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL,
            0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L
        };

        private final boolean truncated; // sha-384 if true
        private long h0;
        private long h1;
        private long h2;
        private long h3;
        private long h4;
        private long h5;
        private long h6;
        private long h7;
        private final byte[] buffer = new byte[128];
        private int bufferLen;
        private long byteCount; // we cap message length at 2^63-1 bytes which is plenty
        private final long[] w = new long[80];
        private final long[] state = new long[8];

        Sha512Family(boolean truncated) {
            this.truncated = truncated;
            reset();
        }

        @Override
        public void reset() {
            if (truncated) {
                h0 = 0xcbbb9d5dc1059ed8L; h1 = 0x629a292a367cd507L; h2 = 0x9159015a3070dd17L;
                h3 = 0x152fecd8f70e5939L; h4 = 0x67332667ffc00b31L; h5 = 0x8eb44a8768581511L;
                h6 = 0xdb0c2e0d64f98fa7L; h7 = 0x47b5481dbefa4fa4L;
            } else {
                h0 = 0x6a09e667f3bcc908L; h1 = 0xbb67ae8584caa73bL; h2 = 0x3c6ef372fe94f82bL;
                h3 = 0xa54ff53a5f1d36f1L; h4 = 0x510e527fade682d1L; h5 = 0x9b05688c2b3e6c1fL;
                h6 = 0x1f83d9abfb41bd6bL; h7 = 0x5be0cd19137e2179L;
            }
            bufferLen = 0;
            byteCount = 0;
        }

        @Override
        public int digestLength() {
            return truncated ? 48 : 64;
        }

        @Override
        void update(byte[] data, int offset, int length) {
            byteCount += length;
            if (bufferLen > 0) {
                int copy = 128 - bufferLen;
                if (copy > length) {
                    copy = length;
                }
                System.arraycopy(data, offset, buffer, bufferLen, copy);
                bufferLen += copy;
                offset += copy;
                length -= copy;
                if (bufferLen == 128) {
                    processBlock(buffer, 0);
                    bufferLen = 0;
                }
            }
            while (length >= 128) {
                processBlock(data, offset);
                offset += 128;
                length -= 128;
            }
            if (length > 0) {
                System.arraycopy(data, offset, buffer, 0, length);
                bufferLen = length;
            }
        }

        @Override
        void update(byte b) {
            byteCount++;
            buffer[bufferLen++] = b;
            if (bufferLen == 128) {
                processBlock(buffer, 0);
                bufferLen = 0;
            }
        }

        @Override
        public byte[] digest() {
            long bits = byteCount * 8L;
            buffer[bufferLen++] = (byte) 0x80;
            if (bufferLen > 112) {
                while (bufferLen < 128) {
                    buffer[bufferLen++] = 0;
                }
                processBlock(buffer, 0);
                bufferLen = 0;
            }
            while (bufferLen < 112) {
                buffer[bufferLen++] = 0;
            }
            // high 64 bits of the 128-bit length field are always 0 here since
            // a Java byte array cannot hold more than 2^31-1 bytes.
            for (int i = 112; i < 120; i++) {
                buffer[i] = 0;
            }
            buffer[120] = (byte) (bits >>> 56);
            buffer[121] = (byte) (bits >>> 48);
            buffer[122] = (byte) (bits >>> 40);
            buffer[123] = (byte) (bits >>> 32);
            buffer[124] = (byte) (bits >>> 24);
            buffer[125] = (byte) (bits >>> 16);
            buffer[126] = (byte) (bits >>> 8);
            buffer[127] = (byte) bits;
            processBlock(buffer, 0);

            byte[] out = new byte[digestLength()];
            writeBE64(out, 0,  h0);
            writeBE64(out, 8,  h1);
            writeBE64(out, 16, h2);
            writeBE64(out, 24, h3);
            writeBE64(out, 32, h4);
            writeBE64(out, 40, h5);
            // sha-384 omits h6, h7
            if (!truncated) {
                writeBE64(out, 48, h6);
                writeBE64(out, 56, h7);
            }
            reset();
            return out;
        }

        private void processBlock(byte[] block, int o) {
            for (int i = 0; i < 16; i++) {
                int p = o + i * 8;
                w[i] = ((long) (block[p]     & 0xff) << 56)
                     | ((long) (block[p + 1] & 0xff) << 48)
                     | ((long) (block[p + 2] & 0xff) << 40)
                     | ((long) (block[p + 3] & 0xff) << 32)
                     | ((long) (block[p + 4] & 0xff) << 24)
                     | ((long) (block[p + 5] & 0xff) << 16)
                     | ((long) (block[p + 6] & 0xff) << 8)
                     | ((long) (block[p + 7] & 0xff));
            }
            long[] st = state;
            st[0] = h0; st[1] = h1; st[2] = h2; st[3] = h3;
            st[4] = h4; st[5] = h5; st[6] = h6; st[7] = h7;
            compress(st, w);
            h0 = st[0]; h1 = st[1]; h2 = st[2]; h3 = st[3];
            h4 = st[4]; h5 = st[5]; h6 = st[6]; h7 = st[7];
        }

        /// One SHA-512 compression of the block whose sixteen big-endian
        /// words are in `w[0..15]` (w has room for 80) into `st[0..7]`.
        static void compress(long[] st, long[] w) {
            for (int i = 16; i < 80; i++) {
                long v15 = w[i - 15];
                long s0 = ((v15 >>> 1) | (v15 << 63))
                        ^ ((v15 >>> 8) | (v15 << 56))
                        ^ (v15 >>> 7);
                long v2 = w[i - 2];
                long s1 = ((v2 >>> 19) | (v2 << 45))
                        ^ ((v2 >>> 61) | (v2 << 3))
                        ^ (v2 >>> 6);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }
            long a = st[0];
            long b = st[1];
            long c = st[2];
            long d = st[3];
            long e = st[4];
            long f = st[5];
            long g = st[6];
            long h = st[7];
            for (int i = 0; i < 80; i++) {
                long s1 = ((e >>> 14) | (e << 50))
                        ^ ((e >>> 18) | (e << 46))
                        ^ ((e >>> 41) | (e << 23));
                long ch = (e & f) ^ (~e & g);
                long t1 = h + s1 + ch + K[i] + w[i];
                long s0 = ((a >>> 28) | (a << 36))
                        ^ ((a >>> 34) | (a << 30))
                        ^ ((a >>> 39) | (a << 25));
                long mj = (a & b) ^ (a & c) ^ (b & c);
                long t2 = s0 + mj;
                h = g; g = f; f = e;
                e = d + t1;
                d = c; c = b; b = a;
                a = t1 + t2;
            }
            st[0] += a; st[1] += b; st[2] += c; st[3] += d;
            st[4] += e; st[5] += f; st[6] += g; st[7] += h;
        }
    }

    // ===============================================================
    // Keccak sponge over Keccak-f[1600] -- FIPS 202, with the original
    // Keccak padding as an alternative domain byte.
    //
    // The two algorithms built on it differ in ONE byte: the first padding
    // byte is 0x01 for the original Keccak (what Ethereum hashes with: block
    // and transaction hashes, addresses, ABI selectors) and 0x06 for the
    // standardised SHA3 (FIPS 202 appends the two domain bits 01 before the
    // pad10*1). Everything else -- the permutation, the rate, the squeeze --
    // is shared, so one class covers both.
    static final class Keccak extends MessageDigestImpl {
        private static final long[] RC = {
            0x0000000000000001L, 0x0000000000008082L, 0x800000000000808aL, 0x8000000080008000L,
            0x000000000000808bL, 0x0000000080000001L, 0x8000000080008081L, 0x8000000000008009L,
            0x000000000000008aL, 0x0000000000000088L, 0x0000000080008009L, 0x000000008000000aL,
            0x000000008000808bL, 0x800000000000008bL, 0x8000000000008089L, 0x8000000000008003L,
            0x8000000000008002L, 0x8000000000000080L, 0x000000000000800aL, 0x800000008000000aL,
            0x8000000080008081L, 0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L
        };

        private final int outLen;
        private final int rate;
        private final byte domain;
        private final long[] state = new long[25];
        private final byte[] buffer;
        private int bufferLen;

        Keccak(int outLen, byte domain) {
            this.outLen = outLen;
            this.rate = 200 - 2 * outLen;
            this.domain = domain;
            this.buffer = new byte[rate];
        }

        @Override
        public void reset() {
            for (int i = 0; i < 25; i++) {
                state[i] = 0;
            }
            bufferLen = 0;
        }

        @Override
        public int digestLength() {
            return outLen;
        }

        @Override
        void update(byte b) {
            buffer[bufferLen++] = b;
            if (bufferLen == rate) {
                absorb();
            }
        }

        @Override
        void update(byte[] data, int offset, int length) {
            int o = offset;
            int remaining = length;
            while (bufferLen == 0 && remaining >= rate) {
                absorb(data, o);
                o += rate;
                remaining -= rate;
            }
            while (remaining > 0) {
                int n = rate - bufferLen;
                if (n > remaining) {
                    n = remaining;
                }
                System.arraycopy(data, o, buffer, bufferLen, n);
                bufferLen += n;
                o += n;
                remaining -= n;
                if (bufferLen == rate) {
                    absorb();
                }
            }
        }

        @Override
        public byte[] digest() {
            for (int i = bufferLen; i < rate; i++) {
                buffer[i] = 0;
            }
            buffer[bufferLen] ^= domain;
            buffer[rate - 1] ^= (byte) 0x80;
            bufferLen = rate;
            absorb();
            byte[] out = new byte[outLen];
            for (int i = 0; i < outLen; i++) {
                out[i] = (byte) (state[i >> 3] >>> ((i & 7) << 3));
            }
            reset();
            return out;
        }

        // XORs the full buffer into the state lane by lane (little endian) and
        // runs the permutation.
        private void absorb() {
            absorb(buffer, 0);
            bufferLen = 0;
        }

        private void absorb(byte[] src, int off) {
            int lanes = rate >> 3;
            long[] st = state;
            for (int i = 0; i < lanes; i++) {
                int o = off + (i << 3);
                st[i] ^= (src[o] & 0xffL) | (src[o + 1] & 0xffL) << 8 | (src[o + 2] & 0xffL) << 16
                        | (src[o + 3] & 0xffL) << 24 | (src[o + 4] & 0xffL) << 32 | (src[o + 5] & 0xffL) << 40
                        | (src[o + 6] & 0xffL) << 48 | (long) src[o + 7] << 56;
            }
            permute();
        }

        // Keccak-f[1600] with the 25 lanes in locals and each round's theta,
        // rho, pi and chi written out lane by lane: no array traffic and no
        // index tables inside the 24 rounds.
        private void permute() {
        long[] st = state;
        long a0 = st[0], a1 = st[1], a2 = st[2], a3 = st[3], a4 = st[4], a5 = st[5], a6 = st[6], a7 = st[7], a8 = st[8], a9 = st[9], a10 = st[10], a11 = st[11], a12 = st[12], a13 = st[13], a14 = st[14], a15 = st[15], a16 = st[16], a17 = st[17], a18 = st[18], a19 = st[19], a20 = st[20], a21 = st[21], a22 = st[22], a23 = st[23], a24 = st[24];
        for (int round = 0; round < 24; round++) {
            long c0 = a0 ^ a5 ^ a10 ^ a15 ^ a20;
            long c1 = a1 ^ a6 ^ a11 ^ a16 ^ a21;
            long c2 = a2 ^ a7 ^ a12 ^ a17 ^ a22;
            long c3 = a3 ^ a8 ^ a13 ^ a18 ^ a23;
            long c4 = a4 ^ a9 ^ a14 ^ a19 ^ a24;
            long d0 = c4 ^ ((c1 << 1) | (c1 >>> 63));
            long d1 = c0 ^ ((c2 << 1) | (c2 >>> 63));
            long d2 = c1 ^ ((c3 << 1) | (c3 >>> 63));
            long d3 = c2 ^ ((c4 << 1) | (c4 >>> 63));
            long d4 = c3 ^ ((c0 << 1) | (c0 >>> 63));
            long b0 = (a0 ^ d0);
            long t1 = a1 ^ d1;
            long b10 = (t1 << 1) | (t1 >>> 63);
            long t2 = a2 ^ d2;
            long b20 = (t2 << 62) | (t2 >>> 2);
            long t3 = a3 ^ d3;
            long b5 = (t3 << 28) | (t3 >>> 36);
            long t4 = a4 ^ d4;
            long b15 = (t4 << 27) | (t4 >>> 37);
            long t5 = a5 ^ d0;
            long b16 = (t5 << 36) | (t5 >>> 28);
            long t6 = a6 ^ d1;
            long b1 = (t6 << 44) | (t6 >>> 20);
            long t7 = a7 ^ d2;
            long b11 = (t7 << 6) | (t7 >>> 58);
            long t8 = a8 ^ d3;
            long b21 = (t8 << 55) | (t8 >>> 9);
            long t9 = a9 ^ d4;
            long b6 = (t9 << 20) | (t9 >>> 44);
            long t10 = a10 ^ d0;
            long b7 = (t10 << 3) | (t10 >>> 61);
            long t11 = a11 ^ d1;
            long b17 = (t11 << 10) | (t11 >>> 54);
            long t12 = a12 ^ d2;
            long b2 = (t12 << 43) | (t12 >>> 21);
            long t13 = a13 ^ d3;
            long b12 = (t13 << 25) | (t13 >>> 39);
            long t14 = a14 ^ d4;
            long b22 = (t14 << 39) | (t14 >>> 25);
            long t15 = a15 ^ d0;
            long b23 = (t15 << 41) | (t15 >>> 23);
            long t16 = a16 ^ d1;
            long b8 = (t16 << 45) | (t16 >>> 19);
            long t17 = a17 ^ d2;
            long b18 = (t17 << 15) | (t17 >>> 49);
            long t18 = a18 ^ d3;
            long b3 = (t18 << 21) | (t18 >>> 43);
            long t19 = a19 ^ d4;
            long b13 = (t19 << 8) | (t19 >>> 56);
            long t20 = a20 ^ d0;
            long b14 = (t20 << 18) | (t20 >>> 46);
            long t21 = a21 ^ d1;
            long b24 = (t21 << 2) | (t21 >>> 62);
            long t22 = a22 ^ d2;
            long b9 = (t22 << 61) | (t22 >>> 3);
            long t23 = a23 ^ d3;
            long b19 = (t23 << 56) | (t23 >>> 8);
            long t24 = a24 ^ d4;
            long b4 = (t24 << 14) | (t24 >>> 50);
            a0 = b0 ^ (~b1 & b2);
            a1 = b1 ^ (~b2 & b3);
            a2 = b2 ^ (~b3 & b4);
            a3 = b3 ^ (~b4 & b0);
            a4 = b4 ^ (~b0 & b1);
            a5 = b5 ^ (~b6 & b7);
            a6 = b6 ^ (~b7 & b8);
            a7 = b7 ^ (~b8 & b9);
            a8 = b8 ^ (~b9 & b5);
            a9 = b9 ^ (~b5 & b6);
            a10 = b10 ^ (~b11 & b12);
            a11 = b11 ^ (~b12 & b13);
            a12 = b12 ^ (~b13 & b14);
            a13 = b13 ^ (~b14 & b10);
            a14 = b14 ^ (~b10 & b11);
            a15 = b15 ^ (~b16 & b17);
            a16 = b16 ^ (~b17 & b18);
            a17 = b17 ^ (~b18 & b19);
            a18 = b18 ^ (~b19 & b15);
            a19 = b19 ^ (~b15 & b16);
            a20 = b20 ^ (~b21 & b22);
            a21 = b21 ^ (~b22 & b23);
            a22 = b22 ^ (~b23 & b24);
            a23 = b23 ^ (~b24 & b20);
            a24 = b24 ^ (~b20 & b21);
            a0 ^= RC[round];
        }
        st[0] = a0;
        st[1] = a1;
        st[2] = a2;
        st[3] = a3;
        st[4] = a4;
        st[5] = a5;
        st[6] = a6;
        st[7] = a7;
        st[8] = a8;
        st[9] = a9;
        st[10] = a10;
        st[11] = a11;
        st[12] = a12;
        st[13] = a13;
        st[14] = a14;
        st[15] = a15;
        st[16] = a16;
        st[17] = a17;
        st[18] = a18;
        st[19] = a19;
        st[20] = a20;
        st[21] = a21;
        st[22] = a22;
        st[23] = a23;
        st[24] = a24;
        }
    }

    // ===============================================================
    // shared little helpers
    static void writeBE(byte[] out, int o, int v) {
        out[o]     = (byte) (v >>> 24);
        out[o + 1] = (byte) (v >>> 16);
        out[o + 2] = (byte) (v >>> 8);
        out[o + 3] = (byte) v;
    }

    static void writeBE64(byte[] out, int o, long v) {
        out[o]     = (byte) (v >>> 56);
        out[o + 1] = (byte) (v >>> 48);
        out[o + 2] = (byte) (v >>> 40);
        out[o + 3] = (byte) (v >>> 32);
        out[o + 4] = (byte) (v >>> 24);
        out[o + 5] = (byte) (v >>> 16);
        out[o + 6] = (byte) (v >>> 8);
        out[o + 7] = (byte) v;
    }
    // RIPEMD-160 (Dobbertin, Bosselaers, Preneel), unrolled: both lines of
    // 80 steps run on locals, and the sixteen message words are read once.
    static final class Ripemd160 extends Block64 {
        int h0;
        int h1;
        int h2;
        int h3;
        int h4;

        Ripemd160() {
            reset();
        }

        @Override
        public void reset() {
            h0 = 0x67452301;
            h1 = 0xefcdab89;
            h2 = 0x98badcfe;
            h3 = 0x10325476;
            h4 = 0xc3d2e1f0;
            bufferLen = 0;
            byteCount = 0;
        }

        @Override
        public int digestLength() {
            return 20;
        }

        @Override
        public byte[] digest() {
            return finishCommon(false);
        }

        @Override
        void writeStateBigEndian(byte[] out) {
            // little-endian, like MD5; the shared finish path keeps the name
            writeLE(out, 0, h0);
            writeLE(out, 4, h1);
            writeLE(out, 8, h2);
            writeLE(out, 12, h3);
            writeLE(out, 16, h4);
        }

        private static void writeLE(byte[] out, int o, int v) {
            out[o] = (byte) v;
            out[o + 1] = (byte) (v >>> 8);
            out[o + 2] = (byte) (v >>> 16);
            out[o + 3] = (byte) (v >>> 24);
        }

        final int[] x = new int[16];
        final int[] lr = new int[5];

        @Override
        void processBlock(byte[] block, int o) {
            int[] x = this.x;
            for (int i = 0; i < 16; i++, o += 4) {
                x[i] = (block[o] & 0xff) | (block[o + 1] & 0xff) << 8 | (block[o + 2] & 0xff) << 16 | block[o + 3] << 24;
            }
            int[] l = lr;
            left(x, h0, h1, h2, h3, h4, l);
            int la = l[0];
            int lb = l[1];
            int lc = l[2];
            int ld = l[3];
            int le = l[4];
            right(x, h0, h1, h2, h3, h4, l);
            int t = h1 + lc + l[3];
            h1 = h2 + ld + l[4];
            h2 = h3 + le + l[0];
            h3 = h4 + la + l[1];
            h4 = h0 + lb + l[2];
            h0 = t;
        }

        // The two lines are separate methods because one method holding all
        // 160 steps is past HotSpot's limit for JIT compilation and runs
        // interpreted, seven times slower.
        private static void left(int[] x, int la, int lb, int lc, int ld, int le, int[] out) {
            int t;
            t = la + (lb ^ lc ^ ld) + x[0]; la = ((t << 11) | (t >>> 21)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + (la ^ lb ^ lc) + x[1]; le = ((t << 14) | (t >>> 18)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + (le ^ la ^ lb) + x[2]; ld = ((t << 15) | (t >>> 17)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + (ld ^ le ^ la) + x[3]; lc = ((t << 12) | (t >>> 20)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ ld ^ le) + x[4]; lb = ((t << 5) | (t >>> 27)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + (lb ^ lc ^ ld) + x[5]; la = ((t << 8) | (t >>> 24)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + (la ^ lb ^ lc) + x[6]; le = ((t << 7) | (t >>> 25)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + (le ^ la ^ lb) + x[7]; ld = ((t << 9) | (t >>> 23)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + (ld ^ le ^ la) + x[8]; lc = ((t << 11) | (t >>> 21)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ ld ^ le) + x[9]; lb = ((t << 13) | (t >>> 19)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + (lb ^ lc ^ ld) + x[10]; la = ((t << 14) | (t >>> 18)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + (la ^ lb ^ lc) + x[11]; le = ((t << 15) | (t >>> 17)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + (le ^ la ^ lb) + x[12]; ld = ((t << 6) | (t >>> 26)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + (ld ^ le ^ la) + x[13]; lc = ((t << 7) | (t >>> 25)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ ld ^ le) + x[14]; lb = ((t << 9) | (t >>> 23)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + (lb ^ lc ^ ld) + x[15]; la = ((t << 8) | (t >>> 24)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lb) | (~la & lc)) + x[7] + 0x5a827999; le = ((t << 7) | (t >>> 25)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le & la) | (~le & lb)) + x[4] + 0x5a827999; ld = ((t << 6) | (t >>> 26)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & le) | (~ld & la)) + x[13] + 0x5a827999; lc = ((t << 8) | (t >>> 24)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc & ld) | (~lc & le)) + x[1] + 0x5a827999; lb = ((t << 13) | (t >>> 19)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb & lc) | (~lb & ld)) + x[10] + 0x5a827999; la = ((t << 11) | (t >>> 21)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lb) | (~la & lc)) + x[6] + 0x5a827999; le = ((t << 9) | (t >>> 23)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le & la) | (~le & lb)) + x[15] + 0x5a827999; ld = ((t << 7) | (t >>> 25)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & le) | (~ld & la)) + x[3] + 0x5a827999; lc = ((t << 15) | (t >>> 17)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc & ld) | (~lc & le)) + x[12] + 0x5a827999; lb = ((t << 7) | (t >>> 25)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb & lc) | (~lb & ld)) + x[0] + 0x5a827999; la = ((t << 12) | (t >>> 20)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lb) | (~la & lc)) + x[9] + 0x5a827999; le = ((t << 15) | (t >>> 17)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le & la) | (~le & lb)) + x[5] + 0x5a827999; ld = ((t << 9) | (t >>> 23)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & le) | (~ld & la)) + x[2] + 0x5a827999; lc = ((t << 11) | (t >>> 21)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc & ld) | (~lc & le)) + x[14] + 0x5a827999; lb = ((t << 7) | (t >>> 25)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb & lc) | (~lb & ld)) + x[11] + 0x5a827999; la = ((t << 13) | (t >>> 19)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lb) | (~la & lc)) + x[8] + 0x5a827999; le = ((t << 12) | (t >>> 20)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le | ~la) ^ lb) + x[3] + 0x6ed9eba1; ld = ((t << 11) | (t >>> 21)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld | ~le) ^ la) + x[10] + 0x6ed9eba1; lc = ((t << 13) | (t >>> 19)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc | ~ld) ^ le) + x[14] + 0x6ed9eba1; lb = ((t << 6) | (t >>> 26)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb | ~lc) ^ ld) + x[4] + 0x6ed9eba1; la = ((t << 7) | (t >>> 25)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la | ~lb) ^ lc) + x[9] + 0x6ed9eba1; le = ((t << 14) | (t >>> 18)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le | ~la) ^ lb) + x[15] + 0x6ed9eba1; ld = ((t << 9) | (t >>> 23)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld | ~le) ^ la) + x[8] + 0x6ed9eba1; lc = ((t << 13) | (t >>> 19)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc | ~ld) ^ le) + x[1] + 0x6ed9eba1; lb = ((t << 15) | (t >>> 17)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb | ~lc) ^ ld) + x[2] + 0x6ed9eba1; la = ((t << 14) | (t >>> 18)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la | ~lb) ^ lc) + x[7] + 0x6ed9eba1; le = ((t << 8) | (t >>> 24)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le | ~la) ^ lb) + x[0] + 0x6ed9eba1; ld = ((t << 13) | (t >>> 19)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld | ~le) ^ la) + x[6] + 0x6ed9eba1; lc = ((t << 6) | (t >>> 26)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc | ~ld) ^ le) + x[13] + 0x6ed9eba1; lb = ((t << 5) | (t >>> 27)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb | ~lc) ^ ld) + x[11] + 0x6ed9eba1; la = ((t << 12) | (t >>> 20)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la | ~lb) ^ lc) + x[5] + 0x6ed9eba1; le = ((t << 7) | (t >>> 25)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le | ~la) ^ lb) + x[12] + 0x6ed9eba1; ld = ((t << 5) | (t >>> 27)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & la) | (le & ~la)) + x[1] + 0x8f1bbcdc; lc = ((t << 11) | (t >>> 21)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc & le) | (ld & ~le)) + x[9] + 0x8f1bbcdc; lb = ((t << 12) | (t >>> 20)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb & ld) | (lc & ~ld)) + x[11] + 0x8f1bbcdc; la = ((t << 14) | (t >>> 18)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lc) | (lb & ~lc)) + x[10] + 0x8f1bbcdc; le = ((t << 15) | (t >>> 17)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le & lb) | (la & ~lb)) + x[0] + 0x8f1bbcdc; ld = ((t << 14) | (t >>> 18)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & la) | (le & ~la)) + x[8] + 0x8f1bbcdc; lc = ((t << 15) | (t >>> 17)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc & le) | (ld & ~le)) + x[12] + 0x8f1bbcdc; lb = ((t << 9) | (t >>> 23)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb & ld) | (lc & ~ld)) + x[4] + 0x8f1bbcdc; la = ((t << 8) | (t >>> 24)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lc) | (lb & ~lc)) + x[13] + 0x8f1bbcdc; le = ((t << 9) | (t >>> 23)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le & lb) | (la & ~lb)) + x[3] + 0x8f1bbcdc; ld = ((t << 14) | (t >>> 18)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & la) | (le & ~la)) + x[7] + 0x8f1bbcdc; lc = ((t << 5) | (t >>> 27)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + ((lc & le) | (ld & ~le)) + x[15] + 0x8f1bbcdc; lb = ((t << 6) | (t >>> 26)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + ((lb & ld) | (lc & ~ld)) + x[14] + 0x8f1bbcdc; la = ((t << 8) | (t >>> 24)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + ((la & lc) | (lb & ~lc)) + x[5] + 0x8f1bbcdc; le = ((t << 6) | (t >>> 26)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + ((le & lb) | (la & ~lb)) + x[6] + 0x8f1bbcdc; ld = ((t << 5) | (t >>> 27)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + ((ld & la) | (le & ~la)) + x[2] + 0x8f1bbcdc; lc = ((t << 12) | (t >>> 20)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ (ld | ~le)) + x[4] + 0xa953fd4e; lb = ((t << 9) | (t >>> 23)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + (lb ^ (lc | ~ld)) + x[0] + 0xa953fd4e; la = ((t << 15) | (t >>> 17)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + (la ^ (lb | ~lc)) + x[5] + 0xa953fd4e; le = ((t << 5) | (t >>> 27)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + (le ^ (la | ~lb)) + x[9] + 0xa953fd4e; ld = ((t << 11) | (t >>> 21)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + (ld ^ (le | ~la)) + x[7] + 0xa953fd4e; lc = ((t << 6) | (t >>> 26)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ (ld | ~le)) + x[12] + 0xa953fd4e; lb = ((t << 8) | (t >>> 24)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + (lb ^ (lc | ~ld)) + x[2] + 0xa953fd4e; la = ((t << 13) | (t >>> 19)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + (la ^ (lb | ~lc)) + x[10] + 0xa953fd4e; le = ((t << 12) | (t >>> 20)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + (le ^ (la | ~lb)) + x[14] + 0xa953fd4e; ld = ((t << 5) | (t >>> 27)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + (ld ^ (le | ~la)) + x[1] + 0xa953fd4e; lc = ((t << 12) | (t >>> 20)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ (ld | ~le)) + x[3] + 0xa953fd4e; lb = ((t << 13) | (t >>> 19)) + la; ld = (ld << 10) | (ld >>> 22);
            t = la + (lb ^ (lc | ~ld)) + x[8] + 0xa953fd4e; la = ((t << 14) | (t >>> 18)) + le; lc = (lc << 10) | (lc >>> 22);
            t = le + (la ^ (lb | ~lc)) + x[11] + 0xa953fd4e; le = ((t << 11) | (t >>> 21)) + ld; lb = (lb << 10) | (lb >>> 22);
            t = ld + (le ^ (la | ~lb)) + x[6] + 0xa953fd4e; ld = ((t << 8) | (t >>> 24)) + lc; la = (la << 10) | (la >>> 22);
            t = lc + (ld ^ (le | ~la)) + x[15] + 0xa953fd4e; lc = ((t << 5) | (t >>> 27)) + lb; le = (le << 10) | (le >>> 22);
            t = lb + (lc ^ (ld | ~le)) + x[13] + 0xa953fd4e; lb = ((t << 6) | (t >>> 26)) + la; ld = (ld << 10) | (ld >>> 22);
            out[0] = la;
            out[1] = lb;
            out[2] = lc;
            out[3] = ld;
            out[4] = le;
        }

        private static void right(int[] x, int ra, int rb, int rc, int rd, int re, int[] out) {
            int t;
            t = ra + (rb ^ (rc | ~rd)) + x[5] + 0x50a28be6; ra = ((t << 8) | (t >>> 24)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + (ra ^ (rb | ~rc)) + x[14] + 0x50a28be6; re = ((t << 9) | (t >>> 23)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + (re ^ (ra | ~rb)) + x[7] + 0x50a28be6; rd = ((t << 9) | (t >>> 23)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + (rd ^ (re | ~ra)) + x[0] + 0x50a28be6; rc = ((t << 11) | (t >>> 21)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ (rd | ~re)) + x[9] + 0x50a28be6; rb = ((t << 13) | (t >>> 19)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + (rb ^ (rc | ~rd)) + x[2] + 0x50a28be6; ra = ((t << 15) | (t >>> 17)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + (ra ^ (rb | ~rc)) + x[11] + 0x50a28be6; re = ((t << 15) | (t >>> 17)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + (re ^ (ra | ~rb)) + x[4] + 0x50a28be6; rd = ((t << 5) | (t >>> 27)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + (rd ^ (re | ~ra)) + x[13] + 0x50a28be6; rc = ((t << 7) | (t >>> 25)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ (rd | ~re)) + x[6] + 0x50a28be6; rb = ((t << 7) | (t >>> 25)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + (rb ^ (rc | ~rd)) + x[15] + 0x50a28be6; ra = ((t << 8) | (t >>> 24)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + (ra ^ (rb | ~rc)) + x[8] + 0x50a28be6; re = ((t << 11) | (t >>> 21)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + (re ^ (ra | ~rb)) + x[1] + 0x50a28be6; rd = ((t << 14) | (t >>> 18)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + (rd ^ (re | ~ra)) + x[10] + 0x50a28be6; rc = ((t << 14) | (t >>> 18)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ (rd | ~re)) + x[3] + 0x50a28be6; rb = ((t << 12) | (t >>> 20)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + (rb ^ (rc | ~rd)) + x[12] + 0x50a28be6; ra = ((t << 6) | (t >>> 26)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rc) | (rb & ~rc)) + x[6] + 0x5c4dd124; re = ((t << 9) | (t >>> 23)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re & rb) | (ra & ~rb)) + x[11] + 0x5c4dd124; rd = ((t << 13) | (t >>> 19)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & ra) | (re & ~ra)) + x[3] + 0x5c4dd124; rc = ((t << 15) | (t >>> 17)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc & re) | (rd & ~re)) + x[7] + 0x5c4dd124; rb = ((t << 7) | (t >>> 25)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb & rd) | (rc & ~rd)) + x[0] + 0x5c4dd124; ra = ((t << 12) | (t >>> 20)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rc) | (rb & ~rc)) + x[13] + 0x5c4dd124; re = ((t << 8) | (t >>> 24)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re & rb) | (ra & ~rb)) + x[5] + 0x5c4dd124; rd = ((t << 9) | (t >>> 23)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & ra) | (re & ~ra)) + x[10] + 0x5c4dd124; rc = ((t << 11) | (t >>> 21)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc & re) | (rd & ~re)) + x[14] + 0x5c4dd124; rb = ((t << 7) | (t >>> 25)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb & rd) | (rc & ~rd)) + x[15] + 0x5c4dd124; ra = ((t << 7) | (t >>> 25)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rc) | (rb & ~rc)) + x[8] + 0x5c4dd124; re = ((t << 12) | (t >>> 20)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re & rb) | (ra & ~rb)) + x[12] + 0x5c4dd124; rd = ((t << 7) | (t >>> 25)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & ra) | (re & ~ra)) + x[4] + 0x5c4dd124; rc = ((t << 6) | (t >>> 26)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc & re) | (rd & ~re)) + x[9] + 0x5c4dd124; rb = ((t << 15) | (t >>> 17)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb & rd) | (rc & ~rd)) + x[1] + 0x5c4dd124; ra = ((t << 13) | (t >>> 19)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rc) | (rb & ~rc)) + x[2] + 0x5c4dd124; re = ((t << 11) | (t >>> 21)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re | ~ra) ^ rb) + x[15] + 0x6d703ef3; rd = ((t << 9) | (t >>> 23)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd | ~re) ^ ra) + x[5] + 0x6d703ef3; rc = ((t << 7) | (t >>> 25)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc | ~rd) ^ re) + x[1] + 0x6d703ef3; rb = ((t << 15) | (t >>> 17)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb | ~rc) ^ rd) + x[3] + 0x6d703ef3; ra = ((t << 11) | (t >>> 21)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra | ~rb) ^ rc) + x[7] + 0x6d703ef3; re = ((t << 8) | (t >>> 24)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re | ~ra) ^ rb) + x[14] + 0x6d703ef3; rd = ((t << 6) | (t >>> 26)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd | ~re) ^ ra) + x[6] + 0x6d703ef3; rc = ((t << 6) | (t >>> 26)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc | ~rd) ^ re) + x[9] + 0x6d703ef3; rb = ((t << 14) | (t >>> 18)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb | ~rc) ^ rd) + x[11] + 0x6d703ef3; ra = ((t << 12) | (t >>> 20)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra | ~rb) ^ rc) + x[8] + 0x6d703ef3; re = ((t << 13) | (t >>> 19)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re | ~ra) ^ rb) + x[12] + 0x6d703ef3; rd = ((t << 5) | (t >>> 27)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd | ~re) ^ ra) + x[2] + 0x6d703ef3; rc = ((t << 14) | (t >>> 18)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc | ~rd) ^ re) + x[10] + 0x6d703ef3; rb = ((t << 13) | (t >>> 19)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb | ~rc) ^ rd) + x[0] + 0x6d703ef3; ra = ((t << 13) | (t >>> 19)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra | ~rb) ^ rc) + x[4] + 0x6d703ef3; re = ((t << 7) | (t >>> 25)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re | ~ra) ^ rb) + x[13] + 0x6d703ef3; rd = ((t << 5) | (t >>> 27)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & re) | (~rd & ra)) + x[8] + 0x7a6d76e9; rc = ((t << 15) | (t >>> 17)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc & rd) | (~rc & re)) + x[6] + 0x7a6d76e9; rb = ((t << 5) | (t >>> 27)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb & rc) | (~rb & rd)) + x[4] + 0x7a6d76e9; ra = ((t << 8) | (t >>> 24)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rb) | (~ra & rc)) + x[1] + 0x7a6d76e9; re = ((t << 11) | (t >>> 21)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re & ra) | (~re & rb)) + x[3] + 0x7a6d76e9; rd = ((t << 14) | (t >>> 18)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & re) | (~rd & ra)) + x[11] + 0x7a6d76e9; rc = ((t << 14) | (t >>> 18)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc & rd) | (~rc & re)) + x[15] + 0x7a6d76e9; rb = ((t << 6) | (t >>> 26)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb & rc) | (~rb & rd)) + x[0] + 0x7a6d76e9; ra = ((t << 14) | (t >>> 18)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rb) | (~ra & rc)) + x[5] + 0x7a6d76e9; re = ((t << 6) | (t >>> 26)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re & ra) | (~re & rb)) + x[12] + 0x7a6d76e9; rd = ((t << 9) | (t >>> 23)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & re) | (~rd & ra)) + x[2] + 0x7a6d76e9; rc = ((t << 12) | (t >>> 20)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + ((rc & rd) | (~rc & re)) + x[13] + 0x7a6d76e9; rb = ((t << 9) | (t >>> 23)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + ((rb & rc) | (~rb & rd)) + x[9] + 0x7a6d76e9; ra = ((t << 12) | (t >>> 20)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + ((ra & rb) | (~ra & rc)) + x[7] + 0x7a6d76e9; re = ((t << 5) | (t >>> 27)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + ((re & ra) | (~re & rb)) + x[10] + 0x7a6d76e9; rd = ((t << 15) | (t >>> 17)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + ((rd & re) | (~rd & ra)) + x[14] + 0x7a6d76e9; rc = ((t << 8) | (t >>> 24)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ rd ^ re) + x[12]; rb = ((t << 8) | (t >>> 24)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + (rb ^ rc ^ rd) + x[15]; ra = ((t << 5) | (t >>> 27)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + (ra ^ rb ^ rc) + x[10]; re = ((t << 12) | (t >>> 20)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + (re ^ ra ^ rb) + x[4]; rd = ((t << 9) | (t >>> 23)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + (rd ^ re ^ ra) + x[1]; rc = ((t << 12) | (t >>> 20)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ rd ^ re) + x[5]; rb = ((t << 5) | (t >>> 27)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + (rb ^ rc ^ rd) + x[8]; ra = ((t << 14) | (t >>> 18)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + (ra ^ rb ^ rc) + x[7]; re = ((t << 6) | (t >>> 26)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + (re ^ ra ^ rb) + x[6]; rd = ((t << 8) | (t >>> 24)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + (rd ^ re ^ ra) + x[2]; rc = ((t << 13) | (t >>> 19)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ rd ^ re) + x[13]; rb = ((t << 6) | (t >>> 26)) + ra; rd = (rd << 10) | (rd >>> 22);
            t = ra + (rb ^ rc ^ rd) + x[14]; ra = ((t << 5) | (t >>> 27)) + re; rc = (rc << 10) | (rc >>> 22);
            t = re + (ra ^ rb ^ rc) + x[0]; re = ((t << 15) | (t >>> 17)) + rd; rb = (rb << 10) | (rb >>> 22);
            t = rd + (re ^ ra ^ rb) + x[3]; rd = ((t << 13) | (t >>> 19)) + rc; ra = (ra << 10) | (ra >>> 22);
            t = rc + (rd ^ re ^ ra) + x[9]; rc = ((t << 11) | (t >>> 21)) + rb; re = (re << 10) | (re >>> 22);
            t = rb + (rc ^ rd ^ re) + x[11]; rb = ((t << 11) | (t >>> 21)) + ra; rd = (rd << 10) | (rd >>> 22);
            out[0] = ra;
            out[1] = rb;
            out[2] = rc;
            out[3] = rd;
            out[4] = re;
        }
    }
}
