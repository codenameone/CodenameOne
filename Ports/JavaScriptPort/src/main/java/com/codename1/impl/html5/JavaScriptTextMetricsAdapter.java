/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptTextMetricsAdapter {
    private JavaScriptTextMetricsAdapter() {
    }

    public interface FontMetricsContext {
        String getCurrentFont();
        void setCurrentFont(String fontCss);
        int measureWidth(String text);
    }

    public interface FontCssSupplier<F> {
        String getCss(F font);
        int getHeight(F font);
        int getAscent(F font);
    }

    public static <F> int charsWidth(FontMetricsContext context, FontCssSupplier<F> supplier, F font, char[] chars, int offset, int length) {
        return stringWidth(context, supplier, font, new String(chars, offset, length));
    }

    public static <F> int stringWidth(FontMetricsContext context, FontCssSupplier<F> supplier, F font, String text) {
        String oldFont = context.getCurrentFont();
        context.setCurrentFont(supplier.getCss(font));
        int width = context.measureWidth(text) + 1;
        context.setCurrentFont(oldFont);
        return width;
    }

    public static <F> int getFontHeight(FontCssSupplier<F> supplier, F font) {
        return supplier.getHeight(font);
    }

    public static <F> int getFontDescent(FontCssSupplier<F> supplier, F font) {
        return getFontHeight(supplier, font) - supplier.getAscent(font);
    }
}
