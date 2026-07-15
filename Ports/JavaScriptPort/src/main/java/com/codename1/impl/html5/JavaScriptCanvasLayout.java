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

public final class JavaScriptCanvasLayout {
    private JavaScriptCanvasLayout() {
    }

    public static final class Dimensions {
        private final int cssWidth;
        private final int cssHeight;
        private final int backingWidth;
        private final int backingHeight;
        private final String styleWidth;
        private final String styleHeight;

        public Dimensions(int cssWidth, int cssHeight, int backingWidth, int backingHeight, String styleWidth, String styleHeight) {
            this.cssWidth = cssWidth;
            this.cssHeight = cssHeight;
            this.backingWidth = backingWidth;
            this.backingHeight = backingHeight;
            this.styleWidth = styleWidth;
            this.styleHeight = styleHeight;
        }

        public int getCssWidth() {
            return cssWidth;
        }

        public int getCssHeight() {
            return cssHeight;
        }

        public int getBackingWidth() {
            return backingWidth;
        }

        public int getBackingHeight() {
            return backingHeight;
        }

        public String getStyleWidth() {
            return styleWidth;
        }

        public String getStyleHeight() {
            return styleHeight;
        }
    }

    public static Dimensions compute(int viewportWidth, int viewportHeight, double devicePixelRatio) {
        int cssWidth = viewportWidth;
        int cssHeight = viewportHeight;
        int backingWidth = cssWidth;
        int backingHeight = cssHeight;
        String styleWidth = null;
        String styleHeight = null;
        int hidpiWidth = (int) (cssWidth * devicePixelRatio);
        int hidpiHeight = (int) (cssHeight * devicePixelRatio);
        if (cssWidth != hidpiWidth) {
            backingWidth = hidpiWidth;
            backingHeight = hidpiHeight;
            styleWidth = cssWidth + "px";
            styleHeight = cssHeight + "px";
        }
        return new Dimensions(cssWidth, cssHeight, backingWidth, backingHeight, styleWidth, styleHeight);
    }
}
