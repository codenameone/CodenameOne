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
#ifndef CN1AppleUI_h
#define CN1AppleUI_h

#import <TargetConditionals.h>

/// The platform's UI framework, whichever that is.
///
/// The Apple native layer is shared between the iOS port and the native macOS
/// one, and most of it needs the UI framework only for a handful of types --
/// an image, a font, a colour, a view to hang a peer component off. This header
/// is the one place that decides which framework supplies them, so a shared
/// source can say `#import <UIKit/UIKit.h>` and compile on both.
///
/// It deliberately does NOT alias UIKit types to AppKit ones. `NSImage` is a
/// resolution-independent container of representations rather than a bitmap, and
/// `NSView` is not flipped by default, so a blanket typedef would produce code
/// that compiles and then behaves differently in ways nothing would catch. The
/// portable currency between the two is CoreGraphics -- `CGImageRef`,
/// `CGColorRef`, `CTFontRef` -- which is identical on both platforms. Sites that
/// need more than that are converted individually.
#if TARGET_OS_OSX
#import <AppKit/AppKit.h>
#import <CoreImage/CoreImage.h>
#else
#import <UIKit/UIKit.h>
#endif

/// Aliases for the handful of framework types that appear in shared *signatures*.
///
/// These exist so a declaration shared by both ports can name a type at all.
/// They are emphatically not permission to treat the two as interchangeable: an
/// implementation that asks a CN1Image for "its" bitmap has to go through
/// CGImageRef, because NSImage holds representations rather than pixels and can
/// answer that question more than one way. The alias makes the header compile;
/// the conversion at each use site is still work, and skipping it produces code
/// that builds and then behaves differently with nothing to catch it.
#if TARGET_OS_OSX
typedef NSImage CN1Image;
typedef NSFont CN1Font;
typedef NSView CN1View;
#else
typedef UIImage CN1Image;
typedef UIFont CN1Font;
typedef UIView CN1View;
#endif

#if TARGET_OS_OSX
/// UIKit drawing helpers that AppKit spells differently but means identically.
///
/// These are real ports, not stubs. `UIGraphicsGetCurrentContext` and
/// `[NSGraphicsContext currentContext].CGContext` are the same CoreGraphics
/// context reached through each framework's own accessor, and `UIRectClip` is
/// `CGContextClipToRect` on that context. The shared mutable-image drawing path
/// uses them on every fill and stroke, so guarding it out would take the
/// software rasteriser with it.
static inline CGContextRef UIGraphicsGetCurrentContext(void) {
    return [[NSGraphicsContext currentContext] CGContext];
}

static inline void UIRectClip(CGRect rect) {
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    if (ctx != NULL) {
        CGContextClipToRect(ctx, rect);
    }
}
/// The image-context family. AppKit's NSGraphicsContext is the same idea with a
/// different shape -- it is pushed and popped rather than begun and ended -- so
/// these keep a small stack of their own to preserve the UIKit call pattern the
/// shared drawing code is written against.
static inline void UIGraphicsBeginImageContextWithOptions(CGSize size, BOOL opaque, CGFloat scale) {
    // The graphics state is saved unconditionally, before anything that can
    // fail. UIGraphicsEndImageContext always restores, so an early return that
    // skipped the save would leave the stack unbalanced -- and AppKit does not
    // tolerate an unmatched pop, it traps. A zero-sized image is not a
    // theoretical case: the framework asks for one whenever a component has
    // been created but not yet laid out.
    [NSGraphicsContext saveGraphicsState];
    CGFloat s = scale > 0 ? scale : 1.0;
    size_t w = (size_t)(size.width * s), h = (size_t)(size.height * s);
    if (w == 0 || h == 0) { return; }
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGContextRef ctx = CGBitmapContextCreate(NULL, w, h, 8, 0, cs,
            (opaque ? kCGImageAlphaNoneSkipFirst : kCGImageAlphaPremultipliedFirst)
            | kCGBitmapByteOrder32Little);
    CGColorSpaceRelease(cs);
    if (ctx == NULL) { return; }
    CGContextScaleCTM(ctx, s, s);
    // Flip to UIKit's top-left origin, which is what the drawing code assumes.
    CGContextTranslateCTM(ctx, 0, size.height);
    CGContextScaleCTM(ctx, 1, -1);
    [NSGraphicsContext setCurrentContext:
        [NSGraphicsContext graphicsContextWithCGContext:ctx flipped:YES]];
    CGContextRelease(ctx);
}

static inline CN1Image * _Nullable UIGraphicsGetImageFromCurrentImageContext(void) {
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    // Nil when the context was never created -- a zero-sized image, or an
    // allocation that failed. The UIKit function answers nil there too, so the
    // caller's existing nil check is the one that runs.
    if (ctx == NULL) { return nil; }
    CGImageRef cg = CGBitmapContextCreateImage(ctx);
    if (cg == NULL) { return nil; }
    NSImage *img = [[NSImage alloc] initWithCGImage:cg
                                               size:NSMakeSize(CGImageGetWidth(cg), CGImageGetHeight(cg))];
    CGImageRelease(cg);
    return img;
}

static inline void UIGraphicsEndImageContext(void) {
    [NSGraphicsContext restoreGraphicsState];
}

/*
 * Name-only differences. The constants and the types behind them are identical;
 * only the prefix moved when the API was brought to AppKit, so aliasing them is
 * the whole of the port for the call sites that use them -- unlike UIImage and
 * NSImage, which are genuinely different ideas and are converted per site.
 */
#define UIFontTextStyleBody          NSFontTextStyleBody
#define UIFontTextStyleHeadline      NSFontTextStyleHeadline
#define UIFontTextStyleSubheadline   NSFontTextStyleSubheadline
#define UIFontTextStyleCaption1      NSFontTextStyleCaption1
#define UIFontTextStyleCaption2      NSFontTextStyleCaption2
#define UIFontTextStyleFootnote      NSFontTextStyleFootnote
#define UIFontWeightUltraLight       NSFontWeightUltraLight
#define UIFontWeightThin             NSFontWeightThin
#define UIFontWeightLight            NSFontWeightLight
#define UIFontWeightRegular          NSFontWeightRegular
#define UIFontWeightMedium           NSFontWeightMedium
#define UIFontWeightSemibold         NSFontWeightSemibold
#define UIFontWeightBold             NSFontWeightBold
#define UIFontWeightHeavy            NSFontWeightHeavy
#define UIFontWeightBlack            NSFontWeightBlack

/*
 * Image encoding. UIKit exposes these as free functions and AppKit as a
 * representation object; the conversion is genuinely mechanical -- unlike
 * NSImage vs UIImage themselves, which are different ideas and are converted
 * per call site.
 */
typedef NS_ENUM(NSInteger, UIImageOrientation) {
    UIImageOrientationUp = 0
};

static inline NSBitmapImageRep * _Nullable CN1AppleBitmapRep(NSImage * _Nullable image) {
    if (image == nil) {
        return nil;
    }
    // Asking the image for "its" bitmap has more than one answer, so the CGImage
    // is taken at the image's own size and wrapped, rather than picking one of
    // the representations and hoping it is the right one.
    CGImageRef cg = [image CGImageForProposedRect:NULL context:nil hints:nil];
    if (cg == NULL) {
        return nil;
    }
    NSBitmapImageRep *rep = [[NSBitmapImageRep alloc] initWithCGImage:cg];
    rep.size = image.size;
    return rep;
}

static inline NSData * _Nullable UIImagePNGRepresentation(NSImage * _Nullable image) {
    NSBitmapImageRep *rep = CN1AppleBitmapRep(image);
    return [rep representationUsingType:NSBitmapImageFileTypePNG properties:@{}];
}

static inline NSData * _Nullable UIImageJPEGRepresentation(NSImage * _Nullable image,
                                                           CGFloat quality) {
    NSBitmapImageRep *rep = CN1AppleBitmapRep(image);
    return [rep representationUsingType:NSBitmapImageFileTypeJPEG
                             properties:@{NSImageCompressionFactor: @(quality)}];
}

static inline void UIRectFill(CGRect rect) {
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    if (ctx != NULL) {
        CGContextFillRect(ctx, rect);
    }
}
#endif

#endif
