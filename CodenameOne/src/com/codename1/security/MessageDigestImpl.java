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
        if (a.equals("MD5")) return new Md5();
        if (a.equals("SHA1")) return new Sha1();
        if (a.equals("SHA224")) return new Sha2_32(true);
        if (a.equals("SHA256")) return new Sha2_32(false);
        if (a.equals("SHA384")) return new Sha2_64(true);
        if (a.equals("SHA512")) return new Sha2_64(false);
        throw new CryptoException("unsupported hash algorithm: " + algorithm);
    }

    static String normalise(String algorithm) {
        StringBuilder b = new StringBuilder(algorithm.length());
        for (int i = 0; i < algorithm.length(); i++) {
            char c = algorithm.charAt(i);
            if (c == '-' || c == '_' || c == ' ') continue;
            if (c >= 'a' && c <= 'z') c = (char) (c - 'a' + 'A');
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

        void update(byte[] data, int offset, int length) {
            byteCount += length;
            if (bufferLen > 0) {
                int copy = 64 - bufferLen;
                if (copy > length) copy = length;
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
                while (bufferLen < 64) buffer[bufferLen++] = 0;
                processBlock(buffer, 0);
                bufferLen = 0;
            }
            while (bufferLen < 56) buffer[bufferLen++] = 0;
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
        int a, b, c, d;

        Md5() { reset(); }

        public void reset() {
            a = 0x67452301;
            b = 0xefcdab89;
            c = 0x98badcfe;
            d = 0x10325476;
            bufferLen = 0;
            byteCount = 0;
        }

        public int digestLength() { return 16; }

        public byte[] digest() { return finishCommon(false); }

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

        private static int rol(int v, int s) { return (v << s) | (v >>> (32 - s)); }

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

            int aa = a, bb = b, cc = c, dd = d;

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
        int h0, h1, h2, h3, h4;
        final int[] w = new int[80];

        Sha1() { reset(); }

        public void reset() {
            h0 = 0x67452301;
            h1 = 0xEFCDAB89;
            h2 = 0x98BADCFE;
            h3 = 0x10325476;
            h4 = 0xC3D2E1F0;
            bufferLen = 0;
            byteCount = 0;
        }

        public int digestLength() { return 20; }

        public byte[] digest() { return finishCommon(true); }

        void writeStateBigEndian(byte[] out) {
            writeBE(out, 0, h0);
            writeBE(out, 4, h1);
            writeBE(out, 8, h2);
            writeBE(out, 12, h3);
            writeBE(out, 16, h4);
        }

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
            int a = h0, b = h1, c = h2, d = h3, e = h4;
            for (int i = 0; i < 80; i++) {
                int f, k;
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
    static final class Sha2_32 extends Block64 {
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
        private int h0, h1, h2, h3, h4, h5, h6, h7;
        private final int[] w = new int[64];

        Sha2_32(boolean truncated) {
            this.truncated = truncated;
            reset();
        }

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

        public int digestLength() { return truncated ? 28 : 32; }

        public byte[] digest() { return finishCommon(true); }

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

        void processBlock(byte[] block, int o) {
            for (int i = 0; i < 16; i++) {
                w[i] = (block[o + i * 4] & 0xff) << 24
                     | (block[o + i * 4 + 1] & 0xff) << 16
                     | (block[o + i * 4 + 2] & 0xff) << 8
                     | (block[o + i * 4 + 3] & 0xff);
            }
            for (int i = 16; i < 64; i++) {
                int v15 = w[i - 15];
                int s0 = ((v15 >>> 7) | (v15 << 25)) ^ ((v15 >>> 18) | (v15 << 14)) ^ (v15 >>> 3);
                int v2 = w[i - 2];
                int s1 = ((v2 >>> 17) | (v2 << 15)) ^ ((v2 >>> 19) | (v2 << 13)) ^ (v2 >>> 10);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }
            int a = h0, b = h1, c = h2, d = h3, e = h4, f = h5, g = h6, h = h7;
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
            h0 += a; h1 += b; h2 += c; h3 += d;
            h4 += e; h5 += f; h6 += g; h7 += h;
        }
    }

    // ===============================================================
    // SHA-384 / SHA-512 -- FIPS 180-4 (64-bit word version, 128-byte blocks,
    // 128-bit length field). Uses a 128-byte block engine.
    static final class Sha2_64 extends MessageDigestImpl {
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
        private long h0, h1, h2, h3, h4, h5, h6, h7;
        private final byte[] buffer = new byte[128];
        private int bufferLen;
        private long byteCount; // we cap message length at 2^63-1 bytes which is plenty
        private final long[] w = new long[80];

        Sha2_64(boolean truncated) {
            this.truncated = truncated;
            reset();
        }

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

        public int digestLength() { return truncated ? 48 : 64; }

        void update(byte[] data, int offset, int length) {
            byteCount += length;
            if (bufferLen > 0) {
                int copy = 128 - bufferLen;
                if (copy > length) copy = length;
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

        void update(byte b) {
            byteCount++;
            buffer[bufferLen++] = b;
            if (bufferLen == 128) {
                processBlock(buffer, 0);
                bufferLen = 0;
            }
        }

        public byte[] digest() {
            long bits = byteCount * 8L;
            buffer[bufferLen++] = (byte) 0x80;
            if (bufferLen > 112) {
                while (bufferLen < 128) buffer[bufferLen++] = 0;
                processBlock(buffer, 0);
                bufferLen = 0;
            }
            while (bufferLen < 112) buffer[bufferLen++] = 0;
            // high 64 bits of the 128-bit length field are always 0 here since
            // a Java byte array cannot hold more than 2^31-1 bytes.
            for (int i = 112; i < 120; i++) buffer[i] = 0;
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
            if (truncated) {
                // sha-384 omits h6, h7
            } else {
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
            long a = h0, b = h1, c = h2, d = h3, e = h4, f = h5, g = h6, h = h7;
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
            h0 += a; h1 += b; h2 += c; h3 += d;
            h4 += e; h5 += f; h6 += g; h7 += h;
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
}
