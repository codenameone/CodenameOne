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
#import "ClipRect.h"
#import "CodenameOne_GLViewController.h"
#import "FillRect.h"
#ifdef USE_ES2
#import "DrawTextureAlphaMask.h"
#import "FillPolygon.h"
#endif

static int clipX, clipY, clipW, clipH;
static BOOL clipApplied = NO;
static BOOL clipIsTexture = NO;
extern float currentScaleX;
extern float currentScaleY;
extern float scaleValue;

@implementation ClipRect
static CGRect drawingRect;

-(id)initWithArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h f:(BOOL)f
{
    return [self initWithArgs:xpos ypos:ypos w:w h:h f:f texture:0];
}
-(id)initWithPolygon:(JAVA_FLOAT*)xIn y:(JAVA_FLOAT*)yIn length:(int)len
{
    texture = 0;
    x = -1;
    y = -1;
    width = -1;
    height = -1;
    numPoints = len;
    
    size_t size = sizeof(JAVA_FLOAT)*len;
    xPoints = malloc(size);
    yPoints = malloc(size);
    
    memcpy(xPoints, xIn, size);
    memcpy(yPoints, yIn, size);
    
    
    return self;
}


-(id)initWithArgs:(int)xpos ypos:(int)ypos w:(int)w h:(int)h f:(BOOL)f texture:(GLuint)tex{
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    texture = tex;
    numPoints = 0;

    firstClip = !f;
    return self;
}

+(void)setDrawRect:(CGRect)rect {
    drawingRect = rect;
}

-(void)executeWithClipping {
    [self execute];
}

-(void)executeWithLog {
    NSDate *start = [NSDate date];
    [self executeWithClipping];
    NSDate *finish = [NSDate date];
    NSTimeInterval t = [finish timeIntervalSinceDate:start];
    NSLog(@"%@ took %f", [self getName], t);
}

-(void)execute {
#ifdef USE_ES2
    if ( texture != 0 || numPoints > 0 ){
        clipX = x; clipY=y; clipW=width; clipH=height;
        glClearStencil(0x0);
        glEnable(GL_STENCIL_TEST);
        //glDisable(GL_STENCIL_TEST);
        _glDisable(GL_SCISSOR_TEST);
        glStencilFunc(GL_NEVER, 1, 0xff);
        
        glStencilOp(GL_REPLACE, GL_KEEP, GL_KEEP);
        glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE);
        glStencilMask(0xff);
        glClear(GL_STENCIL_BUFFER_BIT);
        
        GLKMatrix4 transform = glGetTransformES2();
        glSetTransformES2(GLKMatrix4Identity);
        ExecutableOp *f;
        if ( texture != 0 ){
            f = [[DrawTextureAlphaMask alloc] initWithArgs:texture color:0xffffff alpha:0xff x:x y:y w:width h:height];
        } else {
            f = [[FillPolygon alloc] initWithArgs:xPoints y:yPoints num:numPoints color:0xffffff alpha: 0xff];
        }
        [f execute];

#ifndef CN1_USE_ARC
        [f release];
#endif
        glSetTransformES2(transform);
        
        glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        glStencilMask(0x0);
        glStencilFunc(GL_EQUAL, 1, 0xff);
        clipIsTexture = YES;
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        return;
    }

    
#endif
    clipIsTexture = NO;
    int x2 = x + width;
    int y2 = y + height;
    int orX = drawingRect.origin.x;
    int orY = drawingRect.origin.y;
    if(x < orX) {
        x = orX;
    }
    if(y < orY) {
        y = orY;
    }
    int destX2 = (int)(drawingRect.origin.x + drawingRect.size.width);
    int destY2 = (int)(drawingRect.origin.y + drawingRect.size.height);
    if(x2 > destX2) {
        width = destX2 - x;
    }
    if(y2 > destY2) {
        height = destY2 - y;
    }
    if(width > 0 && height > 0) {
        [super clipBlock:NO];
        // full screen access, no need for this
        int scale = scaleValue;
        int displayHeight = [CodenameOne_GLViewController instance].view.bounds.size.height * scale;
        if(width == [CodenameOne_GLViewController instance].view.bounds.size.width * scale && height == displayHeight) {
            GLErrorLog;
            _glDisable(GL_SCISSOR_TEST);
#ifdef USE_ES2
            glDisable(GL_STENCIL_TEST);
            GLErrorLog;
#endif
            
            return;
        }
        clipX = x;
        clipY = y;
        clipW = width;
        clipH = height;
        [ClipRect updateClipToScale];
        _glEnable(GL_SCISSOR_TEST);
        GLErrorLog;
#ifdef USE_ES2
        glDisable(GL_STENCIL_TEST);
        GLErrorLog;
#endif
        clipApplied = YES;
    } else {
        [super clipBlock:YES];
        _glDisable(GL_SCISSOR_TEST);

        GLErrorLog;
#ifdef USE_ES2
        glDisable(GL_STENCIL_TEST);
        GLErrorLog;
#endif
        clipApplied = NO;
    }

}


+(void)updateClipToScale {
    if ( clipIsTexture ){
        return;
    }
    int displayHeight = [CodenameOne_GLViewController instance].view.bounds.size.height * scaleValue;
    if(currentScaleX == 1 && currentScaleY == 1) {
        //_glEnable(GL_SCISSOR_TEST);
        //NSLog(@"Updating clip to scale");
        glScissor(clipX, displayHeight - clipY - clipH, clipW, clipH);
    }

}

#ifndef CN1_USE_ARC
-(void)dealloc {
    if ( numPoints > 0 ){
        free(xPoints);
        free(yPoints);
    }
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"ClipRect";
}


@end
