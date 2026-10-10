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

import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.EventQueue;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.Toolkit;
import com.codename1.desktopcompat.java.awt.datatransfer.Clipboard;
import java.net.URL;
import com.codename1.ui.Display;

/// The one toolkit there is.
public final class DesktopToolkit extends Toolkit {

    private final EventQueue queue = new EventQueue();
    private Clipboard clipboard;

    @Override
    public Dimension getScreenSize() {
        if (!Display.isInitialized()) {
            return new Dimension(0, 0);
        }
        Display d = Display.getInstance();
        return new Dimension(Units.toLogical(d.getDisplayWidth()), Units.toLogical(d.getDisplayHeight()));
    }

    @Override
    public int getScreenResolution() {
        return 96;
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return Fonts.metrics(font);
    }

    @Override
    public void sync() {
    }

    @Override
    public void beep() {
    }

    @Override
    public Image createImage(byte[] imagedata, int imageoffset, int imagelength) {
        return ImageLoader.wrap(ImageLoader.decode(imagedata, imageoffset, imagelength));
    }

    @Override
    public Image getImage(String filename) {
        return ImageLoader.wrap(ImageLoader.fromPath(filename));
    }

    @Override
    public Image getImage(URL url) {
        return ImageLoader.wrap(ImageLoader.fromUrl(url));
    }

    @Override
    public Image createImage(String filename) {
        return getImage(filename);
    }

    @Override
    public Image createImage(URL url) {
        return getImage(url);
    }

    @Override
    public Clipboard getSystemClipboard() {
        if (clipboard == null) {
            clipboard = new SystemClipboard();
        }
        return clipboard;
    }

    @Override
    protected EventQueue getSystemEventQueueImpl() {
        return queue;
    }
}
