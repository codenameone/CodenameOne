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
        resetOutputs(opts);
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
        resetOutputs(opts);
        if (boundsFromHeader(data, offset, length, opts)) {
            return null;
        }
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
        resetOutputs(opts);
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
            if (boundsFromHeader(data, 0, data.length, opts)) {
                return null;
            }
            Image img;
            try {
                img = Image.createImage(data, 0, data.length);
            } catch (RuntimeException e) {
                return null;
            }
            if (img == null) {
                return null;
            }
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
        resetOutputs(opts);
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

    private static void resetOutputs(Options opts) {
        if (opts != null) {
            opts.outWidth = -1;
            opts.outHeight = -1;
            opts.outMimeType = null;
            opts.outConfig = null;
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

    /// Answers a bounds-only request from the encoded header, so a two-pass
    /// sizing flow over a large camera image does not decode every pixel just
    /// to learn the dimensions. PNG, JPEG and GIF are read here; any other
    /// format, or a header this cannot parse, falls through to a full decode,
    /// which reports the same numbers. Like Android's decoder this ignores
    /// EXIF orientation.
    private static boolean boundsFromHeader(byte[] d, int off, int len, Options opts) {
        if (opts == null || !opts.inJustDecodeBounds) {
            return false;
        }
        String mime = mimeType(d, off, len);
        int[] size = null;
        if ("image/png".equals(mime)) {
            // The IHDR chunk is first: width and height at bytes 16 and 20.
            if (len >= 24 && d[off + 12] == 'I' && d[off + 13] == 'H' && d[off + 14] == 'D' && d[off + 15] == 'R') {
                size = new int[] {be32(d, off + 16), be32(d, off + 20)};
            }
        } else if ("image/gif".equals(mime)) {
            if (len >= 10) {
                size = new int[] {(d[off + 6] & 0xff) | (d[off + 7] & 0xff) << 8,
                    (d[off + 8] & 0xff) | (d[off + 9] & 0xff) << 8};
            }
        } else if ("image/jpeg".equals(mime)) {
            size = jpegSize(d, off, len);
        }
        if (size == null || size[0] <= 0 || size[1] <= 0) {
            return false;
        }
        reportBounds(opts, size[0], size[1], mime);
        return true;
    }

    private static int be32(byte[] d, int i) {
        return (d[i] & 0xff) << 24 | (d[i + 1] & 0xff) << 16 | (d[i + 2] & 0xff) << 8 | (d[i + 3] & 0xff);
    }

    /// The frame size from the first start-of-frame marker, walking the
    /// marker segments by their lengths; null when none is found before the
    /// scan data.
    private static int[] jpegSize(byte[] d, int off, int len) {
        int end = off + len;
        int i = off + 2;
        while (i + 3 < end) {
            if ((d[i] & 0xff) != 0xff) {
                return null;
            }
            int marker = d[i + 1] & 0xff;
            if (marker == 0xff) {
                i++;
                continue;
            }
            if (marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                i += 2;
                continue;
            }
            if (marker == 0xd9 || marker == 0xda) {
                return null;
            }
            int segment = (d[i + 2] & 0xff) << 8 | (d[i + 3] & 0xff);
            if (segment < 2) {
                return null;
            }
            boolean frame = marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
            if (frame) {
                if (i + 8 >= end) {
                    return null;
                }
                int h = (d[i + 5] & 0xff) << 8 | (d[i + 6] & 0xff);
                int w = (d[i + 7] & 0xff) << 8 | (d[i + 8] & 0xff);
                return new int[] {w, h};
            }
            i += 2 + segment;
        }
        return null;
    }

    /// Writes the bounds fields where AOSP's decoder writes them: after
    /// `inSampleSize`, before any density scaling. Answers the sampled size.
    private static int[] reportBounds(Options opts, int w, int h, String mimeType) {
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
        }
        return new int[] {tw, th};
    }

    private static Bitmap finish(Image img, Options opts, int bucketDensity, String mimeType) {
        int[] sampled = reportBounds(opts, img.getWidth(), img.getHeight(), mimeType);
        int tw = sampled[0];
        int th = sampled[1];
        if (opts != null && opts.inJustDecodeBounds) {
            return null;
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
        if (tw != img.getWidth() || th != img.getHeight()) {
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
