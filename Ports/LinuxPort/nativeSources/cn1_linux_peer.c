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

/*
 * Generic native-peer placement for the native Codename One Linux port. An app
 * @NativeInterface that returns a GtkWidget (boxed as a long) is placed into the
 * window's GtkFixed overlay and moved/sized/shown to track the lightweight
 * PeerComponent. peerCaptureArgb renders the widget to a CN1 ARGB int[] for the
 * transition peer-image path. All GtkWidget work runs on the GTK main thread.
 */

#include "cn1_linux_gfx.h"
#include <stdlib.h>
#include <string.h>

extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__JAVA_INT;

typedef struct { GtkWidget* w; int x, y, width, height; int out[2]; JAVA_INT* argb; } CN1PeerOp;

static void cn1PeerInitOnMain(void* p) {
    CN1PeerOp* op = (CN1PeerOp*) p;
    cn1LinuxOverlayAdd(op->w, op->x, op->y, op->width, op->height);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_peerInitialized___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1PeerOp op;
    op.w = (GtkWidget*) (intptr_t) peer;
    if (!op.w) { return; }
    op.x = x; op.y = y; op.width = w; op.height = h;
    cn1LinuxRunOnMainAndWait(cn1PeerInitOnMain, &op);
}

static void cn1PeerBoundsOnMain(void* p) {
    CN1PeerOp* op = (CN1PeerOp*) p;
    cn1LinuxOverlayMove(op->w, op->x, op->y, op->width, op->height);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_peerSetBounds___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1PeerOp op;
    op.w = (GtkWidget*) (intptr_t) peer;
    if (!op.w) { return; }
    op.x = x; op.y = y; op.width = w; op.height = h;
    cn1LinuxRunOnMainAndWait(cn1PeerBoundsOnMain, &op);
}

static void cn1PeerVisibleOnMain(void* p) {
    CN1PeerOp* op = (CN1PeerOp*) p;
    if (op->x) {
        gtk_widget_show(op->w);
    } else {
        gtk_widget_hide(op->w);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_peerSetVisible___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_BOOLEAN visible) {
    CN1PeerOp op;
    op.w = (GtkWidget*) (intptr_t) peer;
    if (!op.w) { return; }
    op.x = visible ? 1 : 0;
    cn1LinuxRunOnMainAndWait(cn1PeerVisibleOnMain, &op);
}

static void cn1PeerDeinitOnMain(void* p) {
    CN1PeerOp* op = (CN1PeerOp*) p;
    cn1LinuxOverlayRemove(op->w); /* the app still owns the widget's lifetime */
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_peerDeinitialized___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1PeerOp op;
    op.w = (GtkWidget*) (intptr_t) peer;
    if (!op.w) { return; }
    cn1LinuxRunOnMainAndWait(cn1PeerDeinitOnMain, &op);
}

static void cn1PeerPrefOnMain(void* p) {
    CN1PeerOp* op = (CN1PeerOp*) p;
    GtkRequisition req;
    gtk_widget_get_preferred_size(op->w, 0, &req);
    op->out[0] = req.width;
    op->out[1] = req.height;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_peerCalcPreferredSize___long_int_int_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT dispW, JAVA_INT dispH, JAVA_OBJECT out) {
    CN1PeerOp op;
    (void) dispW;
    (void) dispH;
    op.w = (GtkWidget*) (intptr_t) peer;
    op.out[0] = 0;
    op.out[1] = 0;
    if (op.w) {
        cn1LinuxRunOnMainAndWait(cn1PeerPrefOnMain, &op);
    }
    if (out != JAVA_NULL && (*(JAVA_ARRAY) out).length >= 2) {
        ((JAVA_INT*) (*(JAVA_ARRAY) out).data)[0] = op.out[0];
        ((JAVA_INT*) (*(JAVA_ARRAY) out).data)[1] = op.out[1];
    }
}

static void cn1PeerCaptureOnMain(void* p) {
    CN1PeerOp* op = (CN1PeerOp*) p;
    GtkAllocation alloc;
    cairo_surface_t* surface;
    cairo_t* cr;
    unsigned char* base;
    int stride, x, y;
    gtk_widget_get_allocation(op->w, &alloc);
    if (alloc.width <= 0 || alloc.height <= 0) {
        op->out[0] = 0;
        op->out[1] = 0;
        op->argb = 0;
        return;
    }
    surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, alloc.width, alloc.height);
    cr = cairo_create(surface);
    gtk_widget_draw(op->w, cr);
    cairo_destroy(cr);
    cairo_surface_flush(surface);
    base = cairo_image_surface_get_data(surface);
    stride = cairo_image_surface_get_stride(surface);
    op->argb = (JAVA_INT*) malloc((size_t) alloc.width * alloc.height * 4);
    for (y = 0; y < alloc.height; y++) {
        uint32_t* row = (uint32_t*) (base + y * stride);
        for (x = 0; x < alloc.width; x++) {
            uint32_t pix = row[x];
            uint32_t a = (pix >> 24) & 0xff, r = (pix >> 16) & 0xff, g = (pix >> 8) & 0xff, b = pix & 0xff;
            if (a != 0 && a != 255) {
                r = r * 255 / a; g = g * 255 / a; b = b * 255 / a;
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
            }
            op->argb[y * alloc.width + x] = (JAVA_INT) ((a << 24) | (r << 16) | (g << 8) | b);
        }
    }
    op->out[0] = alloc.width;
    op->out[1] = alloc.height;
    cairo_surface_destroy(surface);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_peerCaptureArgb___long_int_1ARRAY_R_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT outDims) {
    CN1PeerOp op;
    JAVA_OBJECT arr;
    op.w = (GtkWidget*) (intptr_t) peer;
    op.argb = 0;
    op.out[0] = 0;
    op.out[1] = 0;
    if (!op.w) {
        return JAVA_NULL;
    }
    cn1LinuxRunOnMainAndWait(cn1PeerCaptureOnMain, &op);
    if (!op.argb || op.out[0] <= 0) {
        return JAVA_NULL;
    }
    arr = allocArray(threadStateData, op.out[0] * op.out[1], &class_array1__JAVA_INT, sizeof(JAVA_INT), 1);
    if (arr != JAVA_NULL) {
        memcpy((*(JAVA_ARRAY) arr).data, op.argb, (size_t) op.out[0] * op.out[1] * 4);
    }
    free(op.argb);
    if (outDims != JAVA_NULL && (*(JAVA_ARRAY) outDims).length >= 2) {
        ((JAVA_INT*) (*(JAVA_ARRAY) outDims).data)[0] = op.out[0];
        ((JAVA_INT*) (*(JAVA_ARRAY) outDims).data)[1] = op.out[1];
    }
    return arr;
}
