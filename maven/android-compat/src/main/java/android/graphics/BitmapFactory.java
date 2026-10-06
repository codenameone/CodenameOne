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
package android.graphics;

import android.content.res.Resources;
import android.util.TypedValue;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// Decodes images into bitmaps through Codename One's native image decoders.
public class BitmapFactory {

    public static class Options {
        public boolean inJustDecodeBounds;
        public int inSampleSize = 1;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inMutable;
        public boolean inScaled = true;
        public int inDensity;
        public int inTargetDensity;
        public int inScreenDensity;
        public boolean inDither;
        public boolean inPremultiplied = true;
        public Bitmap inBitmap;
        public int outWidth;
        public int outHeight;
        public String outMimeType;
        public Bitmap.Config outConfig;
    }

    public static Bitmap decodeStream(InputStream is) {
        return decodeStream(is, null, null);
    }

    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        if (is == null) {
            return null;
        }
        try {
            byte[] data = readAll(is);
            return decodeByteArray(data, 0, data.length, opts);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeByteArray(data, offset, length, null);
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length, Options opts) {
        Image img;
        try {
            img = Image.createImage(data, offset, length);
        } catch (RuntimeException e) {
            return null;
        }
        if (img == null) {
            return null;
        }
        return finish(img, opts, 0, mimeType(data, offset, length));
    }

    public static Bitmap decodeResource(Resources res, int id) {
        return decodeResource(res, id, null);
    }

    /// Decodes a drawable resource and, like Android, scales it from the
    /// density bucket it came from to the device's density.
    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        TypedValue value = new TypedValue();
        InputStream in;
        try {
            in = res.openRawResource(id, value);
        } catch (Resources.NotFoundException e) {
            return null;
        }
        int bucket = com.codename1.androidcompat.runtime.DrawableInflater.densityOf(res, id);
        try {
            byte[] data = readAll(in);
            Image img = Image.createImage(data, 0, data.length);
            return finish(img, opts, bucket, mimeType(data, 0, data.length));
        } catch (IOException e) {
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // Nothing to recover.
            }
        }
    }

    public static Bitmap decodeFile(String pathName) {
        return decodeFile(pathName, null);
    }

    public static Bitmap decodeFile(String pathName, Options opts) {
        try {
            InputStream in = FileSystemStorage.getInstance().openInputStream(pathName);
            try {
                return decodeStream(in, null, opts);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /// The MIME type of encoded image data, read from its signature bytes
    /// the way Android's decoder reports it; null when unrecognized.
    static String mimeType(byte[] d, int off, int len) {
        if (d == null || len < 3 || off < 0 || off + len > d.length) {
            return null;
        }
        if (len >= 8 && (d[off] & 0xff) == 0x89 && d[off + 1] == 'P' && d[off + 2] == 'N' && d[off + 3] == 'G') {
            return "image/png";
        }
        if ((d[off] & 0xff) == 0xff && (d[off + 1] & 0xff) == 0xd8 && (d[off + 2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (len >= 6 && d[off] == 'G' && d[off + 1] == 'I' && d[off + 2] == 'F' && d[off + 3] == '8') {
            return "image/gif";
        }
        if (len >= 12 && d[off] == 'R' && d[off + 1] == 'I' && d[off + 2] == 'F' && d[off + 3] == 'F'
                && d[off + 8] == 'W' && d[off + 9] == 'E' && d[off + 10] == 'B' && d[off + 11] == 'P') {
            return "image/webp";
        }
        if (d[off] == 'B' && d[off + 1] == 'M') {
            return "image/bmp";
        }
        return null;
    }

    private static Bitmap finish(Image img, Options opts, int bucketDensity, String mimeType) {
        int w = img.getWidth();
        int h = img.getHeight();
        // Any inSampleSize divides the dimensions as given, not rounded down to
        // a power of two: the Options javadoc still says it rounds, but AOSP's
        // decoder (libs/hwui/jni/BitmapFactory.cpp, needsFineScale) has
        // fine-scaled to exactly width / inSampleSize since SkAndroidCodec
        // replaced the old decoders, so 3 really yields a third.
        int sample = opts == null || opts.inSampleSize < 1 ? 1 : opts.inSampleSize;
        int tw = Math.max(1, w / sample);
        int th = Math.max(1, h / sample);
        if (opts != null) {
            // Reported before the density scaling, exactly where AOSP's
            // decoder writes them: outWidth/outHeight include inSampleSize
            // but never the density ratio, in a bounds-only probe and in a
            // full decode alike. A two-pass sizing flow over a resource
            // therefore sees the image's own size, not the scaled one.
            opts.outWidth = tw;
            opts.outHeight = th;
            opts.outConfig = Bitmap.Config.ARGB_8888;
            opts.outMimeType = mimeType;
            if (opts.inJustDecodeBounds) {
                return null;
            }
        }
        boolean scale = opts == null || opts.inScaled;
        int target = opts != null && opts.inTargetDensity > 0 ? opts.inTargetDensity
                : Resources.getSystem().getDisplayMetrics().densityDpi;
        int source = opts != null && opts.inDensity > 0 ? opts.inDensity : bucketDensity;
        // The bitmap reports the density its pixels are at: the target's
        // when they were scaled to it, otherwise the source's (AOSP's
        // setDensityFromOptions), so an unscaled mdpi decode on an xxhdpi
        // device still says mdpi and density-aware drawing scales it.
        // Without a known source the target stands, as on Android.
        int density = target;
        if (source > 0 && source != 0xffff && target > 0 && source != target) {
            if (scale) {
                tw = Math.max(1, Math.round(tw * (float) target / source));
                th = Math.max(1, Math.round(th * (float) target / source));
            } else {
                density = source;
            }
        }
        if (tw != w || th != h) {
            img = img.scaled(tw, th);
        }
        if (opts != null && opts.inMutable) {
            Image m = Image.createImage(tw, th, 0);
            m.getGraphics().drawImage(img, 0, 0);
            Bitmap b = new Bitmap(m, true, opts.inPreferredConfig);
            b.setDensity(density);
            return b;
        }
        Bitmap b = new Bitmap(img, false, opts == null ? Bitmap.Config.ARGB_8888 : opts.inPreferredConfig);
        b.setDensity(density);
        return b;
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
