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
