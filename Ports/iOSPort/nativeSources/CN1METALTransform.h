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

#ifndef CN1METALTransform_h
#define CN1METALTransform_h

#ifdef CN1_USE_METAL
#import <Foundation/Foundation.h>
@import simd;

// Global matrix state for Metal rendering
extern simd_float4x4 CN1_Metal_ProjectionMatrix;
extern simd_float4x4 CN1_Metal_ModelViewMatrix;
extern simd_float4x4 CN1_Metal_TransformMatrix;

// Version tracking for matrix changes (optimization)
extern int CN1_Metal_ProjectionMatrixVersion;
extern int CN1_Metal_ModelViewMatrixVersion;
extern int CN1_Metal_TransformMatrixVersion;

// Initialize matrices for 2D rendering
void CN1_Metal_InitMatrices(int framebufferWidth, int framebufferHeight);

// Set matrices
void CN1_Metal_SetProjectionMatrix(simd_float4x4 matrix);
void CN1_Metal_SetModelViewMatrix(simd_float4x4 matrix);
void CN1_Metal_SetTransformMatrix(simd_float4x4 matrix);

// Get combined MVP matrix (Projection * ModelView * Transform)
simd_float4x4 CN1_Metal_GetMVPMatrix(void);

// Transform operations
void CN1_Metal_Translate(float x, float y, float z);
void CN1_Metal_Scale(float x, float y, float z);
void CN1_Metal_Rotate(float angle, float x, float y, float z);

// Matrix stack operations (for nested transforms)
void CN1_Metal_PushMatrix(void);
void CN1_Metal_PopMatrix(void);
void CN1_Metal_LoadIdentity(void);

// Helper functions
simd_float4x4 CN1_Metal_MakeOrtho(float left, float right, float bottom, float top, float near, float far);
simd_float4x4 CN1_Metal_MakeTranslation(float x, float y, float z);
simd_float4x4 CN1_Metal_MakeScale(float x, float y, float z);
simd_float4x4 CN1_Metal_MakeRotation(float angle, float x, float y, float z);

#endif // CN1_USE_METAL
#endif // CN1METALTransform_h
