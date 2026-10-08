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

import com.codename1.desktopcompat.rt.Launcher;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/// Opens a web page, a mail message or a file in the application the
/// device has for it.
///
/// Each call hands a URL to Codename One's `Display.execute`. What happens
/// then is the device's business: nothing reports whether an application
/// took the URL, so no call here fails for want of one.
///
/// What differs from the desktop: `EDIT` and `PRINT` are not supported;
/// `open` asks the device to show the file and cannot start a program; a
/// file is not checked for existing first.
public class Desktop {

    /// What a desktop can be asked to do.
    public enum Action {
        OPEN, EDIT, PRINT, MAIL, BROWSE
    }

    private static Desktop desktop;

    private Desktop() {
    }

    public static Desktop getDesktop() {
        if (desktop == null) {
            desktop = new Desktop();
        }
        return desktop;
    }

    /// Always true: every Codename One port can be asked to open a URL.
    public static boolean isDesktopSupported() {
        return true;
    }

    public boolean isSupported(Action action) {
        return action == Action.OPEN || action == Action.MAIL || action == Action.BROWSE;
    }

    /// Opens a URI in the browser.
    public void browse(URI uri) throws IOException {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        Launcher.open(uri.toString());
    }

    /// Opens the mail application with an empty message.
    public void mail() throws IOException {
        Launcher.open("mailto:");
    }

    /// Opens the mail application on a `mailto:` URI.
    public void mail(URI mailtoURI) throws IOException {
        if (mailtoURI == null) {
            throw new NullPointerException("mailtoURI");
        }
        if (!"mailto".equalsIgnoreCase(mailtoURI.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not \"mailto\"");
        }
        Launcher.open(mailtoURI.toString());
    }

    /// Asks the device to show a file.
    public void open(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        String path = file.getAbsolutePath();
        Launcher.open(path.indexOf("://") >= 0 ? path : "file://" + path);
    }
}
