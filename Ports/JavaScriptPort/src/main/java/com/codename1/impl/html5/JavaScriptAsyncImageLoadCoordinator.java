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
