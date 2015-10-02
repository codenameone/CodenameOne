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
#import "DrawString.h"
#import "CodenameOne_GLViewController.h"
#import "DrawStringTextureCache.h"
#include "xmlvm.h"

#ifdef USE_ES2
extern GLKMatrix4 CN1modelViewMatrix;
extern GLKMatrix4 CN1projectionMatrix;
extern GLKMatrix4 CN1transformMatrix;
extern GLuint CN1activeProgram;
static GLuint program=0;
static GLuint vertexShader;
static GLuint fragmentShader;
static GLuint modelViewMatrixUniform;
static GLuint projectionMatrixUniform;
static GLuint transformMatrixUniform;
static GLuint textureUniform;
static GLuint colorUniform;
static GLuint vertexCoordAtt;
static GLuint textureCoordAtt;
static const GLshort textureCoordinates[] = {
    0, 1,
    1, 1,
    0, 0,
    1, 0,
};


static NSString *fragmentShaderSrc =
@"precision highp float;\n"
"uniform lowp vec4 uColor;\n"
"uniform highp sampler2D uTextureRGBA;\n"
"varying highp vec2 vTextureRGBACoord;\n"

"void main(){\n"
"   gl_FragColor = texture2D(uTextureRGBA, vTextureRGBACoord) * uColor; \n"
"}\n";

static NSString *vertexShaderSrc =
@"attribute vec4 aVertexCoord;\n"
"attribute vec2 aTextureRGBACoord;\n"

"uniform mat4 uModelViewMatrix;\n"
"uniform mat4 uProjectionMatrix;\n"
"uniform mat4 uTransformMatrix;\n"

"varying highp vec2 vTextureRGBACoord;\n"

"void main(){\n"
"   gl_Position = uProjectionMatrix *  uModelViewMatrix * uTransformMatrix * aVertexCoord;\n"
"   vTextureRGBACoord = aTextureRGBACoord;\n"
"}";

static GLuint getOGLProgram(){
    if ( program == 0  ){
        program = CN1compileShaderProgram(vertexShaderSrc, fragmentShaderSrc);
        GLErrorLog;
        vertexCoordAtt = glGetAttribLocation(program, "aVertexCoord");
        GLErrorLog;
        
        textureCoordAtt = glGetAttribLocation(program, "aTextureRGBACoord");
        GLErrorLog;
        
        modelViewMatrixUniform = glGetUniformLocation(program, "uModelViewMatrix");
        GLErrorLog;
        projectionMatrixUniform = glGetUniformLocation(program, "uProjectionMatrix");
        GLErrorLog;
        transformMatrixUniform = glGetUniformLocation(program, "uTransformMatrix");
        GLErrorLog;
        textureUniform = glGetUniformLocation(program, "uTextureRGBA");
        GLErrorLog;
        colorUniform = glGetUniformLocation(program, "uColor");
        GLErrorLog;
        
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLErrorLog;
        
        
    }
    return program;
}

#endif

@implementation DrawString
-(id)initWithArgs:(int)c a:(int)a xpos:(int)xpos ypos:(int)ypos s:(NSString*)s f:(UIFont*)f {
    color = c;
    alpha = a;
    x = xpos;
    y = ypos;
    str = s;
    font = f;
#ifndef CN1_USE_ARC
    [str retain];
    [font retain];
#endif
    return self;
}
#ifdef USE_ES2
-(void)execute {
    glUseProgram(getOGLProgram());
    GLuint textureName = [DrawStringTextureCache checkCache:str f:font c:color a:255];
    int w = (int)[str sizeWithFont:font].width;
    int h = (int)[font lineHeight];
    int p2w = nextPowerOf2(w);
    int p2h = nextPowerOf2(h);
    glEnableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    //_glEnableClientState(GL_VERTEX_ARRAY);
    //glEnableClientState(GL_NORMAL_ARRAY);
    //GLErrorLog;
    //_glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    //GLErrorLog;
    glEnableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    //_glEnable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    if(textureName == 0) {
        glGenTextures(1, &textureName);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, textureName);
        GLErrorLog;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        void* imageData = malloc(p2h * p2w * 4);
        CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
        //CGContextTranslateCTM(context, 0, p2h);
        //CGContextScaleCTM(context, 1, -1);
        CGColorSpaceRelease(colorSpace);
        CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));
        
        UIGraphicsPushContext(context);
        [UIColorFromRGB(color, 255) set];
        [str drawAtPoint:CGPointZero withFont:font];
        UIGraphicsPopContext();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, p2w, p2h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        GLErrorLog;
        CGContextRelease(context);
        GLErrorLog;
        free(imageData);
        [DrawStringTextureCache cache:str f:font t:textureName c:color a:255];
    }
    //_glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    GLKVector4 color = GLKVector4Make(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glBindTexture(GL_TEXTURE_2D, textureName);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    
    GLfloat vertexes[] = {
        x, y,
        x + w, y,
        x, y + h,
        x + w, y + h
    };
    
    GLfloat nY = 1.0;
    GLfloat wX = 0;
    GLfloat sY = 1.0 - (GLfloat)h / (GLfloat)p2h;
    GLfloat eX = (GLfloat)w/(GLfloat)p2w;
    
    GLfloat textureCoordinates[] = {
        wX, nY,
        eX, nY,
        wX, sY,
        eX, sY
    };
    
    //_glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    //GLErrorLog;
    glVertexAttribPointer(textureCoordAtt, 2, GL_FLOAT, 0, 0, textureCoordinates);
    GLErrorLog;
    
    //_glVertexPointer(2, GL_FLOAT, 0, vertexes);
    //GLErrorLog;
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    
    glUniformMatrix4fv(projectionMatrixUniform, 1, 0, CN1projectionMatrix.m);
    GLErrorLog;
    glUniformMatrix4fv(modelViewMatrixUniform, 1, 0, CN1modelViewMatrix.m);
    GLErrorLog;
    glUniformMatrix4fv(transformMatrixUniform, 1, 0, CN1transformMatrix.m);
    GLErrorLog;
    
    glUniform1i(textureUniform, 0);
    GLErrorLog;
    glUniform4fv(colorUniform, 1, color.v);
    GLErrorLog;
    
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDisableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glDisableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    
    //glUseProgram(CN1activeProgram);
    //GLErrorLog;
}

#else
-(void)execute {
    GLuint textureName = [DrawStringTextureCache checkCache:str f:font c:color a:255];
    int w = (int)[str sizeWithFont:font].width;
    int h = (int)[font lineHeight];
    int p2w = nextPowerOf2(w);
    int p2h = nextPowerOf2(h);
    _glEnableClientState(GL_VERTEX_ARRAY);
    //glEnableClientState(GL_NORMAL_ARRAY);
    GLErrorLog;
    _glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    _glEnable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    if(textureName == 0) {
        glGenTextures(1, &textureName);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, textureName);
        GLErrorLog;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        void* imageData = malloc(p2h * p2w * 4);
        CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
        //CGContextTranslateCTM(context, 0, p2h);
        //CGContextScaleCTM(context, 1, -1);
        CGColorSpaceRelease(colorSpace);
        CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));
        
        UIGraphicsPushContext(context);
        [UIColorFromRGB(color, 255) set];
        [str drawAtPoint:CGPointZero withFont:font];
        UIGraphicsPopContext();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, p2w, p2h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        GLErrorLog;
        CGContextRelease(context);
        GLErrorLog;
        free(imageData);
        [DrawStringTextureCache cache:str f:font t:textureName c:color a:255];
    }    
    _glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glBindTexture(GL_TEXTURE_2D, textureName);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    
        GLfloat vertexes[] = {
            x, y,
            x + p2w, y,
            x, y + p2h,
            x + p2w, y + p2h
        };
        
        static const GLshort textureCoordinates[] = {
            0, 1,
            1, 1,
            0, 0,
            1, 0,
        };
        
        _glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
        GLErrorLog;
        _glVertexPointer(2, GL_FLOAT, 0, vertexes);
        GLErrorLog;
        _glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GLErrorLog;

    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    //glDisableClientState(GL_NORMAL_ARRAY);
    GLErrorLog;
    _glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}

#endif

#ifndef CN1_USE_ARC
-(void)dealloc {
    [str release];
    [font release];
    [super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawString";
}

@end
