/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

import com.codename1.io.gzip.Adler32;
import com.codename1.io.gzip.CRC32;
import com.codename1.io.gzip.JZlib;
import com.codename1.io.gzip.ZStream;
import java.io.ByteArrayOutputStream;

/// `_cn1zlib`: the deflate streams `zlib`, `gzip` and `zipfile` are built
/// on, over the core's JZlib (`com.codename1.io.gzip`), a Java port of
/// zlib. Window bits follow zlib: 9..15 a zlib stream, -9..-15 raw
/// deflate, 25..31 gzip, and for inflating 40..47 either of the wrapped
/// two. A failure raises `OSError` with zlib's own message, which
/// `zlib.py` turns into `zlib.error`.
final class PyZlib {
    private static final int CHUNK = 16384;

    private PyZlib() {
    }

    static PyModule module() {
        PyModule m = new PyModule("_cn1zlib", new String[0]);
        m.setAttr("deflate_new", PyBuiltin.positional("deflate_new", a -> {
            ZStream z = new ZStream();
            int level = (int) Py.asLong(a[0]);
            int wbits = (int) Py.asLong(a[1]);
            if (wbits == 8 || wbits == -8 || wbits == 24) {
                // zlib widens a window of 8 to 9 for deflate
                wbits += wbits < 0 ? -1 : 1;
            }
            int err = z.deflateInit(level, wbits, (int) Py.asLong(a[2]));
            if (err != JZlib.Z_OK) {
                throw Py.error(Py.VALUE_ERROR, "Invalid initialization option");
            }
            int strategy = (int) Py.asLong(a[3]);
            if (strategy != JZlib.Z_DEFAULT_STRATEGY) {
                z.deflateParams(level, strategy);
            }
            if (a[4] != Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
                byte[] dict = PyBytes.like(a[4]);
                z.deflateSetDictionary(dict, dict.length);
            }
            return new PyJavaObject(z);
        }));
        m.setAttr("deflate", PyBuiltin.positional("deflate", a -> {
            ZStream z = stream(a[0]);
            byte[] in = PyBytes.like(a[1]);
            int flush = (int) Py.asLong(a[2]);
            return new PyBytes(deflate(z, in, flush));
        }));
        m.setAttr("inflate_new", PyBuiltin.positional("inflate_new", a -> {
            ZStream z = new ZStream();
            int err = z.inflateInit((int) Py.asLong(a[0]));
            if (err != JZlib.Z_OK) {
                throw Py.error(Py.VALUE_ERROR, "Invalid initialization option");
            }
            return new PyJavaObject(new InflateState(z, a[1] == Py.NONE ? null : PyBytes.like(a[1]))); //NOPMD CompareObjectsWithEquals - identity is meant
        }));
        m.setAttr("inflate", PyBuiltin.positional("inflate", a -> {
            InflateState s = inflateState(a[0]);
            byte[] in = PyBytes.like(a[1]);
            long max = Py.asLong(a[2]);
            return s.inflate(in, max <= 0 ? Long.MAX_VALUE : max, Py.isTrue(a[3]));
        }));
        m.setAttr("end", PyBuiltin.positional("end", a -> {
            Object v = a[0] instanceof PyJavaObject ? ((PyJavaObject) a[0]).value : null;
            if (v instanceof ZStream) {
                ((ZStream) v).deflateEnd();
            } else if (v instanceof InflateState) {
                ((InflateState) v).z.inflateEnd();
            }
            return Py.NONE;
        }));
        m.setAttr("crc32", PyBuiltin.positional("crc32", a -> {
            byte[] b = PyBytes.like(a[0]);
            CRC32 c = new CRC32();
            c.reset(Py.asLong(a[1]) & 0xffffffffL);
            c.update(b, 0, b.length);
            return PyInt.of(c.getValue() & 0xffffffffL);
        }));
        m.setAttr("adler32", PyBuiltin.positional("adler32", a -> {
            byte[] b = PyBytes.like(a[0]);
            Adler32 c = new Adler32();
            c.reset(Py.asLong(a[1]) & 0xffffffffL);
            c.update(b, 0, b.length);
            return PyInt.of(c.getValue() & 0xffffffffL);
        }));
        return m;
    }

    private static ZStream stream(Object o) {
        if (o instanceof PyJavaObject && ((PyJavaObject) o).value instanceof ZStream) {
            return (ZStream) ((PyJavaObject) o).value;
        }
        throw Py.typeError("not a deflate stream");
    }

    private static InflateState inflateState(Object o) {
        if (o instanceof PyJavaObject && ((PyJavaObject) o).value instanceof InflateState) {
            return (InflateState) ((PyJavaObject) o).value;
        }
        throw Py.typeError("not an inflate stream");
    }

    private static byte[] deflate(ZStream z, byte[] in, int flush) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[CHUNK];
        z.setInput(in, 0, in.length, false);
        while (true) {
            z.setOutput(buf, 0, buf.length);
            int err = z.deflate(flush);
            if (err != JZlib.Z_OK && err != JZlib.Z_STREAM_END && err != JZlib.Z_BUF_ERROR) {
                throw Py.error(Py.OS_ERROR, "Error " + err + " while compressing data" + detail(z));
            }
            out.write(buf, 0, buf.length - z.availOut);
            if (err == JZlib.Z_STREAM_END || (z.availOut != 0 && z.availIn == 0)) {
                break;
            }
        }
        return out.toByteArray();
    }

    private static String detail(ZStream z) {
        return z.msg == null ? "" : ": " + z.msg;
    }

    /// An inflate stream and the dictionary it asks for, if any.
    static final class InflateState {
        final ZStream z;
        private final byte[] dict;

        InflateState(ZStream z, byte[] dict) {
            this.z = z;
            this.dict = dict;
        }

        /// Inflates `in`, producing at most `max` bytes; answers
        /// `(output, bytes of in consumed, end of stream reached)`.
        Object inflate(byte[] in, long max, boolean finish) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[CHUNK];
            z.setInput(in, 0, in.length, false);
            boolean eof = false;
            while (true) {
                long room = max - out.size();
                if (room <= 0) {
                    break;
                }
                int len = (int) Math.min(buf.length, room);
                z.setOutput(buf, 0, len);
                int err = z.inflate(finish ? JZlib.Z_FINISH : JZlib.Z_NO_FLUSH);
                if (err == JZlib.Z_NEED_DICT) {
                    if (dict == null || z.inflateSetDictionary(dict, dict.length) != JZlib.Z_OK) {
                        throw Py.error(Py.OS_ERROR, "Error 2 while decompressing data");
                    }
                    continue;
                }
                int produced = len - z.availOut;
                out.write(buf, 0, produced);
                if (err == JZlib.Z_STREAM_END) {
                    eof = true;
                    break;
                }
                if (err == JZlib.Z_BUF_ERROR) {
                    break;
                }
                if (err != JZlib.Z_OK) {
                    throw Py.error(Py.OS_ERROR, "Error " + err + " while decompressing data" + detail(z));
                }
                if (z.availIn == 0 && produced < len) {
                    break;
                }
            }
            int consumed = in.length - z.availIn;
            return new PyTuple(new Object[] {new PyBytes(out.toByteArray()), PyInt.of(consumed), Py.bool(eof)});
        }
    }
}
