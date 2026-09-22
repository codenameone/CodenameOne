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
#import "TileImage.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif



float* createVertexArray(int x, int y, int imageWidth, int imageHeight) {
    float* vtx = malloc(8 * sizeof(float));
    int w = nextPowerOf2(imageWidth);
    int h = nextPowerOf2(imageHeight);
    vtx[0] = x;
    vtx[1] = y;
    vtx[2] = x + w;
    vtx[3] = y;
    vtx[4] = x;
    vtx[5] = y + h;
    vtx[6] = x + w;
    vtx[7] = y + h;
    return vtx;
    
}

@implementation TileImage
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
#if TARGET_OS_WATCH
-(void)execute {
    if (width <= 0 || height <= 0) {
        return;
    }
    CN1Image *src = [img getImage];
    if (src == nil || src.CGImage == NULL) {
        return;
    }
    CN1CGTileImage(src.CGImage, alpha, x, y, width, height);
}
#else
-(void)execute {
    if (width <= 0 || height <= 0) {
        return;
    }
#ifdef CN1_USE_METAL
    {
        CN1Image *src = [img getImage];
        int imageWidth = (int)src.size.width;
        int imageHeight = (int)src.size.height;
        if (imageWidth > 0 && imageHeight > 0) {
            CN1MetalTileImage([img getMTLTexture], alpha, x, y, width, height, imageWidth, imageHeight);
        }
        return;
    }
#endif // CN1_USE_METAL

}

#endif

#ifndef CN1_USE_ARC
-(void)dealloc {
    [img release];
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"TileImage";
}

@end
