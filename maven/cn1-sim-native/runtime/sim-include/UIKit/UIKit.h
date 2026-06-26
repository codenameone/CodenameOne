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
 * UIKit shim for the macOS simulator dylib build.
 *
 * The iOS port's rendering sources (CN1Metalcompat.m, the ExecutableOp
 * subclasses, the glyph atlas) import <UIKit/UIKit.h> but - on the Metal
 * path - only depend on a small UIKit surface: UIFont (font name, point
 * size, metrics, descriptor), UIImage (a CGImage holder) and the
 * UIGraphics bitmap-context helpers. This header maps that surface onto
 * AppKit/CoreGraphics so those files compile UNMODIFIED in a plain AppKit
 * JVM process where the real UIKit cannot be loaded:
 *
 *  - UIFont is NSFont (compatibility alias) + a category adding the few
 *    UIFont-only methods the sources call. NSFontDescriptor is toll-free
 *    bridged to CTFontDescriptorRef, so CTFontCreateWithFontDescriptor in
 *    the glyph atlas works as-is.
 *  - UIColor is NSColor.
 *  - UIImage is a small CGImage wrapper class implemented in
 *    cn1_sim_uikit_compat.m.
 *
 * The simulator build adds this directory (sim-include) to the include
 * path so #import <UIKit/UIKit.h> resolves here instead of the SDK.
 */
#ifndef CN1SIM_UIKIT_SHIM_H
#define CN1SIM_UIKIT_SHIM_H

#import <AppKit/AppKit.h>
#import <CoreGraphics/CoreGraphics.h>
#import <CoreText/CoreText.h>
#import <QuartzCore/QuartzCore.h>

@compatibility_alias UIFont NSFont;
@compatibility_alias UIColor NSColor;

/* UIFont-only API used by the port sources, grafted onto NSFont */
@interface NSFont (CN1SimUIFont)
/* UIFont's -fontWithSize: equivalent */
- (NSFont *)fontWithSize:(CGFloat)size;
@end

/* legacy NSString measurement API used by the port sources */
@interface NSString (CN1SimUIFont)
- (CGSize)sizeWithFont:(NSFont *)font;
- (CGSize)sizeWithFont:(NSFont *)font constrainedToSize:(CGSize)size;
@end

@interface UIImage : NSObject {
    CGImageRef cgImage;
    CGFloat imgScale;
}
@property (nonatomic, readonly) CGImageRef CGImage;
@property (nonatomic, readonly) CGSize size;
@property (nonatomic, readonly) CGFloat scale;
+ (UIImage *)imageWithCGImage:(CGImageRef)img;
+ (UIImage *)imageWithCGImage:(CGImageRef)img scale:(CGFloat)scale orientation:(NSInteger)orientation;
- (id)initWithCGImage:(CGImageRef)img;
@end

/* UIImageOrientation subset (only "up" is ever used by the port sources) */
enum {
    UIImageOrientationUp = 0
};

/* UIGraphics bitmap-context helpers over CGBitmapContext */
#ifdef __cplusplus
extern "C" {
#endif
void UIGraphicsBeginImageContext(CGSize size);
void UIGraphicsBeginImageContextWithOptions(CGSize size, BOOL opaque, CGFloat scale);
CGContextRef UIGraphicsGetCurrentContext(void);
UIImage *UIGraphicsGetImageFromCurrentImageContext(void);
void UIGraphicsEndImageContext(void);
void UIGraphicsPushContext(CGContextRef context);
void UIGraphicsPopContext(void);
#ifdef __cplusplus
}
#endif

/* legacy UIKit line-break aliases (NSLineBreakMode comes from AppKit) */
enum {
    UILineBreakModeWordWrap = 0,
    UILineBreakModeClip = 2
};
typedef NSLineBreakMode UILineBreakMode;

#endif /* CN1SIM_UIKIT_SHIM_H */
