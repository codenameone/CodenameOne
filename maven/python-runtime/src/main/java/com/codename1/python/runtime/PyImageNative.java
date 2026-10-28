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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// PIL's native image codec over Codename One's `Image` and `ImageIO`. Kept
/// out of `PyImage` so that a backend build, which has no UI package, can
/// leave this class out; the generated registry of a client build installs
/// it.
public final class PyImageNative implements PyImage.Codec {
    private PyImageNative() {
    }

    /// Makes PIL decode and encode through the platform; idempotent.
    public static void install() {
        PyImage.setCodec(new PyImageNative());
    }

    @Override
    public boolean available() {
        return com.codename1.ui.Display.isInitialized();
    }

    @Override
    public PyImage.Img decode(byte[] b) {
        com.codename1.ui.Image ci = com.codename1.ui.Image.createImage(b, 0, b.length);
        int w = ci.getWidth();
        int h = ci.getHeight();
        int[] argb = ci.getRGB();
        PyImage.Img r = new PyImage.Img(w, h, 4);
        for (int p = 0; p < w * h; p++) {
            int c = argb[p];
            r.d[p * 4] = (byte) (c >> 16);
            r.d[p * 4 + 1] = (byte) (c >> 8);
            r.d[p * 4 + 2] = (byte) c;
            r.d[p * 4 + 3] = (byte) (c >>> 24);
        }
        return r;
    }

    @Override
    public byte[] encode(PyImage.Img s, String format, float quality) {
        int[] argb = new int[s.w * s.h];
        for (int p = 0; p < argb.length; p++) {
            argb[p] = (s.d[p * 4 + 3] & 0xff) << 24 | (s.d[p * 4] & 0xff) << 16
                    | (s.d[p * 4 + 1] & 0xff) << 8 | (s.d[p * 4 + 2] & 0xff);
        }
        com.codename1.ui.Image ci = com.codename1.ui.Image.createImage(argb, s.w, s.h);
        com.codename1.ui.util.ImageIO io = com.codename1.ui.util.ImageIO.getImageIO();
        if (io == null) {
            throw Py.error(Py.OS_ERROR, "this platform cannot encode " + format);
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try {
            io.save(ci, bo, format, quality);
        } catch (IOException e) {
            throw Py.error(Py.OS_ERROR, String.valueOf(e.getMessage())); // NOPMD PreserveStackTrace - surfaced as a Python OSError
        }
        return bo.toByteArray();
    }
}
