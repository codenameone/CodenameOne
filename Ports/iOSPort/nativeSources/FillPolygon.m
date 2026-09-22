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
//
//  FillPolygon.m
//  HelloWorldCN1
//
//  Created by Steve Hannah on 2014-06-03.
//
//

#import "FillPolygon.h"
#import "CN1RenderBackend.h"
#import "xmlvm.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif




@implementation FillPolygon
-(id)initWithArgs:(JAVA_FLOAT*)xCoords y:(JAVA_FLOAT*)yCoords num:(int)num color:(int)theColor alpha:(int)theAlpha
{
    color = theColor;
    alpha = theAlpha;
    size_t size = sizeof(JAVA_FLOAT)*num;
    
    x = malloc(size);
    memcpy(x, xCoords, size);
    y = malloc(size);
    memcpy(y, yCoords, size);
    numPoints = num;
    //CN1Log(@"Num points: %d", numPoints);
    return self;
}
#if TARGET_OS_WATCH
-(void)execute
{
    CN1CGFillPolygon(color, alpha, x, y, numPoints);
}
#else
-(void)execute
{
#ifdef CN1_USE_METAL
    CN1MetalFillPolygon(x, y, numPoints, color, alpha);
#endif // CN1_USE_METAL
}
#endif // TARGET_OS_WATCH

-(void)dealloc
{
    free(x);
    free(y);
}
@end
