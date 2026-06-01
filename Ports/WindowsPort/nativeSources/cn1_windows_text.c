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

/*
 * Text and font support for the native Codename One Windows port. Fonts are
 * IDWriteTextFormat handles wrapped in a CN1Font; measurement and drawing go
 * through transient IDWriteTextLayout objects so we can read exact metrics and
 * render with Direct2D. COM is used through its C binding (COBJMACROS), hence
 * the IDWriteFactory_Xxx / ID2D1RenderTarget_Xxx call macros.
 */

#ifdef _WIN32

#include "cn1_windows.h"

#include <math.h>
#include <stdlib.h>
#include <string.h>

/* ------------------------------------------------------------------ helpers */

/*
 * Translate the Codename One Font face/style/size triple into a concrete
 * DirectWrite text format and cache the resulting line metrics.
 *
 * CN1 size handling: com.codename1.ui.Font exposes SIZE_MEDIUM=0, SIZE_SMALL=8
 * and SIZE_LARGE=16 as *bit flags* packed into the size byte. The native port,
 * however, also lets callers pass an explicit pixel size. We disambiguate by
 * magnitude: anything larger than the largest flag value (16) is treated as a
 * literal pixel height; otherwise we map the SIZE_* flag to a sensible default
 * DIP height and scale it by the current DPI. This keeps both the system fonts
 * (flag based) and explicitly-sized fonts working without an extra parameter.
 *
 * CN1 style bits: STYLE_BOLD=1, STYLE_ITALIC=2 (combinable).
 * CN1 face: FACE_SYSTEM=0, FACE_MONOSPACE=32, FACE_PROPORTIONAL=64.
 */
CN1Font* cn1WinCreateFont(int face, int style, int size) {
    CN1Font* font;
    float dip;
    const WCHAR* family;
    DWRITE_FONT_WEIGHT weight;
    DWRITE_FONT_STYLE dwriteStyle;
    IDWriteTextLayout* layout;
    HRESULT hr;

    if (cn1Win.dwriteFactory == NULL) {
        return NULL;
    }

    font = (CN1Font*)calloc(1, sizeof(CN1Font));
    if (font == NULL) {
        return NULL;
    }

    /*
     * Resolve the DIP height. The default (medium) base is ~15px; small ~12px,
     * large ~20px. A value greater than SIZE_LARGE (16) is interpreted as an
     * explicit pixel size and used verbatim. The flag-derived sizes are scaled
     * by the DPI factor; explicit pixel sizes are assumed to already be in the
     * caller's coordinate space and left unscaled.
     */
    if (size > 16) {
        dip = (float)size;                 /* explicit pixel height          */
    } else {
        float base;
        if (size == 8) {                   /* SIZE_SMALL                     */
            base = 12.0f;
        } else if (size == 16) {           /* SIZE_LARGE                     */
            base = 20.0f;
        } else {                           /* SIZE_MEDIUM (0) or unknown     */
            base = 15.0f;
        }
        dip = base * (cn1Win.dpiScale > 0.0f ? cn1Win.dpiScale : 1.0f);
    }
    if (dip < 1.0f) {
        dip = 15.0f;
    }

    /* Face -> font family. Monospace gets Consolas, everything else Segoe UI. */
    if (face == 32) {                      /* FACE_MONOSPACE                 */
        family = L"Consolas";
    } else {
        family = L"Segoe UI";              /* FACE_SYSTEM / FACE_PROPORTIONAL */
    }

    weight = (style & 1) ? DWRITE_FONT_WEIGHT_BOLD : DWRITE_FONT_WEIGHT_NORMAL;
    dwriteStyle = (style & 2) ? DWRITE_FONT_STYLE_ITALIC : DWRITE_FONT_STYLE_NORMAL;

    font->size = dip;
    font->face = face;
    font->style = style;
    font->format = NULL;
    font->ascent = 0.0f;
    font->height = dip; /* sensible fallback if metrics cannot be measured */

    hr = IDWriteFactory_CreateTextFormat(
            cn1Win.dwriteFactory,
            family,
            NULL,
            weight,
            dwriteStyle,
            DWRITE_FONT_STRETCH_NORMAL,
            dip,
            L"",
            &font->format);
    if (FAILED(hr) || font->format == NULL) {
        free(font);
        return NULL;
    }

    /*
     * Measure a representative two-glyph string ("Ag", ascender + descender) to
     * cache the line height and baseline. The layout box is unconstrained.
     */
    layout = NULL;
    hr = IDWriteFactory_CreateTextLayout(
            cn1Win.dwriteFactory,
            L"Ag",
            2,
            font->format,
            100000.0f,
            100000.0f,
            &layout);
    if (SUCCEEDED(hr) && layout != NULL) {
        DWRITE_TEXT_METRICS metrics;
        DWRITE_LINE_METRICS line;
        UINT32 lineCount = 0;
        memset(&metrics, 0, sizeof(metrics));
        if (SUCCEEDED(IDWriteTextLayout_GetMetrics(layout, &metrics))) {
            font->height = metrics.height;
        }
        /* The first line's baseline gives us the ascent. */
        memset(&line, 0, sizeof(line));
        if (SUCCEEDED(IDWriteTextLayout_GetLineMetrics(layout, &line, 1, &lineCount))
                && lineCount > 0) {
            font->ascent = line.baseline;
            if (line.height > 0.0f) {
                font->height = line.height;
            }
        }
        IDWriteTextLayout_Release(layout);
    }

    return font;
}

/*
 * Measure the advance width of a UTF-16 run in DIPs. Returns the width
 * including trailing whitespace so layout is consistent with CN1 expectations.
 */
float cn1WinMeasureWidth(CN1Font* f, const WCHAR* text, UINT32 len) {
    IDWriteTextFormat* format;
    IDWriteTextLayout* layout;
    DWRITE_TEXT_METRICS metrics;
    float width;
    HRESULT hr;

    if (cn1Win.dwriteFactory == NULL || text == NULL || len == 0) {
        return 0.0f;
    }

    format = (f != NULL) ? f->format : NULL;
    if (format == NULL && cn1Win.defaultFont != NULL) {
        format = cn1Win.defaultFont->format;
    }
    if (format == NULL) {
        return 0.0f;
    }

    layout = NULL;
    hr = IDWriteFactory_CreateTextLayout(
            cn1Win.dwriteFactory,
            text,
            len,
            format,
            1000000.0f,
            1000000.0f,
            &layout);
    if (FAILED(hr) || layout == NULL) {
        return 0.0f;
    }

    memset(&metrics, 0, sizeof(metrics));
    width = 0.0f;
    if (SUCCEEDED(IDWriteTextLayout_GetMetrics(layout, &metrics))) {
        width = metrics.widthIncludingTrailingWhitespace;
    }
    IDWriteTextLayout_Release(layout);
    return width;
}

/* Resolve the font a graphics context should draw with, never NULL on success. */
static CN1Font* cn1WinGraphicsFont(CN1Graphics* g) {
    if (g != NULL && g->font != NULL) {
        return g->font;
    }
    if (cn1Win.defaultFont == NULL) {
        /* Lazily materialise the default system font. */
        cn1Win.defaultFont = cn1WinCreateFont(0, 0, 0);
    }
    return cn1Win.defaultFont;
}

/* ------------------------------------------------------------- font bridges */

JAVA_LONG com_codename1_impl_windows_WindowsNative_createFont___int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    CN1Font* font = cn1WinCreateFont((int)__cn1Arg1, (int)__cn1Arg2, (int)__cn1Arg3);
    return (JAVA_LONG)(intptr_t)font;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_getDefaultFont___R_long(CODENAME_ONE_THREAD_STATE) {
    if (cn1Win.defaultFont == NULL) {
        /* Default face SYSTEM, plain style, medium size. */
        cn1Win.defaultFont = cn1WinCreateFont(0, 0, 0);
    }
    return (JAVA_LONG)(intptr_t)cn1Win.defaultFont;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_stringWidth___long_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    CN1Font* font = (CN1Font*)(intptr_t)__cn1Arg1;
    UINT32 len = 0;
    WCHAR* text;
    float width;

    if (__cn1Arg2 == JAVA_NULL) {
        return 0;
    }
    text = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &len);
    if (text == NULL) {
        return 0;
    }
    width = cn1WinMeasureWidth(font, text, len);
    free(text);
    return (JAVA_INT)(width + 0.5f);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_charWidth___long_char_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_CHAR __cn1Arg2) {
    CN1Font* font = (CN1Font*)(intptr_t)__cn1Arg1;
    WCHAR one[1];
    float width;

    one[0] = (WCHAR)__cn1Arg2;
    width = cn1WinMeasureWidth(font, one, 1);
    return (JAVA_INT)(width + 0.5f);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_charsWidth___long_char_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Font* font = (CN1Font*)(intptr_t)__cn1Arg1;
    JAVA_ARRAY_CHAR* d;
    int offset = (int)__cn1Arg3;
    int length = (int)__cn1Arg4;
    float width;

    if (__cn1Arg2 == JAVA_NULL || length <= 0) {
        return 0;
    }
    d = (JAVA_ARRAY_CHAR*)(*(JAVA_ARRAY)__cn1Arg2).data;
    /* JAVA_ARRAY_CHAR is unsigned short, layout-compatible with WCHAR. */
    width = cn1WinMeasureWidth(font, (const WCHAR*)(d + offset), (UINT32)length);
    return (JAVA_INT)(width + 0.5f);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_fontHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Font* font = (CN1Font*)(intptr_t)__cn1Arg1;
    if (font == NULL) {
        font = cn1WinGraphicsFont(NULL);
        if (font == NULL) {
            return 0;
        }
    }
    return (JAVA_INT)ceil(font->height);
}

/* ----------------------------------------------------------------- drawing */

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawString___long_java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Graphics* g = (CN1Graphics*)(intptr_t)__cn1Arg1;
    CN1Font* font;
    UINT32 len = 0;
    WCHAR* text;
    IDWriteTextLayout* layout;
    ID2D1SolidColorBrush* brush;
    D2D1_POINT_2F origin;
    HRESULT hr;

    if (g == NULL || g->target == NULL || __cn1Arg2 == JAVA_NULL) {
        return;
    }

    font = cn1WinGraphicsFont(g);
    if (font == NULL || font->format == NULL) {
        return;
    }

    text = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &len);
    if (text == NULL) {
        return;
    }
    if (len == 0) {
        free(text);
        return;
    }

    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    if (brush == NULL) {
        free(text);
        return;
    }

    /*
     * Build a transient layout and draw it. CN1 drawString positions the TOP of
     * the text at (x, y); DirectWrite DrawTextLayout also anchors at the layout
     * box top-left, so (x, y) maps directly with no baseline adjustment.
     */
    layout = NULL;
    hr = IDWriteFactory_CreateTextLayout(
            cn1Win.dwriteFactory,
            text,
            len,
            font->format,
            1000000.0f,
            1000000.0f,
            &layout);
    if (SUCCEEDED(hr) && layout != NULL) {
        origin.x = (FLOAT)__cn1Arg3;
        origin.y = (FLOAT)__cn1Arg4;
        ID2D1RenderTarget_DrawTextLayout(
                g->target,
                origin,
                (IDWriteTextLayout*)layout,
                (ID2D1Brush*)brush,
                D2D1_DRAW_TEXT_OPTIONS_NONE);
        IDWriteTextLayout_Release(layout);
    }

    free(text);
}

#endif /* _WIN32 */
