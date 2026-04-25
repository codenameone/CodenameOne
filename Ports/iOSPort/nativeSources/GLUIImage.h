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
    // via mtlMutableEncoder. After finishDrawingOnImage the encoder ends but
    // the command buffer stays uncommitted (deferred); pixel-reading paths
    // commit-and-wait via -flushMutable. The texture is the source of truth
    // for pixels until a UIImage snapshot is requested.
    id<MTLTexture> mtlMutableTexture;
    id<MTLCommandBuffer> mtlMutableCommandBuffer;
    id<MTLRenderCommandEncoder> mtlMutableEncoder;
    int mtlMutableWidth;
    int mtlMutableHeight;
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
#endif
@end
