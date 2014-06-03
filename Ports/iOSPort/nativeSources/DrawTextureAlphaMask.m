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
-(void)execute
{
    
    if ( textureName == 0 ){
        NSLog(@"Attempt to draw null texture.  Skipping");
    }
    
    GlColorFromRGB(color, alpha);
    GLErrorLog;
    
    static const GLshort textureCoordinates[] = {
        0, 0,
        1, 0,
        0, 1,
        1, 1,
    };
    
    //NSLog(@"Drawing mask %d %d %d %d", x,y,w,h);
    GLfloat vertexes[] = {
        (GLfloat)x, (GLfloat)y,
        (GLfloat)(x+w), (GLfloat)y,
        (GLfloat)x, (GLfloat)(y+h),
        (GLfloat)(x+w), (GLfloat)(y+h)
    };
    
    glActiveTexture(GL_TEXTURE1);
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
@end
