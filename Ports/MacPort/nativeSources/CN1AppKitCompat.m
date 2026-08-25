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
#import "CN1AppKitCompat.h"
#if TARGET_OS_OSX

CGFloat CN1AppKitBackingScale(NSView *view) {
    NSWindow *w = view != nil ? [view window] : nil;
    if (w != nil) {
        return [w backingScaleFactor];
    }
    NSScreen *screen = [NSScreen mainScreen];
    if (screen != nil) {
        return [screen backingScaleFactor];
    }
    return 1.0;
}

CGImageRef CN1AppKitCGImageFromNSImage(NSImage *image) {
    if (image == nil) {
        return NULL;
    }
    // A rect in the image's own pixel terms rather than its point size, so a
    // 2x representation comes back at its full resolution instead of being
    // halved. Passing NULL for context and hints lets the image pick the
    // representation that best fits.
    NSSize sz = [image size];
    if (sz.width <= 0 || sz.height <= 0) {
        return NULL;
    }
    NSRect rect = NSMakeRect(0, 0, sz.width, sz.height);
    return [image CGImageForProposedRect:&rect context:nil hints:nil];
}

NSImage *CN1AppKitNSImageFromCGImage(CGImageRef cgImage) {
    if (cgImage == NULL) {
        return nil;
    }
    // Sized in pixels, so AppKit treats one image pixel as one point and does
    // not resample for the current display's scale. Callers that want a
    // point-sized image divide by the backing scale themselves, which keeps
    // that decision at the call site where the intent is known.
    size_t w = CGImageGetWidth(cgImage);
    size_t h = CGImageGetHeight(cgImage);
    if (w == 0 || h == 0) {
        return nil;
    }
    return [[NSImage alloc] initWithCGImage:cgImage size:NSMakeSize(w, h)];
}

BOOL CN1AppKitReadARGB(CGImageRef cgImage, unsigned int *argb, int width, int height) {
    if (cgImage == NULL || argb == NULL || width <= 0 || height <= 0) {
        return NO;
    }
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    if (cs == NULL) {
        return NO;
    }
    // kCGImageAlphaPremultipliedFirst with 32-bit-little order gives BGRA in
    // memory, which read back as a host-order 32-bit word is ARGB -- the
    // layout Codename One's int[] pixels use on every other port.
    CGContextRef ctx = CGBitmapContextCreate(argb, (size_t)width, (size_t)height, 8,
                                             (size_t)width * 4, cs,
                                             kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
    CGColorSpaceRelease(cs);
    if (ctx == NULL) {
        return NO;
    }
    CGContextClearRect(ctx, CGRectMake(0, 0, width, height));
    CGContextDrawImage(ctx, CGRectMake(0, 0, width, height), cgImage);
    CGContextRelease(ctx);
    return YES;
}

#endif

NSImage * _Nullable CN1AppKitNSImageFromARGB(const unsigned int * _Nonnull argb,
                                             int width, int height) {
    if (argb == NULL || width <= 0 || height <= 0) {
        return nil;
    }
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
    if (space == NULL) {
        return nil;
    }
    // Little-endian, to match how the pixels are written below: as native
    // uint32 values in ARGB order. Declared big-endian the bytes are read back
    // in the reverse order, which turns an opaque red into a mostly
    // transparent one -- and the only place that shows is a screenshot, where
    // it reads as garbage rather than as an obviously wrong colour. Every
    // other ARGB context in the shared code uses Little for the same reason.
    CGContextRef ctx = CGBitmapContextCreate(NULL, width, height, 8, width * 4, space,
                                             kCGImageAlphaPremultipliedFirst
                                             | kCGBitmapByteOrder32Little);
    CGColorSpaceRelease(space);
    if (ctx == NULL) {
        return nil;
    }
    unsigned int *dest = (unsigned int *)CGBitmapContextGetData(ctx);
    if (dest == NULL) {
        CGContextRelease(ctx);
        return nil;
    }
    // The framework's raster is straight alpha and the context wants it
    // premultiplied; handing it over unconverted makes every partly transparent
    // pixel too bright.
    for (int i = 0; i < width * height; i++) {
        unsigned int p = argb[i];
        unsigned int a = (p >> 24) & 0xff;
        unsigned int r = (p >> 16) & 0xff;
        unsigned int g = (p >> 8) & 0xff;
        unsigned int b = p & 0xff;
        r = (r * a + 127) / 255;
        g = (g * a + 127) / 255;
        b = (b * a + 127) / 255;
        dest[i] = (a << 24) | (r << 16) | (g << 8) | b;
    }
    CGImageRef cgImage = CGBitmapContextCreateImage(ctx);
    CGContextRelease(ctx);
    if (cgImage == NULL) {
        return nil;
    }
    NSImage *image = CN1AppKitNSImageFromCGImage(cgImage);
    CGImageRelease(cgImage);
    return image;
}
