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
#if TARGET_OS_WATCH
#import "CN1CGGraphics.h"
#endif
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
#if TARGET_OS_WATCH
    // Core Graphics clip: rectangular clip -> CN1CGSetClipRect; polygon clip
    // -> CN1CGSetClipPolygon; texture-mask clip falls back to its bbox.
    if (numPoints > 0 && xPoints != NULL && yPoints != NULL) {
        CN1CGSetClipPolygon(xPoints, yPoints, numPoints);
        clipApplied = YES;
    } else {
        int sx = x, sy = y, sw = width, sh = height;
        if (sx < 0) { sw += sx; sx = 0; }
        if (sy < 0) { sh += sy; sy = 0; }
        // Issue #5273: confine a screen clip to the current flush region
        // (drawingRect), mirroring the Metal and GL/ES2 branches below. The
        // watch CG surface is persistent across partial repaints, so without
        // this clamp a clip emitted during a partial flush (e.g. an
        // independently scrollable BorderLayout.CENTER under a fixed band) can
        // extend past the flushed sub-region and overwrite the fixed band,
        // which then stays corrupted until a full repaint -- the same defect
        // the Metal backend had. This is scoped to screen ops by drawFrame,
        // which sets drawingRect to the flush region only while draining the
        // screen op queue and resets it to zero afterwards; a mutable-image
        // draw runs immediately outside drawFrame with drawingRect zero, so the
        // non-empty guard below makes the clamp a no-op for it. The guard also
        // covers the pre-first-flush state, so a clip is never collapsed to
        // nothing and the watch surface can never be blanked.
        if (drawingRect.size.width > 0 && drawingRect.size.height > 0) {
            int sx2 = sx + sw;
            int sy2 = sy + sh;
            int orX = (int)drawingRect.origin.x;
            int orY = (int)drawingRect.origin.y;
            int destX2 = (int)(drawingRect.origin.x + drawingRect.size.width);
            int destY2 = (int)(drawingRect.origin.y + drawingRect.size.height);
            if (sx < orX) { sx = orX; sw = sx2 - sx; }
            if (sy < orY) { sy = orY; sh = sy2 - sy; }
            if (sx2 > destX2) { sw = destX2 - sx; }
            if (sy2 > destY2) { sh = destY2 - sy; }
        }
        if (sw > 0 && sh > 0) {
            CN1CGSetClipRect(sx, sy, sw, sh);
            clipApplied = YES;
        } else {
            // NOTE (issue #5263): an empty clip on the watch CG backend
            // should cull everything, but watch text fields / pickers
            // currently compute a degenerate clip and rely on this reset to
            // stay visible. Culling here regresses ChatInput/ChatView/the
            // lightweight picker, so the watch empty-clip fix is deferred to
            // a follow-up that also fixes the underlying clip computation.
            CN1CGResetClip();
            clipApplied = NO;
        }
    }
#else
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
        // Issue #5273: confine a screen clip to the current flush region
        // (drawingRect). On a partial repaint -- e.g. an independently
        // scrollable BorderLayout.CENTER container scrolling under a fixed
        // toolbar / BorderLayout.NORTH -- paintDirty() flushes only the dirty
        // sub-region, yet a clip emitted during that flush can still extend
        // above it into the fixed header band. The GL backend clamps every
        // clip to drawingRect (see the ES2 branch below), so an escaping draw
        // can never touch pixels outside the flushed region. The Metal backend
        // was missing that clamp: because it renders into a PERSISTENT
        // screenTexture (MTLLoadActionLoad) the escaping fill overwrote the
        // toolbar / NORTH band and it stayed blank until a full repaint (e.g.
        // opening the overflow menu). Mirror the GL clamp here. Screen ops only
        // (target == nil): mutable-image draws run against their own encoder
        // with their own framebuffer bounds, where drawingRect (the screen
        // flush rect) does not apply.
        if ([self target] == nil) {
            int sx2 = sx + sw;
            int sy2 = sy + sh;
            int orX = (int)drawingRect.origin.x;
            int orY = (int)drawingRect.origin.y;
            int destX2 = (int)(drawingRect.origin.x + drawingRect.size.width);
            int destY2 = (int)(drawingRect.origin.y + drawingRect.size.height);
            if (sx < orX) { sx = orX; sw = sx2 - sx; }
            if (sy < orY) { sy = orY; sh = sy2 - sy; }
            if (sx2 > destX2) { sw = destX2 - sx; }
            if (sy2 > destY2) { sh = destY2 - sy; }
        }
        CN1MetalDisablePolygonStencilClip();
        CN1MetalSetScissor(sx, sy, sw, sh);
        clipApplied = (sw > 0 && sh > 0);
    }
#else
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
#endif // TARGET_OS_WATCH

}


+(void)updateClipToScale {
    if ( clipIsTexture ){
        return;
    }
#if !defined(CN1_USE_METAL) && !TARGET_OS_WATCH
    int displayHeight = [CodenameOne_GLViewController instance].view.bounds.size.height * scaleValue;
    if(currentScaleX == 1 && currentScaleY == 1) {
        //_glEnable(GL_SCISSOR_TEST);
        //CN1Log(@"Updating clip to scale");
        glScissor(clipX, displayHeight - clipY - clipH, clipW, clipH);
    }
#endif // !CN1_USE_METAL
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
