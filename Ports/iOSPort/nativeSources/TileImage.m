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
#import "TileImage.h"
#import "CodenameOne_GLViewController.h"
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
static GLfloat textureCoordinates[] = {
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

GLfloat* createVertexArray(int x, int y, int imageWidth, int imageHeight) {
    GLfloat* vtx = malloc(8 * sizeof(GLfloat));
    int w = nextPowerOf2(imageWidth);
    int h = nextPowerOf2(imageHeight);
    vtx[0] = x;
    vtx[1] = y;
    vtx[2] = x + w;
    vtx[3] = y;
    vtx[4] = x;
    vtx[5] = y + h;
    vtx[6] = x + w;
    vtx[7] = y + h;
    return vtx;
    
}

@implementation TileImage
-(id)initWithArgs:(int)a xpos:(int)xpos ypos:(int)ypos i:(GLUIImage*)i w:(int)w h:(int)h {
    alpha = a;
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    img = i;
#ifndef CN1_USE_ARC
    [img retain];
#endif
    return self;
}
#ifdef USE_ES2
-(void)execute {
    if (width <= 0 || height <= 0) {
        return;
    }
    glUseProgram(getOGLProgram());
    GLKVector4 color = GLKVector4Make(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    
    int imageWidth = (int)[[img getImage] size].width;
    int imageHeight = (int)[[img getImage] size].height;
    
    if (imageWidth <=0 || imageHeight <= 0) {
        return;
    }
    GLuint tex = [img getTexture:imageWidth texHeight:imageHeight];
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, tex);
    
    GLErrorLog;
    
    glEnableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glEnableVertexAttribArray(vertexCoordAtt);
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
    
    int numTiles = ceil((float)width / (float)imageWidth) * ceil((float)height / (float)imageHeight);
    
    // For tileImage we use GL_TRIANGLES instead of TRIANGLE_STRIP because it is easier to batch and
    // connect texture coordinates to vertex coordinates.  This means that each quad requires 6 coordinates
    // instead of 4 - but we can batch all tiles in a single drawArrays call which should more than
    // make up for the extra overhead.
    
    GLfloat* vertexes = malloc(12 * numTiles * sizeof(GLfloat));
    GLfloat* texCoords = malloc(12 * numTiles * sizeof(GLfloat));
    

    
    int p2w = nextPowerOf2(imageWidth);
    int p2h = nextPowerOf2(imageHeight);
    
    GLfloat wRatio = (GLfloat)imageWidth / (GLfloat)p2w;
    GLfloat hRatio = (GLfloat)imageHeight / (GLfloat)p2h;
    
    GLfloat t0Y = 1.0 - hRatio;
    GLfloat t0X = 0;
    GLfloat t1Y = 1;
    GLfloat t1X = wRatio;
    
    
    int tileOffset = 0;
    for (int xPos = 0; xPos < width; xPos += imageWidth) {
        for (int yPos = 0; yPos < height; yPos += imageHeight) {
            texCoords[tileOffset+0] = t0X;//0;
            texCoords[tileOffset+1] = t1Y;//1;
            texCoords[tileOffset+2] = t1X;//1;
            texCoords[tileOffset+3] = t1Y;//1;
            texCoords[tileOffset+4] = t0X;//0;
            texCoords[tileOffset+5] = t0Y;//0;
            
            texCoords[tileOffset+6] = t0X;//0;  // Same as 4
            texCoords[tileOffset+7] = t0Y;//0;  // Same as 5
            texCoords[tileOffset+8] = t1X;//1;  // Same as 2
            texCoords[tileOffset+9] = t1Y;//1;  // Same as 3
            
            texCoords[tileOffset+10] = t1X;//1;
            texCoords[tileOffset+11] = t0Y;//0;
            
            vertexes[tileOffset+0] = x+xPos;
            vertexes[tileOffset+1] = y+yPos;
            vertexes[tileOffset+2] = vertexes[tileOffset+0] + imageWidth;//p2w;
            vertexes[tileOffset+3] = vertexes[tileOffset+1];
            vertexes[tileOffset+4] = vertexes[tileOffset+0];
            vertexes[tileOffset+5] = vertexes[tileOffset+1] + imageHeight;//p2h;
            
            vertexes[tileOffset+6] = vertexes[tileOffset+0];        // Same as 4
            vertexes[tileOffset+7] = vertexes[tileOffset+1] + imageHeight;//p2h;  // Same as 5
            vertexes[tileOffset+8] = vertexes[tileOffset+0] + imageWidth;//p2w;  // Same as 2
            vertexes[tileOffset+9] = vertexes[tileOffset+1];        // Same as 3
            
            vertexes[tileOffset+10] = vertexes[tileOffset+2];
            vertexes[tileOffset+11] = vertexes[tileOffset+5];
            
            if (xPos + imageWidth > width) {
                vertexes[tileOffset+2] = vertexes[tileOffset+8] =  vertexes[tileOffset+10] = x + width;
                texCoords[tileOffset+2] = texCoords[tileOffset+8] = texCoords[tileOffset+10] = (GLfloat)(width-xPos)/ (GLfloat)p2w;
            }
            if (yPos + imageHeight > height) {
                vertexes[tileOffset+5] = vertexes[tileOffset+7] = vertexes[tileOffset+11] = y + height;
                texCoords[tileOffset+5] = texCoords[tileOffset+7] = texCoords[tileOffset+11] = 1.0 - (GLfloat)(height-yPos) / (GLfloat)p2h;
            }
            
            tileOffset += 12;
        }
    }
    
    glVertexAttribPointer(textureCoordAtt, 2, GL_FLOAT, 0, 0, texCoords);
    GLErrorLog;
    
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    
    glDrawArrays(GL_TRIANGLES, 0, 6 * numTiles);
    GLErrorLog;
    
    free(vertexes);
    free(texCoords);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDisableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glDisableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;

}

#else


-(void)execute {
    _glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glActiveTexture(GL_TEXTURE0);
    _glEnable(GL_TEXTURE_2D);
    GLErrorLog;
    int imageWidth = (int)[[img getImage] size].width;
    int imageHeight = (int)[[img getImage] size].height;
    GLuint tex = [img getTexture:imageWidth texHeight:imageHeight];
    glBindTexture(GL_TEXTURE_2D, tex);
    GLErrorLog;

    static const GLshort textureCoordinates[] = {
        0, 1,
        1, 1,
        0, 0,
        1, 0,
    };

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    _glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    GLErrorLog;

    for (int xPos = 0; xPos <= width; xPos += imageWidth) {
        for (int yPos = 0; yPos < height; yPos += imageHeight) {
            GLfloat* vertexes = createVertexArray(x + xPos, y + yPos, imageWidth, imageHeight);
            _glVertexPointer(2, GL_FLOAT, 0, vertexes);
            GLErrorLog;
            _glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            GLErrorLog;
            free(vertexes);
        }
    }    
    
    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    _glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}
#endif

#ifndef CN1_USE_ARC
-(void)dealloc {
    [img release];
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"TileImage";
}

@end
