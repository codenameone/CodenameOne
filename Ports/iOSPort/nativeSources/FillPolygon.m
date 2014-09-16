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
//
//  FillPolygon.m
//  HelloWorldCN1
//
//  Created by Steve Hannah on 2014-06-03.
//
//

#import "FillPolygon.h"
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES1/gl.h>
#import "CN1ES2compat.h"
#import "xmlvm.h"

@implementation FillPolygon
-(id)initWithArgs:(JAVA_FLOAT*)xCoords y:(JAVA_FLOAT*)yCoords num:(int)num color:(int)theColor alpha:(int)theAlpha
{
    color = theColor;
    alpha = theAlpha;
    size_t size = sizeof(JAVA_FLOAT)*num;
    
    x = malloc(size);
    memcpy(x, xCoords, size);
    y = malloc(size);
    memcpy(y, yCoords, size);
    numPoints = num;
    //NSLog(@"Num points: %d", numPoints);
    return self;
}
-(void)execute
{
    
    
    GlColorFromRGB(color, alpha);
    GLfloat vertexes[numPoints*2];// = malloc(sizeof(GLfloat)*numPoints*2);
    GLErrorLog;
    int j = 0;
    for ( int i=0; i<numPoints; i++){
        //NSLog(@"Point: %f %f", x[i], y[i]);
        vertexes[j++] = (GLfloat)x[i];
        vertexes[j++] = (GLfloat)y[i];
    }
    
    _glVertexPointer(2, GL_FLOAT, 0, vertexes);
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glDrawArrays(GL_TRIANGLE_FAN, 0, numPoints);
    GLErrorLog;
    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    //free(vertexes);

}

-(void)dealloc
{
    free(x);
    free(y);
}
@end
