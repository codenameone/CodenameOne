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
package com.codename1.impl.html5;

public final class JavaScriptBrowserInteractionCoordinator {
    private JavaScriptBrowserInteractionCoordinator() {
    }

    public interface ResizeHooks {
        void waitForResizeStabilization();
        void updateCanvasSize();
        void sizeChanged();
        void revalidate();
    }

    public interface CursorLocator {
        boolean isCursorEnabled();
        int resolveCursorAt(int x, int y);
    }

    public interface HoverHooks {
        void dispatchHover(int x, int y);
        void setCursor(int cursor);
        void callSerially(Runnable runnable);
    }

    public static void handleResize(ResizeHooks hooks) {
        hooks.waitForResizeStabilization();
        hooks.updateCanvasSize();
        hooks.sizeChanged();
        hooks.revalidate();
    }

    public static void handleHover(final HoverHooks hooks, final CursorLocator locator, final int x, final int y, final int defaultCursor) {
        hooks.dispatchHover(x, y);
        if (!locator.isCursorEnabled()) {
            hooks.setCursor(defaultCursor);
            return;
        }
        hooks.callSerially(new Runnable() {
            @Override
            public void run() {
                int cursor = locator.resolveCursorAt(x, y);
                hooks.setCursor(cursor);
            }
        });
    }
}
