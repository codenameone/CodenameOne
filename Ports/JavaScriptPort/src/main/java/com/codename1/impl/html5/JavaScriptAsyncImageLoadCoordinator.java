/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptAsyncImageLoadCoordinator {
    private JavaScriptAsyncImageLoadCoordinator() {
    }

    public static final class State {
        private boolean loaded;
        private boolean error;
        private boolean listenersInstalled;
        private boolean suppressRepaint;
        private int width;
        private int height;

        public boolean isLoaded() {
            return loaded;
        }

        public boolean isError() {
            return error;
        }

        public boolean areListenersInstalled() {
            return listenersInstalled;
        }

        public void setListenersInstalled(boolean listenersInstalled) {
            this.listenersInstalled = listenersInstalled;
        }

        public boolean isSuppressRepaint() {
            return suppressRepaint;
        }

        public void setSuppressRepaint(boolean suppressRepaint) {
            this.suppressRepaint = suppressRepaint;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public static boolean handleImmediateCompletion(State state, int naturalWidth, int naturalHeight) {
        if (naturalHeight <= 0) {
            state.loaded = false;
            state.error = true;
            return false;
        }
        state.loaded = true;
        state.error = false;
        if (state.width <= 0) {
            state.width = naturalWidth;
        }
        if (state.height <= 0) {
            state.height = naturalHeight;
        }
        return true;
    }

    public static void beginLoading(State state) {
        state.loaded = false;
        state.error = false;
    }

    public static boolean handleLoad(State state, int naturalWidth, int naturalHeight) {
        if (naturalWidth < 0 || naturalHeight < 0) {
            state.loaded = false;
            state.error = true;
            return false;
        }
        if (state.width <= 0) {
            state.width = naturalWidth;
        }
        if (state.height <= 0) {
            state.height = naturalHeight;
        }
        state.loaded = true;
        state.error = false;
        return true;
    }

    public static void handleError(State state) {
        state.error = true;
        state.loaded = false;
    }

    public static boolean shouldWait(State state) {
        return !state.loaded && !state.error;
    }

    public static boolean shouldRepaintOnLoad(State state) {
        return state.loaded && !state.error && !state.suppressRepaint;
    }
}
