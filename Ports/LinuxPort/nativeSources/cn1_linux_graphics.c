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
 * Cairo 2D drawing for the native Codename One Linux port. Every primitive draws
 * into a CN1Graphics' backing image surface. Pen state (colour / alpha / clip /
 * affine transform) lives on the CN1Graphics; each primitive saves the Cairo
 * state, applies the screen-space rect clip (set under the identity CTM so it
 * stays in device space), installs the current affine, sets the source from the
 * colour+alpha, draws and restores. A shape clip is reduced to its bounding box
 * for this first cut (the curved-clip precise path is a later refinement).
 *
 * NOTE: implemented against Cairo but not yet compiled/run on a Linux/GTK host.
 */

#include "cn1_linux_gfx.h"
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define CN1G(p) ((CN1Graphics*) (intptr_t) (p))

void cn1LinuxApplySource(CN1Graphics* g) {
    double r = ((g->color >> 16) & 0xff) / 255.0;
    double gg = ((g->color >> 8) & 0xff) / 255.0;
    double b = (g->color & 0xff) / 255.0;
    double a = g->alpha / 255.0;
    cairo_set_source_rgba(g->cr, r, gg, b, a);
}

/* Saves state, applies the device-space rect clip and the current affine, sets
 * the source. Pair with cn1End. */
static void cn1Begin(CN1Graphics* g) {
    cairo_save(g->cr);
    cairo_identity_matrix(g->cr);
    cairo_rectangle(g->cr, g->clipX, g->clipY, g->clipW, g->clipH);
    cairo_clip(g->cr);
    cairo_set_matrix(g->cr, &g->transform);
    cn1LinuxApplySource(g);
}

static void cn1End(CN1Graphics* g) {
    cairo_restore(g->cr);
}

/* --------------------------------------------------------- graphics state */

JAVA_INT com_codename1_impl_linux_LinuxNative_getColor___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics) {
    return CN1G(graphics)->color;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setColor___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT rgb) {
    CN1G(graphics)->color = rgb & 0xffffff;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setAlpha___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT alpha) {
    CN1G(graphics)->alpha = alpha & 0xff;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getAlpha___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics) {
    return CN1G(graphics)->alpha;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setNativeFont___long_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_LONG font) {
    CN1G(graphics)->font = (CN1Font*) (intptr_t) font;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getClipX___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics) {
    return CN1G(graphics)->clipX;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getClipY___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics) {
    return CN1G(graphics)->clipY;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getClipWidth___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics) {
    return CN1G(graphics)->clipW;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getClipHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics) {
    return CN1G(graphics)->clipH;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setClip___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = CN1G(graphics);
    g->clipX = x;
    g->clipY = y;
    g->clipW = width;
    g->clipH = height;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_clipRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    /* Intersect with the existing clip (CN1 clipRect narrows). */
    CN1Graphics* g = CN1G(graphics);
    int x2 = x + width, y2 = y + height;
    int cx2 = g->clipX + g->clipW, cy2 = g->clipY + g->clipH;
    int nx = x > g->clipX ? x : g->clipX;
    int ny = y > g->clipY ? y : g->clipY;
    int nx2 = x2 < cx2 ? x2 : cx2;
    int ny2 = y2 < cy2 ? y2 : cy2;
    g->clipX = nx;
    g->clipY = ny;
    g->clipW = nx2 > nx ? nx2 - nx : 0;
    g->clipH = ny2 > ny ? ny2 - ny : 0;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setClipShape___long_float_1ARRAY_int_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT coords, JAVA_OBJECT types, JAVA_INT typeCount, JAVA_INT windingRule) {
    /* Reduce the flattened path to its bounding box (first cut). */
    CN1Graphics* g = CN1G(graphics);
    float* c;
    int clen;
    int i;
    float minX, minY, maxX, maxY;
    (void) types;
    (void) typeCount;
    (void) windingRule;
    if (coords == JAVA_NULL) {
        return;
    }
    c = (float*) (*(JAVA_ARRAY) coords).data;
    clen = (int) (*(JAVA_ARRAY) coords).length;
    if (clen < 2) {
        return;
    }
    minX = maxX = c[0];
    minY = maxY = c[1];
    for (i = 0; i + 1 < clen; i += 2) {
        if (c[i] < minX) minX = c[i];
        if (c[i] > maxX) maxX = c[i];
        if (c[i + 1] < minY) minY = c[i + 1];
        if (c[i + 1] > maxY) maxY = c[i + 1];
    }
    g->clipX = (int) floorf(minX);
    g->clipY = (int) floorf(minY);
    g->clipW = (int) ceilf(maxX - minX);
    g->clipH = (int) ceilf(maxY - minY);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setTransform___long_float_float_float_float_float_float(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_FLOAT m00, JAVA_FLOAT m10, JAVA_FLOAT m01, JAVA_FLOAT m11, JAVA_FLOAT m02, JAVA_FLOAT m12) {
    /* CN1 affine (m00,m10,m01,m11,m02,m12) maps to cairo_matrix (xx,yx,xy,yy,x0,y0). */
    cairo_matrix_init(&CN1G(graphics)->transform, m00, m10, m01, m11, m02, m12);
}

/* --------------------------------------------------------------- drawing */

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawLine___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x1, JAVA_INT y1, JAVA_INT x2, JAVA_INT y2) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cairo_set_line_width(g->cr, 1.0);
    cairo_move_to(g->cr, x1 + 0.5, y1 + 0.5);
    cairo_line_to(g->cr, x2 + 0.5, y2 + 0.5);
    cairo_stroke(g->cr);
    cn1End(g);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fillRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cairo_rectangle(g->cr, x, y, width, height);
    cairo_fill(g->cr);
    cn1End(g);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cairo_set_line_width(g->cr, 1.0);
    cairo_rectangle(g->cr, x + 0.5, y + 0.5, width, height);
    cairo_stroke(g->cr);
    cn1End(g);
}

static void cn1RoundRectPath(cairo_t* cr, double x, double y, double w, double h, double aw, double ah) {
    double rx = aw / 2.0, ry = ah / 2.0;
    if (rx > w / 2.0) rx = w / 2.0;
    if (ry > h / 2.0) ry = h / 2.0;
    /* Approximate the elliptical corners with a uniform radius (min of rx,ry). */
    double r = rx < ry ? rx : ry;
    cairo_new_sub_path(cr);
    cairo_arc(cr, x + w - r, y + r, r, -M_PI / 2, 0);
    cairo_arc(cr, x + w - r, y + h - r, r, 0, M_PI / 2);
    cairo_arc(cr, x + r, y + h - r, r, M_PI / 2, M_PI);
    cairo_arc(cr, x + r, y + r, r, M_PI, 3 * M_PI / 2);
    cairo_close_path(cr);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawRoundRect___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT arcWidth, JAVA_INT arcHeight) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cairo_set_line_width(g->cr, 1.0);
    cn1RoundRectPath(g->cr, x + 0.5, y + 0.5, width, height, arcWidth, arcHeight);
    cairo_stroke(g->cr);
    cn1End(g);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fillRoundRect___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT arcWidth, JAVA_INT arcHeight) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cn1RoundRectPath(g->cr, x, y, width, height, arcWidth, arcHeight);
    cairo_fill(g->cr);
    cn1End(g);
}

/* CN1 angles: degrees, 0 == 3 o'clock, counter-clockwise positive. Cairo:
 * radians, clockwise positive (y down). Convert with negation and use a unit
 * circle scaled to the ellipse bounds. */
static void cn1ArcPath(cairo_t* cr, double x, double y, double w, double h, double startDeg, double sweepDeg, int pie) {
    /* A zero (or negative) width/height arc is degenerate -- nothing visible to
     * draw. Critically, cairo_scale(cr, 0, ...) below would make the CTM non-
     * invertible, which puts the cairo_t into a sticky CAIRO_STATUS_INVALID_MATRIX
     * error that cairo_restore does NOT clear: every subsequent operation on this
     * context (the shared back buffer) then silently no-ops, freezing all further
     * rendering. DrawArc sweeps width/height down to exactly 0, so guard here.
     * Matches the Direct2D port, which renders nothing for a degenerate ellipse. */
    if (w <= 0 || h <= 0) {
        return;
    }
    double cx = x + w / 2.0, cy = y + h / 2.0;
    double a0 = -startDeg * M_PI / 180.0;
    double a1 = -(startDeg + sweepDeg) * M_PI / 180.0;
    cairo_save(cr);
    cairo_translate(cr, cx, cy);
    cairo_scale(cr, w / 2.0, h / 2.0);
    if (pie) {
        cairo_move_to(cr, 0, 0);
        cairo_line_to(cr, cos(a0), sin(a0));
    }
    if (sweepDeg >= 0) {
        cairo_arc_negative(cr, 0, 0, 1.0, a0, a1);
    } else {
        cairo_arc(cr, 0, 0, 1.0, a0, a1);
    }
    if (pie) {
        cairo_close_path(cr);
    }
    cairo_restore(cr);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fillArc___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT startAngle, JAVA_INT arcAngle) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cn1ArcPath(g->cr, x, y, width, height, startAngle, arcAngle, 1);
    cairo_fill(g->cr);
    cn1End(g);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawArc___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT startAngle, JAVA_INT arcAngle) {
    CN1Graphics* g = CN1G(graphics);
    cn1Begin(g);
    cairo_set_line_width(g->cr, 1.0);
    cn1ArcPath(g->cr, x, y, width, height, startAngle, arcAngle, 0);
    cairo_stroke(g->cr);
    cn1End(g);
}

/* Walks the (coords, types) flattened path into the current Cairo path. Segment
 * ops: 0=move(2 floats),1=line(2),2=quad(4),3=cubic(6),4=close. Cairo has no
 * quadratic, so a quad is promoted to the equivalent cubic. */
static void cn1BuildPath(cairo_t* cr, float* coords, int* types, int typeCount) {
    int ci = 0;
    int t;
    double cx = 0, cy = 0; /* current point for quad->cubic promotion */
    for (t = 0; t < typeCount; t++) {
        switch (types[t]) {
            case 0:
                cx = coords[ci]; cy = coords[ci + 1];
                cairo_move_to(cr, cx, cy);
                ci += 2;
                break;
            case 1:
                cx = coords[ci]; cy = coords[ci + 1];
                cairo_line_to(cr, cx, cy);
                ci += 2;
                break;
            case 2: {
                double qx = coords[ci], qy = coords[ci + 1];
                double ex = coords[ci + 2], ey = coords[ci + 3];
                double c1x = cx + 2.0 / 3.0 * (qx - cx);
                double c1y = cy + 2.0 / 3.0 * (qy - cy);
                double c2x = ex + 2.0 / 3.0 * (qx - ex);
                double c2y = ey + 2.0 / 3.0 * (qy - ey);
                cairo_curve_to(cr, c1x, c1y, c2x, c2y, ex, ey);
                cx = ex; cy = ey;
                ci += 4;
                break;
            }
            case 3:
                cairo_curve_to(cr, coords[ci], coords[ci + 1], coords[ci + 2], coords[ci + 3], coords[ci + 4], coords[ci + 5]);
                cx = coords[ci + 4]; cy = coords[ci + 5];
                ci += 6;
                break;
            case 4:
                cairo_close_path(cr);
                break;
            default:
                break;
        }
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_fillShape___long_float_1ARRAY_int_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT coords, JAVA_OBJECT types, JAVA_INT typeCount, JAVA_INT windingRule) {
    CN1Graphics* g = CN1G(graphics);
    if (coords == JAVA_NULL || types == JAVA_NULL) {
        return;
    }
    cn1Begin(g);
    cairo_set_fill_rule(g->cr, windingRule == 0 ? CAIRO_FILL_RULE_EVEN_ODD : CAIRO_FILL_RULE_WINDING);
    cn1BuildPath(g->cr, (float*) (*(JAVA_ARRAY) coords).data, (int*) (*(JAVA_ARRAY) types).data, typeCount);
    cairo_fill(g->cr);
    cn1End(g);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_drawShape___long_float_1ARRAY_int_1ARRAY_int_int_float(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT coords, JAVA_OBJECT types, JAVA_INT typeCount, JAVA_INT windingRule, JAVA_FLOAT lineWidth) {
    CN1Graphics* g = CN1G(graphics);
    (void) windingRule;
    if (coords == JAVA_NULL || types == JAVA_NULL) {
        return;
    }
    cn1Begin(g);
    cairo_set_line_width(g->cr, lineWidth > 0 ? lineWidth : 1.0);
    cn1BuildPath(g->cr, (float*) (*(JAVA_ARRAY) coords).data, (int*) (*(JAVA_ARRAY) types).data, typeCount);
    cairo_stroke(g->cr);
    cn1End(g);
}

/* drawImage / drawImageScaled draw a CN1Image's surface; defined in cn1_linux_image.c
 * where the CN1Image struct lifecycle lives, to keep image ownership in one unit. */
