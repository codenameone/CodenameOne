/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.rive.transcoder;

/// Little-endian reader over the bytes of a `.riv` file: LEB128 variable
/// length unsigned integers, 32-bit floats, length-prefixed strings and byte
/// runs. A read past the end sets [#didOverflow()] and answers zero, so a
/// truncated file stops the import instead of throwing out of it.
public final class RiveReader {
    private final byte[] bytes;
    private final int end;
    private int pos;
    private boolean overflowed;

    public RiveReader(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public RiveReader(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.pos = offset;
        this.end = offset + length;
    }

    public boolean reachedEnd() {
        return pos >= end || overflowed;
    }

    public boolean didOverflow() {
        return overflowed;
    }

    public int position() {
        return pos;
    }

    private void overflow() {
        overflowed = true;
        pos = end;
    }

    /// A LEB128 unsigned value; values wider than 32 bits keep their low 32.
    public long readVarUint64() {
        long result = 0;
        int shift = 0;
        while (true) {
            if (pos >= end || shift >= 64) {
                overflow();
                return 0;
            }
            int b = bytes[pos++] & 0xff;
            result |= ((long) (b & 0x7f)) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
    }

    public int readVarUint() {
        return (int) readVarUint64();
    }

    public int readByte() {
        if (pos >= end) {
            overflow();
            return 0;
        }
        return bytes[pos++] & 0xff;
    }

    public int readUint32() {
        if (end - pos < 4) {
            overflow();
            return 0;
        }
        int v = (bytes[pos] & 0xff) | ((bytes[pos + 1] & 0xff) << 8)
                | ((bytes[pos + 2] & 0xff) << 16) | ((bytes[pos + 3] & 0xff) << 24);
        pos += 4;
        return v;
    }

    public float readFloat32() {
        return Float.intBitsToFloat(readUint32());
    }

    private int readLength() {
        long len = readVarUint64();
        if (len > end - pos || len < 0) {
            overflow();
            return 0;
        }
        return (int) len;
    }

    /// A UTF-8 string, decoded by hand: the device class library has no charset lookup.
    public String readString() {
        int len = readLength();
        StringBuilder sb = new StringBuilder(len);
        int i = pos;
        int stop = pos + len;
        while (i < stop) {
            int c = bytes[i++] & 0xff;
            if (c < 0x80) {
                sb.append((char) c);
            } else if (c < 0xe0 && i < stop) {
                sb.append((char) (((c & 0x1f) << 6) | (bytes[i++] & 0x3f)));
            } else if (c < 0xf0 && i + 1 < stop) {
                sb.append((char) (((c & 0x0f) << 12) | ((bytes[i] & 0x3f) << 6) | (bytes[i + 1] & 0x3f)));
                i += 2;
            } else if (i + 2 < stop) {
                int cp = ((c & 0x07) << 18) | ((bytes[i] & 0x3f) << 12)
                        | ((bytes[i + 1] & 0x3f) << 6) | (bytes[i + 2] & 0x3f);
                i += 3;
                cp -= 0x10000;
                sb.append((char) (0xd800 + (cp >> 10)));
                sb.append((char) (0xdc00 + (cp & 0x3ff)));
            } else {
                break;
            }
        }
        pos = stop;
        return sb.toString();
    }

    public byte[] readBytes() {
        int len = readLength();
        byte[] out = new byte[len];
        System.arraycopy(bytes, pos, out, 0, len);
        pos += len;
        return out;
    }
}
