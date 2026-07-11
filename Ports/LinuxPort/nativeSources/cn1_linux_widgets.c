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
 * Desktop "applet" widget windows for the com.codename1.surfaces framework:
 * each pinned widget (and the live-activity pill) is a frameless GTK3 toplevel
 * that blits ARGB pixels rasterized on the Codename One EDT by the shared
 * SurfaceRasterizer. LinuxWidgetBridge owns the lifecycle from the Java side.
 *
 * Window recipe: undecorated + GDK_WINDOW_TYPE_HINT_UTILITY + keep-above +
 * stick (all workspaces) + skip-taskbar/pager, app-paintable with the screen's
 * RGBA visual so the rounded corners baked into the pixel alpha actually
 * composite as translucency (null-checked fallback to the default visual on
 * non-compositing X servers -- corners then render on an opaque black square).
 *
 * Threading: the Codename One EDT calls the LinuxNative widget* bridges below;
 * they never touch GTK directly. Every GTK operation is marshaled onto the GTK
 * main loop with gdk_threads_add_idle + a heap payload (the port's documented
 * pattern, see cn1_linux_window.c). Idle callbacks run in enqueue order, so
 * create -> update -> move -> destroy sequencing needs no extra handshakes; a
 * mutex-guarded static slot table hands out ids synchronously while the window
 * itself materializes asynchronously. Events travel back through a
 * mutex-guarded string queue drained by the main-thread input pump
 * (LinuxImplementation.drainInput -> LinuxNative.widgetPollEvent), formatted
 * "<id>;click;<x>;<y>" / "<id>;moved;<x>;<y>" in window (logical) coordinates.
 *
 * Pixel format: cairo CAIRO_FORMAT_ARGB32 is PREMULTIPLIED alpha in a
 * native-endian 32-bit word; the Java side ships straight-alpha CN1 ARGB, so
 * the bridge premultiplies while copying. Reading/writing whole uint32 values
 * keeps the channel layout correct on both little and big endian hosts.
 *
 * WAYLAND HONESTY: a Wayland compositor exposes no global window positioning,
 * no keep-above and no stick to ordinary clients -- gtk_window_move /
 * gtk_window_set_keep_above / gtk_window_stick are silent no-ops there, and
 * configure-event reports (0,0). Widgets degrade to plain floating windows the
 * user positions manually (interactive moves via gtk_window_begin_move_drag
 * still work); the full applet behavior needs X11 or XWayland. Layering the
 * widgets properly under Wayland via gtk-layer-shell (dlopen'd, like libnotify)
 * is a noted future enhancement. Like the rest of this port, this unit has not
 * yet been exercised on real GTK hardware.
 */

#include "cn1_linux_gfx.h"
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern GtkWidget* cn1LinuxWindowWidget(void);

/* ------------------------------------------------------------- slot table */

#define CN1_WIDGET_SLOTS 64

typedef struct {
    /* Allocated synchronously on the calling (EDT) thread under cn1WidgetLock;
     * cleared only by the destroy op on the GTK thread, so a slot can never be
     * recycled while the old window's signal handlers might still fire. */
    gint64 id;                  /* nonzero when allocated */
    GtkWidget* window;          /* GTK main thread only */
    cairo_surface_t* surface;   /* premultiplied ARGB32; GTK main thread only */
    int winW, winH;             /* logical window size */
    int imgW, imgH;             /* pixel-buffer size (draw scales to winW/winH) */
    int* hitRects;              /* x,y,w,h quads, window coords; cn1WidgetLock */
    int hitRectCount;           /* number of quads; cn1WidgetLock */
    int x, y;                   /* last known window position; cn1WidgetLock */
    gint64 lastMovePush;        /* monotonic us of the last "moved" event; GTK thread */
} CN1WidgetSlot;

static CN1WidgetSlot cn1Widgets[CN1_WIDGET_SLOTS];
static pthread_mutex_t cn1WidgetLock = PTHREAD_MUTEX_INITIALIZER;
static gint64 cn1WidgetNextId = 1;

/* Push at most one "moved" event per this interval (microseconds); the Java
 * side re-reads the authoritative position via widgetGetX/Y when persisting,
 * so a swallowed trailing configure-event cannot leave stale geometry. */
#define CN1_WIDGET_MOVE_THROTTLE_US 200000

/* Returns the slot currently bound to id, or NULL. Caller holds cn1WidgetLock. */
static CN1WidgetSlot* cn1WidgetById(gint64 id) {
    int i;
    if (id == 0) {
        return 0;
    }
    for (i = 0; i < CN1_WIDGET_SLOTS; i++) {
        if (cn1Widgets[i].id == id) {
            return &cn1Widgets[i];
        }
    }
    return 0;
}

/* ------------------------------------------------------------ event queue */

#define CN1_WIDGET_EVENT_QUEUE 128
static char* cn1WidgetEvents[CN1_WIDGET_EVENT_QUEUE];
static int cn1WidgetEvHead = 0;
static int cn1WidgetEvTail = 0;
static pthread_mutex_t cn1WidgetEventLock = PTHREAD_MUTEX_INITIALIZER;

/* Queues "<id>;<kind>;<x>;<y>" for the Java side; drops (frees) when full. */
static void cn1WidgetPushEvent(gint64 id, const char* kind, int x, int y) {
    char* msg = g_strdup_printf("%" G_GINT64_FORMAT ";%s;%d;%d", id, kind, x, y);
    int next;
    pthread_mutex_lock(&cn1WidgetEventLock);
    next = (cn1WidgetEvTail + 1) % CN1_WIDGET_EVENT_QUEUE;
    if (next != cn1WidgetEvHead) {
        cn1WidgetEvents[cn1WidgetEvTail] = msg;
        cn1WidgetEvTail = next;
    } else {
        g_free(msg);
    }
    pthread_mutex_unlock(&cn1WidgetEventLock);
}

/* Next queued widget event, or NULL. The caller owns the string (g_free). */
char* cn1LinuxWidgetPollEvent(void) {
    char* msg = 0;
    pthread_mutex_lock(&cn1WidgetEventLock);
    if (cn1WidgetEvHead != cn1WidgetEvTail) {
        msg = cn1WidgetEvents[cn1WidgetEvHead];
        cn1WidgetEvHead = (cn1WidgetEvHead + 1) % CN1_WIDGET_EVENT_QUEUE;
    }
    pthread_mutex_unlock(&cn1WidgetEventLock);
    return msg;
}

/* --------------------------------------------------------- GTK callbacks */

static gboolean cn1WidgetOnDraw(GtkWidget* widget, cairo_t* cr, gpointer data) {
    CN1WidgetSlot* s = (CN1WidgetSlot*) data;
    (void) widget;
    /* Clear to fully transparent first (SOURCE, not OVER) so the alpha baked
     * into the rasterized pixels defines the window's translucent shape. */
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    cairo_set_source_rgba(cr, 0, 0, 0, 0);
    cairo_paint(cr);
    cairo_set_operator(cr, CAIRO_OPERATOR_OVER);
    if (s->surface && s->imgW > 0 && s->imgH > 0) {
        /* The bridge rasterizes at 2x for crispness; scale down to the window. */
        cairo_scale(cr, (double) s->winW / s->imgW, (double) s->winH / s->imgH);
        cairo_set_source_surface(cr, s->surface, 0, 0);
        cairo_pattern_set_filter(cairo_get_source(cr), CAIRO_FILTER_GOOD);
        cairo_paint(cr);
    }
    return FALSE;
}

static gboolean cn1WidgetOnButton(GtkWidget* widget, GdkEventButton* e, gpointer data) {
    CN1WidgetSlot* s = (CN1WidgetSlot*) data;
    int x, y, i, hit = 0;
    gint64 id;
    if (e->type != GDK_BUTTON_PRESS) {
        return FALSE;
    }
    x = (int) e->x;
    y = (int) e->y;
    pthread_mutex_lock(&cn1WidgetLock);
    id = s->id;
    for (i = 0; i < s->hitRectCount; i++) {
        int rx = s->hitRects[i * 4];
        int ry = s->hitRects[i * 4 + 1];
        int rw = s->hitRects[i * 4 + 2];
        int rh = s->hitRects[i * 4 + 3];
        if (x >= rx && x < rx + rw && y >= ry && y < ry + rh) {
            hit = 1;
            break;
        }
    }
    pthread_mutex_unlock(&cn1WidgetLock);
    if (hit) {
        cn1WidgetPushEvent(id, "click", x, y);
    } else {
        /* Anywhere outside an action rectangle drags the applet. Under Wayland
         * this is the ONE positioning primitive that still works (the
         * compositor drives the move). */
        gtk_window_begin_move_drag(GTK_WINDOW(widget), (gint) e->button,
                (gint) e->x_root, (gint) e->y_root, e->time);
    }
    return TRUE;
}

static gboolean cn1WidgetOnConfigure(GtkWidget* widget, GdkEventConfigure* e, gpointer data) {
    CN1WidgetSlot* s = (CN1WidgetSlot*) data;
    gint64 id = 0;
    int movedNow = 0;
    (void) widget;
    pthread_mutex_lock(&cn1WidgetLock);
    if (s->x != e->x || s->y != e->y) {
        s->x = e->x;
        s->y = e->y;
        id = s->id;
        movedNow = 1;
    }
    pthread_mutex_unlock(&cn1WidgetLock);
    if (movedNow) {
        /* Throttled; the cached x/y above is always current for widgetGetX/Y. */
        gint64 now = g_get_monotonic_time();
        if (now - s->lastMovePush >= CN1_WIDGET_MOVE_THROTTLE_US) {
            s->lastMovePush = now;
            cn1WidgetPushEvent(id, "moved", e->x, e->y);
        }
    }
    return FALSE;
}

static gboolean cn1WidgetOnDelete(GtkWidget* widget, GdkEvent* e, gpointer data) {
    (void) widget;
    (void) e;
    (void) data;
    /* The bridge owns the lifecycle; ignore WM close attempts. */
    return TRUE;
}

/* --------------------------------------------------------------- create */

typedef struct {
    gint64 id;
} CN1WidgetIdOp;

static gboolean cn1WidgetCreateOnMain(gpointer data) {
    CN1WidgetIdOp* op = (CN1WidgetIdOp*) data;
    CN1WidgetSlot* s;
    GtkWidget* win;
    GdkScreen* screen;
    GdkVisual* rgba;
    int defX, defY, index;
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById(op->id);
    pthread_mutex_unlock(&cn1WidgetLock);
    if (!s) {
        free(op);
        return FALSE;
    }
    win = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_decorated(GTK_WINDOW(win), FALSE);
    gtk_window_set_type_hint(GTK_WINDOW(win), GDK_WINDOW_TYPE_HINT_UTILITY);
    gtk_window_set_keep_above(GTK_WINDOW(win), TRUE);
    gtk_window_stick(GTK_WINDOW(win));
    gtk_window_set_skip_taskbar_hint(GTK_WINDOW(win), TRUE);
    gtk_window_set_skip_pager_hint(GTK_WINDOW(win), TRUE);
    /* An applet should never steal keyboard focus from the app; action clicks
     * explicitly present the main window instead (widgetFocusApp). */
    gtk_window_set_accept_focus(GTK_WINDOW(win), FALSE);
    gtk_window_set_resizable(GTK_WINDOW(win), FALSE);
    gtk_widget_set_app_paintable(win, TRUE);
    screen = gtk_widget_get_screen(win);
    rgba = screen ? gdk_screen_get_rgba_visual(screen) : 0;
    if (rgba) {
        /* Translucent rounded corners need the compositor's ARGB visual. */
        gtk_widget_set_visual(win, rgba);
    }
    gtk_window_set_default_size(GTK_WINDOW(win), s->winW, s->winH);
    gtk_widget_set_events(win, gtk_widget_get_events(win)
            | GDK_BUTTON_PRESS_MASK | GDK_STRUCTURE_MASK);
    g_signal_connect(win, "draw", G_CALLBACK(cn1WidgetOnDraw), s);
    g_signal_connect(win, "button-press-event", G_CALLBACK(cn1WidgetOnButton), s);
    g_signal_connect(win, "configure-event", G_CALLBACK(cn1WidgetOnConfigure), s);
    g_signal_connect(win, "delete-event", G_CALLBACK(cn1WidgetOnDelete), s);
    /* Default placement near the top-right, cascading by slot so several
     * widgets do not stack exactly; the bridge overrides with the persisted
     * position right after create (idles run in order). No-op under Wayland. */
    index = (int) (s - cn1Widgets);
    defX = 100;
    if (screen) {
        int sw = gdk_screen_get_width(screen);
        if (sw > s->winW + 40) {
            defX = sw - s->winW - 40;
        }
    }
    defY = 60 + (index % 8) * 40;
    gtk_window_move(GTK_WINDOW(win), defX, defY);
    pthread_mutex_lock(&cn1WidgetLock);
    s->window = win;
    s->x = defX;
    s->y = defY;
    pthread_mutex_unlock(&cn1WidgetLock);
    gtk_widget_show_all(win);
    free(op);
    return FALSE;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_widgetCreate___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT w, JAVA_INT h) {
    CN1WidgetSlot* s = 0;
    CN1WidgetIdOp* op;
    gint64 id;
    int i;
    if (cn1LinuxWindowWidget() == 0) {
        /* Headless / no GTK loop: no surface to materialize on. */
        return 0;
    }
    pthread_mutex_lock(&cn1WidgetLock);
    for (i = 0; i < CN1_WIDGET_SLOTS; i++) {
        if (cn1Widgets[i].id == 0) {
            s = &cn1Widgets[i];
            break;
        }
    }
    if (!s) {
        pthread_mutex_unlock(&cn1WidgetLock);
        return 0;
    }
    memset(s, 0, sizeof(CN1WidgetSlot));
    id = cn1WidgetNextId++;
    s->id = id;
    s->winW = w > 0 ? w : 1;
    s->winH = h > 0 ? h : 1;
    pthread_mutex_unlock(&cn1WidgetLock);
    op = (CN1WidgetIdOp*) malloc(sizeof(CN1WidgetIdOp));
    op->id = id;
    gdk_threads_add_idle(cn1WidgetCreateOnMain, op);
    return (JAVA_LONG) id;
}

/* --------------------------------------------------------------- pixels */

typedef struct {
    gint64 id;
    uint32_t* px;   /* premultiplied ARGB, tightly packed w*h */
    int w, h;
} CN1WidgetPixelsOp;

/* Straight CN1 ARGB -> premultiplied cairo ARGB32 (both are logical uint32
 * values with A in the top byte, so whole-word stores are endian-correct). */
static uint32_t cn1WidgetPremul(uint32_t p) {
    uint32_t a = p >> 24;
    uint32_t r, g, b;
    if (a == 255) {
        return p;
    }
    if (a == 0) {
        return 0;
    }
    r = (((p >> 16) & 0xff) * a + 127) / 255;
    g = (((p >> 8) & 0xff) * a + 127) / 255;
    b = ((p & 0xff) * a + 127) / 255;
    return (a << 24) | (r << 16) | (g << 8) | b;
}

static gboolean cn1WidgetPixelsOnMain(gpointer data) {
    CN1WidgetPixelsOp* op = (CN1WidgetPixelsOp*) data;
    CN1WidgetSlot* s;
    cairo_surface_t* surface;
    unsigned char* dst;
    int stride, row;
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById(op->id);
    pthread_mutex_unlock(&cn1WidgetLock);
    if (!s || !s->window) {
        free(op->px);
        free(op);
        return FALSE;
    }
    surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, op->w, op->h);
    if (cairo_surface_status(surface) == CAIRO_STATUS_SUCCESS) {
        cairo_surface_flush(surface);
        dst = cairo_image_surface_get_data(surface);
        stride = cairo_image_surface_get_stride(surface);
        for (row = 0; row < op->h; row++) {
            memcpy(dst + (size_t) row * stride, op->px + (size_t) row * op->w,
                    (size_t) op->w * 4);
        }
        cairo_surface_mark_dirty(surface);
        if (s->surface) {
            cairo_surface_destroy(s->surface);
        }
        s->surface = surface;
        s->imgW = op->w;
        s->imgH = op->h;
        gtk_widget_queue_draw(s->window);
    } else {
        cairo_surface_destroy(surface);
    }
    free(op->px);
    free(op);
    return FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_widgetUpdatePixels___long_int_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT argb, JAVA_INT w, JAVA_INT h) {
    CN1WidgetPixelsOp* op;
    JAVA_INT* src;
    int64_t count, i;
    if (peer == 0 || argb == JAVA_NULL || w <= 0 || h <= 0) {
        return;
    }
    count = (int64_t) w * h;
    if ((int64_t) (*(JAVA_ARRAY) argb).length < count) {
        return;
    }
    src = (JAVA_INT*) (*(JAVA_ARRAY) argb).data;
    op = (CN1WidgetPixelsOp*) malloc(sizeof(CN1WidgetPixelsOp));
    op->id = (gint64) peer;
    op->w = w;
    op->h = h;
    op->px = (uint32_t*) malloc((size_t) count * 4);
    for (i = 0; i < count; i++) {
        op->px[i] = cn1WidgetPremul((uint32_t) src[i]);
    }
    gdk_threads_add_idle(cn1WidgetPixelsOnMain, op);
}

/* -------------------------------------------------------------- position */

typedef struct {
    gint64 id;
    int x, y;
} CN1WidgetMoveOp;

static gboolean cn1WidgetMoveOnMain(gpointer data) {
    CN1WidgetMoveOp* op = (CN1WidgetMoveOp*) data;
    CN1WidgetSlot* s;
    int x = op->x;
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById(op->id);
    pthread_mutex_unlock(&cn1WidgetLock);
    if (s && s->window) {
        if (x < 0) {
            /* x == -1 centers horizontally (the live-activity pill docks
             * top-center without the Java side needing a screen-size native). */
            GdkScreen* screen = gtk_widget_get_screen(s->window);
            int sw = screen ? gdk_screen_get_width(screen) : 0;
            x = sw > s->winW ? (sw - s->winW) / 2 : 0;
        }
        /* Silent no-op under Wayland (see the file header). */
        gtk_window_move(GTK_WINDOW(s->window), x, op->y);
        pthread_mutex_lock(&cn1WidgetLock);
        s->x = x;
        s->y = op->y;
        pthread_mutex_unlock(&cn1WidgetLock);
    }
    free(op);
    return FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_widgetSetPosition___long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y) {
    CN1WidgetMoveOp* op;
    if (peer == 0) {
        return;
    }
    op = (CN1WidgetMoveOp*) malloc(sizeof(CN1WidgetMoveOp));
    op->id = (gint64) peer;
    op->x = x;
    op->y = y;
    gdk_threads_add_idle(cn1WidgetMoveOnMain, op);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_widgetGetX___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1WidgetSlot* s;
    int x = 0;
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById((gint64) peer);
    if (s) {
        x = s->x;
    }
    pthread_mutex_unlock(&cn1WidgetLock);
    return x;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_widgetGetY___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1WidgetSlot* s;
    int y = 0;
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById((gint64) peer);
    if (s) {
        y = s->y;
    }
    pthread_mutex_unlock(&cn1WidgetLock);
    return y;
}

/* -------------------------------------------------------------- hit rects */

JAVA_VOID com_codename1_impl_linux_LinuxNative_widgetSetHitRects___long_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT rects) {
    /* Plain shared data (no GTK), so a mutex-guarded swap suffices -- the
     * button-press handler reads it under the same lock. */
    CN1WidgetSlot* s;
    int* copy = 0;
    int count = 0;
    if (rects != JAVA_NULL) {
        int len = (int) (*(JAVA_ARRAY) rects).length;
        count = len / 4;
        if (count > 0) {
            JAVA_INT* src = (JAVA_INT*) (*(JAVA_ARRAY) rects).data;
            int i;
            copy = (int*) malloc((size_t) count * 4 * sizeof(int));
            for (i = 0; i < count * 4; i++) {
                copy[i] = src[i];
            }
        }
    }
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById((gint64) peer);
    if (s) {
        free(s->hitRects);
        s->hitRects = copy;
        s->hitRectCount = count;
        copy = 0;
    }
    pthread_mutex_unlock(&cn1WidgetLock);
    free(copy); /* slot vanished before the swap */
}

/* ---------------------------------------------------------------- destroy */

static gboolean cn1WidgetDestroyOnMain(gpointer data) {
    CN1WidgetIdOp* op = (CN1WidgetIdOp*) data;
    CN1WidgetSlot* s;
    pthread_mutex_lock(&cn1WidgetLock);
    s = cn1WidgetById(op->id);
    pthread_mutex_unlock(&cn1WidgetLock);
    if (s) {
        /* We ARE the GTK thread: destroying the window here runs its handlers
         * synchronously, so the surface/slot teardown below cannot race them. */
        if (s->window) {
            gtk_widget_destroy(s->window);
            s->window = 0;
        }
        if (s->surface) {
            cairo_surface_destroy(s->surface);
            s->surface = 0;
        }
        pthread_mutex_lock(&cn1WidgetLock);
        free(s->hitRects);
        s->hitRects = 0;
        s->hitRectCount = 0;
        s->id = 0; /* only now may the EDT recycle the slot */
        pthread_mutex_unlock(&cn1WidgetLock);
    }
    free(op);
    return FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_widgetDestroy___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1WidgetIdOp* op;
    if (peer == 0) {
        return;
    }
    op = (CN1WidgetIdOp*) malloc(sizeof(CN1WidgetIdOp));
    op->id = (gint64) peer;
    gdk_threads_add_idle(cn1WidgetDestroyOnMain, op);
}

/* ------------------------------------------------------------ focus / poll */

static gboolean cn1WidgetFocusAppOnMain(gpointer data) {
    GtkWidget* main = cn1LinuxWindowWidget();
    (void) data;
    if (main) {
        gtk_window_present(GTK_WINDOW(main));
    }
    return FALSE;
}

/* Presents (raises + focuses) the main app window; used when a widget action
 * is clicked so the handler surfaces in a visible app. */
void cn1LinuxWidgetFocusApp(void) {
    gdk_threads_add_idle(cn1WidgetFocusAppOnMain, 0);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_widgetFocusApp__(CODENAME_ONE_THREAD_STATE) {
    cn1LinuxWidgetFocusApp();
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_widgetPollEvent___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char* msg = cn1LinuxWidgetPollEvent();
    JAVA_OBJECT result;
    if (!msg) {
        return JAVA_NULL;
    }
    result = newStringFromCString(threadStateData, msg);
    g_free(msg);
    return result;
}
