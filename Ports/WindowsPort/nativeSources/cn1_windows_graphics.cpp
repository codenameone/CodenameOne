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

#ifdef _WIN32

#include "cn1_windows.h"
#include <math.h>
#include <stdlib.h>

/* C++ unit (Direct2D is C++-only); keep C linkage for the bridge + helpers. */
extern "C" {

/*
 * cn1_windows_graphics.c -- Direct2D backed implementation of the Codename One
 * Graphics primitives for the native Windows port. Every WindowsNative graphics
 * bridge call receives the graphics peer as its first JAVA_LONG argument; we
 * recover it with (CN1Graphics*)__cn1Arg1.
 *
 * Frame management: Direct2D requires BeginDraw before any draw call and EndDraw
 * to flush. We open the frame lazily on the first primitive of a paint cycle via
 * cn1WinBeginFrame; the windowing layer (cn1WinPpresent / flushGraphics) is
 * responsible for calling cn1WinEndFrame to present.
 *
 * Clip strategy: we keep the CN1 clip as a plain axis-aligned rectangle stored
 * on the peer. Rather than holding a clip pushed for the whole frame (which would
 * fight with per-primitive geometry), each primitive brackets its own draw calls
 * with PushAxisAlignedClip / PopAxisAlignedClip through the cn1WinPushClip /
 * cn1WinPopClip helpers below. This is the simplest correct approach: the clip is
 * always exactly the current g->clip* rectangle for the duration of one drawing
 * operation, and the render target is never left with a dangling pushed clip.
 */

/* ---------------------------------------------------------------- helpers */

D2D1_COLOR_F cn1WinColorF(JAVA_INT rgb, JAVA_INT alpha) {
    D2D1_COLOR_F c;
    c.r = (float)((rgb >> 16) & 0xff) / 255.0f;
    c.g = (float)((rgb >> 8) & 0xff) / 255.0f;
    c.b = (float)(rgb & 0xff) / 255.0f;
    c.a = (float)(alpha & 0xff) / 255.0f;
    return c;
}

CN1Graphics* cn1WinCreateGraphics(ID2D1RenderTarget* target) {
    CN1Graphics* g = (CN1Graphics*)malloc(sizeof(CN1Graphics));
    if (g == NULL) {
        return NULL;
    }
    g->target = target;
    g->brush = NULL;
    g->color = 0;
    g->alpha = 255;
    g->clipX = 0;
    g->clipY = 0;
    /* Default clip is the full target surface. Query its size in DIPs. */
    if (target != NULL) {
        D2D1_SIZE_F size = ID2D1RenderTarget_GetSize(target);
        g->clipW = (JAVA_INT)(size.width + 0.5f);
        g->clipH = (JAVA_INT)(size.height + 0.5f);
    } else {
        g->clipW = 0;
        g->clipH = 0;
    }
    g->clipIsRect = JAVA_TRUE;
    g->font = cn1Win.defaultFont;
    g->inFrame = JAVA_FALSE;
    g->wicBitmap = NULL;
    return g;
}

void cn1WinBeginFrame(CN1Graphics* g) {
    if (g == NULL || g->target == NULL) {
        return;
    }
    /* The window target owns any pending resize; apply it here, on the EDT,
     * before opening a frame (see cn1WinApplyPendingResize). */
    if (g == cn1Win.windowGraphics) {
        cn1WinApplyPendingResize();
    }
    if (!g->inFrame) {
        ID2D1RenderTarget_BeginDraw(g->target);
        /* Each frame starts with the identity transform; Codename One sets any
         * affine via setTransform during the frame and resets to identity when
         * done. Without this reset a transform left over from the previous frame
         * would leak into the next one. */
        D2D1_MATRIX_3X2_F idm = D2D1::Matrix3x2F::Identity();
        ID2D1RenderTarget_SetTransform(g->target, &idm);
        g->inFrame = JAVA_TRUE;
    }
}

/* Sets the 2D affine transform on the render target. The six values are the
 * Codename One affine (m00,m10,m01,m11,m02,m12): x' = m00*x + m01*y + m02,
 * y' = m10*x + m11*y + m12, mapped to D2D's row-vector 3x2 matrix. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_setTransform___long_float_float_float_float_float_float(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_FLOAT m00, JAVA_FLOAT m10, JAVA_FLOAT m01,
        JAVA_FLOAT m11, JAVA_FLOAT m02, JAVA_FLOAT m12) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) __cn1Arg1;
    if (g == NULL || g->target == NULL) {
        return;
    }
    /* D2D1::Matrix3x2F(_11,_12,_21,_22,_31,_32) maps directly from the CN1 affine. */
    D2D1_MATRIX_3X2_F m = D2D1::Matrix3x2F(m00, m10, m01, m11, m02, m12);
    ID2D1RenderTarget_SetTransform(g->target, &m);
}

void cn1WinEndFrame(CN1Graphics* g) {
    if (g == NULL || g->target == NULL) {
        return;
    }
    if (g->inFrame) {
        ID2D1RenderTarget_EndDraw(g->target, NULL, NULL);
        g->inFrame = JAVA_FALSE;
    }
}

ID2D1SolidColorBrush* cn1WinBrush(CN1Graphics* g) {
    D2D1_COLOR_F c = cn1WinColorF(g->color, g->alpha);
    if (g->brush == NULL) {
        /* Lazily create the reusable solid color brush. */
        ID2D1RenderTarget_CreateSolidColorBrush(g->target, &c, NULL, &g->brush);
        if (g->brush == NULL) {
            return NULL;
        }
    } else {
        ID2D1SolidColorBrush_SetColor(g->brush, &c);
    }
    return g->brush;
}

/*
 * Build a D2D1_RECT_F covering g's current clip rectangle and push it as an
 * axis-aligned clip. Must be paired with cn1WinPopClip after the primitive.
 */
static void cn1WinPushClip(CN1Graphics* g) {
    D2D1_RECT_F clip;
    clip.left = (FLOAT)g->clipX;
    clip.top = (FLOAT)g->clipY;
    clip.right = (FLOAT)(g->clipX + g->clipW);
    clip.bottom = (FLOAT)(g->clipY + g->clipH);
    ID2D1RenderTarget_PushAxisAlignedClip(g->target, &clip, D2D1_ANTIALIAS_MODE_PER_PRIMITIVE);
}

static void cn1WinPopClip(CN1Graphics* g) {
    ID2D1RenderTarget_PopAxisAlignedClip(g->target);
}

/* Convert CN1/AWT arc angle (degrees, 0 == 3 o'clock, CCW positive) into a
 * point on the ellipse described by (cx, cy) with radii (rx, ry). Direct2D's Y
 * axis grows downward, so the CCW math angle maps to a clockwise screen sweep
 * which we account for when choosing the sweep direction. */
static D2D1_POINT_2F cn1WinArcPoint(float cx, float cy, float rx, float ry, float degrees) {
    D2D1_POINT_2F p;
    float rad = degrees * (float)M_PI / 180.0f;
    p.x = cx + rx * (float)cos(rad);
    /* subtract because screen Y is inverted relative to math Y */
    p.y = cy - ry * (float)sin(rad);
    return p;
}

/* ------------------------------------------------------------ pen state */

JAVA_INT com_codename1_impl_windows_WindowsNative_getColor___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    return g->color;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_setColor___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    g->color = __cn1Arg2 & 0xffffff;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_setAlpha___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    g->alpha = __cn1Arg2 & 0xff;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getAlpha___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    return g->alpha;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_setNativeFont___long_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_LONG __cn1Arg2) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    CN1Font* f = (CN1Font*)__cn1Arg2;
    g->font = (f != NULL) ? f : cn1Win.defaultFont;
}

/* ----------------------------------------------------------------- clip */

JAVA_INT com_codename1_impl_windows_WindowsNative_getClipX___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return ((CN1Graphics*)__cn1Arg1)->clipX;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getClipY___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return ((CN1Graphics*)__cn1Arg1)->clipY;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getClipWidth___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return ((CN1Graphics*)__cn1Arg1)->clipW;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_getClipHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    return ((CN1Graphics*)__cn1Arg1)->clipH;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_setClip___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    g->clipX = __cn1Arg2;
    g->clipY = __cn1Arg3;
    g->clipW = __cn1Arg4;
    g->clipH = __cn1Arg5;
    g->clipIsRect = JAVA_TRUE;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_clipRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    /* Intersect the requested rectangle with the current clip. */
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    JAVA_INT curRight = g->clipX + g->clipW;
    JAVA_INT curBottom = g->clipY + g->clipH;
    JAVA_INT reqRight = __cn1Arg2 + __cn1Arg4;
    JAVA_INT reqBottom = __cn1Arg3 + __cn1Arg5;

    JAVA_INT newX = (__cn1Arg2 > g->clipX) ? __cn1Arg2 : g->clipX;
    JAVA_INT newY = (__cn1Arg3 > g->clipY) ? __cn1Arg3 : g->clipY;
    JAVA_INT newRight = (reqRight < curRight) ? reqRight : curRight;
    JAVA_INT newBottom = (reqBottom < curBottom) ? reqBottom : curBottom;

    g->clipX = newX;
    g->clipY = newY;
    g->clipW = (newRight > newX) ? (newRight - newX) : 0;
    g->clipH = (newBottom > newY) ? (newBottom - newY) : 0;
    g->clipIsRect = JAVA_TRUE;
}

/* ------------------------------------------------------------- primitives */

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawLine___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    D2D1_POINT_2F p0, p1;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    /* +0.5 offsets land the 1px line on the pixel center for crispness. */
    p0.x = (FLOAT)__cn1Arg2 + 0.5f;
    p0.y = (FLOAT)__cn1Arg3 + 0.5f;
    p1.x = (FLOAT)__cn1Arg4 + 0.5f;
    p1.y = (FLOAT)__cn1Arg5 + 0.5f;
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawLine(g->target, p0, p1, (ID2D1Brush*)brush, 1.0f, NULL);
    cn1WinPopClip(g);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fillRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    D2D1_RECT_F rect;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    rect.left = (FLOAT)__cn1Arg2;
    rect.top = (FLOAT)__cn1Arg3;
    rect.right = (FLOAT)(__cn1Arg2 + __cn1Arg4);
    rect.bottom = (FLOAT)(__cn1Arg3 + __cn1Arg5);
    cn1WinPushClip(g);
    ID2D1RenderTarget_FillRectangle(g->target, &rect, (ID2D1Brush*)brush);
    cn1WinPopClip(g);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawRect___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    D2D1_RECT_F rect;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    /* Inset by 0.5 so the 1px stroke sits inside the requested bounds crisply. */
    rect.left = (FLOAT)__cn1Arg2 + 0.5f;
    rect.top = (FLOAT)__cn1Arg3 + 0.5f;
    rect.right = (FLOAT)(__cn1Arg2 + __cn1Arg4) - 0.5f;
    rect.bottom = (FLOAT)(__cn1Arg3 + __cn1Arg5) - 0.5f;
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawRectangle(g->target, &rect, (ID2D1Brush*)brush, 1.0f, NULL);
    cn1WinPopClip(g);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawRoundRect___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    D2D1_ROUNDED_RECT rr;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    rr.rect.left = (FLOAT)__cn1Arg2 + 0.5f;
    rr.rect.top = (FLOAT)__cn1Arg3 + 0.5f;
    rr.rect.right = (FLOAT)(__cn1Arg2 + __cn1Arg4) - 0.5f;
    rr.rect.bottom = (FLOAT)(__cn1Arg3 + __cn1Arg5) - 0.5f;
    rr.radiusX = (FLOAT)__cn1Arg6 / 2.0f;
    rr.radiusY = (FLOAT)__cn1Arg7 / 2.0f;
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawRoundedRectangle(g->target, &rr, (ID2D1Brush*)brush, 1.0f, NULL);
    cn1WinPopClip(g);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fillRoundRect___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    D2D1_ROUNDED_RECT rr;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    rr.rect.left = (FLOAT)__cn1Arg2;
    rr.rect.top = (FLOAT)__cn1Arg3;
    rr.rect.right = (FLOAT)(__cn1Arg2 + __cn1Arg4);
    rr.rect.bottom = (FLOAT)(__cn1Arg3 + __cn1Arg5);
    rr.radiusX = (FLOAT)__cn1Arg6 / 2.0f;
    rr.radiusY = (FLOAT)__cn1Arg7 / 2.0f;
    cn1WinPushClip(g);
    ID2D1RenderTarget_FillRoundedRectangle(g->target, &rr, (ID2D1Brush*)brush);
    cn1WinPopClip(g);
}

/*
 * Arc helper shared by fillArc / drawArc. Builds a path geometry describing the
 * arc segment on the ellipse bounded by (x, y, w, h). For a filled pie we move
 * to the ellipse center, line to the arc start, sweep the arc and close back to
 * the center. For a stroked arc we just trace the open arc segment.
 *
 * CN1/AWT angles: startAngle is degrees CCW from 3 o'clock, arcAngle is the
 * signed CCW sweep. Direct2D's Y axis points down, so a mathematically CCW sweep
 * renders clockwise on screen; we therefore emit a D2D1_SWEEP_DIRECTION that is
 * the screen-space inverse of the math sign.
 */
static ID2D1PathGeometry* cn1WinBuildArcGeometry(CN1Graphics* g, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT startAngle, JAVA_INT arcAngle, JAVA_BOOLEAN pie) {
    ID2D1PathGeometry* geom = NULL;
    ID2D1GeometrySink* sink = NULL;
    float cx = (float)x + (float)w / 2.0f;
    float cy = (float)y + (float)h / 2.0f;
    float rx = (float)w / 2.0f;
    float ry = (float)h / 2.0f;
    D2D1_POINT_2F start = cn1WinArcPoint(cx, cy, rx, ry, (float)startAngle);
    D2D1_POINT_2F end = cn1WinArcPoint(cx, cy, rx, ry, (float)(startAngle + arcAngle));
    D2D1_ARC_SEGMENT arc;
    D2D1_POINT_2F center;

    if (FAILED(ID2D1Factory_CreatePathGeometry(cn1Win.d2dFactory, &geom)) || geom == NULL) {
        return NULL;
    }
    if (FAILED(ID2D1PathGeometry_Open(geom, &sink)) || sink == NULL) {
        ID2D1PathGeometry_Release(geom);
        return NULL;
    }

    if (pie) {
        center.x = cx;
        center.y = cy;
        ID2D1GeometrySink_BeginFigure(sink, center, D2D1_FIGURE_BEGIN_FILLED);
        ID2D1GeometrySink_AddLine(sink, start);
    } else {
        ID2D1GeometrySink_BeginFigure(sink, start, D2D1_FIGURE_BEGIN_HOLLOW);
    }

    arc.point = end;
    arc.size.width = rx;
    arc.size.height = ry;
    arc.rotationAngle = 0.0f;
    /* |arcAngle| >= 180 needs the large arc flag. */
    arc.arcSize = (arcAngle <= -180 || arcAngle >= 180) ? D2D1_ARC_SIZE_LARGE : D2D1_ARC_SIZE_SMALL;
    /* Positive math sweep (CCW) renders clockwise once Y is flipped, and vice versa. */
    arc.sweepDirection = (arcAngle >= 0) ? D2D1_SWEEP_DIRECTION_COUNTER_CLOCKWISE : D2D1_SWEEP_DIRECTION_CLOCKWISE;
    ID2D1GeometrySink_AddArc(sink, &arc);

    ID2D1GeometrySink_EndFigure(sink, pie ? D2D1_FIGURE_END_CLOSED : D2D1_FIGURE_END_OPEN);
    ID2D1GeometrySink_Close(sink);
    ID2D1GeometrySink_Release(sink);
    return geom;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fillArc___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    ID2D1PathGeometry* geom;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    geom = cn1WinBuildArcGeometry(g, __cn1Arg2, __cn1Arg3, __cn1Arg4, __cn1Arg5, __cn1Arg6, __cn1Arg7, JAVA_TRUE);
    if (geom == NULL) {
        return;
    }
    cn1WinPushClip(g);
    ID2D1RenderTarget_FillGeometry(g->target, (ID2D1Geometry*)geom, (ID2D1Brush*)brush, NULL);
    cn1WinPopClip(g);
    ID2D1PathGeometry_Release(geom);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawArc___long_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    ID2D1PathGeometry* geom;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    geom = cn1WinBuildArcGeometry(g, __cn1Arg2, __cn1Arg3, __cn1Arg4, __cn1Arg5, __cn1Arg6, __cn1Arg7, JAVA_FALSE);
    if (geom == NULL) {
        return;
    }
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawGeometry(g->target, (ID2D1Geometry*)geom, (ID2D1Brush*)brush, 1.0f, NULL);
    cn1WinPopClip(g);
    ID2D1PathGeometry_Release(geom);
}

/* ---------------------------------------------------------------- images */

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawImage___long_long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_LONG __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    CN1Image* img = (CN1Image*)__cn1Arg2;
    ID2D1Bitmap* bmp;
    D2D1_RECT_F dest;
    if (img == NULL) {
        return;
    }
    cn1WinBeginFrame(g);
    bmp = cn1WinEnsureBitmap(img, g->target);
    if (bmp == NULL) {
        return;
    }
    dest.left = (FLOAT)__cn1Arg3;
    dest.top = (FLOAT)__cn1Arg4;
    dest.right = (FLOAT)(__cn1Arg3 + img->width);
    dest.bottom = (FLOAT)(__cn1Arg4 + img->height);
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawBitmap(g->target, bmp, &dest, 1.0f, D2D1_BITMAP_INTERPOLATION_MODE_LINEAR, NULL);
    cn1WinPopClip(g);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawImageScaled___long_long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_LONG __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    CN1Image* img = (CN1Image*)__cn1Arg2;
    ID2D1Bitmap* bmp;
    D2D1_RECT_F dest;
    if (img == NULL) {
        return;
    }
    cn1WinBeginFrame(g);
    bmp = cn1WinEnsureBitmap(img, g->target);
    if (bmp == NULL) {
        return;
    }
    dest.left = (FLOAT)__cn1Arg3;
    dest.top = (FLOAT)__cn1Arg4;
    dest.right = (FLOAT)(__cn1Arg3 + __cn1Arg5);
    dest.bottom = (FLOAT)(__cn1Arg4 + __cn1Arg6);
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawBitmap(g->target, bmp, &dest, 1.0f, D2D1_BITMAP_INTERPOLATION_MODE_LINEAR, NULL);
    cn1WinPopClip(g);
}

/*
 * drawRGB renders a sub-region of a CN1 int[] of 0xAARRGGBB pixels. We build a
 * transient premultiplied BGRA Direct2D bitmap from the requested w x h window of
 * the source array (honouring offset and scanline stride), draw it at (x, y) and
 * release it. CN1 ints are 0xAARRGGBB; Direct2D's B8G8R8A8 wants bytes in B,G,R,A
 * order, so we repack per pixel. When processAlpha is false every pixel is forced
 * opaque; PREMULTIPLIED alpha means RGB must be scaled by A.
 */
JAVA_VOID com_codename1_impl_windows_WindowsNative_drawRGB___long_int_1ARRAY_int_int_int_int_int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7, JAVA_BOOLEAN __cn1Arg8) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    JAVA_INT offset = __cn1Arg3;
    JAVA_INT x = __cn1Arg4;
    JAVA_INT y = __cn1Arg5;
    JAVA_INT w = __cn1Arg6;
    JAVA_INT h = __cn1Arg7;
    JAVA_BOOLEAN processAlpha = __cn1Arg8;
    JAVA_ARRAY_INT* px;
    uint32_t* bgra;
    ID2D1Bitmap* bmp = NULL;
    D2D1_SIZE_U sz;
    D2D1_BITMAP_PROPERTIES props;
    D2D1_RECT_F dest;
    JAVA_INT row, col;

    if (__cn1Arg2 == JAVA_NULL || w <= 0 || h <= 0) {
        return;
    }
    px = (JAVA_ARRAY_INT*)(*(JAVA_ARRAY)__cn1Arg2).data;

    /* Repack the source window into a tightly packed (stride == w) BGRA buffer. */
    bgra = (uint32_t*)malloc((size_t)w * (size_t)h * sizeof(uint32_t));
    if (bgra == NULL) {
        return;
    }
    for (row = 0; row < h; row++) {
        /* The CN1 contract uses scanlength == w for the source rows. */
        JAVA_INT srcRow = offset + row * w;
        for (col = 0; col < w; col++) {
            uint32_t argb = (uint32_t)px[srcRow + col];
            uint32_t a = processAlpha ? ((argb >> 24) & 0xff) : 0xff;
            uint32_t r = (argb >> 16) & 0xff;
            uint32_t gc = (argb >> 8) & 0xff;
            uint32_t b = argb & 0xff;
            if (processAlpha && a != 0xff) {
                /* Premultiply: the target alpha mode is PREMULTIPLIED. */
                r = (r * a) / 255;
                gc = (gc * a) / 255;
                b = (b * a) / 255;
            }
            /* B8G8R8A8 little-endian byte order packs into 0xAARRGGBB-style word
             * as (A<<24)|(R<<16)|(G<<8)|B, which is exactly memory B,G,R,A. */
            bgra[row * w + col] = (a << 24) | (r << 16) | (gc << 8) | b;
        }
    }

    cn1WinBeginFrame(g);

    sz.width = (UINT32)w;
    sz.height = (UINT32)h;
    props.pixelFormat.format = DXGI_FORMAT_B8G8R8A8_UNORM;
    props.pixelFormat.alphaMode = D2D1_ALPHA_MODE_PREMULTIPLIED;
    props.dpiX = 96.0f;
    props.dpiY = 96.0f;

    if (FAILED(ID2D1RenderTarget_CreateBitmap(g->target, sz, bgra, (UINT32)(w * sizeof(uint32_t)), &props, &bmp)) || bmp == NULL) {
        free(bgra);
        return;
    }

    dest.left = (FLOAT)x;
    dest.top = (FLOAT)y;
    dest.right = (FLOAT)(x + w);
    dest.bottom = (FLOAT)(y + h);
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawBitmap(g->target, bmp, &dest, 1.0f, D2D1_BITMAP_INTERPOLATION_MODE_LINEAR, NULL);
    cn1WinPopClip(g);

    ID2D1Bitmap_Release(bmp);
    free(bgra);
}

/*
 * Builds a Direct2D path geometry from a flattened Codename One path: parallel
 * arrays of segment types (0=move,1=line,2=quad,3=cubic,4=close) and the packed
 * coordinate stream they index into (move/line=2 floats, quad=4, cubic=6,
 * close=0). This is what lets RoundBorder / RoundRectBorder (the material pill
 * buttons, rounded dialogs, chat bubbles) and Graphics.fillShape/drawShape paint
 * -- without it isShapeSupported is false and those backgrounds never render.
 */
static ID2D1PathGeometry* cn1WinBuildPathGeometry(const JAVA_ARRAY_FLOAT* coords, const JAVA_ARRAY_INT* types,
        JAVA_INT typeCount, JAVA_INT windingRule) {
    ID2D1PathGeometry* geom = NULL;
    ID2D1GeometrySink* sink = NULL;
    JAVA_INT ci = 0;
    JAVA_INT i;
    int figureOpen = 0;
    if (FAILED(ID2D1Factory_CreatePathGeometry(cn1Win.d2dFactory, &geom)) || geom == NULL) {
        return NULL;
    }
    if (FAILED(ID2D1PathGeometry_Open(geom, &sink)) || sink == NULL) {
        ID2D1PathGeometry_Release(geom);
        return NULL;
    }
    /* CN1 PathIterator: WIND_EVEN_ODD = 0, WIND_NON_ZERO = 1. */
    ID2D1GeometrySink_SetFillMode(sink, windingRule == 0 ? D2D1_FILL_MODE_ALTERNATE : D2D1_FILL_MODE_WINDING);
    for (i = 0; i < typeCount; i++) {
        switch (types[i]) {
            case 0: { /* MOVETO */
                D2D1_POINT_2F p;
                if (figureOpen) {
                    ID2D1GeometrySink_EndFigure(sink, D2D1_FIGURE_END_OPEN);
                }
                p.x = coords[ci++];
                p.y = coords[ci++];
                ID2D1GeometrySink_BeginFigure(sink, p, D2D1_FIGURE_BEGIN_FILLED);
                figureOpen = 1;
                break;
            }
            case 1: { /* LINETO */
                D2D1_POINT_2F p;
                p.x = coords[ci++];
                p.y = coords[ci++];
                if (figureOpen) {
                    ID2D1GeometrySink_AddLine(sink, p);
                }
                break;
            }
            case 2: { /* QUADTO */
                D2D1_QUADRATIC_BEZIER_SEGMENT qb;
                qb.point1.x = coords[ci++];
                qb.point1.y = coords[ci++];
                qb.point2.x = coords[ci++];
                qb.point2.y = coords[ci++];
                if (figureOpen) {
                    ID2D1GeometrySink_AddQuadraticBezier(sink, &qb);
                }
                break;
            }
            case 3: { /* CUBICTO */
                D2D1_BEZIER_SEGMENT bz;
                bz.point1.x = coords[ci++];
                bz.point1.y = coords[ci++];
                bz.point2.x = coords[ci++];
                bz.point2.y = coords[ci++];
                bz.point3.x = coords[ci++];
                bz.point3.y = coords[ci++];
                if (figureOpen) {
                    ID2D1GeometrySink_AddBezier(sink, &bz);
                }
                break;
            }
            case 4: /* CLOSE */
                if (figureOpen) {
                    ID2D1GeometrySink_EndFigure(sink, D2D1_FIGURE_END_CLOSED);
                    figureOpen = 0;
                }
                break;
            default:
                break;
        }
    }
    if (figureOpen) {
        ID2D1GeometrySink_EndFigure(sink, D2D1_FIGURE_END_OPEN);
    }
    if (FAILED(ID2D1GeometrySink_Close(sink))) {
        ID2D1GeometrySink_Release(sink);
        ID2D1PathGeometry_Release(geom);
        return NULL;
    }
    ID2D1GeometrySink_Release(sink);
    return geom;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_fillShape___long_float_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_OBJECT __cn1Arg3,
        JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    ID2D1PathGeometry* geom;
    const JAVA_ARRAY_FLOAT* coords;
    const JAVA_ARRAY_INT* types;
    if (g == NULL || __cn1Arg2 == NULL || __cn1Arg3 == NULL) {
        return;
    }
    coords = (const JAVA_ARRAY_FLOAT*)(*(JAVA_ARRAY)__cn1Arg2).data;
    types = (const JAVA_ARRAY_INT*)(*(JAVA_ARRAY)__cn1Arg3).data;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    geom = cn1WinBuildPathGeometry(coords, types, __cn1Arg4, __cn1Arg5);
    if (geom == NULL) {
        return;
    }
    cn1WinPushClip(g);
    ID2D1RenderTarget_FillGeometry(g->target, (ID2D1Geometry*)geom, (ID2D1Brush*)brush, NULL);
    cn1WinPopClip(g);
    ID2D1PathGeometry_Release(geom);
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_drawShape___long_float_1ARRAY_int_1ARRAY_int_int_float(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_OBJECT __cn1Arg3,
        JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_FLOAT __cn1Arg6) {
    CN1Graphics* g = (CN1Graphics*)__cn1Arg1;
    ID2D1SolidColorBrush* brush;
    ID2D1PathGeometry* geom;
    const JAVA_ARRAY_FLOAT* coords;
    const JAVA_ARRAY_INT* types;
    float lineWidth = __cn1Arg6 > 0.0f ? (float)__cn1Arg6 : 1.0f;
    if (g == NULL || __cn1Arg2 == NULL || __cn1Arg3 == NULL) {
        return;
    }
    coords = (const JAVA_ARRAY_FLOAT*)(*(JAVA_ARRAY)__cn1Arg2).data;
    types = (const JAVA_ARRAY_INT*)(*(JAVA_ARRAY)__cn1Arg3).data;
    cn1WinBeginFrame(g);
    brush = cn1WinBrush(g);
    geom = cn1WinBuildPathGeometry(coords, types, __cn1Arg4, __cn1Arg5);
    if (geom == NULL) {
        return;
    }
    cn1WinPushClip(g);
    ID2D1RenderTarget_DrawGeometry(g->target, (ID2D1Geometry*)geom, (ID2D1Brush*)brush, lineWidth, NULL);
    cn1WinPopClip(g);
    ID2D1PathGeometry_Release(geom);
}

} /* extern "C" */

#endif /* _WIN32 */
