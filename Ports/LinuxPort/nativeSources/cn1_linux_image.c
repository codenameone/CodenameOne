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
 * Image decode/encode/manipulation for the native Codename One Linux port. Decode
 * goes through GdkPixbuf (PNG/JPEG/GIF/BMP/...); every image is held as a Cairo
 * ARGB32 surface so the graphics primitives blit it uniformly and getImageGraphics
 * can wrap a mutable image as a draw target. Codename One pixels are straight
 * (non-premultiplied) 0xAARRGGBB int[]; Cairo ARGB32 is premultiplied native-endian,
 * so createImageFromARGB / imageGetRGB convert between the two.
 *
 * NOTE: implemented against GdkPixbuf/Cairo but not yet compiled/run on a
 * Linux/GTK host -- see Ports/LinuxPort/status.md.
 */

#include "cn1_linux_gfx.h"
#include <stdlib.h>
#include <string.h>

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__JAVA_INT;
int cn1LinuxSurfaceToPng(cairo_surface_t* surface, unsigned char** outData, int* outLen);

#define CN1I(p) ((CN1Image*) (intptr_t) (p))

static CN1Image* cn1WrapSurface(cairo_surface_t* surface) {
    CN1Image* img = (CN1Image*) calloc(1, sizeof(CN1Image));
    img->surface = surface;
    img->width = cairo_image_surface_get_width(surface);
    img->height = cairo_image_surface_get_height(surface);
    return img;
}

/* Writes CN1 straight 0xAARRGGBB pixels into a fresh premultiplied ARGB32 surface. */
static cairo_surface_t* cn1SurfaceFromArgb(const JAVA_INT* argb, int w, int h) {
    cairo_surface_t* s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    unsigned char* base = cairo_image_surface_get_data(s);
    int stride = cairo_image_surface_get_stride(s);
    int x, y;
    cairo_surface_flush(s);
    for (y = 0; y < h; y++) {
        uint32_t* row = (uint32_t*) (base + y * stride);
        for (x = 0; x < w; x++) {
            uint32_t p = (uint32_t) argb[y * w + x];
            uint32_t a = (p >> 24) & 0xff;
            uint32_t r = (p >> 16) & 0xff;
            uint32_t g = (p >> 8) & 0xff;
            uint32_t b = p & 0xff;
            r = r * a / 255;
            g = g * a / 255;
            b = b * a / 255;
            row[x] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
    cairo_surface_mark_dirty(s);
    return s;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_createImageFromARGB___int_1ARRAY_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) {
    JAVA_INT* px;
    if (argb == JAVA_NULL || width <= 0 || height <= 0) {
        return 0;
    }
    px = (JAVA_INT*) (*(JAVA_ARRAY) argb).data;
    return (JAVA_LONG) (intptr_t) cn1WrapSurface(cn1SurfaceFromArgb(px, width, height));
}

static CN1Image* cn1ImageFromPixbuf(GdkPixbuf* pixbuf) {
    cairo_surface_t* s;
    cairo_t* cr;
    int w, h;
    if (pixbuf == 0) {
        return 0;
    }
    w = gdk_pixbuf_get_width(pixbuf);
    h = gdk_pixbuf_get_height(pixbuf);
    s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    cr = cairo_create(s);
    gdk_cairo_set_source_pixbuf(cr, pixbuf, 0, 0);
    cairo_paint(cr);
    cairo_destroy(cr);
    return cn1WrapSurface(s);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_createImageFromFile___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path) {
    const char* p = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    GdkPixbuf* pix;
    CN1Image* img;
    if (!p) {
        return 0;
    }
    pix = gdk_pixbuf_new_from_file(p, 0);
    if (!pix) {
        return 0;
    }
    img = cn1ImageFromPixbuf(pix);
    g_object_unref(pix);
    return (JAVA_LONG) (intptr_t) img;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_createImageFromBytes___byte_1ARRAY_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT offset, JAVA_INT length) {
    unsigned char* bytes;
    GInputStream* stream;
    GdkPixbuf* pix;
    CN1Image* img;
    if (data == JAVA_NULL || length <= 0) {
        return 0;
    }
    bytes = (unsigned char*) (*(JAVA_ARRAY) data).data;
    stream = g_memory_input_stream_new_from_data(bytes + offset, length, 0);
    pix = gdk_pixbuf_new_from_stream(stream, 0, 0);
    g_object_unref(stream);
    if (!pix) {
        return 0;
    }
    img = cn1ImageFromPixbuf(pix);
    g_object_unref(pix);
    return (JAVA_LONG) (intptr_t) img;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_createMutableImage___int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT width, JAVA_INT height, JAVA_INT fillColor) {
    cairo_surface_t* s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width > 0 ? width : 1, height > 0 ? height : 1);
    cairo_t* cr = cairo_create(s);
    double a = ((fillColor >> 24) & 0xff) / 255.0;
    double r = ((fillColor >> 16) & 0xff) / 255.0;
    double g = ((fillColor >> 8) & 0xff) / 255.0;
    double b = (fillColor & 0xff) / 255.0;
    cairo_set_source_rgba(cr, r, g, b, a);
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_paint(cr);
    cairo_destroy(cr);
    return (JAVA_LONG) (intptr_t) cn1WrapSurface(s);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_imageWidth___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG image) {
    CN1Image* img = CN1I(image);
    return img ? img->width : 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_imageHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG image) {
    CN1Image* img = CN1I(image);
    return img ? img->height : 0;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_scaleImage___long_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG image, JAVA_INT width, JAVA_INT height) {
    CN1Image* src = CN1I(image);
    cairo_surface_t* s;
    cairo_t* cr;
    if (!src || width <= 0 || height <= 0) {
        return 0;
    }
    s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
    cr = cairo_create(s);
    cairo_scale(cr, (double) width / src->width, (double) height / src->height);
    cairo_set_source_surface(cr, src->surface, 0, 0);
    cairo_paint(cr);
    cairo_destroy(cr);
    return (JAVA_LONG) (intptr_t) cn1WrapSurface(s);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_imageGetRGB___long_int_1ARRAY_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG image, JAVA_OBJECT arr, JAVA_INT offset, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1Image* img = CN1I(image);
    JAVA_INT* out;
    unsigned char* base;
    int stride;
    int ix, iy;
    if (!img || arr == JAVA_NULL) {
        return;
    }
    out = (JAVA_INT*) (*(JAVA_ARRAY) arr).data;
    cairo_surface_flush(img->surface);
    base = cairo_image_surface_get_data(img->surface);
    stride = cairo_image_surface_get_stride(img->surface);
    for (iy = 0; iy < height; iy++) {
        uint32_t* row = (uint32_t*) (base + (y + iy) * stride);
        for (ix = 0; ix < width; ix++) {
            uint32_t p = row[x + ix];
            uint32_t a = (p >> 24) & 0xff;
            uint32_t r = (p >> 16) & 0xff;
            uint32_t g = (p >> 8) & 0xff;
            uint32_t b = p & 0xff;
            if (a != 0 && a != 255) {
                r = r * 255 / a;
                g = g * 255 / a;
                b = b * 255 / a;
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
            }
            out[offset + iy * width + ix] = (JAVA_INT) ((a << 24) | (r << 16) | (g << 8) | b);
        }
    }
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_getImageGraphics___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG image) {
    CN1Image* img = CN1I(image);
    CN1Graphics* g;
    if (!img) {
        return 0;
    }
    if (img->mutableGraphics != 0) {
        return (JAVA_LONG) (intptr_t) img->mutableGraphics;
    }
    g = (CN1Graphics*) calloc(1, sizeof(CN1Graphics));
    g->surface = img->surface;
    g->cr = cairo_create(img->surface);
    g->width = img->width;
    g->height = img->height;
    g->alpha = 255;
    g->clipW = img->width;
    g->clipH = img->height;
    cairo_matrix_init_identity(&g->transform);
    img->mutableGraphics = g;
    return (JAVA_LONG) (intptr_t) g;
}

/* drawImage / drawImageScaled live here (image ownership unit). They blit the
 * CN1Image surface onto the target graphics honouring its clip, affine and
 * alpha. */
JAVA_VOID com_codename1_impl_linux_LinuxNative_drawImage___long_long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_LONG image, JAVA_INT x, JAVA_INT y) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    CN1Image* img = CN1I(image);
    if (!g || !img) {
        return;
    }
    cairo_save(g->cr);
    cairo_identity_matrix(g->cr);
    cairo_rectangle(g->cr, g->clipX, g->clipY, g->clipW, g->clipH);
    cairo_clip(g->cr);
    cairo_set_matrix(g->cr, &g->transform);
    cairo_set_source_surface(g->cr, img->surface, x, y);
    cairo_paint_with_alpha(g->cr, g->alpha / 255.0);
    cairo_restore(g->cr);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawImageScaled___long_long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_LONG image, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    CN1Image* img = CN1I(image);
    double sx, sy;
    if (!g || !img || img->width <= 0 || img->height <= 0) {
        return;
    }
    sx = (double) width / img->width;
    sy = (double) height / img->height;
    cairo_save(g->cr);
    cairo_identity_matrix(g->cr);
    cairo_rectangle(g->cr, g->clipX, g->clipY, g->clipW, g->clipH);
    cairo_clip(g->cr);
    cairo_set_matrix(g->cr, &g->transform);
    cairo_translate(g->cr, x, y);
    cairo_scale(g->cr, sx, sy);
    cairo_set_source_surface(g->cr, img->surface, 0, 0);
    cairo_paint_with_alpha(g->cr, g->alpha / 255.0);
    cairo_restore(g->cr);
}

/* drawRGB draws a CN1 straight-ARGB block directly at (x,y). */
JAVA_VOID com_codename1_impl_linux_LinuxNative_drawRGB___long_int_1ARRAY_int_int_int_int_int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT rgbData, JAVA_INT offset, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN processAlpha) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    JAVA_INT* px;
    cairo_surface_t* tmp;
    (void) processAlpha;
    if (!g || rgbData == JAVA_NULL || width <= 0 || height <= 0) {
        return;
    }
    px = (JAVA_INT*) (*(JAVA_ARRAY) rgbData).data;
    tmp = cn1SurfaceFromArgb(px + offset, width, height);
    cairo_save(g->cr);
    cairo_identity_matrix(g->cr);
    cairo_rectangle(g->cr, g->clipX, g->clipY, g->clipW, g->clipH);
    cairo_clip(g->cr);
    cairo_set_matrix(g->cr, &g->transform);
    cairo_set_source_surface(g->cr, tmp, x, y);
    cairo_paint_with_alpha(g->cr, g->alpha / 255.0);
    cairo_restore(g->cr);
    cairo_surface_destroy(tmp);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_encodeArgbToPng___int_1ARRAY_int_int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) {
    JAVA_INT* px;
    cairo_surface_t* s;
    unsigned char* data = 0;
    int len = 0;
    JAVA_OBJECT result;
    if (argb == JAVA_NULL || width <= 0 || height <= 0) {
        return JAVA_NULL;
    }
    px = (JAVA_INT*) (*(JAVA_ARRAY) argb).data;
    s = cn1SurfaceFromArgb(px, width, height);
    if (!cn1LinuxSurfaceToPng(s, &data, &len)) {
        cairo_surface_destroy(s);
        return JAVA_NULL;
    }
    cairo_surface_destroy(s);
    result = cn1LinuxNewByteArray(threadStateData, data, len);
    free(data);
    return result;
}
