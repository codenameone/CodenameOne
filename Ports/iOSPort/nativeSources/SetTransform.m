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
#ifdef CN1_USE_METAL
    // The (xx,yy) origin path is dead (gated by `NO`) and only kept for
    // GL-path parity. Skip the GLKMatrix4* helper calls so the Mac Catalyst
    // slice can compile without GLKit math symbols.
    m = matrix;
#else
    if ( NO && (xx != 0 || yy != 0) ){
        m = GLKMatrix4MakeTranslation(xx, yy, 0);
        m = GLKMatrix4Multiply(m, matrix);
        m = GLKMatrix4Translate(m, -xx, -yy, 0);
    } else {
        m = matrix;
    }
#endif



    //m = GLKMatrix4Translate(matrix, xx, yy, 0);
    currentTransform = m;
    currentTransformInitialized = YES;
    return self;
}

-(void)execute
{
#ifdef CN1_USE_METAL
    CN1MetalSetTransform(m);
#else
    glSetTransformES2(m);
#endif
}

+(GLKMatrix4)currentTransform
{
    if ( !currentTransformInitialized ){
#ifdef CN1_USE_METAL
        currentTransform = (GLKMatrix4){ { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 } };
#else
        currentTransform = GLKMatrix4Identity;
#endif
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
