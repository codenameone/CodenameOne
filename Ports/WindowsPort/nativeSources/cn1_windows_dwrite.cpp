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
#include <dwrite_3.h>
#include <wchar.h>

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

/*
 * Bundled-font registry. Codename One ships icon/text fonts (notably
 * material-design-font.ttf, the source of every FontImage glyph -- checkbox
 * check, switch thumb, FAB "+", arrows, toast icon) as files next to the exe.
 * Those families are not installed system-wide, so CreateTextFormat against the
 * system collection finds nothing and silently substitutes Segoe UI (no glyphs).
 * We load each such file into a private IDWriteFontCollection1 and remember it by
 * its real family name; cn1dwCreateFormat then binds matching formats to the
 * private collection. The set is tiny (material + maybe an app font), so a fixed
 * array keyed by family name is enough.
 */
struct CN1CustomFont {
    wchar_t family[128];
    IDWriteFontCollection1* collection;
};
static CN1CustomFont g_customFonts[16];
static int g_customFontCount = 0;

static IDWriteFontCollection1* cn1dwFindCustom(const wchar_t* family) {
    if (family == nullptr) {
        return nullptr;
    }
    for (int i = 0; i < g_customFontCount; i++) {
        if (wcscmp(g_customFonts[i].family, family) == 0) {
            return g_customFonts[i].collection;
        }
    }
    return nullptr;
}

extern "C" int cn1dwRegisterFontFile(const wchar_t* path, wchar_t* outFamily, int outLen) {
    IDWriteFactory* f = dwFactory();
    if (f == nullptr || path == nullptr) {
        return 0;
    }
    IDWriteFactory3* f3 = nullptr;
    if (FAILED(f->QueryInterface(__uuidof(IDWriteFactory3), reinterpret_cast<void**>(&f3))) || f3 == nullptr) {
        cn1WindowsLog("cn1dwRegisterFontFile: IDWriteFactory3 unavailable");
        return 0;
    }
    int result = 0;
    IDWriteFontFile* file = nullptr;
    IDWriteFontSetBuilder* builder = nullptr;
    IDWriteFontSetBuilder1* builder1 = nullptr;
    IDWriteFontSet* set = nullptr;
    IDWriteFontCollection1* coll = nullptr;
    IDWriteFontFamily* fam = nullptr;
    IDWriteLocalizedStrings* names = nullptr;
    BOOL supported = FALSE;
    DWRITE_FONT_FILE_TYPE fileType;
    DWRITE_FONT_FACE_TYPE faceType;
    UINT32 numFaces = 0;
    /* AddFontFile(IDWriteFontFile*) is on IDWriteFontSetBuilder1, not the base
     * IDWriteFontSetBuilder (which only takes a font-face reference), so query
     * for it. CreateFontSet stays on the base interface (same object). */
    if (SUCCEEDED(f3->CreateFontFileReference(path, nullptr, &file)) && file != nullptr &&
            SUCCEEDED(file->Analyze(&supported, &fileType, &faceType, &numFaces)) && supported &&
            SUCCEEDED(f3->CreateFontSetBuilder(&builder)) && builder != nullptr &&
            SUCCEEDED(builder->QueryInterface(__uuidof(IDWriteFontSetBuilder1),
                    reinterpret_cast<void**>(&builder1))) && builder1 != nullptr &&
            SUCCEEDED(builder1->AddFontFile(file)) &&
            SUCCEEDED(builder->CreateFontSet(&set)) && set != nullptr &&
            SUCCEEDED(f3->CreateFontCollectionFromFontSet(set, &coll)) && coll != nullptr &&
            coll->GetFontFamilyCount() > 0 &&
            SUCCEEDED(coll->GetFontFamily(0, &fam)) && fam != nullptr &&
            SUCCEEDED(fam->GetFamilyNames(&names)) && names != nullptr) {
        UINT32 idx = 0;
        BOOL exists = FALSE;
        names->FindLocaleName(L"en-us", &idx, &exists);
        if (!exists) {
            idx = 0;
        }
        wchar_t famName[128];
        famName[0] = 0;
        if (SUCCEEDED(names->GetString(idx, famName, 128))) {
            if (cn1dwFindCustom(famName) == nullptr && g_customFontCount < 16) {
                wcsncpy(g_customFonts[g_customFontCount].family, famName, 127);
                g_customFonts[g_customFontCount].family[127] = 0;
                coll->AddRef();
                g_customFonts[g_customFontCount].collection = coll;
                g_customFontCount++;
            }
            if (outFamily != nullptr && outLen > 0) {
                wcsncpy(outFamily, famName, (size_t) (outLen - 1));
                outFamily[outLen - 1] = 0;
            }
            result = 1;
        }
    }
    if (names) { names->Release(); }
    if (fam) { fam->Release(); }
    if (coll) { coll->Release(); }
    if (set) { set->Release(); }
    if (builder1) { builder1->Release(); }
    if (builder) { builder->Release(); }
    if (file) { file->Release(); }
    f3->Release();
    if (result == 0) {
        cn1WindowsLog("cn1dwRegisterFontFile: failed to load bundled font file");
    }
    return result;
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
    /* Bundled fonts (material-design-font.ttf, app TTFs) live in a private
     * collection keyed by family name; system families pass nullptr. */
    IDWriteFontCollection1* custom = cn1dwFindCustom(family);
    IDWriteTextFormat* fmt = nullptr;
    HRESULT hr = f->CreateTextFormat(family ? family : L"Segoe UI", custom, weight, style,
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
