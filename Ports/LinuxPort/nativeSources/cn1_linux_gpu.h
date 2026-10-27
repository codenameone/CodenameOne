/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
#ifndef CN1_LINUX_GPU_H
#define CN1_LINUX_GPU_H

/*
 * The embedded target's GPU renderer (cn1_linux_gpu.c): OpenGL ES 2 through EGL,
 * on a GBM surface scanned out by DRM/KMS, or offscreen on the memory display.
 *
 * It is a second back end of the renderer in cn1_linux_raster.c rather than a
 * second renderer. The drawing units keep calling the cn1r_* API; a context
 * whose target surface is the GPU window hands each operation here as one
 * device-space rectangle, a source (a colour or an image) and an optional 8-bit
 * coverage mask the raster unit computed exactly as it would for the CPU. So a
 * shape is antialiased identically on both paths, and the GPU does the part
 * that dominates a frame: compositing images, glyphs and fills into the window
 * and putting the window on the screen. The design is the one the iOS port's
 * OpenGL ES renderer used -- quads, a texture per image, shapes and glyphs as
 * alpha-mask textures -- with every mask packed into one atlas so a frame of
 * text is a handful of draw calls.
 *
 * Compiled only when the build selects it (the linux.embedded.renderer build
 * hint, or a board profile that defaults to it); without CN1_EMBEDDED_GPU none
 * of this exists and the software renderer is the only one. When the build has
 * it but the device has no usable GPU, initialisation fails and the port runs
 * the software renderer instead.
 */

#include "cn1_linux_raster.h"

#ifdef __cplusplus
extern "C" {
#endif

/* What a GPU operation draws: a premultiplied colour, or an image surface
 * sampled through toSource (device pixels -> surface pixels). */
typedef struct {
    int solid;
    uint32_t pixel;
    cairo_surface_t* surface;
    cairo_matrix_t toSource;
    int filter;
} CN1GpuSource;

/* Brings up EGL on a DRM device the display unit opened and owns (fd, the CRTC
 * and connector it chose, the mode's size). 0 on success; on failure nothing is
 * left behind and the display unit falls back to dumb buffers. */
int cn1GpuInitKms(int drmFd, unsigned crtc, unsigned connector, const void* drmModeInfo,
        int physW, int physH, int rotation);
/* Offscreen: the memory display, where present reads the window back. */
int cn1GpuInitOffscreen(void);
int cn1GpuActive(void);
/* 1 when the GPU puts frames on the screen itself (KMS); 0 offscreen. */
int cn1GpuScansOut(void);

/* Makes `window` (an image surface of the logical size) the GPU render target. */
int cn1GpuAttachWindow(cairo_surface_t* window);

/* Composites src into target's rectangle [x0, x1) x [y0, y1), weighted by alpha
 * (0..255) and by cov (an 8-bit mask of the rectangle's size, pitch bytes a row;
 * NULL for full coverage), under a cairo_operator_t. */
void cn1GpuDraw(cairo_surface_t* target, int x0, int y0, int x1, int y1,
        const CN1GpuSource* src, unsigned alpha, int op, const unsigned char* cov, int pitch);

/* Reads the GPU window back into its surface's pixels: all of it, or a rectangle.
 * cn1GpuReadRect answers 0 when it could not read (no GPU window, or the context
 * is current on another thread). */
void cn1GpuSync(cairo_surface_t* target);
int cn1GpuReadRect(cairo_surface_t* target, int x, int y, int w, int h);

/* Puts the window on the screen (KMS): one textured quad, rotated, then a page
 * flip on the next vertical blank. */
void cn1GpuPresent(void);

/* A surface that had a texture is gone; its texture is deleted on the GL thread. */
void cn1GpuForgetTexture(unsigned texture);

/* The hardware cursor plane (KMS): shows the arrow with its tip at the physical
 * pixel (px, py), or hides it. 0 when the device has no cursor plane. */
int cn1GpuCursor(int show, int px, int py);

/* Restores nothing itself; releases the GPU's buffers before the display unit
 * puts the console's CRTC back. */
void cn1GpuShutdown(void);

#ifdef __cplusplus
}
#endif

#endif /* CN1_LINUX_GPU_H */
