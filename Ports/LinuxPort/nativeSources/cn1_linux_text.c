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
 * Font creation, text measurement and drawing for the native Codename One Linux
 * port, backed by Pango (layout/measure) over Cairo (raster) with FontConfig/
 * FreeType supplying the families. A CN1Font wraps a PangoFontDescription plus a
 * cached metrics snapshot. Measurement uses a PangoLayout on the shared context;
 * drawString renders through pango_cairo onto the graphics' Cairo context.
 *
 * NOTE: implemented against Pango/PangoCairo but not yet compiled/run on a
 * Linux/GTK host. In-memory TrueType registration (loadTrueTypeFontFromMemory)
 * currently falls back to a system family -- wiring the bytes through
 * FcConfigAppFontAddMemFace/FreeType is a later refinement.
 */

#define _GNU_SOURCE
#include "cn1_linux_gfx.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fontconfig/fontconfig.h>
#include <ft2build.h>
#include FT_FREETYPE_H

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* CN1 font constants (com.codename1.ui.Font). */
#define CN1_FACE_SYSTEM      0
#define CN1_FACE_MONOSPACE   32
#define CN1_FACE_PROPORTIONAL 64
#define CN1_STYLE_BOLD       1
#define CN1_STYLE_ITALIC     2
#define CN1_SIZE_MEDIUM      0
#define CN1_SIZE_SMALL       8
#define CN1_SIZE_LARGE       16

static PangoContext* cn1Pango = 0;

PangoContext* cn1LinuxPangoContext(void) {
    if (cn1Pango == 0) {
        PangoFontMap* fm = pango_cairo_font_map_get_default();
        cn1Pango = pango_font_map_create_context(fm);
    }
    return cn1Pango;
}

/* Snapshots ascent/height for the description into the CN1Font. */
static void cn1FontMetrics(CN1Font* f) {
    PangoFontMetrics* m = pango_context_get_metrics(cn1LinuxPangoContext(), f->desc, 0);
    if (m != 0) {
        f->ascent = pango_font_metrics_get_ascent(m) / PANGO_SCALE;
        f->height = (pango_font_metrics_get_ascent(m) + pango_font_metrics_get_descent(m)) / PANGO_SCALE;
        pango_font_metrics_unref(m);
    } else {
        f->ascent = f->pixelSize;
        f->height = f->pixelSize + f->pixelSize / 4;
    }
}

static CN1Font* cn1MakeFont(const char* family, int weight, int italic, int pixelSize) {
    CN1Font* f = (CN1Font*) calloc(1, sizeof(CN1Font));
    f->desc = pango_font_description_new();
    pango_font_description_set_family(f->desc, family);
    pango_font_description_set_weight(f->desc, weight);
    pango_font_description_set_style(f->desc, italic ? PANGO_STYLE_ITALIC : PANGO_STYLE_NORMAL);
    pango_font_description_set_absolute_size(f->desc, pixelSize * PANGO_SCALE);
    f->pixelSize = pixelSize;
    cn1FontMetrics(f);
    return f;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_createFont___int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT face, JAVA_INT style, JAVA_INT size) {
    const char* family = "Sans";
    int pixelSize = 18;
    int weight = PANGO_WEIGHT_NORMAL;
    int italic = 0;
    if ((face & CN1_FACE_MONOSPACE) != 0) {
        family = "Monospace";
    } else if ((face & CN1_FACE_PROPORTIONAL) != 0) {
        family = "Serif";
    }
    if ((size & CN1_SIZE_SMALL) != 0) {
        pixelSize = 14;
    } else if ((size & CN1_SIZE_LARGE) != 0) {
        pixelSize = 24;
    }
    if ((style & CN1_STYLE_BOLD) != 0) {
        weight = PANGO_WEIGHT_BOLD;
    }
    if ((style & CN1_STYLE_ITALIC) != 0) {
        italic = 1;
    }
    return (JAVA_LONG) (intptr_t) cn1MakeFont(family, weight, italic, pixelSize);
}

static CN1Font* cn1DefaultFont = 0;

JAVA_LONG com_codename1_impl_linux_LinuxNative_getDefaultFont___R_long(CODENAME_ONE_THREAD_STATE) {
    if (cn1DefaultFont == 0) {
        cn1DefaultFont = cn1MakeFont("Sans", PANGO_WEIGHT_NORMAL, 0, 18);
    }
    return (JAVA_LONG) (intptr_t) cn1DefaultFont;
}

static CN1Font* cn1FontOrDefault(JAVA_LONG font) {
    CN1Font* f = (CN1Font*) (intptr_t) font;
    if (f == 0) {
        if (cn1DefaultFont == 0) {
            cn1DefaultFont = cn1MakeFont("Sans", PANGO_WEIGHT_NORMAL, 0, 18);
        }
        f = cn1DefaultFont;
    }
    return f;
}

/* Pango rejects invalid UTF-8 outright -- it logs a warning and leaves the layout
 * empty, so pango_layout_get_pixel_size returns 0. A 0 width then wedges any
 * width-driven reflow (SpanLabel/TextArea word wrap) into a loop that can never
 * fit the next word and never terminates (observed as a hard hang flooding
 * "Invalid UTF-8 string passed to pango_layout_set_text"). stringToUTF8 hands back
 * a pointer into one shared per-thread scratch buffer that an intervening string
 * conversion -- including an exception string built by the fault->NPE handler --
 * can clobber mid-call, so validate defensively and substitute a sanitized copy
 * rather than trust the bytes. */
static void cn1LayoutSetText(PangoLayout* layout, const char* s, int len) {
    if (s == NULL) {
        pango_layout_set_text(layout, "", 0);
        return;
    }
    if (g_utf8_validate(s, len, NULL)) {
        pango_layout_set_text(layout, s, len);
        return;
    }
    char* valid = g_utf8_make_valid(s, len);
    pango_layout_set_text(layout, valid, -1);
    g_free(valid);
}

static int cn1MeasureUtf8(CN1Font* f, const char* utf8, int byteLen) {
    PangoLayout* layout = pango_layout_new(cn1LinuxPangoContext());
    int w = 0, h = 0;
    pango_layout_set_font_description(layout, f->desc);
    cn1LayoutSetText(layout, utf8, byteLen);
    pango_layout_get_pixel_size(layout, &w, &h);
    g_object_unref(layout);
    return w;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_stringWidth___long_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_OBJECT str) {
    const char* s;
    if (str == JAVA_NULL) {
        return 0;
    }
    s = stringToUTF8(threadStateData, str);
    return cn1MeasureUtf8(cn1FontOrDefault(font), s, -1);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_charWidth___long_char_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_CHAR c) {
    char buf[8];
    int n = 0;
    /* UTF-8 encode the single UTF-16 code unit (BMP only; good enough for width). */
    unsigned int cp = (unsigned int) c;
    if (cp < 0x80) {
        buf[n++] = (char) cp;
    } else if (cp < 0x800) {
        buf[n++] = (char) (0xC0 | (cp >> 6));
        buf[n++] = (char) (0x80 | (cp & 0x3F));
    } else {
        buf[n++] = (char) (0xE0 | (cp >> 12));
        buf[n++] = (char) (0x80 | ((cp >> 6) & 0x3F));
        buf[n++] = (char) (0x80 | (cp & 0x3F));
    }
    return cn1MeasureUtf8(cn1FontOrDefault(font), buf, n);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_charsWidth___long_char_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_OBJECT chars, JAVA_INT offset, JAVA_INT length) {
    /* A Java char[] stores 2-byte JAVA_ARRAY_CHAR elements, NOT 4-byte JAVA_CHAR
     * (which is `int`). Reading the array through a JAVA_CHAR* strides 4 bytes per
     * element and over-reads ~2x past the end -- an out-of-bounds heap read that
     * silently corrupts adjacent memory (it surfaced as a TextArea word-wrap
     * over-read during Form.show that wedged later tests). Use JAVA_ARRAY_CHAR*. */
    JAVA_ARRAY_CHAR* data;
    char* utf8;
    int n = 0;
    int i;
    int width;
    if (chars == JAVA_NULL || length <= 0) {
        return 0;
    }
    data = (JAVA_ARRAY_CHAR*) (*(JAVA_ARRAY) chars).data;
    utf8 = (char*) malloc((size_t) length * 4 + 1);
    for (i = 0; i < length; i++) {
        unsigned int cp = (unsigned int) data[offset + i];
        if (cp < 0x80) {
            utf8[n++] = (char) cp;
        } else if (cp < 0x800) {
            utf8[n++] = (char) (0xC0 | (cp >> 6));
            utf8[n++] = (char) (0x80 | (cp & 0x3F));
        } else {
            utf8[n++] = (char) (0xE0 | (cp >> 12));
            utf8[n++] = (char) (0x80 | ((cp >> 6) & 0x3F));
            utf8[n++] = (char) (0x80 | (cp & 0x3F));
        }
    }
    utf8[n] = 0;
    width = cn1MeasureUtf8(cn1FontOrDefault(font), utf8, n);
    free(utf8);
    return width;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_fontHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font) {
    return cn1FontOrDefault(font)->height;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_loadTrueTypeFont___java_lang_String_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT fontName, JAVA_OBJECT fileName) {
    const char* name = fontName == JAVA_NULL ? "Sans" : stringToUTF8(threadStateData, fontName);
    const char* family = name;
    (void) fileName;
    /* "native:" scheme maps to the system UI family. */
    if (strncmp(name, "native:", 7) == 0) {
        family = "Sans";
    }
    return (JAVA_LONG) (intptr_t) cn1MakeFont(family, PANGO_WEIGHT_NORMAL, 0, 18);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_loadTrueTypeFontFromMemory___java_lang_String_byte_1ARRAY_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT fontName, JAVA_OBJECT data) {
    /* Register the embedded TrueType bytes with FontConfig so Pango can resolve
     * the bundled family by name (this is how the material icon font + any app
     * @font renders). FontConfig adds from a file, so the bytes are spooled to a
     * temp file; the family name is read from the face via FreeType. */
    const char* name = fontName == JAVA_NULL ? "Sans" : stringToUTF8(threadStateData, fontName);
    unsigned char* bytes;
    int len;
    char tmpl[] = "/tmp/cn1fontXXXXXX";
    int fd;
    char family[256];
    FT_Library lib = 0;
    FT_Face face = 0;
    family[0] = 0;
    if (data == JAVA_NULL || (len = (int) (*(JAVA_ARRAY) data).length) <= 0) {
        if (strncmp(name, "native:", 7) == 0) {
            name = "Sans";
        }
        return (JAVA_LONG) (intptr_t) cn1MakeFont(name, PANGO_WEIGHT_NORMAL, 0, 18);
    }
    bytes = (unsigned char*) (*(JAVA_ARRAY) data).data;

    fd = mkstemp(tmpl);
    if (fd >= 0) {
        if (write(fd, bytes, (size_t) len) == len) {
            close(fd);
            FcConfigAppFontAddFile(FcConfigGetCurrent(), (const FcChar8*) tmpl);
            /* Reset Pango's default font map (and our cached context) so the next
             * layout sees the freshly registered family. */
            pango_cairo_font_map_set_default(0);
            cn1Pango = 0;
        } else {
            close(fd);
        }
    }

    /* The font's own family name is the reliable handle for Pango. */
    if (FT_Init_FreeType(&lib) == 0) {
        if (FT_New_Memory_Face(lib, bytes, len, 0, &face) == 0) {
            if (face->family_name) {
                strncpy(family, face->family_name, sizeof(family) - 1);
                family[sizeof(family) - 1] = 0;
            }
            FT_Done_Face(face);
        }
        FT_Done_FreeType(lib);
    }
    if (family[0] == 0) {
        /* Fall back to the requested name (minus the native: scheme). */
        strncpy(family, strncmp(name, "native:", 7) == 0 ? name + 7 : name, sizeof(family) - 1);
        family[sizeof(family) - 1] = 0;
    }
    return (JAVA_LONG) (intptr_t) cn1MakeFont(family, PANGO_WEIGHT_NORMAL, 0, 18);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_deriveTrueTypeFont___long_float_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_FLOAT size, JAVA_INT weight) {
    CN1Font* src = cn1FontOrDefault(font);
    CN1Font* f = (CN1Font*) calloc(1, sizeof(CN1Font));
    f->desc = pango_font_description_copy(src->desc);
    /* Pango asserts (size >= 0) and ignores the call otherwise; CN1 can transiently
     * request a non-positive derived size during layout. Clamp to keep a usable
     * description instead of leaving the copied source size in place. */
    if (size < 1.0f) {
        size = 1.0f;
    }
    pango_font_description_set_absolute_size(f->desc, (int) (size * PANGO_SCALE));
    /* CN1 weight bits: bit 0 bold, bit 1 italic (Font.STYLE_*). */
    if ((weight & 1) != 0) {
        pango_font_description_set_weight(f->desc, PANGO_WEIGHT_BOLD);
    }
    if ((weight & 2) != 0) {
        pango_font_description_set_style(f->desc, PANGO_STYLE_ITALIC);
    }
    f->pixelSize = (int) (size + 0.5f);
    cn1FontMetrics(f);
    return (JAVA_LONG) (intptr_t) f;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawString___long_java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT str, JAVA_INT x, JAVA_INT y) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    const char* s;
    PangoLayout* layout;
    if (str == JAVA_NULL || g == 0) {
        return;
    }
    s = stringToUTF8(threadStateData, str);
    cairo_save(g->cr);
    cairo_identity_matrix(g->cr);
    cairo_rectangle(g->cr, g->clipX, g->clipY, g->clipW, g->clipH);
    cairo_clip(g->cr);
    cairo_set_matrix(g->cr, &g->transform);
    cn1LinuxApplySource(g);
    layout = pango_cairo_create_layout(g->cr);
    pango_layout_set_font_description(layout, cn1FontOrDefault(g->font ? (JAVA_LONG) (intptr_t) g->font : 0)->desc);
    cn1LayoutSetText(layout, s, -1);
    cairo_move_to(g->cr, x, y);
    pango_cairo_show_layout(g->cr, layout);
    g_object_unref(layout);
    cairo_restore(g->cr);
}
