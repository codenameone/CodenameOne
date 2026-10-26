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
#ifndef CN1_LINUX_RASTER_H
#define CN1_LINUX_RASTER_H

/*
 * The embedded target's 2D renderer: the part of Cairo's API the port's drawing
 * units call, implemented in cn1_linux_raster.c with nothing under it but libc,
 * libm and (for writing PNG) libpng.
 *
 * Why not Cairo itself: a distribution's libcairo is built for a desktop and
 * links X11, xcb, epoxy and pixman, and Pango, HarfBuzz, FontConfig, GdkPixbuf
 * and GLib/GIO came with it. That was 23 MB of shared libraries mapped by an
 * application that draws into a framebuffer, read from storage on every cold
 * start, for the fifty-odd functions listed here.
 *
 * The real names are macros over cn1r_ symbols. A library the application loads
 * later (a GStreamer plugin, say) may bring the real libcairo into the process,
 * and symbols of our own under Cairo's names would then be bound in its place.
 *
 * Semantics follow Cairo's where the port relies on them: ARGB32 is premultiplied
 * and native endian, a path is held in device space as it is built (so a save,
 * transform and restore around cairo_arc shapes the arc), the clip, matrix,
 * source and operator are saved and restored, and fill, stroke and clip consume
 * the path. What the port never asks for is not there: other formats, patterns
 * other than a colour or a surface, dashes, line caps other than butt, joins
 * other than miter.
 */

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum { CAIRO_FORMAT_ARGB32 = 0 } cairo_format_t;

typedef enum {
    CAIRO_STATUS_SUCCESS = 0,
    CAIRO_STATUS_NO_MEMORY = 1,
    CAIRO_STATUS_INVALID_MATRIX = 5,
    CAIRO_STATUS_WRITE_ERROR = 11
} cairo_status_t;

typedef enum {
    CAIRO_OPERATOR_CLEAR = 0,
    CAIRO_OPERATOR_SOURCE = 1,
    CAIRO_OPERATOR_OVER = 2
} cairo_operator_t;

typedef enum {
    CAIRO_FILL_RULE_WINDING = 0,
    CAIRO_FILL_RULE_EVEN_ODD = 1
} cairo_fill_rule_t;

typedef enum {
    CAIRO_FILTER_FAST = 0,
    CAIRO_FILTER_GOOD = 1,
    CAIRO_FILTER_BEST = 2,
    CAIRO_FILTER_NEAREST = 3,
    CAIRO_FILTER_BILINEAR = 4
} cairo_filter_t;

typedef struct {
    double xx, yx, xy, yy, x0, y0;
} cairo_matrix_t;

typedef struct cn1r_surface cairo_surface_t;
typedef struct cn1r_context cairo_t;
typedef struct cn1r_pattern cairo_pattern_t;
typedef cairo_status_t (*cairo_write_func_t)(void* closure, const unsigned char* data, unsigned int length);

#define cairo_image_surface_create cn1r_image_surface_create
#define cairo_image_surface_get_data cn1r_image_surface_get_data
#define cairo_image_surface_get_stride cn1r_image_surface_get_stride
#define cairo_image_surface_get_width cn1r_image_surface_get_width
#define cairo_image_surface_get_height cn1r_image_surface_get_height
#define cairo_surface_destroy cn1r_surface_destroy
#define cairo_surface_flush cn1r_surface_flush
#define cairo_surface_mark_dirty cn1r_surface_mark_dirty
#define cairo_surface_status cn1r_surface_status
#define cairo_surface_write_to_png cn1r_surface_write_to_png
#define cairo_surface_write_to_png_stream cn1r_surface_write_to_png_stream
#define cairo_create cn1r_create
#define cairo_destroy cn1r_destroy
#define cairo_save cn1r_save
#define cairo_restore cn1r_restore
#define cairo_set_source_rgb cn1r_set_source_rgb
#define cairo_set_source_rgba cn1r_set_source_rgba
#define cairo_set_source_surface cn1r_set_source_surface
#define cairo_get_source cn1r_get_source
#define cairo_pattern_set_filter cn1r_pattern_set_filter
#define cairo_set_operator cn1r_set_operator
#define cairo_set_line_width cn1r_set_line_width
#define cairo_set_fill_rule cn1r_set_fill_rule
#define cairo_paint cn1r_paint
#define cairo_paint_with_alpha cn1r_paint_with_alpha
#define cairo_fill cn1r_fill
#define cairo_stroke cn1r_stroke
#define cairo_clip cn1r_clip
#define cairo_new_path cn1r_new_path
#define cairo_new_sub_path cn1r_new_sub_path
#define cairo_move_to cn1r_move_to
#define cairo_line_to cn1r_line_to
#define cairo_curve_to cn1r_curve_to
#define cairo_arc cn1r_arc
#define cairo_arc_negative cn1r_arc_negative
#define cairo_rectangle cn1r_rectangle
#define cairo_close_path cn1r_close_path
#define cairo_set_matrix cn1r_set_matrix
#define cairo_get_matrix cn1r_get_matrix
#define cairo_identity_matrix cn1r_identity_matrix
#define cairo_translate cn1r_translate
#define cairo_scale cn1r_scale
#define cairo_matrix_init cn1r_matrix_init
#define cairo_matrix_init_identity cn1r_matrix_init_identity
#define cairo_matrix_transform_point cn1r_matrix_transform_point

cairo_surface_t* cn1r_image_surface_create(cairo_format_t format, int width, int height);
unsigned char* cn1r_image_surface_get_data(cairo_surface_t* surface);
int cn1r_image_surface_get_stride(cairo_surface_t* surface);
int cn1r_image_surface_get_width(cairo_surface_t* surface);
int cn1r_image_surface_get_height(cairo_surface_t* surface);
void cn1r_surface_destroy(cairo_surface_t* surface);
void cn1r_surface_flush(cairo_surface_t* surface);
void cn1r_surface_mark_dirty(cairo_surface_t* surface);
cairo_status_t cn1r_surface_status(cairo_surface_t* surface);
cairo_status_t cn1r_surface_write_to_png(cairo_surface_t* surface, const char* filename);
cairo_status_t cn1r_surface_write_to_png_stream(cairo_surface_t* surface, cairo_write_func_t write, void* closure);

cairo_t* cn1r_create(cairo_surface_t* target);
void cn1r_destroy(cairo_t* cr);
void cn1r_save(cairo_t* cr);
void cn1r_restore(cairo_t* cr);
void cn1r_set_source_rgb(cairo_t* cr, double red, double green, double blue);
void cn1r_set_source_rgba(cairo_t* cr, double red, double green, double blue, double alpha);
void cn1r_set_source_surface(cairo_t* cr, cairo_surface_t* surface, double x, double y);
cairo_pattern_t* cn1r_get_source(cairo_t* cr);
void cn1r_pattern_set_filter(cairo_pattern_t* pattern, cairo_filter_t filter);
void cn1r_set_operator(cairo_t* cr, cairo_operator_t op);
void cn1r_set_line_width(cairo_t* cr, double width);
void cn1r_set_fill_rule(cairo_t* cr, cairo_fill_rule_t rule);
void cn1r_paint(cairo_t* cr);
void cn1r_paint_with_alpha(cairo_t* cr, double alpha);
void cn1r_fill(cairo_t* cr);
void cn1r_stroke(cairo_t* cr);
void cn1r_clip(cairo_t* cr);
void cn1r_new_path(cairo_t* cr);
void cn1r_new_sub_path(cairo_t* cr);
void cn1r_move_to(cairo_t* cr, double x, double y);
void cn1r_line_to(cairo_t* cr, double x, double y);
void cn1r_curve_to(cairo_t* cr, double x1, double y1, double x2, double y2, double x3, double y3);
void cn1r_arc(cairo_t* cr, double xc, double yc, double radius, double angle1, double angle2);
void cn1r_arc_negative(cairo_t* cr, double xc, double yc, double radius, double angle1, double angle2);
void cn1r_rectangle(cairo_t* cr, double x, double y, double width, double height);
void cn1r_close_path(cairo_t* cr);
void cn1r_set_matrix(cairo_t* cr, const cairo_matrix_t* matrix);
void cn1r_get_matrix(cairo_t* cr, cairo_matrix_t* matrix);
void cn1r_identity_matrix(cairo_t* cr);
void cn1r_translate(cairo_t* cr, double tx, double ty);
void cn1r_scale(cairo_t* cr, double sx, double sy);
void cn1r_matrix_init(cairo_matrix_t* matrix, double xx, double yx, double xy, double yy, double x0, double y0);
void cn1r_matrix_init_identity(cairo_matrix_t* matrix);
void cn1r_matrix_transform_point(const cairo_matrix_t* matrix, double* x, double* y);

/*
 * Not Cairo: composites the current source through an 8-bit coverage mask whose
 * top-left pixel lands on device pixel (x, y), under the current clip and
 * operator. The text unit draws its glyphs with it.
 */
void cn1r_mask_a8(cairo_t* cr, const unsigned char* mask, int pitch, int width, int height, int x, int y);

#ifdef __cplusplus
}
#endif

#endif /* CN1_LINUX_RASTER_H */
