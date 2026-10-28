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
 * Holds the embedded port's GPU renderer to its software renderer.
 *
 * Draws the same scenes twice -- into a CPU surface with the software renderer,
 * and into the GPU window -- and compares them pixel by pixel. Every operation
 * the drawing units use is covered: rectangles and paths with each operator,
 * text-sized and screen-sized masks, images unscaled, scaled and rotated, clips
 * by rectangle and by shape, and the window drawn into itself. Run by
 * check-gpu.sh on Mesa's llvmpipe, so it needs no GPU.
 *
 *   gpu_check [out-dir]    exit 0 when every scene matches; writes the
 *                           differing scenes' two images to out-dir
 *
 * The two renderers are expected to differ only by rounding: blending in 8-bit
 * fixed point on one side and in floating point on the other. A pixel counts as
 * different when a channel is off by more than CHANNEL_TOLERANCE.
 */
#include "cn1_linux_gpu.h"
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define W 320
#define H 240
#define CHANNEL_TOLERANCE 3
/* Share of pixels allowed past the tolerance: the edges of scaled images,
 * where a bilinear sample lands differently. */
#define MAX_DIFFERENT 0.002

typedef void (*Scene)(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target);

static void fills(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    (void) image;
    cairo_set_source_rgb(cr, 0.1, 0.2, 0.3);
    cairo_paint(cr);
    cairo_set_source_rgba(cr, 1, 0.5, 0, 1);
    cairo_rectangle(cr, 10, 10, 100, 50);
    cairo_fill(cr);
    cairo_set_source_rgba(cr, 0, 0.8, 0.4, 0.5);
    cairo_rectangle(cr, 60, 30, 100, 80);
    cairo_fill(cr);
    cairo_set_source_rgba(cr, 1, 1, 1, 0.7);
    cairo_rectangle(cr, 200.25, 20.5, 60.5, 30.75);
    cairo_fill(cr);
}

static void paths(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    (void) image;
    cairo_set_source_rgb(cr, 0.06, 0.09, 0.12);
    cairo_paint(cr);
    cairo_set_source_rgb(cr, 0.2, 0.67, 1);
    cairo_arc(cr, 160, 120, 100, 0, 2 * M_PI);
    cairo_fill(cr);
    cairo_set_source_rgb(cr, 0.06, 0.09, 0.12);
    cairo_arc(cr, 160, 120, 76, 0, 2 * M_PI);
    cairo_fill(cr);
    cairo_set_source_rgba(cr, 1, 1, 1, 1);
    cairo_set_line_width(cr, 1.5);
    cairo_move_to(cr, 160, 120);
    cairo_line_to(cr, 250, 60);
    cairo_stroke(cr);
    cairo_set_line_width(cr, 6);
    cairo_set_source_rgba(cr, 1, 0.3, 0.3, 0.8);
    cairo_move_to(cr, 20, 220);
    cairo_curve_to(cr, 80, 120, 200, 260, 300, 160);
    cairo_stroke(cr);
}

static void glyphs(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    unsigned char mask[16 * 20];
    int x, y, i;
    (void) image;
    cairo_set_source_rgb(cr, 0.9, 0.9, 0.9);
    cairo_paint(cr);
    /* Glyph-like masks: many small ones, as a line of text draws. */
    for (y = 0; y < 20; y++) {
        for (x = 0; x < 16; x++) {
            int d = abs(x - 8) + abs(y - 10);
            mask[y * 16 + x] = (unsigned char) (d < 6 ? 255 : d < 9 ? (9 - d) * 80 : 0);
        }
    }
    cairo_set_source_rgb(cr, 0, 0, 0);
    for (i = 0; i < 300; i++) {
        cn1r_mask_a8(cr, mask, 16, 16, 20, (i * 17) % 300, 10 + (i / 18) * 13);
    }
    cairo_set_source_rgba(cr, 0.8, 0, 0, 0.6);
    cn1r_mask_a8(cr, mask, 16, 16, 20, -5, -5);
    cn1r_mask_a8(cr, mask, 16, 16, 20, W - 10, H - 12);
}

static void images(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    cairo_matrix_t m;
    cairo_set_source_rgb(cr, 0.2, 0.2, 0.25);
    cairo_paint(cr);
    cairo_set_source_surface(cr, image, 10, 10);
    cairo_paint(cr);
    cairo_set_source_surface(cr, image, 90, 10);
    cairo_paint_with_alpha(cr, 0.5);
    cairo_save(cr);
    cairo_translate(cr, 170, 10);
    cairo_scale(cr, 2, 2);
    cairo_set_source_surface(cr, image, 0, 0);
    cairo_pattern_set_filter(cairo_get_source(cr), CAIRO_FILTER_NEAREST);
    cairo_paint(cr);
    cairo_restore(cr);
    cairo_save(cr);
    cairo_matrix_init(&m, cos(0.5), sin(0.5), -sin(0.5), cos(0.5), 80, 120);
    cairo_set_matrix(cr, &m);
    cairo_set_source_surface(cr, image, 0, 0);
    cairo_pattern_set_filter(cairo_get_source(cr), CAIRO_FILTER_NEAREST);
    cairo_paint(cr);
    cairo_restore(cr);
    /* An image as the source of a shape. */
    cairo_set_source_surface(cr, image, 220, 150);
    cairo_arc(cr, 252, 182, 30, 0, 2 * M_PI);
    cairo_fill(cr);
}

static void operators(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    cairo_set_source_rgb(cr, 0.3, 0.6, 0.9);
    cairo_paint(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_rgba(cr, 1, 0, 0, 0.5);
    cairo_rectangle(cr, 20, 20, 100, 100);
    cairo_fill(cr);
    cairo_arc(cr, 220, 80, 50, 0, 2 * M_PI);
    cairo_fill(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_CLEAR);
    cairo_arc(cr, 160, 180, 40, 0, 2 * M_PI);
    cairo_fill(cr);
    cairo_rectangle(cr, 10, 200, 30, 30);
    cairo_fill(cr);
    /* With an operation alpha below 1, SOURCE and CLEAR keep part of the
     * destination: dst * (1 - alpha * coverage). */
    cairo_save(cr);
    cairo_rectangle(cr, 140, 20, 60, 60);
    cairo_clip(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_surface(cr, image, 130, 10);
    cairo_paint_with_alpha(cr, 0.4);
    cairo_restore(cr);
    cairo_save(cr);
    cairo_rectangle(cr, 230, 150, 60, 60);
    cairo_clip(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_rgba(cr, 0, 1, 0, 0.8);
    cairo_paint_with_alpha(cr, 0.5);
    cairo_set_operator(cr, CAIRO_OPERATOR_CLEAR);
    cairo_paint_with_alpha(cr, 0.3);
    cairo_restore(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
}

static void clips(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    unsigned char mask[40 * 40];
    int i;
    for (i = 0; i < 40 * 40; i++) {
        mask[i] = (unsigned char) (i * 7);
    }
    cairo_set_source_rgb(cr, 0, 0, 0);
    cairo_paint(cr);
    cairo_save(cr);
    cairo_rectangle(cr, 30, 30, 120, 90);
    cairo_clip(cr);
    cairo_set_source_rgb(cr, 0.9, 0.8, 0.1);
    cairo_paint(cr);
    cairo_restore(cr);
    cairo_save(cr);
    cairo_arc(cr, 230, 100, 70, 0, 2 * M_PI);
    cairo_clip(cr);
    cairo_set_source_rgb(cr, 0.2, 0.9, 0.5);
    cairo_rectangle(cr, 150, 20, 160, 100);
    cairo_fill(cr);
    cairo_set_source_surface(cr, image, 200, 100);
    cairo_paint(cr);
    cairo_set_source_rgb(cr, 1, 1, 1);
    cn1r_mask_a8(cr, mask, 40, 40, 40, 180, 120);
    cairo_restore(cr);
}

static void copies(cairo_t* cr, cairo_surface_t* image, cairo_surface_t* target) {
    (void) target;
    (void) image;
    cairo_set_source_rgb(cr, 0.5, 0.1, 0.1);
    cairo_paint(cr);
    cairo_set_source_rgb(cr, 0.1, 0.9, 0.1);
    cairo_arc(cr, 60, 60, 40, 0, 2 * M_PI);
    cairo_fill(cr);
    /* The target drawn into itself, as copyArea does: (20, 20) to (140, 80),
     * clipped so the source and the destination do not overlap. */
    cairo_save(cr);
    cairo_rectangle(cr, 140, 80, 80, 80);
    cairo_clip(cr);
    cairo_set_source_surface(cr, target, 120, 60);
    cairo_paint(cr);
    cairo_restore(cr);
}

static const Scene SCENES[] = { fills, paths, glyphs, images, operators, clips, copies };
static const char* const NAMES[] = { "fills", "paths", "glyphs", "images", "operators", "clips", "copies" };

static cairo_surface_t* makeImage(void) {
    cairo_surface_t* s = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, 64, 64);
    uint32_t* p = (uint32_t*) cairo_image_surface_get_data(s);
    int x, y;
    for (y = 0; y < 64; y++) {
        for (x = 0; x < 64; x++) {
            unsigned a = (unsigned) ((x + y) * 2 > 255 ? 255 : (x + y) * 2);
            unsigned r = (unsigned) (x * 4) * a / 255, g = (unsigned) (y * 4) * a / 255, b = ((x ^ y) & 8 ? 200u : 40u) * a / 255;
            p[y * 64 + x] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
    cairo_surface_mark_dirty(s);
    return s;
}

int main(int argc, char** argv) {
    const char* out = argc > 1 ? argv[1] : ".";
    cairo_surface_t* image;
    cairo_surface_t* window;
    size_t i;
    int failed = 0;
    if (cn1GpuInitOffscreen() != 0) {
        fprintf(stderr, "gpu_check: no EGL / GLES 2 display\n");
        return 2;
    }
    window = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, W, H);
    if (cn1GpuAttachWindow(window) != 0) {
        fprintf(stderr, "gpu_check: cannot render into the window\n");
        return 2;
    }
    image = makeImage();
    for (i = 0; i < sizeof(SCENES) / sizeof(SCENES[0]); i++) {
        cairo_surface_t* cpu = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, W, H);
        cairo_t* a = cairo_create(cpu);
        cairo_t* b = cairo_create(window);
        const uint32_t* pa;
        const uint32_t* pb;
        long different = 0, worst = 0, n;
        SCENES[i](a, image, cpu);
        SCENES[i](b, image, window);
        cairo_surface_flush(window);
        pa = (const uint32_t*) cairo_image_surface_get_data(cpu);
        pb = (const uint32_t*) cairo_image_surface_get_data(window);
        for (n = 0; n < (long) W * H; n++) {
            int c, off = 0;
            for (c = 0; c < 32; c += 8) {
                int d = abs((int) ((pa[n] >> c) & 0xff) - (int) ((pb[n] >> c) & 0xff));
                if (d > off) off = d;
            }
            if (off > CHANNEL_TOLERANCE) different++;
            if (off > worst) worst = off;
        }
        if (different > (long) (W * H * MAX_DIFFERENT)) {
            char path[512];
            failed = 1;
            snprintf(path, sizeof(path), "%s/%s-software.png", out, NAMES[i]);
            cairo_surface_write_to_png(cpu, path);
            snprintf(path, sizeof(path), "%s/%s-gpu.png", out, NAMES[i]);
            cairo_surface_write_to_png(window, path);
        }
        printf("%-10s %6ld pixels differ (%.3f%%), worst channel %ld  %s\n", NAMES[i], different,
                100.0 * different / (W * H), worst, different > (long) (W * H * MAX_DIFFERENT) ? "FAIL" : "ok");
        cairo_destroy(a);
        cairo_destroy(b);
        cairo_surface_destroy(cpu);
    }
    cairo_surface_destroy(image);
    return failed;
}
