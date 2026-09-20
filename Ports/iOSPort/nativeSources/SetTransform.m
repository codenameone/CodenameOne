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
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif
#import "CodenameOne_GLViewController.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif

static CN1Matrix4 currentTransform;
static BOOL currentTransformInitialized = NO;
@implementation SetTransform

-(id)initWithArgs:(CN1Matrix4)matrix originX:(int)xx originY:(int)yy
{
    // The (xx,yy) origin is ignored: the translate-around-origin path was
    // dead (gated by `NO`) for as long as this op has existed.
    m = matrix;
    currentTransform = m;
    currentTransformInitialized = YES;
    return self;
}

-(void)execute
{
#if TARGET_OS_WATCH
    // Column-major CN1Matrix4 -> CG affine 2x3 submatrix.
    CN1CGSetAffine(m.m[0], m.m[1], m.m[4], m.m[5], m.m[12], m.m[13], 0, 0);
#elif defined(CN1_USE_METAL)
    CN1MetalSetTransform(m);
#endif
}

+(CN1Matrix4)currentTransform
{
    if ( !currentTransformInitialized ){
        currentTransform = (CN1Matrix4){ { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 } };
    }
    return currentTransform;
}

+(void)currentTransform:(CN1Matrix4)matrix
{
    currentTransform = matrix;
    currentTransformInitialized = YES;
}

@end
