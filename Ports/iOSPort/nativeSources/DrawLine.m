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
#ifdef USE_ES2
extern GLKMatrix4 CN1modelViewMatrix;
extern GLKMatrix4 CN1projectionMatrix;
extern GLKMatrix4 CN1transformMatrix;
extern int CN1modelViewMatrixVersion;
extern int CN1projectionMatrixVersion;
extern int CN1transformMatrixVersion;
extern GLuint CN1activeProgram;
static GLuint program=0;
static GLuint vertexShader;
static GLuint fragmentShader;
static GLuint modelViewMatrixUniform;
static GLuint projectionMatrixUniform;
static GLuint transformMatrixUniform;
static GLuint colorUniform;
static GLuint vertexCoordAtt;
static GLuint textureCoordAtt;
static int currentCN1modelViewMatrixVersion=-1;
static int currentCN1projectionMatrixVersion=-1;
static int currentCN1transformMatrixVersion=-1;

static NSString *fragmentShaderSrc =
@"precision highp float;\n"
"uniform lowp vec4 uColor;\n"

"void main(){\n"
"   gl_FragColor = uColor; \n"
"}\n";

static NSString *vertexShaderSrc =
@"attribute vec4 aVertexCoord;\n"

"uniform mat4 uModelViewMatrix;\n"
"uniform mat4 uProjectionMatrix;\n"
"uniform mat4 uTransformMatrix;\n"

"void main(){\n"
"   gl_Position = uProjectionMatrix *  uModelViewMatrix * uTransformMatrix * aVertexCoord;\n"
"}";

static GLuint getOGLProgram(){
    if ( program == 0  ){
        program = CN1compileShaderProgram(vertexShaderSrc, fragmentShaderSrc);
        GLErrorLog;
        vertexCoordAtt = glGetAttribLocation(program, "aVertexCoord");
        GLErrorLog;
        
        modelViewMatrixUniform = glGetUniformLocation(program, "uModelViewMatrix");
        GLErrorLog;
        projectionMatrixUniform = glGetUniformLocation(program, "uProjectionMatrix");
        GLErrorLog;
        transformMatrixUniform = glGetUniformLocation(program, "uTransformMatrix");
        GLErrorLog;
        
        colorUniform = glGetUniformLocation(program, "uColor");
        GLErrorLog;
        
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLErrorLog;
        
        
    }
    return program;
}

#endif
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

#ifdef USE_ES2
-(void)execute {
    glUseProgram(getOGLProgram());
    
    GLKVector4 colorV = GLKVector4Make(((float)((color >> 16) & 0xff))/255.0, \
                                       ((float)((color >> 8) & 0xff))/255.0, ((float)(color & 0xff))/255.0, ((float)alpha)/255.0);
    //GlColorFromRGB(color, alpha);
    GLfloat vertexes[] = {
        x1+0.5, y1+0.5,
        x2+0.5, y2+0.5,
    };
    //_glEnableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    glEnableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    if (currentCN1projectionMatrixVersion != CN1projectionMatrixVersion) {
        glUniformMatrix4fv(projectionMatrixUniform, 1, 0, CN1projectionMatrix.m);
        currentCN1projectionMatrixVersion = CN1projectionMatrixVersion;
        
        GLErrorLog;
    }
    if (currentCN1modelViewMatrixVersion != CN1modelViewMatrixVersion) {
        glUniformMatrix4fv(modelViewMatrixUniform, 1, 0, CN1modelViewMatrix.m);
        currentCN1modelViewMatrixVersion = CN1modelViewMatrixVersion;
        GLErrorLog;
    }
    if (currentCN1transformMatrixVersion != CN1transformMatrixVersion) {
        glUniformMatrix4fv(transformMatrixUniform, 1, 0, CN1transformMatrix.m);
        GLErrorLog;
        currentCN1transformMatrixVersion = CN1transformMatrixVersion;
    }
    glUniform4fv(colorUniform, 1, colorV.v);
    GLErrorLog;
    
    //_glVertexPointer(2, GL_FLOAT, 0, vertexes);
    //GLErrorLog;
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    
    if (alpha<255){
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GLErrorLog;
    }
    
    glDrawArrays(GL_LINES, 0, 2);
    GLErrorLog;
    //_glDisableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    glDisableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    if (alpha<255){
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        GLErrorLog;
    }
    
    //glUseProgram(CN1activeProgram);
    //GLErrorLog;
}
#else
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
#endif
#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawLine";
}


@end
