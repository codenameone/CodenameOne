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
package com.codename1.desktopcompat.javax.imageio;

import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.java.awt.image.RenderedImage;
import com.codename1.desktopcompat.rt.ImageLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/// Reads an encoded image into a [BufferedImage] and writes one out as
/// PNG or JPEG.
///
/// Decoding and encoding are the device's, through Codename One's `Image`
/// and its `ImageIO`. An image is read whole into memory, as type
/// `TYPE_INT_ARGB`.
///
/// What differs from the desktop: the formats are the ones the device
/// has, PNG and JPEG everywhere and usually nothing else to write; there
/// are no readers, writers, streams or plugins to ask for, and no cache
/// settings. `write` answers `false`, as it does on the desktop for a
/// format without a writer, when the device cannot encode at all.
public final class ImageIO {

    private ImageIO() {
    }

    /// Decodes a stream, which is read to its end and left open.
    ///
    /// #### Returns
    ///
    /// the image, or `null` when the bytes are not an image the device
    /// decodes
    public static BufferedImage read(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        return toBuffered(ImageLoader.decode(input));
    }

    public static BufferedImage read(File input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if (!input.canRead()) {
            throw new IOException("Can't read input file!");
        }
        InputStream in = new FileInputStream(input);
        try {
            return read(in);
        } finally {
            in.close();
        }
    }

    public static BufferedImage read(URL input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        InputStream in;
        try {
            in = input.openStream();
        } catch (IOException e) {
            throw new IOException("Can't get input stream from URL!");
        }
        try {
            return read(in);
        } finally {
            in.close();
        }
    }

    private static BufferedImage toBuffered(com.codename1.ui.Image decoded) {
        if (decoded == null) {
            return null;
        }
        int w = decoded.getWidth();
        int h = decoded.getHeight();
        int[] rgb = decoded.getRGB();
        if (w <= 0 || h <= 0 || rgb == null || rgb.length < w * h) {
            return null;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, rgb, 0, w);
        return out;
    }

    /// The Codename One name of a format name, or `null` for one it does
    /// not write. Format names are ASCII and compared without case.
    private static String format(String formatName) {
        if ("png".equalsIgnoreCase(formatName)) {
            return com.codename1.ui.util.ImageIO.FORMAT_PNG;
        }
        if ("jpg".equalsIgnoreCase(formatName) || "jpeg".equalsIgnoreCase(formatName)) {
            return com.codename1.ui.util.ImageIO.FORMAT_JPEG;
        }
        return null;
    }

    /// Encodes an image to a stream, which is left open.
    ///
    /// #### Returns
    ///
    /// `false` when nothing was written: the format is not `png`, `jpg` or
    /// `jpeg`, the device has no encoder for it, or the image is not a
    /// [BufferedImage]
    public static boolean write(RenderedImage im, String formatName, OutputStream output) throws IOException {
        if (im == null) {
            throw new IllegalArgumentException("im == null!");
        }
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        String format = format(formatName);
        com.codename1.ui.util.ImageIO io = com.codename1.ui.util.ImageIO.getImageIO();
        if (format == null || io == null || !io.isFormatSupported(format) || !(im instanceof BufferedImage)) {
            return false;
        }
        com.codename1.ui.Image pixels = ((BufferedImage) im).cn1Image();
        if (pixels == null) {
            return false;
        }
        io.save(pixels, output, format, 0.9f);
        output.flush();
        return true;
    }

    /// Encodes an image to a file, replacing it. Nothing is written, and
    /// an existing file is left as it was, when the answer is `false`.
    public static boolean write(RenderedImage im, String formatName, File output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        if (im == null) {
            throw new IllegalArgumentException("im == null!");
        }
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        String format = format(formatName);
        com.codename1.ui.util.ImageIO io = com.codename1.ui.util.ImageIO.getImageIO();
        if (format == null || io == null || !io.isFormatSupported(format) || !(im instanceof BufferedImage)) {
            return false;
        }
        OutputStream out = new FileOutputStream(output);
        try {
            return write(im, formatName, out);
        } finally {
            out.close();
        }
    }
}
