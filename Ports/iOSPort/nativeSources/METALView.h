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

@import Metal;
#import "GLUIImage.h"


// This class wraps the CAEAGLLayer from CoreAnimation into a convenient UIView subclass.
// The view content is basically an EAGL surface you render your OpenGL scene into.
// Note that setting the view non-opaque will only work if the EAGL surface has an alpha channel.
@interface METALView : UIView<UITextViewDelegate, UITextFieldDelegate> {
@private
    // The pixel dimensions of the CAEAGLLayer.
    int framebufferWidth;
    int framebufferHeight;
    
    // The OpenGL ES names for the framebuffer and renderbuffer used to render to this view.
    GLuint defaultFramebuffer, colorRenderbuffer;
    
}
@property (nonatomic, retain) MTLCommandQueue* commandQueue;
@property (nonatomic, retain) MTLCommandBuffer* commandBuffer;
@property (nonatomic, retain) MTLRenderPassDescriptor* renderPassDescriptor;
@property (nonatomic, retain) MTLRenderCommandEncoder* renderCommandEncoder;
@property (nonatomic, retain) MTLDrawable* drawable;
@property (nonatomic, retain) UIView* peerComponentsLayer;

-(void)textViewDidChange:(UITextView *)textView;
-(void)deleteFramebuffer;
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;
-(void)updateFrameBufferSize:(int)w h:(int)h;
-(void)textFieldDidChange;
-(void) keyboardDoneClicked;
-(void) keyboardNextClicked;
-(void) addPeerComponent:(UIView*) view;
-(void) removePeerComponent:(UIView*) view;
@end
#endif