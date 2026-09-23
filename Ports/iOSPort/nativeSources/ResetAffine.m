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
#import "CN1RenderBackend.h"
#import "ResetAffine.h"
#import "CodenameOne_GLViewController.h"
#import "SetTransform.h"
#include "xmlvm.h"
#include "TargetConditionals.h"
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
extern float currentScaleX;
extern float currentScaleY;
//extern int effectiveTranslationX;
//extern int effectiveTranslationY;
//extern int currentTranslationX;
//extern int currentTranslationY;

@implementation ResetAffine

-(id)init {
#if !TARGET_OS_WATCH
    // No library supplies an identity constant for CN1Matrix4; write the
    // literal.
    CN1Matrix4 identity = (CN1Matrix4){ { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 } };
    [SetTransform currentTransform:identity];
#endif // !TARGET_OS_WATCH

    return self;
}

#if TARGET_OS_WATCH
-(void)execute {
    CN1CGResetAffine();
    currentScaleX = 1;
    currentScaleY = 1;
}
#else
-(void)execute {
    CN1Matrix4 identity = (CN1Matrix4){ { 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 } };
    SetTransform *f = [[SetTransform alloc] initWithArgs:identity originX:0 originY:0];
    [f execute];
    [f release];
    currentScaleX = 1;
    currentScaleY = 1;
}
#endif // TARGET_OS_WATCH

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif


-(NSString*)getName {
    return @"ResetAffine";
}


@end
