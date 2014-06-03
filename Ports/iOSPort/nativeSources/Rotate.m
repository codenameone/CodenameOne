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

float currentRotate = 1;
float currentRotateX = 1;
float currentRotateY = 1;

@implementation Rotate

-(id)initWithArgs:(float)ang xx:(int)xx yy:(int)yy {
    x = xx;
    y = yy;
    angle = ang;
#ifdef USE_ES2
    m = [SetTransform currentTransform];
    float a = angle * M_PI / 180.0;
    m = GLKMatrix4Translate(m, x, y, 0);
    m = GLKMatrix4Rotate(m, a, 0, 0, 1);
    m = GLKMatrix4Translate(m, -x, -y, 0);

    [SetTransform currentTransform:m];
    
#endif
    return self;
}

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

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"Rotate";
}


@end
