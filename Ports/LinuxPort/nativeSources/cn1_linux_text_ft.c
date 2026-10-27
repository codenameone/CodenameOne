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
 * Fonts and text for the embedded target, on FreeType alone. The desktop target
 * uses Pango over FontConfig (cn1_linux_text.c, which the embedded project does
 * not compile); this unit defines the same natives.
 *
 * Finding a font. FontConfig answers "Sans, bold" by reading its configuration
 * -- dozens of XML files on a stock image -- and a cache, on every start. Here
 * a family is found by FILE NAME: the directories in CN1_FONT_PATH (colon
 * separated), a "fonts" directory beside the executable, then /usr/share/fonts
 * and /usr/local/share/fonts, are listed once, and the generic families map to
 * the file names the common free families ship under (DejaVu, Liberation, Noto,
 * FreeFont, Roboto, Open Sans). A named family matches a file whose name starts
 * with it. Nothing is parsed until a face is used. A face the application
 * registers from memory (its own TrueType fonts, the material icon font) is
 * matched by the family name inside it, before any file.
 *
 * Bold and italic come from the family's own faces when it has them and are
 * synthesized otherwise (FreeType emboldens and slants the outline).
 *
 * Shaping is per character: advances, no kerning and no ligatures. That is what
 * a UI's labels need in the scripts DejaVu covers; complex scripts need the
 * desktop target's HarfBuzz, and are not what the embedded target is for.
 *
 * A character the face lacks is drawn from the Sans face, so a symbol in a label
 * set in an icon font, or an accent the icon font lacks, still shows.
 */
#ifdef CN1_LINUX_EMBEDDED

#define _GNU_SOURCE
#include "cn1_linux_gfx.h"
#include <dirent.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_SIZES_H
#include FT_SYNTHESIS_H

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* CN1 font constants (com.codename1.ui.Font). */
#define CN1_FACE_MONOSPACE   32
#define CN1_FACE_PROPORTIONAL 64
#define CN1_STYLE_BOLD       1
#define CN1_STYLE_ITALIC     2
#define CN1_SIZE_SMALL       8
#define CN1_SIZE_LARGE       16

/* --------------------------------------------------------------- the faces */

typedef struct CN1FtFace {
    FT_Face face;
    char family[128];
    char path[PATH_MAX];        /* empty for a face registered from memory */
    int bold;
    int italic;
    unsigned char* memory;      /* owned bytes of a face registered from memory */
    struct CN1FtFace* next;
} CN1FtFace;

typedef struct {
    uint32_t cp;                /* 0: empty slot */
    int advance;                /* 26.6 */
    int left, top, width, height;
    unsigned char* bits;        /* width*height coverage, NULL for a blank glyph */
} CN1FtGlyph;

typedef struct CN1FtSized {
    CN1FtFace* face;
    int px;
    int synthBold;
    int synthItalic;
    FT_Size size;
    int ascent;
    int height;
    CN1FtGlyph* glyphs;
    int glyphCapacity;
    int glyphCount;
    struct CN1FtSized* fallback; /* the Sans face at this size; NULL for Sans itself */
    int fallbackResolved;
    struct CN1FtSized* next;
} CN1FtSized;

struct CN1Font {
    CN1FtSized* sized;          /* NULL when no face could be found at all */
    char family[128];
    int bold;
    int italic;
    int pixelSize;
    int ascent;
    int height;
};

static FT_Library cn1Ft = 0;
static CN1FtFace* cn1Faces = 0;
static CN1FtSized* cn1Sized = 0;

typedef struct {
    char* path;
    char* key;                  /* lower case, no extension, letters and digits only */
} CN1FontFile;

static CN1FontFile* cn1FontFiles = 0;
static int cn1FontFileCount = 0;
static int cn1FontFileCapacity = 0;
static int cn1FontFilesListed = 0;

static FT_Library cn1FtLibrary(void) {
    if (cn1Ft == 0 && FT_Init_FreeType(&cn1Ft) != 0) {
        cn1Ft = 0;
    }
    return cn1Ft;
}

/* "DejaVuSans-Bold.ttf" -> "dejavusansbold": the form names are compared in. */
static void cn1FontKey(const char* name, char* out, size_t room) {
    size_t n = 0;
    const char* dot = strrchr(name, '.');
    const char* end = dot ? dot : name + strlen(name);
    for (; name < end && n + 1 < room; name++) {
        char c = *name;
        if (c >= 'A' && c <= 'Z') {
            out[n++] = (char) (c - 'A' + 'a');
        } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
            out[n++] = c;
        }
    }
    out[n] = 0;
}

static int cn1IsFontFile(const char* name) {
    const char* dot = strrchr(name, '.');
    return dot != 0 && (strcasecmp(dot, ".ttf") == 0 || strcasecmp(dot, ".otf") == 0 || strcasecmp(dot, ".ttc") == 0);
}

static void cn1AddFontFile(const char* path, const char* name) {
    char key[256];
    if (cn1FontFileCount == cn1FontFileCapacity) {
        int capacity = cn1FontFileCapacity == 0 ? 64 : cn1FontFileCapacity * 2;
        CN1FontFile* files = (CN1FontFile*) realloc(cn1FontFiles, sizeof(CN1FontFile) * (size_t) capacity);
        if (files == 0) {
            return;
        }
        cn1FontFiles = files;
        cn1FontFileCapacity = capacity;
    }
    cn1FontKey(name, key, sizeof(key));
    cn1FontFiles[cn1FontFileCount].path = strdup(path);
    cn1FontFiles[cn1FontFileCount].key = strdup(key);
    if (cn1FontFiles[cn1FontFileCount].path != 0 && cn1FontFiles[cn1FontFileCount].key != 0) {
        cn1FontFileCount++;
    }
}

static void cn1ListFontDir(const char* dir, int depth) {
    DIR* d;
    struct dirent* entry;
    if (depth > 4 || (d = opendir(dir)) == 0) {
        return;
    }
    while ((entry = readdir(d)) != 0) {
        char path[PATH_MAX];
        struct stat st;
        if (entry->d_name[0] == '.') {
            continue;
        }
        if (snprintf(path, sizeof(path), "%s/%s", dir, entry->d_name) >= (int) sizeof(path)) {
            continue;
        }
        if (cn1IsFontFile(entry->d_name)) {
            cn1AddFontFile(path, entry->d_name);
        } else if (stat(path, &st) == 0 && S_ISDIR(st.st_mode)) {
            cn1ListFontDir(path, depth + 1);
        }
    }
    closedir(d);
}

static void cn1ListFontFiles(void) {
    const char* env;
    char exe[PATH_MAX];
    ssize_t n;
    if (cn1FontFilesListed) {
        return;
    }
    cn1FontFilesListed = 1;
    env = getenv("CN1_FONT_PATH");
    if (env != 0) {
        char* copy = strdup(env);
        char* save = 0;
        char* part = copy ? strtok_r(copy, ":", &save) : 0;
        while (part != 0) {
            cn1ListFontDir(part, 0);
            part = strtok_r(0, ":", &save);
        }
        free(copy);
    }
    n = readlink("/proc/self/exe", exe, sizeof(exe) - 7);
    if (n > 0) {
        char* slash;
        exe[n] = 0;
        slash = strrchr(exe, '/');
        if (slash != 0) {
            strcpy(slash + 1, "fonts");
            cn1ListFontDir(exe, 0);
        }
    }
    cn1ListFontDir("/usr/share/fonts", 0);
    cn1ListFontDir("/usr/local/share/fonts", 0);
}

static const char* cn1FindFontFile(const char* key) {
    int i;
    for (i = 0; i < cn1FontFileCount; i++) {
        if (strcmp(cn1FontFiles[i].key, key) == 0) {
            return cn1FontFiles[i].path;
        }
    }
    return 0;
}

static CN1FtFace* cn1FaceFromFile(const char* path) {
    CN1FtFace* f;
    FT_Library lib = cn1FtLibrary();
    for (f = cn1Faces; f != 0; f = f->next) {
        if (strcmp(f->path, path) == 0) {
            return f;
        }
    }
    if (lib == 0) {
        return 0;
    }
    f = (CN1FtFace*) calloc(1, sizeof(CN1FtFace));
    if (f == 0) {
        return 0;
    }
    if (FT_New_Face(lib, path, 0, &f->face) != 0) {
        free(f);
        return 0;
    }
    strncpy(f->path, path, sizeof(f->path) - 1);
    strncpy(f->family, f->face->family_name ? f->face->family_name : "", sizeof(f->family) - 1);
    f->bold = (f->face->style_flags & FT_STYLE_FLAG_BOLD) != 0;
    f->italic = (f->face->style_flags & FT_STYLE_FLAG_ITALIC) != 0;
    f->next = cn1Faces;
    cn1Faces = f;
    return f;
}

/* File names of the generic families, regular / bold / italic / bold italic. */
static const char* const cn1SansNames[4][7] = {
    { "dejavusans", "liberationsansregular", "notosansregular", "freesans", "robotoregular", "opensansregular", "arial" },
    { "dejavusansbold", "liberationsansbold", "notosansbold", "freesansbold", "robotobold", "opensansbold", "arialbd" },
    { "dejavusansoblique", "liberationsansitalic", "notosansitalic", "freesansoblique", "robotoitalic", "opensansitalic", "ariali" },
    { "dejavusansboldoblique", "liberationsansbolditalic", "notosansbolditalic", "freesansboldoblique", "robotobolditalic", "opensansbolditalic", "arialbi" }
};
static const char* const cn1SerifNames[4][7] = {
    { "dejavuserif", "liberationserifregular", "notoserifregular", "freeserif", 0, 0, 0 },
    { "dejavuserifbold", "liberationserifbold", "notoserifbold", "freeserifbold", 0, 0, 0 },
    { "dejavuserifitalic", "liberationserifitalic", "notoserifitalic", "freeserifitalic", 0, 0, 0 },
    { "dejavuserifbolditalic", "liberationserifbolditalic", "notoserifbolditalic", "freeserifbolditalic", 0, 0, 0 }
};
static const char* const cn1MonoNames[4][7] = {
    { "dejavusansmono", "liberationmonoregular", "notosansmonoregular", "freemono", 0, 0, 0 },
    { "dejavusansmonobold", "liberationmonobold", "notosansmonobold", "freemonobold", 0, 0, 0 },
    { "dejavusansmonooblique", "liberationmonoitalic", "notosansmonoitalic", "freemonooblique", 0, 0, 0 },
    { "dejavusansmonoboldoblique", "liberationmonobolditalic", "notosansmonobolditalic", "freemonoboldoblique", 0, 0, 0 }
};

static CN1FtFace* cn1GenericFace(const char* const names[4][7], int style) {
    int i;
    for (i = 0; i < 7; i++) {
        const char* path = names[style][i] ? cn1FindFontFile(names[style][i]) : 0;
        if (path != 0) {
            CN1FtFace* f = cn1FaceFromFile(path);
            if (f != 0) {
                return f;
            }
        }
    }
    return 0;
}

/* The Sans regular face: the default, and the fallback for missing glyphs. */
static CN1FtFace* cn1SansFace(void) {
    CN1FtFace* f;
    cn1ListFontFiles();
    f = cn1GenericFace(cn1SansNames, 0);
    if (f == 0 && cn1FontFileCount > 0) {
        /* No family this unit knows: any font beats no text. */
        f = cn1FaceFromFile(cn1FontFiles[0].path);
    }
    return f;
}

/*
 * The face for a family and style, and whether bold or italic still has to be
 * synthesized on top of it.
 */
static CN1FtFace* cn1ResolveFace(const char* family, int bold, int italic, int* synthBold, int* synthItalic) {
    const char* const (*generic)[7] = 0;
    CN1FtFace* best = 0;
    CN1FtFace* f;
    char key[256];
    int style = (bold ? 1 : 0) + (italic ? 2 : 0);
    int i;
    *synthBold = 0;
    *synthItalic = 0;
    /* A face the application registered from memory, by its own family name. */
    for (f = cn1Faces; f != 0; f = f->next) {
        if (f->memory != 0 && strcasecmp(f->family, family) == 0) {
            if (f->bold == bold && f->italic == italic) {
                best = f;
                break;
            }
            if (best == 0) {
                best = f;
            }
        }
    }
    if (best == 0) {
        cn1ListFontFiles();
        if (strcasecmp(family, "Serif") == 0) {
            generic = cn1SerifNames;
        } else if (strcasecmp(family, "Monospace") == 0) {
            generic = cn1MonoNames;
        } else if (strcasecmp(family, "Sans") == 0 || strcasecmp(family, "sans-serif") == 0
                || strcasecmp(family, "System") == 0) {
            generic = cn1SansNames;
        }
        if (generic != 0) {
            best = cn1GenericFace(generic, style);
            if (best == 0 && style != 0) {
                best = cn1GenericFace(generic, 0);
            }
        } else {
            /* A named family: its files start with its name. Prefer the one whose
             * name says the style asked for. */
            static const char* const suffixes[4][3] = {
                { "regular", "", "book" },
                { "bold", "bd", "semibold" },
                { "italic", "oblique", "it" },
                { "bolditalic", "boldoblique", "bi" }
            };
            char want[300];
            cn1FontKey(family, key, sizeof(key));
            for (i = 0; i < 3 && best == 0; i++) {
                const char* path;
                snprintf(want, sizeof(want), "%s%s", key, suffixes[style][i]);
                path = cn1FindFontFile(want);
                if (path != 0) {
                    best = cn1FaceFromFile(path);
                }
            }
            for (i = 0; i < 3 && best == 0 && style != 0; i++) {
                const char* path;
                snprintf(want, sizeof(want), "%s%s", key, suffixes[0][i]);
                path = cn1FindFontFile(want);
                if (path != 0) {
                    best = cn1FaceFromFile(path);
                }
            }
        }
        if (best == 0) {
            best = cn1GenericFace(cn1SansNames, style);
            if (best == 0) {
                best = cn1SansFace();
            }
        }
    }
    if (best != 0) {
        *synthBold = bold && !best->bold;
        *synthItalic = italic && !best->italic;
    }
    return best;
}

/* ------------------------------------------------------------- face, sized */

static CN1FtSized* cn1SizedFace(CN1FtFace* face, int px, int synthBold, int synthItalic) {
    CN1FtSized* s;
    if (face == 0) {
        return 0;
    }
    if (px < 1) {
        px = 1;
    }
    for (s = cn1Sized; s != 0; s = s->next) {
        if (s->face == face && s->px == px && s->synthBold == synthBold && s->synthItalic == synthItalic) {
            return s;
        }
    }
    s = (CN1FtSized*) calloc(1, sizeof(CN1FtSized));
    if (s == 0) {
        return 0;
    }
    if (FT_New_Size(face->face, &s->size) != 0) {
        free(s);
        return 0;
    }
    FT_Activate_Size(s->size);
    FT_Set_Pixel_Sizes(face->face, 0, (FT_UInt) px);
    s->face = face;
    s->px = px;
    s->synthBold = synthBold;
    s->synthItalic = synthItalic;
    s->ascent = (int) ((s->size->metrics.ascender + 32) >> 6);
    s->height = s->ascent + (int) ((-s->size->metrics.descender + 32) >> 6);
    if (s->height <= 0) {
        s->ascent = px;
        s->height = px + px / 4;
    }
    s->next = cn1Sized;
    cn1Sized = s;
    return s;
}

/* Loads glyph index `index` of `s` into its face's slot, styled; 0 on success. */
static int cn1LoadGlyph(CN1FtSized* s, FT_UInt index) {
    FT_Face face = s->face->face;
    FT_Activate_Size(s->size);
    if (FT_Load_Glyph(face, index, FT_LOAD_TARGET_LIGHT | FT_LOAD_NO_BITMAP) != 0) {
        return -1;
    }
    if (s->synthItalic) {
        FT_GlyphSlot_Oblique(face->glyph);
    }
    if (s->synthBold) {
        FT_GlyphSlot_Embolden(face->glyph);
    }
    return 0;
}

static CN1FtSized* cn1Fallback(CN1FtSized* s) {
    if (!s->fallbackResolved) {
        CN1FtFace* sans = cn1SansFace();
        s->fallbackResolved = 1;
        if (sans != 0 && sans != s->face) {
            s->fallback = cn1SizedFace(sans, s->px, s->synthBold, s->synthItalic);
        }
    }
    return s->fallback;
}

/* The face (this one or the fallback) and index that draw code point cp. */
static CN1FtSized* cn1GlyphSource(CN1FtSized* s, uint32_t cp, FT_UInt* index) {
    *index = FT_Get_Char_Index(s->face->face, cp);
    if (*index == 0) {
        CN1FtSized* fb = cn1Fallback(s);
        if (fb != 0) {
            FT_UInt other = FT_Get_Char_Index(fb->face->face, cp);
            if (other != 0) {
                *index = other;
                return fb;
            }
        }
    }
    return s;
}

static CN1FtGlyph* cn1GlyphSlot(CN1FtSized* s, uint32_t cp) {
    uint32_t mask = (uint32_t) s->glyphCapacity - 1;
    uint32_t i = (cp * 2654435761u) & mask;
    while (s->glyphs[i].cp != 0 && s->glyphs[i].cp != cp) {
        i = (i + 1) & mask;
    }
    return &s->glyphs[i];
}

static int cn1GrowGlyphs(CN1FtSized* s) {
    CN1FtGlyph* old = s->glyphs;
    int oldCapacity = s->glyphCapacity, i;
    int capacity = oldCapacity == 0 ? 256 : oldCapacity * 2;
    CN1FtGlyph* grown = (CN1FtGlyph*) calloc((size_t) capacity, sizeof(CN1FtGlyph));
    if (grown == 0) {
        return 0;
    }
    s->glyphs = grown;
    s->glyphCapacity = capacity;
    for (i = 0; i < oldCapacity; i++) {
        if (old[i].cp != 0) {
            *cn1GlyphSlot(s, old[i].cp) = old[i];
        }
    }
    free(old);
    return 1;
}

/* The rendered, cached glyph for code point cp (never 0: callers map NUL). */
static CN1FtGlyph* cn1Glyph(CN1FtSized* s, uint32_t cp) {
    CN1FtGlyph* g;
    CN1FtSized* src;
    FT_UInt index;
    FT_GlyphSlot slot;
    if ((s->glyphCount + 1) * 10 >= s->glyphCapacity * 7 && !cn1GrowGlyphs(s)) {
        return 0;
    }
    g = cn1GlyphSlot(s, cp);
    if (g->cp == cp) {
        return g;
    }
    memset(g, 0, sizeof(*g));
    g->cp = cp;
    s->glyphCount++;
    src = cn1GlyphSource(s, cp, &index);
    if (cn1LoadGlyph(src, index) != 0) {
        return g;
    }
    slot = src->face->face->glyph;
    g->advance = (int) slot->advance.x;
    if (FT_Render_Glyph(slot, FT_RENDER_MODE_NORMAL) == 0 && slot->bitmap.width > 0 && slot->bitmap.rows > 0) {
        int y;
        g->left = slot->bitmap_left;
        g->top = slot->bitmap_top;
        g->width = (int) slot->bitmap.width;
        g->height = (int) slot->bitmap.rows;
        g->bits = (unsigned char*) malloc((size_t) g->width * (size_t) g->height);
        /* A negative pitch puts the top row last in memory; the normal render
         * mode never produces one, and a glyph that has it is drawn blank. */
        if (g->bits != 0 && slot->bitmap.pitch < g->width) {
            memset(g->bits, 0, (size_t) g->width * (size_t) g->height);
        } else if (g->bits != 0) {
            for (y = 0; y < g->height; y++) {
                memcpy(g->bits + (size_t) y * (size_t) g->width,
                       slot->bitmap.buffer + (ptrdiff_t) y * slot->bitmap.pitch, (size_t) g->width);
            }
        }
    }
    return g;
}

/* ------------------------------------------------------------------- fonts */

static CN1Font* cn1MakeFont(const char* family, int bold, int italic, int pixelSize) {
    CN1Font* f = (CN1Font*) calloc(1, sizeof(CN1Font));
    int synthBold = 0, synthItalic = 0;
    CN1FtFace* face;
    if (f == 0) {
        return 0;
    }
    if (pixelSize < 1) {
        pixelSize = 1;
    }
    strncpy(f->family, family, sizeof(f->family) - 1);
    f->bold = bold;
    f->italic = italic;
    f->pixelSize = pixelSize;
    face = cn1ResolveFace(family, bold, italic, &synthBold, &synthItalic);
    f->sized = cn1SizedFace(face, pixelSize, synthBold, synthItalic);
    if (f->sized != 0) {
        f->ascent = f->sized->ascent;
        f->height = f->sized->height;
    } else {
        f->ascent = pixelSize;
        f->height = pixelSize + pixelSize / 4;
    }
    return f;
}

static CN1Font* cn1DefaultFont = 0;

static CN1Font* cn1FontOrDefault(JAVA_LONG font) {
    CN1Font* f = (CN1Font*) (intptr_t) font;
    if (f == 0) {
        if (cn1DefaultFont == 0) {
            cn1DefaultFont = cn1MakeFont("Sans", 0, 0, 18);
        }
        f = cn1DefaultFont;
    }
    return f;
}

/* The next code point of UTF-8 text; malformed bytes read as U+FFFD. */
static uint32_t cn1NextCodePoint(const unsigned char** p, const unsigned char* end) {
    const unsigned char* s = *p;
    uint32_t c = *s++;
    int extra = 0;
    if (c >= 0xf0 && c < 0xf8) {
        c &= 0x07;
        extra = 3;
    } else if (c >= 0xe0) {
        c &= 0x0f;
        extra = 2;
    } else if (c >= 0xc0) {
        c &= 0x1f;
        extra = 1;
    } else if (c >= 0x80) {
        *p = s;
        return 0xfffd;
    }
    while (extra-- > 0) {
        if (s >= end || (*s & 0xc0) != 0x80) {
            *p = s;
            return 0xfffd;
        }
        c = (c << 6) | (*s++ & 0x3f);
    }
    *p = s;
    return c == 0 ? 0xfffd : c;
}

static int cn1MeasureUtf8(CN1Font* f, const char* utf8, int len) {
    const unsigned char* p = (const unsigned char*) utf8;
    const unsigned char* end;
    long total = 0;
    if (utf8 == 0 || f->sized == 0) {
        return 0;
    }
    end = p + (len < 0 ? (int) strlen(utf8) : len);
    while (p < end) {
        CN1FtGlyph* g = cn1Glyph(f->sized, cn1NextCodePoint(&p, end));
        if (g != 0) {
            total += g->advance;
        }
    }
    return (int) ((total + 32) >> 6);
}

/* Transformed text (a rotation or a scale on the graphics): glyphs are rendered
 * through the matrix as they are drawn and not cached; the UI rarely does it. */
static void cn1DrawTransformed(CN1Graphics* g, CN1Font* f, const char* s, double x, double baseline) {
    const unsigned char* p = (const unsigned char*) s;
    const unsigned char* end = p + strlen(s);
    const cairo_matrix_t* m = &g->transform;
    FT_Matrix ft;
    double pen = x;
    /* FreeType's y axis points up, the screen's down. */
    ft.xx = (FT_Fixed) (m->xx * 65536);
    ft.xy = (FT_Fixed) (-m->xy * 65536);
    ft.yx = (FT_Fixed) (-m->yx * 65536);
    ft.yy = (FT_Fixed) (m->yy * 65536);
    while (p < end) {
        uint32_t cp = cn1NextCodePoint(&p, end);
        FT_UInt index;
        CN1FtSized* src = cn1GlyphSource(f->sized, cp, &index);
        FT_Face face = src->face->face;
        FT_GlyphSlot slot;
        double dx = pen, dy = baseline;
        FT_Vector delta;
        cairo_matrix_transform_point(m, &dx, &dy);
        delta.x = (FT_Pos) ((dx - floor(dx)) * 64);
        delta.y = (FT_Pos) (-(dy - floor(dy)) * 64);
        FT_Set_Transform(face, &ft, &delta);
        if (cn1LoadGlyph(src, index) == 0) {
            slot = face->glyph;
            if (FT_Render_Glyph(slot, FT_RENDER_MODE_NORMAL) == 0 && slot->bitmap.width > 0 && slot->bitmap.pitch > 0) {
                cn1r_mask_a8(g->cr, slot->bitmap.buffer, slot->bitmap.pitch, (int) slot->bitmap.width,
                             (int) slot->bitmap.rows, (int) floor(dx) + slot->bitmap_left, (int) floor(dy) - slot->bitmap_top);
            }
        }
        FT_Set_Transform(face, 0, 0);
        {
            /* The advance in user space: the untransformed glyph's. */
            CN1FtGlyph* cached = cn1Glyph(f->sized, cp);
            pen += cached != 0 ? cached->advance / 64.0 : 0;
        }
    }
}

/* ------------------------------------------------------------------ natives */

JAVA_LONG com_codename1_impl_linux_LinuxNative_createFont___int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT face, JAVA_INT style, JAVA_INT size) {
    const char* family = "Sans";
    int pixelSize = 18;
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
    return (JAVA_LONG) (intptr_t) cn1MakeFont(family, (style & CN1_STYLE_BOLD) != 0, (style & CN1_STYLE_ITALIC) != 0, pixelSize);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_getDefaultFont___R_long(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_LONG) (intptr_t) cn1FontOrDefault(0);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_stringWidth___long_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_OBJECT str) {
    if (str == JAVA_NULL) {
        return 0;
    }
    return cn1MeasureUtf8(cn1FontOrDefault(font), stringToUTF8(threadStateData, str), -1);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_charWidth___long_char_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_CHAR c) {
    CN1Font* f = cn1FontOrDefault(font);
    CN1FtGlyph* g;
    if (f->sized == 0) {
        return 0;
    }
    g = cn1Glyph(f->sized, c == 0 ? 0xfffd : (uint32_t) (c & 0xffff));
    return g != 0 ? (g->advance + 32) >> 6 : 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_charsWidth___long_char_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_OBJECT chars, JAVA_INT offset, JAVA_INT length) {
    /* A Java char[] holds 2-byte JAVA_ARRAY_CHAR elements, not 4-byte JAVA_CHAR. */
    JAVA_ARRAY_CHAR* data;
    CN1Font* f = cn1FontOrDefault(font);
    long total = 0;
    int i;
    if (chars == JAVA_NULL || length <= 0 || f->sized == 0) {
        return 0;
    }
    if (offset < 0 || length > ((JAVA_ARRAY) chars)->length - offset) {
        return 0;
    }
    data = (JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA(chars);
    for (i = 0; i < length; i++) {
        uint32_t cp = (uint32_t) data[offset + i] & 0xffff;
        CN1FtGlyph* g;
        /* A surrogate pair is one character. */
        if (cp >= 0xd800 && cp < 0xdc00 && i + 1 < length) {
            uint32_t low = (uint32_t) data[offset + i + 1] & 0xffff;
            if (low >= 0xdc00 && low < 0xe000) {
                cp = 0x10000 + ((cp - 0xd800) << 10) + (low - 0xdc00);
                i++;
            }
        }
        g = cn1Glyph(f->sized, cp == 0 ? 0xfffd : cp);
        if (g != 0) {
            total += g->advance;
        }
    }
    return (int) ((total + 32) >> 6);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_fontHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG font) {
    return cn1FontOrDefault(font)->height;
}

/* The Font.STYLE_* bits a "native:" name asks for (see cn1_linux_text.c). */
static int cn1NativeFontStyle(const char* name) {
    int style = 0;
    if (name == 0 || strncmp(name, "native:", 7) != 0) {
        return 0;
    }
    if (strstr(name + 7, "Bold") != 0 || strstr(name + 7, "Black") != 0) {
        style |= CN1_STYLE_BOLD;
    }
    if (strstr(name + 7, "Italic") != 0) {
        style |= CN1_STYLE_ITALIC;
    }
    return style;
}

static CN1Font* cn1NamedFont(const char* name) {
    if (strncmp(name, "native:", 7) == 0) {
        int style = cn1NativeFontStyle(name);
        return cn1MakeFont("Sans", (style & CN1_STYLE_BOLD) != 0, (style & CN1_STYLE_ITALIC) != 0, 18);
    }
    return cn1MakeFont(name, 0, 0, 18);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_loadTrueTypeFont___java_lang_String_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT fontName, JAVA_OBJECT fileName) {
    char name[256];
    const char* s = fontName == JAVA_NULL ? "Sans" : stringToUTF8(threadStateData, fontName);
    strncpy(name, s ? s : "Sans", sizeof(name) - 1);
    name[sizeof(name) - 1] = 0;
    if (fileName != JAVA_NULL && strncmp(name, "native:", 7) != 0) {
        /* A file the font directories have under that name is the family. */
        const char* file = stringToUTF8(threadStateData, fileName);
        const char* slash = file ? strrchr(file, '/') : 0;
        char key[256];
        const char* path;
        cn1ListFontFiles();
        cn1FontKey(slash ? slash + 1 : (file ? file : ""), key, sizeof(key));
        path = key[0] ? cn1FindFontFile(key) : 0;
        if (path != 0) {
            CN1FtFace* face = cn1FaceFromFile(path);
            if (face != 0) {
                return (JAVA_LONG) (intptr_t) cn1MakeFont(face->family, face->bold, face->italic, 18);
            }
        }
    }
    return (JAVA_LONG) (intptr_t) cn1NamedFont(name);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_loadTrueTypeFontFromMemory___java_lang_String_byte_1ARRAY_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT fontName, JAVA_OBJECT data) {
    char name[256];
    const char* s = fontName == JAVA_NULL ? "Sans" : stringToUTF8(threadStateData, fontName);
    CN1FtFace* f;
    FT_Library lib;
    int len;
    strncpy(name, s ? s : "Sans", sizeof(name) - 1);
    name[sizeof(name) - 1] = 0;
    if (data == JAVA_NULL || (len = (int) ((JAVA_ARRAY) data)->length) <= 0 || (lib = cn1FtLibrary()) == 0) {
        return (JAVA_LONG) (intptr_t) cn1NamedFont(name);
    }
    f = (CN1FtFace*) calloc(1, sizeof(CN1FtFace));
    if (f == 0) {
        return (JAVA_LONG) (intptr_t) cn1NamedFont(name);
    }
    /* FreeType reads the face from these bytes for as long as it lives. */
    f->memory = (unsigned char*) malloc((size_t) len);
    if (f->memory == 0) {
        free(f);
        return (JAVA_LONG) (intptr_t) cn1NamedFont(name);
    }
    memcpy(f->memory, CN1_ARRAY_DATA(data), (size_t) len);
    if (FT_New_Memory_Face(lib, f->memory, len, 0, &f->face) != 0) {
        free(f->memory);
        free(f);
        return (JAVA_LONG) (intptr_t) cn1NamedFont(name);
    }
    strncpy(f->family, f->face->family_name ? f->face->family_name
            : (strncmp(name, "native:", 7) == 0 ? name + 7 : name), sizeof(f->family) - 1);
    f->bold = (f->face->style_flags & FT_STYLE_FLAG_BOLD) != 0;
    f->italic = (f->face->style_flags & FT_STYLE_FLAG_ITALIC) != 0;
    f->next = cn1Faces;
    cn1Faces = f;
    return (JAVA_LONG) (intptr_t) cn1MakeFont(f->family, f->bold, f->italic, 18);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_deriveTrueTypeFont___long_float_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG font, JAVA_FLOAT size, JAVA_INT weight) {
    CN1Font* src = cn1FontOrDefault(font);
    /* A non-positive size is the documented "unset" sentinel (see cn1_linux_text.c). */
    if (size <= 0.0f) {
        size = 15.0f;
    }
    return (JAVA_LONG) (intptr_t) cn1MakeFont(src->family, src->bold || (weight & 1) != 0,
            src->italic || (weight & 2) != 0, (int) (size + 0.5f));
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawString___long_java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT str, JAVA_INT x, JAVA_INT y) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    CN1Font* f;
    const char* s;
    const cairo_matrix_t* m;
    if (str == JAVA_NULL || g == 0) {
        return;
    }
    f = cn1FontOrDefault(g->font ? (JAVA_LONG) (intptr_t) g->font : 0);
    if (f->sized == 0) {
        return;
    }
    s = stringToUTF8(threadStateData, str);
    if (s == 0 || *s == 0) {
        return;
    }
    cairo_save(g->cr);
    cn1LinuxApplyClip(g);
    cairo_set_matrix(g->cr, &g->transform);
    cn1LinuxApplySource(g);
    m = &g->transform;
    if (m->xx == 1 && m->yy == 1 && m->xy == 0 && m->yx == 0) {
        /* (x, y) is the top of the line, as with Pango; the baseline is an ascent
         * below it. Glyphs land on whole pixels, where they were hinted. */
        const unsigned char* p = (const unsigned char*) s;
        const unsigned char* end = p + strlen(s);
        long pen = (long) ((x + m->x0) * 64);
        int baseline = (int) floor(y + m->y0 + 0.5) + f->ascent;
        while (p < end) {
            CN1FtGlyph* glyph = cn1Glyph(f->sized, cn1NextCodePoint(&p, end));
            if (glyph == 0) {
                continue;
            }
            if (glyph->bits != 0) {
                cn1r_mask_a8(g->cr, glyph->bits, glyph->width, glyph->width, glyph->height,
                             (int) ((pen + 32) >> 6) + glyph->left, baseline - glyph->top);
            }
            pen += glyph->advance;
        }
    } else {
        cn1DrawTransformed(g, f, s, x, y + f->ascent);
    }
    cairo_restore(g->cr);
}

#endif /* CN1_LINUX_EMBEDDED */
