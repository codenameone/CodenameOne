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
package com.codename1.backend;

/**
 * A growable byte buffer that callers reuse.
 *
 * This exists because building output as a String and then encoding it is the
 * single most expensive thing a server can do per request. Measured on this
 * server: the response head alone, built with a StringBuilder, turned into a
 * String and then into bytes, was a third of all allocation; the JSON body was
 * most of the rest. Written as bytes into a buffer that lives as long as the
 * connection, both cost nothing.
 *
 * Deliberately not java.io.ByteArrayOutputStream: that one cannot be reset
 * without discarding its buffer on some implementations, has synchronized
 * methods, and hands out a COPY of its contents -- three allocations where this
 * has none.
 *
 * Not thread safe. One per connection, used by the worker that owns it.
 */
public final class ByteSink {
    private byte[] data;
    private int length;

    public ByteSink(int initialCapacity) {
        data = new byte[initialCapacity < 16 ? 16 : initialCapacity];
    }

    /** The backing array. Valid up to {@link #length}; not a copy. */
    public byte[] bytes() {
        return data;
    }

    public int length() {
        return length;
    }

    public void reset() {
        length = 0;
    }

    public void ensure(int extra) {
        if(length + extra <= data.length) {
            return;
        }
        int size = data.length * 2;
        while(size < length + extra) {
            size *= 2;
        }
        byte[] grown = new byte[size];
        System.arraycopy(data, 0, grown, 0, length);
        data = grown;
    }

    public void put(int b) {
        ensure(1);
        data[length++] = (byte)b;
    }

    public void put(byte[] source, int offset, int count) {
        ensure(count);
        System.arraycopy(source, offset, data, length, count);
        length += count;
    }

    public void put(ByteSink other) {
        put(other.data, 0, other.length);
    }

    /**
     * ASCII only, one byte per character. For header names, JSON punctuation and
     * other text this code owns; anything from outside goes through
     * {@link #putUtf8}.
     */
    public void putAscii(String ascii) {
        int n = ascii.length();
        ensure(n);
        for(int iter = 0 ; iter < n ; iter++) {
            data[length++] = (byte)ascii.charAt(iter);
        }
    }

    /**
     * UTF-8, encoded in place.
     *
     * String.getBytes("UTF-8") would allocate the array this exists to avoid, and
     * on the translated target it goes through the platform's encoder for every
     * call. Surrogate pairs are combined; an unpaired surrogate becomes U+FFFD,
     * because emitting a lone surrogate produces bytes no decoder will accept.
     */
    public void putUtf8(String value) {
        int n = value.length();
        ensure(n);                       // exact for ASCII, grown below otherwise
        for(int iter = 0 ; iter < n ; iter++) {
            int c = value.charAt(iter);
            if(c < 0x80) {
                ensure(1);
                data[length++] = (byte)c;
            } else if(c < 0x800) {
                ensure(2);
                data[length++] = (byte)(0xc0 | (c >> 6));
                data[length++] = (byte)(0x80 | (c & 0x3f));
            } else if(c >= 0xd800 && c <= 0xdbff && iter + 1 < n
                    && value.charAt(iter + 1) >= 0xdc00 && value.charAt(iter + 1) <= 0xdfff) {
                int code = 0x10000 + ((c - 0xd800) << 10) + (value.charAt(iter + 1) - 0xdc00);
                iter++;
                ensure(4);
                data[length++] = (byte)(0xf0 | (code >> 18));
                data[length++] = (byte)(0x80 | ((code >> 12) & 0x3f));
                data[length++] = (byte)(0x80 | ((code >> 6) & 0x3f));
                data[length++] = (byte)(0x80 | (code & 0x3f));
            } else if(c >= 0xd800 && c <= 0xdfff) {
                ensure(3);               // unpaired surrogate -> U+FFFD
                data[length++] = (byte)0xef;
                data[length++] = (byte)0xbf;
                data[length++] = (byte)0xbd;
            } else {
                ensure(3);
                data[length++] = (byte)(0xe0 | (c >> 12));
                data[length++] = (byte)(0x80 | ((c >> 6) & 0x3f));
                data[length++] = (byte)(0x80 | (c & 0x3f));
            }
        }
    }

    /**
     * One code point as UTF-8.
     *
     * Separate from {@link #putUtf8} because a caller walking a String character
     * by character has to combine a surrogate PAIR itself -- handing the halves
     * over one at a time turns an emoji into two replacement characters, which is
     * what the JSON writer did until its output was compared against the String
     * form byte for byte.
     */
    public void putCodePoint(int code) {
        if(code < 0x80) {
            ensure(1);
            data[length++] = (byte)code;
        } else if(code < 0x800) {
            ensure(2);
            data[length++] = (byte)(0xc0 | (code >> 6));
            data[length++] = (byte)(0x80 | (code & 0x3f));
        } else if(code < 0x10000) {
            ensure(3);
            data[length++] = (byte)(0xe0 | (code >> 12));
            data[length++] = (byte)(0x80 | ((code >> 6) & 0x3f));
            data[length++] = (byte)(0x80 | (code & 0x3f));
        } else {
            ensure(4);
            data[length++] = (byte)(0xf0 | (code >> 18));
            data[length++] = (byte)(0x80 | ((code >> 12) & 0x3f));
            data[length++] = (byte)(0x80 | ((code >> 6) & 0x3f));
            data[length++] = (byte)(0x80 | (code & 0x3f));
        }
    }

    /**
     * A number as ASCII digits, written in place. Long.toString would allocate a
     * String and its char[], and this is on the path of every response (the
     * status and the content length) and every JSON number.
     */
    public void putNumber(long value) {
        if(value < 0) {
            put('-');
            if(value == Long.MIN_VALUE) {
                // Negating it overflows; it has no positive counterpart.
                putAscii("9223372036854775808");
                return;
            }
            value = -value;
        }
        if(value == 0) {
            put('0');
            return;
        }
        int digits = 0;
        long counter = value;
        while(counter > 0) {
            digits++;
            counter /= 10;
        }
        ensure(digits);
        length += digits;
        int at = length;
        while(value > 0) {
            data[--at] = (byte)('0' + (int)(value % 10));
            value /= 10;
        }
    }
}
