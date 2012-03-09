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
#import "ResetAffine.h"
#import "CodenameOne_GLViewController.h"

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl();

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl();
extern float currentScaleX;
extern float currentScaleY;

@implementation ResetAffine

-(id)init {
    return self;
}

-(void)execute {
    glLoadIdentity();
    GLErrorLog;
    glOrthof(0, Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl(), 0, Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl(), -1, 1);
    GLErrorLog;
    glMatrixMode(GL_MODELVIEW);
    GLErrorLog;
    glLoadIdentity();
    GLErrorLog;
    glScalef(1, -1, 1);
    GLErrorLog;
    glTranslatef(0, -Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl(), 0);
    GLErrorLog;
    currentScaleX = 1;
    currentScaleY = 1;
}

-(void)dealloc {
	[super dealloc];
}


-(NSString*)getName {
    return @"ResetAffine";
}


@end
