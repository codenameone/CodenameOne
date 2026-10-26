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
 * Offscreen render targets and PNG encoding, shared by the GTK window
 * (cn1_linux_window.c) and the embedded display (cn1_linux_embedded.c). Pure
 * Cairo, so it is compiled into both Linux targets unchanged.
 */

#include "cn1_linux_gfx.h"
#include <stdlib.h>
#include <string.h>

JAVA_LONG com_codename1_impl_linux_LinuxNative_createOffscreenGraphics___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = (CN1Graphics*) calloc(1, sizeof(CN1Graphics));
    int w = width > 0 ? width : 1;
    int h = height > 0 ? height : 1;
    g->surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    g->cr = cairo_create(g->surface);
    g->width = w;
    g->height = h;
    g->alpha = 255;
    g->clipW = w;
    g->clipH = h;
    cairo_matrix_init_identity(&g->transform);
    return (JAVA_LONG) (intptr_t) g;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_saveGraphicsToPng___long_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT path) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    const char* p = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    if (!g || !g->surface || !p) {
        return JAVA_FALSE;
    }
    cairo_surface_flush(g->surface);
    return cairo_surface_write_to_png(g->surface, p) == CAIRO_STATUS_SUCCESS ? JAVA_TRUE : JAVA_FALSE;
}

/* A growable byte buffer used as the cairo PNG write closure. */
struct CN1PngBuf { unsigned char* data; int len; int cap; };

static cairo_status_t cn1PngWrite(void* closure, const unsigned char* data, unsigned int length) {
    struct CN1PngBuf* b = (struct CN1PngBuf*) closure;
    if (b->len + (int) length > b->cap) {
        int cap = b->cap > 0 ? b->cap * 2 : 8192;
        while (cap < b->len + (int) length) cap *= 2;
        b->data = (unsigned char*) realloc(b->data, cap);
        b->cap = cap;
    }
    memcpy(b->data + b->len, data, length);
    b->len += (int) length;
    return CAIRO_STATUS_SUCCESS;
}

/* PNG-encodes an ARGB32 surface into a freshly malloc'd buffer; caller free()s
 * *outData. Returns 1 on success. Shared by image.c (encodeArgbToPng). */
int cn1LinuxSurfaceToPng(cairo_surface_t* surface, unsigned char** outData, int* outLen) {
    struct CN1PngBuf buf = { 0, 0, 0 };
    cairo_status_t st;
    cairo_surface_flush(surface);
    st = cairo_surface_write_to_png_stream(surface, cn1PngWrite, &buf);
    if (st != CAIRO_STATUS_SUCCESS) {
        free(buf.data);
        *outData = 0;
        *outLen = 0;
        return 0;
    }
    *outData = buf.data;
    *outLen = buf.len;
    return 1;
}

