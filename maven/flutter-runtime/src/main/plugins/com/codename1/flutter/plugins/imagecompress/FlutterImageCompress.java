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
package com.codename1.flutter.plugins.imagecompress;

import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.plugins.crossfile.XFile;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dart.async.Future;
import dart.typed_data.Uint8List;

/**
 * flutter_image_compress's {@code FlutterImageCompress}, over {@code Image} and
 * {@code ImageIO}. As in the plugin, {@code minWidth}/{@code minHeight} are the smallest
 * size the result may have: the image is scaled down by the smaller of the two ratios and
 * never up; {@code inSampleSize} divides it further; {@code rotate} turns it by a multiple
 * of 90 degrees; {@code quality} is the JPEG quality. JPEG and PNG are written, HEIC and
 * WebP only where the platform's ImageIO supports them (otherwise the future fails, as the
 * plugin's does on an unsupported format). EXIF is not kept and
 * {@code autoCorrectionAngle} is not applied. With no display there is no codec, and the
 * future fails with {@code UnsupportedError} rather than answering the input as if it had
 * been compressed.
 */
public final class FlutterImageCompress {

    private FlutterImageCompress() {
    }

    private static long or(Long v, long def) {
        return v == null ? def : v.longValue();
    }

    static String formatName(CompressFormat f) {
        if (f == CompressFormat.png) {
            return ImageIO.FORMAT_PNG;
        }
        if (f == CompressFormat.heic) {
            return "heic";
        }
        if (f == CompressFormat.webp) {
            return "webp";
        }
        return ImageIO.FORMAT_JPEG;
    }

    static byte[] compress(byte[] src, Long minWidth, Long minHeight, Long inSampleSize, Long quality, Long rotate,
            CompressFormat format) throws IOException {
        ImageIO io = Display.isInitialized() ? ImageIO.getImageIO() : null;
        if (io == null) {
            throw new dart.core.UnsupportedError("flutter_image_compress: no image codec on this platform");
        }
        String fmt = formatName(format);
        if (!io.isFormatSupported(fmt)) {
            throw new dart.core.UnsupportedError("flutter_image_compress: " + fmt + " is not supported here");
        }
        Image img = Image.createImage(src, 0, src.length);
        int w = img.getWidth();
        int h = img.getHeight();
        double minW = Math.max(1, or(minWidth, 1920));
        double minH = Math.max(1, or(minHeight, 1080));
        double scale = Math.max(1.0, Math.min(w / minW, h / minH));
        long sample = Math.max(1, or(inSampleSize, 1));
        int dw = Math.max(1, (int) (w / scale / sample));
        int dh = Math.max(1, (int) (h / scale / sample));
        if (dw != w || dh != h) {
            img = img.scaled(dw, dh);
        }
        int r = (int) (or(rotate, 0) % 360);
        if (r < 0) {
            r += 360;
        }
        if (r == 90) {
            img = img.rotate90Degrees(true);
        } else if (r == 180) {
            img = img.rotate180Degrees(true);
        } else if (r == 270) {
            img = img.rotate270Degrees(true);
        }
        float q = Math.max(0, Math.min(100, or(quality, 95))) / 100f;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        io.save(img, out, fmt, q);
        return out.toByteArray();
    }

    private static byte[] readFile(String path) throws IOException {
        InputStream in = FileSystemStorage.getInstance().openInputStream(path);
        try {
            return Util.readInputStream(in);
        } finally {
            Util.cleanup(in);
        }
    }

    public static Future<Uint8List> compressWithFile(String path, Long minWidth, Long minHeight, Long inSampleSize,
            Long quality, Long rotate, Boolean autoCorrectionAngle, CompressFormat format, Boolean keepExif,
            Long numberOfRetries) {
        try {
            if (!Display.isInitialized()) {
                return Future.error(new dart.core.UnsupportedError(
                        "flutter_image_compress: no image codec on this platform"));
            }
            if (!FileSystemStorage.getInstance().exists(path)) {
                return Future.value(null);
            }
            return Future.value(Uint8List.fromBytes(
                    compress(readFile(path), minWidth, minHeight, inSampleSize, quality, rotate, format)));
        } catch (Throwable t) {
            return Future.error(t);
        }
    }

    public static Future<XFile> compressAndGetFile(String path, String targetPath, Long minWidth, Long minHeight,
            Long inSampleSize, Long quality, Long rotate, Boolean autoCorrectionAngle, CompressFormat format,
            Boolean keepExif, Long numberOfRetries) {
        if (path != null && path.equals(targetPath)) {
            return Future.error(new dart.core.ArgumentError("Target path and source path cannot be the same."));
        }
        try {
            if (!Display.isInitialized()) {
                return Future.error(new dart.core.UnsupportedError(
                        "flutter_image_compress: no image codec on this platform"));
            }
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (!fs.exists(path)) {
                return Future.value(null);
            }
            byte[] out = compress(readFile(path), minWidth, minHeight, inSampleSize, quality, rotate, format);
            OutputStream os = fs.openOutputStream(targetPath);
            try {
                os.write(out);
            } finally {
                Util.cleanup(os);
            }
            return Future.value(new XFile(targetPath));
        } catch (Throwable t) {
            return Future.error(t);
        }
    }

    public static Future<Uint8List> compressWithList(Uint8List image, Long minWidth, Long minHeight, Long quality,
            Long rotate, Long inSampleSize, Boolean autoCorrectionAngle, CompressFormat format, Boolean keepExif) {
        try {
            return Future.value(Uint8List.fromBytes(
                    compress(image.toBytes(), minWidth, minHeight, inSampleSize, quality, rotate, format)));
        } catch (Throwable t) {
            return Future.error(t);
        }
    }

    public static Future<Uint8List> compressAssetImage(String assetName, Long minWidth, Long minHeight, Long quality,
            Long rotate, Boolean autoCorrectionAngle, CompressFormat format, Boolean keepExif) {
        try {
            if (!Display.isInitialized()) {
                return Future.error(new dart.core.UnsupportedError(
                        "flutter_image_compress: no image codec on this platform"));
            }
            InputStream in = Display.getInstance().getResourceAsStream(FlutterImageCompress.class,
                    FlutterAssets.resourceName(assetName));
            if (in == null) {
                return Future.value(null);
            }
            byte[] src;
            try {
                src = Util.readInputStream(in);
            } finally {
                Util.cleanup(in);
            }
            return Future.value(Uint8List.fromBytes(compress(src, minWidth, minHeight, null, quality, rotate, format)));
        } catch (Throwable t) {
            return Future.error(t);
        }
    }
}
