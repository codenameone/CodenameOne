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
// CN1_USE_METAL marks a slice that renders through Metal, which is every
// platform this port builds for except watchOS: watchOS has no Metal (and
// never had OpenGL ES either), so the watch slice renders through Core
// Graphics in CN1WatchRenderingView / CN1CGGraphics instead. There is no
// second renderer to choose between and nothing for the builder to switch:
// the OpenGL ES 2 backend this file used to carry has been removed.
#if !TARGET_OS_WATCH
#define CN1_USE_METAL
#endif
// IPhoneBuilder.java replaces the line below with one of:
//   #define CN1_METAL_COLORSPACE_SRGB
//   #define CN1_METAL_COLORSPACE_DISPLAY_P3
//   #define CN1_METAL_COLORSPACE_DEVICE_RGB
//   #define CN1_METAL_COLORSPACE_LINEAR_SRGB
//   #define CN1_METAL_COLORSPACE_EXTENDED_SRGB
//   #define CN1_METAL_COLORSPACE_EXTENDED_LINEAR_SRGB
//   #define CN1_METAL_COLORSPACE_NONE
// based on the `ios.metal.colorSpace` build hint. METALView.m falls back
// to sRGB when none of these are defined.
//#define CN1_METAL_COLORSPACE_PLACEHOLDER

// The transform currency of the drawing ops: CN1MetalSetTransform takes one
// of these, SetTransform stores one, and the watch backend reads the affine
// 2x3 submatrix out of it. Column-major, and layout-compatible with the
// GLKit GLKMatrix4 this used to be -- GLKit is OpenGL's math helper and went
// out with the OpenGL ES backend, and nothing here ever called a GLK
// function, only brace-initialized the type and indexed .m.
typedef struct { float m[16]; } CN1Matrix4;

#import "ExecutableOp.h"
