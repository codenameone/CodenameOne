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

@implementation DrawString
-(id)initWithArgs:(int)c a:(int)a xpos:(int)xpos ypos:(int)ypos s:(NSString*)s f:(UIFont*)f {
    color = c;
    alpha = a;
    x = xpos;
    y = ypos;
    str = s;
    [str retain];
    font = f;
    [font retain];
    return self;
}

-(void)execute {
    GLuint textureName = [DrawStringTextureCache checkCache:str f:font c:color a:255];
    int w = (int)[str sizeWithFont:font].width;
    int h = (int)[font lineHeight];
    int p2w = nextPowerOf2(w);
    int p2h = nextPowerOf2(h);
    glEnableClientState(GL_VERTEX_ARRAY);
    //glEnableClientState(GL_NORMAL_ARRAY);
    GLErrorLog;
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glEnable(GL_TEXTURE_2D);
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
    glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
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
        
        glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
        GLErrorLog;
        glVertexPointer(2, GL_FLOAT, 0, vertexes);
        GLErrorLog;
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GLErrorLog;

    glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    //glDisableClientState(GL_NORMAL_ARRAY);
    GLErrorLog;
    glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}

-(void)dealloc {
    [str release];
    [font release];
    [super dealloc];
}

-(NSString*)getName {
    return @"DrawString";
}

@end
