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
package com.codename1.impl.linux;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.UITimer;

/// Native Linux BrowserComponent peer backed by a WebKitGTK WebView (the native
/// lifecycle lives in cn1_linux_browser.c). The peer polls the native event
/// queue to fire `onLoad` and to route the JS return-value bridge into the
/// BrowserComponent's navigation callbacks.
///
/// `generatePeerImage()` is wired to a native PNG capture so the view can be
/// drawn into an offscreen screenshot, where the live WebKit widget would not
/// appear. That capture is **not implemented yet**: WebKit snapshots are async
/// (webkit_web_view_get_snapshot) and `browserCapturePng` currently returns
/// null pending that bridge, so `generatePeerImage()` returns null and the peer
/// falls back to the live widget. Do not read this as a description of working
/// behaviour -- it is the shape the capture will take once the snapshot bridge
/// lands.
///
/// (This description previously named WebView2, Direct2D and a .cpp file, none
/// of which exist here -- it had been copied from the Windows port.)
class LinuxBrowserComponent extends PeerComponent {
    private final long peer;
    private final BrowserComponent browser;
    private UITimer poller;

    LinuxBrowserComponent(BrowserComponent browser) {
        super(null);
        this.browser = browser;
        // Slot resolved from the BrowserComponent, so the WebKit view is hosted by
        // the window the component is in rather than always by the main window.
        this.peer = LinuxNative.browserCreate(800, 600,
                LinuxWindowManager.slotForComponent(browser));
    }

    long peer() {
        return peer;
    }

    @Override
    protected boolean shouldRenderPeerImage() {
        return true;
    }

    @Override
    protected Image generatePeerImage() {
        byte[] png = LinuxNative.browserCapturePng(peer);
        if (png == null) {
            return null;
        }
        try {
            return EncodedImage.create(png);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(Display.getInstance().getDisplayWidth() / 2,
                Display.getInstance().getDisplayHeight() / 2);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        LinuxNative.browserSetBounds(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
        LinuxNative.browserSetVisible(peer, true);
        if (poller == null && getComponentForm() != null) {
            poller = UITimer.timer(60, true, getComponentForm(), new Runnable() {
                public void run() {
                    poll();
                }
            });
        }
    }

    @Override
    protected void deinitialize() {
        if (poller != null) {
            poller.cancel();
            poller = null;
        }
        // Hide the native WebKit widget so it does not stay floating in the window
        // overlay over the next form once this BrowserComponent's form is gone.
        LinuxNative.browserSetVisible(peer, false);
        super.deinitialize();
    }

    @Override
    protected void onPositionSizeChange() {
        super.onPositionSizeChange();
        LinuxNative.browserSetBounds(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
    }

    void setHtml(String html) {
        LinuxNative.browserSetHtml(peer, html);
    }

    void setUrl(String url) {
        LinuxNative.browserSetUrl(peer, url);
    }

    void execute(String js) {
        LinuxNative.browserExecute(peer, js);
    }

    void destroy() {
        LinuxNative.browserDestroy(peer);
    }

    private void poll() {
        String ev;
        while ((ev = LinuxNative.browserPollEvent(peer)) != null) {
            if ("LOAD".equals(ev)) {
                Image img = generatePeerImage();
                if (img != null) {
                    setPeerImage(img);
                    repaint();
                }
                browser.fireWebEvent(BrowserComponent.onLoad, new ActionEvent(""));
            } else if (ev.startsWith("NAV|")) {
                browser.fireBrowserNavigationCallbacks(ev.substring(4));
            } else if (ev.startsWith("MSG|")) {
                // The JS->Java bridge. The page's cn1application.shouldNavigate
                // posts here rather than assigning window.location, so an
                // execute() return value comes back through the message handler
                // instead of a navigation to codenameone.com -- which needed
                // working egress to deliver a callback and took the page under
                // test away with it. Same sink as a navigation callback: the
                // portable layer decodes the return-value URL either way.
                browser.fireBrowserNavigationCallbacks(ev.substring(4));
            }
        }
    }
}
