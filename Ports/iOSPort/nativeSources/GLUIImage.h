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
    id<MTLTexture> mtlTexture;
    // Phase 3 v2: mutable-image render target. Allocated lazily by
    // CN1MetalEnsureMutableTexture sized to the mutable image's logical
    // dimensions. drawFrame opens an MTLRenderCommandEncoder against this
    // texture for any queued op whose target == this GLUIImage. Pixels
    // accumulate across frames via MTLLoadActionLoad. The screen-side
    // drawImage pipeline samples this texture (getMTLTexture returns it
    // when present, else falls back to building from the UIImage).
    id<MTLTexture> mtlMutableTexture;
    int mtlMutableWidth;
    int mtlMutableHeight;
    // The most-recently-committed command buffer that wrote to
    // mtlMutableTexture. Readback paths (Image.getRGB, PNG/JPEG encode,
    // toImage, cross-image consumption) call waitUntilCompleted on this
    // before sampling the texture. nil = no pending GPU work, safe to read.
    id<MTLCommandBuffer> mtlMutableCommandBuffer;
    // Initial fill colour passed to createNativeMutableImage(w, h, argb)
    // -- 0xAARRGGBB. CN1MetalEnsureMutableTexture clears the freshly-
    // allocated mtlMutableTexture to this colour so mutable images behave
    // like the CG path's UIRectFill(argb). Sentinel 0 means "honor
    // CN1's createImage(w,h) default of 0xffffffff opaque white" --
    // a literal argb of 0 (fully transparent black) is also the Metal
    // texture's natural cleared state, so the sentinel collision is a
    // no-op in practice.
    int mtlMutableInitialARGB;
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
// is unavailable or the image is empty. If a mutable texture exists
// (Phase 3 mutable-image render target), that is returned instead -- it
// is the freshest pixel source.
-(id<MTLTexture>)getMTLTexture;

// Phase 3 v2 mutable-image accessors. CN1Metalcompat owns the lifecycle;
// these are the storage hooks. Accessors only -- consumers outside this
// file route through the CN1Metal*MutableImage API rather than poking
// these directly.
-(id<MTLTexture>)mtlMutableTexture;
-(void)setMtlMutableTexture:(id<MTLTexture>)t width:(int)w height:(int)h;
-(int)mtlMutableWidth;
-(int)mtlMutableHeight;
-(id<MTLCommandBuffer>)mtlMutableCommandBuffer;
-(void)setMtlMutableCommandBuffer:(id<MTLCommandBuffer>)cb;
-(int)mtlMutableInitialARGB;
-(void)setMtlMutableInitialARGB:(int)argb;
#endif
@end
