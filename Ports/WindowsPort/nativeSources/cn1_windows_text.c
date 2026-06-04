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
 * Font + text bridge for the Windows port. This C translation unit owns the
 * Java-runtime side (String/char[] conversion, peer boxing) and delegates the
 * actual DirectWrite work to the plain-C facade in cn1_windows_dwrite.h, whose
 * implementation is the lone C++ unit. Fonts are held as CN1Font with an opaque
 * IDWriteTextFormat* handle; render target and brush are passed through as the
 * Direct2D COM pointers the graphics layer manages.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include "cn1_windows_dwrite.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

/* CN1 Font constants (com.codename1.ui.Font). */
#define CN1_FACE_MONOSPACE 32
#define CN1_STYLE_BOLD     1
#define CN1_STYLE_ITALIC   2
#define CN1_SIZE_MEDIUM    0
#define CN1_SIZE_SMALL     8
#define CN1_SIZE_LARGE     16

/* Owned UTF-16 duplicate; avoids depending on the _wcsdup CRT name. */
static wchar_t* cn1WinWideDup(const wchar_t* src) {
    if (src == NULL) {
        return NULL;
    }
    size_t bytes = (wcslen(src) + 1) * sizeof(wchar_t);
    wchar_t* copy = (wchar_t*) malloc(bytes);
    if (copy != NULL) {
        memcpy(copy, src, bytes);
    }
    return copy;
}

/* Builds the absolute path of a file shipped next to the executable (the
 * directory bundled resources -- themes, fonts -- are staged in). Returns 1 on
 * success. */
static int cn1WinExeRelativePath(const wchar_t* fileName, wchar_t* out, int outLen) {
    if (fileName == NULL || out == NULL || outLen <= 0) {
        return 0;
    }
    wchar_t exePath[MAX_PATH];
    DWORD n = GetModuleFileNameW(NULL, exePath, MAX_PATH);
    if (n == 0 || n >= MAX_PATH) {
        return 0;
    }
    wchar_t* slash = wcsrchr(exePath, L'\\');
    if (slash == NULL) {
        return 0;
    }
    slash[1] = L'\0';
    if ((int) (wcslen(exePath) + wcslen(fileName) + 1) > outLen) {
        return 0;
    }
    wcscpy(out, exePath);
    wcscat(out, fileName);
    return 1;
}

/* Case-insensitive ".ttf" suffix test. */
static int cn1WinIsTtf(const wchar_t* name) {
    if (name == NULL) {
        return 0;
    }
    size_t len = wcslen(name);
    if (len < 4) {
        return 0;
    }
    const wchar_t* ext = name + (len - 4);
    return ext[0] == L'.'
            && (ext[1] == L't' || ext[1] == L'T')
            && (ext[2] == L't' || ext[2] == L'T')
            && (ext[3] == L'f' || ext[3] == L'F');
}

/* Builds a CN1Font for an explicit family / pixel size / style. */
static CN1Font* cn1WinMakeFont(const wchar_t* family, float px, int face, int style) {
    int bold = (style & CN1_STYLE_BOLD) != 0;
    int italic = (style & CN1_STYLE_ITALIC) != 0;
    CN1Font* font = (CN1Font*) malloc(sizeof(CN1Font));
    if (font == NULL) {
        return NULL;
    }
    font->format = cn1dwCreateFormat(family, px, bold, italic);
    font->size = px;
    font->face = face;
    font->style = style;
    font->height = cn1dwFontHeight(font->format);
    font->ascent = cn1dwFontAscent(font->format);
    font->family = cn1WinWideDup(family);
    return font;
}

CN1Font* cn1WinCreateFont(int face, int style, int size) {
    float dpi = cn1Win.dpiScale > 0.0f ? cn1Win.dpiScale : 1.0f;
    /* Size flags map to base DIPs; a value above the LARGE flag is treated as an
     * explicit pixel height (the path Font.derive uses). */
    float px;
    if (size == CN1_SIZE_SMALL) {
        px = 12.0f;
    } else if (size == CN1_SIZE_LARGE) {
        px = 20.0f;
    } else if (size > CN1_SIZE_LARGE) {
        px = (float) size;
    } else {
        px = 15.0f;
    }
    px *= dpi;

    const wchar_t* family = (face == CN1_FACE_MONOSPACE) ? L"Consolas" : L"Segoe UI";
    return cn1WinMakeFont(family, px, face, style);
}

static CN1Font* cn1WinDefaultFont(void) {
    if (cn1Win.defaultFont == NULL) {
        cn1Win.defaultFont = cn1WinCreateFont(0, 0, CN1_SIZE_MEDIUM);
    }
    return cn1Win.defaultFont;
}

/* ------------------------------------------------- WindowsNative bridge */

JAVA_LONG com_codename1_impl_windows_WindowsNative_createFont___int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    return (JAVA_LONG) (intptr_t) cn1WinCreateFont(__cn1Arg1, __cn1Arg2, __cn1Arg3);
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_getDefaultFont___R_long(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_LONG) (intptr_t) cn1WinDefaultFont();
}

JAVA_INT com_codename1_impl_windows_WindowsNative_stringWidth___long_java_lang_String_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    CN1Font* font = (CN1Font*) (intptr_t) __cn1Arg1;
    if (font == NULL) {
        font = cn1WinDefaultFont();
    }
    UINT32 len = 0;
    WCHAR* w = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &len);
    float width = cn1dwMeasure(font->format, w, (int) len);
    free(w);
    return (JAVA_INT) (width + 0.5f);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_charWidth___long_char_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_CHAR __cn1Arg2) {
    CN1Font* font = (CN1Font*) (intptr_t) __cn1Arg1;
    if (font == NULL) {
        font = cn1WinDefaultFont();
    }
    WCHAR ch[1];
    ch[0] = (WCHAR) __cn1Arg2;
    return (JAVA_INT) (cn1dwMeasure(font->format, ch, 1) + 0.5f);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_charsWidth___long_char_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Font* font = (CN1Font*) (intptr_t) __cn1Arg1;
    if (font == NULL) {
        font = cn1WinDefaultFont();
    }
    /* JAVA_ARRAY_CHAR is a 16-bit code unit, identical to WCHAR. */
    JAVA_ARRAY_CHAR* data = (JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY) __cn1Arg2).data;
    return (JAVA_INT) (cn1dwMeasure(font->format, (const WCHAR*) (data + __cn1Arg3), __cn1Arg4) + 0.5f);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_fontHeight___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Font* font = (CN1Font*) (intptr_t) __cn1Arg1;
    if (font == NULL) {
        font = cn1WinDefaultFont();
    }
    return (JAVA_INT) ceilf(font->height);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawString___long_java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) __cn1Arg1;
    if (g == NULL) {
        return;
    }
    cn1WinBeginFrame(g);
    CN1Font* font = g->font ? g->font : cn1WinDefaultFont();
    UINT32 len = 0;
    WCHAR* w = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &len);
    cn1dwDrawText(g->target, font ? font->format : NULL, cn1WinBrush(g), w, (int) len,
            (float) __cn1Arg3, (float) __cn1Arg4);
    free(w);
}

/*
 * loadTrueTypeFont maps a Codename One font to a DirectWrite text format.
 *
 * A bundled TrueType *file* (fileName ends in .ttf -- material-design-font.ttf,
 * which backs every FontImage glyph, or an app-supplied font) is loaded into a
 * private DirectWrite font collection via cn1dwRegisterFontFile, because the
 * family isn't installed system-wide and would otherwise silently fall back to
 * Segoe UI (rendering no icon glyphs). The register call returns the font's real
 * family name, which cn1WinMakeFont -> cn1dwCreateFormat binds to that private
 * collection.
 *
 * Otherwise the name selects a system family: the "native:" scheme names
 * (native:MainLight, native:ItalicBold, ...) resolve to the platform UI family
 * (Segoe UI) with weight/slant from the suffix; any other name is a literal
 * family.
 */
JAVA_LONG com_codename1_impl_windows_WindowsNative_loadTrueTypeFont___java_lang_String_java_lang_String_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    WCHAR* name = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, NULL);
    WCHAR* fileName = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, NULL);
    float dpi = cn1Win.dpiScale > 0.0f ? cn1Win.dpiScale : 1.0f;
    CN1Font* font = NULL;

    if (cn1WinIsTtf(fileName)) {
        wchar_t fullPath[MAX_PATH];
        wchar_t registered[128];
        registered[0] = L'\0';
        if (cn1WinExeRelativePath(fileName, fullPath, MAX_PATH)
                && cn1dwRegisterFontFile(fullPath, registered, 128) && registered[0] != L'\0') {
            font = cn1WinMakeFont(registered, 15.0f * dpi, 0, 0);
        }
    }

    if (font == NULL) {
        const wchar_t* family = L"Segoe UI";
        int style = 0;
        if (name != NULL && wcsncmp(name, L"native:", 7) == 0) {
            const wchar_t* suffix = name + 7;
            if (wcsstr(suffix, L"Bold") != NULL || wcsstr(suffix, L"Black") != NULL) {
                style |= CN1_STYLE_BOLD;
            }
            if (wcsstr(suffix, L"Italic") != NULL) {
                style |= CN1_STYLE_ITALIC;
            }
        } else if (name != NULL && name[0] != L'\0') {
            family = name;
        }
        font = cn1WinMakeFont(family, 15.0f * dpi, 0, style);
    }
    free(name);
    free(fileName);
    return (JAVA_LONG) (intptr_t) font;
}

/*
 * deriveTrueTypeFont produces a new peer at the requested pixel size and weight,
 * reusing the base font's family. weight carries the CN1 STYLE_* bits.
 */
JAVA_LONG com_codename1_impl_windows_WindowsNative_deriveTrueTypeFont___long_float_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_FLOAT __cn1Arg2, JAVA_INT __cn1Arg3) {
    CN1Font* base = (CN1Font*) (intptr_t) __cn1Arg1;
    const wchar_t* family = (base != NULL && base->family != NULL) ? base->family : L"Segoe UI";
    float px = __cn1Arg2 > 0.0f ? __cn1Arg2 : 15.0f;
    CN1Font* font = cn1WinMakeFont(family, px, 0, __cn1Arg3);
    return (JAVA_LONG) (intptr_t) font;
}

#endif /* _WIN32 */
