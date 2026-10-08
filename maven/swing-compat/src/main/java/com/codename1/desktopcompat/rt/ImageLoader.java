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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.ui.Display;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/// Reads encoded images (PNG, JPEG, whatever the device decodes) from the
/// places a desktop application names them by: a file, a URL, a stream or
/// bytes in memory.
public final class ImageLoader {

    private ImageLoader() {
    }

    /// Everything a stream holds; the stream is left open.
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n = in.read(buffer);
        while (n >= 0) {
            out.write(buffer, 0, n);
            n = in.read(buffer);
        }
        return out.toByteArray();
    }

    /// Decodes an image, or answers `null` for bytes that are not one.
    public static com.codename1.ui.Image decode(byte[] data, int offset, int length) {
        if (data == null || length <= 0 || !Display.isInitialized()) {
            return null;
        }
        com.codename1.ui.Image img;
        try {
            img = com.codename1.ui.Image.createImage(data, offset, length);
        } catch (RuntimeException e) {
            // A port reports bytes it cannot decode this way.
            return null;
        }
        return img;
    }

    /// Reads a stream to its end and decodes it; the stream is left open.
    public static com.codename1.ui.Image decode(InputStream in) throws IOException {
        byte[] data = readAll(in);
        return decode(data, 0, data.length);
    }

    /// The image a URL names, or `null` when it cannot be read or is not
    /// an image.
    public static com.codename1.ui.Image fromUrl(URL url) {
        if (url == null) {
            return null;
        }
        try {
            InputStream in = url.openStream();
            try {
                return decode(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /// The image a path names: the file when there is one, else the
    /// resource of the application with the path's last name. A desktop
    /// application reads `images/logo.png` from its working directory; on
    /// a device the same picture is a resource the build packaged.
    public static com.codename1.ui.Image fromPath(String filename) {
        if (filename == null || !Display.isInitialized()) {
            return null;
        }
        try {
            File file = new File(filename);
            InputStream in = null;
            if (file.isFile()) {
                in = new FileInputStream(file);
            } else {
                String name = filename;
                int cut = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                if (cut >= 0) {
                    name = name.substring(cut + 1);
                }
                if (name.length() > 0) {
                    in = Display.getInstance().getResourceAsStream(null, "/" + name);
                }
            }
            if (in == null) {
                return null;
            }
            try {
                return decode(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /// An AWT image of a decoded one. An image that could not be read is
    /// an image without pixels, of width and height -1, as on the desktop.
    public static Image wrap(com.codename1.ui.Image image) {
        return new NativeImage(image);
    }
}
