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
#import "DrawGradient.h"
#import "CodenameOne_GLViewController.h"
#import "DrawGradientTextureCache.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif



@implementation DrawGradient

-(id)initWithArgs:(int)typeA startColorA:(int)startColorA endColorA:(int)endColorA xA:(int)xA yA:(int)yA widthA:(int)widthA heightA:(int)heightA relativeXA:(float)relativeXA relativeYA:(float)relativeYA relativeSizeA:(float)relativeSizeA {
    type = typeA;
    startColor = startColorA;
    endColor = endColorA;
    x = xA;
    y = yA;
    width = widthA;
    height = heightA;
    relativeX = relativeXA;
    relativeY = relativeYA;
    relativeSize = relativeSizeA;

    return self;
}
#if TARGET_OS_WATCH
-(void)execute {
    CN1CGGradientRect(type, startColor, endColor, x, y, width, height,
                      relativeX, relativeY, relativeSize);
}
#else
-(void)execute {
#ifdef CN1_USE_METAL
    CN1MetalDrawGradient(type, startColor, endColor, x, y, width, height,
                         relativeX, relativeY, relativeSize);
#endif // CN1_USE_METAL

}
#endif

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawGradient";
}

@end
