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
#import "DrawImage.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif



@implementation DrawImage
-(id)initWithArgs:(int)a xpos:(int)xpos ypos:(int)ypos i:(GLUIImage*)i w:(int)w h:(int)h {
    alpha = a;
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    img = i;
#ifndef CN1_USE_ARC
    [img retain];
#endif
    return self;
}
-(void)setCornerRadius:(float)r {
    cornerRadius = r;
}

#if TARGET_OS_WATCH
-(void)execute {
    CN1Image *src = [img getImage];
    if (src == nil || src.CGImage == NULL) {
        return;
    }
    CN1CGDrawImage(src.CGImage, alpha, x, y, width, height);
}
#else
-(void)execute {
#ifdef CN1_USE_METAL
    if (cornerRadius > 0.0f) {
        CN1MetalDrawImageRounded([img getMTLTexture], alpha, x, y, width, height, cornerRadius);
    } else {
        CN1MetalDrawImage([img getMTLTexture], alpha, x, y, width, height);
    }
#endif // CN1_USE_METAL

}

#endif
-(void)dealloc {
#ifndef CN1_USE_ARC
    [img release];
    [super dealloc];
#endif
}

-(NSString*)getName {
    return @"DrawImage";
}

-(void)setRenderingHints:(JAVA_INT)hints {
    renderingHints = hints;
}

// See Graphics.RENDERING_HINT_FAST
-(BOOL)useFastRendering {
    return (renderingHints & 1) == 1;
}

@end
