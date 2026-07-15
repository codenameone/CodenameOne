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
package com.codename1.maven.help;

import java.awt.Desktop;
import java.net.URI;

/**
 * Opens a URL in the user's browser. Abstracted behind an interface so the "Get help"
 * flow can be unit-tested without actually launching a browser, and so headless
 * environments degrade gracefully.
 */
public interface BrowserOpener {

    /** @return true if the browser was launched, false if it could not be (headless, etc.). */
    boolean open(String url);

    /** The real implementation, backed by {@link java.awt.Desktop}. Never throws. */
    BrowserOpener DESKTOP = new BrowserOpener() {
        @Override
        public boolean open(String url) {
            if (url == null || url.length() == 0) {
                return false;
            }
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    return true;
                }
            } catch (Throwable ignore) {
                // Headless / no browser / sandbox — caller prints the URL as a fallback.
            }
            return false;
        }
    };
}
