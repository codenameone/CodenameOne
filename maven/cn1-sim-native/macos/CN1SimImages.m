/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 * Image natives for the desktop simulator: decode/encode via ImageIO
 * (CGImageSource) and CoreGraphics, peers are retained GLUIImage instances -
 * the same peer type the device uses, so the DrawImage/TileImage ops and the
 * Metal texture pipeline work unmodified.
 */
#import <UIKit/UIKit.h>   /* sim shim */
#import <ImageIO/ImageIO.h>
#include "cn1jni_runtime.h"
#import "CodenameOne_GLViewController.h"
#import "GLUIImage.h"
#import "DrawImage.h"
#import "TileImage.h"

static CGImageRef cn1simDecodeImage(const void *bytes, int len) {
    CFDataRef data = CFDataCreate(NULL, (const UInt8 *) bytes, len);
    CGImageSourceRef src = CGImageSourceCreateWithData(data, NULL);
    CFRelease(data);
    if (src == NULL) {
        return NULL;
    }
    CGImageRef img = CGImageSourceCreateImageAtIndex(src, 0, NULL);
    CFRelease(src);
    return img;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImage___byte_1ARRAY_int_1ARRAY_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_OBJECT dataArr, JAVA_OBJECT whArr) {
    @autoreleasepool {
        JAVA_ARRAY byteArray = (JAVA_ARRAY) dataArr;
        JAVA_ARRAY intArray = (JAVA_ARRAY) whArr;
        CGImageRef cg = cn1simDecodeImage(byteArray->data, byteArray->length);
        if (cg == NULL) {
            return 0;
        }
        UIImage *img = [UIImage imageWithCGImage:cg];
        CGImageRelease(cg);
        GLUIImage *g = [[GLUIImage alloc] initWithImage:img];
        JAVA_ARRAY_INT *wh = (JAVA_ARRAY_INT *) intArray->data;
        wh[0] = (JAVA_ARRAY_INT) [img size].width;
        wh[1] = (JAVA_ARRAY_INT) [img size].height;
        return (JAVA_LONG) (intptr_t) g;
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createImageFromARGB___int_1ARRAY_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_OBJECT argbArr, JAVA_INT w, JAVA_INT h) {
    @autoreleasepool {
        JAVA_ARRAY arr = (JAVA_ARRAY) argbArr;
        const uint32_t *argb = (const uint32_t *) arr->data;
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, w, h, 8, 0, cs,
                kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host);
        CGColorSpaceRelease(cs);
        if (ctx == NULL) {
            return 0;
        }
        uint32_t *dst = (uint32_t *) CGBitmapContextGetData(ctx);
        size_t stride = CGBitmapContextGetBytesPerRow(ctx) / 4;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                /* premultiply ARGB into the host byte order */
                uint32_t p = argb[y * w + x];
                uint32_t a = (p >> 24) & 0xff;
                uint32_t r = (p >> 16) & 0xff;
                uint32_t gC = (p >> 8) & 0xff;
                uint32_t b = p & 0xff;
                r = r * a / 255;
                gC = gC * a / 255;
                b = b * a / 255;
                dst[y * stride + x] = (a << 24) | (r << 16) | (gC << 8) | b;
            }
        }
        CGImageRef cg = CGBitmapContextCreateImage(ctx);
        CGContextRelease(ctx);
        UIImage *img = [UIImage imageWithCGImage:cg];
        CGImageRelease(cg);
        GLUIImage *g = [[GLUIImage alloc] initWithImage:img];
        return (JAVA_LONG) (intptr_t) g;
    }
}

void com_codename1_impl_ios_IOSNative_imageRgbToIntArray___long_int_1ARRAY_int_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT arrObj,
        JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT imgWidth, JAVA_INT imgHeight) {
    @autoreleasepool {
        GLUIImage *g = (GLUIImage *) (intptr_t) peer;
        JAVA_ARRAY arr = (JAVA_ARRAY) arrObj;
        uint32_t *out = (uint32_t *) arr->data;
        UIImage *img = [g getImage];
        if (img == nil || out == NULL) {
            return;
        }
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, imgWidth, imgHeight, 8, 0, cs,
                kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host);
        CGColorSpaceRelease(cs);
        if (ctx == NULL) {
            return;
        }
        CGContextDrawImage(ctx, CGRectMake(0, 0, imgWidth, imgHeight), [img CGImage]);
        const uint32_t *px = (const uint32_t *) CGBitmapContextGetData(ctx);
        size_t stride = CGBitmapContextGetBytesPerRow(ctx) / 4;
        for (int row = 0; row < height; row++) {
            int srcY = y + row;
            /* CG bitmap memory row 0 is the top scanline - no flip */
            for (int col = 0; col < width; col++) {
                uint32_t p = px[srcY * stride + (x + col)];
                uint32_t a = (p >> 24) & 0xff;
                uint32_t r = (p >> 16) & 0xff;
                uint32_t gC = (p >> 8) & 0xff;
                uint32_t b = p & 0xff;
                if (a != 0 && a != 255) {
                    /* un-premultiply */
                    r = r * 255 / a;
                    gC = gC * 255 / a;
                    b = b * 255 / a;
                }
                out[row * width + col] = (a << 24) | (r << 16) | (gC << 8) | b;
            }
        }
        CGContextRelease(ctx);
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_scale___long_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_INT w, JAVA_INT h) {
    @autoreleasepool {
        GLUIImage *g = (GLUIImage *) (intptr_t) peer;
        UIImage *img = [g getImage];
        if (img == nil) {
            return 0;
        }
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, w, h, 8, 0, cs,
                kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host);
        CGColorSpaceRelease(cs);
        if (ctx == NULL) {
            return 0;
        }
        CGContextSetInterpolationQuality(ctx, kCGInterpolationHigh);
        CGContextDrawImage(ctx, CGRectMake(0, 0, w, h), [img CGImage]);
        CGImageRef cg = CGBitmapContextCreateImage(ctx);
        CGContextRelease(ctx);
        UIImage *scaled = [UIImage imageWithCGImage:cg];
        CGImageRelease(cg);
        GLUIImage *result = [[GLUIImage alloc] initWithImage:scaled];
        return (JAVA_LONG) (intptr_t) result;
    }
}

void com_codename1_impl_ios_IOSNative_nativeDrawImageGlobal___long_int_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_INT renderingHints) {
    @autoreleasepool {
        GLUIImage *g = (GLUIImage *) (intptr_t) peer;
        if (g == nil) {
            return;
        }
        DrawImage *d = [[DrawImage alloc] initWithArgs:alpha xpos:x ypos:y i:g w:w h:h];
        [[CodenameOne_GLViewController instance] upcomingAdd:d];
        [d release];
    }
}

void com_codename1_impl_ios_IOSNative_nativeTileImageGlobal___long_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    @autoreleasepool {
        GLUIImage *g = (GLUIImage *) (intptr_t) peer;
        if (g == nil) {
            return;
        }
        TileImage *t = [[TileImage alloc] initWithArgs:alpha xpos:x ypos:y i:g w:w h:h];
        [[CodenameOne_GLViewController instance] upcomingAdd:t];
        [t release];
    }
}

void com_codename1_impl_ios_IOSNative_releasePeer___long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    @autoreleasepool {
        NSObject *o = (NSObject *) (intptr_t) peer;
        [o release];
    }
}

/* ---- mutable images (CoreGraphics-backed) -----------------------------------
 * A mutable image owns a CGBitmapContext with a top-left-origin CTM; mutable
 * draw natives render into it immediately (no op queue - the context is
 * CPU-side and independent of the screen pipeline). mutableFinishSim
 * snapshots the bitmap into the inherited GLUIImage, whose setImage:
 * invalidates any cached textures. Powers Image.createImage(w,h) and with it
 * the form transition buffers.
 */

@interface CN1SimMutableImage : GLUIImage {
@public
    CGContextRef mctx;
    int mw, mh;
}
@end

@implementation CN1SimMutableImage

- (void)dealloc {
    if (mctx != NULL) {
        CGContextRelease(mctx);
    }
    [super dealloc];
}

@end

static CN1SimMutableImage *cn1simMutable(JAVA_LONG peer) {
    NSObject *o = (NSObject *) (intptr_t) peer;
    if (![o isKindOfClass:[CN1SimMutableImage class]]) {
        return nil;
    }
    return (CN1SimMutableImage *) o;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createMutableImageSim___int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT w, JAVA_INT h, JAVA_INT argb) {
    @autoreleasepool {
        if (w <= 0 || h <= 0) {
            return 0;
        }
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, w, h, 8, (size_t) w * 4, cs,
                kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
        CGColorSpaceRelease(cs);
        if (ctx == NULL) {
            return 0;
        }
        /* top-left origin for CN1 coordinates */
        CGContextTranslateCTM(ctx, 0, h);
        CGContextScaleCTM(ctx, 1, -1);
        /* base graphics state - clip changes restore to this level */
        CGContextSaveGState(ctx);
        CGFloat a = ((argb >> 24) & 0xff) / 255.0;
        CGContextSetRGBFillColor(ctx,
                ((argb >> 16) & 0xff) / 255.0,
                ((argb >> 8) & 0xff) / 255.0,
                (argb & 0xff) / 255.0, a);
        CGContextFillRect(ctx, CGRectMake(0, 0, w, h));
        CGImageRef snap = CGBitmapContextCreateImage(ctx);
        UIImage *ui = [UIImage imageWithCGImage:snap];
        CGImageRelease(snap);
        CN1SimMutableImage *m = [[CN1SimMutableImage alloc] initWithImage:ui];
        m->mctx = ctx;
        m->mw = w;
        m->mh = h;
        return (JAVA_LONG) (intptr_t) m;
    }
}

void com_codename1_impl_ios_IOSNative_mutableClipSim___long_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1SimMutableImage *m = cn1simMutable(peer);
    if (m == nil) {
        return;
    }
    /* back to the base state, then apply the new clip */
    CGContextRestoreGState(m->mctx);
    CGContextSaveGState(m->mctx);
    CGContextClipToRect(m->mctx, CGRectMake(x, y, MAX(0, w), MAX(0, h)));
}

void com_codename1_impl_ios_IOSNative_mutableFillRectSim___long_int_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_INT color, JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1SimMutableImage *m = cn1simMutable(peer);
    if (m == nil) {
        return;
    }
    CGContextSetRGBFillColor(m->mctx,
            ((color >> 16) & 0xff) / 255.0, ((color >> 8) & 0xff) / 255.0,
            (color & 0xff) / 255.0, alpha / 255.0);
    CGContextFillRect(m->mctx, CGRectMake(x, y, w, h));
}

void com_codename1_impl_ios_IOSNative_mutableDrawLineSim___long_int_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_INT color, JAVA_INT alpha, JAVA_INT x1, JAVA_INT y1, JAVA_INT x2, JAVA_INT y2) {
    CN1SimMutableImage *m = cn1simMutable(peer);
    if (m == nil) {
        return;
    }
    CGContextSetRGBStrokeColor(m->mctx,
            ((color >> 16) & 0xff) / 255.0, ((color >> 8) & 0xff) / 255.0,
            (color & 0xff) / 255.0, alpha / 255.0);
    CGContextSetLineWidth(m->mctx, 1);
    CGContextBeginPath(m->mctx);
    CGContextMoveToPoint(m->mctx, x1 + 0.5, y1 + 0.5);
    CGContextAddLineToPoint(m->mctx, x2 + 0.5, y2 + 0.5);
    CGContextStrokePath(m->mctx);
}

void com_codename1_impl_ios_IOSNative_mutableDrawStringSim___long_long_int_int_java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_LONG fontPeer, JAVA_INT color, JAVA_INT alpha, JAVA_OBJECT str,
        JAVA_INT x, JAVA_INT y) {
    @autoreleasepool {
        CN1SimMutableImage *m = cn1simMutable(peer);
        NSString *s = toNSString(threadStateData, str);
        NSFont *font = (NSFont *) (intptr_t) fontPeer;
        if (m == nil || s == nil || font == nil) {
            return;
        }
        NSGraphicsContext *old = [NSGraphicsContext currentContext];
        NSGraphicsContext *gc = [NSGraphicsContext graphicsContextWithCGContext:m->mctx
                                                                         flipped:YES];
        [NSGraphicsContext setCurrentContext:gc];
        NSColor *c = [NSColor colorWithCalibratedRed:((color >> 16) & 0xff) / 255.0
                                               green:((color >> 8) & 0xff) / 255.0
                                                blue:(color & 0xff) / 255.0
                                               alpha:alpha / 255.0];
        [s drawAtPoint:NSMakePoint(x, y)
        withAttributes:@{NSFontAttributeName : font, NSForegroundColorAttributeName : c}];
        [NSGraphicsContext setCurrentContext:old];
    }
}

void com_codename1_impl_ios_IOSNative_mutableDrawImageSim___long_long_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_LONG imgPeer, JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    @autoreleasepool {
        CN1SimMutableImage *m = cn1simMutable(peer);
        GLUIImage *src = (GLUIImage *) (intptr_t) imgPeer;
        if (m == nil || src == nil) {
            return;
        }
        CGImageRef cg = [[src getImage] CGImage];
        if (cg == NULL) {
            return;
        }
        CGContextSaveGState(m->mctx);
        /* CGContextDrawImage is bottom-up; locally unflip around the rect */
        CGContextTranslateCTM(m->mctx, x, y + h);
        CGContextScaleCTM(m->mctx, 1, -1);
        CGContextSetAlpha(m->mctx, alpha / 255.0);
        CGContextDrawImage(m->mctx, CGRectMake(0, 0, w, h), cg);
        CGContextRestoreGState(m->mctx);
    }
}

void com_codename1_impl_ios_IOSNative_mutableShapeSim___long_byte_1ARRAY_int_float_1ARRAY_int_int_int_boolean_float_int_int_float(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer,
        JAVA_OBJECT commands, JAVA_INT commandsLen, JAVA_OBJECT points, JAVA_INT pointsLen,
        JAVA_INT color, JAVA_INT alpha, JAVA_BOOLEAN stroke, JAVA_FLOAT lineWidth,
        JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT miterLimit) {
    CN1SimMutableImage *m = cn1simMutable(peer);
    if (m == nil) {
        return;
    }
    const JAVA_ARRAY_BYTE *cmd = (const JAVA_ARRAY_BYTE *) ((JAVA_ARRAY) commands)->data;
    const JAVA_ARRAY_FLOAT *pts = (const JAVA_ARRAY_FLOAT *) ((JAVA_ARRAY) points)->data;
    CGContextBeginPath(m->mctx);
    int p = 0;
    for (int i = 0; i < commandsLen; i++) {
        switch (cmd[i]) {
            case 0:
                CGContextMoveToPoint(m->mctx, pts[p], pts[p + 1]);
                p += 2;
                break;
            case 1:
                CGContextAddLineToPoint(m->mctx, pts[p], pts[p + 1]);
                p += 2;
                break;
            case 2:
                CGContextAddQuadCurveToPoint(m->mctx, pts[p], pts[p + 1], pts[p + 2], pts[p + 3]);
                p += 4;
                break;
            case 3:
                CGContextAddCurveToPoint(m->mctx, pts[p], pts[p + 1], pts[p + 2], pts[p + 3],
                        pts[p + 4], pts[p + 5]);
                p += 6;
                break;
            case 4:
                CGContextClosePath(m->mctx);
                break;
            default:
                break;
        }
    }
    if (stroke) {
        CGContextSetRGBStrokeColor(m->mctx,
                ((color >> 16) & 0xff) / 255.0, ((color >> 8) & 0xff) / 255.0,
                (color & 0xff) / 255.0, alpha / 255.0);
        CGContextSetLineWidth(m->mctx, MAX(0.5f, lineWidth));
        CGContextSetLineCap(m->mctx, capStyle == 1 ? kCGLineCapRound
                : capStyle == 2 ? kCGLineCapSquare : kCGLineCapButt);
        CGContextSetLineJoin(m->mctx, joinStyle == 1 ? kCGLineJoinRound
                : joinStyle == 2 ? kCGLineJoinBevel : kCGLineJoinMiter);
        CGContextSetMiterLimit(m->mctx, miterLimit);
        CGContextStrokePath(m->mctx);
    } else {
        CGContextSetRGBFillColor(m->mctx,
                ((color >> 16) & 0xff) / 255.0, ((color >> 8) & 0xff) / 255.0,
                (color & 0xff) / 255.0, alpha / 255.0);
        CGContextFillPath(m->mctx);
    }
}

void com_codename1_impl_ios_IOSNative_mutableFinishSim___long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    @autoreleasepool {
        CN1SimMutableImage *m = cn1simMutable(peer);
        if (m == nil) {
            return;
        }
        CGImageRef snap = CGBitmapContextCreateImage(m->mctx);
        if (snap != NULL) {
            /* setImage: also invalidates any cached textures */
            [m setImage:[UIImage imageWithCGImage:snap]];
            CGImageRelease(snap);
        }
    }
}
