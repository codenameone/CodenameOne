/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
