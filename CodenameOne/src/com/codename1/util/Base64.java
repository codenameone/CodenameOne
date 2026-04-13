/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/// @author Alexander Y. Kleymenov

package com.codename1.util;

import com.codename1.annotations.DisableDebugInfo;
import com.codename1.annotations.DisableNullChecksAndArrayBoundsChecks;


/// This class implements Base64 encoding/decoding functionality
/// as specified in RFC 2045 (http://www.ietf.org/rfc/rfc2045.txt).
public abstract class Base64 {

    private static final int DECODE_INVALID = -1;
    private static final int DECODE_WHITESPACE = -2;

    private static final byte[] map = new byte[]
            {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
                    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
                    'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
                    'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
                    '4', '5', '6', '7', '8', '9', '+', '/'};

    private static final byte[] decodeMap = new byte[256];
    private static final int[] decodeMapInt = new int[256];
    private static final int SIMD_LANES = 16;
    private static final int SIMD_SCRATCH_INTS = 192;

    static {
        for (int i = 0; i < decodeMap.length; i++) {
            decodeMap[i] = (byte) DECODE_INVALID;
            decodeMapInt[i] = DECODE_INVALID;
        }
        for (int i = 0; i < map.length; i++) {
            decodeMap[map[i] & 0xff] = (byte) i;
            decodeMapInt[map[i] & 0xff] = i;
        }
        decodeMap['\n'] = (byte) DECODE_WHITESPACE;
        decodeMap['\r'] = (byte) DECODE_WHITESPACE;
        decodeMap[' '] = (byte) DECODE_WHITESPACE;
        decodeMap['\t'] = (byte) DECODE_WHITESPACE;
        decodeMapInt['\n'] = DECODE_WHITESPACE;
        decodeMapInt['\r'] = DECODE_WHITESPACE;
        decodeMapInt[' '] = DECODE_WHITESPACE;
        decodeMapInt['\t'] = DECODE_WHITESPACE;
    }

    public static byte[] decode(byte[] in) {
        return decode(in, in.length);
    }

    /// Decodes an array of bytes containing a Base64 ascii string into binary data
    ///
    /// #### Parameters
    ///
    /// - `in`: the array
    ///
    /// - `len`: the length of the array
    ///
    /// #### Returns
    ///
    /// the decoded array
    public static byte[] decode(byte[] in, int len) {
        if (len == 0) {
            return new byte[0];
        }
        int maxOutputLength = (len / 4) * 3 + 3;
        byte[] out = allocByteMaybeSimd(maxOutputLength);
        int outputLength = decode(in, len, out);
        if (outputLength < 0) {
            return null;
        }
        if (outputLength == out.length) {
            return out;
        }
        byte[] trimmed = allocByteMaybeSimd(outputLength);
        System.arraycopy(out, 0, trimmed, 0, outputLength);
        return trimmed;
    }

    /// Decodes Base64 input into a caller-provided output buffer.
    ///
    /// #### Parameters
    ///
    /// - `in`: Base64 bytes
    ///
    /// - `len`: bytes from `in` to decode
    ///
    /// - `out`: destination buffer
    ///
    /// #### Returns
    ///
    /// decoded length, or `-1` for invalid Base64
    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    public static int decode(byte[] in, int len, byte[] out) {
        if (len == 0) {
            return 0;
        }
        if ((len & 0x3) == 0) {
            int fastLength = decodeNoWhitespace(in, len, out);
            if (fastLength >= 0) {
                return fastLength;
            }
        }
        int pad = 0;
        int end = len;
        while (end > 0) {
            int chr = in[end - 1] & 0xff;
            if (decodeMapInt[chr] == DECODE_WHITESPACE) {
                end--;
                continue;
            }
            if (chr == '=') {
                pad++;
                end--;
            } else {
                break;
            }
        }

        int validChars = 0;
        for (int i = 0; i < end; i++) {
            int chr = in[i] & 0xff;
            if (chr == '=') {
                break;
            }
            int value = decodeMapInt[chr];
            if (value == DECODE_WHITESPACE) {
                continue;
            }
            if (value == DECODE_INVALID) {
                return -1;
            }
            validChars++;
        }

        int totalSymbols = validChars + pad;
        int outputLength = (totalSymbols / 4) * 3 - pad;
        if (outputLength <= 0) {
            return 0;
        }
        if (out.length < outputLength) {
            throw new IllegalArgumentException("Output buffer too small for decoded data");
        }
        int outIndex = 0;

        int quantum = 0;
        int quantumChars = 0;
        for (int i = 0; i < end; i++) {
            int chr = in[i] & 0xff;
            if (chr == '=') {
                break;
            }
            int bits = decodeMapInt[chr];
            if (bits == DECODE_WHITESPACE) {
                continue;
            }
            if (bits == DECODE_INVALID) {
                return -1;
            }
            quantum = (quantum << 6) | bits;
            quantumChars++;
            if (quantumChars == 4) {
                out[outIndex++] = (byte) ((quantum & 0x00FF0000) >> 16);
                out[outIndex++] = (byte) ((quantum & 0x0000FF00) >> 8);
                out[outIndex++] = (byte) (quantum & 0x000000FF);
                quantumChars = 0;
                quantum = 0;
            }
        }

        if (pad > 0) {
            if ((pad == 1 && quantumChars != 3) || (pad == 2 && quantumChars != 2)) {
                return -1;
            }
            quantum = quantum << (6 * pad);
            out[outIndex++] = (byte) ((quantum & 0x00FF0000) >> 16);
            if (pad == 1) {
                out[outIndex++] = (byte) ((quantum & 0x0000FF00) >> 8);
            }
        }

        return outIndex;
    }

    public static int decode(byte[] in, byte[] out) {
        return decode(in, in.length, out);
    }

    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    private static int decodeNoWhitespace(byte[] in, int len, byte[] out) {
        if ((len & 0x3) != 0) {
            return -1;
        }
        int pad = 0;
        if (len > 0 && in[len - 1] == '=') {
            pad++;
            if (len > 1 && in[len - 2] == '=') {
                pad++;
            }
        }
        if (pad > 2) {
            return -1;
        }

        int outLength = (len / 4) * 3 - pad;
        if (outLength <= 0) {
            return 0;
        }
        if (out.length < outLength) {
            throw new IllegalArgumentException("Output buffer too small for decoded data");
        }
        int outIndex = 0;
        int fullLen = len - (pad > 0 ? 4 : 0);
        int[] decodeMapLocal = decodeMapInt;
        int simdFullLen = 0;

        for (int i = simdFullLen; i < fullLen; i += 4) {
            int c0 = in[i] & 0xff;
            int c1 = in[i + 1] & 0xff;
            int c2 = in[i + 2] & 0xff;
            int c3 = in[i + 3] & 0xff;
            int b0 = decodeMapLocal[c0];
            int b1 = decodeMapLocal[c1];
            int b2 = decodeMapLocal[c2];
            int b3 = decodeMapLocal[c3];
            if ((b0 | b1 | b2 | b3) < 0) {
                return -1;
            }
            int quantum = (b0 << 18) | (b1 << 12) | (b2 << 6) | b3;
            out[outIndex++] = (byte) ((quantum >> 16) & 0xff);
            out[outIndex++] = (byte) ((quantum >> 8) & 0xff);
            out[outIndex++] = (byte) (quantum & 0xff);
        }

        if (pad == 0) {
            return outIndex;
        }

        int i = len - 4;
        int c0 = in[i] & 0xff;
        int c1 = in[i + 1] & 0xff;
        int b0 = decodeMapLocal[c0];
        int b1 = decodeMapLocal[c1];
        if ((b0 | b1) < 0) {
            return -1;
        }
        out[outIndex++] = (byte) ((b0 << 2) | (b1 >> 4));
        if (pad == 2) {
            return (in[i + 2] == '=' && in[i + 3] == '=') ? outIndex : -1;
        }

        if (in[i + 3] != '=') {
            return -1;
        }
        int b2 = decodeMapLocal[in[i + 2] & 0xff];
        if (b2 < 0) {
            return -1;
        }
        out[outIndex] = (byte) ((b1 << 4) | (b2 >> 2));
        return outLength;
    }

    /// Encodes the given array as a base64 string
    ///
    /// #### Parameters
    ///
    /// - `in`: the array to encode
    ///
    /// #### Returns
    ///
    /// the String containing the array
    public static String encode(byte[] in) {
        int length = in.length * 4 / 3;
        length += length / 76 + 3; // for crlr
        byte[] out = new byte[length];
        int index = 0;
        int i;
        int crlr = 0;
        int end = in.length - in.length % 3;
        for (i = 0; i < end; i += 3) {
            out[index++] = map[(in[i] & 0xff) >> 2];
            out[index++] = map[((in[i] & 0x03) << 4)
                    | ((in[i + 1] & 0xff) >> 4)];
            out[index++] = map[((in[i + 1] & 0x0f) << 2)
                    | ((in[i + 2] & 0xff) >> 6)];
            out[index++] = map[(in[i + 2] & 0x3f)];
            if (((index - crlr) % 76 == 0) && (index != 0)) {
                out[index++] = '\n';
                crlr++;
                //out[index++] = '\r';
                //crlr++;
            }
        }
        switch (in.length % 3) {
            case 1:
                out[index++] = map[(in[end] & 0xff) >> 2];
                out[index++] = map[(in[end] & 0x03) << 4];
                out[index++] = '=';
                out[index++] = '=';
                break;
            case 2:
                out[index++] = map[(in[end] & 0xff) >> 2];
                out[index++] = map[((in[end] & 0x03) << 4)
                        | ((in[end + 1] & 0xff) >> 4)];
                out[index++] = map[((in[end + 1] & 0x0f) << 2)];
                out[index++] = '=';
                break;
            default:
                break;
        }
        return com.codename1.util.StringUtil.newString(out, 0, index);
    }

    /// Encodes the given array as a base64 string without breaking lines
    ///
    /// #### Parameters
    ///
    /// - `in`: the array to encode
    ///
    /// #### Returns
    ///
    /// the String containing the array
    public static String encodeNoNewline(byte[] in) {
        int inputLength = in.length;
        if (inputLength == 0) {
            return "";
        }
        int outputLength = ((inputLength + 2) / 3) * 4;
        byte[] out = allocByteMaybeSimd(outputLength);
        encodeNoNewline(in, out);
        return com.codename1.util.StringUtil.newString(out, 0, outputLength);
    }

    /// Encodes input into a caller-provided output buffer without line breaks.
    ///
    /// #### Parameters
    ///
    /// - `in`: input bytes
    ///
    /// - `out`: destination buffer
    ///
    /// #### Returns
    ///
    /// number of bytes written to `out`
    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    public static int encodeNoNewline(byte[] in, byte[] out) {
        int inputLength = in.length;
        int outputLength = ((inputLength + 2) / 3) * 4;
        if (out.length < outputLength) {
            throw new IllegalArgumentException("Output buffer too small for encoded data");
        }
        if (inputLength == 0) {
            return 0;
        }
        byte[] mapLocal = map;
        int end = inputLength - (inputLength % 3);
        int outIndex = 0;
        int i = 0;
        int fastEnd = end - 12;
        for (; i <= fastEnd; i += 12) {
            int b0 = in[i] & 0xff;
            int b1 = in[i + 1] & 0xff;
            int b2 = in[i + 2] & 0xff;
            int b3 = in[i + 3] & 0xff;
            int b4 = in[i + 4] & 0xff;
            int b5 = in[i + 5] & 0xff;
            int b6 = in[i + 6] & 0xff;
            int b7 = in[i + 7] & 0xff;
            int b8 = in[i + 8] & 0xff;
            int b9 = in[i + 9] & 0xff;
            int b10 = in[i + 10] & 0xff;
            int b11 = in[i + 11] & 0xff;

            out[outIndex++] = mapLocal[b0 >> 2];
            out[outIndex++] = mapLocal[((b0 & 0x03) << 4) | (b1 >> 4)];
            out[outIndex++] = mapLocal[((b1 & 0x0f) << 2) | (b2 >> 6)];
            out[outIndex++] = mapLocal[b2 & 0x3f];

            out[outIndex++] = mapLocal[b3 >> 2];
            out[outIndex++] = mapLocal[((b3 & 0x03) << 4) | (b4 >> 4)];
            out[outIndex++] = mapLocal[((b4 & 0x0f) << 2) | (b5 >> 6)];
            out[outIndex++] = mapLocal[b5 & 0x3f];

            out[outIndex++] = mapLocal[b6 >> 2];
            out[outIndex++] = mapLocal[((b6 & 0x03) << 4) | (b7 >> 4)];
            out[outIndex++] = mapLocal[((b7 & 0x0f) << 2) | (b8 >> 6)];
            out[outIndex++] = mapLocal[b8 & 0x3f];

            out[outIndex++] = mapLocal[b9 >> 2];
            out[outIndex++] = mapLocal[((b9 & 0x03) << 4) | (b10 >> 4)];
            out[outIndex++] = mapLocal[((b10 & 0x0f) << 2) | (b11 >> 6)];
            out[outIndex++] = mapLocal[b11 & 0x3f];
        }
        for (; i < end; i += 3) {
            int b0 = in[i] & 0xff;
            int b1 = in[i + 1] & 0xff;
            int b2 = in[i + 2] & 0xff;

            out[outIndex++] = mapLocal[b0 >> 2];
            out[outIndex++] = mapLocal[((b0 & 0x03) << 4) | (b1 >> 4)];
            out[outIndex++] = mapLocal[((b1 & 0x0f) << 2) | (b2 >> 6)];
            out[outIndex++] = mapLocal[b2 & 0x3f];
        }

        switch (inputLength - end) {
            case 1: {
                int b0 = in[end] & 0xff;
                out[outIndex++] = mapLocal[b0 >> 2];
                out[outIndex++] = mapLocal[(b0 & 0x03) << 4];
                out[outIndex++] = '=';
                out[outIndex++] = '=';
                break;
            }
            case 2: {
                int b0 = in[end] & 0xff;
                int b1 = in[end + 1] & 0xff;
                out[outIndex++] = mapLocal[b0 >> 2];
                out[outIndex++] = mapLocal[((b0 & 0x03) << 4) | (b1 >> 4)];
                out[outIndex++] = mapLocal[(b1 & 0x0f) << 2];
                out[outIndex++] = '=';
                break;
            }
            default:
                break;
        }
        return outIndex;
    }

    /// SIMD-optimized Base64 encoding with explicit offsets and caller scratch.
    /// Scratch layout: a single SIMD-allocated `int[]` buffer of at least 192 ints.
    ///
    /// Usage example:
    /// ```java
    /// Simd simd = Simd.get();
    /// byte[] input = simd.allocByte(data.length);
    /// System.arraycopy(data, 0, input, 0, data.length);
    /// byte[] output = simd.allocByte(((data.length + 2) / 3) * 4);
    /// int[] scratch = simd.allocInt(192);
    /// int written = Base64.encodeNoNewlineSimd(input, 0, input.length, output, 0, scratch);
    /// ```
    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    public static int encodeNoNewlineSimd(byte[] in, int inOffset, int inLength, byte[] out, int outOffset, int[] scratch) {
        Simd simd = Simd.get();
        int outputLength = ((inLength + 2) / 3) * 4;
        if (out.length - outOffset < outputLength) {
            throw new IllegalArgumentException("Output buffer too small for encoded data");
        }
        if (inLength == 0) {
            return 0;
        }
        requireScratch(scratch);
        requireSimdApiArrays(simd, in, out, scratch);

        final int b0 = 0;
        final int b1 = b0 + SIMD_LANES;
        final int b2 = b1 + SIMD_LANES;
        final int s0 = b2 + SIMD_LANES;
        final int s1 = s0 + SIMD_LANES;
        final int s2 = s1 + SIMD_LANES;
        final int s3 = s2 + SIMD_LANES;
        final int t0 = s3 + SIMD_LANES;
        final int t1 = t0 + SIMD_LANES;
        final int c3 = t1 + SIMD_LANES;
        final int c15 = c3 + SIMD_LANES;
        final int c63 = c15 + SIMD_LANES;

        for (int lane = 0; lane < SIMD_LANES; lane++) {
            scratch[c3 + lane] = 3;
            scratch[c15 + lane] = 15;
            scratch[c63 + lane] = 63;
        }

        int end = inOffset + inLength - (inLength % 3);
        int simdEnd = end - ((end - inOffset) % 48);
        int inIndex = inOffset;
        int outIndex = outOffset;
        for (; inIndex < simdEnd; inIndex += 48) {
            for (int lane = 0; lane < SIMD_LANES; lane++) {
                int src = inIndex + lane * 3;
                scratch[b0 + lane] = in[src] & 0xff;
                scratch[b1 + lane] = in[src + 1] & 0xff;
                scratch[b2 + lane] = in[src + 2] & 0xff;
            }

            simd.shrLogical(scratch, b0, 2, scratch, s0, SIMD_LANES);
            simd.and(scratch, b0, scratch, c3, scratch, t0, SIMD_LANES);
            simd.shl(scratch, t0, 4, scratch, t0, SIMD_LANES);
            simd.shrLogical(scratch, b1, 4, scratch, t1, SIMD_LANES);
            simd.or(scratch, t0, scratch, t1, scratch, s1, SIMD_LANES);
            simd.and(scratch, b1, scratch, c15, scratch, t0, SIMD_LANES);
            simd.shl(scratch, t0, 2, scratch, t0, SIMD_LANES);
            simd.shrLogical(scratch, b2, 6, scratch, t1, SIMD_LANES);
            simd.or(scratch, t0, scratch, t1, scratch, s2, SIMD_LANES);
            simd.and(scratch, b2, scratch, c63, scratch, s3, SIMD_LANES);

            for (int lane = 0; lane < SIMD_LANES; lane++) {
                out[outIndex++] = map[scratch[s0 + lane]];
                out[outIndex++] = map[scratch[s1 + lane]];
                out[outIndex++] = map[scratch[s2 + lane]];
                out[outIndex++] = map[scratch[s3 + lane]];
            }
        }

        for (; inIndex < end; inIndex += 3) {
            int x0 = in[inIndex] & 0xff;
            int x1 = in[inIndex + 1] & 0xff;
            int x2 = in[inIndex + 2] & 0xff;
            out[outIndex++] = map[x0 >> 2];
            out[outIndex++] = map[((x0 & 0x03) << 4) | (x1 >> 4)];
            out[outIndex++] = map[((x1 & 0x0f) << 2) | (x2 >> 6)];
            out[outIndex++] = map[x2 & 0x3f];
        }

        switch (inOffset + inLength - end) {
            case 1: {
                int x0 = in[end] & 0xff;
                out[outIndex++] = map[x0 >> 2];
                out[outIndex++] = map[(x0 & 0x03) << 4];
                out[outIndex++] = '=';
                out[outIndex++] = '=';
                break;
            }
            case 2: {
                int x0 = in[end] & 0xff;
                int x1 = in[end + 1] & 0xff;
                out[outIndex++] = map[x0 >> 2];
                out[outIndex++] = map[((x0 & 0x03) << 4) | (x1 >> 4)];
                out[outIndex++] = map[(x1 & 0x0f) << 2];
                out[outIndex++] = '=';
                break;
            }
            default:
                break;
        }
        return outputLength;
    }

    /// SIMD-optimized Base64 decoding for no-whitespace input.
    /// Scratch layout: a single SIMD-allocated `int[]` buffer of at least 192 ints.
    ///
    /// Returns decoded bytes written, or `-1` for invalid input.
    ///
    /// Usage example:
    /// ```java
    /// Simd simd = Simd.get();
    /// byte[] encoded = simd.allocByte(base64Bytes.length);
    /// System.arraycopy(base64Bytes, 0, encoded, 0, base64Bytes.length);
    /// byte[] decoded = simd.allocByte((encoded.length / 4) * 3);
    /// int[] scratch = simd.allocInt(192);
    /// int written = Base64.decodeNoWhitespaceSimd(encoded, 0, encoded.length, decoded, 0, scratch);
    /// ```
    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    public static int decodeNoWhitespaceSimd(byte[] in, int inOffset, int inLength, byte[] out, int outOffset, int[] scratch) {
        if ((inLength & 0x3) != 0) {
            return -1;
        }
        int pad = 0;
        if (inLength > 0 && in[inOffset + inLength - 1] == '=') {
            pad++;
            if (inLength > 1 && in[inOffset + inLength - 2] == '=') {
                pad++;
            }
        }
        if (pad > 2) {
            return -1;
        }
        int outLength = (inLength / 4) * 3 - pad;
        if (outLength <= 0) {
            return 0;
        }
        if (out.length - outOffset < outLength) {
            throw new IllegalArgumentException("Output buffer too small for decoded data");
        }

        requireScratch(scratch);
        Simd simd = Simd.get();
        requireSimdApiArrays(simd, in, out, scratch);

        final int q0 = 0;
        final int q1 = q0 + SIMD_LANES;
        final int q2 = q1 + SIMD_LANES;
        final int q3 = q2 + SIMD_LANES;
        final int o0 = q3 + SIMD_LANES;
        final int o1 = o0 + SIMD_LANES;
        final int o2 = o1 + SIMD_LANES;
        final int t0 = o2 + SIMD_LANES;
        final int t1 = t0 + SIMD_LANES;
        final int c3 = t1 + SIMD_LANES;
        final int c15 = c3 + SIMD_LANES;

        for (int lane = 0; lane < SIMD_LANES; lane++) {
            scratch[c3 + lane] = 3;
            scratch[c15 + lane] = 15;
        }

        int fullLen = inLength - (pad > 0 ? 4 : 0);
        int simdFullLen = fullLen - (fullLen % 64);
        int inIndex = inOffset;
        int outIndex = outOffset;
        int endVector = inOffset + simdFullLen;
        for (; inIndex < endVector; inIndex += 64) {
            for (int lane = 0; lane < SIMD_LANES; lane++) {
                int src = inIndex + lane * 4;
                int d0 = decodeMapInt[in[src] & 0xff];
                int d1 = decodeMapInt[in[src + 1] & 0xff];
                int d2 = decodeMapInt[in[src + 2] & 0xff];
                int d3 = decodeMapInt[in[src + 3] & 0xff];
                if ((d0 | d1 | d2 | d3) < 0) {
                    return -1;
                }
                scratch[q0 + lane] = d0;
                scratch[q1 + lane] = d1;
                scratch[q2 + lane] = d2;
                scratch[q3 + lane] = d3;
            }

            simd.shl(scratch, q0, 2, scratch, o0, SIMD_LANES);
            simd.shrLogical(scratch, q1, 4, scratch, t0, SIMD_LANES);
            simd.or(scratch, o0, scratch, t0, scratch, o0, SIMD_LANES);
            simd.and(scratch, q1, scratch, c15, scratch, t0, SIMD_LANES);
            simd.shl(scratch, t0, 4, scratch, t0, SIMD_LANES);
            simd.shrLogical(scratch, q2, 2, scratch, t1, SIMD_LANES);
            simd.or(scratch, t0, scratch, t1, scratch, o1, SIMD_LANES);
            simd.and(scratch, q2, scratch, c3, scratch, t0, SIMD_LANES);
            simd.shl(scratch, t0, 6, scratch, t0, SIMD_LANES);
            simd.or(scratch, t0, scratch, q3, scratch, o2, SIMD_LANES);

            for (int lane = 0; lane < SIMD_LANES; lane++) {
                out[outIndex++] = (byte)scratch[o0 + lane];
                out[outIndex++] = (byte)scratch[o1 + lane];
                out[outIndex++] = (byte)scratch[o2 + lane];
            }
        }

        int fullEnd = inOffset + fullLen;
        for (; inIndex < fullEnd; inIndex += 4) {
            int c0 = in[inIndex] & 0xff;
            int c1 = in[inIndex + 1] & 0xff;
            int c2 = in[inIndex + 2] & 0xff;
            int c3v = in[inIndex + 3] & 0xff;
            int x0 = decodeMapInt[c0];
            int x1 = decodeMapInt[c1];
            int x2 = decodeMapInt[c2];
            int x3 = decodeMapInt[c3v];
            if ((x0 | x1 | x2 | x3) < 0) {
                return -1;
            }
            int quantum = (x0 << 18) | (x1 << 12) | (x2 << 6) | x3;
            out[outIndex++] = (byte)((quantum >> 16) & 0xff);
            out[outIndex++] = (byte)((quantum >> 8) & 0xff);
            out[outIndex++] = (byte)(quantum & 0xff);
        }

        if (pad == 0) {
            return outLength;
        }

        int i = inOffset + inLength - 4;
        int c0 = in[i] & 0xff;
        int c1 = in[i + 1] & 0xff;
        int x0 = decodeMapInt[c0];
        int x1 = decodeMapInt[c1];
        if ((x0 | x1) < 0) {
            return -1;
        }
        out[outIndex++] = (byte)((x0 << 2) | (x1 >> 4));
        if (pad == 2) {
            return (in[i + 2] == '=' && in[i + 3] == '=') ? outLength : -1;
        }
        if (in[i + 3] != '=') {
            return -1;
        }
        int x2 = decodeMapInt[in[i + 2] & 0xff];
        if (x2 < 0) {
            return -1;
        }
        out[outIndex] = (byte)((x1 << 4) | (x2 >> 2));
        return outLength;
    }

    /// Convenience overload for `encodeNoNewlineSimd(byte[], int, int, byte[], int, int[])`
    /// using zero offsets.
    public static int encodeNoNewlineSimd(byte[] in, byte[] out, int[] scratch) {
        return encodeNoNewlineSimd(in, 0, in.length, out, 0, scratch);
    }

    /// Convenience overload for `decodeNoWhitespaceSimd(byte[], int, int, byte[], int, int[])`
    /// using zero offsets.
    public static int decodeNoWhitespaceSimd(byte[] in, int len, byte[] out, int[] scratch) {
        return decodeNoWhitespaceSimd(in, 0, len, out, 0, scratch);
    }

    private static void requireScratch(int[] scratch) {
        if (scratch == null || scratch.length < SIMD_SCRATCH_INTS) {
            throw new IllegalArgumentException("scratch must be an int[] allocated with Simd.allocInt(192) or larger");
        }
    }

    private static void requireSimdApiArrays(Simd simd, byte[] in, byte[] out, int[] scratch) {
        simd.unpackUnsignedByteToInt(in, scratch, 0, 0);
        simd.packIntToByteTruncate(scratch, out, 0, 0);
    }

    private static byte[] allocByteMaybeSimd(int size) {
        if (size <= 0) {
            return new byte[0];
        }
        Simd simd = Simd.get();
        if (simd.isSupported() && size >= 16) {
            return simd.allocByte(size);
        }
        return new byte[size];
    }
}
