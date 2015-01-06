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
#import "CN1ES2compat.h"
#import "DrawTextureAlphaMask.h"
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
    0, 0,
    1, 0,
    0, 1,
    1, 1,
};


static NSString *fragmentShaderSrc =
@"precision highp float;\n"
"uniform lowp vec4 uColor;\n"
"uniform highp sampler2D uTextureRGBA;\n"
"varying highp vec2 vTextureRGBACoord;\n"

"void main(){\n"
//"   gl_FragColor = texture2D(uTextureRGBA, vTextureRGBACoord) * uColor; \n"
"   float texA = texture2D(uTextureRGBA, vTextureRGBACoord).a;\n"
"   vec4 color = vec4(uColor.rgb*texA, texA*uColor.a);\n"
//"   gl_FragColor = color;\n"
"   if ( color.a < .0001 ){ discard;} else {gl_FragColor = color;}\n"
//"   if ( color.a < .01 ){ color.a = 0.0; gl_FragColor=color;} else {gl_FragColor = color;}\n"
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

@implementation DrawTextureAlphaMask

-(id)initWithArgs:(GLuint)pTextName color:(int)pColor alpha:(int)pAlpha x:(int)pX y:(int)pY w:(int)pW h:(int)pH
{
    textureName = pTextName;
    color = pColor;
    alpha = pAlpha;
    x = pX;
    y = pY;
    w = pW;
    h = pH;
    return self;
}
#ifdef USE_ES2
-(void)execute
{
    
    if ( textureName == 0 ){
        NSLog(@"Attempt to draw null texture.  Skipping");
    }
    glUseProgram(getOGLProgram());
    //GLKVector4 color = GLKVector4Make(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    GLKVector4 colorV = GLKVector4Make(((float)((color >> 16) & 0xff))/255.0, \
                                       ((float)((color >> 8) & 0xff))/255.0, ((float)(color & 0xff))/255.0, ((float)alpha)/255.0);
    
    //GlColorFromRGB(color, alpha);
    //GLErrorLog;
    
    //static const GLshort textureCoordinates[] = {
    //    0, 0,
    //    1, 0,
    //    0, 1,
    //    1, 1,
    //};
    
    //NSLog(@"Drawing mask %d %d %d %d", x,y,w,h);
    GLfloat vertexes[] = {
        (GLfloat)x, (GLfloat)y,
        (GLfloat)(x+w), (GLfloat)y,
        (GLfloat)x, (GLfloat)(y+h),
        (GLfloat)(x+w), (GLfloat)(y+h)
    };
    
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, textureName);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    GLErrorLog;
    
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    GLErrorLog;
    
    
    
    //_glEnableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    //_glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    //_glEnableCN1State(CN1_GL_ALPHA_TEXTURE);
    //GLErrorLog;
    
    glEnableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glEnableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    //_glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    //_glAlphaMaskTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    //GLErrorLog;
    //_glVertexPointer(2, GL_FLOAT, 0, vertexes);
    //GLErrorLog;
    
    glVertexAttribPointer(textureCoordAtt, 2, GL_SHORT, 0, 0, textureCoordinates);
    GLErrorLog;
    
    glUniformMatrix4fv(projectionMatrixUniform, 1, 0, CN1projectionMatrix.m);
    GLErrorLog;
    glUniformMatrix4fv(modelViewMatrixUniform, 1, 0, CN1modelViewMatrix.m);
    GLErrorLog;
    glUniformMatrix4fv(transformMatrixUniform, 1, 0, CN1transformMatrix.m);
    GLErrorLog;
    
    glUniform1i(textureUniform, 0);
    GLErrorLog;
    glUniform4fv(colorUniform, 1, colorV.v);
    GLErrorLog;
    
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    //_glDisableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    //_glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    //_glDisableCN1State(CN1_GL_ALPHA_TEXTURE);
    //GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    //_glDisable(GL_TEXTURE_2D);
    //GLErrorLog;
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDisableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glDisableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
}
#else
-(void)execute {}
#endif
@end
