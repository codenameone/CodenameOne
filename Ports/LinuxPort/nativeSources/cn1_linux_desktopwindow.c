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
 * Additional desktop windows for the Linux port.
 *
 * The application's main window keeps its own file statics in cn1_linux_window.c
 * and is untouched by this file. A Codename One Window takes a slot in the table
 * below, with its own GtkWindow, GtkOverlay, GtkDrawingArea, GtkFixed peer layer
 * and cairo back buffer, so the existing single-window path cannot change.
 *
 * Routing is free here: every GTK signal handler already receives a gpointer, so
 * passing the slot as the closure data makes each handler window-scoped with no
 * lookup at all. The message loop needs no change either -- gtk_main_iteration
 * already services every window in the process.
 *
 * GTK is not thread safe, so every entry point runs on the GTK main thread. The
 * EDT-facing natives marshal through cn1LinuxRunOnMainAndWait, which the port
 * already uses for exactly this.
 */

#include "cn1_linux.h"
#include "cn1_linux_gfx.h"
#include <gtk/gtk.h>
#include <string.h>

typedef struct {
    GtkWidget* window;
    GtkWidget* overlay;
    GtkWidget* drawingArea;
    GtkWidget* fixed;         /* positioned native peers live here */
    CN1Graphics g;            /* the window's cairo back buffer */
    int width;
    int height;
    int windowId;
    int monitorIndex;
    int inUse;
} CN1LinuxWindow;

static CN1LinuxWindow cn1DesktopWindows[CN1_MAX_DESKTOP_WINDOWS];

static CN1LinuxWindow* slotAt(int slot) {
    if (slot < 0 || slot >= CN1_MAX_DESKTOP_WINDOWS) {
        return 0;
    }
    if (!cn1DesktopWindows[slot].inUse) {
        return 0;
    }
    return &cn1DesktopWindows[slot];
}

CN1Graphics* cn1LinuxDesktopGraphics(int slot) {
    CN1LinuxWindow* w = slotAt(slot);
    return w == 0 ? 0 : &w->g;
}

GtkWidget* cn1LinuxDesktopWidget(int slot) {
    CN1LinuxWindow* w = slotAt(slot);
    return w == 0 ? 0 : w->window;
}

GtkWidget* cn1LinuxDesktopFixed(int slot) {
    CN1LinuxWindow* w = slotAt(slot);
    return w == 0 ? 0 : w->fixed;
}

/* ------------------------------------------------------------ back buffer */

static void cn1DesktopResizeBuffer(CN1LinuxWindow* w, int width, int height) {
    if (width <= 0) width = 1;
    if (height <= 0) height = 1;
    if (w->g.cr) {
        cairo_destroy(w->g.cr);
    }
    if (w->g.surface) {
        cairo_surface_destroy(w->g.surface);
    }
    w->g.surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, width, height);
    w->g.cr = cairo_create(w->g.surface);
    w->g.width = width;
    w->g.height = height;
    w->g.color = 0;
    w->g.alpha = 255;
    w->g.clipX = 0;
    w->g.clipY = 0;
    w->g.clipW = width;
    w->g.clipH = height;
    w->g.clipIsRect = 1;
    /* Enables the #5273 flush-region clip clamp, as for the main window: a clip
     * set while a component paints is confined to the region about to be flushed
     * so a fill cannot escape into the retained surface. */
    w->g.isWindowTarget = 1;
    cairo_matrix_init_identity(&w->g.transform);
}

/* ------------------------------------------------------- monitors */

static int cn1DesktopMonitorIndexFor(GtkWidget* window) {
    GdkDisplay* display = gdk_display_get_default();
    GdkWindow* gdkWindow;
    GdkMonitor* mon;
    int count;
    int iter;
    if (display == 0 || window == 0) {
        return 0;
    }
    gdkWindow = gtk_widget_get_window(window);
    if (gdkWindow == 0) {
        return 0;
    }
    mon = gdk_display_get_monitor_at_window(display, gdkWindow);
    count = gdk_display_get_n_monitors(display);
    for (iter = 0; iter < count; iter++) {
        if (gdk_display_get_monitor(display, iter) == mon) {
            return iter;
        }
    }
    return 0;
}

/* --------------------------------------------------------- GTK callbacks */

static gboolean cn1DesktopOnDraw(GtkWidget* widget, cairo_t* cr, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    (void) widget;
    if (w != 0 && w->g.surface) {
        cairo_set_source_surface(cr, w->g.surface, 0, 0);
        cairo_paint(cr);
    }
    return FALSE;
}

static gboolean cn1DesktopOnConfigure(GtkWidget* widget, GdkEventConfigure* e, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    (void) widget;
    if (w == 0) {
        return FALSE;
    }
    if (e->width != w->width || e->height != w->height) {
        w->width = e->width;
        w->height = e->height;
        cn1DesktopResizeBuffer(w, w->width, w->height);
        cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_SIZE_CHANGED, w->width, w->height, 0);
    }
    {
        int now = cn1DesktopMonitorIndexFor(w->window);
        if (now != w->monitorIndex) {
            w->monitorIndex = now;
            cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_MONITOR, 0, 0, now);
        }
    }
    return FALSE;
}

static int cn1DesktopButtonMask(guint button) {
    switch (button) {
        case 1: return CN1_PE_MASK_PRIMARY;
        case 2: return CN1_PE_MASK_MIDDLE;
        case 3: return CN1_PE_MASK_SECONDARY;
        case 8: return CN1_PE_MASK_BACK;
        case 9: return CN1_PE_MASK_FORWARD;
        default: return CN1_PE_MASK_PRIMARY;
    }
}

static gboolean cn1DesktopOnButton(GtkWidget* widget, GdkEventButton* e, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    (void) widget;
    if (w == 0) {
        return FALSE;
    }
    if (e->type == GDK_BUTTON_PRESS) {
        cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_POINTER_PRESSED,
                (int) e->x, (int) e->y, cn1DesktopButtonMask(e->button));
    } else if (e->type == GDK_BUTTON_RELEASE) {
        cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_POINTER_RELEASED,
                (int) e->x, (int) e->y, cn1DesktopButtonMask(e->button));
    }
    return FALSE;
}

static gboolean cn1DesktopOnMotion(GtkWidget* widget, GdkEventMotion* e, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    int mask = 0;
    (void) widget;
    if (w == 0) {
        return FALSE;
    }
    if (e->state & GDK_BUTTON1_MASK) { mask |= CN1_PE_MASK_PRIMARY; }
    if (e->state & GDK_BUTTON2_MASK) { mask |= CN1_PE_MASK_MIDDLE; }
    if (e->state & GDK_BUTTON3_MASK) { mask |= CN1_PE_MASK_SECONDARY; }
    if (mask != 0) {
        cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_POINTER_DRAGGED,
                (int) e->x, (int) e->y, mask);
    }
    return FALSE;
}

static gboolean cn1DesktopOnKey(GtkWidget* widget, GdkEventKey* e, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    (void) widget;
    if (w == 0) {
        return FALSE;
    }
    cn1LinuxPushWindowEvent(w->windowId,
            e->type == GDK_KEY_PRESS ? CN1_EVENT_KEY_PRESSED : CN1_EVENT_KEY_RELEASED,
            0, 0, (int) e->keyval);
    return FALSE;
}

static gboolean cn1DesktopOnDelete(GtkWidget* widget, GdkEvent* e, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    (void) widget;
    (void) e;
    if (w != 0) {
        cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_CLOSE, 0, 0, 0);
    }
    /* TRUE stops GTK destroying the window: Codename One decides, because an
     * application may veto the close from a listener. */
    return TRUE;
}

static gboolean cn1DesktopOnFocus(GtkWidget* widget, GdkEventFocus* e, gpointer data) {
    CN1LinuxWindow* w = (CN1LinuxWindow*) data;
    (void) widget;
    if (w != 0) {
        cn1LinuxPushWindowEvent(w->windowId, CN1_EVENT_WINDOW_FOCUS, 0, 0, e->in ? 1 : 0);
    }
    return FALSE;
}

/* ------------------------------------------------------- create / destroy */

typedef struct {
    int slot;
    int windowId;
    const char* title;
    int x;
    int y;
    int width;
    int height;
    int decorated;
    int resizable;
    int result;
} CN1DesktopCreateOp;

static void cn1DesktopCreateOnMain(void* arg) {
    CN1DesktopCreateOp* op = (CN1DesktopCreateOp*) arg;
    CN1LinuxWindow* w = &cn1DesktopWindows[op->slot];

    memset(w, 0, sizeof(*w));
    w->windowId = op->windowId;
    w->width = op->width > 0 ? op->width : 1;
    w->height = op->height > 0 ? op->height : 1;
    w->inUse = 1;

    w->window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(w->window), op->title != 0 ? op->title : "");
    gtk_window_set_default_size(GTK_WINDOW(w->window), w->width, w->height);
    gtk_window_set_decorated(GTK_WINDOW(w->window), op->decorated ? TRUE : FALSE);
    gtk_window_set_resizable(GTK_WINDOW(w->window), op->resizable ? TRUE : FALSE);
    if (op->x >= 0 && op->y >= 0) {
        gtk_window_move(GTK_WINDOW(w->window), op->x, op->y);
    }

    /* Same structure as the main window: a drawing area for the Codename One
     * back buffer, with a GtkFixed above it hosting native peers so a browser or
     * video widget can be positioned over the lightweight components. */
    w->overlay = gtk_overlay_new();
    w->drawingArea = gtk_drawing_area_new();
    w->fixed = gtk_fixed_new();
    gtk_widget_set_has_window(w->fixed, FALSE);

    gtk_container_add(GTK_CONTAINER(w->overlay), w->drawingArea);
    gtk_overlay_add_overlay(GTK_OVERLAY(w->overlay), w->fixed);
    gtk_container_add(GTK_CONTAINER(w->window), w->overlay);

    gtk_widget_add_events(w->drawingArea,
            GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK
            | GDK_POINTER_MOTION_MASK | GDK_SCROLL_MASK);

    /* Every handler takes the window as its closure data, which is what makes
     * routing free -- no lookup, no shared state. */
    g_signal_connect(w->drawingArea, "draw", G_CALLBACK(cn1DesktopOnDraw), w);
    g_signal_connect(w->drawingArea, "configure-event", G_CALLBACK(cn1DesktopOnConfigure), w);
    g_signal_connect(w->drawingArea, "button-press-event", G_CALLBACK(cn1DesktopOnButton), w);
    g_signal_connect(w->drawingArea, "button-release-event", G_CALLBACK(cn1DesktopOnButton), w);
    g_signal_connect(w->drawingArea, "motion-notify-event", G_CALLBACK(cn1DesktopOnMotion), w);
    g_signal_connect(w->window, "key-press-event", G_CALLBACK(cn1DesktopOnKey), w);
    g_signal_connect(w->window, "key-release-event", G_CALLBACK(cn1DesktopOnKey), w);
    g_signal_connect(w->window, "delete-event", G_CALLBACK(cn1DesktopOnDelete), w);
    g_signal_connect(w->window, "focus-in-event", G_CALLBACK(cn1DesktopOnFocus), w);
    g_signal_connect(w->window, "focus-out-event", G_CALLBACK(cn1DesktopOnFocus), w);

    cn1DesktopResizeBuffer(w, w->width, w->height);
    w->monitorIndex = cn1DesktopMonitorIndexFor(w->window);
    op->result = 1;
}

static void cn1DesktopDestroyOnMain(void* arg) {
    int slot = *((int*) arg);
    CN1LinuxWindow* w = slotAt(slot);
    if (w == 0) {
        return;
    }
    if (w->g.cr) {
        cairo_destroy(w->g.cr);
        w->g.cr = 0;
    }
    if (w->g.surface) {
        cairo_surface_destroy(w->g.surface);
        w->g.surface = 0;
    }
    if (w->window != 0) {
        gtk_widget_destroy(w->window);
        w->window = 0;
    }
    w->inUse = 0;
    w->windowId = 0;
}

typedef struct {
    int slot;
    int a;
    int b;
    int c;
    int d;
    const char* text;
    int out[4];
} CN1DesktopOp;

static void cn1DesktopShowOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w != 0) {
        if (op->a) {
            gtk_widget_show_all(w->window);
        } else {
            gtk_widget_hide(w->window);
        }
    }
}

static void cn1DesktopTitleOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w != 0) {
        gtk_window_set_title(GTK_WINDOW(w->window), op->text != 0 ? op->text : "");
    }
}

static void cn1DesktopBoundsOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w != 0) {
        gtk_window_move(GTK_WINDOW(w->window), op->a, op->b);
        gtk_window_resize(GTK_WINDOW(w->window), op->c > 0 ? op->c : 1, op->d > 0 ? op->d : 1);
    }
}

static void cn1DesktopGetBoundsOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w != 0) {
        gtk_window_get_position(GTK_WINDOW(w->window), &op->out[0], &op->out[1]);
        gtk_window_get_size(GTK_WINDOW(w->window), &op->out[2], &op->out[3]);
    }
}

static void cn1DesktopFlagOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w == 0) {
        return;
    }
    /* a selects the flag, b its value */
    switch (op->a) {
        case 0:
            gtk_window_set_resizable(GTK_WINDOW(w->window), op->b ? TRUE : FALSE);
            break;
        case 1:
            gtk_window_set_keep_above(GTK_WINDOW(w->window), op->b ? TRUE : FALSE);
            break;
        case 2:
            gtk_window_set_modal(GTK_WINDOW(w->window), op->b ? TRUE : FALSE);
            break;
        case 3:
            gtk_window_set_decorated(GTK_WINDOW(w->window), op->b ? TRUE : FALSE);
            break;
        default:
            break;
    }
}

static void cn1DesktopStateOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w == 0) {
        return;
    }
    if (op->a == 1) {
        gtk_window_iconify(GTK_WINDOW(w->window));
    } else if (op->a == 2) {
        GdkWindow* gw = gtk_widget_get_window(w->window);
        if (gw != 0 && (gdk_window_get_state(gw) & GDK_WINDOW_STATE_MAXIMIZED)) {
            gtk_window_unmaximize(GTK_WINDOW(w->window));
        } else {
            gtk_window_maximize(GTK_WINDOW(w->window));
        }
    } else if (op->a == 3) {
        gtk_window_present(GTK_WINDOW(w->window));
    } else {
        gtk_window_deiconify(GTK_WINDOW(w->window));
    }
}

static void cn1DesktopFlushOnMain(void* arg) {
    CN1DesktopOp* op = (CN1DesktopOp*) arg;
    CN1LinuxWindow* w = slotAt(op->slot);
    if (w != 0 && w->drawingArea != 0) {
        gtk_widget_queue_draw_area(w->drawingArea, op->a, op->b, op->c, op->d);
    }
}

/* ------------------------------------------------------ LinuxNative bridge */

JAVA_INT com_codename1_impl_linux_LinuxNative_desktopWindowCreate___int_java_lang_String_int_int_int_int_boolean_boolean_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT windowId, JAVA_OBJECT title,
        JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height,
        JAVA_BOOLEAN decorated, JAVA_BOOLEAN resizable) {
    CN1DesktopCreateOp op;
    char* utf8 = 0;
    int slot = -1;
    int iter;
    for (iter = 0; iter < CN1_MAX_DESKTOP_WINDOWS; iter++) {
        if (!cn1DesktopWindows[iter].inUse) {
            slot = iter;
            break;
        }
    }
    if (slot < 0) {
        return -1;
    }
    if (title != JAVA_NULL) {
        utf8 = cn1LinuxJStrDup(threadStateData, title);
    }
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.windowId = windowId;
    op.title = utf8 != 0 ? utf8 : "";
    op.x = x;
    op.y = y;
    op.width = width;
    op.height = height;
    op.decorated = decorated == JAVA_TRUE ? 1 : 0;
    op.resizable = resizable == JAVA_TRUE ? 1 : 0;
    cn1LinuxRunOnMainAndWait(cn1DesktopCreateOnMain, &op);
    if (utf8 != 0) {
        free(utf8);
    }
    return op.result ? slot : -1;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowDestroy___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    int s = slot;
    cn1LinuxRunOnMainAndWait(cn1DesktopDestroyOnMain, &s);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowShow___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_BOOLEAN visible) {
    CN1DesktopOp op;
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.a = visible == JAVA_TRUE ? 1 : 0;
    cn1LinuxRunOnMainAndWait(cn1DesktopShowOnMain, &op);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowSetTitle___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_OBJECT title) {
    CN1DesktopOp op;
    char* utf8 = title == JAVA_NULL ? 0 : cn1LinuxJStrDup(threadStateData, title);
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.text = utf8;
    cn1LinuxRunOnMainAndWait(cn1DesktopTitleOnMain, &op);
    if (utf8 != 0) {
        free(utf8);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowSetBounds___int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT x, JAVA_INT y,
        JAVA_INT width, JAVA_INT height) {
    CN1DesktopOp op;
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.a = x;
    op.b = y;
    op.c = width;
    op.d = height;
    cn1LinuxRunOnMainAndWait(cn1DesktopBoundsOnMain, &op);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowGetBounds___int_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_OBJECT out) {
    CN1DesktopOp op;
    JAVA_INT* arr;
    if (out == JAVA_NULL) {
        return;
    }
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    cn1LinuxRunOnMainAndWait(cn1DesktopGetBoundsOnMain, &op);
    arr = (JAVA_INT*) (*(JAVA_ARRAY) out).data;
    if ((int) (*(JAVA_ARRAY) out).length >= 4) {
        arr[0] = op.out[0];
        arr[1] = op.out[1];
        arr[2] = op.out[2];
        arr[3] = op.out[3];
    }
}

JAVA_INT com_codename1_impl_linux_LinuxNative_desktopWindowGetWidth___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1LinuxWindow* w = slotAt(slot);
    return w == 0 ? 0 : w->width;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_desktopWindowGetHeight___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1LinuxWindow* w = slotAt(slot);
    return w == 0 ? 0 : w->height;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_desktopWindowGraphics___int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    return (JAVA_LONG) (intptr_t) cn1LinuxDesktopGraphics(slot);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowFlush___int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT x, JAVA_INT y,
        JAVA_INT width, JAVA_INT height) {
    CN1DesktopOp op;
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.a = x;
    op.b = y;
    op.c = width > 0 ? width : 1;
    op.d = height > 0 ? height : 1;
    cn1LinuxRunOnMainAndWait(cn1DesktopFlushOnMain, &op);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowSetFlag___int_int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT flag, JAVA_BOOLEAN value) {
    CN1DesktopOp op;
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.a = flag;
    op.b = value == JAVA_TRUE ? 1 : 0;
    cn1LinuxRunOnMainAndWait(cn1DesktopFlagOnMain, &op);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_desktopWindowSetState___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot, JAVA_INT state) {
    CN1DesktopOp op;
    memset(&op, 0, sizeof(op));
    op.slot = slot;
    op.a = state;
    cn1LinuxRunOnMainAndWait(cn1DesktopStateOnMain, &op);
}

/* ---- monitors ---- */

JAVA_INT com_codename1_impl_linux_LinuxNative_monitorCount___R_int(CODENAME_ONE_THREAD_STATE) {
    GdkDisplay* display = gdk_display_get_default();
    if (display == 0) {
        return 1;
    }
    {
        int n = gdk_display_get_n_monitors(display);
        return n > 0 ? n : 1;
    }
}

JAVA_INT com_codename1_impl_linux_LinuxNative_primaryMonitor___R_int(CODENAME_ONE_THREAD_STATE) {
    GdkDisplay* display = gdk_display_get_default();
    GdkMonitor* primary;
    int count;
    int iter;
    if (display == 0) {
        return 0;
    }
    primary = gdk_display_get_primary_monitor(display);
    count = gdk_display_get_n_monitors(display);
    for (iter = 0; iter < count; iter++) {
        if (gdk_display_get_monitor(display, iter) == primary) {
            return iter;
        }
    }
    return 0;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_monitorBounds___int_boolean_int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_INT monitor, JAVA_BOOLEAN workArea, JAVA_OBJECT out) {
    GdkDisplay* display = gdk_display_get_default();
    GdkMonitor* mon;
    GdkRectangle r;
    JAVA_INT* arr;
    if (out == JAVA_NULL || display == 0) {
        return;
    }
    mon = gdk_display_get_monitor(display, monitor);
    if (mon == 0) {
        mon = gdk_display_get_primary_monitor(display);
    }
    if (mon == 0) {
        return;
    }
    if (workArea == JAVA_TRUE) {
        gdk_monitor_get_workarea(mon, &r);
    } else {
        gdk_monitor_get_geometry(mon, &r);
    }
    arr = (JAVA_INT*) (*(JAVA_ARRAY) out).data;
    if ((int) (*(JAVA_ARRAY) out).length >= 4) {
        arr[0] = r.x;
        arr[1] = r.y;
        arr[2] = r.width;
        arr[3] = r.height;
    }
}

JAVA_INT com_codename1_impl_linux_LinuxNative_monitorScale___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT monitor) {
    GdkDisplay* display = gdk_display_get_default();
    GdkMonitor* mon;
    if (display == 0) {
        return 1;
    }
    mon = gdk_display_get_monitor(display, monitor);
    if (mon == 0) {
        return 1;
    }
    return gdk_monitor_get_scale_factor(mon);
}

/*
 * Physical dots per inch, derived from the monitor's reported millimetre size.
 * GTK exposes only an integer scale factor, which is too coarse to describe a
 * display's real resolution, so this reports the measured value and lets the Java
 * side keep the integer factor separately.
 */
JAVA_INT com_codename1_impl_linux_LinuxNative_monitorDpi___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT monitor) {
    GdkDisplay* display = gdk_display_get_default();
    GdkMonitor* mon;
    GdkRectangle r;
    int widthMm;
    if (display == 0) {
        return 96;
    }
    mon = gdk_display_get_monitor(display, monitor);
    if (mon == 0) {
        return 96;
    }
    gdk_monitor_get_geometry(mon, &r);
    widthMm = gdk_monitor_get_width_mm(mon);
    if (widthMm <= 0 || r.width <= 0) {
        return 96;
    }
    return (int) ((r.width * 25.4) / widthMm + 0.5);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_monitorForWindow___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT slot) {
    CN1LinuxWindow* w = slotAt(slot);
    if (w == 0) {
        return 0;
    }
    return w->monitorIndex;
}
