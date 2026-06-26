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
 * Implementation of the UIKit shim (see sim-include/UIKit/UIKit.h):
 * UIImage over CGImage, the NSFont/NSString categories, and the UIGraphics
 * bitmap-context stack. Compile with -fno-objc-arc (matches the port
 * sources).
 */
#import <UIKit/UIKit.h>

/* ---- UIImage ------------------------------------------------------------- */

@implementation UIImage

+ (UIImage *)imageWithCGImage:(CGImageRef)img {
    return [[[UIImage alloc] initWithCGImage:img] autorelease];
}

+ (UIImage *)imageWithCGImage:(CGImageRef)img scale:(CGFloat)scale orientation:(NSInteger)orientation {
    UIImage *u = [[[UIImage alloc] initWithCGImage:img] autorelease];
    if (u != nil) {
        u->imgScale = scale;
    }
    return u;
}

- (id)initWithCGImage:(CGImageRef)img {
    self = [super init];
    if (self != nil) {
        cgImage = CGImageRetain(img);
        imgScale = 1.0;
    }
    return self;
}

- (CGImageRef)CGImage {
    return cgImage;
}

- (CGSize)size {
    if (cgImage == NULL) {
        return CGSizeZero;
    }
    return CGSizeMake(CGImageGetWidth(cgImage) / imgScale, CGImageGetHeight(cgImage) / imgScale);
}

- (CGFloat)scale {
    return imgScale;
}

- (void)dealloc {
    if (cgImage != NULL) {
        CGImageRelease(cgImage);
        cgImage = NULL;
    }
    [super dealloc];
}

@end

/* ---- NSFont category ------------------------------------------------------ */

@implementation NSFont (CN1SimUIFont)

- (NSFont *)fontWithSize:(CGFloat)size {
    NSFont *f = [NSFont fontWithName:[self fontName] size:size];
    if (f == nil) {
        f = [NSFont systemFontOfSize:size];
    }
    return f;
}

@end

/* ---- NSString measurement -------------------------------------------------- */

@implementation NSString (CN1SimUIFont)

- (CGSize)sizeWithFont:(NSFont *)font {
    if (font == nil) {
        return CGSizeZero;
    }
    NSSize s = [self sizeWithAttributes:@{NSFontAttributeName : font}];
    return CGSizeMake(ceil(s.width), ceil(s.height));
}

- (CGSize)sizeWithFont:(NSFont *)font constrainedToSize:(CGSize)size {
    CGSize s = [self sizeWithFont:font];
    if (s.width > size.width) {
        s.width = size.width;
    }
    if (s.height > size.height) {
        s.height = size.height;
    }
    return s;
}

@end

/* ---- UIGraphics context stack ---------------------------------------------- */

static NSMutableArray *cn1simContextStack = nil;
static NSMutableArray *cn1simBitmapStack = nil;

void UIGraphicsPushContext(CGContextRef context) {
    if (cn1simContextStack == nil) {
        cn1simContextStack = [[NSMutableArray alloc] init];
    }
    NSGraphicsContext *prev = [NSGraphicsContext currentContext];
    [cn1simContextStack addObject:(prev != nil ? (id) prev : (id) [NSNull null])];
    NSGraphicsContext *gc = [NSGraphicsContext graphicsContextWithCGContext:context flipped:NO];
    [NSGraphicsContext setCurrentContext:gc];
}

void UIGraphicsPopContext(void) {
    if (cn1simContextStack == nil || [cn1simContextStack count] == 0) {
        return;
    }
    id prev = [cn1simContextStack lastObject];
    [cn1simContextStack removeLastObject];
    [NSGraphicsContext setCurrentContext:(prev == [NSNull null] ? nil : (NSGraphicsContext *) prev)];
}

void UIGraphicsBeginImageContextWithOptions(CGSize size, BOOL opaque, CGFloat scale) {
    if (scale <= 0) {
        scale = 1.0;
    }
    size_t w = (size_t) (size.width * scale);
    size_t h = (size_t) (size.height * scale);
    if (w == 0) {
        w = 1;
    }
    if (h == 0) {
        h = 1;
    }
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef ctx = CGBitmapContextCreate(NULL, w, h, 8, 0, cs,
            kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(cs);
    CGContextScaleCTM(ctx, scale, scale);
    if (cn1simBitmapStack == nil) {
        cn1simBitmapStack = [[NSMutableArray alloc] init];
    }
    [cn1simBitmapStack addObject:@[[NSValue valueWithPointer:ctx], @(scale)]];
    UIGraphicsPushContext(ctx);
}

void UIGraphicsBeginImageContext(CGSize size) {
    UIGraphicsBeginImageContextWithOptions(size, NO, 1.0);
}

CGContextRef UIGraphicsGetCurrentContext(void) {
    NSGraphicsContext *gc = [NSGraphicsContext currentContext];
    return gc != nil ? [gc CGContext] : NULL;
}

UIImage *UIGraphicsGetImageFromCurrentImageContext(void) {
    if (cn1simBitmapStack == nil || [cn1simBitmapStack count] == 0) {
        return nil;
    }
    NSArray *top = [cn1simBitmapStack lastObject];
    CGContextRef ctx = (CGContextRef) [(NSValue *) top[0] pointerValue];
    CGFloat scale = [(NSNumber *) top[1] doubleValue];
    CGImageRef img = CGBitmapContextCreateImage(ctx);
    UIImage *u = [UIImage imageWithCGImage:img scale:scale orientation:UIImageOrientationUp];
    CGImageRelease(img);
    return u;
}

void UIGraphicsEndImageContext(void) {
    if (cn1simBitmapStack == nil || [cn1simBitmapStack count] == 0) {
        return;
    }
    NSArray *top = [cn1simBitmapStack lastObject];
    CGContextRef ctx = (CGContextRef) [(NSValue *) top[0] pointerValue];
    [cn1simBitmapStack removeLastObject];
    UIGraphicsPopContext();
    CGContextRelease(ctx);
}
