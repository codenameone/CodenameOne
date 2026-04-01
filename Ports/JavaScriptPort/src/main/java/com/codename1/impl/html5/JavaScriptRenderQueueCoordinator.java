/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import java.util.List;

public final class JavaScriptRenderQueueCoordinator {
    private JavaScriptRenderQueueCoordinator() {
    }

    public interface FlushBarrier {
        boolean isGraphicsLocked();
        void sleep(int millis) throws InterruptedException;
    }

    public interface GraphicsLock {
        void setGraphicsLocked(boolean locked);
    }

    public static void waitUntilFlushable(FlushBarrier barrier, JavaScriptRenderQueueState<?> state) {
        while (barrier.isGraphicsLocked() || state.hasPendingOps()) {
            try {
                barrier.sleep(1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static <T> void queueFlush(GraphicsLock lock, JavaScriptRenderQueueState<T> state, List<T> ops, int x, int y, int width, int height) {
        lock.setGraphicsLocked(true);
        try {
            state.replace(ops, x, y, width, height);
        } finally {
            lock.setGraphicsLocked(false);
        }
    }

    public static <T> JavaScriptRenderQueueState.FrameSnapshot<T> beginFrame(GraphicsLock lock, JavaScriptRenderQueueState<T> state) {
        lock.setGraphicsLocked(true);
        try {
            return state.snapshotAndClear();
        } finally {
            lock.setGraphicsLocked(false);
        }
    }
}
