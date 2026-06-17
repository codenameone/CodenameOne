/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
 * CN1CGGraphics is the watchOS rendering backend for Codename One.
 *
 * watchOS has no OpenGL ES and no Metal, so the GL/Metal ExecutableOp paths
 * used on iOS cannot run. Instead the same ExecutableOp queue is rasterized
 * into a CGBitmapContext using Core Graphics + Core Text. The bitmap is then
 * handed to the watch host surface (see CN1WatchRenderingView) for display.
 *
 * The whole module is compiled only on the watchOS slice; every entry point is
 * wrapped in TARGET_OS_WATCH so the iOS build is byte-for-byte unchanged.
 *
 * Coordinate system: CN1 (like UIKit) uses a top-left origin with y growing
 * downward. CGBitmapContext is bottom-left by default, so CN1CGBeginFrame
 * installs a flip (translate 0,h; scale 1,-1) as part of the base graphics
 * state. All op coordinates below are therefore in CN1's top-left space.
 *
 * Color ints are 0xRRGGBB with a separate 0-255 alpha, matching the GL ops.
 */
#ifndef CN1CGGraphics_h
#define CN1CGGraphics_h

#include "TargetConditionals.h"

#if TARGET_OS_WATCH
#import <Foundation/Foundation.h>
#import <CoreGraphics/CoreGraphics.h>
#import <UIKit/UIKit.h>   // value types only on watchOS: UIImage, UIFont, UIColor

// Frame lifecycle ----------------------------------------------------------

// Bind ctx as the active target and reset transform + clip to the full
// w x h surface (top-left origin). Called by CN1WatchRenderingView when it
// begins a frame against its bitmap context (the screen) or a mutable image.
void CN1CGBeginFrame(CGContextRef ctx, int w, int h);

// Detach the active context. After this, the CN1CG* draw calls are no-ops
// until the next CN1CGBeginFrame.
void CN1CGEndFrame(void);

// The context currently bound by CN1CGBeginFrame, or NULL.
CGContextRef CN1CGCurrentContext(void);

// 2D primitives ------------------------------------------------------------

void CN1CGFillRect(int color, int alpha, int x, int y, int w, int h);
void CN1CGDrawRect(int color, int alpha, int x, int y, int w, int h, float lineWidth);
void CN1CGClearRect(int x, int y, int w, int h);
void CN1CGDrawLine(int color, int alpha, int x1, int y1, int x2, int y2);
void CN1CGFillPolygon(int color, int alpha, float *xPoints, float *yPoints, int numPoints);
void CN1CGDrawImage(CGImageRef img, int alpha, int x, int y, int w, int h);
void CN1CGTileImage(CGImageRef img, int alpha, int x, int y, int w, int h);

// Fill an 8-bit coverage/alpha mask (row-major, top-down, w*h bytes) with the
// given color. Used to rasterize anti-aliased shapes (DrawPath / the alpha-mask
// shape pipeline) that the iOS port renders through a GL alpha texture.
void CN1CGFillAlphaMask(int color, int alpha, int x, int y, int w, int h, const unsigned char *maskAlphas);

// On watchOS the iOS "alpha mask texture" (a GL/Metal texture handle on those
// platforms) is replaced by a heap-allocated copy of the coverage bytes. The
// JAVA_LONG texture handle returned by nativePathRendererCreateTexture points
// at one of these; DrawTextureAlphaMask reads it and feeds CN1CGFillAlphaMask.
typedef struct {
    int width;
    int height;
    unsigned char *alphas; // width*height bytes, row-major top-down
} CN1CGAlphaMask;

// Draw str at (x,y) (top-left baseline-independent) in the given font/color
// via Core Text, honoring the active clip + transform.
void CN1CGDrawString(int color, int alpha, int x, int y, NSString *str, UIFont *font);

// Linear/radial gradient fill of the x,y,w,h rect. type matches DrawGradient:
// 1=radial, 2=horizontal, 3=vertical.
void CN1CGGradientRect(int type, int startColor, int endColor, int x, int y,
                       int w, int h, float relativeX, float relativeY, float relativeSize);

// Clip ---------------------------------------------------------------------
// CN1's model "sets" (replaces) the clip; Core Graphics can only intersect.
// We emulate replace-semantics by restoring to the per-frame base graphics
// state (saved in CN1CGBeginFrame) before applying the new clip.

void CN1CGSetClipRect(int x, int y, int w, int h);
void CN1CGSetClipPolygon(float *xPoints, float *yPoints, int numPoints);
void CN1CGResetClip(void);

// Affine transform ----------------------------------------------------------
// CN1 ships a GLKMatrix4 for the 2D transform; only the affine 2x3 submatrix
// is meaningful for CG. Callers pass the six affine components directly.

void CN1CGSetAffine(CGFloat a, CGFloat b, CGFloat c, CGFloat d, CGFloat tx, CGFloat ty,
                    int originX, int originY);
void CN1CGScale(CGFloat sx, CGFloat sy);
void CN1CGRotate(CGFloat radians, int x, int y);
void CN1CGResetAffine(void);

#endif // TARGET_OS_WATCH
#endif // CN1CGGraphics_h
