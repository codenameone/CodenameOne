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
#ifndef CN1Metalcompat_h
#define CN1Metalcompat_h

#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL

@import Metal;
@import simd;
#import <UIKit/UIKit.h>
#import <GLKit/GLKit.h>

// Metal rendering backend for Codename One iOS.
//
// The existing OpenGL ES 2 ExecutableOp implementations call raw GL functions
// (glUseProgram, glUniformMatrix4fv, glDrawArrays). They cannot transparently
// run on Metal, so each op gets an #ifdef CN1_USE_METAL branch in its execute
// method that calls the higher-level C API declared here.
//
// State lives in CN1Metalcompat.m:
//   - active render command encoder (set by METALView.setFramebuffer,
//     cleared by presentFramebuffer)
//   - projection + modelView + transform matrices
//   - current clip rect / scissor
//   - pipeline state cache (lazy built, keyed by pipeline variant + blend)
//
// Matrices use the same GLKMatrix4 type as the GL path so SetTransform.m can
// pass its matrix through unchanged; CN1Metalcompat converts to simd_float4x4
// at upload time.

// -------- Pipeline variants --------
typedef NS_ENUM(NSInteger, CN1MetalPipeline) {
    CN1MetalPipelineSolidColor = 0,    // FillRect, DrawLine, FillPolygon
    CN1MetalPipelineTexturedRGBA,      // DrawImage, TileImage
    CN1MetalPipelineAlphaMask,         // DrawString glyph, DrawTextureAlphaMask
    CN1MetalPipelineClearPunch,        // ClearRect: write zeros, no blend
    CN1MetalPipelineLinearGradient,    // Phase 2
    CN1MetalPipelineRadialGradient,    // Phase 2
    CN1MetalPipelineCount
};

// -------- Uniform struct matching CN1MetalShaders.metal --------
// Vertex stage receives this struct at buffer index 1.
typedef struct {
    simd_float4x4 projection;
    simd_float4x4 modelView;
    simd_float4x4 transform;
} CN1MetalMatrices;

// -------- Encoder lifecycle (called by CodenameOne_GLViewController / METALView) --------

// Called by METALView.setFramebuffer after acquiring a command encoder for
// the screen drawable. Captures the encoder so ops can issue draw calls
// against it. Also captures the projection matrix for the current drawable
// size. A nil encoder is valid and means "frame dropped" — ops must no-op.
void CN1MetalBeginFrame(id<MTLRenderCommandEncoder> encoder,
                        simd_float4x4 projection,
                        int framebufferWidth,
                        int framebufferHeight);

// Called by METALView.presentFramebuffer just before commit. Clears the
// active encoder reference so subsequent ops no-op until the next frame.
void CN1MetalEndFrame(void);

// Returns the active encoder or nil if no frame is in flight. Ops use this
// to skip drawing when setFramebuffer couldn't acquire a drawable.
id<MTLRenderCommandEncoder> CN1MetalActiveEncoder(void);

// Access to the framebuffer dimensions for the current encoder.
int CN1MetalFramebufferWidth(void);
int CN1MetalFramebufferHeight(void);

// -------- Matrix state (mirrors CN1modelViewMatrix / CN1projectionMatrix / CN1transformMatrix in the GL path) --------

// Called by SetTransform.execute. Replaces the current transform matrix.
void CN1MetalSetTransform(GLKMatrix4 transform);

// Returns the current transform matrix (for SetTransform.currentTransform).
GLKMatrix4 CN1MetalGetTransform(void);

// Matrix stack operations used by the drawFrame flip workaround, Rotate,
// Scale, ResetAffine. (Phase 2.)
void CN1MetalPushMatrix(void);
void CN1MetalPopMatrix(void);
void CN1MetalScale(float x, float y, float z);
void CN1MetalTranslate(float x, float y, float z);
void CN1MetalRotate(float angle, float x, float y, float z);
void CN1MetalLoadIdentity(void);

// -------- Clip state (mirrors ClipRect.m) --------

// Set a scissor rect in framebuffer pixel coordinates (Y-down, matching
// our projection). Passing width<=0 or height<=0 disables clipping.
void CN1MetalSetScissor(int x, int y, int width, int height);

// -------- Draw primitives (invoked from ExecutableOp subclasses' execute methods) --------

// Fill a rectangle with a solid color + alpha (0-255 each). x/y/w/h in
// iOS Y-down framebuffer coordinates (matches the GL path's semantic after
// its drawFrame flip).
void CN1MetalFillRect(int color, int alpha, int x, int y, int width, int height);

// Same geometry but punches zero into color+alpha with no blending —
// equivalent to the ClearRect fragment shader writing vec4(0,0,0,0).
void CN1MetalClearRect(int x, int y, int width, int height);

// Draw an RGBA image to (x,y,w,h) with a uniform alpha modulator (0-255).
// Texture is owned by the caller (typically a GLUIImage); it is retained
// only for the current command buffer.
void CN1MetalDrawImage(id<MTLTexture> texture, int alpha, int x, int y, int width, int height);

// -------- Texture helpers for GLUIImage --------

// Lazily build an MTLTexture from a UIImage. Cached on the GLUIImage.
// Returns nil on failure. Caller does not own the texture.
id<MTLTexture> CN1MetalTextureFromUIImage(UIImage *image);

// Global Metal device (from METALView's command queue); shared by anyone
// who needs to allocate Metal resources.
id<MTLDevice> CN1MetalDevice(void);

#endif /* CN1_USE_METAL */
#endif /* CN1Metalcompat_h */
