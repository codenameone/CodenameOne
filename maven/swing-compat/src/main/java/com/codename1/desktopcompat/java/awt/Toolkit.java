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
package com.codename1.desktopcompat.java.awt;

import com.codename1.desktopcompat.java.awt.datatransfer.Clipboard;
import com.codename1.desktopcompat.rt.DesktopToolkit;
import java.net.URL;

/// The few toolkit services this layer has: screen metrics, font metrics,
/// image decoding and the event queue.
public abstract class Toolkit {

    private static Toolkit toolkit;

    public Toolkit() {
    }

    public static Toolkit getDefaultToolkit() {
        if (toolkit == null) {
            toolkit = new DesktopToolkit();
        }
        return toolkit;
    }

    /// The display's size in logical pixels.
    public abstract Dimension getScreenSize();

    /// Always 96: a logical pixel is 1/96 inch.
    public abstract int getScreenResolution();

    public abstract FontMetrics getFontMetrics(Font font);

    /// Does nothing: painting is flushed by Codename One.
    public abstract void sync();

    public abstract void beep();

    /// Decodes an encoded image (PNG, JPEG) at once.
    public abstract Image createImage(byte[] imagedata, int imageoffset, int imagelength);

    /// The image a path names: the file, or else the application's
    /// resource of the path's last name. It is read here and now; an image
    /// that cannot be read has a width and height of -1.
    public abstract Image getImage(String filename);

    /// The image a URL names, read here and now.
    public abstract Image getImage(URL url);

    /// As [#getImage(String)]; no image is shared between callers either
    /// way.
    public abstract Image createImage(String filename);

    public abstract Image createImage(URL url);

    /// The device's clipboard. It carries text.
    public abstract Clipboard getSystemClipboard();

    public Image createImage(byte[] imagedata) {
        return createImage(imagedata, 0, imagedata.length);
    }

    /// Always empty: the size a window gets is already the room it has.
    public Insets getScreenInsets(GraphicsConfiguration gc) {
        return new Insets(0, 0, 0, 0);
    }

    /// The control key everywhere. Menu accelerators given with it are
    /// shown with the platform's own primary modifier in a native menu.
    public int getMenuShortcutKeyMask() {
        return com.codename1.desktopcompat.java.awt.event.InputEvent.CTRL_MASK;
    }

    /// Whether a frame can be put in a state; true for the normal state
    /// everywhere, and for the others where there is a window manager.
    public boolean isFrameStateSupported(int state) {
        return state == Frame.NORMAL || com.codename1.ui.Desktop.isSupported();
    }

    protected abstract EventQueue getSystemEventQueueImpl();

    public final EventQueue getSystemEventQueue() {
        return getSystemEventQueueImpl();
    }
}
