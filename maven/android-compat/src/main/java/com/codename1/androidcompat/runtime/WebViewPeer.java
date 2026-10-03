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
package com.codename1.androidcompat.runtime;

import android.view.View;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

/// The peer of a `WebView`: a container holding the native browser, which
/// fills it. The view paints its background underneath. Unlike other peers
/// the browser takes pointer events itself, because only the native view can
/// scroll, zoom and follow links; touch listeners on the `WebView` therefore
/// see no events.
public final class WebViewPeer extends Container {

    private final View view;
    private final BrowserComponent browser;

    public WebViewPeer(View view, BrowserComponent browser) {
        super(new EditTextPeer.FillLayout());
        this.view = view;
        this.browser = browser;
        ViewPeer.strip(this);
        setFocusable(false);
        if (browser.getParent() != null) {
            browser.getParent().removeComponent(browser);
        }
        addComponent(browser);
    }

    public BrowserComponent getBrowser() {
        return browser;
    }

    @Override
    public void paint(Graphics g) {
        view.paintPeer(g, getX(), getY());
        super.paint(g);
    }

    @Override
    protected void paintBackground(Graphics g) {
    }

    @Override
    protected Dimension calcPreferredSize() {
        return ViewPeer.measureUnbounded(view);
    }
}
