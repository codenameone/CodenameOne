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
#import "CN1RenderBackend.h"
#import "DrawTextureAlphaMask.h"
#import "PaintOp.h"
#import "RadialGradientPaint.h"
#import "CodenameOne_GLViewController.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif


@implementation DrawTextureAlphaMask

-(id)initWithArgs:(JAVA_LONG)pTextName color:(int)pColor alpha:(int)pAlpha x:(int)pX y:(int)pY w:(int)pW h:(int)pH
{
    textureName = pTextName;
    color = pColor;
    alpha = pAlpha;
    x = pX;
    y = pY;
    w = pW;
    h = pH;
#ifdef CN1_USE_METAL
    // The handle is a CFBridgingRetain'd id<MTLTexture> owned by the Java
    // TextureAlphaMask object (which the textureCache holds only as a
    // SoftWeak ref). With the concurrent GC, finalize() can run between
    // op queueing and drawFrame draining; finalize -> dispose ->
    // nativeDeleteTexture -> CFBridgingRelease drops the +1, and we'd
    // sample a freed texture at execute time. Take an additional retain
    // here so the texture survives until this op deallocs after drain.
    if (textureName != 0) {
        CFRetain((CFTypeRef)(void *)(uintptr_t)textureName);
    }
    // Snapshot the active RadialGradientPaint at queue time so the op
    // renders the gradient even after the mutable-side unapplyPaint clears
    // PaintOp.currentMutable. Read from currentMutable -- the screen path
    // already queues a RadialGradientPaint op that runs in-order with
    // execute, so its mutation happens between this op's queue and execute.
    hasRadialPaint = NO;
    PaintOp *snapshot = [PaintOp getCurrentMutable];
    if (snapshot != NULL && [snapshot isKindOfClass:[RadialGradientPaint class]]) {
        RadialGradientPaint *g = (RadialGradientPaint *)snapshot;
        hasRadialPaint = YES;
        radialStartColor = g.startColor;
        radialEndColor = g.endColor;
        radialX = g.x;
        radialY = g.y;
        radialWidth = g.width;
        radialHeight = g.height;
    }
#endif
    return self;
}

#ifdef CN1_USE_METAL
-(void)dealloc {
    if (textureName != 0) {
        CFRelease((CFTypeRef)(void *)(uintptr_t)textureName);
    }
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}
#endif
#if TARGET_OS_WATCH
-(void)execute
{
    if (textureName == 0) { return; }
    CN1CGAlphaMask *mask = (CN1CGAlphaMask *)(uintptr_t)textureName;
    if (mask == NULL || mask->alphas == NULL || mask->width <= 0 || mask->height <= 0) { return; }
    // Draw the coverage mask at the op's position using the mask's own
    // dimensions (they match the bounds captured at texture-create time).
    CN1CGFillAlphaMask(color, alpha, x, y, mask->width, mask->height, mask->alphas);
}
#else
-(void)execute
{
#ifdef CN1_USE_METAL
    if (textureName == 0) {
        CN1Log(@"Attempt to draw null alpha-mask texture. Skipping");
        return;
    }
    // textureName is a CFBridgingRetain'd id<MTLTexture> set up by
    // IOSNative.m's nativePathRendererCreateTexture under Metal. Just bridge
    // back to the Obj-C handle (no transfer of ownership) and dispatch.
    id<MTLTexture> tex = (__bridge id<MTLTexture>)(void *)(uintptr_t)textureName;
    // Resolve the radial-gradient paint differently per target:
    //
    //   Mutable (target != nil): the Java-side applyPaint() and
    //     unapplyPaint() invoke applyRadialGradientPaintMutable /
    //     clearRadialGradientPaintMutable as direct C calls -- they set and
    //     clear PaintOp.currentMutable synchronously around the queue call,
    //     before drainOps runs. Reading PaintOp.currentMutable at execute
    //     time is too late; use the snapshot captured at init.
    //
    //   Screen (target == nil): the Java-side applyPaint() invokes
    //     applyRadialGradientPaintGlobal which queues a RadialGradientPaint
    //     op into the same queue as this DrawTextureAlphaMask op. Its
    //     execute sets PaintOp.current just before our execute runs, so
    //     reading it here is correct.
    if (target != nil && hasRadialPaint) {
        CN1MetalDrawAlphaMaskRadial(tex, x, y, w, h,
                                    radialStartColor, radialEndColor,
                                    (float)radialX, (float)radialY,
                                    (float)radialWidth, (float)radialHeight);
        return;
    }
    if (target == nil) {
        PaintOp *paint = [PaintOp getCurrent];
        if (paint != NULL && [paint isKindOfClass:[RadialGradientPaint class]]) {
            RadialGradientPaint *g = (RadialGradientPaint *)paint;
            CN1MetalDrawAlphaMaskRadial(tex, x, y, w, h,
                                        g.startColor, g.endColor,
                                        (float)g.x, (float)g.y,
                                        (float)g.width, (float)g.height);
            return;
        }
    }
    CN1MetalDrawAlphaMask(tex, color, alpha, x, y, w, h);
#endif // CN1_USE_METAL
}
#endif
@end
