/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.pdf;

import java.io.ByteArrayOutputStream;
import java.util.List;

/// Helpers for page content streams.
@com.codename1.impl.SharedWithBackend
public final class PdfContent {
    private PdfContent() {
    }

    /// The decoded data of a `Contents` entry: one stream, or an array
    /// of streams joined with a separating newline. A stream that cannot
    /// be decoded is skipped.
    public static byte[] concat(PdfFile file, Object contents) {
        Object c = file != null ? file.resolve(contents) : contents;
        if (c instanceof PdfStream) {
            try {
                return ((PdfStream) c).getData();
            } catch (java.io.IOException e) {
                return new byte[0];
            }
        }
        if (!(c instanceof List)) {
            return new byte[0];
        }
        List<?> l = (List<?>) c;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        for (int i = 0; i < l.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object s = file != null ? file.resolve(l.get(i)) : l.get(i);
            if (s instanceof PdfStream) {
                try {
                    byte[] d = ((PdfStream) s).getData();
                    bo.write(d, 0, d.length);
                    bo.write('\n');
                } catch (java.io.IOException e) {
                    // skip
                }
            }
        }
        return bo.toByteArray();
    }

    /// The visible area of a page as `{x, y, width, height}` in
    /// points, in the page's own unrotated coordinates: its crop box,
    /// clipped to the media box.
    public static double[] box(PdfDict page) {
        double[] media = rect(page.getArray("MediaBox"), new double[] {0, 0, 612, 792});
        double[] crop = rect(page.getArray("CropBox"), media);
        double x0 = Math.max(media[0], crop[0]);
        double y0 = Math.max(media[1], crop[1]);
        double x1 = Math.min(media[2], crop[2]);
        double y1 = Math.min(media[3], crop[3]);
        if (x1 <= x0 || y1 <= y0) {
            x0 = media[0];
            y0 = media[1];
            x1 = media[2];
            y1 = media[3];
        }
        return new double[] {x0, y0, x1 - x0, y1 - y0};
    }

    private static double[] rect(List<Object> a, double[] def) {
        if (a == null || a.size() < 4) {
            return def;
        }
        double[] r = new double[4];
        for (int i = 0; i < 4; i++) {
            Object o = a.get(i);
            if (!(o instanceof Number)) {
                return def;
            }
            r[i] = ((Number) o).doubleValue();
        }
        return new double[] {Math.min(r[0], r[2]), Math.min(r[1], r[3]), Math.max(r[0], r[2]),
            Math.max(r[1], r[3])};
    }

    /// The clockwise rotation of a page in degrees: 0, 90, 180 or 270.
    public static int rotation(PdfDict page) {
        int r = page.getInt("Rotate", 0) % 360;
        if (r < 0) {
            r += 360;
        }
        return r / 90 * 90;
    }

}
