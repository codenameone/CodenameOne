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
#import "CN1CGGraphics.h"

#if TARGET_OS_WATCH
#import <CoreText/CoreText.h>

// Active target context for the current frame, plus the surface height (used
// for the top-left coordinate flip) and a snapshot of the base graphics state.
static CGContextRef cn1ActiveContext = NULL;
static int cn1SurfaceWidth = 0;
static int cn1SurfaceHeight = 0;

// Decompose an int color (0xRRGGBB) + 0-255 alpha into CG components.
static inline void cn1Components(int color, int alpha, CGFloat *r, CGFloat *g, CGFloat *b, CGFloat *a) {
    *r = (CGFloat)((color >> 16) & 0xff) / 255.0;
    *g = (CGFloat)((color >> 8) & 0xff) / 255.0;
    *b = (CGFloat)(color & 0xff) / 255.0;
    *a = (CGFloat)alpha / 255.0;
}

void CN1CGBeginFrame(CGContextRef ctx, int w, int h) {
    cn1ActiveContext = ctx;
    cn1SurfaceWidth = w;
    cn1SurfaceHeight = h;
    if (ctx == NULL) {
        return;
    }
    // The caller (CN1WatchRenderingView) has already applied the device-scale
    // to the context CTM, so 1 unit == 1 logical point with a bottom-left,
    // y-up origin. Save that scaled base (S0), flip to a top-left y-down origin
    // to match CN1/UIKit, then save again (S1) as the per-frame base that
    // CN1CGResetClip / CN1CGResetAffine rebase to. Do NOT reset the CTM here -
    // doing so would discard the device scale and render at 1x in a corner.
    CGContextSaveGState(ctx);            // S0: scaled device base
    CGContextTranslateCTM(ctx, 0, h);    // flip (h in logical units)
    CGContextScaleCTM(ctx, 1, -1);
    CGContextSaveGState(ctx);            // S1: per-frame base (flip, no clip)
}

void CN1CGEndFrame(void) {
    if (cn1ActiveContext != NULL) {
        CGContextRestoreGState(cn1ActiveContext); // pop S1
        CGContextRestoreGState(cn1ActiveContext); // pop S0 -> back to scaled base
    }
    cn1ActiveContext = NULL;
}

CGContextRef CN1CGCurrentContext(void) {
    return cn1ActiveContext;
}

void CN1CGFillRect(int color, int alpha, int x, int y, int w, int h) {
    if (cn1ActiveContext == NULL) { return; }
    CGFloat r, g, b, a;
    cn1Components(color, alpha, &r, &g, &b, &a);
    CGContextSetRGBFillColor(cn1ActiveContext, r, g, b, a);
    CGContextFillRect(cn1ActiveContext, CGRectMake(x, y, w, h));
}

void CN1CGDrawRect(int color, int alpha, int x, int y, int w, int h, float lineWidth) {
    if (cn1ActiveContext == NULL) { return; }
    CGFloat r, g, b, a;
    cn1Components(color, alpha, &r, &g, &b, &a);
    CGContextSetRGBStrokeColor(cn1ActiveContext, r, g, b, a);
    CGContextSetLineWidth(cn1ActiveContext, lineWidth <= 0 ? 1 : lineWidth);
    CGContextStrokeRect(cn1ActiveContext, CGRectMake(x, y, w, h));
}

void CN1CGClearRect(int x, int y, int w, int h) {
    if (cn1ActiveContext == NULL) { return; }
    CGContextClearRect(cn1ActiveContext, CGRectMake(x, y, w, h));
}

void CN1CGDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
    if (cn1ActiveContext == NULL) { return; }
    CGFloat r, g, b, a;
    cn1Components(color, alpha, &r, &g, &b, &a);
    CGContextSetRGBStrokeColor(cn1ActiveContext, r, g, b, a);
    CGContextSetLineWidth(cn1ActiveContext, 1);
    CGContextBeginPath(cn1ActiveContext);
    CGContextMoveToPoint(cn1ActiveContext, x1, y1);
    CGContextAddLineToPoint(cn1ActiveContext, x2, y2);
    CGContextStrokePath(cn1ActiveContext);
}

void CN1CGFillPolygon(int color, int alpha, float *xPoints, float *yPoints, int numPoints) {
    if (cn1ActiveContext == NULL || numPoints < 2 || xPoints == NULL || yPoints == NULL) { return; }
    CGFloat r, g, b, a;
    cn1Components(color, alpha, &r, &g, &b, &a);
    CGContextSetRGBFillColor(cn1ActiveContext, r, g, b, a);
    CGContextBeginPath(cn1ActiveContext);
    CGContextMoveToPoint(cn1ActiveContext, xPoints[0], yPoints[0]);
    for (int i = 1; i < numPoints; i++) {
        CGContextAddLineToPoint(cn1ActiveContext, xPoints[i], yPoints[i]);
    }
    CGContextClosePath(cn1ActiveContext);
    CGContextEOFillPath(cn1ActiveContext);
}

// Images are drawn with their own top-left->bottom-left flip so they aren't
// mirrored by the surface flip installed in CN1CGBeginFrame.
void CN1CGDrawImage(CGImageRef img, int alpha, int x, int y, int w, int h) {
    if (cn1ActiveContext == NULL || img == NULL) { return; }
    CGContextSaveGState(cn1ActiveContext);
    CGContextSetAlpha(cn1ActiveContext, (CGFloat)alpha / 255.0);
    CGContextTranslateCTM(cn1ActiveContext, x, y + h);
    CGContextScaleCTM(cn1ActiveContext, 1, -1);
    CGContextDrawImage(cn1ActiveContext, CGRectMake(0, 0, w, h), img);
    CGContextRestoreGState(cn1ActiveContext);
}

void CN1CGTileImage(CGImageRef img, int alpha, int x, int y, int w, int h) {
    if (cn1ActiveContext == NULL || img == NULL) { return; }
    size_t iw = CGImageGetWidth(img);
    size_t ih = CGImageGetHeight(img);
    if (iw == 0 || ih == 0) { return; }
    CGContextSaveGState(cn1ActiveContext);
    CGContextClipToRect(cn1ActiveContext, CGRectMake(x, y, w, h));
    for (int ty = y; ty < y + h; ty += ih) {
        for (int tx = x; tx < x + w; tx += iw) {
            CN1CGDrawImage(img, alpha, tx, ty, (int)iw, (int)ih);
        }
    }
    CGContextRestoreGState(cn1ActiveContext);
}

void CN1CGFillAlphaMask(int color, int alpha, int x, int y, int w, int h, const unsigned char *maskAlphas) {
    if (cn1ActiveContext == NULL || maskAlphas == NULL || w <= 0 || h <= 0) { return; }
    CGFloat r, g, b, a;
    cn1Components(color, alpha, &r, &g, &b, &a);
    // Bake color * coverage into a premultiplied-RGBA bitmap so the result
    // matches the GL "alpha texture modulated by color" semantics exactly.
    size_t count = (size_t)w * (size_t)h;
    unsigned char *rgba = (unsigned char *)malloc(count * 4);
    if (rgba == NULL) { return; }
    for (size_t i = 0; i < count; i++) {
        CGFloat cov = ((CGFloat)maskAlphas[i] / 255.0) * a;
        rgba[i * 4 + 0] = (unsigned char)(r * cov * 255.0 + 0.5);
        rgba[i * 4 + 1] = (unsigned char)(g * cov * 255.0 + 0.5);
        rgba[i * 4 + 2] = (unsigned char)(b * cov * 255.0 + 0.5);
        rgba[i * 4 + 3] = (unsigned char)(cov * 255.0 + 0.5);
    }
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
    CGContextRef bmp = CGBitmapContextCreate(rgba, w, h, 8, w * 4, space,
                                             kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGImageRef img = bmp != NULL ? CGBitmapContextCreateImage(bmp) : NULL;
    if (img != NULL) {
        CN1CGDrawImage(img, 255, x, y, w, h);
        CGImageRelease(img);
    }
    if (bmp != NULL) { CGContextRelease(bmp); }
    CGColorSpaceRelease(space);
    free(rgba);
}

void CN1CGDrawString(int color, int alpha, int x, int y, NSString *str, UIFont *font) {
    if (cn1ActiveContext == NULL || str == nil || str.length == 0) { return; }
    CGFloat r, g, b, a;
    cn1Components(color, alpha, &r, &g, &b, &a);
    UIColor *uc = [UIColor colorWithRed:r green:g blue:b alpha:a];
    CTFontRef ctFont = NULL;
    if (font != nil) {
        ctFont = CTFontCreateWithName((CFStringRef)font.fontName, font.pointSize, NULL);
    } else {
        ctFont = CTFontCreateWithName(CFSTR("Helvetica"), 16, NULL);
    }
    NSDictionary *attrs = @{
        (id)kCTFontAttributeName : (__bridge id)ctFont,
        (id)kCTForegroundColorAttributeName : (id)uc.CGColor
    };
    NSAttributedString *attr = [[NSAttributedString alloc] initWithString:str attributes:attrs];
    CTLineRef line = CTLineCreateWithAttributedString((CFAttributedStringRef)attr);

    // CN1 positions text by its top-left corner. Core Text draws from the
    // baseline, so offset down by the font ascent. The text matrix is flipped
    // back to top-left to counter the surface flip.
    CGFloat ascent = CTFontGetAscent(ctFont);
    CGContextSaveGState(cn1ActiveContext);
    CGContextSetTextMatrix(cn1ActiveContext, CGAffineTransformMakeScale(1, -1));
    CGContextSetTextPosition(cn1ActiveContext, x, y + ascent);
    CTLineDraw(line, cn1ActiveContext);
    CGContextRestoreGState(cn1ActiveContext);

    CFRelease(line);
    if (ctFont != NULL) { CFRelease(ctFont); }
#ifndef CN1_USE_ARC
    [attr release];
#endif
}

void CN1CGGradientRect(int type, int startColor, int endColor, int x, int y,
                       int w, int h, float relativeX, float relativeY, float relativeSize) {
    if (cn1ActiveContext == NULL) { return; }
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
    CGFloat sr, sg, sb, sa, er, eg, eb, ea;
    cn1Components(startColor, 255, &sr, &sg, &sb, &sa);
    cn1Components(endColor, 255, &er, &eg, &eb, &ea);
    CGFloat comps[] = { sr, sg, sb, 1.0, er, eg, eb, 1.0 };
    CGFloat locations[] = { 0.0, 1.0 };
    CGGradientRef gradient = CGGradientCreateWithColorComponents(space, comps, locations, 2);

    CGContextSaveGState(cn1ActiveContext);
    CGContextClipToRect(cn1ActiveContext, CGRectMake(x, y, w, h));
    if (type == 1) { // radial
        CGFloat cx = x + w * relativeX;
        CGFloat cy = y + h * relativeY;
        CGFloat radius = MAX(w, h) * relativeSize;
        CGContextDrawRadialGradient(cn1ActiveContext, gradient,
                                    CGPointMake(cx, cy), 0,
                                    CGPointMake(cx, cy), radius,
                                    kCGGradientDrawsAfterEndLocation);
    } else if (type == 2) { // horizontal
        CGContextDrawLinearGradient(cn1ActiveContext, gradient,
                                    CGPointMake(x, y), CGPointMake(x + w, y), 0);
    } else { // vertical
        CGContextDrawLinearGradient(cn1ActiveContext, gradient,
                                    CGPointMake(x, y), CGPointMake(x, y + h), 0);
    }
    CGContextRestoreGState(cn1ActiveContext);
    CGGradientRelease(gradient);
    CGColorSpaceRelease(space);
}

// --- Clip: emulate CN1's replace-semantics by restoring the base gstate. ---

static void cn1RebaseGState(void) {
    // Pop back to the base state saved in BeginFrame, then re-save so the next
    // restore is balanced. The CTM (flip) is preserved by the base state.
    CGContextRestoreGState(cn1ActiveContext);
    CGContextSaveGState(cn1ActiveContext);
}

void CN1CGSetClipRect(int x, int y, int w, int h) {
    if (cn1ActiveContext == NULL) { return; }
    cn1RebaseGState();
    CGContextClipToRect(cn1ActiveContext, CGRectMake(x, y, w, h));
}

void CN1CGSetClipPolygon(float *xPoints, float *yPoints, int numPoints) {
    if (cn1ActiveContext == NULL || numPoints < 2 || xPoints == NULL || yPoints == NULL) { return; }
    cn1RebaseGState();
    CGContextBeginPath(cn1ActiveContext);
    CGContextMoveToPoint(cn1ActiveContext, xPoints[0], yPoints[0]);
    for (int i = 1; i < numPoints; i++) {
        CGContextAddLineToPoint(cn1ActiveContext, xPoints[i], yPoints[i]);
    }
    CGContextClosePath(cn1ActiveContext);
    CGContextClip(cn1ActiveContext);
}

void CN1CGResetClip(void) {
    if (cn1ActiveContext == NULL) { return; }
    cn1RebaseGState();
}

// --- Affine transform -------------------------------------------------------

void CN1CGSetAffine(CGFloat a, CGFloat b, CGFloat c, CGFloat d, CGFloat tx, CGFloat ty,
                    int originX, int originY) {
    if (cn1ActiveContext == NULL) { return; }
    // Apply about the supplied origin: translate to origin, concat, translate back.
    CGAffineTransform t = CGAffineTransformMake(a, b, c, d, tx, ty);
    CGContextTranslateCTM(cn1ActiveContext, originX, originY);
    CGContextConcatCTM(cn1ActiveContext, t);
    CGContextTranslateCTM(cn1ActiveContext, -originX, -originY);
}

void CN1CGScale(CGFloat sx, CGFloat sy) {
    if (cn1ActiveContext == NULL) { return; }
    CGContextScaleCTM(cn1ActiveContext, sx, sy);
}

void CN1CGRotate(CGFloat radians, int x, int y) {
    if (cn1ActiveContext == NULL) { return; }
    CGContextTranslateCTM(cn1ActiveContext, x, y);
    CGContextRotateCTM(cn1ActiveContext, radians);
    CGContextTranslateCTM(cn1ActiveContext, -x, -y);
}

void CN1CGResetAffine(void) {
    if (cn1ActiveContext == NULL) { return; }
    // Restore base (which also resets clip) then re-save. Transform and clip
    // share the gstate, so a full affine reset rebases both - acceptable since
    // CN1 issues ResetAffine at the start of a component paint, before clip.
    cn1RebaseGState();
}

#endif // TARGET_OS_WATCH
