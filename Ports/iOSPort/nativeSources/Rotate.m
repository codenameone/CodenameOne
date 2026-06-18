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
#import "Rotate.h"
#import "ClipRect.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"
#ifdef USE_ES2
#import "SetTransform.h"
#endif
#include "TargetConditionals.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif

float currentRotate = 1;
float currentRotateX = 1;
float currentRotateY = 1;

@implementation Rotate

-(id)initWithArgs:(float)ang xx:(int)xx yy:(int)yy {
    x = xx;
    y = yy;
    angle = ang;
#if !TARGET_OS_WATCH
#ifdef USE_ES2
    m = [SetTransform currentTransform];
    float a = angle * M_PI / 180.0;
#ifdef CN1_USE_METAL
    // GLKMatrix4Translate / GLKMatrix4Rotate are inline GLKit math
    // helpers that the Mac Catalyst stub headers don't provide. Inline
    // the equivalent column-major matrix multiplies so the math is
    // identical to the GL path without depending on GLKit symbols.
    // Step 1: m = m * Translate(x, y, 0)
    //   New columns: c0,c1,c2 unchanged; c3' = c3 + x*c0 + y*c1.
    {
        GLKMatrix4 r = m;
        for (int i = 0; i < 4; i++) {
            r.m[3*4 + i] = m.m[3*4 + i] + ((float)x) * m.m[0*4 + i]
                                        + ((float)y) * m.m[1*4 + i];
        }
        m = r;
    }
    // Step 2: m = m * Rotate(a, 0, 0, 1). Z-axis rotation: only the
    //   first two columns rotate; columns 2 and 3 are unchanged.
    //   c0' =  c0*cos(a) + c1*sin(a)
    //   c1' = -c0*sin(a) + c1*cos(a)
    {
        float ca = cosf(a);
        float sa = sinf(a);
        GLKMatrix4 r = m;
        for (int i = 0; i < 4; i++) {
            r.m[0*4 + i] =  m.m[0*4 + i] * ca + m.m[1*4 + i] * sa;
            r.m[1*4 + i] = -m.m[0*4 + i] * sa + m.m[1*4 + i] * ca;
        }
        m = r;
    }
    // Step 3: m = m * Translate(-x, -y, 0). Same as step 1, signs flipped.
    {
        GLKMatrix4 r = m;
        for (int i = 0; i < 4; i++) {
            r.m[3*4 + i] = m.m[3*4 + i] + (-(float)x) * m.m[0*4 + i]
                                        + (-(float)y) * m.m[1*4 + i];
        }
        m = r;
    }
#else
    m = GLKMatrix4Translate(m, x, y, 0);
    m = GLKMatrix4Rotate(m, a, 0, 0, 1);
    m = GLKMatrix4Translate(m, -x, -y, 0);
#endif

    [SetTransform currentTransform:m];

#endif
#endif // !TARGET_OS_WATCH
    return self;
}

#if TARGET_OS_WATCH
-(void)execute {
    CN1CGRotate((CGFloat)(angle * M_PI / 180.0), x, y);
    currentRotateX = x;
    currentRotateY = y;
    currentRotate = angle;
}
#else
-(void)execute {
#ifndef USE_ES2
    _glTranslatef(x, y, 0);
    _glRotatef(angle, 0, 0, 1);
    _glTranslatef(-x, -y, 0);

    GLErrorLog;
#else
    SetTransform *f = [[SetTransform alloc] initWithArgs:m originX:0 originY:0];
    [f execute];
    [f release];
#endif
    currentRotateX = x;
    currentRotateY = y;
    currentRotate = angle;
}
#endif // TARGET_OS_WATCH

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"Rotate";
}


@end
