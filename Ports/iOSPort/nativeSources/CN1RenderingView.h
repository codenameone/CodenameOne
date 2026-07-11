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
#include "TargetConditionals.h"

// Shared method surface implemented by the rendering backends:
//   - EAGLView          (OpenGL ES 2, iOS)
//   - METALView         (Metal, iOS)
//   - CN1WatchRenderingView (Core Graphics, watchOS)
// CodenameOne_GLViewController calls through this protocol so it can drive any
// backend behind the CN1_USE_METAL / TARGET_OS_WATCH ifdefs.
//
// addPeerComponent takes a UIView* on iOS. watchOS has no UIView hierarchy and
// no native peer components, so the argument degrades to id there (callers on
// the watch slice pass nil; the watch backend ignores it).
@protocol CN1RenderingView <NSObject>
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;
- (void)deleteFramebuffer;
- (void)updateFrameBufferSize:(int)w h:(int)h;
#if TARGET_OS_WATCH
- (void)addPeerComponent:(id)view;
#else
- (void)addPeerComponent:(UIView *)view;
#endif
- (void)keyboardDoneClicked;
- (void)keyboardNextClicked;
- (void)textFieldDidChange;
@optional
- (void)invalidateRetainedFramebuffer;
- (void)prepareRetainedFramebufferForDrawRect:(CGRect)rect displayWidth:(int)displayWidth displayHeight:(int)displayHeight;
@end

#endif
