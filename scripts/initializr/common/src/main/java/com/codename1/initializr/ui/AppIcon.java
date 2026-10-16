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
package com.codename1.initializr.ui;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// The icon a generated project starts with when its colour is chosen here: the
/// first letter of the application's name on that colour.
///
/// It is a placeholder a developer can ship with until they have artwork, and it
/// is the one thing that makes two generated apps tell apart on a home screen.
/// The image is square and opaque on purpose: every platform masks an icon to its
/// own shape, and iOS refuses one with transparency.
public final class AppIcon {
    /// The size the build tools scale every platform's icons down from.
    public static final int SIZE = 512;

    private AppIcon() {
    }

    /// The icon for `appName` on `color` (0xRRGGBB), `size` pixels square.
    public static Image create(String appName, int color, int size) {
        Image image = Image.createImage(size, size, 0xff000000 | color);
        Graphics g = image.getGraphics();
        g.setAntiAliased(true);
        g.setAntiAliasedText(true);
        g.setColor(color);
        g.fillRect(0, 0, size, size);
        String letter = initial(appName);
        Font font = Font.createTrueTypeFont(Font.NATIVE_MAIN_BOLD, Font.NATIVE_MAIN_BOLD)
                .derive(size * 0.56f, Font.STYLE_BOLD);
        g.setFont(font);
        g.setColor(readableOn(color));
        g.drawString(letter, (size - font.stringWidth(letter)) / 2, (size - font.getHeight()) / 2);
        return image;
    }

    /// The icon as the PNG a project keeps in `icon.png`, or null where this
    /// platform cannot encode one; the project then keeps the stock icon.
    public static byte[] png(String appName, int color) {
        ImageIO io = ImageIO.getImageIO();
        if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            io.save(create(appName, color, SIZE), out, ImageIO.FORMAT_PNG, 1f);
        } catch (IOException failed) {
            return null;
        }
        byte[] data = out.toByteArray();
        return data.length == 0 ? null : data;
    }

    /// The letter on the icon: the first letter or digit of the name, in upper
    /// case when it is ASCII, and `A` for a name that has none.
    static String initial(String appName) {
        String name = appName == null ? "" : appName.trim();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'a' && c <= 'z') {
                return String.valueOf((char) (c - 'a' + 'A'));
            }
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                return String.valueOf(c);
            }
        }
        return "A";
    }

    /// Near-black or white, whichever reads on `color`.
    static int readableOn(int color) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;
        return (r * 299 + g * 587 + b * 114) / 1000 > 140 ? 0x0b1020 : 0xffffff;
    }
}
