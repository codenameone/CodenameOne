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
 * DirectWrite text layer for the Windows port. This is the one translation unit
 * compiled as C++ (DirectWrite has no C binding), exposing the plain-C facade
 * declared in cn1_windows_dwrite.h. It deliberately does NOT include
 * cn1_windows.h or cn1_globals.h: it speaks only opaque pointers and wide
 * strings, so the C bridge (cn1_windows_text.c) owns all of the Java-runtime
 * interaction. Direct2D objects (render target, brush) arrive as void* and are
 * the same COM pointers the C layer created.
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <d2d1.h>
#include <dwrite.h>

#include "cn1_windows_dwrite.h"

extern "C" void cn1WindowsLog(const char* message);

/* One shared DirectWrite factory for the process. */
static IDWriteFactory* g_dwrite = nullptr;

static IDWriteFactory* dwFactory() {
    if (g_dwrite == nullptr) {
        DWriteCreateFactory(DWRITE_FACTORY_TYPE_SHARED, __uuidof(IDWriteFactory),
                reinterpret_cast<IUnknown**>(&g_dwrite));
    }
    return g_dwrite;
}

/* A very large layout box so metrics reflect the unconstrained run. */
static const float CN1_DW_HUGE = 1.0e6f;

extern "C" void* cn1dwCreateFormat(const wchar_t* family, float sizePx, int bold, int italic) {
    IDWriteFactory* f = dwFactory();
    if (f == nullptr || sizePx <= 0.0f) {
        return nullptr;
    }
    DWRITE_FONT_WEIGHT weight = bold ? DWRITE_FONT_WEIGHT_BOLD : DWRITE_FONT_WEIGHT_NORMAL;
    DWRITE_FONT_STYLE style = italic ? DWRITE_FONT_STYLE_ITALIC : DWRITE_FONT_STYLE_NORMAL;
    IDWriteTextFormat* fmt = nullptr;
    HRESULT hr = f->CreateTextFormat(family ? family : L"Segoe UI", nullptr, weight, style,
            DWRITE_FONT_STRETCH_NORMAL, sizePx, L"", &fmt);
    if (FAILED(hr)) {
        cn1WindowsLog("cn1dwCreateFormat: CreateTextFormat failed");
        return nullptr;
    }
    return fmt;
}

/* Lays out "Ag" once to read the line metrics for the format. */
static void cn1dwMetrics(IDWriteTextFormat* fmt, float* height, float* ascent) {
    *height = 0.0f;
    *ascent = 0.0f;
    IDWriteFactory* f = dwFactory();
    if (f == nullptr || fmt == nullptr) {
        return;
    }
    IDWriteTextLayout* layout = nullptr;
    if (FAILED(f->CreateTextLayout(L"Ag", 2, fmt, CN1_DW_HUGE, CN1_DW_HUGE, &layout))) {
        return;
    }
    DWRITE_TEXT_METRICS tm;
    if (SUCCEEDED(layout->GetMetrics(&tm))) {
        *height = tm.height;
    }
    DWRITE_LINE_METRICS lm;
    UINT32 lineCount = 0;
    if (SUCCEEDED(layout->GetLineMetrics(&lm, 1, &lineCount)) && lineCount > 0) {
        *ascent = lm.baseline;
    }
    layout->Release();
}

extern "C" float cn1dwFontHeight(void* format) {
    float h, a;
    cn1dwMetrics(reinterpret_cast<IDWriteTextFormat*>(format), &h, &a);
    return h;
}

extern "C" float cn1dwFontAscent(void* format) {
    float h, a;
    cn1dwMetrics(reinterpret_cast<IDWriteTextFormat*>(format), &h, &a);
    return a;
}

extern "C" float cn1dwMeasure(void* format, const wchar_t* text, int len) {
    IDWriteFactory* f = dwFactory();
    IDWriteTextFormat* fmt = reinterpret_cast<IDWriteTextFormat*>(format);
    if (f == nullptr || fmt == nullptr || text == nullptr || len <= 0) {
        return 0.0f;
    }
    IDWriteTextLayout* layout = nullptr;
    if (FAILED(f->CreateTextLayout(text, (UINT32) len, fmt, CN1_DW_HUGE, CN1_DW_HUGE, &layout))) {
        return 0.0f;
    }
    float width = 0.0f;
    DWRITE_TEXT_METRICS tm;
    if (SUCCEEDED(layout->GetMetrics(&tm))) {
        width = tm.widthIncludingTrailingWhitespace;
    }
    layout->Release();
    return width;
}

extern "C" void cn1dwDrawText(void* renderTarget, void* format, void* brush,
        const wchar_t* text, int len, float x, float y) {
    IDWriteFactory* f = dwFactory();
    ID2D1RenderTarget* target = reinterpret_cast<ID2D1RenderTarget*>(renderTarget);
    IDWriteTextFormat* fmt = reinterpret_cast<IDWriteTextFormat*>(format);
    ID2D1Brush* b = reinterpret_cast<ID2D1Brush*>(brush);
    if (f == nullptr || target == nullptr || fmt == nullptr || b == nullptr || text == nullptr || len <= 0) {
        return;
    }
    IDWriteTextLayout* layout = nullptr;
    if (FAILED(f->CreateTextLayout(text, (UINT32) len, fmt, CN1_DW_HUGE, CN1_DW_HUGE, &layout))) {
        return;
    }
    D2D1_POINT_2F origin;
    origin.x = x;
    origin.y = y;
    target->DrawTextLayout(origin, layout, b, D2D1_DRAW_TEXT_OPTIONS_NONE);
    layout->Release();
}

#endif /* _WIN32 */
