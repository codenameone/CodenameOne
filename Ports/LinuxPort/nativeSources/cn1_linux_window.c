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

#include "cn1_linux_gfx.h"
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>
#ifdef __GLIBC__
#include <execinfo.h> /* backtrace() -- glibc only; musl has no execinfo.h */
#endif
#include <unistd.h>

/* ----------------------------------------------------------- event ring */

#define CN1_EVENT_RING 1024
typedef struct {
    int type, x, y, key;
} CN1Event;
static CN1Event cn1EventRing[CN1_EVENT_RING];
static int cn1EventHead = 0;
static int cn1EventTail = 0;
static pthread_mutex_t cn1EventLock = PTHREAD_MUTEX_INITIALIZER;

void cn1LinuxPushEvent(int type, int x, int y, int keyCode) {
    pthread_mutex_lock(&cn1EventLock);
    int next = (cn1EventTail + 1) % CN1_EVENT_RING;
    if (next != cn1EventHead) {
        cn1EventRing[cn1EventTail].type = type;
        cn1EventRing[cn1EventTail].x = x;
        cn1EventRing[cn1EventTail].y = y;
        cn1EventRing[cn1EventTail].key = keyCode;
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
        cn1EventHead = (cn1EventHead + 1) % CN1_EVENT_RING;
        has = 1;
    }
    pthread_mutex_unlock(&cn1EventLock);
    return has;
}

/* ------------------------------------------------------------- globals */

static GtkWidget* cn1Window = 0;
static GtkWidget* cn1DrawingArea = 0;
static GtkWidget* cn1Overlay = 0;       /* GtkOverlay: drawing area + native widget layer */
static GtkWidget* cn1Fixed = 0;         /* GtkFixed overlay hosting positioned native peers */
static CN1Graphics cn1WindowG;          /* the on-screen / headless back buffer */
static int cn1DisplayWidth = 800;
static int cn1DisplayHeight = 600;
static int cn1WindowOpen = 0;
static int cn1Headless = 0;
static char cn1HeadlessPath[4096];
static int cn1Initialized = 0;

CN1Graphics* cn1LinuxWindowGraphics(void) {
    return &cn1WindowG;
}

/* Native-peer overlay management (edit / browser / video / generic peers). All
 * must run on the GTK main thread (callers marshal via gdk_threads_add_idle). */
void cn1LinuxOverlayAdd(GtkWidget* w, int x, int y, int width, int height) {
    if (cn1Fixed == 0 || w == 0) {
        return;
    }
    gtk_widget_set_size_request(w, width, height);
    gtk_fixed_put(GTK_FIXED(cn1Fixed), w, x, y);
    gtk_widget_show_all(w);
}

void cn1LinuxOverlayMove(GtkWidget* w, int x, int y, int width, int height) {
    if (cn1Fixed == 0 || w == 0) {
        return;
    }
    gtk_widget_set_size_request(w, width, height);
    gtk_fixed_move(GTK_FIXED(cn1Fixed), w, x, y);
}

void cn1LinuxOverlayRemove(GtkWidget* w) {
    if (cn1Fixed != 0 && w != 0 && gtk_widget_get_parent(w) == cn1Fixed) {
        gtk_container_remove(GTK_CONTAINER(cn1Fixed), w);
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

/* ------------------------------------------------------ GTK callbacks */

static gboolean cn1OnDraw(GtkWidget* widget, cairo_t* cr, gpointer data) {
    (void) widget;
    (void) data;
    if (cn1WindowG.surface) {
        cairo_set_source_surface(cr, cn1WindowG.surface, 0, 0);
        cairo_paint(cr);
    }
    return FALSE;
}

static gboolean cn1OnConfigure(GtkWidget* widget, GdkEventConfigure* e, gpointer data) {
    (void) widget;
    (void) data;
    if (e->width != cn1DisplayWidth || e->height != cn1DisplayHeight) {
        cn1DisplayWidth = e->width;
        cn1DisplayHeight = e->height;
        cn1ResizeBackBuffer(cn1DisplayWidth, cn1DisplayHeight);
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

/* True when an event originated from a touchscreen. GTK also synthesizes button
 * / motion events from touch for widgets that ignore touch, so we drop those
 * here and let cn1OnTouch drive the pointer instead (avoids double dispatch). */
static int cn1LinuxIsTouchSource(GdkEvent* e) {
    GdkDevice* dev = gdk_event_get_source_device(e);
    return dev != NULL && gdk_device_get_source(dev) == GDK_SOURCE_TOUCHSCREEN;
}

static gboolean cn1OnButton(GtkWidget* widget, GdkEventButton* e, gpointer data) {
    (void) widget;
    (void) data;
    if (cn1LinuxIsTouchSource((GdkEvent*) e)) {
        return TRUE;
    }
    cn1LinuxPushEvent(e->type == GDK_BUTTON_PRESS ? CN1_EVENT_POINTER_PRESSED : CN1_EVENT_POINTER_RELEASED,
            (int) e->x, (int) e->y, cn1LinuxButtonMask(e->button));
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
        cn1LinuxPushEvent(CN1_EVENT_POINTER_DRAGGED, (int) e->x, (int) e->y, mask);
    }
    return TRUE;
}

/* The primary touch sequence currently driving the pointer (single-touch
 * model). Additional concurrent fingers are ignored until it ends. */
static GdkEventSequence* cn1TouchSeq = NULL;

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
 * angle_delta is already a per-event delta in degrees, forwarded as incremental
 * radians. These map to Display.fireMagnifyGesture / fireRotationGesture, the
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
            double rad = pe->angle_delta * G_PI / 180.0;
            cn1LinuxPushEvent(CN1_EVENT_ROTATE, x, y,
                    (int) (rad * CN1_GESTURE_FIXED + (rad >= 0 ? 0.5 : -0.5)));
        }
    }
    return TRUE;
}

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
static void cn1LinuxFaultToException(int sig, siginfo_t* si, void* ucv) {
    (void) ucv;
    /* Only a genuine null-ish deref (null + a small field/vtable/array offset)
     * is converted to an NPE. A wild faulting address is real memory corruption:
     * restore the default disposition and return so the re-executed instruction
     * faults again into a core dump that stays diagnosable, instead of being
     * masked as a recoverable NPE that silently corrupts further state. */
    if ((sig == SIGSEGV || sig == SIGBUS) && si != NULL &&
            (uintptr_t) si->si_addr >= 0x10000) {
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
    /* Restore default SIGSEGV/SIGBUS so a fault while walking a corrupted stack
     * just dies here instead of longjmp-ing out via the NPE handler. */
    signal(SIGSEGV, SIG_DFL);
    signal(SIGBUS, SIG_DFL);
    const char* hdr = "\n=====CN1 ABORT BACKTRACE=====\n";
    write(2, hdr, strlen(hdr));
#ifdef __GLIBC__
    void* bt[80];
    int n = backtrace(bt, 80);
    backtrace_symbols_fd(bt, n, 2);
#else
    /* musl has no execinfo.h/backtrace(); fall through with just the markers. */
    const char* na = "(native backtrace unavailable on this libc)\n";
    write(2, na, strlen(na));
#endif
    const char* ftr = "=====END CN1 ABORT BACKTRACE=====\n";
    write(2, ftr, strlen(ftr));
    signal(sig, SIG_DFL);
    raise(sig);
}

static void cn1LinuxInstallFaultHandlers() {
    static int installed = 0;
    if (installed || getenv("CN1_LINUX_NO_FAULT_HANDLER") != NULL) {
        return;
    }
    installed = 1;
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1LinuxFaultToException;
    sa.sa_flags = SA_SIGINFO | SA_NODEFER;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);
    /* Dump a native backtrace if the process aborts (e.g. __stack_chk_fail or a
     * glib g_error) so the failing frame is identifiable from the CI log without a
     * debugger. Silent unless an abort actually fires. */
    signal(SIGABRT, cn1LinuxAbortBacktrace);
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
            GDK_KEY_PRESS_MASK | GDK_KEY_RELEASE_MASK | GDK_SCROLL_MASK | GDK_TOUCH_MASK |
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
    gtk_container_add(GTK_CONTAINER(cn1Window), cn1Overlay);

    g_signal_connect(cn1DrawingArea, "draw", G_CALLBACK(cn1OnDraw), 0);
    g_signal_connect(cn1DrawingArea, "configure-event", G_CALLBACK(cn1OnConfigure), 0);
    g_signal_connect(cn1DrawingArea, "button-press-event", G_CALLBACK(cn1OnButton), 0);
    g_signal_connect(cn1DrawingArea, "button-release-event", G_CALLBACK(cn1OnButton), 0);
    g_signal_connect(cn1DrawingArea, "motion-notify-event", G_CALLBACK(cn1OnMotion), 0);
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
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_pollEvent___int_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    int scratch[4];
    if (out == JAVA_NULL) {
        return JAVA_FALSE;
    }
    if (cn1LinuxPopEvent(scratch)) {
        JAVA_INT* arr = (JAVA_INT*) (*(JAVA_ARRAY) out).data;
        int len = (int) (*(JAVA_ARRAY) out).length;
        if (len >= 4) {
            arr[0] = scratch[0];
            arr[1] = scratch[1];
            arr[2] = scratch[2];
            arr[3] = scratch[3];
            return JAVA_TRUE;
        }
    }
    return JAVA_FALSE;
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
    /* Block for the next event (TRUE = may block) so we are not a busy loop. */
    g_main_context_iteration(NULL, TRUE);
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
