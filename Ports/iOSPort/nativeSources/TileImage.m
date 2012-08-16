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
    [img retain];
    return self;
}

-(void)execute {
    glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glEnable(GL_TEXTURE_2D);
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
    glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    GLErrorLog;

    for (int xPos = 0; xPos <= width; xPos += imageWidth) {
        for (int yPos = 0; yPos < height; yPos += imageHeight) {
            GLfloat* vertexes = createVertexArray(x + xPos, y + yPos, imageWidth, imageHeight);
            glVertexPointer(2, GL_FLOAT, 0, vertexes);
            GLErrorLog;
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            GLErrorLog;
            free(vertexes);
        }
    }    
    
    glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}

-(void)dealloc {
    [img release];
	[super dealloc];
}

-(NSString*)getName {
    return @"TileImage";
}

@end
