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
#ifdef CN1_USE_METAL
#import <UIKit/UIKit.h>
#import <QuartzCore/CAMetalLayer.h>

@import Metal;
@import simd;
#import "GLUIImage.h"
#import "CN1RenderingView.h"


// Metal-backed rendering view. Wraps a CAMetalLayer into a UIView subclass.
// Gated by CN1_USE_METAL; the OpenGL ES 2 backend (EAGLView) is the default.
@interface METALView : UIView<UITextViewDelegate, UITextFieldDelegate, CN1RenderingView> {
@private
    // The pixel dimensions of the CAMetalLayer's drawable.
    int framebufferWidth;
    int framebufferHeight;

    // Orthographic projection matrix sized to (framebufferWidth, framebufferHeight).
    // Rebuilt by updateFrameBufferSize:h: and uploaded to shaders as a uniform.
    simd_float4x4 projectionMatrix;
}
@property (nonatomic, retain) id<MTLCommandQueue> commandQueue;
@property (nonatomic, retain) id<MTLCommandBuffer> commandBuffer;
@property (nonatomic, retain) MTLRenderPassDescriptor* renderPassDescriptor;
@property (nonatomic, retain) id<MTLRenderCommandEncoder> renderCommandEncoder;
@property (nonatomic, retain) id<CAMetalDrawable> drawable;
@property (nonatomic, retain) UIView* peerComponentsLayer;
@property (nonatomic, readonly) int framebufferWidth;
@property (nonatomic, readonly) int framebufferHeight;
@property (nonatomic, readonly) simd_float4x4 projectionMatrix;

-(void)textViewDidChange:(UITextView *)textView;
-(void)deleteFramebuffer;
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;
-(void)updateFrameBufferSize:(int)w h:(int)h;
-(void)textFieldDidChange;
-(void) keyboardDoneClicked;
-(void) keyboardNextClicked;
-(void) addPeerComponent:(UIView*) view;
@end
#endif