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
 * Image decoding for the embedded target: PNG through libpng, JPEG through
 * libjpeg, straight into the premultiplied ARGB32 surfaces the renderer draws.
 * The desktop target decodes through GdkPixbuf instead (cn1_linux_image.c),
 * which also reads GIF and BMP; here those two answer NULL, which the port
 * reports as an image that could not be created.
 */
#ifdef CN1_LINUX_EMBEDDED

#include "cn1_linux_gfx.h"
#include <png.h>
#include <setjmp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jpeglib.h>

static cairo_surface_t* cn1DecodePng(const unsigned char* bytes, size_t length) {
    png_image image;
    cairo_surface_t* s;
    uint32_t* px;
    size_t i, count;
    memset(&image, 0, sizeof(image));
    image.version = PNG_IMAGE_VERSION;
    if (!png_image_begin_read_from_memory(&image, bytes, length)) {
        return 0;
    }
    /* BGRA in memory is a native-endian 0xAARRGGBB word on a little-endian CPU;
     * ARGB in memory is the same word on a big-endian one. */
#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
    image.format = PNG_FORMAT_BGRA;
#else
    image.format = PNG_FORMAT_ARGB;
#endif
    s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, (int) image.width, (int) image.height);
    if (s == 0) {
        png_image_free(&image);
        return 0;
    }
    px = (uint32_t*) cairo_image_surface_get_data(s);
    if (!png_image_finish_read(&image, 0, px, cairo_image_surface_get_stride(s), 0)) {
        png_image_free(&image);
        cairo_surface_destroy(s);
        return 0;
    }
    count = (size_t) image.width * image.height;
    for (i = 0; i < count; i++) {
        uint32_t p = px[i];
        uint32_t a = p >> 24;
        if (a != 255) {
            uint32_t r = ((p >> 16) & 0xff) * a / 255;
            uint32_t g = ((p >> 8) & 0xff) * a / 255;
            uint32_t b = (p & 0xff) * a / 255;
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
    return s;
}

typedef struct {
    struct jpeg_error_mgr base;
    jmp_buf escape;
} CN1JpegError;

/* libjpeg's default handler calls exit(); a damaged picture must not end the
 * application. */
static void cn1JpegFail(j_common_ptr info) {
    longjmp(((CN1JpegError*) info->err)->escape, 1);
}

static void cn1JpegQuiet(j_common_ptr info, int level) {
    (void) info;
    (void) level;
}

static cairo_surface_t* cn1DecodeJpeg(const unsigned char* bytes, size_t length) {
    struct jpeg_decompress_struct info;
    CN1JpegError err;
    cairo_surface_t* volatile s = 0;
    unsigned char* volatile row = 0;
    info.err = jpeg_std_error(&err.base);
    err.base.error_exit = cn1JpegFail;
    err.base.emit_message = cn1JpegQuiet;
    if (setjmp(err.escape)) {
        jpeg_destroy_decompress(&info);
        free(row);
        if (s != 0) {
            cairo_surface_destroy(s);
        }
        return 0;
    }
    jpeg_create_decompress(&info);
    jpeg_mem_src(&info, (unsigned char*) bytes, (unsigned long) length);
    jpeg_read_header(&info, TRUE);
    info.out_color_space = JCS_RGB;
    jpeg_start_decompress(&info);
    s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, (int) info.output_width, (int) info.output_height);
    row = (unsigned char*) malloc((size_t) info.output_width * 3);
    if (s == 0 || row == 0) {
        longjmp(err.escape, 1);
    }
    {
        unsigned char* base = cairo_image_surface_get_data(s);
        int stride = cairo_image_surface_get_stride(s);
        while (info.output_scanline < info.output_height) {
            JSAMPROW rows[1];
            uint32_t* out = (uint32_t*) (base + (size_t) info.output_scanline * (size_t) stride);
            unsigned x;
            rows[0] = row;
            jpeg_read_scanlines(&info, rows, 1);
            for (x = 0; x < info.output_width; x++) {
                out[x] = 0xff000000u | ((uint32_t) row[x * 3] << 16) | ((uint32_t) row[x * 3 + 1] << 8) | row[x * 3 + 2];
            }
        }
    }
    jpeg_finish_decompress(&info);
    jpeg_destroy_decompress(&info);
    free(row);
    return s;
}

cairo_surface_t* cn1LinuxDecodeImage(const unsigned char* bytes, size_t length) {
    static const unsigned char pngMagic[8] = { 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a };
    if (bytes == 0 || length < 8) {
        return 0;
    }
    if (memcmp(bytes, pngMagic, 8) == 0) {
        return cn1DecodePng(bytes, length);
    }
    if (bytes[0] == 0xff && bytes[1] == 0xd8) {
        return cn1DecodeJpeg(bytes, length);
    }
    return 0;
}

#endif /* CN1_LINUX_EMBEDDED */
