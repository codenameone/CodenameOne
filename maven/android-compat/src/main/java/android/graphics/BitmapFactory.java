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
        return finish(img, opts, 0);
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
            return finish(img, opts, bucket);
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

    private static Bitmap finish(Image img, Options opts, int bucketDensity) {
        int w = img.getWidth();
        int h = img.getHeight();
        int sample = opts == null || opts.inSampleSize < 1 ? 1 : opts.inSampleSize;
        int tw = Math.max(1, w / sample);
        int th = Math.max(1, h / sample);
        boolean scale = opts == null || opts.inScaled;
        int target = opts != null && opts.inTargetDensity > 0 ? opts.inTargetDensity
                : Resources.getSystem().getDisplayMetrics().densityDpi;
        int source = opts != null && opts.inDensity > 0 ? opts.inDensity : bucketDensity;
        if (scale && source > 0 && source != 0xffff && target > 0 && source != target) {
            tw = Math.max(1, Math.round(tw * (float) target / source));
            th = Math.max(1, Math.round(th * (float) target / source));
        }
        if (opts != null) {
            opts.outWidth = tw;
            opts.outHeight = th;
            opts.outConfig = Bitmap.Config.ARGB_8888;
            opts.outMimeType = "image/png";
            if (opts.inJustDecodeBounds) {
                return null;
            }
        }
        if (tw != w || th != h) {
            img = img.scaled(tw, th);
        }
        if (opts != null && opts.inMutable) {
            Image m = Image.createImage(tw, th, 0);
            m.getGraphics().drawImage(img, 0, 0);
            Bitmap b = new Bitmap(m, true, opts.inPreferredConfig);
            b.setDensity(target);
            return b;
        }
        Bitmap b = new Bitmap(img, false, opts == null ? Bitmap.Config.ARGB_8888 : opts.inPreferredConfig);
        b.setDensity(target);
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
