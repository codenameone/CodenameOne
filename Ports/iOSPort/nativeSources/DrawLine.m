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
#import "DrawLine.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"

@implementation DrawLine

-(id)initWithArgs:(int)c a:(int)a xpos1:(int)xpos1 ypos1:(int)ypos1 xpos2:(int)xpos2 ypos2:(int)ypos2 {
    color = c;
    alpha = a;
    x1 = xpos1;
    x2 = xpos2;
    y1 = ypos1;
    y2 = ypos2;
    return self;
}

-(void)execute {
    GlColorFromRGB(color, alpha);
    GLfloat vertexes[] = {
        x1, y1,
        x2, y2,
    };
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glVertexPointer(2, GL_FLOAT, 0, vertexes);
    GLErrorLog;
    _glDrawArrays(GL_LINES, 0, 2);
    GLErrorLog;
    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
}

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawLine";
}


@end
