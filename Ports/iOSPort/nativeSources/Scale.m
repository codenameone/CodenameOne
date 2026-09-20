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
#import "Scale.h"
#import "ClipRect.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif
#include "TargetConditionals.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif

float currentScaleX = 1;
float currentScaleY = 1;

@implementation Scale

-(id)initWithArgs:(float)xx yy:(float)yy {
    x = xx;
    y = yy;
    return self;
}

#if TARGET_OS_WATCH
-(void)execute {
    CN1CGScale(x, y);
    currentScaleX = x;
    currentScaleY = y;
}
#else
-(void)execute {
#ifdef CN1_USE_METAL
    {
        // CN1MetalSetTransform(CN1MetalGetTransform() * scale(x, y, 0)),
        // written out: CN1Matrix4 is a plain struct with no math library
        // behind it.
        CN1Matrix4 cur = CN1MetalGetTransform();
        // m' = cur * diag(x, y, 0, 1). Column-major: scaling the i-th
        // column of `cur` by the i-th diagonal entry of the scale matrix.
        CN1Matrix4 result;
        for (int i = 0; i < 4; i++) {
            result.m[0*4 + i] = cur.m[0*4 + i] * x;
            result.m[1*4 + i] = cur.m[1*4 + i] * y;
            result.m[2*4 + i] = cur.m[2*4 + i] * 0;
            result.m[3*4 + i] = cur.m[3*4 + i];
        }
        CN1MetalSetTransform(result);
        currentScaleX = x;
        currentScaleY = y;
        return;
    }
#endif // CN1_USE_METAL
}
#endif // TARGET_OS_WATCH

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"Scale";
}


@end
