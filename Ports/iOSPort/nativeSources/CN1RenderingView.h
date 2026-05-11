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
#ifndef CN1RenderingView_h
#define CN1RenderingView_h
#import <UIKit/UIKit.h>

// Shared method surface implemented by both EAGLView (OpenGL ES 2 backend)
// and METALView (Metal backend). CodenameOne_GLViewController calls through
// this protocol so it can drive either backend under the CN1_USE_METAL ifdef.
@protocol CN1RenderingView <NSObject>
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;
- (void)deleteFramebuffer;
- (void)updateFrameBufferSize:(int)w h:(int)h;
- (void)addPeerComponent:(UIView *)view;
- (void)keyboardDoneClicked;
- (void)keyboardNextClicked;
- (void)textFieldDidChange;
@end

#endif
