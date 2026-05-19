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
#import "SetTransform.h"
#ifdef USE_ES2
#import <GLKit/GLKit.h>
#import "CodenameOne_GLViewController.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif

static GLKMatrix4 currentTransform;
static BOOL currentTransformInitialized = NO;
@implementation SetTransform

-(id)initWithArgs:(GLKMatrix4)matrix originX:(int)xx originY:(int)yy
{
    if ( NO && (xx != 0 || yy != 0) ){
        m = GLKMatrix4MakeTranslation(xx, yy, 0);
        m = GLKMatrix4Multiply(m, matrix);
        m = GLKMatrix4Translate(m, -xx, -yy, 0);
    } else {
        m = matrix;
    }
    
    
    
    //m = GLKMatrix4Translate(matrix, xx, yy, 0);
    currentTransform = m;
    currentTransformInitialized = YES;
    return self; 
}

extern int CN1DbgRemainingOps;
extern int CN1DbgPolygonClipSeq;

-(void)execute
{
#ifdef CN1_USE_METAL
    CN1MetalSetTransform(m);
#else
    if (CN1DbgRemainingOps > 0) {
        NSLog(@"CN1SS:DBG SetTransform.exec polySeq=%d remaining=%d m=[%f %f %f %f / %f %f %f %f / %f %f %f %f / %f %f %f %f]",
              CN1DbgPolygonClipSeq, CN1DbgRemainingOps,
              m.m[0], m.m[1], m.m[2], m.m[3],
              m.m[4], m.m[5], m.m[6], m.m[7],
              m.m[8], m.m[9], m.m[10], m.m[11],
              m.m[12], m.m[13], m.m[14], m.m[15]);
        CN1DbgRemainingOps--;
    }
    glSetTransformES2(m);
#endif
}

+(GLKMatrix4)currentTransform
{
    if ( !currentTransformInitialized ){
        currentTransform = GLKMatrix4Identity;
    }
    return currentTransform;
}

+(void)currentTransform:(GLKMatrix4)matrix
{
    currentTransform = matrix;
    currentTransformInitialized = YES;
}

@end
#endif
