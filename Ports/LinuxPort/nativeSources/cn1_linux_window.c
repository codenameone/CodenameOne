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
 * Windowing, the GTK main-loop pump, the input ring buffer and the on-screen /
 * headless render target for the native Codename One Linux port.
 *
 * Threading: GTK runs on the process main thread (the app's main() calls
 * Display.init -> initDisplay here, then owns the loop via pumpMessages). The
 * Codename One EDT is a separate translated thread that draws into the shared
 * back-buffer Cairo surface and calls flushGraphics; flushGraphics marshals the
 * redraw onto the main loop (g_idle_add) since GTK widget calls are not
 * thread-safe. The draw signal blits the back buffer to the window.
 *
 * NOTE: implemented against GTK3/Cairo but not yet compiled/run on a Linux/GTK
 * host -- see Ports/LinuxPort/status.md.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE   /* pthread_getattr_np + ucontext REG_* gregs; before any libc include */
#endif
#include "cn1_linux_gfx.h"
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>
#include <ucontext.h>
#ifdef __GLIBC__
#include <execinfo.h> /* backtrace() -- glibc only; musl has no execinfo.h */
#endif
#include <unistd.h>

/* ----------------------------------------------------------- event ring */

#define CN1_EVENT_RING 1024
typedef struct {
    int type, x, y, key;
    /* Which window the event came from. Zero is the application's main window,
     * which is every event this port produced before desktop windows existed. */
    int windowId;
} CN1Event;
static CN1Event cn1EventRing[CN1_EVENT_RING];
static int cn1EventHead = 0;
static int cn1EventTail = 0;
static pthread_mutex_t cn1EventLock = PTHREAD_MUTEX_INITIALIZER;


void cn1LinuxPushEvent(int type, int x, int y, int keyCode) {
    cn1LinuxPushWindowEvent(0, type, x, y, keyCode);
}

/* Events the framework cannot reconstruct if they are lost, of which there are two
 * kinds.
 *
 * Lifecycle: a lost hide leaves a window the framework believes is on screen, painting
 * and animating until something else happens to it, and a lost close leaves it
 * registered with no native window behind it.
 *
 * Terminations: a release ends something a press started. Lose it and the component
 * the press went to stays in that state for good -- the key goes on repeating, the
 * button stays down, the drag never finishes -- and the focus change that would
 * otherwise cancel a held gesture is no use as a backstop if it is droppable too.
 *
 * Note the asymmetry with presses, which stay droppable: a release that arrives with
 * no press behind it finds no recorded target and is discarded harmlessly, so when
 * something has to go it must never be the release. */
/* Unlike hover motion, leave has no later motion outside the window to repair
 * a dropped notification. Protect only the terminal sentinel, not the motion stream. */
static int cn1LinuxIsProtectedEvent(int type, int x, int y) {
    return type == CN1_EVENT_WINDOW_SHOWN || type == CN1_EVENT_WINDOW_HIDDEN
            || type == CN1_EVENT_WINDOW_CLOSE
            || type == CN1_EVENT_KEY_RELEASED
            || type == CN1_EVENT_POINTER_RELEASED
            || type == CN1_EVENT_WINDOW_FOCUS
            || type == CN1_EVENT_SIZE_CHANGED
            || (type == CN1_EVENT_POINTER_HOVER && x == -1 && y == -1);
}

/* Visibility only. A close request is protected from eviction like any other
 * lifecycle event, but it is not a state that a later one supersedes: the delete
 * signal does not destroy the window, so a close that a subsequent minimize overwrote
 * would take the close listener and the close operation with it. */
static int cn1LinuxStateClass(int type) {
    if (type == CN1_EVENT_WINDOW_SHOWN || type == CN1_EVENT_WINDOW_HIDDEN) {
        return 1;
    }
    if (type == CN1_EVENT_SIZE_CHANGED) {
        return 2;
    }
    return 0;
}

/* Replaces a queued visibility event for the same window with this newer one. The
 * latest state is the one that matters -- a hide followed by a show leaves the window
 * shown -- so superseding costs nothing and needs no room. */
static int cn1LinuxCoalesceLifecycleLocked(int windowId, int type, int x, int y,
        int keyCode) {
    int idx = cn1EventHead;
    int newest = -1;
    int cls = cn1LinuxStateClass(type);
    if (cls == 0) {
        return 0;
    }
    /* The *newest* match, not the first one found. A window can already have more than
     * one transition queued -- a hide then a show -- and replacing the older of the two
     * leaves the newer one as the last word, so the framework would end up believing a
     * window that is natively hidden is on screen, and go on painting it. */
    while (idx != cn1EventTail) {
        if (cn1EventRing[idx].windowId == windowId
                && cn1LinuxStateClass(cn1EventRing[idx].type) == cls) {
            newest = idx;
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    if (newest < 0) {
        return 0;
    }
    cn1EventRing[newest].type = type;
    cn1EventRing[newest].x = x;
    cn1EventRing[newest].y = y;
    cn1EventRing[newest].key = keyCode;
    return 1;
}

/* Removes the oldest droppable event, closing the gap. Used to make room for a
 * protected one: advancing the head instead would evict whatever is oldest, and that
 * can be a protected event itself -- which is the very thing being kept. */
static void cn1LinuxRemoveAtLocked(int idx) {
    int cur = idx;
    int follow = (cur + 1) % CN1_EVENT_RING;
    while (follow != cn1EventTail) {
        cn1EventRing[cur] = cn1EventRing[follow];
        cur = follow;
        follow = (follow + 1) % CN1_EVENT_RING;
    }
    cn1EventTail = cur;
}

static int cn1LinuxEvictInputLocked(void) {
    int idx = cn1EventHead;
    while (idx != cn1EventTail) {
        if (!cn1LinuxIsProtectedEvent(cn1EventRing[idx].type,
                cn1EventRing[idx].x, cn1EventRing[idx].y)) {
            cn1LinuxRemoveAtLocked(idx);
            return 1;
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    return 0;
}

/* Last resort when the ring holds nothing but lifecycle events and so has no input to
 * give up. A window that toggled visibility several times before the framework drained
 * anything has more than one transition queued, and every one but its last is already
 * superseded, so dropping the oldest of them frees a slot without changing what any
 * window ends up as. Without this a close arriving for a *different* window has nowhere
 * to go and is dropped, which is the one outcome this whole path exists to prevent. */
static int cn1LinuxEvictSupersededVisibilityLocked(void) {
    int idx = cn1EventHead;
    while (idx != cn1EventTail) {
        int cls = cn1LinuxStateClass(cn1EventRing[idx].type);
        if (cls != 0) {
            int scan = (idx + 1) % CN1_EVENT_RING;
            while (scan != cn1EventTail) {
                if (cn1EventRing[scan].windowId == cn1EventRing[idx].windowId
                        && cn1LinuxStateClass(cn1EventRing[scan].type) == cls) {
                    cn1LinuxRemoveAtLocked(idx);
                    return 1;
                }
                scan = (scan + 1) % CN1_EVENT_RING;
            }
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    return 0;
}

/* Last resort before giving up an entry outright: drop the oldest *termination*.
 *
 * When the queue cannot grow, the question is only which loss costs least, and the
 * order is droppable input, then a state a later event already supersedes, then a
 * termination, then a lifecycle event. A lost release latches one component; a lost
 * close or hide loses a whole window -- the close operation never runs, or the
 * framework goes on painting a window that is not on screen. So a queued close or
 * visibility transition outranks any number of releases behind it. */
static int cn1LinuxEvictOldestTerminationLocked(void) {
    int idx = cn1EventHead;
    while (idx != cn1EventTail) {
        int t = cn1EventRing[idx].type;
        if (t == CN1_EVENT_KEY_RELEASED || t == CN1_EVENT_POINTER_RELEASED
                || t == CN1_EVENT_WINDOW_FOCUS
                || (t == CN1_EVENT_POINTER_HOVER && cn1EventRing[idx].x == -1
                        && cn1EventRing[idx].y == -1)) {
            cn1LinuxRemoveAtLocked(idx);
            return 1;
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    return 0;
}

void cn1LinuxPushWindowEvent(int windowId, int type, int x, int y, int keyCode) {
    pthread_mutex_lock(&cn1EventLock);
    int next = (cn1EventTail + 1) % CN1_EVENT_RING;
    if (next == cn1EventHead && cn1LinuxIsProtectedEvent(type, x, y)) {
        /* Full, and this one must not be the casualty. Supersede this window's own
         * queued transition if it has one, otherwise take the room from an input event,
         * and failing that from a transition that a later one already supersedes. Never
         * from a transition that is still some window's last word. */
        if (cn1LinuxCoalesceLifecycleLocked(windowId, type, x, y, keyCode)) {
            pthread_mutex_unlock(&cn1EventLock);
            return;
        }
        if (cn1LinuxEvictInputLocked() || cn1LinuxEvictSupersededVisibilityLocked()
                || cn1LinuxEvictOldestTerminationLocked()) {
            next = (cn1EventTail + 1) % CN1_EVENT_RING;
        } else {
            /* Nothing left but lifecycle events -- closes and visibility transitions
             * for more distinct windows than the ring can hold, which needs more windows
             * open than any application has. Giving up the oldest is all that remains,
             * and the newer event at least describes the more recent state. */
            cn1EventHead = (cn1EventHead + 1) % CN1_EVENT_RING;
            next = (cn1EventTail + 1) % CN1_EVENT_RING;
        }
    }
    if (next != cn1EventHead) {
        cn1EventRing[cn1EventTail].type = type;
        cn1EventRing[cn1EventTail].x = x;
        cn1EventRing[cn1EventTail].y = y;
        cn1EventRing[cn1EventTail].key = keyCode;
        cn1EventRing[cn1EventTail].windowId = windowId;
        cn1EventTail = next;
    }
    pthread_mutex_unlock(&cn1EventLock);
}

int cn1LinuxPopEvent(int* out) {
    int has = 0;
    pthread_mutex_lock(&cn1EventLock);
    if (cn1EventHead != cn1EventTail) {
        out[0] = cn1EventRing[cn1EventHead].type;
        out[1] = cn1EventRing[cn1EventHead].x;
        out[2] = cn1EventRing[cn1EventHead].y;
        out[3] = cn1EventRing[cn1EventHead].key;
        out[4] = cn1EventRing[cn1EventHead].windowId;
        cn1EventHead = (cn1EventHead + 1) % CN1_EVENT_RING;
        has = 1;
    }
    pthread_mutex_unlock(&cn1EventLock);
    return has;
}

/* ------------------------------------------------------------- globals */

/* The modifier mask from the most recent key event: 1 shift, 2 control, 4 alt. Same bit
 * values as the macOS and Windows ports, so the Java side of all three reads one encoding.
 *
 * Latched from the key event rather than queried, which is what this use needs: Shift-Tab
 * asks whether Shift is down while handling the Tab, and the Tab event's own state field
 * carries it. (A modifier pressed alone produces no key event, so this does not track one
 * held in isolation -- nothing here asks.)
 *
 * Declared BELOW the globals marker on purpose. scripts/test_native_hover_queue.py compiles
 * the event ring standalone by slicing this file from CN1_EVENT_RING to that marker, with
 * -Wall -Wextra -Werror; a static declared inside the slice and used only further down is an
 * unused variable there and fails the build. */
static volatile int cn1CurrentModifiers = 0;

static GtkWidget* cn1Window = 0;
static GtkWidget* cn1DrawingArea = 0;
static GtkWidget* cn1Overlay = 0;       /* GtkOverlay: drawing area + native widget layer */
static GtkWidget* cn1RootBox = 0;       /* GtkBox: optional menu bar above the overlay */
static GtkWidget* cn1MenuBar = 0;       /* the native menu bar, when commands published one */
/* Created ONCE and reused. A fresh group per rebuild would leak one per published form and
 * leave the window holding every group it had ever been given -- the menu items go away
 * with the bar, but the groups themselves do not. */
static GtkAccelGroup* cn1MenuAccels = 0;
static GtkWidget* cn1Fixed = 0;         /* GtkFixed overlay hosting positioned native peers */
static GtkWidget* cn1AccessibilityFixed = 0; /* transparent GTK/ATK semantic hierarchy */
static CN1Graphics cn1WindowG;          /* the on-screen / headless back buffer */
/* Back-buffer replacement is deferred to the drawing thread. GTK reports a resize
 * on its own thread, while the event dispatch thread paints through cn1WindowG.cr
 * for the whole frame -- destroying the context or surface underneath it is a use
 * after free, not merely a torn frame. cn1OnConfigure records the new size and
 * flushGraphics applies it between frames, the same shape the secondary desktop
 * windows and the Windows port use. */
static volatile int cn1PendingResize;
static int cn1PendingW;
static int cn1PendingH;
/* Held while GTK blits the surface and while the drawing thread swaps it: those
 * are the two places one thread can destroy what the other is reading. */
static pthread_mutex_t cn1BufferLock = PTHREAD_MUTEX_INITIALIZER;

/* Applies a resize recorded by cn1OnConfigure. Must run on the drawing thread,
 * between frames. */
static void cn1ApplyPendingResize(void);
static int cn1DisplayWidth = 800;
static int cn1DisplayHeight = 600;
static int cn1WindowOpen = 0;
static int cn1Headless = 0;
static char cn1HeadlessPath[4096];
static int cn1Initialized = 0;

CN1Graphics* cn1LinuxWindowGraphics(void) {
    return &cn1WindowG;
}

/* The GtkFixed a peer belongs in: a secondary desktop window's own overlay, or the
 * main window's. CN1_MAIN_WINDOW_SLOT means the application's main window.
 *
 * Peers used to go into cn1Fixed unconditionally, so a BrowserComponent or native
 * editor inside a Window appeared over the *main* window while the window it
 * belonged to stayed empty. */
static GtkWidget* cn1LinuxOverlayHost(int slot) {
    if (slot == CN1_MAIN_WINDOW_SLOT) {
        return cn1Fixed;
    }
    return cn1LinuxDesktopFixed(slot);
}

/* Native-peer overlay management (edit / browser / video / generic peers). All
 * must run on the GTK main thread (callers marshal via gdk_threads_add_idle). */
void cn1LinuxOverlayAdd(int slot, GtkWidget* w, int x, int y, int width, int height) {
    GtkWidget* host = cn1LinuxOverlayHost(slot);
    if (host == 0 || w == 0) {
        return;
    }
    gtk_widget_set_size_request(w, width, height);
    gtk_fixed_put(GTK_FIXED(host), w, x, y);
    gtk_widget_show_all(w);
}

void cn1LinuxOverlayMove(int slot, GtkWidget* w, int x, int y, int width, int height) {
    GtkWidget* host = cn1LinuxOverlayHost(slot);
    if (host == 0 || w == 0) {
        return;
    }
    gtk_widget_set_size_request(w, width, height);
    gtk_fixed_move(GTK_FIXED(host), w, x, y);
}

void cn1LinuxOverlayRemove(int slot, GtkWidget* w) {
    GtkWidget* host = cn1LinuxOverlayHost(slot);
    if (host != 0 && w != 0 && gtk_widget_get_parent(w) == host) {
        gtk_container_remove(GTK_CONTAINER(host), w);
    }
}

/* The top-level GtkWindow (NULL in headless mode); used as the transient parent
 * for modal dialogs (file chooser, print) and the WebKit/edit hosts. */
GtkWidget* cn1LinuxWindowWidget(void) {
    return cn1Window;
}

/* Posts fn(arg) onto the GTK main loop and blocks the calling (EDT) thread until
 * it has run. Shared by the services / edit / browser / media units for the GTK
 * calls that must happen on the main thread. In headless mode (no window, no
 * loop) it runs inline so callers never deadlock. */
typedef struct {
    void (*fn)(void*);
    void* arg;
    pthread_mutex_t m;
    pthread_cond_t c;
    int done;
} CN1MainCall;

static gboolean cn1MainCallTrampoline(gpointer p) {
    CN1MainCall* mc = (CN1MainCall*) p;
    mc->fn(mc->arg);
    pthread_mutex_lock(&mc->m);
    mc->done = 1;
    pthread_cond_signal(&mc->c);
    pthread_mutex_unlock(&mc->m);
    return FALSE;
}

void cn1LinuxRunOnMainAndWait(void (*fn)(void*), void* arg) {
    CN1MainCall mc;
    if (cn1Window == 0) {
        fn(arg);
        return;
    }
    mc.fn = fn;
    mc.arg = arg;
    mc.done = 0;
    pthread_mutex_init(&mc.m, 0);
    pthread_cond_init(&mc.c, 0);
    gdk_threads_add_idle(cn1MainCallTrampoline, &mc);
    pthread_mutex_lock(&mc.m);
    while (!mc.done) {
        pthread_cond_wait(&mc.c, &mc.m);
    }
    pthread_mutex_unlock(&mc.m);
    pthread_mutex_destroy(&mc.m);
    pthread_cond_destroy(&mc.c);
}

/* (Re)allocates the back-buffer surface to w x h, preserving nothing. */
static void cn1ResizeBackBuffer(int w, int h) {
    if (w <= 0) w = 1;
    if (h <= 0) h = 1;
    if (cn1WindowG.cr) {
        cairo_destroy(cn1WindowG.cr);
    }
    if (cn1WindowG.surface) {
        cairo_surface_destroy(cn1WindowG.surface);
    }
    cn1WindowG.surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    cn1WindowG.cr = cairo_create(cn1WindowG.surface);
    cn1WindowG.width = w;
    cn1WindowG.height = h;
    cn1WindowG.color = 0;
    cn1WindowG.alpha = 255;
    cn1WindowG.clipX = 0;
    cn1WindowG.clipY = 0;
    cn1WindowG.clipW = w;
    cn1WindowG.clipH = h;
    cn1WindowG.isWindowTarget = 1;
    cairo_matrix_init_identity(&cn1WindowG.transform);
}

static void cn1ApplyPendingResize(void) {
    if (!cn1PendingResize) {
        return;
    }
    pthread_mutex_lock(&cn1BufferLock);
    /* Re-checked under the lock: GTK can record another resize between the test
     * above and here. */
    if (cn1PendingResize) {
        cn1ResizeBackBuffer(cn1PendingW, cn1PendingH);
        cn1PendingResize = 0;
    }
    pthread_mutex_unlock(&cn1BufferLock);
}

/* ------------------------------------------------------ GTK callbacks */

static gboolean cn1OnDraw(GtkWidget* widget, cairo_t* cr, gpointer data) {
    (void) widget;
    (void) data;
    /* Locked so the drawing thread cannot swap the surface out from under this
     * blit. */
    pthread_mutex_lock(&cn1BufferLock);
    if (cn1WindowG.surface) {
        cairo_set_source_surface(cr, cn1WindowG.surface, 0, 0);
        cairo_paint(cr);
    }
    pthread_mutex_unlock(&cn1BufferLock);
    return FALSE;
}

static gboolean cn1OnConfigure(GtkWidget* widget, GdkEventConfigure* e, gpointer data) {
    (void) widget;
    (void) data;
    if (e->width != cn1DisplayWidth || e->height != cn1DisplayHeight) {
        cn1DisplayWidth = e->width;
        cn1DisplayHeight = e->height;
        /* Recorded, not applied: this is the GTK thread and the event dispatch
         * thread may be part way through a frame on the current buffer. */
        pthread_mutex_lock(&cn1BufferLock);
        cn1PendingW = cn1DisplayWidth;
        cn1PendingH = cn1DisplayHeight;
        cn1PendingResize = 1;
        pthread_mutex_unlock(&cn1BufferLock);
        cn1LinuxPushEvent(CN1_EVENT_SIZE_CHANGED, cn1DisplayWidth, cn1DisplayHeight, 0);
    }
    return FALSE;
}

/* Maps a GdkEventButton.button (1=left, 2=middle, 3=right, 8=back, 9=forward)
 * to a CN1_PE_MASK_* bit. */
static int cn1LinuxButtonMask(guint button) {
    switch (button) {
        case 1:  return CN1_PE_MASK_PRIMARY;
        case 2:  return CN1_PE_MASK_MIDDLE;
        case 3:  return CN1_PE_MASK_SECONDARY;
        case 8:  return CN1_PE_MASK_BACK;
        case 9:  return CN1_PE_MASK_FORWARD;
        default: return CN1_PE_MASK_PRIMARY;
    }
}

/* Buttons held down according to a GdkEvent state mask, used to label a drag. */
static int cn1LinuxStateMask(guint state) {
    int mask = 0;
    if (state & GDK_BUTTON1_MASK) mask |= CN1_PE_MASK_PRIMARY;
    if (state & GDK_BUTTON2_MASK) mask |= CN1_PE_MASK_MIDDLE;
    if (state & GDK_BUTTON3_MASK) mask |= CN1_PE_MASK_SECONDARY;
    if (state & GDK_BUTTON4_MASK) mask |= CN1_PE_MASK_BACK;
    if (state & GDK_BUTTON5_MASK) mask |= CN1_PE_MASK_FORWARD;
    return mask;
}

/* Hover and contact must retain the same physical source. GDK_SOURCE_CURSOR
 * is a tablet puck, not a pen; pen and eraser retain their distinct pointer types. */
int cn1LinuxPointerSourceFlag(GdkEvent* event) {
    GdkDevice* device = gdk_event_get_source_device(event);
    if (device == NULL) {
        return 0;
    }
    GdkInputSource source = gdk_device_get_source(device);
    if (source == GDK_SOURCE_TOUCHSCREEN) {
        return CN1_PE_TOUCH_FLAG;
    }
    if (source == GDK_SOURCE_ERASER) {
        return CN1_PE_ERASER_FLAG;
    }
    if (source == GDK_SOURCE_PEN) {
        return CN1_PE_PEN_FLAG;
    }
    return 0;
}

/* True when an event originated from a touchscreen. GTK also synthesizes button
 * / motion events from touch for widgets that ignore touch, so we drop those
 * here and let cn1OnTouch drive the pointer instead (avoids double dispatch). */
static int cn1LinuxIsTouchSource(GdkEvent* e) {
    return cn1LinuxPointerSourceFlag(e) == CN1_PE_TOUCH_FLAG;
}

static gboolean cn1OnButton(GtkWidget* widget, GdkEventButton* e, gpointer data) {
    (void) widget;
    (void) data;
    if (cn1LinuxIsTouchSource((GdkEvent*) e)) {
        return TRUE;
    }
    cn1LinuxPushEvent(e->type == GDK_BUTTON_PRESS ? CN1_EVENT_POINTER_PRESSED : CN1_EVENT_POINTER_RELEASED,
            (int) e->x, (int) e->y, cn1LinuxButtonMask(e->button) | cn1LinuxPointerSourceFlag((GdkEvent*) e));
    return TRUE;
}

static gboolean cn1OnMotion(GtkWidget* widget, GdkEventMotion* e, gpointer data) {
    (void) widget;
    (void) data;
    if (cn1LinuxIsTouchSource((GdkEvent*) e)) {
        return TRUE;
    }
    int mask = cn1LinuxStateMask(e->state);
    if (mask != 0) {
        cn1LinuxPushEvent(CN1_EVENT_POINTER_DRAGGED, (int) e->x, (int) e->y,
                mask | cn1LinuxPointerSourceFlag((GdkEvent*) e));
    } else {
        /* No button held: this is hover, and it used to be dropped here.
         * Component's hover style is driven by Form.pointerHover, which has
         * nothing else to fire it, so every hover rule in a desktop theme was
         * inert. Droppable rather than protected: a lost hover costs nothing
         * because hover is idempotent and the next motion re-establishes it. */
        cn1LinuxPushEvent(CN1_EVENT_POINTER_HOVER, (int) e->x, (int) e->y,
                cn1LinuxPointerSourceFlag((GdkEvent*) e));
    }
    return TRUE;
}

/* The primary touch sequence currently driving the pointer (single-touch
 * model). Additional concurrent fingers are ignored until it ends. */
static GdkEventSequence* cn1TouchSeq = NULL;

/* The pointer left the drawing area: clear hover.
 *
 * Without this the cursor can move straight off the window and the last hovered control
 * stays lit -- motion simply stops, and Form only clears its tracked hover when a
 * DIFFERENT component is reported. -1,-1 is the agreed "nothing is under the pointer"
 * coordinate, the same one the Windows port sends from WM_MOUSELEAVE; a real coordinate
 * is never negative, so the two cannot be confused.
 *
 * GDK_NOTIFY_INFERIOR is ignored: that is the pointer moving onto a CHILD of the drawing
 * area, which has not left the window at all, and treating it as a leave would blink the
 * hover off and on again. */
static gboolean cn1OnLeave(GtkWidget* widget, GdkEventCrossing* e, gpointer data) {
    (void) widget;
    (void) data;
    if (e->detail != GDK_NOTIFY_INFERIOR) {
        cn1LinuxPushEvent(CN1_EVENT_POINTER_HOVER, -1, -1, cn1LinuxPointerSourceFlag((GdkEvent*) e));
    }
    return FALSE;
}

static gboolean cn1OnTouch(GtkWidget* widget, GdkEventTouch* e, gpointer data) {
    (void) widget;
    (void) data;
    switch (e->type) {
        case GDK_TOUCH_BEGIN:
            if (cn1TouchSeq == NULL) {
                cn1TouchSeq = e->sequence;
                cn1LinuxPushEvent(CN1_EVENT_POINTER_PRESSED, (int) e->x, (int) e->y,
                        CN1_PE_MASK_PRIMARY | CN1_PE_TOUCH_FLAG);
            }
            return TRUE;
        case GDK_TOUCH_UPDATE:
            if (e->sequence == cn1TouchSeq) {
                cn1LinuxPushEvent(CN1_EVENT_POINTER_DRAGGED, (int) e->x, (int) e->y,
                        CN1_PE_MASK_PRIMARY | CN1_PE_TOUCH_FLAG);
            }
            return TRUE;
        case GDK_TOUCH_END:
        case GDK_TOUCH_CANCEL:
            if (e->sequence == cn1TouchSeq) {
                cn1TouchSeq = NULL;
                cn1LinuxPushEvent(CN1_EVENT_POINTER_RELEASED, (int) e->x, (int) e->y,
                        CN1_PE_MASK_PRIMARY | CN1_PE_TOUCH_FLAG);
            }
            return TRUE;
        default:
            return FALSE;
    }
}

static gboolean cn1OnKey(GtkWidget* widget, GdkEventKey* e, gpointer data) {
    (void) widget;
    (void) data;
    /* Recorded before the peer-focus check below returns: the modifiers are true for this
     * keystroke whether or not Codename One goes on to handle it. */
    if (e != 0) {
        int mods = 0;
        if (e->state & GDK_SHIFT_MASK) {
            mods |= 1;
        }
        if (e->state & GDK_CONTROL_MASK) {
            mods |= 2;
        }
        if (e->state & GDK_MOD1_MASK) {
            mods |= 4;
        }
        cn1CurrentModifiers = mods;
    }
    /* The key handler is on the toplevel window so it sees keystrokes regardless
     * of which child has focus. But when a native peer widget (the text-edit
     * GtkEntry/GtkTextView, a WebKit view, an app @NativeInterface widget) holds
     * the focus, the keystroke belongs to IT, not the CN1 EDT: return FALSE so
     * GtkWindow's default handler forwards the event to the focused widget.
     * Returning TRUE here unconditionally is what made typing into the native
     * editor show nothing -- it suppressed that default forwarding. Once the peer
     * is torn down GTK clears the toplevel focus to NULL, so CN1 keys resume. */
    if (cn1Window != 0) {
        GtkWidget* focus = gtk_window_get_focus(GTK_WINDOW(cn1Window));
        if (focus != 0 && focus != cn1DrawingArea) {
            return FALSE;
        }
    }
    /* Map the GDK keyval to a Codename One key code: printable Unicode passes
     * through gdk_keyval_to_unicode; the navigation keys map to the CN1 game-key
     * codes the EDT recognises. */
    int code = (int) gdk_keyval_to_unicode(e->keyval);
    if (code == 0) {
        code = (int) e->keyval;
    }
    cn1LinuxPushEvent(e->type == GDK_KEY_PRESS ? CN1_EVENT_KEY_PRESSED : CN1_EVENT_KEY_RELEASED, 0, 0, code);
    return TRUE;
}

/* Touchpad pinch / rotate (GDK_TOUCHPAD_PINCH, libinput). scale is cumulative
 * relative to the gesture's BEGIN, so we forward the incremental multiplier;
 * angle_delta is a per-event delta already in radians, which is what the Java side
 * reads it as. These map to Display.fireMagnifyGesture / fireRotationGesture, the
 * same hooks the macOS trackpad drives. Delivered through the generic "event"
 * signal, so we return FALSE for anything else to leave other handlers intact. */
static double cn1PinchLastScale = 1.0;

static gboolean cn1OnGenericEvent(GtkWidget* widget, GdkEvent* e, gpointer data) {
    (void) widget;
    (void) data;
    if (e->type != GDK_TOUCHPAD_PINCH) {
        return FALSE;
    }
    GdkEventTouchpadPinch* pe = (GdkEventTouchpadPinch*) e;
    if (pe->phase == GDK_TOUCHPAD_GESTURE_PHASE_BEGIN) {
        cn1PinchLastScale = pe->scale > 0 ? pe->scale : 1.0;
        /* Forwarded, not just consumed. The phase is what lets the Java side
         * deliver the whole gesture to one component and, more importantly,
         * end it: a touchpad emits no pointer events, so the two-pointer path
         * that normally calls pinchReleased() never runs here. */
        cn1LinuxPushEvent(CN1_EVENT_PINCH_BEGIN, (int) pe->x, (int) pe->y, 0);
    } else if (pe->phase == GDK_TOUCHPAD_GESTURE_PHASE_END
            || pe->phase == GDK_TOUCHPAD_GESTURE_PHASE_CANCEL) {
        /* CANCEL as well as END: a cancelled gesture leaves the component
         * mid-pinch exactly as a dropped END would, and the Java side treats
         * both as "the fingers left the touchpad". */
        cn1PinchLastScale = 1.0;
        cn1LinuxPushEvent(CN1_EVENT_PINCH_END, (int) pe->x, (int) pe->y, 0);
    } else if (pe->phase == GDK_TOUCHPAD_GESTURE_PHASE_UPDATE) {
        int x = (int) pe->x;
        int y = (int) pe->y;
        if (pe->scale > 0 && cn1PinchLastScale > 0) {
            double inc = pe->scale / cn1PinchLastScale;
            cn1PinchLastScale = pe->scale;
            if (inc != 1.0) {
                cn1LinuxPushEvent(CN1_EVENT_PINCH, x, y, (int) (inc * CN1_GESTURE_FIXED + 0.5));
            }
        }
        if (pe->angle_delta != 0.0) {
            /* Radians already. GdkEventTouchpadPinch.angle_delta is documented as
             * "the angle change in radians", and the Java side reads the packed value
             * as radians too -- converting it as though it were degrees divided every
             * rotation by 57.3, so a gesture the user could plainly feel barely moved
             * anything on screen. */
            double rad = pe->angle_delta;
            cn1LinuxPushEvent(CN1_EVENT_ROTATE, x, y,
                    (int) (rad * CN1_GESTURE_FIXED + (rad >= 0 ? 0.5 : -0.5)));
        }
    }
    return TRUE;
}

/* Converts a stream of fractional scroll notches into whole ones.
 *
 * Smooth scroll deltas are fractions of a notch, so they cannot be forwarded one
 * for one: wheelUnits() on the Java side floors any sub-notch delta to a whole
 * notch, and a touchpad emits deltas continuously. Whole notches are returned and
 * the remainder is carried in *residue until it adds up. Truncation is toward
 * zero, so the residue always keeps the sign of the travel. */
int cn1LinuxTakeWholeNotches(double delta, double* residue) {
    double total = *residue + delta;
    int notches = (int) total;
    *residue = total - notches;
    return notches;
}

/* The main window's smooth-scroll residue. Secondary windows keep their own in
 * CN1LinuxWindow, so two windows cannot consume each other's partial notches. */
static double cn1ScrollResidueX = 0;
static double cn1ScrollResidueY = 0;

static gboolean cn1OnScroll(GtkWidget* widget, GdkEventScroll* e, gpointer data) {
    (void) widget;
    (void) data;
    /* One notch == 120 units (the WHEEL_DELTA the impl converts to pixels). */
    if (e->direction == GDK_SCROLL_UP) {
        cn1LinuxPushEvent(CN1_EVENT_MOUSE_WHEEL, (int) e->x, (int) e->y, 120);
    } else if (e->direction == GDK_SCROLL_DOWN) {
        cn1LinuxPushEvent(CN1_EVENT_MOUSE_WHEEL, (int) e->x, (int) e->y, -120);
    } else if (e->direction == GDK_SCROLL_LEFT) {
        cn1LinuxPushEvent(CN1_EVENT_MOUSE_HWHEEL, (int) e->x, (int) e->y, -120);
    } else if (e->direction == GDK_SCROLL_RIGHT) {
        cn1LinuxPushEvent(CN1_EVENT_MOUSE_HWHEEL, (int) e->x, (int) e->y, 120);
    } else if (e->direction == GDK_SCROLL_SMOOTH) {
        /* Two-finger touchpad scrolling arrives here and nowhere else: a touchpad
         * reports no discrete steps, so GDK emits only a smooth event for it, and
         * drops that event before delivery unless the widget selected
         * GDK_SMOOTH_SCROLL_MASK. Without the mask and this branch the main window
         * ignored touchpad scrolling entirely. Selecting the mask also makes GDK
         * drop the pointer-emulated discrete events a real wheel produces, so the
         * branches above and this one cannot both fire for one movement. */
        int vertical;
        int horizontal;
        if (e->is_stop) {
            cn1ScrollResidueX = 0;
            cn1ScrollResidueY = 0;
            return TRUE;
        }
        vertical = cn1LinuxTakeWholeNotches(e->delta_y, &cn1ScrollResidueY);
        horizontal = cn1LinuxTakeWholeNotches(e->delta_x, &cn1ScrollResidueX);
        if (vertical != 0) {
            /* delta_y grows downwards, which GDK_SCROLL_DOWN reports as negative
             * units above. */
            cn1LinuxPushEvent(CN1_EVENT_MOUSE_WHEEL, (int) e->x, (int) e->y, -vertical * 120);
        }
        if (horizontal != 0) {
            cn1LinuxPushEvent(CN1_EVENT_MOUSE_HWHEEL, (int) e->x, (int) e->y, horizontal * 120);
        }
    }
    return TRUE;
}

static gboolean cn1OnDelete(GtkWidget* widget, GdkEvent* e, gpointer data) {
    (void) widget;
    (void) e;
    (void) data;
    cn1LinuxPushEvent(CN1_EVENT_CLOSE, 0, 0, 0);
    cn1WindowOpen = 0;
    return TRUE; /* don't auto-destroy; the app exits via the CLOSE event */
}

/* --------------------------------------------------------- lifecycle */

/* ---- hardware fault -> Java exception (POSIX analog of the iOS SignalHandler
 * and the Win32 cn1WinFaultToException) -------------------------------------
 *
 * ParparVM's clean C target only NULL-checks a method's `this` at entry. A null
 * *argument* deref, a virtual call on a null receiver, or an operand-stack slot
 * that legitimately reads back null all fault as a raw SIGSEGV in generated C
 * rather than a catchable NullPointerException -- which hard-kills the process
 * (and, on CI, the whole screenshot suite) with no Java stack trace. The iOS port
 * converts the identical fault into an NPE via a SIGSEGV handler that calls
 * throwException(); this is the Linux analog, hardened with the Win32 port's
 * "wild faulting address == real corruption, leave it diagnosable" guard.
 *
 * The handler runs on the faulting thread's own stack for a synchronous signal,
 * so longjmp-ing out of it via throwException() to the nearest CN1 try/catch
 * behaves exactly as a normally thrown Java exception (this is the documented
 * iOS technique). SA_NODEFER keeps the signal unblocked so faults remain
 * catchable on subsequent tests after we leave the handler.
 *
 * NOTE: like every such handler this interferes with a native debugger (gdb sees
 * the fault first-chance). It is a release/CI resilience mechanism, not a debug
 * aid -- set CN1_LINUX_NO_FAULT_HANDLER=1 to disable it when debugging under gdb.
 */
/* Dump the faulting thread's native backtrace to stderr (the app-output tee). Async-
 * signal-safe (write + backtrace). With the per-thread alternate signal stack this runs
 * even on a STACK OVERFLOW; the repeating frames are the recursion. Called from both the
 * SIGABRT path and the wild-address SIGSEGV path (stack-overflow guard-page faults land
 * there), so an overflow is finally legible instead of a bare, corrupt-unwind core. */
/* async-signal-safe "KEY=0xHEX\n" */
static void cn1LinuxWriteHexKV(const char* key, unsigned long v) {
    char buf[64]; int i = 0;
    while (key[i]) { buf[i] = key[i]; i++; }
    buf[i++] = '='; buf[i++] = '0'; buf[i++] = 'x';
    char hx[16]; int h = 0;
    if (v == 0) hx[h++] = '0';
    while (v) { int d = v & 0xf; hx[h++] = (char)(d < 10 ? '0' + d : 'a' + d - 10); v >>= 4; }
    while (h > 0) buf[i++] = hx[--h];
    buf[i++] = '\n';
    write(2, buf, i);
}

static void cn1LinuxDumpNativeBacktrace(const char* label, siginfo_t* si, void* ucv) {
    signal(SIGSEGV, SIG_DFL);
    signal(SIGBUS, SIG_DFL);
    write(2, "\n=====CN1 ", 10);
    write(2, label, strlen(label));
    write(2, " BACKTRACE=====\n", 16);
    if (si != NULL) cn1LinuxWriteHexKV("faultAddr", (unsigned long)(uintptr_t)si->si_addr);
    /* faulting PC + SP from the ucontext, and this thread's real stack bounds -- if the
     * used span (top - sp) is a fraction of a MB, this is a SMALL-STACK thread overflow
     * (e.g. CN1 rendering on a WebKit/Gallium/GLib worker thread), not deep recursion. */
    if (ucv != NULL) {
        ucontext_t* uc = (ucontext_t*)ucv;
        unsigned long sp = 0, pc = 0;
#if defined(__aarch64__)
        sp = (unsigned long)uc->uc_mcontext.sp;
        pc = (unsigned long)uc->uc_mcontext.pc;
#elif defined(__x86_64__)
        sp = (unsigned long)uc->uc_mcontext.gregs[REG_RSP];
        pc = (unsigned long)uc->uc_mcontext.gregs[REG_RIP];
#endif
        if (pc) cn1LinuxWriteHexKV("faultPC", pc);
        if (sp) cn1LinuxWriteHexKV("faultSP", sp);
        /* The integer registers. Reading them costs nothing and touches no memory, and
         * they are the only place the faulting POINTER survives: a wild address tells you
         * a read went wrong, the register file tells you what the value WAS and which
         * other register held the object it should have come from. gdb cannot supply this
         * from the core -- the suite is built -O3 with LTO, so `info args` and
         * `info locals` both answer "No locals" and every global reports "unknown type".
         * The one 0x100000028 fault that reached a core was diagnosed this far only
         * because faultAddr was printed here rather than recovered afterwards. */
#if defined(__x86_64__)
        {
            static const char* const rn[16] = {
                "rax","rbx","rcx","rdx","rsi","rdi","rbp","rsp",
                "r8","r9","r10","r11","r12","r13","r14","r15" };
            static const int ri[16] = {
                REG_RAX,REG_RBX,REG_RCX,REG_RDX,REG_RSI,REG_RDI,REG_RBP,REG_RSP,
                REG_R8,REG_R9,REG_R10,REG_R11,REG_R12,REG_R13,REG_R14,REG_R15 };
            int r;
            for (r = 0; r < 16; r++) {
                cn1LinuxWriteHexKV(rn[r], (unsigned long)uc->uc_mcontext.gregs[ri[r]]);
            }
        }
#elif defined(__aarch64__)
        {
            char nm[8]; int r;
            for (r = 0; r <= 30; r++) {
                nm[0] = 'x';
                if (r < 10) { nm[1] = (char)('0' + r); nm[2] = 0; }
                else { nm[1] = (char)('0' + r / 10); nm[2] = (char)('0' + r % 10); nm[3] = 0; }
                cn1LinuxWriteHexKV(nm, (unsigned long)uc->uc_mcontext.regs[r]);
            }
        }
#endif
        pthread_attr_t at;
        if (pthread_getattr_np(pthread_self(), &at) == 0) {
            void* base = 0; size_t sz = 0;
            if (pthread_attr_getstack(&at, &base, &sz) == 0) {
                cn1LinuxWriteHexKV("stackLo", (unsigned long)(uintptr_t)base);
                cn1LinuxWriteHexKV("stackSz", (unsigned long)sz);
                if (sp) cn1LinuxWriteHexKV("stackUsed", (unsigned long)(((uintptr_t)base + sz) - sp));
                /* The top of the faulting frame. Bounded by the real stack top, so this
                 * reads only mapped memory and cannot fault a second time -- the faulting
                 * ADDRESS is unmapped by definition and is deliberately not read.
                 *
                 * What it is for: a pointer-shaped fault whose low 32 bits are zero (the
                 * 0x100000000 case) is either a 64-bit value read as an object or a 32-bit
                 * write into a 64-bit slot, and the two look identical from the address
                 * alone. The neighbouring words separate them -- an adjacent half of the
                 * same 64-bit value is the first, an intact pointer beside a clobbered one
                 * is the second. */
                unsigned long hi = (unsigned long)(uintptr_t)base + (unsigned long)sz;
                if (sp && sp >= (unsigned long)(uintptr_t)base && sp < hi) {
                    unsigned long lim = sp + 32 * sizeof(unsigned long);
                    if (lim > hi) { lim = hi; }
                    unsigned long a;
                    int slot = 0;
                    char nm[16];
                    for (a = sp; a + sizeof(unsigned long) <= lim; a += sizeof(unsigned long)) {
                        nm[0]='s'; nm[1]='p'; nm[2]='[';
                        nm[3]=(char)('0' + slot / 10); nm[4]=(char)('0' + slot % 10);
                        nm[5]=']'; nm[6]=0;
                        cn1LinuxWriteHexKV(nm, *(unsigned long*)(uintptr_t)a);
                        slot++;
                    }
                }
            }
            pthread_attr_destroy(&at);
        }
    }
#ifdef __GLIBC__
    void* bt[96];
    int n = backtrace(bt, 96);
    backtrace_symbols_fd(bt, n, 2);
#else
    const char* na = "(native backtrace unavailable on this libc)\n";
    write(2, na, strlen(na));
#endif
    const char* ftr = "=====END CN1 BACKTRACE=====\n";
    write(2, ftr, strlen(ftr));
}

static void cn1LinuxFaultToException(int sig, siginfo_t* si, void* ucv) {
    (void) ucv;
    /* Only a genuine null-ish deref (null + a small field/vtable/array offset)
     * is converted to an NPE. A wild faulting address is real memory corruption OR a
     * stack overflow (the guard-page address is high): dump a backtrace so the failing
     * frame / recursion is identifiable from the CI log, then restore the default
     * disposition and return so the re-executed instruction faults again into a core
     * dump, instead of being masked as a recoverable NPE that silently corrupts state. */
    if ((sig == SIGSEGV || sig == SIGBUS) && si != NULL &&
            (uintptr_t) si->si_addr >= 0x10000) {
        cn1LinuxDumpNativeBacktrace("FAULT", si, ucv);
        signal(sig, SIG_DFL);
        return;
    }
    struct ThreadLocalData* t = getThreadLocalData();
    if (t == NULL || t->tryBlockOffset <= 0) {
        signal(sig, SIG_DFL);
        return;
    }
    throwException(t, __NEW_INSTANCE_java_lang_NullPointerException(t));
}

/* Diagnostic: when the process aborts (e.g. __stack_chk_fail / a glib g_error),
 * dump the native call stack so the failing frame is identifiable from the log
 * even where an interactive debugger is unavailable (CI containers). Enabled only
 * when CN1_LINUX_ABORT_BACKTRACE is set so it never interferes with normal runs. */
static void cn1LinuxAbortBacktrace(int sig) {
    cn1LinuxDumpNativeBacktrace("ABORT", NULL, NULL);
    signal(sig, SIG_DFL);
    raise(sig);
}

static void cn1LinuxInstallFaultHandlers() {
    static int installed = 0;
    if (installed || getenv("CN1_LINUX_NO_FAULT_HANDLER") != NULL) {
        return;
    }
    installed = 1;
    /* Alternate signal stack for THIS (main) thread so SA_ONSTACK works here too. The CN1
     * worker/EDT/GC threads register their own in threadRunner/gcMarkWorkerMain. */
    {
        stack_t ss;
        ss.ss_sp = malloc(512 * 1024);
        if (ss.ss_sp != NULL) { ss.ss_size = 512 * 1024; ss.ss_flags = 0; sigaltstack(&ss, NULL); }
    }
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1LinuxFaultToException;
    /* SA_ONSTACK: run the handler on the per-thread alternate signal stack (registered in
     * threadRunner / gcMarkWorkerMain / crash-protection install) so it can execute even
     * on a STACK OVERFLOW, when the faulting thread's own stack is exhausted -- otherwise
     * the handler re-faults and the process dies silently with no backtrace. */
    sa.sa_flags = SA_SIGINFO | SA_NODEFER | SA_ONSTACK;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);
    /* Dump a native backtrace if the process aborts (e.g. __stack_chk_fail or a
     * glib g_error) so the failing frame is identifiable from the CI log without a
     * debugger. Silent unless an abort actually fires. */
    signal(SIGABRT, cn1LinuxAbortBacktrace);
}

/* The main window's title, after initDisplay has already set it once.
 *
 * Needed because desktop "native" title-bar mode moves the form title OUT of the CN1 title
 * area and into the OS window's, and until now this port had nowhere to put it: the title was
 * a CreateWindow-time argument and LinuxNative.desktopWindowSetTitle addresses the SECONDARY
 * Window peers by slot, never the main one. Without this, suppressing the CN1 title area would
 * simply lose the title.
 *
 * Marshalled onto the GTK main loop like every other widget call here.
 * cn1LinuxRunOnMainAndWait runs the callback inline when there is no window, which is what the
 * headless screenshot mode wants -- the setter is then a no-op on a window that does not exist.
 */
static void cn1MainTitleOnMain(void* arg) {
    const char* t = (const char*) arg;
    if (cn1Window != 0) {
        gtk_window_set_title(GTK_WINDOW(cn1Window), t != 0 ? t : "");
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mainWindowSetTitle___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT title) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    const char* t = title == JAVA_NULL ? "" : stringToUTF8(threadStateData, title);
    cn1LinuxRunOnMainAndWait(cn1MainTitleOnMain, (void*) t);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_initDisplay___java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT title, JAVA_INT width, JAVA_INT height) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    const char* t = title == JAVA_NULL ? "Codename One" : stringToUTF8(threadStateData, title);
    if (cn1Initialized) {
        return;
    }
    cn1Initialized = 1;
    /* Install the fault->exception handler before anything else (and before the
     * headless early-return below) so it is active for the CI screenshot run. */
    cn1LinuxInstallFaultHandlers();
    /* In headless screenshot mode the size set by enableHeadlessScreenshot is
     * authoritative (CI fixes the screenshot dimensions); otherwise take the
     * window size the impl requests. */
    if (!cn1Headless) {
        if (width > 0) cn1DisplayWidth = width;
        if (height > 0) cn1DisplayHeight = height;
    }

    if (cn1Headless) {
        /* No window: render into an offscreen back buffer of the requested size. */
        cn1ResizeBackBuffer(cn1DisplayWidth, cn1DisplayHeight);
        return;
    }

    gtk_init(0, 0);
    cn1Window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(cn1Window), t);
    gtk_window_set_default_size(GTK_WINDOW(cn1Window), cn1DisplayWidth, cn1DisplayHeight);

    cn1DrawingArea = gtk_drawing_area_new();
    gtk_widget_set_events(cn1DrawingArea,
            GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK | GDK_POINTER_MOTION_MASK |
            /* LEAVE_NOTIFY drives the hover clear (cn1OnLeave). A g_signal_connect for
             * an event the mask does not select is never called, so the handler would
             * have been dead code without this bit. */
            GDK_LEAVE_NOTIFY_MASK |
            GDK_KEY_PRESS_MASK | GDK_KEY_RELEASE_MASK | GDK_SCROLL_MASK |
            GDK_SMOOTH_SCROLL_MASK | GDK_TOUCH_MASK |
            GDK_TOUCHPAD_GESTURE_MASK | GDK_STRUCTURE_MASK);
    gtk_widget_set_can_focus(cn1DrawingArea, TRUE);

    /* A GtkOverlay layers a transparent, pass-through GtkFixed over the drawing
     * area so native peers (text edit, browser, video sink, app @NativeInterface
     * widgets) can be positioned over the Cairo-rendered UI without intercepting
     * input where there is no child. */
    cn1Overlay = gtk_overlay_new();
    gtk_container_add(GTK_CONTAINER(cn1Overlay), cn1DrawingArea);
    cn1Fixed = gtk_fixed_new();
    gtk_overlay_add_overlay(GTK_OVERLAY(cn1Overlay), cn1Fixed);
    gtk_overlay_set_overlay_pass_through(GTK_OVERLAY(cn1Overlay), cn1Fixed, TRUE);
    cn1AccessibilityFixed = gtk_fixed_new();
    gtk_widget_set_opacity(cn1AccessibilityFixed, 0.01);
    gtk_overlay_add_overlay(GTK_OVERLAY(cn1Overlay), cn1AccessibilityFixed);
    gtk_overlay_set_overlay_pass_through(GTK_OVERLAY(cn1Overlay), cn1AccessibilityFixed, TRUE);
    /* A vertical box between the window and the overlay, so a menu bar has somewhere to go.
     * It is created unconditionally and stays EMPTY until commands arrive: an application
     * that publishes none packs nothing above the overlay, and a GtkBox with one child
     * that expands is laid out exactly as the overlay was when it was the window's direct
     * child. That is what keeps every existing screenshot byte-identical. */
    cn1RootBox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    gtk_box_pack_start(GTK_BOX(cn1RootBox), cn1Overlay, TRUE, TRUE, 0);
    gtk_container_add(GTK_CONTAINER(cn1Window), cn1RootBox);

    g_signal_connect(cn1DrawingArea, "draw", G_CALLBACK(cn1OnDraw), 0);
    g_signal_connect(cn1DrawingArea, "configure-event", G_CALLBACK(cn1OnConfigure), 0);
    g_signal_connect(cn1DrawingArea, "button-press-event", G_CALLBACK(cn1OnButton), 0);
    g_signal_connect(cn1DrawingArea, "button-release-event", G_CALLBACK(cn1OnButton), 0);
    g_signal_connect(cn1DrawingArea, "motion-notify-event", G_CALLBACK(cn1OnMotion), 0);
    g_signal_connect(cn1DrawingArea, "leave-notify-event", G_CALLBACK(cn1OnLeave), 0);
    g_signal_connect(cn1DrawingArea, "touch-event", G_CALLBACK(cn1OnTouch), 0);
    g_signal_connect(cn1DrawingArea, "event", G_CALLBACK(cn1OnGenericEvent), 0);
    g_signal_connect(cn1Window, "key-press-event", G_CALLBACK(cn1OnKey), 0);
    g_signal_connect(cn1Window, "key-release-event", G_CALLBACK(cn1OnKey), 0);
    g_signal_connect(cn1DrawingArea, "scroll-event", G_CALLBACK(cn1OnScroll), 0);
    g_signal_connect(cn1Window, "delete-event", G_CALLBACK(cn1OnDelete), 0);

    cn1ResizeBackBuffer(cn1DisplayWidth, cn1DisplayHeight);
    gtk_widget_show_all(cn1Window);
    gtk_widget_grab_focus(cn1DrawingArea);
    cn1WindowOpen = 1;
}

/* ------------------------------------------------------------- menu bar */

/*
 * The native menu bar.
 *
 * Commands arrive as one encoded row each, the format
 * IOSImplementation.setNativeCommands writes for the macOS menu and
 * WindowsImplementation writes for the Win32 one:
 *
 *   "<menuHint>\t<label>\t<shortcutKeyChar>\t<shortcutModifiers>\t<commandId>"
 *
 * rows separated by '\n'. The three ParparVM desktop ports share it deliberately: a second
 * encoding is a second thing to keep in step with Command's placement constants.
 *
 * GtkMenuBar and GtkMenuItem, because this port is GTK 3 -- the GMenu/GtkPopoverMenuBar
 * pair is GTK 4. (The GNOME fidelity reference app IS GTK 4; it is a separate program.)
 *
 * Everything here runs on the GTK main thread, marshalled by the caller.
 */

#define CN1_MENU_MAX_ITEMS 512
#define CN1_MENU_MAX_POPUPS 32

static int cn1MenuCommandIds[CN1_MENU_MAX_ITEMS];
static int cn1MenuItemCount = 0;

/* The standard top-level menus. The hint strings are Command.DESKTOP_MENU_* verbatim;
 * anything else becomes a top-level menu titled with the hint itself, and an empty hint
 * lands in "Commands".
 *
 * About, Preferences and Quit have no application menu to go to -- GNOME has no menu bar
 * app menu the way macOS does -- so they go where a GNOME user looks: About under Help,
 * the other two under File. A placement decision, which is why this table is not simply a
 * copy of the macOS one. */
static const char* cn1MenuTitleForHint(const char* hint) {
    if (strcmp(hint, "File") == 0 || strcmp(hint, "Preferences") == 0
            || strcmp(hint, "Quit") == 0 || strcmp(hint, "App") == 0) {
        return "File";
    }
    if (strcmp(hint, "Edit") == 0) {
        return "Edit";
    }
    if (strcmp(hint, "View") == 0) {
        return "View";
    }
    if (strcmp(hint, "Window") == 0) {
        return "Window";
    }
    if (strcmp(hint, "Help") == 0 || strcmp(hint, "About") == 0) {
        return "Help";
    }
    return hint[0] == '\0' ? "Commands" : hint;
}

/* The chosen item's slot, carried as the widget's own data so the callback needs no
 * lookup table beyond the id array. */
static void cn1OnMenuItem(GtkMenuItem* item, gpointer data) {
    (void) item;
    int slot = GPOINTER_TO_INT(data);
    if (slot < 0 || slot >= cn1MenuItemCount) {
        return;
    }
    /* Queued, not run here: this is the GTK thread. The EDT drains it through pollEvent,
     * which is the rule every other input on this port follows. */
    cn1LinuxPushEvent(CN1_EVENT_MENU_COMMAND, 0, 0, cn1MenuCommandIds[slot]);
}

/* Reads one tab-delimited field into out, advancing *cursor past the delimiter. Returns 0
 * at the end of the row. */
static int cn1MenuNextField(const char** cursor, char* out, size_t cap) {
    const char* p = *cursor;
    size_t n = 0;
    while (*p != '\0' && *p != '\t' && *p != '\n') {
        if (n + 1 < cap) {
            out[n++] = *p;
        }
        p++;
    }
    out[n] = '\0';
    if (*p == '\t') {
        p++;
        *cursor = p;
        return 1;
    }
    *cursor = p;
    return 0;
}

/* Attaches the accelerator so GTK both draws it beside the item and responds to it. */
static void cn1MenuAddAccel(GtkWidget* item, GtkAccelGroup* accels, int keyChar,
                            int modifiers) {
    if (keyChar == 0 || accels == NULL) {
        return;
    }
    GdkModifierType mods = 0;
    /* Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY is Control here, which is the point of the
     * constant: the same application code produces Command on a Mac. */
    if (modifiers & 1) {
        mods |= GDK_CONTROL_MASK;
    }
    if (modifiers & 2) {
        mods |= GDK_SHIFT_MASK;
    }
    if (modifiers & 4) {
        mods |= GDK_MOD1_MASK;
    }
    guint key = gdk_unicode_to_keyval((guint) keyChar);
    if (key != 0) {
        gtk_widget_add_accelerator(item, "activate", accels, key, mods, GTK_ACCEL_VISIBLE);
    }
}

/* Rebuilds the bar. On the GTK main thread; see the section header. */
static void cn1MenuRebuild(void* arg) {
    const char* spec = (const char*) arg;
    if (cn1Window == 0 || cn1RootBox == 0) {
        return;
    }
    if (cn1MenuBar != 0) {
        gtk_widget_destroy(cn1MenuBar);
        cn1MenuBar = 0;
    }
    cn1MenuItemCount = 0;
    if (spec == NULL || spec[0] == '\0') {
        /* No commands: nothing is packed above the overlay, which is the layout every
         * existing screenshot was captured with. */
        return;
    }

    GtkWidget* bar = gtk_menu_bar_new();
    if (cn1MenuAccels == 0) {
        cn1MenuAccels = gtk_accel_group_new();
        gtk_window_add_accel_group(GTK_WINDOW(cn1Window), cn1MenuAccels);
    }
    GtkAccelGroup* accels = cn1MenuAccels;

    GtkWidget* popups[CN1_MENU_MAX_POPUPS];
    char titles[CN1_MENU_MAX_POPUPS][64];
    int popupCount = 0;

    const char* cursor = spec;
    while (*cursor != '\0' && cn1MenuItemCount < CN1_MENU_MAX_ITEMS) {
        char hint[64];
        char label[256];
        char keyField[16];
        char modField[16];
        char idField[24];
        cn1MenuNextField(&cursor, hint, sizeof(hint));
        cn1MenuNextField(&cursor, label, sizeof(label));
        cn1MenuNextField(&cursor, keyField, sizeof(keyField));
        cn1MenuNextField(&cursor, modField, sizeof(modField));
        cn1MenuNextField(&cursor, idField, sizeof(idField));
        if (*cursor == '\n') {
            cursor++;
        }
        if (label[0] == '\0') {
            continue;
        }

        const char* title = cn1MenuTitleForHint(hint);
        GtkWidget* popup = 0;
        for (int i = 0; i < popupCount; i++) {
            if (strcmp(titles[i], title) == 0) {
                popup = popups[i];
                break;
            }
        }
        if (popup == 0) {
            if (popupCount >= CN1_MENU_MAX_POPUPS) {
                continue;
            }
            popup = gtk_menu_new();
            GtkWidget* top = gtk_menu_item_new_with_label(title);
            gtk_menu_item_set_submenu(GTK_MENU_ITEM(top), popup);
            gtk_menu_shell_append(GTK_MENU_SHELL(bar), top);
            popups[popupCount] = popup;
            strncpy(titles[popupCount], title, 63);
            titles[popupCount][63] = '\0';
            popupCount++;
        }

        int slot = cn1MenuItemCount++;
        cn1MenuCommandIds[slot] = atoi(idField);
        GtkWidget* item = gtk_menu_item_new_with_label(label);
        cn1MenuAddAccel(item, accels, atoi(keyField), atoi(modField));
        g_signal_connect(item, "activate", G_CALLBACK(cn1OnMenuItem),
                GINT_TO_POINTER(slot));
        gtk_menu_shell_append(GTK_MENU_SHELL(popup), item);
    }

    if (popupCount == 0) {
        /* Every row was unusable. An empty bar is a strip the user cannot explain, so it is
         * not packed at all. */
        gtk_widget_destroy(bar);
        cn1MenuItemCount = 0;
        return;
    }

    cn1MenuBar = bar;
    gtk_box_pack_start(GTK_BOX(cn1RootBox), bar, FALSE, FALSE, 0);
    /* Above the overlay. pack_start appends, so the bar would otherwise sit under the
     * content it is supposed to head. */
    gtk_box_reorder_child(GTK_BOX(cn1RootBox), bar, 0);
    gtk_widget_show_all(bar);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_currentModifiers___R_int(
        CODENAME_ONE_THREAD_STATE) {
    return (JAVA_INT) cn1CurrentModifiers;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_menuSetCommands___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT spec) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    const char* utf8 = spec == JAVA_NULL ? "" : stringToUTF8(threadStateData, spec);
    /* AndWait, not a post, and that is load bearing twice over. The menu is in place before
     * this returns, so a form shown immediately afterwards cannot race the rebuild -- and,
     * more sharply, stringToUTF8 hands back THIS THREAD'S scratch buffer, which the next
     * conversion on this thread overwrites. A posted callback would read it after it had
     * moved on. A blocking hand-off cannot: nothing else runs here until it returns. */
    cn1LinuxRunOnMainAndWait(cn1MenuRebuild, (void*) utf8);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getDisplayWidth___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1DisplayWidth;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getDisplayHeight___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1DisplayHeight;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_screenDpi___R_int(CODENAME_ONE_THREAD_STATE) {
    if (cn1Window != 0) {
        GdkScreen* screen = gtk_widget_get_screen(cn1Window);
        if (screen != 0) {
            gdouble dpi = gdk_screen_get_resolution(screen);
            if (dpi > 0) {
                return (int) (dpi + 0.5);
            }
        }
    }
    return 96;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isHighContrastEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    const char* theme = getenv("GTK_THEME");
    return theme != NULL && strcasestr(theme, "highcontrast") != NULL ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isReduceMotionEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    const char* animations = getenv("GTK_ENABLE_ANIMATIONS");
    return animations != NULL && strcmp(animations, "0") == 0 ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isScreenReaderEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    const char* modules = getenv("GTK_MODULES");
    if (modules == NULL) {
        return JAVA_FALSE;
    }
    return strcasestr(modules, "atk-bridge") != NULL || strcasestr(modules, "gail") != NULL
            ? JAVA_TRUE : JAVA_FALSE;
}

/* True when the default seat has a touchscreen pointing device attached, so the
 * framework reports a touch device (Display.isTouchScreen()). */
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isTouchDevice___R_boolean(CODENAME_ONE_THREAD_STATE) {
    GdkDisplay* display = gdk_display_get_default();
    if (display == NULL) {
        return JAVA_FALSE;
    }
    GdkSeat* seat = gdk_display_get_default_seat(display);
    if (seat == NULL) {
        return JAVA_FALSE;
    }
    GList* devices = gdk_seat_get_slaves(seat, GDK_SEAT_CAPABILITY_ALL_POINTING);
    int found = 0;
    for (GList* l = devices; l != NULL; l = l->next) {
        GdkDevice* dev = (GdkDevice*) l->data;
        if (dev != NULL && gdk_device_get_source(dev) == GDK_SOURCE_TOUCHSCREEN) {
            found = 1;
            break;
        }
    }
    g_list_free(devices);
    return found ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_getWindowGraphics___R_long(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_LONG) (intptr_t) &cn1WindowG;
}

/* Schedules a redraw of the dirty rect on the GTK main loop (thread-safe entry). */
typedef struct { int x, y, w, h; } CN1Rect;

static gboolean cn1QueueDrawIdle(gpointer data) {
    CN1Rect* r = (CN1Rect*) data;
    if (cn1DrawingArea != 0) {
        gtk_widget_queue_draw_area(cn1DrawingArea, r->x, r->y, r->w, r->h);
    }
    free(r);
    return FALSE; /* one-shot */
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_flushGraphics___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    (void) graphics;
    if (cn1WindowG.cr) {
        cairo_surface_flush(cn1WindowG.surface);
    }
    if (cn1DrawingArea != 0) {
        CN1Rect* r = (CN1Rect*) malloc(sizeof(CN1Rect));
        r->x = x; r->y = y; r->w = width; r->h = height;
        gdk_threads_add_idle(cn1QueueDrawIdle, r);
    }
    /* The frame is finished and the next has not started, which is the only point
     * on this thread where replacing the buffer cannot pull it out from under a
     * paint in progress. Cairo is immediate mode, so there is no frame-open hook
     * to hang this on the way the Direct2D port does. */
    cn1ApplyPendingResize();
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_pollEvent___int_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    int scratch[5];
    if (out == JAVA_NULL) {
        return JAVA_FALSE;
    }
    if (cn1LinuxPopEvent(scratch)) {
        JAVA_INT* arr = (JAVA_INT*) CN1_ARRAY_DATA(out);
        int len = (int) (*(JAVA_ARRAY) out).length;
        if (len >= 4) {
            arr[0] = scratch[0];
            arr[1] = scratch[1];
            arr[2] = scratch[2];
            arr[3] = scratch[3];
            if (len >= 5) {
                arr[4] = scratch[4];
            }
            return JAVA_TRUE;
        }
    }
    return JAVA_FALSE;
}

/* ------------------------------------------------ accessibility / AT-SPI */

typedef struct CN1A11yEntry {
    long long id;
    long long parentId;
    GtkWidget* widget;
    int x, y;
    struct CN1A11yEntry* next;
} CN1A11yEntry;

typedef struct {
    long long nodeId;
    int actionHash;
    char* actionId;
    char* label;
} CN1A11yAction;

static CN1A11yEntry* cn1A11yEntries = 0;

static void cn1A11yFreeEntries(void) {
    CN1A11yEntry* entry = cn1A11yEntries;
    while (entry) {
        CN1A11yEntry* next = entry->next;
        free(entry);
        entry = next;
    }
    cn1A11yEntries = 0;
}

static CN1A11yEntry* cn1A11yFind(long long id) {
    CN1A11yEntry* entry;
    for (entry = cn1A11yEntries; entry; entry = entry->next) {
        if (entry->id == id) return entry;
    }
    return 0;
}

static AtkRole cn1A11yRole(const char* role) {
    if (!role) return ATK_ROLE_PANEL;
    if (!strcmp(role, "BUTTON") || !strcmp(role, "TOGGLE_BUTTON")) return ATK_ROLE_PUSH_BUTTON;
    if (!strcmp(role, "CHECKBOX")) return ATK_ROLE_CHECK_BOX;
    if (!strcmp(role, "RADIO_BUTTON")) return ATK_ROLE_RADIO_BUTTON;
    if (!strcmp(role, "SWITCH")) return ATK_ROLE_TOGGLE_BUTTON;
    if (!strcmp(role, "HEADING")) return ATK_ROLE_HEADING;
    if (!strcmp(role, "LINK")) return ATK_ROLE_LINK;
    if (!strcmp(role, "IMAGE")) return ATK_ROLE_IMAGE;
    if (!strcmp(role, "STATIC_TEXT")) return ATK_ROLE_LABEL;
    if (!strcmp(role, "TEXT_FIELD") || !strcmp(role, "SEARCH_FIELD")) return ATK_ROLE_ENTRY;
    if (!strcmp(role, "SLIDER")) return ATK_ROLE_SLIDER;
    if (!strcmp(role, "PROGRESS_BAR")) return ATK_ROLE_PROGRESS_BAR;
    if (!strcmp(role, "LIST")) return ATK_ROLE_LIST;
    if (!strcmp(role, "LIST_ITEM")) return ATK_ROLE_LIST_ITEM;
    if (!strcmp(role, "GRID")) return ATK_ROLE_TABLE;
    if (!strcmp(role, "ROW")) return ATK_ROLE_TABLE_ROW;
    if (!strcmp(role, "CELL")) return ATK_ROLE_TABLE_CELL;
    if (!strcmp(role, "COLUMN_HEADER")) return ATK_ROLE_COLUMN_HEADER;
    if (!strcmp(role, "ROW_HEADER")) return ATK_ROLE_ROW_HEADER;
    if (!strcmp(role, "TAB_LIST")) return ATK_ROLE_PAGE_TAB_LIST;
    if (!strcmp(role, "TAB")) return ATK_ROLE_PAGE_TAB;
    if (!strcmp(role, "DIALOG")) return ATK_ROLE_DIALOG;
    if (!strcmp(role, "ALERT")) return ATK_ROLE_ALERT;
    if (!strcmp(role, "MENU")) return ATK_ROLE_MENU;
    if (!strcmp(role, "MENU_ITEM")) return ATK_ROLE_MENU_ITEM;
    if (!strcmp(role, "TOOLBAR")) return ATK_ROLE_TOOL_BAR;
    if (!strcmp(role, "SCROLL_BAR")) return ATK_ROLE_SCROLL_BAR;
    if (!strcmp(role, "COMBO_BOX")) return ATK_ROLE_COMBO_BOX;
    if (!strcmp(role, "TREE")) return ATK_ROLE_TREE;
    if (!strcmp(role, "TREE_ITEM")) return ATK_ROLE_TREE_ITEM;
    if (!strcmp(role, "SEPARATOR")) return ATK_ROLE_SEPARATOR;
    return ATK_ROLE_PANEL;
}

static void cn1A11yBeginMain(void* ignored) {
    (void) ignored;
    cn1A11yFreeEntries();
    if (cn1AccessibilityFixed) {
        GList* children = gtk_container_get_children(GTK_CONTAINER(cn1AccessibilityFixed));
        GList* item;
        for (item = children; item; item = item->next) {
            gtk_widget_destroy(GTK_WIDGET(item->data));
        }
        g_list_free(children);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityBegin__(CODENAME_ONE_THREAD_STATE) {
    cn1LinuxRunOnMainAndWait(cn1A11yBeginMain, 0);
}

typedef struct {
    long long id, parentId;
    char *role, *label, *description, *value;
    int x, y, width, height, flags;
} CN1A11yNodeCall;

static GtkWidget* cn1A11yWidget(const char* role, const char* label, const char* value, int flags) {
    GtkWidget* widget;
    if (role && (!strcmp(role, "BUTTON") || !strcmp(role, "TOGGLE_BUTTON") || !strcmp(role, "SWITCH"))) {
        widget = gtk_toggle_button_new_with_label(label ? label : "");
        gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(widget), (flags & 0x30) == 0x20);
        gtk_toggle_button_set_inconsistent(GTK_TOGGLE_BUTTON(widget), (flags & 0x30) == 0x30);
    } else if (role && (!strcmp(role, "CHECKBOX") || !strcmp(role, "RADIO_BUTTON"))) {
        widget = gtk_check_button_new_with_label(label ? label : "");
        gtk_toggle_button_set_active(GTK_TOGGLE_BUTTON(widget), (flags & 0x30) == 0x20);
        gtk_toggle_button_set_inconsistent(GTK_TOGGLE_BUTTON(widget), (flags & 0x30) == 0x30);
    } else if (role && (!strcmp(role, "TEXT_FIELD") || !strcmp(role, "SEARCH_FIELD"))) {
        widget = gtk_entry_new();
        if (value) gtk_entry_set_text(GTK_ENTRY(widget), value);
    } else if (role && (!strcmp(role, "SLIDER") || !strcmp(role, "PROGRESS_BAR"))) {
        widget = gtk_scale_new_with_range(GTK_ORIENTATION_HORIZONTAL, 0, 100, 1);
        if (value) gtk_range_set_value(GTK_RANGE(widget), atof(value));
    } else {
        widget = gtk_fixed_new();
    }
    return widget;
}

static void cn1A11yNodeMain(void* pointer) {
    CN1A11yNodeCall* call = (CN1A11yNodeCall*) pointer;
    CN1A11yEntry* parent = cn1A11yFind(call->parentId);
    GtkWidget* host = parent ? parent->widget : cn1AccessibilityFixed;
    GtkWidget* widget = cn1A11yWidget(call->role, call->label, call->value, call->flags);
    AtkObject* accessible = gtk_widget_get_accessible(widget);
    atk_object_set_role(accessible, cn1A11yRole(call->role));
    if (call->label) atk_object_set_name(accessible, call->label);
    if (call->description) atk_object_set_description(accessible, call->description);
    gtk_widget_set_can_focus(widget, (call->flags & 1) != 0);
    gtk_widget_set_sensitive(widget, (call->flags & 4) != 0);
    gtk_widget_set_size_request(widget, call->width > 0 ? call->width : 1,
            call->height > 0 ? call->height : 1);
    if (host && GTK_IS_FIXED(host)) {
        int px = parent ? call->x - parent->x : call->x;
        int py = parent ? call->y - parent->y : call->y;
        gtk_fixed_put(GTK_FIXED(host), widget, px, py);
    } else if (host && GTK_IS_CONTAINER(host)) {
        gtk_container_add(GTK_CONTAINER(host), widget);
    }
    gtk_widget_show_all(widget);
    CN1A11yEntry* entry = (CN1A11yEntry*) calloc(1, sizeof(CN1A11yEntry));
    entry->id = call->id; entry->parentId = call->parentId; entry->widget = widget;
    entry->x = call->x; entry->y = call->y; entry->next = cn1A11yEntries; cn1A11yEntries = entry;
    free(call->role); free(call->label); free(call->description); free(call->value); free(call);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityNode___long_long_java_lang_String_java_lang_String_java_lang_String_java_lang_String_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG id, JAVA_LONG parentId, JAVA_OBJECT role, JAVA_OBJECT label,
        JAVA_OBJECT description, JAVA_OBJECT value, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height,
        JAVA_INT flags) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1A11yNodeCall* call = (CN1A11yNodeCall*) calloc(1, sizeof(CN1A11yNodeCall));
    call->id = id; call->parentId = parentId; call->x = x; call->y = y;
    call->width = width; call->height = height; call->flags = flags;
    call->role = role == JAVA_NULL ? 0 : strdup(stringToUTF8(threadStateData, role));
    call->label = label == JAVA_NULL ? 0 : strdup(stringToUTF8(threadStateData, label));
    call->description = description == JAVA_NULL ? 0 : strdup(stringToUTF8(threadStateData, description));
    call->value = value == JAVA_NULL ? 0 : strdup(stringToUTF8(threadStateData, value));
    cn1LinuxRunOnMainAndWait(cn1A11yNodeMain, call);
}

static void cn1A11yActionActivated(GtkWidget* widget, gpointer pointer) {
    CN1A11yAction* action = (CN1A11yAction*) pointer;
    (void) widget;
    cn1LinuxPushEvent(CN1_EVENT_ACCESSIBILITY_ACTION, (int) action->nodeId, 0, action->actionHash);
}

static void cn1A11yActionFree(gpointer pointer, GClosure* closure) {
    CN1A11yAction* action = (CN1A11yAction*) pointer;
    (void) closure;
    free(action->actionId); free(action->label); free(action);
}

static void cn1A11yActionMain(void* pointer) {
    CN1A11yAction* action = (CN1A11yAction*) pointer;
    CN1A11yEntry* entry = cn1A11yFind(action->nodeId);
    if (!entry) { cn1A11yActionFree(action, 0); return; }
    if (!strcmp(action->actionId, "activate") || !strcmp(action->actionId, "focus")) {
        if (GTK_IS_BUTTON(entry->widget)) {
            g_signal_connect_data(entry->widget, "clicked", G_CALLBACK(cn1A11yActionActivated), action,
                    cn1A11yActionFree, 0);
            return;
        }
    } else {
        GtkWidget* custom = gtk_button_new_with_label(action->label ? action->label : action->actionId);
        AtkObject* accessible = gtk_widget_get_accessible(custom);
        atk_object_set_name(accessible, action->label ? action->label : action->actionId);
        atk_object_set_description(accessible, "Custom accessibility action");
        gtk_widget_set_size_request(custom, entry->widget ? gtk_widget_get_allocated_width(entry->widget) : 1,
                entry->widget ? gtk_widget_get_allocated_height(entry->widget) : 1);
        gtk_fixed_put(GTK_FIXED(cn1AccessibilityFixed), custom, entry->x, entry->y);
        gtk_widget_show(custom);
        g_signal_connect_data(custom, "clicked", G_CALLBACK(cn1A11yActionActivated), action,
                cn1A11yActionFree, 0);
        return;
    }
    cn1A11yActionFree(action, 0);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityAction___long_java_lang_String_int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG nodeId, JAVA_OBJECT actionId, JAVA_INT actionHash, JAVA_OBJECT label) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1A11yAction* action = (CN1A11yAction*) calloc(1, sizeof(CN1A11yAction));
    action->nodeId = nodeId; action->actionHash = actionHash;
    action->actionId = actionId == JAVA_NULL ? strdup("") : strdup(stringToUTF8(threadStateData, actionId));
    action->label = label == JAVA_NULL ? 0 : strdup(stringToUTF8(threadStateData, label));
    cn1LinuxRunOnMainAndWait(cn1A11yActionMain, action);
}

static void cn1A11yEndMain(void* pointer) {
    (void) pointer;
    if (cn1AccessibilityFixed) gtk_widget_show_all(cn1AccessibilityFixed);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityEnd___int(CODENAME_ONE_THREAD_STATE, JAVA_INT changeType) {
    (void) changeType;
    cn1LinuxRunOnMainAndWait(cn1A11yEndMain, 0);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_pumpMessages___R_boolean(CODENAME_ONE_THREAD_STATE) {
    /* Process all pending GTK events, then block briefly for the next one so the
     * loop yields the CPU. Returns false once the window has been closed. */
    while (gtk_events_pending()) {
        gtk_main_iteration_do(FALSE);
    }
    if (!cn1WindowOpen) {
        return JAVA_FALSE;
    }
    /* Block for the next event so we are not a busy loop -- with this thread PARKED for
     * the wait and only the wait, as the Windows pump parks across GetMessage.
     *
     * This thread is the process's main thread, and the clean target registers main
     * as a managed thread (it runs Java until exit), so the collector waits for it to
     * reach a safepoint every cycle. g_main_context_iteration(NULL, TRUE) waits AND
     * dispatches in one call, and a thread blocked in its poll reaches no safepoint:
     * every collection waited the full safepoint bound (250ms) and then force-stopped
     * it, which is what made objectAllocation 3.5x slower in the Linux hello and
     * gallery apps. Parking across the whole call is not an answer either, because
     * the dispatch runs GTK callbacks that enter Java on this thread.
     *
     * So the iteration is spelled out -- prepare, query, poll, check, dispatch, which
     * is what g_main_context_iteration does internally -- and only the poll is
     * bracketed. Dispatch runs with the thread active again. */
    {
        GMainContext* ctx = g_main_context_default();
        if (!g_main_context_acquire(ctx)) {
            /* Another thread owns the context; nothing of ours to dispatch. */
            g_usleep(1000);
            return cn1WindowOpen ? JAVA_TRUE : JAVA_FALSE;
        }
        gint maxPriority = 0;
        g_main_context_prepare(ctx, &maxPriority);
        GPollFD stackFds[16];
        GPollFD* fds = stackFds;
        gint capacity = 16;
        gint timeout = -1;
        gint count = g_main_context_query(ctx, maxPriority, &timeout, fds, capacity);
        if (count > capacity) {
            capacity = count;
            fds = g_new(GPollFD, capacity);
            count = g_main_context_query(ctx, maxPriority, &timeout, fds, capacity);
        }
        GPollFunc poll = g_main_context_get_poll_func(ctx);
        CN1_YIELD_THREAD;
        poll(fds, (guint) count, timeout);
        CN1_RESUME_THREAD;
        if (g_main_context_check(ctx, maxPriority, fds, count)) {
            g_main_context_dispatch(ctx);
        }
        if (fds != stackFds) {
            g_free(fds);
        }
        g_main_context_release(ctx);
    }
    return cn1WindowOpen ? JAVA_TRUE : JAVA_FALSE;
}

/* ----------------------------------------------------- offscreen / headless */

JAVA_LONG com_codename1_impl_linux_LinuxNative_createOffscreenGraphics___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT width, JAVA_INT height) {
    CN1Graphics* g = (CN1Graphics*) calloc(1, sizeof(CN1Graphics));
    int w = width > 0 ? width : 1;
    int h = height > 0 ? height : 1;
    g->surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    g->cr = cairo_create(g->surface);
    g->width = w;
    g->height = h;
    g->alpha = 255;
    g->clipW = w;
    g->clipH = h;
    cairo_matrix_init_identity(&g->transform);
    return (JAVA_LONG) (intptr_t) g;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_saveGraphicsToPng___long_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_OBJECT path) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1Graphics* g = (CN1Graphics*) (intptr_t) graphics;
    const char* p = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    if (!g || !g->surface || !p) {
        return JAVA_FALSE;
    }
    cairo_surface_flush(g->surface);
    return cairo_surface_write_to_png(g->surface, p) == CAIRO_STATUS_SUCCESS ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_enableHeadlessScreenshot___java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_INT width, JAVA_INT height) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    const char* p = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    cn1Headless = 1;
    if (width > 0) cn1DisplayWidth = width;
    if (height > 0) cn1DisplayHeight = height;
    if (p) {
        strncpy(cn1HeadlessPath, p, sizeof(cn1HeadlessPath) - 1);
        cn1HeadlessPath[sizeof(cn1HeadlessPath) - 1] = 0;
    }
}

/* A growable byte buffer used as the cairo PNG write closure. */
struct CN1PngBuf { unsigned char* data; int len; int cap; };

static cairo_status_t cn1PngWrite(void* closure, const unsigned char* data, unsigned int length) {
    struct CN1PngBuf* b = (struct CN1PngBuf*) closure;
    if (b->len + (int) length > b->cap) {
        int cap = b->cap > 0 ? b->cap * 2 : 8192;
        while (cap < b->len + (int) length) cap *= 2;
        b->data = (unsigned char*) realloc(b->data, cap);
        b->cap = cap;
    }
    memcpy(b->data + b->len, data, length);
    b->len += (int) length;
    return CAIRO_STATUS_SUCCESS;
}

/* PNG-encodes an ARGB32 surface into a freshly malloc'd buffer; caller free()s
 * *outData. Returns 1 on success. Shared by image.c (encodeArgbToPng). */
int cn1LinuxSurfaceToPng(cairo_surface_t* surface, unsigned char** outData, int* outLen) {
    struct CN1PngBuf buf = { 0, 0, 0 };
    cairo_status_t st;
    cairo_surface_flush(surface);
    st = cairo_surface_write_to_png_stream(surface, cn1PngWrite, &buf);
    if (st != CAIRO_STATUS_SUCCESS) {
        free(buf.data);
        *outData = 0;
        *outLen = 0;
        return 0;
    }
    *outData = buf.data;
    *outLen = buf.len;
    return 1;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_captureWindowToPngBytes___R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    unsigned char* data = 0;
    int len = 0;
    JAVA_OBJECT arr;
    if (!cn1WindowG.surface || !cn1LinuxSurfaceToPng(cn1WindowG.surface, &data, &len)) {
        return JAVA_NULL;
    }
    arr = cn1LinuxNewByteArray(threadStateData, data, len);
    free(data);
    return arr;
}

/* ------------------------------------------------------------- colour scheme */

/* The desktop's colour scheme: 1 dark, 0 light, -1 unknown.
 *
 * Asked of the DESKTOP's setting, not of GTK's. The obvious-looking
 * gtk-application-prefer-dark-theme is the wrong source: it expresses whether the
 * application is ASKING for a dark GTK theme, and stays false unless the application sets
 * it -- so reading it reported light on a GNOME desktop in dark mode, and every $Dark
 * entry in the Adwaita theme stayed unreachable.
 *
 * org.gnome.desktop.interface color-scheme is what the user's toggle actually writes, and
 * what the XDG appearance portal reports to sandboxed apps. Queried through GSettings
 * rather than over D-Bus so there is no round trip and no portal dependency.
 *
 * The schema is looked up before it is opened. g_settings_new ABORTS the process when the
 * schema is not installed, which is a real configuration on a minimal container or a
 * non-GNOME desktop, and a theme query has no business killing the application.
 *
 * -1 is a real answer, not an error smuggled into the return: a session with no such
 * schema has no preference to report, and calling that "light" would be a guess presented
 * as a fact. The Java side maps it to null.
 *
 * The signature is ParparVM's and is checked by nothing at build time -- a wrong name
 * compiles, links, and leaves the Java method looking unused to the dead-code pass,
 * which then removes it. scripts/check-native-signatures.sh is what catches that.
 */
JAVA_INT com_codename1_impl_linux_LinuxNative_systemColorScheme___R_int(CODENAME_ONE_THREAD_STATE) {
    GSettingsSchemaSource* source = g_settings_schema_source_get_default();
    if (source == NULL) {
        return -1;
    }
    GSettingsSchema* schema = g_settings_schema_source_lookup(source,
            "org.gnome.desktop.interface", TRUE);
    if (schema == NULL) {
        return -1;
    }
    int result = -1;
    /* has_key as well as the schema lookup: color-scheme arrived in GNOME 42, and the
     * schema exists without it on older desktops. g_settings_get_string on a missing key
     * aborts the same way a missing schema does. */
    if (g_settings_schema_has_key(schema, "color-scheme")) {
        GSettings* settings = g_settings_new("org.gnome.desktop.interface");
        gchar* scheme = g_settings_get_string(settings, "color-scheme");
        if (scheme != NULL) {
            result = strcmp(scheme, "prefer-dark") == 0 ? 1 : 0;
            g_free(scheme);
        }
        g_object_unref(settings);
    }
    g_settings_schema_unref(schema);
    return result;
}
