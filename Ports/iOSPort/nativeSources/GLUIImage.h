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
#import <Foundation/Foundation.h>
#import <OpenGLES/EAGL.h>

#import <OpenGLES/ES1/gl.h>
#import <OpenGLES/ES1/glext.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <UIKit/UIKit.h>
#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
@import Metal;
#endif

@interface GLUIImage : NSObject {
    UIImage* img;
    GLuint textureName;
    NSString* name;
    int textureWidth;
    int textureHeight;
#ifdef CN1_USE_METAL
    // Cached read-only texture, built lazily from `img` for screen-side
    // DrawImage / TileImage. Invalidated by setImage:.
    id<MTLTexture> mtlTexture;

    // Phase 3: Metal-backed mutable-image storage. When this image is the
    // target of a startDrawingOnImage call, ops render into mtlMutableTexture
    // via mtlMutableEncoder. After finishDrawingOnImage the encoder ends and
    // the command buffer commits + waits (sync model). Pixel-reading paths
    // sample mtlMutableTexture directly.
    id<MTLTexture> mtlMutableTexture;
    id<MTLCommandBuffer> mtlMutableCommandBuffer;
    id<MTLRenderCommandEncoder> mtlMutableEncoder;
    int mtlMutableWidth;
    int mtlMutableHeight;
    // Transform applied to mutable draws (set by nativeSetTransformMutableImpl).
    // Stored as 16 floats so we can put it in a plain ivar without dragging
    // GLKMatrix4 into the header. Defaults to identity in initWithImage:.
    float mtlMutableTransform[16];
    // Pending clip rect from setNativeClippingMutableImpl. We can't safely
    // setScissorRect on the encoder from one thread and draw on another, so
    // we stash it here and EnterMutableScope applies it on the draw thread.
    // mtlMutableClipValid==NO means "no clip" (full framebuffer scissor).
    int mtlMutableClipX;
    int mtlMutableClipY;
    int mtlMutableClipW;
    int mtlMutableClipH;
    BOOL mtlMutableClipValid;
#endif
}
-(id)initWithImage:(UIImage*)i;
-(UIImage*)getImage;
-(GLuint)getTexture:(int)texWidth texHeight:(int)texHeight;
-(void)setName:(NSString*)s;
-(void)setImage:(UIImage*)i;
-(int)getTextureWidth;
-(int)getTextureHeight;
#ifdef CN1_USE_METAL
// Lazily build (and cache on the GLUIImage instance) an MTLTexture for
// this image. Invalidated automatically by setImage:. nil if the device
// is unavailable or the image is empty.
-(id<MTLTexture>)getMTLTexture;

// Phase 3 mutable-image accessors. Implementation in GLUIImage.m; the
// CN1Metalcompat.m mutable-image API is the public surface that callers
// outside this file should use.
-(id<MTLTexture>)mtlMutableTexture;
-(void)setMtlMutableTexture:(id<MTLTexture>)t width:(int)w height:(int)h;
-(id<MTLCommandBuffer>)mtlMutableCommandBuffer;
-(void)setMtlMutableCommandBuffer:(id<MTLCommandBuffer>)cb;
-(id<MTLRenderCommandEncoder>)mtlMutableEncoder;
-(void)setMtlMutableEncoder:(id<MTLRenderCommandEncoder>)e;
-(int)mtlMutableWidth;
-(int)mtlMutableHeight;
// Pointer to the 16-float transform array (GLKMatrix4-shaped). Allows
// CN1Metalcompat to read/write without copying or boxing.
-(float*)mtlMutableTransformPtr;
// Pending mutable-image clip accessors. setMutableClip stores; getMutableClip
// fills (x,y,w,h) and returns YES if a clip was set. EnterMutableScope reads
// and applies via setScissorRect on the encoder.
-(void)setMtlMutableClipX:(int)x y:(int)y w:(int)w h:(int)h;
-(BOOL)getMtlMutableClipX:(int*)x y:(int*)y w:(int*)w h:(int*)h;
-(void)clearMtlMutableClip;
#endif
@end
