/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
