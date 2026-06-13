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
#ifndef CN1_LINUX_GFX_H
#define CN1_LINUX_GFX_H

/*
 * Render-stack internals for the GTK3 translation units (window / graphics /
 * text / image). Pulls in GTK + Cairo + Pango + GdkPixbuf, so only the rendering
 * units include it (the POSIX io/net/socket units include the GTK-free
 * cn1_linux.h instead).
 *
 * NOTE: this layer was seeded from the proven native Windows port's structure
 * (Direct2D/DirectWrite) and reimplemented against GTK3/Cairo/Pango/GdkPixbuf. It
 * compiles against the real GTK headers and the 2D render path has been run
 * headless under Xvfb (see Ports/LinuxPort/status.md). Some inline comments still
 * reference the Windows stack and are being migrated.
 */

#include "cn1_linux.h"
#include <gtk/gtk.h>
#include <cairo.h>
#include <pango/pangocairo.h>
#include <gdk-pixbuf/gdk-pixbuf.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * A drawing target: a Cairo context over a backing image surface. The on-screen
 * window target and every mutable/offscreen image share this struct, so all the
 * graphics primitives work uniformly against any target (the Windows port's
 * CN1Graphics analog). Pen state (colour/alpha/font/clip/transform) is held here
 * rather than on the Cairo context so getColor/getAlpha/getClip* can read it
 * back the way the Codename One graphics SPI expects.
 */
typedef struct CN1Graphics {
    cairo_surface_t* surface;   /* ARGB32 backing surface */
    cairo_t* cr;                /* context drawing into surface */
    int width;
    int height;
    int color;                  /* 0xRRGGBB */
    int alpha;                  /* 0..255 */
    int clipX, clipY, clipW, clipH;
    struct CN1Font* font;       /* current font (not owned) */
    cairo_matrix_t transform;   /* current affine (identity by default) */
    int isWindowTarget;         /* 1 for the on-screen/headless window buffer */
} CN1Graphics;

/* A resolved Pango font description + a metrics snapshot for fast char widths. */
typedef struct CN1Font {
    PangoFontDescription* desc;
    int pixelSize;
    int ascent;
    int height;
} CN1Font;

/* A decoded image: an ARGB32 Cairo surface; getImageGraphics wraps it lazily. */
typedef struct CN1Image {
    cairo_surface_t* surface;
    int width;
    int height;
    CN1Graphics* mutableGraphics; /* non-NULL once getImageGraphics was called */
} CN1Image;

/* The shared PangoContext (created once over the default font map). */
PangoContext* cn1LinuxPangoContext(void);

/* Applies g->color/alpha as the current Cairo source. */
void cn1LinuxApplySource(CN1Graphics* g);

/* The process-wide window graphics target (the on-screen/headless back buffer). */
CN1Graphics* cn1LinuxWindowGraphics(void);

/* Encodes an ARGB32 Cairo surface to PNG bytes; caller g_free()s *outData. */
int cn1LinuxSurfaceToPng(cairo_surface_t* surface, unsigned char** outData, int* outLen);

/* Native-peer overlay management (defined in cn1_linux_window.c). The GtkFixed
 * sits in a pass-through GtkOverlay over the Cairo drawing area; these place /
 * move / remove a native widget tracking a lightweight PeerComponent. Must run
 * on the GTK main thread. */
void cn1LinuxOverlayAdd(GtkWidget* w, int x, int y, int width, int height);
void cn1LinuxOverlayMove(GtkWidget* w, int x, int y, int width, int height);
void cn1LinuxOverlayRemove(GtkWidget* w);

/* The top-level GtkWindow (NULL in headless mode). */
GtkWidget* cn1LinuxWindowWidget(void);

/* Runs fn(arg) on the GTK main loop and blocks the caller until done (inline in
 * headless mode). For GTK calls the EDT must not make directly. */
void cn1LinuxRunOnMainAndWait(void (*fn)(void*), void* arg);

#ifdef __cplusplus
}
#endif

#endif /* CN1_LINUX_GFX_H */
