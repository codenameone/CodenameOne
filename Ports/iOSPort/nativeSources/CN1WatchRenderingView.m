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
#import "CN1WatchRenderingView.h"

#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"

@implementation CN1WatchRenderingView

- (id)initWithWidth:(int)w height:(int)h scale:(CGFloat)s {
    self = [super init];
    if (self) {
        scale = s <= 0 ? 1 : s;
        [self allocBitmap:w h:h];
    }
    return self;
}

- (void)allocBitmap:(int)w h:(int)h {
    if (bitmapContext != NULL) {
        CGContextRelease(bitmapContext);
        bitmapContext = NULL;
    }
    bufferWidth = (int)(w * scale);
    bufferHeight = (int)(h * scale);
    if (bufferWidth <= 0 || bufferHeight <= 0) {
        return;
    }
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    bitmapContext = CGBitmapContextCreate(NULL, bufferWidth, bufferHeight, 8,
                                          bufferWidth * 4, cs,
                                          kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(cs);
    if (bitmapContext != NULL) {
        // Render in logical points; the bitmap is scaled to device pixels.
        CGContextScaleCTM(bitmapContext, scale, scale);
    }
}

- (int)logicalWidth {
    return scale > 0 ? (int)(bufferWidth / scale) : bufferWidth;
}

- (int)logicalHeight {
    return scale > 0 ? (int)(bufferHeight / scale) : bufferHeight;
}

- (void)setFramebuffer {
    // Bind the bitmap context to the CG backend for the upcoming op queue.
    CN1CGBeginFrame(bitmapContext, bufferWidth / scale, bufferHeight / scale);
}

- (BOOL)presentFramebuffer {
    CN1CGEndFrame();
    if (bitmapContext == NULL) {
        return NO;
    }
    CGImageRef cgImage = CGBitmapContextCreateImage(bitmapContext);
    if (cgImage == NULL) {
        return NO;
    }
    UIImage *frame = [UIImage imageWithCGImage:cgImage scale:scale orientation:UIImageOrientationUp];
    CGImageRelease(cgImage);
    id<CN1WatchFramePresenter> p = self.presenter;
    if (p != nil) {
        // Hop to the main thread; the host surface (SpriteKit/SwiftUI) must be
        // touched there. The frame UIImage is immutable + retained by the block.
        dispatch_async(dispatch_get_main_queue(), ^{
            [p presentWatchFrame:frame];
        });
    }
    return YES;
}

- (UIImage *)currentFrame {
    if (bitmapContext == NULL) {
        return nil;
    }
    CGImageRef cgImage = CGBitmapContextCreateImage(bitmapContext);
    if (cgImage == NULL) {
        return nil;
    }
    UIImage *frame = [UIImage imageWithCGImage:cgImage scale:scale orientation:UIImageOrientationUp];
    CGImageRelease(cgImage);
    return frame;
}

- (void)deleteFramebuffer {
    if (bitmapContext != NULL) {
        CGContextRelease(bitmapContext);
        bitmapContext = NULL;
    }
}

- (void)updateFrameBufferSize:(int)w h:(int)h {
    [self allocBitmap:w h:h];
}

// No native peer components on watchOS; accept and ignore.
- (void)addPeerComponent:(id)view {
}

- (void)keyboardDoneClicked {
}

- (void)keyboardNextClicked {
}

- (void)textFieldDidChange {
}

- (void)dealloc {
    [self deleteFramebuffer];
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}

@end

#endif // TARGET_OS_WATCH
