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
#import "CN1METALTransform.h"
@import simd;

// Global matrix state
simd_float4x4 CN1_Metal_ProjectionMatrix;
simd_float4x4 CN1_Metal_ModelViewMatrix;
simd_float4x4 CN1_Metal_TransformMatrix;

int CN1_Metal_ProjectionMatrixVersion = 0;
int CN1_Metal_ModelViewMatrixVersion = 0;
int CN1_Metal_TransformMatrixVersion = 0;

// Matrix stack for nested transforms (max depth 32)
#define MAX_MATRIX_STACK_DEPTH 32
static simd_float4x4 matrixStack[MAX_MATRIX_STACK_DEPTH];
static int matrixStackTop = -1;

void CN1_Metal_InitMatrices(int framebufferWidth, int framebufferHeight) {
    // NSLog(@"CN1_Metal_InitMatrices called with width=%d, height=%d", framebufferWidth, framebufferHeight);

    // Create orthographic projection for 2D UI rendering
    // Origin at top-left (0,0), Y increases downward (UIKit convention)
    CN1_Metal_ProjectionMatrix = CN1_Metal_MakeOrtho(
        0, framebufferWidth,
        framebufferHeight, 0,  // Flipped Y for UIKit coordinates
        -1, 1
    );
    CN1_Metal_ProjectionMatrixVersion++;

    // NSLog(@"Projection matrix after init: [%.6f %.6f %.6f %.6f] [%.6f %.6f %.6f %.6f] [%.6f %.6f %.6f %.6f] [%.6f %.6f %.6f %.6f]",
    //       CN1_Metal_ProjectionMatrix.columns[0][0], CN1_Metal_ProjectionMatrix.columns[0][1], CN1_Metal_ProjectionMatrix.columns[0][2], CN1_Metal_ProjectionMatrix.columns[0][3],
    //       CN1_Metal_ProjectionMatrix.columns[1][0], CN1_Metal_ProjectionMatrix.columns[1][1], CN1_Metal_ProjectionMatrix.columns[1][2], CN1_Metal_ProjectionMatrix.columns[1][3],
    //       CN1_Metal_ProjectionMatrix.columns[2][0], CN1_Metal_ProjectionMatrix.columns[2][1], CN1_Metal_ProjectionMatrix.columns[2][2], CN1_Metal_ProjectionMatrix.columns[2][3],
    //       CN1_Metal_ProjectionMatrix.columns[3][0], CN1_Metal_ProjectionMatrix.columns[3][1], CN1_Metal_ProjectionMatrix.columns[3][2], CN1_Metal_ProjectionMatrix.columns[3][3]);

    // Initialize model-view and transform to identity
    CN1_Metal_ModelViewMatrix = matrix_identity_float4x4;
    CN1_Metal_ModelViewMatrixVersion++;

    CN1_Metal_TransformMatrix = matrix_identity_float4x4;
    CN1_Metal_TransformMatrixVersion++;

    // Reset matrix stack
    matrixStackTop = -1;
}

void CN1_Metal_SetProjectionMatrix(simd_float4x4 matrix) {
    CN1_Metal_ProjectionMatrix = matrix;
    CN1_Metal_ProjectionMatrixVersion++;
}

void CN1_Metal_SetModelViewMatrix(simd_float4x4 matrix) {
    CN1_Metal_ModelViewMatrix = matrix;
    CN1_Metal_ModelViewMatrixVersion++;
}

void CN1_Metal_SetTransformMatrix(simd_float4x4 matrix) {
    CN1_Metal_TransformMatrix = matrix;
    CN1_Metal_TransformMatrixVersion++;
}

simd_float4x4 CN1_Metal_GetMVPMatrix(void) {
    // Combine matrices: Projection * ModelView * Transform
    return simd_mul(CN1_Metal_ProjectionMatrix,
                    simd_mul(CN1_Metal_ModelViewMatrix,
                             CN1_Metal_TransformMatrix));
}

void CN1_Metal_Translate(float x, float y, float z) {
    simd_float4x4 translation = CN1_Metal_MakeTranslation(x, y, z);
    CN1_Metal_TransformMatrix = simd_mul(CN1_Metal_TransformMatrix, translation);
    CN1_Metal_TransformMatrixVersion++;
}

void CN1_Metal_Scale(float x, float y, float z) {
    simd_float4x4 scale = CN1_Metal_MakeScale(x, y, z);
    CN1_Metal_TransformMatrix = simd_mul(CN1_Metal_TransformMatrix, scale);
    CN1_Metal_TransformMatrixVersion++;
}

void CN1_Metal_Rotate(float angle, float x, float y, float z) {
    simd_float4x4 rotation = CN1_Metal_MakeRotation(angle, x, y, z);
    CN1_Metal_TransformMatrix = simd_mul(CN1_Metal_TransformMatrix, rotation);
    CN1_Metal_TransformMatrixVersion++;
}

void CN1_Metal_PushMatrix(void) {
    if (matrixStackTop < MAX_MATRIX_STACK_DEPTH - 1) {
        matrixStackTop++;
        matrixStack[matrixStackTop] = CN1_Metal_TransformMatrix;
    } else {
        NSLog(@"CN1METALTransform: Matrix stack overflow!");
    }
}

void CN1_Metal_PopMatrix(void) {
    if (matrixStackTop >= 0) {
        CN1_Metal_TransformMatrix = matrixStack[matrixStackTop];
        matrixStackTop--;
        CN1_Metal_TransformMatrixVersion++;
    } else {
        NSLog(@"CN1METALTransform: Matrix stack underflow!");
    }
}

void CN1_Metal_LoadIdentity(void) {
    CN1_Metal_TransformMatrix = matrix_identity_float4x4;
    CN1_Metal_TransformMatrixVersion++;
}

// Helper: Create orthographic projection matrix
simd_float4x4 CN1_Metal_MakeOrtho(float left, float right, float bottom, float top, float near, float far) {
    simd_float4x4 m = matrix_identity_float4x4;
    m.columns[0][0] = 2.0f / (right - left);
    m.columns[1][1] = 2.0f / (top - bottom);
    m.columns[2][2] = -2.0f / (far - near);
    m.columns[3][0] = -(right + left) / (right - left);
    m.columns[3][1] = -(top + bottom) / (top - bottom);
    m.columns[3][2] = -(far + near) / (far - near);
    m.columns[3][3] = 1.0f;
    return m;
}

// Helper: Create translation matrix
simd_float4x4 CN1_Metal_MakeTranslation(float x, float y, float z) {
    simd_float4x4 m = matrix_identity_float4x4;
    m.columns[3][0] = x;
    m.columns[3][1] = y;
    m.columns[3][2] = z;
    return m;
}

// Helper: Create scale matrix
simd_float4x4 CN1_Metal_MakeScale(float x, float y, float z) {
    simd_float4x4 m = matrix_identity_float4x4;
    m.columns[0][0] = x;
    m.columns[1][1] = y;
    m.columns[2][2] = z;
    return m;
}

// Helper: Create rotation matrix around arbitrary axis
simd_float4x4 CN1_Metal_MakeRotation(float angleRadians, float x, float y, float z) {
    // Normalize axis
    float length = sqrtf(x * x + y * y + z * z);
    if (length == 0) {
        return matrix_identity_float4x4;
    }
    x /= length;
    y /= length;
    z /= length;

    float c = cosf(angleRadians);
    float s = sinf(angleRadians);
    float t = 1.0f - c;

    simd_float4x4 m = matrix_identity_float4x4;
    m.columns[0][0] = t * x * x + c;
    m.columns[0][1] = t * x * y + s * z;
    m.columns[0][2] = t * x * z - s * y;

    m.columns[1][0] = t * x * y - s * z;
    m.columns[1][1] = t * y * y + c;
    m.columns[1][2] = t * y * z + s * x;

    m.columns[2][0] = t * x * z + s * y;
    m.columns[2][1] = t * y * z - s * x;
    m.columns[2][2] = t * z * z + c;

    return m;
}

#endif // CN1_USE_METAL
