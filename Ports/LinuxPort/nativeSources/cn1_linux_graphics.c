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
#include <stdlib.h>
#include <string.h>

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

static void cn1BuildPath(cairo_t* cr, float* coords, int* types, int typeCount);

void cn1LinuxFreeClipShape(CN1Graphics* g) {
    free(g->clipCoords);
    g->clipCoords = 0;
    free(g->clipTypes);
    g->clipTypes = 0;
    g->clipTypeCount = 0;
}

/* Applies the current clip to g->cr (assumes the caller already cairo_save'd and
 * will set the drawing matrix afterwards). The clip is applied under identity
 * (rect) or the frozen clip-set transform (shape) so it stays where it was set
 * rather than following the drawing transform -- this is what makes a clip
 * survive a later rotate/scale and what makes setClip(Shape) clip to the real
 * shape rather than its bounding box. Shared with the image/text draw paths. */
void cn1LinuxApplyClip(CN1Graphics* g) {
    if (!g->clipIsRect && g->clipCoords != 0 && g->clipTypeCount > 0) {
        cairo_set_matrix(g->cr, &g->clipTransform);
        cairo_new_path(g->cr);
        cn1BuildPath(g->cr, g->clipCoords, g->clipTypes, g->clipTypeCount);
        cairo_clip(g->cr);
    } else {
        int cx = g->clipX, cy = g->clipY, cw = g->clipW, ch = g->clipH;
        /* Issue #5273: confine a screen clip to the current flush region. On a
         * partial repaint paintDirty pushes the dirty sub-region via setFlushRect
         * before the component paints; a clip the component then sets can extend
         * past that sub-region, and because the window draws into a PERSISTENT
         * Cairo surface the escaping fill would overwrite -- and leave stale --
         * pixels outside the flushed area until a full repaint (the same defect
         * the iOS Metal backend had). Clamp so the fill can never touch them.
         * Window target only (mutable images keep flushW == 0 and are
         * unaffected); skipped when no flush rect is set so a clip is never
         * collapsed to nothing. */
        if (g->isWindowTarget && g->clipH > 300) {
            /* CN1DIAG #5273: temporary -- reveals whether the flush region is the
             * dirty sub-region or the full screen when the escaping clip applies. */
            fprintf(stderr, "CN1DIAG applyClip win clip=(%d,%d,%d,%d) flush=(%d,%d,%d,%d)\n",
                    g->clipX, g->clipY, g->clipW, g->clipH, g->flushX, g->flushY, g->flushW, g->flushH);
            fflush(stderr);
        }
        if (g->isWindowTarget && g->flushW > 0 && g->flushH > 0) {
            int cx2 = cx + cw, cy2 = cy + ch;
            int fx2 = g->flushX + g->flushW, fy2 = g->flushY + g->flushH;
            if (cx < g->flushX) { cx = g->flushX; }
            if (cy < g->flushY) { cy = g->flushY; }
            if (cx2 > fx2) { cx2 = fx2; }
            if (cy2 > fy2) { cy2 = fy2; }
            cw = cx2 - cx;
            ch = cy2 - cy;
            if (cw < 0) { cw = 0; }
            if (ch < 0) { ch = 0; }
        }
        cairo_identity_matrix(g->cr);
        cairo_rectangle(g->cr, cx, cy, cw, ch);
        cairo_clip(g->cr);
    }
}

/* Saves state, applies the clip and the current affine, sets the source. Pair
 * with cn1End. */
static void cn1Begin(CN1Graphics* g) {
    cairo_save(g->cr);
    cn1LinuxApplyClip(g);
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
    g->clipIsRect = 1;
    cn1LinuxFreeClipShape(g);
}

/* Issue #5273: record the current paintDirty flush region (screen space) so a
 * rectangular screen clip set while a component paints is confined to it in
 * cn1LinuxApplyClip. width/height == 0 disables the clamp (full repaint / no
 * partial flush). Only meaningful on the window graphics; mutable-image
 * graphics never receive this call. */
JAVA_VOID com_codename1_impl_linux_LinuxNative_setFlushRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = CN1G(graphics);
    g->flushX = x;
    g->flushY = y;
    g->flushW = width;
    g->flushH = height;
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
    g->clipIsRect = 1;
    cn1LinuxFreeClipShape(g);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_setClipShape___long_float_1ARRAY_int_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT coords, JAVA_OBJECT types, JAVA_INT typeCount, JAVA_INT windingRule) {
    /* Store the real flattened path so cn1Begin can clip to the exact shape (a
     * circle, rounded rect, rotated rect ...) rather than its bounding box. The
     * path is kept in its raw coordinate space and clipTransform freezes the
     * world transform that was active now, so the clip lands where drawShape would
     * draw the same path. Mirrors the Windows port's clip-geometry model. */
    CN1Graphics* g = CN1G(graphics);
    float* c;
    int* t;
    int clen, tlen, tcount, i;
    double minX, minY, maxX, maxY;
    (void) windingRule;
    cn1LinuxFreeClipShape(g);
    if (coords == JAVA_NULL || types == JAVA_NULL || typeCount <= 0) {
        g->clipIsRect = 1;
        return;
    }
    c = (float*) (*(JAVA_ARRAY) coords).data;
    clen = (int) (*(JAVA_ARRAY) coords).length;
    t = (int*) (*(JAVA_ARRAY) types).data;
    tlen = (int) (*(JAVA_ARRAY) types).length;
    if (clen < 2) {
        g->clipIsRect = 1;
        return;
    }
    g->clipCoords = (float*) malloc(sizeof(float) * (size_t) clen);
    memcpy(g->clipCoords, c, sizeof(float) * (size_t) clen);
    tcount = typeCount < tlen ? typeCount : tlen;
    g->clipTypes = (int*) malloc(sizeof(int) * (size_t) tcount);
    memcpy(g->clipTypes, t, sizeof(int) * (size_t) tcount);
    g->clipTypeCount = tcount;
    g->clipTransform = g->transform;
    g->clipIsRect = 0;
    /* Keep clipX/Y/W/H as the shape's screen-space bounding box (each raw vertex
     * pushed through the frozen transform) so getClip* stays meaningful. */
    minX = minY = 1e30;
    maxX = maxY = -1e30;
    for (i = 0; i + 1 < clen; i += 2) {
        double dx = c[i], dy = c[i + 1];
        cairo_matrix_transform_point(&g->clipTransform, &dx, &dy);
        if (dx < minX) minX = dx;
        if (dx > maxX) maxX = dx;
        if (dy < minY) minY = dy;
        if (dy > maxY) maxY = dy;
    }
    g->clipX = (int) floor(minX);
    g->clipY = (int) floor(minY);
    g->clipW = (int) ceil(maxX - minX);
    g->clipH = (int) ceil(maxY - minY);
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
