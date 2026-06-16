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
 * BrowserComponent backing for the native Codename One Linux port, via WebKitGTK
 * (webkit2gtk-4.1). The peer is a WebKitWebView placed in the window overlay and
 * tracked to the lightweight BrowserComponent bounds. Navigation/load events are
 * queued for the EDT (browserPollEvent), and the JS->Java bridge is a WebKit user
 * content script message handler ("cn1") whose messages surface as MSG| events.
 * All WebKit calls run on the GTK main thread.
 *
 * WebKitGTK is loaded with dlopen() at first use rather than linked, so the port
 * binary depends only on the GTK3 core at runtime: a desktop without WebKitGTK
 * installed still runs -- BrowserComponent just reports unsupported. The header is
 * still included (for the types/macros/enums) but no webkit symbol is referenced
 * at link time; every entry point is resolved through dlsym (see cn1LoadWebkit).
 */

#include "cn1_linux_gfx.h"
#include <webkit2/webkit2.h>
#include <dlfcn.h>
#include <pthread.h>
#include <string.h>
#include <stdlib.h>

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

/* --- lazily resolved WebKitGTK entry points (dlopen, not linked) ---------- */
static __typeof__(webkit_web_view_get_uri)*                                    p_webkit_web_view_get_uri;
static __typeof__(webkit_user_content_manager_new)*                            p_webkit_user_content_manager_new;
static __typeof__(webkit_user_content_manager_register_script_message_handler)* p_webkit_user_content_manager_register_script_message_handler;
static __typeof__(webkit_web_view_new_with_user_content_manager)*              p_webkit_web_view_new_with_user_content_manager;
static __typeof__(webkit_javascript_result_get_js_value)*                      p_webkit_javascript_result_get_js_value;
static __typeof__(jsc_value_to_string)*                                        p_jsc_value_to_string;
static __typeof__(webkit_web_view_load_html)*                                  p_webkit_web_view_load_html;
static __typeof__(webkit_web_view_load_uri)*                                   p_webkit_web_view_load_uri;
static __typeof__(webkit_web_view_run_javascript)*                             p_webkit_web_view_run_javascript;

static int cn1_wk_state = 0; /* 0 = untried, 1 = available, -1 = unavailable */

/* dlopen WebKitGTK and resolve the entry points once. Returns non-zero when the
 * BrowserComponent peer can be created; 0 (and a one-time log) when WebKitGTK is
 * not installed, so the rest of the UI keeps working on a GTK3-only system. */
static int cn1LoadWebkit(void) {
    void* h;
    int ok = 1;
    if (cn1_wk_state) { return cn1_wk_state > 0; }
    /* Prefer webkit2gtk-4.1 (libsoup3), but fall back to the still-common 4.0
     * (libsoup2) build: the entry points we resolve below have identical names and
     * signatures across both, so basic browsing works either way. Ubuntu/Debian in
     * particular frequently ship only 4.0 (soname libwebkit2gtk-4.0.so.37), which is
     * why a 4.1-only lookup left BrowserComponent blank on those desktops. */
    h = dlopen("libwebkit2gtk-4.1.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!h) { h = dlopen("libwebkit2gtk-4.1.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!h) { h = dlopen("libwebkit2gtk-4.0.so.37", RTLD_LAZY | RTLD_GLOBAL); }
    if (!h) { h = dlopen("libwebkit2gtk-4.0.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!h) {
        cn1_wk_state = -1;
        cn1LinuxStubOnce("WebKitGTK (libwebkit2gtk-4.1/4.0) not installed; BrowserComponent unsupported");
        return 0;
    }
#define CN1_WK_SYM(ptr, name) do { *(void**)(&ptr) = dlsym(h, name); if (!(ptr)) { ok = 0; } } while (0)
    CN1_WK_SYM(p_webkit_web_view_get_uri, "webkit_web_view_get_uri");
    CN1_WK_SYM(p_webkit_user_content_manager_new, "webkit_user_content_manager_new");
    CN1_WK_SYM(p_webkit_user_content_manager_register_script_message_handler, "webkit_user_content_manager_register_script_message_handler");
    CN1_WK_SYM(p_webkit_web_view_new_with_user_content_manager, "webkit_web_view_new_with_user_content_manager");
    CN1_WK_SYM(p_webkit_javascript_result_get_js_value, "webkit_javascript_result_get_js_value");
    CN1_WK_SYM(p_jsc_value_to_string, "jsc_value_to_string");
    CN1_WK_SYM(p_webkit_web_view_load_html, "webkit_web_view_load_html");
    CN1_WK_SYM(p_webkit_web_view_load_uri, "webkit_web_view_load_uri");
    CN1_WK_SYM(p_webkit_web_view_run_javascript, "webkit_web_view_run_javascript");
#undef CN1_WK_SYM
    cn1_wk_state = ok ? 1 : -1;
    if (!ok) { cn1LinuxStubOnce("WebKitGTK present but an expected symbol was missing; BrowserComponent unsupported"); }
    return ok;
}

#define CN1_BROWSER_QUEUE 64

typedef struct {
    GtkWidget* view;
    char* queue[CN1_BROWSER_QUEUE];
    int head, tail;
    pthread_mutex_t lock;
} CN1Browser;

static void cn1BrowserPush(CN1Browser* b, const char* msg) {
    int next;
    pthread_mutex_lock(&b->lock);
    next = (b->tail + 1) % CN1_BROWSER_QUEUE;
    if (next != b->head) {
        b->queue[b->tail] = g_strdup(msg);
        b->tail = next;
    }
    pthread_mutex_unlock(&b->lock);
}

static void cn1BrowserLoadChanged(WebKitWebView* view, WebKitLoadEvent ev, gpointer data) {
    CN1Browser* b = (CN1Browser*) data;
    if (ev == WEBKIT_LOAD_STARTED) {
        const char* uri = p_webkit_web_view_get_uri(view);
        char* msg = g_strconcat("NAV|", uri ? uri : "", NULL);
        cn1BrowserPush(b, msg);
        g_free(msg);
    } else if (ev == WEBKIT_LOAD_FINISHED) {
        cn1BrowserPush(b, "LOAD");
    }
}

static void cn1BrowserScriptMessage(WebKitUserContentManager* mgr, WebKitJavascriptResult* res, gpointer data) {
    CN1Browser* b = (CN1Browser*) data;
    JSCValue* value = p_webkit_javascript_result_get_js_value(res);
    char* str = p_jsc_value_to_string(value);
    char* msg = g_strconcat("MSG|", str ? str : "", NULL);
    cn1BrowserPush(b, msg);
    g_free(msg);
    g_free(str);
    (void) mgr;
}

typedef struct { int w, h; CN1Browser* result; } CN1BrowserCreateReq;

static void cn1BrowserCreateOnMain(void* p) {
    CN1BrowserCreateReq* req = (CN1BrowserCreateReq*) p;
    CN1Browser* b = (CN1Browser*) calloc(1, sizeof(CN1Browser));
    WebKitUserContentManager* mgr = p_webkit_user_content_manager_new();
    pthread_mutex_init(&b->lock, 0);
    p_webkit_user_content_manager_register_script_message_handler(mgr, "cn1");
    g_signal_connect(mgr, "script-message-received::cn1", G_CALLBACK(cn1BrowserScriptMessage), b);
    b->view = p_webkit_web_view_new_with_user_content_manager(mgr);
    g_signal_connect(b->view, "load-changed", G_CALLBACK(cn1BrowserLoadChanged), b);
    cn1LinuxOverlayAdd(b->view, 0, 0, req->w > 0 ? req->w : 1, req->h > 0 ? req->h : 1);
    req->result = b;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_browserSupported___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return (cn1LinuxWindowWidget() != 0 && cn1LoadWebkit()) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_browserCreate___int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT w, JAVA_INT h) {
    CN1BrowserCreateReq req;
    if (cn1LinuxWindowWidget() == 0 || !cn1LoadWebkit()) {
        return 0;
    }
    req.w = w;
    req.h = h;
    req.result = 0;
    cn1LinuxRunOnMainAndWait(cn1BrowserCreateOnMain, &req);
    return (JAVA_LONG) (intptr_t) req.result;
}

typedef struct { CN1Browser* b; char* a; char* c; int x, y, w, h; } CN1BrowserOp;

static void cn1BrowserSetHtmlOnMain(void* p) {
    CN1BrowserOp* op = (CN1BrowserOp*) p;
    p_webkit_web_view_load_html(WEBKIT_WEB_VIEW(op->b->view), op->a ? op->a : "", op->c);
}

static void cn1BrowserSetUrlOnMain(void* p) {
    CN1BrowserOp* op = (CN1BrowserOp*) p;
    p_webkit_web_view_load_uri(WEBKIT_WEB_VIEW(op->b->view), op->a ? op->a : "about:blank");
}

static void cn1BrowserExecuteOnMain(void* p) {
    CN1BrowserOp* op = (CN1BrowserOp*) p;
    p_webkit_web_view_run_javascript(WEBKIT_WEB_VIEW(op->b->view), op->a ? op->a : "", 0, 0, 0);
}

static void cn1BrowserBoundsOnMain(void* p) {
    CN1BrowserOp* op = (CN1BrowserOp*) p;
    cn1LinuxOverlayMove(op->b->view, op->x, op->y, op->w, op->h);
}

static void cn1BrowserSetVisibleOnMain(void* p) {
    CN1BrowserOp* op = (CN1BrowserOp*) p;
    if (op->x) {
        gtk_widget_show(op->b->view);
    } else {
        gtk_widget_hide(op->b->view);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_browserSetHtml___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT html) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    CN1BrowserOp op;
    if (!b) { return; }
    op.b = b;
    op.a = html == JAVA_NULL ? 0 : g_strdup(stringToUTF8(threadStateData, html));
    op.c = 0;
    cn1LinuxRunOnMainAndWait(cn1BrowserSetHtmlOnMain, &op);
    g_free(op.a);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_browserSetUrl___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT url) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    CN1BrowserOp op;
    if (!b) { return; }
    op.b = b;
    op.a = url == JAVA_NULL ? 0 : g_strdup(stringToUTF8(threadStateData, url));
    cn1LinuxRunOnMainAndWait(cn1BrowserSetUrlOnMain, &op);
    g_free(op.a);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_browserExecute___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_OBJECT js) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    CN1BrowserOp op;
    if (!b) { return; }
    op.b = b;
    op.a = js == JAVA_NULL ? 0 : g_strdup(stringToUTF8(threadStateData, js));
    cn1LinuxRunOnMainAndWait(cn1BrowserExecuteOnMain, &op);
    g_free(op.a);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_browserSetBounds___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    CN1BrowserOp op;
    if (!b) { return; }
    op.b = b;
    op.x = x; op.y = y; op.w = w; op.h = h;
    cn1LinuxRunOnMainAndWait(cn1BrowserBoundsOnMain, &op);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_browserSetVisible___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer, JAVA_BOOLEAN visible) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    CN1BrowserOp op;
    if (!b) { return; }
    op.b = b;
    op.x = visible ? 1 : 0;
    cn1LinuxRunOnMainAndWait(cn1BrowserSetVisibleOnMain, &op);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_browserPollEvent___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    JAVA_OBJECT result = JAVA_NULL;
    if (!b) {
        return JAVA_NULL;
    }
    pthread_mutex_lock(&b->lock);
    if (b->head != b->tail) {
        char* msg = b->queue[b->head];
        b->head = (b->head + 1) % CN1_BROWSER_QUEUE;
        result = newStringFromCString(threadStateData, msg);
        g_free(msg);
    }
    pthread_mutex_unlock(&b->lock);
    return result;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_browserCapturePng___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    /* WebKit snapshots are async (webkit_web_view_get_snapshot); the transition
     * peer-image path tolerates a null (no snapshot) and falls back to the live
     * widget, so this returns null until the async snapshot bridge is wired. */
    (void) peer;
    cn1LinuxStubOnce("browserCapturePng (async WebKit snapshot pending)");
    return JAVA_NULL;
}

static void cn1BrowserDestroyOnMain(void* p) {
    CN1Browser* b = (CN1Browser*) p;
    if (b->view) {
        cn1LinuxOverlayRemove(b->view);
        gtk_widget_destroy(b->view);
        b->view = 0;
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_browserDestroy___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Browser* b = (CN1Browser*) (intptr_t) peer;
    if (!b) {
        return;
    }
    cn1LinuxRunOnMainAndWait(cn1BrowserDestroyOnMain, b);
    pthread_mutex_destroy(&b->lock);
    free(b);
}
