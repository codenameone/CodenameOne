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
package com.codename1.flutter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import dart.async.Future;
import dart.typed_data.ByteData;

/**
 * dart:ui's {@code Image} as {@code Picture.toImage} returns it. (The name {@code Image}
 * is the widget's in these stubs, so code that writes the type as {@code ui.Image}
 * should use {@code final} or {@code var}; the members are dart:ui's.) It wraps a
 * Codename One image, or none headless, where it is fully transparent.
 *
 * <p>{@code toByteData(format: ImageByteFormat.png)} encodes with the port's
 * {@code ImageIO} when it has one, and otherwise with a small built-in encoder
 * (uncompressed deflate), so the bytes are a valid PNG everywhere, headless included.
 * {@code rawRgba} and {@code rawStraightRgba} answer the pixels row by row; the other
 * raw formats answer the same bytes. {@code ByteData.buffer} is not provided by the
 * runtime's ByteData, so read the bytes with {@code getUint8}.
 */
public class UiImage {
    private final int width;
    private final int height;
    private final Image image;
    private boolean disposed;

    UiImage(int width, int height, Image image) {
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public long width() {
        return width;
    }

    public long height() {
        return height;
    }

    /** The Codename One image, or null headless. */
    public Image cn1Image() {
        return image;
    }

    public void dispose() {
        disposed = true;
    }

    public boolean debugDisposed() {
        return disposed;
    }

    public Future<ByteData> toByteData(ImageByteFormat format) {
        try {
            byte[] b = format == ImageByteFormat.png ? png() : rgba();
            return Future.value(ByteData.fromBytes(b));
        } catch (IOException e) {
            return Future.error(new dart.core.StateError("Could not encode the image: " + e.getMessage()));
        }
    }

    private int[] argb() {
        return image == null ? new int[width * height] : image.getRGB();
    }

    private byte[] rgba() {
        int[] px = argb();
        byte[] out = new byte[px.length * 4];
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            out[i * 4] = (byte) (p >> 16);
            out[i * 4 + 1] = (byte) (p >> 8);
            out[i * 4 + 2] = (byte) p;
            out[i * 4 + 3] = (byte) (p >>> 24);
        }
        return out;
    }

    private byte[] png() throws IOException {
        ImageIO io = image == null ? null : ImageIO.getImageIO();
        if (io != null && io.isFormatSupported(ImageIO.FORMAT_PNG)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            io.save(image, bo, ImageIO.FORMAT_PNG, 1f);
            byte[] b = bo.toByteArray();
            if (b.length > 8) {
                return b;
            }
        }
        return encodePng(width, height, argb());
    }

    /// A PNG of the ARGB pixels: 8-bit RGBA, filter 0, stored (uncompressed) deflate.
    static byte[] encodePng(int w, int h, int[] px) throws IOException {
        int stride = w * 4 + 1;
        byte[] raw = new byte[stride * h];
        for (int y = 0; y < h; y++) {
            int o = y * stride;
            raw[o] = 0;
            for (int x = 0; x < w; x++) {
                int p = px[y * w + x];
                int q = o + 1 + x * 4;
                raw[q] = (byte) (p >> 16);
                raw[q + 1] = (byte) (p >> 8);
                raw[q + 2] = (byte) p;
                raw[q + 3] = (byte) (p >>> 24);
            }
        }
        ByteArrayOutputStream z = new ByteArrayOutputStream();
        z.write(0x78);
        z.write(0x01);
        int pos = 0;
        do {
            int n = Math.min(65535, raw.length - pos);
            boolean last = pos + n >= raw.length;
            z.write(last ? 1 : 0);
            z.write(n & 0xff);
            z.write((n >> 8) & 0xff);
            z.write(~n & 0xff);
            z.write((~n >> 8) & 0xff);
            z.write(raw, pos, n);
            pos += n;
        } while (pos < raw.length);
        long a = 1;
        long b = 0;
        for (int i = 0; i < raw.length; i++) {
            a = (a + (raw[i] & 0xff)) % 65521;
            b = (b + a) % 65521;
        }
        writeInt(z, (int) ((b << 16) | a));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        writeInt(ihdr, w);
        writeInt(ihdr, h);
        ihdr.write(8);
        ihdr.write(6);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        chunk(out, "IHDR", ihdr.toByteArray());
        chunk(out, "IDAT", z.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void writeInt(ByteArrayOutputStream o, int v) {
        o.write((v >>> 24) & 0xff);
        o.write((v >>> 16) & 0xff);
        o.write((v >>> 8) & 0xff);
        o.write(v & 0xff);
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] t = new byte[] {(byte) type.charAt(0), (byte) type.charAt(1), (byte) type.charAt(2),
            (byte) type.charAt(3)};
        writeInt(out, data.length);
        out.write(t);
        out.write(data);
        int crc = 0xffffffff;
        crc = crc(crc, t);
        crc = crc(crc, data);
        writeInt(out, ~crc);
    }

    private static int crc(int crc, byte[] b) {
        int c = crc;
        for (int i = 0; i < b.length; i++) {
            c ^= b[i] & 0xff;
            for (int k = 0; k < 8; k++) {
                c = (c & 1) != 0 ? (c >>> 1) ^ 0xedb88320 : c >>> 1;
            }
        }
        return c;
    }
}
