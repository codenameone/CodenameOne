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
package com.codename1.fxcompat.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

import javafx.scene.Scene;
import javafx.stage.Window;

/// What the three kinds of host share: keeping the peer of the scene's
/// root in the content pane, and waiting for a window to close.
final class HostCore {

    private final Window window;
    private final Container content;
    private final StageHost host;
    private final AtomicBoolean closed = new AtomicBoolean();
    private Scene scene;
    private Component rootPeer;

    HostCore(Window window, Container content, StageHost host) {
        this.window = window;
        this.content = content;
        this.host = host;
        PeerPaint.strip(content);
        content.setLayout(new BorderLayout());
        content.setScrollable(false);
    }

    Window window() {
        return window;
    }

    Scene scene() {
        return scene;
    }

    Component rootPeer() {
        return rootPeer;
    }

    void sceneChanged() {
        scene = window.getScene();
        content.removeAll();
        rootPeer = null;
        if (scene != null) {
            scene.cn1SetHost(host, window);
            if (scene.getRoot() != null) {
                rootPeer = scene.getRoot().cn1Peer();
                Container holder = rootPeer.getParent();
                if (holder != null) {
                    holder.removeComponent(rootPeer);
                }
                content.addComponent(BorderLayout.CENTER, rootPeer);
            }
        }
        content.revalidate();
    }

    void requestPulse() {
        if (rootPeer != null) {
            rootPeer.setShouldCalcPreferredSize(true);
            content.revalidateLater();
        }
    }

    void showCursor(int cursor) {
        if (rootPeer != null) {
            rootPeer.setCursor(cursor);
        }
    }

    void sized() {
        if (rootPeer != null) {
            double x = window.getX();
            double y = window.getY();
            window.cn1Bounds(Double.isNaN(x) ? 0 : x, Double.isNaN(y) ? 0 : y, Units.toLogical(content.getWidth()),
                    Units.toLogical(content.getHeight()));
        }
    }

    void markClosed() {
        closed.set(true);
    }

    /// Returns once [#markClosed()] was called. The event thread keeps
    /// running: Codename One handles events on a nested loop while the
    /// waiting itself happens on another thread.
    void block() {
        if (!Display.isInitialized() || closed.get()) {
            return;
        }
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (!closed.get()) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException interrupted) {
                        return;
                    }
                }
            }
        });
    }
}
