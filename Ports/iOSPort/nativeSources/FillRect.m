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
#import "FillRect.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"

@implementation FillRect
-(id)initWithArgs:(int)c a:(int)a xpos:(int)xpos ypos:(int)ypos w:(int)w h:(int)h {
    color = c;
    alpha = a;
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    return self;
}

-(void)execute {
    //[UIColorFromRGB(color, alpha) set];
    //CGContextFillRect(context, CGRectMake(x, y, width, height));    
    GlColorFromRGB(color, alpha);
    GLfloat vertexes[] = {
        x, y,
        x + width, y,
        x, y + height,
        x + width, y + height
    };
    
    GLErrorLog;
    glVertexPointer(2, GL_FLOAT, 0, vertexes);
    glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
}

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"FillRect";
}


@end
