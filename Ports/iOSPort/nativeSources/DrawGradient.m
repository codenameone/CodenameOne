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
#import "DrawGradient.h"
#import "CodenameOne_GLViewController.h"
#import "DrawGradientTextureCache.h"

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


@implementation DrawGradient

-(id)initWithArgs:(int)typeA startColorA:(int)startColorA endColorA:(int)endColorA xA:(int)xA yA:(int)yA widthA:(int)widthA heightA:(int)heightA relativeXA:(float)relativeXA relativeYA:(float)relativeYA relativeSizeA:(float)relativeSizeA {
    type = typeA;
    startColor = startColorA;
    endColor = endColorA;
    x = xA;
    y = yA;
    width = widthA;
    height = heightA;
    relativeX = relativeXA;
    relativeY = relativeYA;
    relativeSize = relativeSizeA;

    return self;
}
#ifdef USE_ES2
-(void)execute {
    glUseProgram(getOGLProgram());
    GLuint textureName = [DrawGradientTextureCache checkCache:type startColorA:startColor endColorA:endColor widthA:width heightA:height relativeXA:relativeX relativeYA:relativeY relativeSizeA:relativeSize];
    int p2w = nextPowerOf2(width);
    int p2h = nextPowerOf2(height);
    //_glEnableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    //_glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    //GLErrorLog;
    //_glEnable(GL_TEXTURE_2D);
    //GLErrorLog;
    
    glEnableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glEnableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    if(textureName == 0) {
        glGenTextures(1, &textureName);
        GLErrorLog;
        glActiveTexture(GL_TEXTURE0);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, textureName);
        GLErrorLog;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        void* imageData = malloc(p2h * p2w * 4);
        CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
        CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));
        
        UIGraphicsPushContext(context);
        CGFloat components[8] = {
            ((float)((startColor >> 16) & 0xff))/255.0,
            ((float)((startColor >> 8) & 0xFF))/255.0,
            ((float)(startColor & 0xff))/255.0,
            1.0,
            ((float)((endColor >> 16) & 0xFF))/255.0,
            ((float)((endColor >> 8) & 0xFF))/255.0,
            ((float)(endColor & 0xff))/255.0,
            1.0 };
        size_t num_locations = 2;
        CGFloat locations[2] = { 0.0, 1.0 };
        CGGradientRef myGradient = CGGradientCreateWithColorComponents (colorSpace, components, locations, num_locations);
        switch (type) {
            case GRADIENT_TYPE_RADIAL:
                [UIColorFromRGB(endColor, 255) set];
                CGContextFillRect(context, CGRectMake(0, 0, width, height));
                CGPoint myCentrePoint = CGPointMake(relativeX * width, relativeY * height);
                float myRadius = MIN(width, height) * relativeSize;
                CGContextDrawRadialGradient (context, myGradient, myCentrePoint,
                                             0, myCentrePoint, myRadius,
                                             kCGGradientDrawsAfterEndLocation);
                break;
                
            case GRADIENT_TYPE_HORIZONTAL:
                CGContextDrawLinearGradient(context, myGradient,
                                            CGPointMake(0, 0), CGPointMake(0, width), kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);
                break;
                
            case GRADIENT_TYPE_VERTICAL:
                CGContextDrawLinearGradient(context, myGradient,
                                            CGPointMake(0, 0), CGPointMake(height, 0), kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);
                break;
        }
        UIGraphicsPopContext();
        CGColorSpaceRelease(colorSpace);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, p2w, p2h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        GLErrorLog;
        CGContextRelease(context);
        GLErrorLog;
        free(imageData);
        [DrawGradientTextureCache cache:type startColorA:startColor endColorA:endColor widthA:width heightA:height relativeXA:relativeX relativeYA:relativeY relativeSizeA:relativeSize tA:textureName];
    }
    //_glColor4f(1, 1, 1 ,1);
    GLKVector4 color = GLKVector4Make(1,1,1,1);
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
    
    //static const GLshort textureCoordinates[] = {
    //    0, 1,
    //    1, 1,
    //    0, 0,
    //    1, 0,
    //};
    
    //_glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    //GLErrorLog;
    glVertexAttribPointer(textureCoordAtt, 2, GL_SHORT, 0, 0, textureCoordinates);
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
    
    //_glDisableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    //_glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    //GLErrorLog;
    //glBindTexture(GL_TEXTURE_2D, 0);
    //GLErrorLog;
    //glDisableClientState(GL_NORMAL_ARRAY);
    //GLErrorLog;
    //_glDisable(GL_TEXTURE_2D);
    //GLErrorLog;
    
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
    GLuint textureName = [DrawGradientTextureCache checkCache:type startColorA:startColor endColorA:endColor widthA:width heightA:height relativeXA:relativeX relativeYA:relativeY relativeSizeA:relativeSize];
    int p2w = nextPowerOf2(width);
    int p2h = nextPowerOf2(height);
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    _glEnable(GL_TEXTURE_2D);
    GLErrorLog;
    if(textureName == 0) {
        glGenTextures(1, &textureName);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, textureName);
        GLErrorLog;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        void* imageData = malloc(p2h * p2w * 4);
        CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
        CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));
        
        UIGraphicsPushContext(context);
        CGFloat components[8] = { 
            ((float)((startColor >> 16) & 0xff))/255.0, 
            ((float)((startColor >> 8) & 0xFF))/255.0,
            ((float)(startColor & 0xff))/255.0, 
            1.0, 
            ((float)((endColor >> 16) & 0xFF))/255.0, 
            ((float)((endColor >> 8) & 0xFF))/255.0,
            ((float)(endColor & 0xff))/255.0, 
            1.0 };
        size_t num_locations = 2;
        CGFloat locations[2] = { 0.0, 1.0 };
        CGGradientRef myGradient = CGGradientCreateWithColorComponents (colorSpace, components, locations, num_locations);
        switch (type) {
            case GRADIENT_TYPE_RADIAL:
                [UIColorFromRGB(endColor, 255) set];
                CGContextFillRect(context, CGRectMake(0, 0, width, height));
                CGPoint myCentrePoint = CGPointMake(relativeX * width, relativeY * height);
                float myRadius = MIN(width, height) * relativeSize;
                CGContextDrawRadialGradient (context, myGradient, myCentrePoint,
                                             0, myCentrePoint, myRadius,
                                             kCGGradientDrawsAfterEndLocation);
                break;

            case GRADIENT_TYPE_HORIZONTAL:
                CGContextDrawLinearGradient(context, myGradient, 
                                            CGPointMake(0, 0), CGPointMake(0, width), kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);
                break;

            case GRADIENT_TYPE_VERTICAL:
                CGContextDrawLinearGradient(context, myGradient, 
                                            CGPointMake(0, 0), CGPointMake(height, 0), kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);                
                break;
        }
        UIGraphicsPopContext();
        CGColorSpaceRelease(colorSpace);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, p2w, p2h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        GLErrorLog;
        CGContextRelease(context);
        GLErrorLog;
        free(imageData);
        [DrawGradientTextureCache cache:type startColorA:startColor endColorA:endColor widthA:width heightA:height relativeXA:relativeX relativeYA:relativeY relativeSizeA:relativeSize tA:textureName];
    }    
    _glColor4f(1, 1, 1 ,1);
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
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawGradient";
}

@end
