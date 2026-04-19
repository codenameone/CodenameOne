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
    private static final int SIMD_BYTE_LANES = 16;

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

    private static final int SIMD_ENC_LANE0 = 0;
    private static final int SIMD_ENC_LANE1 = SIMD_ENC_LANE0 + SIMD_BYTE_LANES;
    private static final int SIMD_ENC_LANE2 = SIMD_ENC_LANE1 + SIMD_BYTE_LANES;
    private static final int SIMD_ENC_OUT0 = SIMD_ENC_LANE2 + SIMD_BYTE_LANES;
    private static final int SIMD_ENC_TMP = SIMD_ENC_OUT0 + SIMD_BYTE_LANES;
    private static final int SIMD_ENC_OUT1 = SIMD_ENC_TMP + SIMD_BYTE_LANES;
    private static final int SIMD_ENC_OUT2 = SIMD_ENC_OUT1 + SIMD_BYTE_LANES;
    private static final int SIMD_ENCODE_SCRATCH_BYTES = SIMD_ENC_OUT2 + SIMD_BYTE_LANES;

    private static final int SIMD_DEC_OUT0 = 0;
    private static final int SIMD_DEC_OUT1 = SIMD_DEC_OUT0 + SIMD_BYTE_LANES;
    private static final int SIMD_DEC_OUT2 = SIMD_DEC_OUT1 + SIMD_BYTE_LANES;
    private static final int SIMD_DEC_OUT3 = SIMD_DEC_OUT2 + SIMD_BYTE_LANES;
    private static final int SIMD_DEC_TMP = SIMD_DEC_OUT3 + SIMD_BYTE_LANES;
    private static final int SIMD_DECODE_SCRATCH_BYTES = SIMD_DEC_TMP + SIMD_BYTE_LANES;

    private static byte[] simdEncodeMap;
    private static byte[] simdDecodeMap;
    private static byte[] simdConst03;
    private static byte[] simdConst0F;
    private static byte[] simdConst3F;

    private static byte[] getSimdEncodeMap(Simd simd) {
        byte[] out = simdEncodeMap;
        if (out != null) {
            return out;
        }
        out = simd.allocByte(64);
        System.arraycopy(map, 0, out, 0, 64);
        simdEncodeMap = out;
        return out;
    }

    private static byte[] getSimdDecodeMap(Simd simd) {
        byte[] out = simdDecodeMap;
        if (out != null) {
            return out;
        }
        out = simd.allocByte(256);
        fillRange(out, (byte) -1);
        for (int i = 0; i < 64; i++) {
            out[map[i] & 0xff] = (byte) i;
        }
        simdDecodeMap = out;
        return out;
    }

    private static byte[] getSimdConst03(Simd simd) {
        byte[] out = simdConst03;
        if (out != null) {
            return out;
        }
        out = simd.allocByte(SIMD_BYTE_LANES);
        fillRange(out, (byte) 0x03);
        simdConst03 = out;
        return out;
    }

    private static byte[] getSimdConst0F(Simd simd) {
        byte[] out = simdConst0F;
        if (out != null) {
            return out;
        }
        out = simd.allocByte(SIMD_BYTE_LANES);
        fillRange(out, (byte) 0x0F);
        simdConst0F = out;
        return out;
    }

    private static byte[] getSimdConst3F(Simd simd) {
        byte[] out = simdConst3F;
        if (out != null) {
            return out;
        }
        out = simd.allocByte(SIMD_BYTE_LANES);
        fillRange(out, (byte) 0x3F);
        simdConst3F = out;
        return out;
    }

    private static void fillRange(byte[] arr, byte value) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = value;
        }
    }

    /// SIMD-optimized Base64 encoding with explicit offsets.
    /// Uses generic SIMD byte primitives over a single caller-invisible scratch slab.
    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    public static int encodeNoNewlineSimd(byte[] in, int inOffset, int inLength, byte[] out, int outOffset) {
        int outputLength = ((inLength + 2) / 3) * 4;
        if (inLength == 0) {
            return 0;
        }
        Simd simd = Simd.get();
        byte[] encodeMap = getSimdEncodeMap(simd);

        byte[] const03 = getSimdConst03(simd);
        byte[] const0F = getSimdConst0F(simd);
        byte[] const3F = getSimdConst3F(simd);
        int end3 = inOffset + inLength - (inLength % 3);
        int si = inOffset;
        int di = outOffset;
        int simdEnd = end3 - SIMD_BYTE_LANES * 3 + 1;
        byte[] scratch = simdEnd > si ? simd.allocaByte(SIMD_ENCODE_SCRATCH_BYTES) : null;

        while (si < simdEnd) {
            simd.unpackBytesInterleaved3(in, si, scratch, SIMD_ENC_LANE0, SIMD_ENC_LANE1, SIMD_ENC_LANE2, SIMD_BYTE_LANES);
            simd.shrLogical(scratch, SIMD_ENC_LANE0, 2, scratch, SIMD_ENC_OUT0, SIMD_BYTE_LANES);

            simd.and(scratch, SIMD_ENC_LANE0, const03, 0, scratch, SIMD_ENC_TMP, SIMD_BYTE_LANES);
            simd.shl(scratch, SIMD_ENC_TMP, 4, scratch, SIMD_ENC_TMP, SIMD_BYTE_LANES);
            simd.shrLogical(scratch, SIMD_ENC_LANE1, 4, scratch, SIMD_ENC_OUT1, SIMD_BYTE_LANES);
            simd.or(scratch, SIMD_ENC_TMP, scratch, SIMD_ENC_OUT1, scratch, SIMD_ENC_OUT1, SIMD_BYTE_LANES);

            simd.and(scratch, SIMD_ENC_LANE1, const0F, 0, scratch, SIMD_ENC_TMP, SIMD_BYTE_LANES);
            simd.shl(scratch, SIMD_ENC_TMP, 2, scratch, SIMD_ENC_TMP, SIMD_BYTE_LANES);
            simd.shrLogical(scratch, SIMD_ENC_LANE2, 6, scratch, SIMD_ENC_OUT2, SIMD_BYTE_LANES);
            simd.or(scratch, SIMD_ENC_TMP, scratch, SIMD_ENC_OUT2, scratch, SIMD_ENC_OUT2, SIMD_BYTE_LANES);

            simd.and(scratch, SIMD_ENC_LANE2, const3F, 0, scratch, SIMD_ENC_LANE2, SIMD_BYTE_LANES);

            simd.lookupBytes(encodeMap, scratch, SIMD_ENC_OUT0, scratch, SIMD_ENC_LANE0, SIMD_BYTE_LANES);
            simd.lookupBytes(encodeMap, scratch, SIMD_ENC_OUT1, scratch, SIMD_ENC_LANE1, SIMD_BYTE_LANES);
            simd.lookupBytes(encodeMap, scratch, SIMD_ENC_OUT2, scratch, SIMD_ENC_OUT0, SIMD_BYTE_LANES);
            simd.lookupBytes(encodeMap, scratch, SIMD_ENC_LANE2, scratch, SIMD_ENC_LANE2, SIMD_BYTE_LANES);

            simd.packBytesInterleaved4(scratch, SIMD_ENC_LANE0, SIMD_ENC_LANE1, SIMD_ENC_OUT0, SIMD_ENC_LANE2, out, di, SIMD_BYTE_LANES);
            si += SIMD_BYTE_LANES * 3;
            di += SIMD_BYTE_LANES * 4;
        }

        byte[] mapLocal = map;
        while (si < end3) {
            int b0 = in[si] & 0xff;
            int b1 = in[si + 1] & 0xff;
            int b2 = in[si + 2] & 0xff;
            out[di]     = mapLocal[b0 >> 2];
            out[di + 1] = mapLocal[((b0 & 0x03) << 4) | (b1 >> 4)];
            out[di + 2] = mapLocal[((b1 & 0x0f) << 2) | (b2 >> 6)];
            out[di + 3] = mapLocal[b2 & 0x3f];
            si += 3;
            di += 4;
        }

        switch (inOffset + inLength - end3) {
            case 1: {
                int b0 = in[si] & 0xff;
                out[di]     = mapLocal[b0 >> 2];
                out[di + 1] = mapLocal[(b0 & 0x03) << 4];
                out[di + 2] = '=';
                out[di + 3] = '=';
                break;
            }
            case 2: {
                int b0 = in[si] & 0xff;
                int b1 = in[si + 1] & 0xff;
                out[di]     = mapLocal[b0 >> 2];
                out[di + 1] = mapLocal[((b0 & 0x03) << 4) | (b1 >> 4)];
                out[di + 2] = mapLocal[(b1 & 0x0f) << 2];
                out[di + 3] = '=';
                break;
            }
            default:
                break;
        }
        return outputLength;
    }

    /// Compatibility overload that ignores the legacy scratch parameter.
    public static int encodeNoNewlineSimd(byte[] in, int inOffset, int inLength, byte[] out, int outOffset, int[] scratch) {
        return encodeNoNewlineSimd(in, inOffset, inLength, out, outOffset);
    }

    /// SIMD-optimized Base64 decoding for no-whitespace input.
    /// Uses generic SIMD byte primitives over a single caller-invisible scratch slab.
    @DisableDebugInfo
    @DisableNullChecksAndArrayBoundsChecks
    public static int decodeNoWhitespaceSimd(byte[] in, int inOffset, int inLength, byte[] out, int outOffset) {
        if (inLength == 0) {
            return 0;
        }
        if ((inLength & 0x3) != 0) {
            return -1;
        }
        Simd simd = Simd.get();

        int pad = 0;
        if (in[inOffset + inLength - 1] == '=') {
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

        byte[] decodeMap = getSimdDecodeMap(simd);
        int[] decodeMapLocal = decodeMapInt;

        int fullLen = inLength - (pad > 0 ? 4 : 0);
        int fullEnd = inOffset + fullLen;
        int si = inOffset;
        int di = outOffset;
        int simdEnd = fullEnd - SIMD_BYTE_LANES * 4 + 1;
        byte[] scratch = simdEnd > si ? simd.allocaByte(SIMD_DECODE_SCRATCH_BYTES) : null;
        while (si < simdEnd) {
            if (simd.unpackLookupBytesInterleaved4(decodeMap, in, si, scratch, SIMD_DEC_OUT0, SIMD_DEC_OUT1, SIMD_DEC_OUT2, SIMD_DEC_OUT3, SIMD_BYTE_LANES) < 0) {
                return -1;
            }
            simd.shl(scratch, SIMD_DEC_OUT0, 2, scratch, SIMD_DEC_OUT0, SIMD_BYTE_LANES);
            simd.shrLogical(scratch, SIMD_DEC_OUT1, 4, scratch, SIMD_DEC_TMP, SIMD_BYTE_LANES);
            simd.or(scratch, SIMD_DEC_OUT0, scratch, SIMD_DEC_TMP, scratch, SIMD_DEC_OUT0, SIMD_BYTE_LANES);

            simd.shl(scratch, SIMD_DEC_OUT1, 4, scratch, SIMD_DEC_OUT1, SIMD_BYTE_LANES);
            simd.shrLogical(scratch, SIMD_DEC_OUT2, 2, scratch, SIMD_DEC_TMP, SIMD_BYTE_LANES);
            simd.or(scratch, SIMD_DEC_OUT1, scratch, SIMD_DEC_TMP, scratch, SIMD_DEC_OUT1, SIMD_BYTE_LANES);

            simd.shl(scratch, SIMD_DEC_OUT2, 6, scratch, SIMD_DEC_OUT2, SIMD_BYTE_LANES);
            simd.or(scratch, SIMD_DEC_OUT2, scratch, SIMD_DEC_OUT3, scratch, SIMD_DEC_OUT2, SIMD_BYTE_LANES);

            simd.packBytesInterleaved3(scratch, SIMD_DEC_OUT0, SIMD_DEC_OUT1, SIMD_DEC_OUT2, out, di, SIMD_BYTE_LANES);
            si += SIMD_BYTE_LANES * 4;
            di += SIMD_BYTE_LANES * 3;
        }

        while (si < fullEnd) {
            int c0 = in[si] & 0xff;
            int c1 = in[si + 1] & 0xff;
            int c2 = in[si + 2] & 0xff;
            int c3 = in[si + 3] & 0xff;
            int b0 = decodeMapLocal[c0];
            int b1 = decodeMapLocal[c1];
            int b2 = decodeMapLocal[c2];
            int b3 = decodeMapLocal[c3];
            if ((b0 | b1 | b2 | b3) < 0) {
                return -1;
            }
            int quantum = (b0 << 18) | (b1 << 12) | (b2 << 6) | b3;
            out[di++] = (byte) ((quantum >> 16) & 0xff);
            out[di++] = (byte) ((quantum >> 8) & 0xff);
            out[di++] = (byte) (quantum & 0xff);
            si += 4;
        }

        if (pad > 0) {
            int i = inOffset + inLength - 4;
            int c0 = in[i] & 0xff;
            int c1 = in[i + 1] & 0xff;
            int b0 = decodeMapLocal[c0];
            int b1 = decodeMapLocal[c1];
            if ((b0 | b1) < 0) {
                return -1;
            }
            out[di++] = (byte) ((b0 << 2) | (b1 >> 4));
            if (pad == 2) {
                return (in[i + 2] == '=' && in[i + 3] == '=') ? outLength : -1;
            }
            if (in[i + 3] != '=') {
                return -1;
            }
            int b2 = decodeMapLocal[in[i + 2] & 0xff];
            if (b2 < 0) {
                return -1;
            }
            out[di] = (byte) ((b1 << 4) | (b2 >> 2));
        }

        return outLength;
    }

    /// Compatibility overload that ignores the legacy scratch parameter.
    public static int decodeNoWhitespaceSimd(byte[] in, int inOffset, int inLength, byte[] out, int outOffset, int[] scratch) {
        return decodeNoWhitespaceSimd(in, inOffset, inLength, out, outOffset);
    }

    /// Convenience overload for `encodeNoNewlineSimd(byte[], int, int, byte[], int)`
    /// using zero offsets.
    public static int encodeNoNewlineSimd(byte[] in, byte[] out) {
        return encodeNoNewlineSimd(in, 0, in.length, out, 0);
    }

    /// Convenience overload for `encodeNoNewlineSimd(byte[], int, int, byte[], int)`
    /// that preserves the legacy scratch-bearing signature.
    public static int encodeNoNewlineSimd(byte[] in, byte[] out, int[] scratch) {
        return encodeNoNewlineSimd(in, 0, in.length, out, 0);
    }

    /// Convenience overload for `decodeNoWhitespaceSimd(byte[], int, int, byte[], int)`
    /// using zero input offset.
    public static int decodeNoWhitespaceSimd(byte[] in, int len, byte[] out) {
        return decodeNoWhitespaceSimd(in, 0, len, out, 0);
    }

    /// Convenience overload for `decodeNoWhitespaceSimd(byte[], int, int, byte[], int)`
    /// that preserves the legacy scratch-bearing signature.
    public static int decodeNoWhitespaceSimd(byte[] in, int len, byte[] out, int[] scratch) {
        return decodeNoWhitespaceSimd(in, 0, len, out, 0);
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
