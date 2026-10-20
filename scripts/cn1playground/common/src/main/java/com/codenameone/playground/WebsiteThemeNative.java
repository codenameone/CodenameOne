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

package com.codenameone.playground;

import com.codename1.system.NativeInterface;

public interface WebsiteThemeNative extends NativeInterface {
    boolean isDarkMode();
    void notifyUiReady();

    /// The host-page URL (including query string) as seen by the browser. On the
    /// JavaScript port ``CN.getProperty("browser.window.location.href")`` is
    /// unreliable -- the call is inlined/devirtualised past
    /// ``HTML5Implementation.getProperty`` to the base implementation, which just
    /// returns the default -- so deep links (``?sample=`` / ``?code=`` / ``?css=``)
    /// were never seen. This bridges straight to the page (and its parent frame
    /// when embedded) where the real URL lives. Returns ``null`` off-browser.
    String locationHref();

    /// Inline mode (the Playground embedded under a documentation snippet): the
    /// code the host page most recently asked this frame to show, cleared by the
    /// read, or ``null`` when there is none. The page reuses one frame for every
    /// snippet it opens, so moving to another snippet costs a recompile, not a
    /// second VM boot.
    String takeInlineCode();

    /// Inline mode: tells the host page how the last run went. ``summary`` is the
    /// first error, or empty when ``ok``; ``sourceUrl`` is the full Playground
    /// carrying the reader's edits. The host page opens that one from its own
    /// button: a window opened from inside this frame, after the click has
    /// crossed to the EDT, is no longer a user gesture and gets popup-blocked.
    void notifyInlineResult(boolean ok, String summary, String sourceUrl);
}
