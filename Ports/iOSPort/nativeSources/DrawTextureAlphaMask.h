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
#import "ExecutableOp.h"
#import "xmlvm.h"
#ifdef USE_ES2
#import <OpenGLES/ES2/gl.h>
#else
#import <OpenGLES/ES1/gl.h>
#endif

// textureName is a 64-bit handle:
//   - On the GL build it's a GLuint zero-extended to JAVA_LONG.
//   - On the Metal build it's a CFBridgingRetain'd id<MTLTexture> pointer
//     (set up by IOSNative.m's nativePathRendererCreateTexture under
//     CN1_USE_METAL and freed by nativeDeleteTexture).
@interface DrawTextureAlphaMask : ExecutableOp {
    JAVA_LONG textureName;
    int color;
    int alpha;
    int x;
    int y;
    int w;
    int h;
}

-(id)initWithArgs:(JAVA_LONG)pTextName color:(int)pColor alpha:(int)pAlpha x:(int)pX y:(int)pY w:(int)pW h:(int)pH;
-(void)execute;

@end
