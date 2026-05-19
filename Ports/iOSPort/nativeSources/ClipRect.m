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
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif
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

// matrix-translate-flag debug probe (#3302). Set to N>0 when a polygon
// clip is applied; subsequent draw ops decrement and emit one CN1DBG
// line each. Lets us reconstruct, from the CI artifact log, the exact
// sequence of (transform, color, geometry) that lands at the FillRect
// op which fails to render in iOS GL panel 2 of graphics-clip-under-
// rotation. Without this probe the polygon-clip / FillRect / stencil
// interaction is opaque from outside the simulator.
int CN1DbgRemainingOps = 0;
int CN1DbgPolygonClipSeq = 0;

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
    CN1Log(@"%@ took %f", [self getName], t);
}

-(void)execute {
#ifdef CN1_USE_METAL
    // Issue #3921 path. Three shapes can arrive here:
    //
    //   1. A rectangular clip (initWithArgs:...) -- x/y/w/h hold the
    //      rect, no polygon points, no texture. Set the scissor and
    //      disable any prior polygon-stencil clip so we don't carry a
    //      stale stencil test forward.
    //
    //   2. A polygon clip (initWithPolygon:...) -- numPoints > 0 with
    //      xPoints/yPoints in framebuffer pixel space (built by
    //      NativeGraphics.clipRect's non-identity-transform branch via
    //      inverseClip + transform back to screen space). Use the
    //      stencil pipeline: render the polygon to the stencil at a
    //      fresh reference value, then bind a stencil-test depth state
    //      so subsequent draws on this encoder are masked to the
    //      polygon shape. Mirrors the GL ES2 sequence below.
    //
    //   3. A texture-mask clip (initWithArgs:...:texture:) -- texture
    //      != 0 with x/y/w/h holding the mask bbox. Metal's GLuint
    //      texture handle from the GL path isn't directly compatible
    //      with MTLTexture, so this still falls back to a bbox scissor
    //      (matches the documented Phase 2 fallback for the texture
    //      mask case; a follow-up could rasterise the alpha mask into
    //      an MTLTexture and sample it in a stencil-write shader).
    if (numPoints > 0 && xPoints != NULL && yPoints != NULL) {
        CN1MetalApplyPolygonStencilClip(xPoints, yPoints, numPoints);
        clipApplied = YES;
    } else {
        int sx = x, sy = y, sw = width, sh = height;
        if (sx < 0) { sw += sx; sx = 0; }
        if (sy < 0) { sh += sy; sy = 0; }
        CN1MetalDisablePolygonStencilClip();
        CN1MetalSetScissor(sx, sy, sw, sh);
        clipApplied = (sw > 0 && sh > 0);
    }
#else
#ifdef USE_ES2
    if ( texture != 0 || numPoints > 0 ){
        CN1DbgPolygonClipSeq++;
        CN1DbgRemainingOps = 6;
        if (numPoints > 0) {
            NSLog(@"CN1SS:DBG ClipRect.polygon seq=%d numPoints=%d first=(%f,%f) last=(%f,%f) bbox=...", CN1DbgPolygonClipSeq, numPoints, xPoints[0], yPoints[0], xPoints[numPoints-1], yPoints[numPoints-1]);
            float minX = xPoints[0], maxX = xPoints[0], minY = yPoints[0], maxY = yPoints[0];
            for (int i = 1; i < numPoints; i++) {
                if (xPoints[i] < minX) minX = xPoints[i];
                if (xPoints[i] > maxX) maxX = xPoints[i];
                if (yPoints[i] < minY) minY = yPoints[i];
                if (yPoints[i] > maxY) maxY = yPoints[i];
            }
            NSLog(@"CN1SS:DBG ClipRect.polygon seq=%d bbox x=[%f,%f] y=[%f,%f]", CN1DbgPolygonClipSeq, minX, maxX, minY, maxY);
            GLKMatrix4 cur = glGetTransformES2();
            NSLog(@"CN1SS:DBG ClipRect.polygon seq=%d preTransform=[%f %f %f %f / %f %f %f %f / %f %f %f %f / %f %f %f %f]",
                  CN1DbgPolygonClipSeq,
                  cur.m[0], cur.m[1], cur.m[2], cur.m[3],
                  cur.m[4], cur.m[5], cur.m[6], cur.m[7],
                  cur.m[8], cur.m[9], cur.m[10], cur.m[11],
                  cur.m[12], cur.m[13], cur.m[14], cur.m[15]);
        }
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
        [super clipBlock:NO];
        
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
        width = x2 - x;
    }
    if(y < orY) {
        y = orY;
        height = y2 - y;
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
#ifdef USE_ES2
        clipX = x;
        
        clipW = width;
        if (clipX<0){
            clipX=0;
            clipW=width;
        }
        
        clipY = y;
        clipH = height;
        if (clipY<0){
            clipY=0;
            clipH=height;
        }
#else
        clipX = x;
        clipW = width;
        clipY = y;
        clipH = height;
#endif
        
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
#endif // CN1_USE_METAL

}


+(void)updateClipToScale {
    if ( clipIsTexture ){
        return;
    }
    int displayHeight = [CodenameOne_GLViewController instance].view.bounds.size.height * scaleValue;
    if(currentScaleX == 1 && currentScaleY == 1) {
        //_glEnable(GL_SCISSOR_TEST);
        //CN1Log(@"Updating clip to scale");
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
