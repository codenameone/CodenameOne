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
package pilclienttest;

import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The image encoder of the headless client run: a port's `ImageIO`, written
/// over the JDK's (headless) image writers. Installed by ConformanceTest into
/// `HeadlessImplementation.imageIO`.
public class JdkImageIO extends ImageIO {
    @Override
    public void save(InputStream image, OutputStream response, String format, int width, int height,
            float quality) throws IOException {
        saveImage(Image.createImage(image).scaled(width, height), response, format, quality);
    }

    @Override
    protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean jpeg = FORMAT_JPEG.equals(format);
        BufferedImage b = new BufferedImage(w, h, jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        b.setRGB(0, 0, w, h, img.getRGB(), 0, w);
        if (!javax.imageio.ImageIO.write(b, jpeg ? "jpeg" : "png", response)) {
            throw new IOException("no writer for " + format);
        }
    }

    @Override
    public boolean isFormatSupported(String format) {
        return FORMAT_JPEG.equals(format) || FORMAT_PNG.equals(format);
    }
}
