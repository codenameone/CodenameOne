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
#import "xmlvm.h"
#import "CN1ES2compat.h"
#import "DrawPath.h"
#import "CodenameOne_GLViewController.h"
#import "Renderer.h"
#import "Transformer.h"
#import "PathConsumer.h"
#import "AlphaConsumer.h"
#define min(a,b) ((a)<(b)?(a):(b))
#define max(a,b) ((a)>(b)?(a):(b))
#define abs(x) ((x)>0?(x):-(x))

@implementation DrawPath


-(id)initWithArgs:(Renderer*)r color:(int)c alpha:(int)a //x:(int)xx y:(int)yy w:(int)ww h:(int)hh
{
    color = c;
    alpha = a;
    renderer = r;
    return self;
}
-(void)execute
{
    
    GlColorFromRGB(color, alpha);
    JAVA_INT outputBounds[4];
    
    Renderer_getOutputBounds(renderer, (JAVA_INT*)&outputBounds);
    if ( outputBounds[2] < 0 || outputBounds[3] < 0 ){
        return;
    }
    JAVA_INT x = min(outputBounds[0], outputBounds[2]);
    JAVA_INT y = min(outputBounds[1], outputBounds[3]);
    JAVA_INT width = outputBounds[2]-outputBounds[0];
    JAVA_INT height = outputBounds[3]-outputBounds[1];
    
    if ( width < 0 ) width = -width;
    if ( height < 0 ) height = -height;
    
    GLfloat vertexes[] = {
          (GLfloat)x, (GLfloat)y,
          (GLfloat)(x+width), (GLfloat)y,
          (GLfloat)x, (GLfloat)(y+height),
          (GLfloat)(x+width), (GLfloat)(y+height)
    };
    static const GLshort textureCoordinates[] = {
        0, 0,
        1, 0,
        0, 1,
        1, 1,
    };
    
    
    AlphaConsumer ac = {
        x,
        y,
        width,
        height,
    };
    
    jbyte maskArray[ac.width*ac.height];
    
    ac.alphas = (JAVA_BYTE*)&maskArray;
    Renderer_produceAlphas(renderer, &ac);
    
    
    glGenTextures(1, &tex);
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, tex);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    
    glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, ac.width, ac.height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, maskArray);
    
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    //_glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    _glEnableCN1State(CN1_GL_ALPHA_TEXTURE);
    GLErrorLog;
    //_glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    _glAlphaMaskTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    GLErrorLog;
    _glVertexPointer(2, GL_FLOAT, 0, vertexes);
    GLErrorLog;
    _glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    //_glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    _glDisableCN1State(CN1_GL_ALPHA_TEXTURE);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    //_glDisable(GL_TEXTURE_2D);
    GLErrorLog;

    
}
-(void)dealloc
{
    glDeleteTextures(1, &tex);
    Renderer_destroy(renderer);
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
    
}
@end
