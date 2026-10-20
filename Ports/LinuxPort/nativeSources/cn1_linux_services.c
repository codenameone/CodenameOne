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
 * Desktop service integrations for the native Codename One Linux port:
 *   - clipboard           GtkClipboard
 *   - shellOpen           GIO g_app_info_launch_default_for_uri (http/mailto/tel/file)
 *   - local notifications libnotify
 *   - secure storage      libsecret (Secret Service / GNOME Keyring)
 *   - file dialog         GtkFileChooser (modal, marshaled to the GTK thread)
 *   - location            GeoClue 2 (libgeoclue)
 *   - share / contacts    honest minimal (no universal Linux desktop API)
 *   - biometrics          fprintd presence detection over D-Bus
 *
 * Modal / GTK-thread-only calls invoked from the EDT are marshaled onto the GTK
 * main loop and the EDT blocks for the result (cn1RunOnMainAndWait).
 */

#include "cn1_linux_gfx.h"
#include <libnotify/notify.h>
#include <libsecret/secret.h>
#include <geoclue.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern GtkWidget* cn1LinuxWindowWidget(void);
extern JAVA_OBJECT cn1LinuxNewByteArray(CODENAME_ONE_THREAD_STATE, const void* src, int n);
extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__java_lang_String;

/* The run-on-GTK-thread-and-wait helper (cn1LinuxRunOnMainAndWait) is shared from
 * cn1_linux_window.c. */

/* ----------------------------------------------------------------------------
 * The three desktop services that depend on optional libraries -- secure storage
 * (libsecret), local notifications (libnotify) and location (libgeoclue) -- are
 * loaded with dlopen() at first use rather than linked, so the port binary
 * depends only on the GTK3/GLib/GIO core at runtime. A desktop missing any of
 * these libraries still runs: the affected service simply reports unsupported and
 * returns a safe default. GLib/GObject/GIO/GDBus/GTK and libc stay linked.
 *
 * Each library has an independent loader (cn1LoadSecret / cn1LoadNotify /
 * cn1LoadGeoclue) that dlopens the soname, resolves every entry point through
 * dlsym into a __typeof__ function pointer (signatures kept correct by the
 * already-included headers) and caches its state. Upper-case macros/enums/types
 * are compile-time constructs and are not resolved here. */

/* --- libsecret (libsecret-1.so.0) ----------------------------------------- */
static __typeof__(secret_password_store_sync)*  p_secret_password_store_sync;
static __typeof__(secret_password_lookup_sync)* p_secret_password_lookup_sync;
static __typeof__(secret_password_free)*        p_secret_password_free;
static __typeof__(secret_password_clear_sync)*  p_secret_password_clear_sync;
static int cn1_secret_state = 0; /* 0 = untried, 1 = available, -1 = unavailable */

static int cn1LoadSecret(void) {
    void* h;
    int ok = 1;
    if (cn1_secret_state) { return cn1_secret_state > 0; }
    h = dlopen("libsecret-1.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!h) { h = dlopen("libsecret-1.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!h) {
        cn1_secret_state = -1;
        cn1LinuxStubOnce("libsecret (libsecret-1) not installed; secure storage unsupported");
        return 0;
    }
#define CN1_SECRET_SYM(ptr, name) do { *(void**)(&ptr) = dlsym(h, name); if (!(ptr)) { ok = 0; } } while (0)
    CN1_SECRET_SYM(p_secret_password_store_sync, "secret_password_store_sync");
    CN1_SECRET_SYM(p_secret_password_lookup_sync, "secret_password_lookup_sync");
    CN1_SECRET_SYM(p_secret_password_free, "secret_password_free");
    CN1_SECRET_SYM(p_secret_password_clear_sync, "secret_password_clear_sync");
#undef CN1_SECRET_SYM
    cn1_secret_state = ok ? 1 : -1;
    if (!ok) { cn1LinuxStubOnce("libsecret present but an expected symbol was missing; secure storage unsupported"); }
    return ok;
}

/* --- libnotify (libnotify.so.4) ------------------------------------------- */
static __typeof__(notify_init)*                   p_notify_init;
static __typeof__(notify_notification_new)*       p_notify_notification_new;
static __typeof__(notify_notification_add_action)* p_notify_notification_add_action;
static __typeof__(notify_notification_show)*      p_notify_notification_show;
static int cn1_notify_state = 0; /* 0 = untried, 1 = available, -1 = unavailable */

static int cn1LoadNotify(void) {
    void* h;
    int ok = 1;
    if (cn1_notify_state) { return cn1_notify_state > 0; }
    h = dlopen("libnotify.so.4", RTLD_LAZY | RTLD_GLOBAL);
    if (!h) { h = dlopen("libnotify.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!h) {
        cn1_notify_state = -1;
        cn1LinuxStubOnce("libnotify not installed; local notifications unsupported");
        return 0;
    }
#define CN1_NOTIFY_SYM(ptr, name) do { *(void**)(&ptr) = dlsym(h, name); if (!(ptr)) { ok = 0; } } while (0)
    CN1_NOTIFY_SYM(p_notify_init, "notify_init");
    CN1_NOTIFY_SYM(p_notify_notification_new, "notify_notification_new");
    CN1_NOTIFY_SYM(p_notify_notification_add_action, "notify_notification_add_action");
    CN1_NOTIFY_SYM(p_notify_notification_show, "notify_notification_show");
#undef CN1_NOTIFY_SYM
    cn1_notify_state = ok ? 1 : -1;
    if (!ok) { cn1LinuxStubOnce("libnotify present but an expected symbol was missing; local notifications unsupported"); }
    return ok;
}

/* --- geoclue (libgeoclue-2.so.0) ------------------------------------------ */
static __typeof__(gclue_simple_new_sync)*       p_gclue_simple_new_sync;
static __typeof__(gclue_simple_get_location)*   p_gclue_simple_get_location;
static __typeof__(gclue_location_get_latitude)* p_gclue_location_get_latitude;
static __typeof__(gclue_location_get_longitude)* p_gclue_location_get_longitude;
static __typeof__(gclue_location_get_accuracy)* p_gclue_location_get_accuracy;
static __typeof__(gclue_location_get_altitude)* p_gclue_location_get_altitude;
static __typeof__(gclue_location_get_heading)*  p_gclue_location_get_heading;
static __typeof__(gclue_location_get_speed)*    p_gclue_location_get_speed;
static int cn1_geoclue_state = 0; /* 0 = untried, 1 = available, -1 = unavailable */

static int cn1LoadGeoclue(void) {
    void* h;
    int ok = 1;
    if (cn1_geoclue_state) { return cn1_geoclue_state > 0; }
    h = dlopen("libgeoclue-2.so.0", RTLD_LAZY | RTLD_GLOBAL);
    if (!h) { h = dlopen("libgeoclue-2.so", RTLD_LAZY | RTLD_GLOBAL); }
    if (!h) {
        cn1_geoclue_state = -1;
        cn1LinuxStubOnce("libgeoclue (libgeoclue-2) not installed; location unsupported");
        return 0;
    }
#define CN1_GCLUE_SYM(ptr, name) do { *(void**)(&ptr) = dlsym(h, name); if (!(ptr)) { ok = 0; } } while (0)
    CN1_GCLUE_SYM(p_gclue_simple_new_sync, "gclue_simple_new_sync");
    CN1_GCLUE_SYM(p_gclue_simple_get_location, "gclue_simple_get_location");
    CN1_GCLUE_SYM(p_gclue_location_get_latitude, "gclue_location_get_latitude");
    CN1_GCLUE_SYM(p_gclue_location_get_longitude, "gclue_location_get_longitude");
    CN1_GCLUE_SYM(p_gclue_location_get_accuracy, "gclue_location_get_accuracy");
    CN1_GCLUE_SYM(p_gclue_location_get_altitude, "gclue_location_get_altitude");
    CN1_GCLUE_SYM(p_gclue_location_get_heading, "gclue_location_get_heading");
    CN1_GCLUE_SYM(p_gclue_location_get_speed, "gclue_location_get_speed");
#undef CN1_GCLUE_SYM
    cn1_geoclue_state = ok ? 1 : -1;
    if (!ok) { cn1LinuxStubOnce("libgeoclue present but an expected symbol was missing; location unsupported"); }
    return ok;
}

/* ----------------------------------------------------------- clipboard */

/* Every GtkClipboard call below runs on the GTK main thread via
 * cn1LinuxRunOnMainAndWait, never inline on the calling (EDT) thread.
 *
 * This is not defensive style, it is required: the retrieval calls
 * (gtk_clipboard_wait_for_text / _image / _uris) and gtk_clipboard_store all
 * pump a nested main loop until the selection owner answers. Pumping the
 * default GMainContext from a second thread while the GTK thread owns it makes
 * the caller block in g_main_context_wait() for an acquire that only completes
 * when the GTK thread happens to release the context -- so the EDT wedges for
 * good whenever the timing lines up. It usually does not, which is exactly why
 * this presented as an intermittent suite hang (the EDT stack was parked in
 * gtk_clipboard_wait_for_text under ClipboardRoundTripTest).
 *
 * The GTK work therefore happens in the *OnMain helpers over plain C structs;
 * every JAVA_OBJECT conversion stays on the calling thread, matching the file
 * dialog / notification pattern used elsewhere in this file. */

typedef struct { const char* text; } CN1ClipSetText;

static void cn1ClipSetTextOnMain(void* p) {
    CN1ClipSetText* r = (CN1ClipSetText*) p;
    GtkClipboard* cb = gtk_clipboard_get(GDK_SELECTION_CLIPBOARD);
    gtk_clipboard_set_text(cb, r->text, -1);
    gtk_clipboard_store(cb);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_clipboardSetText___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT text) {
    CN1ClipSetText r;
    r.text = text == JAVA_NULL ? "" : stringToUTF8(threadStateData, text);
    cn1LinuxRunOnMainAndWait(cn1ClipSetTextOnMain, &r);
}

typedef struct { gchar* text; } CN1ClipGetText;

static void cn1ClipGetTextOnMain(void* p) {
    CN1ClipGetText* r = (CN1ClipGetText*) p;
    r->text = gtk_clipboard_wait_for_text(gtk_clipboard_get(GDK_SELECTION_CLIPBOARD));
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_clipboardGetText___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    CN1ClipGetText r;
    JAVA_OBJECT result;
    r.text = NULL;
    cn1LinuxRunOnMainAndWait(cn1ClipGetTextOnMain, &r);
    result = r.text ? newStringFromUtf8Len(threadStateData, r.text, (int) strlen(r.text)) : JAVA_NULL;
    if (r.text) {
        g_free(r.text);
    }
    return result;
}

typedef struct { const unsigned char* bytes; int len; } CN1ClipSetImage;

static void cn1ClipSetImageOnMain(void* p) {
    CN1ClipSetImage* r = (CN1ClipSetImage*) p;
    GdkPixbufLoader* loader = gdk_pixbuf_loader_new();
    GdkPixbuf* pix;
    GtkClipboard* cb;
    if (!gdk_pixbuf_loader_write(loader, r->bytes, (gsize) r->len, NULL)) {
        gdk_pixbuf_loader_close(loader, NULL);
        g_object_unref(loader);
        return;
    }
    gdk_pixbuf_loader_close(loader, NULL);
    /* Borrowed reference owned by the loader; do not unref pix directly. */
    pix = gdk_pixbuf_loader_get_pixbuf(loader);
    if (pix) {
        cb = gtk_clipboard_get(GDK_SELECTION_CLIPBOARD);
        gtk_clipboard_set_image(cb, pix);
        gtk_clipboard_store(cb);
    }
    g_object_unref(loader);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_clipboardSetImage___byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT png) {
    CN1ClipSetImage r;
    if (png == JAVA_NULL) {
        return;
    }
    /* The array stays reachable from this (blocked) frame for the whole call. */
    r.bytes = (const unsigned char*) CN1_ARRAY_DATA(png);
    r.len = (int) (*(JAVA_ARRAY) png).length;
    if (r.len <= 0) {
        return;
    }
    cn1LinuxRunOnMainAndWait(cn1ClipSetImageOnMain, &r);
}

typedef struct { gchar* buf; gsize len; } CN1ClipGetImage;

static void cn1ClipGetImageOnMain(void* p) {
    CN1ClipGetImage* r = (CN1ClipGetImage*) p;
    GdkPixbuf* pix = gtk_clipboard_wait_for_image(gtk_clipboard_get(GDK_SELECTION_CLIPBOARD));
    if (!pix) {
        return;
    }
    if (!gdk_pixbuf_save_to_buffer(pix, &r->buf, &r->len, "png", NULL, NULL) || r->buf == NULL) {
        if (r->buf) {
            g_free(r->buf);
            r->buf = NULL;
        }
    }
    g_object_unref(pix);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_clipboardGetImage___R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    CN1ClipGetImage r;
    JAVA_OBJECT result;
    r.buf = NULL;
    r.len = 0;
    cn1LinuxRunOnMainAndWait(cn1ClipGetImageOnMain, &r);
    if (!r.buf) {
        return JAVA_NULL;
    }
    result = cn1LinuxNewByteArray(threadStateData, r.buf, (int) r.len);
    g_free(r.buf);
    return result;
}

/* Holds the NULL-terminated URI list handed to the clipboard; freed by the
 * clear-func when the clipboard content is replaced. */
typedef struct { gchar** uris; } CN1UriListData;

static void cn1UriListGet(GtkClipboard* cb, GtkSelectionData* selection, guint info, gpointer userData) {
    CN1UriListData* d = (CN1UriListData*) userData;
    (void) cb;
    (void) info;
    if (d && d->uris) {
        gtk_selection_data_set_uris(selection, d->uris);
    }
}

static void cn1UriListClear(GtkClipboard* cb, gpointer userData) {
    CN1UriListData* d = (CN1UriListData*) userData;
    (void) cb;
    if (d) {
        if (d->uris) {
            g_strfreev(d->uris);
        }
        g_free(d);
    }
}

/* Takes ownership of the CN1UriListData: either the clipboard holds it (and the
 * clear-func frees it later) or it is released here. */
static void cn1ClipSetFilesOnMain(void* p) {
    CN1UriListData* data = (CN1UriListData*) p;
    GtkClipboard* cb = gtk_clipboard_get(GDK_SELECTION_CLIPBOARD);
    GtkTargetList* tl = gtk_target_list_new(NULL, 0);
    GtkTargetEntry* targets;
    gint nTargets = 0;
    gtk_target_list_add_uri_targets(tl, 0);
    targets = gtk_target_table_new_from_list(tl, &nTargets);
    if (!gtk_clipboard_set_with_data(cb, targets, nTargets, cn1UriListGet, cn1UriListClear, data)) {
        cn1UriListClear(cb, data);
    } else {
        gtk_clipboard_set_can_store(cb, targets, nTargets);
        gtk_clipboard_store(cb);
    }
    if (targets) {
        gtk_target_table_free(targets, nTargets);
    }
    gtk_target_list_unref(tl);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_clipboardSetFiles___java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT paths) {
    int n, i;
    JAVA_OBJECT* elements;
    CN1UriListData* data;
    if (paths == JAVA_NULL) {
        return;
    }
    n = (int) (*(JAVA_ARRAY) paths).length;
    if (n <= 0) {
        return;
    }
    elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(paths);
    data = (CN1UriListData*) g_malloc0(sizeof(CN1UriListData));
    data->uris = (gchar**) g_malloc0(sizeof(gchar*) * (n + 1));
    for (i = 0; i < n; i++) {
        const char* p = elements[i] == JAVA_NULL ? "" : stringToUTF8(threadStateData, elements[i]);
        if (g_str_has_prefix(p, "file:") || strstr(p, "://") != NULL) {
            data->uris[i] = g_strdup(p);
        } else {
            gchar* uri = g_filename_to_uri(p, NULL, NULL);
            data->uris[i] = uri ? uri : g_strdup(p);
        }
    }
    data->uris[n] = NULL;

    cn1LinuxRunOnMainAndWait(cn1ClipSetFilesOnMain, data);
}

typedef struct { gchar** uris; } CN1ClipGetFiles;

static void cn1ClipGetFilesOnMain(void* p) {
    CN1ClipGetFiles* r = (CN1ClipGetFiles*) p;
    r->uris = gtk_clipboard_wait_for_uris(gtk_clipboard_get(GDK_SELECTION_CLIPBOARD));
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_clipboardGetFiles___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    CN1ClipGetFiles r;
    gchar** uris;
    int n = 0;
    int i;
    JAVA_OBJECT arr;
    JAVA_OBJECT* elements;
    r.uris = NULL;
    cn1LinuxRunOnMainAndWait(cn1ClipGetFilesOnMain, &r);
    uris = r.uris;
    if (!uris) {
        return JAVA_NULL;
    }
    while (uris[n] != NULL) {
        n++;
    }
    arr = allocArray(threadStateData, n, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    if (arr != JAVA_NULL) {
        elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
        for (i = 0; i < n; i++) {
            JAVA_OBJECT cn1__s = newStringFromCString(threadStateData, uris[i]);
            CN1_WRITE_BARRIER(arr, cn1__s);  /* each allocation is a safepoint: arr may be old */
            elements[i] = cn1__s;
        }
    }
    g_strfreev(uris);
    return arr;
}

/* ------------------------------------------------------ shell / launch */

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_shellOpen___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT target) {
    const char* t = target == JAVA_NULL ? 0 : stringToUTF8(threadStateData, target);
    GError* err = 0;
    gboolean ok;
    char* uri;
    if (!t) {
        return JAVA_FALSE;
    }
    /* A bare filesystem path needs a file: URI; a scheme (http:, mailto:, tel:,
     * sms:) is launched as-is by the registered default handler. */
    if (t[0] == '/') {
        uri = g_strconcat("file://", t, NULL);
    } else {
        uri = g_strdup(t);
    }
    ok = g_app_info_launch_default_for_uri(uri, NULL, &err);
    g_free(uri);
    if (err) {
        g_error_free(err);
    }
    return ok ? JAVA_TRUE : JAVA_FALSE;
}

/* --------------------------------------------------- local notifications */

static int cn1NotifyInited = 0;
static char cn1ClickedNotification[512];
static pthread_mutex_t cn1NotifyLock = PTHREAD_MUTEX_INITIALIZER;

static void cn1NotifyAction(NotifyNotification* n, char* action, gpointer userData) {
    (void) n;
    (void) action;
    pthread_mutex_lock(&cn1NotifyLock);
    strncpy(cn1ClickedNotification, (const char*) userData, sizeof(cn1ClickedNotification) - 1);
    cn1ClickedNotification[sizeof(cn1ClickedNotification) - 1] = 0;
    pthread_mutex_unlock(&cn1NotifyLock);
}

typedef struct { const char* id; const char* title; const char* body; } CN1NotifyReq;

static void cn1ShowNotifyOnMain(void* p) {
    CN1NotifyReq* r = (CN1NotifyReq*) p;
    NotifyNotification* n;
    if (!cn1NotifyInited) {
        p_notify_init("Codename One");
        cn1NotifyInited = 1;
    }
    n = p_notify_notification_new(r->title ? r->title : "", r->body ? r->body : "", 0);
    /* "default" action fires when the body is clicked (servers that support it). */
    p_notify_notification_add_action(n, "default", "Open", (NotifyActionCallback) cn1NotifyAction,
            g_strdup(r->id ? r->id : ""), g_free);
    p_notify_notification_show(n, 0);
    g_object_unref(n);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_showNotification___java_lang_String_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT id, JAVA_OBJECT title, JAVA_OBJECT body) {
    CN1NotifyReq r;
    if (!cn1LoadNotify()) {
        return;
    }
    /* Copy each: stringToUTF8 reuses one buffer per thread, so converting the
     * title would otherwise repoint the id at it, and the body at both. */
    char* idCopy = cn1LinuxJStrDup(threadStateData, id);
    char* titleCopy = cn1LinuxJStrDup(threadStateData, title);
    char* bodyCopy = cn1LinuxJStrDup(threadStateData, body);
    r.id = idCopy == 0 ? "" : idCopy;
    r.title = titleCopy == 0 ? "" : titleCopy;
    r.body = bodyCopy == 0 ? "" : bodyCopy;
    cn1LinuxRunOnMainAndWait(cn1ShowNotifyOnMain, &r);
    free(idCopy);
    free(titleCopy);
    free(bodyCopy);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_notificationPollClicked___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    JAVA_OBJECT result = JAVA_NULL;
    pthread_mutex_lock(&cn1NotifyLock);
    if (cn1ClickedNotification[0] != 0) {
        result = newStringFromCString(threadStateData, cn1ClickedNotification);
        cn1ClickedNotification[0] = 0;
    }
    pthread_mutex_unlock(&cn1NotifyLock);
    return result;
}

/* ------------------------------------------------------ secure storage
 * libsecret is a key->secret store, not a blob-encrypt primitive, so the
 * Windows DPAPI protect/unprotect contract is met by storing the bytes in the
 * Secret Service under a random token and returning the token as the
 * "ciphertext": the secret never leaves the keyring; SecureStorage persists only
 * the opaque token. */

static const SecretSchema cn1SecretSchema = {
    "com.codename1.SecureStorage", SECRET_SCHEMA_NONE,
    { { "token", SECRET_SCHEMA_ATTRIBUTE_STRING }, { "NULL", 0 } }
};

/* 0 = untried, 1 = the service answered. A negative is never cached: a session whose D-Bus or
 * keyring comes up later must be able to start working without a restart. */
static int cn1_secret_service_state = 0;

/*
 * Whether a Secret Service is actually REACHABLE, which is not the same question as whether
 * libsecret is installed.
 *
 * Loading the library and resolving its symbols says nothing about the session behind it: on a
 * headless machine, or one with a broken D-Bus or no keyring daemon, libsecret loads perfectly
 * and secret_password_store_sync still fails -- so reporting availability from cn1LoadSecret
 * alone advertised a store whose every write returns null, which is the same defect one layer up
 * from answering it out of ordinary CN1 storage.
 *
 * The probe is a LOOKUP, for a token that cannot exist. That reaches the service over D-Bus, and
 * it is the only one of the four symbols this port resolves that both contacts the service and
 * stores nothing -- dpapiProtect answers the same question by writing a secret into the user's
 * keyring, which is no way to answer a capability query. A miss and an unreachable service both
 * return NULL, so the GError is what separates them: set means the call could not be made, unset
 * means the service answered and had nothing.
 *
 * No unlock prompt: the collection is unlocked per matching item, and a token that matches
 * nothing gives libsecret nothing to unlock.
 */
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_secretServiceAvailable___R_boolean(CODENAME_ONE_THREAD_STATE) {
    GError* err = 0;
    gchar* found;
    if (!cn1LoadSecret()) {
        return JAVA_FALSE;
    }
    if (cn1_secret_service_state > 0) {
        return JAVA_TRUE;
    }
    found = p_secret_password_lookup_sync(&cn1SecretSchema, 0, &err,
            "token", "cn1-secret-service-probe", NULL);
    if (found) {
        /* Cannot happen with this token, and freed rather than leaked if it ever does. */
        p_secret_password_free(found);
    }
    if (err) {
        g_error_free(err);
        return JAVA_FALSE;
    }
    cn1_secret_service_state = 1;
    return JAVA_TRUE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_dpapiProtect___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char* bytes;
    int len;
    gchar* b64;
    gchar* token;
    GError* err = 0;
    JAVA_OBJECT result;
    if (data == JAVA_NULL || !cn1LoadSecret()) {
        return JAVA_NULL;
    }
    bytes = (unsigned char*) CN1_ARRAY_DATA(data);
    len = (int) (*(JAVA_ARRAY) data).length;
    b64 = g_base64_encode(bytes, len);
    token = g_uuid_string_random();
    if (!p_secret_password_store_sync(&cn1SecretSchema, SECRET_COLLECTION_DEFAULT,
            "Codename One Secure Storage", b64, 0, &err, "token", token, NULL)) {
        if (err) {
            g_error_free(err);
        }
        g_free(b64);
        g_free(token);
        return JAVA_NULL;
    }
    g_free(b64);
    result = cn1LinuxNewByteArray(threadStateData, token, (int) strlen(token));
    g_free(token);
    return result;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_dpapiUnprotect___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char* tokenBytes;
    int len;
    char token[128];
    gchar* b64;
    GError* err = 0;
    guchar* decoded;
    gsize decodedLen = 0;
    JAVA_OBJECT result;
    if (data == JAVA_NULL || !cn1LoadSecret()) {
        return JAVA_NULL;
    }
    tokenBytes = (unsigned char*) CN1_ARRAY_DATA(data);
    len = (int) (*(JAVA_ARRAY) data).length;
    if (len <= 0 || len >= (int) sizeof(token)) {
        return JAVA_NULL;
    }
    memcpy(token, tokenBytes, len);
    token[len] = 0;
    b64 = p_secret_password_lookup_sync(&cn1SecretSchema, 0, &err, "token", token, NULL);
    if (err) {
        g_error_free(err);
    }
    if (!b64) {
        return JAVA_NULL;
    }
    decoded = g_base64_decode(b64, &decodedLen);
    p_secret_password_free(b64);
    result = cn1LinuxNewByteArray(threadStateData, decoded, (int) decodedLen);
    g_free(decoded);
    return result;
}

/*
 * Removes the keyring entry a token names.
 *
 * The blob this port hands back from dpapiProtect is not the secret: the secret is in the Secret
 * Service and the blob is the token it is filed under. So deleting the stored blob makes the
 * secret unreachable through this API while leaving it in the user's keyring -- which for
 * forgetting a database key is the difference between erased and merely hidden, and leaves an
 * orphan behind for every key ever forgotten.
 */
JAVA_INT com_codename1_impl_linux_LinuxNative_dpapiForget___byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char* tokenBytes;
    int len;
    char token[128];
    GError* err = 0;
    gboolean cleared;
    /* -1 could not, 0 nothing to remove, 1 removed. Three answers rather than two because the
     * caller has to tell "the keyring refused" from "there was nothing there": the first must keep
     * the token, since the token is the only way to find the entry again, and the second must not,
     * since keeping it would leave a token naming nothing. Reporting both as false deleted the
     * token in the refusal case and put the orphan back. */
    if (data == JAVA_NULL || !cn1LoadSecret()) {
        return -1;
    }
    tokenBytes = (unsigned char*) CN1_ARRAY_DATA(data);
    len = (int) (*(JAVA_ARRAY) data).length;
    if (len <= 0 || len >= (int) sizeof(token)) {
        return -1;
    }
    memcpy(token, tokenBytes, len);
    token[len] = 0;
    cleared = p_secret_password_clear_sync(&cn1SecretSchema, 0, &err, "token", token, NULL);
    if (err) {
        g_error_free(err);
        return -1;
    }
    return cleared ? 1 : 0;
}

/* ----------------------------------------------------------- file dialog */

typedef struct { int save; int type; const char* title; char result[4096]; int got; } CN1FileDlg;

static void cn1FileDialogOnMain(void* p) {
    CN1FileDlg* d = (CN1FileDlg*) p;
    GtkWidget* dialog = gtk_file_chooser_dialog_new(
            d->title ? d->title : (d->save ? "Save" : "Open"),
            GTK_WINDOW(cn1LinuxWindowWidget()),
            d->save ? GTK_FILE_CHOOSER_ACTION_SAVE : GTK_FILE_CHOOSER_ACTION_OPEN,
            "_Cancel", GTK_RESPONSE_CANCEL,
            d->save ? "_Save" : "_Open", GTK_RESPONSE_ACCEPT, NULL);
    /* type: 0 image, 1 video, 2 all -- apply a coarse filter. */
    if (d->type == 0 || d->type == 1) {
        GtkFileFilter* f = gtk_file_filter_new();
        gtk_file_filter_add_mime_type(f, d->type == 0 ? "image/*" : "video/*");
        gtk_file_chooser_add_filter(GTK_FILE_CHOOSER(dialog), f);
    }
    if (gtk_dialog_run(GTK_DIALOG(dialog)) == GTK_RESPONSE_ACCEPT) {
        char* name = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(dialog));
        if (name) {
            strncpy(d->result, name, sizeof(d->result) - 1);
            d->result[sizeof(d->result) - 1] = 0;
            d->got = 1;
            g_free(name);
        }
    }
    gtk_widget_destroy(dialog);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_fileDialog___boolean_int_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_BOOLEAN save, JAVA_INT type, JAVA_OBJECT title) {
    CN1FileDlg d;
    d.save = save ? 1 : 0;
    d.type = type;
    d.title = title == JAVA_NULL ? 0 : stringToUTF8(threadStateData, title);
    d.got = 0;
    d.result[0] = 0;
    if (cn1LinuxWindowWidget() == 0) {
        return JAVA_NULL; /* headless */
    }
    cn1LinuxRunOnMainAndWait(cn1FileDialogOnMain, &d);
    return d.got ? newStringFromCString(threadStateData, d.result) : JAVA_NULL;
}

/* ------------------------------------------------- rich open/save dialog
 * The dialog behind Display.showOpenDialog/showSaveDialog. Every Java string is
 * copied into C memory before the hop to the GTK thread, because that thread
 * must not touch the Java heap; the answer comes back the same way and is only
 * turned into Java strings on the calling thread.
 *
 * filters[i] is "label\next1\next2...", options[i] is
 * "type\nid\nlabel\ndefault\nchoice1\nchoice2..." (type 1 checkbox, 2 choice,
 * 3 text). The answer is null on cancel, else
 * [selected filter index, one value per option, path...]. */

#define CN1_FDX_MAX_ITEMS 64

typedef struct {
    int save;
    int multi;
    int dirs;
    int confirmOverwrite;
    int showHidden;
    char* title;
    char* acceptLabel;
    char* initialDir;
    char* suggestedName;
    int filterCount;
    char* filters[CN1_FDX_MAX_ITEMS];
    int selectedFilter;
    int optionCount;
    char* options[CN1_FDX_MAX_ITEMS];
    /* answer */
    int accepted;
    int chosenFilter;
    char* values[CN1_FDX_MAX_ITEMS];
    GSList* paths;
} CN1FileDlgEx;

static char* cn1FdxDup(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    const char* c;
    if (s == JAVA_NULL) {
        return 0;
    }
    c = stringToUTF8(threadStateData, s);
    return c ? strdup(c) : 0;
}

static int cn1FdxDupArray(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr, char** out) {
    int n, i;
    JAVA_OBJECT* elements;
    if (arr == JAVA_NULL) {
        return 0;
    }
    n = (int) (*(JAVA_ARRAY) arr).length;
    if (n > CN1_FDX_MAX_ITEMS) {
        n = CN1_FDX_MAX_ITEMS;
    }
    elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
    for (i = 0; i < n; i++) {
        out[i] = cn1FdxDup(threadStateData, elements[i]);
    }
    return n;
}

/* Splits a "\n" separated spec in place; returns the number of fields. */
static int cn1FdxSplit(char* spec, char** fields, int max) {
    int n = 0;
    char* p = spec;
    if (!spec) {
        return 0;
    }
    while (n < max) {
        char* nl = strchr(p, '\n');
        fields[n++] = p;
        if (!nl) {
            break;
        }
        *nl = 0;
        p = nl + 1;
    }
    return n;
}

/* Appends the filter's first extension to a save path that carries none of
 * its extensions, the same rule as FileDialogSupport.withExtension, so the
 * overwrite check sees the file that will really be written. The filter
 * string was already split by cn1FdxSplit into n NUL separated fields: the
 * label, then the extensions. Returns 1 when the path was changed. */
static int cn1FdxAddExtension(GSList* paths, const char* filter, int n) {
    const char* path = (const char*) paths->data;
    const char* field = filter + strlen(filter) + 1;
    const char* first = 0;
    size_t len = strlen(path);
    int j;
    for (j = 1; j < n; j++) {
        size_t el = strlen(field);
        if (strcmp(field, "*") == 0) {
            /* the all-files filter accepts any name as typed */
            return 0;
        }
        if (!first) {
            first = field;
        }
        /* >= so a dotfile such as ".gitignore" already counts as carrying it */
        if (len >= el + 1 && path[len - el - 1] == '.'
                && g_ascii_strcasecmp(path + len - el, field) == 0) {
            return 0;
        }
        field += el + 1;
    }
    if (!first || !*first) {
        return 0;
    }
    paths->data = g_strconcat(path, ".", first, NULL);
    g_free((gpointer) path);
    return 1;
}

static int cn1FdxConfirmReplace(GtkWidget* parent, const char* path) {
    GtkWidget* q;
    int answer;
    gchar* base = g_path_get_basename(path);
    q = gtk_message_dialog_new(GTK_WINDOW(parent), GTK_DIALOG_MODAL,
            GTK_MESSAGE_QUESTION, GTK_BUTTONS_YES_NO,
            "A file named \"%s\" already exists. Do you want to replace it?", base);
    answer = gtk_dialog_run(GTK_DIALOG(q));
    gtk_widget_destroy(q);
    g_free(base);
    return answer == GTK_RESPONSE_YES;
}

static void cn1FileDialogExOnMain(void* p) {
    CN1FileDlgEx* d = (CN1FileDlgEx*) p;
    GtkFileChooserAction action;
    GtkWidget* dialog;
    GtkWidget* extra = 0;
    GtkWidget* entries[CN1_FDX_MAX_ITEMS];
    GtkFileFilter* filters[CN1_FDX_MAX_ITEMS];
    int filterFields[CN1_FDX_MAX_ITEMS];
    int i;
    if (d->save) {
        action = GTK_FILE_CHOOSER_ACTION_SAVE;
    } else {
        action = d->dirs ? GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER : GTK_FILE_CHOOSER_ACTION_OPEN;
    }
    dialog = gtk_file_chooser_dialog_new(
            d->title ? d->title : (d->save ? "Save" : "Open"),
            GTK_WINDOW(cn1LinuxWindowWidget()), action,
            "_Cancel", GTK_RESPONSE_CANCEL,
            d->acceptLabel ? d->acceptLabel : (d->save ? "_Save" : "_Open"), GTK_RESPONSE_ACCEPT, NULL);
    gtk_file_chooser_set_select_multiple(GTK_FILE_CHOOSER(dialog), d->multi && !d->save);
    gtk_file_chooser_set_show_hidden(GTK_FILE_CHOOSER(dialog), d->showHidden);
    /* GTK would confirm the typed name, before cn1FdxAddExtension changes it,
     * and so warn about the wrong file (or miss the real one). The loop below
     * confirms the final path itself instead. */
    gtk_file_chooser_set_do_overwrite_confirmation(GTK_FILE_CHOOSER(dialog), FALSE);
    if (d->initialDir) {
        gtk_file_chooser_set_current_folder(GTK_FILE_CHOOSER(dialog), d->initialDir);
    }
    if (d->save && d->suggestedName) {
        gtk_file_chooser_set_current_name(GTK_FILE_CHOOSER(dialog), d->suggestedName);
    }
    for (i = 0; i < d->filterCount; i++) {
        char* fields[CN1_FDX_MAX_ITEMS];
        int n = cn1FdxSplit(d->filters[i], fields, CN1_FDX_MAX_ITEMS);
        int j;
        filterFields[i] = n;
        filters[i] = gtk_file_filter_new();
        gtk_file_filter_set_name(filters[i], n > 0 ? fields[0] : "");
        for (j = 1; j < n; j++) {
            if (strcmp(fields[j], "*") == 0) {
                /* the all-files extension: "*.*" would hide README */
                gtk_file_filter_add_pattern(filters[i], "*");
                continue;
            }
            /* case-insensitive glob, so "png" also matches IMG.PNG */
            char glob[256];
            int k, o = 0;
            glob[o++] = '*';
            glob[o++] = '.';
            for (k = 0; fields[j][k] && o < (int) sizeof(glob) - 5; k++) {
                char c = fields[j][k];
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    glob[o++] = '[';
                    glob[o++] = (char) (c | 0x20);
                    glob[o++] = (char) (c & ~0x20);
                    glob[o++] = ']';
                } else {
                    glob[o++] = c;
                }
            }
            glob[o] = 0;
            gtk_file_filter_add_pattern(filters[i], glob);
        }
        if (n <= 1) {
            gtk_file_filter_add_pattern(filters[i], "*");
        }
        gtk_file_chooser_add_filter(GTK_FILE_CHOOSER(dialog), filters[i]);
    }
    if (d->filterCount > 0 && d->selectedFilter >= 0 && d->selectedFilter < d->filterCount) {
        gtk_file_chooser_set_filter(GTK_FILE_CHOOSER(dialog), filters[d->selectedFilter]);
    }
    /* Checkboxes and choices are GtkFileChooser choices, which the desktop
     * portal can show too; a text option has no choice form and goes into the
     * extra widget. */
    for (i = 0; i < d->optionCount; i++) {
        /* Sized from the spec itself: a choice option may carry any number of
         * choices, and a fixed array would silently drop the extra ones. */
        int max = 1;
        const char* c;
        char** fields;
        int n;
        for (c = d->options[i]; c && *c; c++) {
            if (*c == '\n') {
                max++;
            }
        }
        fields = g_new0(char*, max);
        n = cn1FdxSplit(d->options[i], fields, max);
        entries[i] = 0;
        if (n < 4) {
            g_free(fields);
            continue;
        }
        if (fields[0][0] == '1') {
            gtk_file_chooser_add_choice(GTK_FILE_CHOOSER(dialog), fields[1], fields[2], NULL, NULL);
            gtk_file_chooser_set_choice(GTK_FILE_CHOOSER(dialog), fields[1], fields[3]);
        } else if (fields[0][0] == '2') {
            const char** ids = g_new0(const char*, n - 3);
            int k;
            for (k = 4; k < n; k++) {
                ids[k - 4] = fields[k];
            }
            ids[n - 4] = NULL;
            gtk_file_chooser_add_choice(GTK_FILE_CHOOSER(dialog), fields[1], fields[2], ids, ids);
            gtk_file_chooser_set_choice(GTK_FILE_CHOOSER(dialog), fields[1], fields[3]);
            g_free(ids);
        } else {
            GtkWidget* label = gtk_label_new(fields[2]);
            if (!extra) {
                extra = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 6);
            }
            entries[i] = gtk_entry_new();
            gtk_entry_set_text(GTK_ENTRY(entries[i]), fields[3]);
            gtk_box_pack_start(GTK_BOX(extra), label, FALSE, FALSE, 0);
            gtk_box_pack_start(GTK_BOX(extra), entries[i], FALSE, FALSE, 0);
        }
        g_free(fields);
    }
    if (extra) {
        gtk_widget_show_all(extra);
        gtk_file_chooser_set_extra_widget(GTK_FILE_CHOOSER(dialog), extra);
    }
    while (gtk_dialog_run(GTK_DIALOG(dialog)) == GTK_RESPONSE_ACCEPT) {
        GtkFileFilter* chosen = gtk_file_chooser_get_filter(GTK_FILE_CHOOSER(dialog));
        d->accepted = 1;
        d->chosenFilter = -1;
        for (i = 0; i < d->filterCount; i++) {
            if (filters[i] == chosen) {
                d->chosenFilter = i;
            }
        }
        for (i = 0; i < d->optionCount; i++) {
            const char* v = 0;
            if (entries[i]) {
                v = gtk_entry_get_text(GTK_ENTRY(entries[i]));
            } else if (d->options[i]) {
                /* cn1FdxSplit left the id as the second field */
                const char* id = d->options[i] + strlen(d->options[i]) + 1;
                v = gtk_file_chooser_get_choice(GTK_FILE_CHOOSER(dialog), id);
            }
            d->values[i] = v ? strdup(v) : 0;
        }
        d->paths = gtk_file_chooser_get_filenames(GTK_FILE_CHOOSER(dialog));
        if (d->save && d->paths && d->chosenFilter >= 0) {
            cn1FdxAddExtension(d->paths, d->filters[d->chosenFilter], filterFields[d->chosenFilter]);
        }
        if (d->save && d->paths && d->confirmOverwrite
                && g_file_test((const char*) d->paths->data, G_FILE_TEST_EXISTS)
                && !cn1FdxConfirmReplace(dialog, (const char*) d->paths->data)) {
            /* confirmed against the final, extension-adjusted path; the user
             * declined replacing it, so the dialog is shown again */
            for (i = 0; i < d->optionCount; i++) {
                free(d->values[i]);
                d->values[i] = 0;
            }
            g_slist_free_full(d->paths, g_free);
            d->paths = 0;
            d->accepted = 0;
            continue;
        }
        break;
    }
    gtk_widget_destroy(dialog);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_fileDialogEx___boolean_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_1ARRAY_int_boolean_boolean_boolean_boolean_java_lang_String_1ARRAY_R_java_lang_String_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_BOOLEAN save, JAVA_OBJECT title, JAVA_OBJECT acceptLabel,
        JAVA_OBJECT initialDir, JAVA_OBJECT suggestedName, JAVA_OBJECT filters, JAVA_INT selectedFilter,
        JAVA_BOOLEAN multi, JAVA_BOOLEAN dirs, JAVA_BOOLEAN confirmOverwrite, JAVA_BOOLEAN showHidden,
        JAVA_OBJECT options) {
    CN1FileDlgEx d;
    JAVA_OBJECT arr = JAVA_NULL;
    JAVA_OBJECT* elements;
    GSList* it;
    int count, i, o;
    char num[16];
    memset(&d, 0, sizeof(d));
    if (cn1LinuxWindowWidget() == 0) {
        return JAVA_NULL; /* headless */
    }
    d.save = save ? 1 : 0;
    d.multi = multi ? 1 : 0;
    d.dirs = dirs ? 1 : 0;
    d.confirmOverwrite = confirmOverwrite ? 1 : 0;
    d.showHidden = showHidden ? 1 : 0;
    d.selectedFilter = selectedFilter;
    d.title = cn1FdxDup(threadStateData, title);
    d.acceptLabel = cn1FdxDup(threadStateData, acceptLabel);
    d.initialDir = cn1FdxDup(threadStateData, initialDir);
    d.suggestedName = cn1FdxDup(threadStateData, suggestedName);
    d.filterCount = cn1FdxDupArray(threadStateData, filters, d.filters);
    d.optionCount = cn1FdxDupArray(threadStateData, options, d.options);
    cn1LinuxRunOnMainAndWait(cn1FileDialogExOnMain, &d);
    if (d.accepted && d.paths) {
        count = 1 + d.optionCount + (int) g_slist_length(d.paths);
        arr = allocArray(threadStateData, count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
        if (arr != JAVA_NULL) {
            JAVA_OBJECT s;
            o = 0;
            snprintf(num, sizeof(num), "%d", d.chosenFilter);
            s = newStringFromCString(threadStateData, num);
            elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
            CN1_WRITE_BARRIER(arr, s);
            elements[o++] = s;
            for (i = 0; i < d.optionCount; i++) {
                s = d.values[i] ? newStringFromCString(threadStateData, d.values[i]) : JAVA_NULL;
                elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
                CN1_WRITE_BARRIER(arr, s);
                elements[o++] = s;
            }
            for (it = d.paths; it; it = it->next) {
                s = newStringFromCString(threadStateData, (const char*) it->data);
                elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
                CN1_WRITE_BARRIER(arr, s);
                elements[o++] = s;
            }
        }
    }
    if (d.paths) {
        g_slist_free_full(d.paths, g_free);
    }
    free(d.title);
    free(d.acceptLabel);
    free(d.initialDir);
    free(d.suggestedName);
    for (i = 0; i < d.filterCount; i++) {
        free(d.filters[i]);
    }
    for (i = 0; i < d.optionCount; i++) {
        free(d.options[i]);
        free(d.values[i]);
    }
    return arr;
}

/* ------------------------------------------------------------- location */

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_locationGetCurrent___double_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    /* GeoClue 2 via libgeoclue: a synchronous one-shot fix. Reached lazily through
     * a GDBus proxy resolved by name so a build without the geoclue headers still
     * links (the symbols are weak-declared below). */
    extern int cn1LinuxGeoclueFix(double* out6);
    double fix[6];
    JAVA_DOUBLE* arr;
    int i;
    if (out == JAVA_NULL || !cn1LoadGeoclue()) {
        return JAVA_FALSE;
    }
    if (!cn1LinuxGeoclueFix(fix)) {
        return JAVA_FALSE;
    }
    arr = (JAVA_DOUBLE*) CN1_ARRAY_DATA(out);
    for (i = 0; i < 6 && i < (int) (*(JAVA_ARRAY) out).length; i++) {
        arr[i] = fix[i];
    }
    return JAVA_TRUE;
}

/* ----------------------------------------------------- share / contacts */

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_shareText___java_lang_String_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT text, JAVA_OBJECT title) {
    /* No universal share sheet on the Linux desktop (xdg-desktop-portal's Share
     * is not yet broadly available); fall back to composing a mail draft via the
     * default mailto handler, which is the closest portable "share". */
    /* Copy the text before converting the title -- they share one buffer. */
    char* t = cn1LinuxJStrDup(threadStateData, text);
    char* subj = cn1LinuxJStrDup(threadStateData, title);
    char* body = g_uri_escape_string(t == 0 ? "" : t, NULL, FALSE);
    char* s = g_uri_escape_string(subj == 0 ? "" : subj, NULL, FALSE);
    char* uri = g_strconcat("mailto:?subject=", s, "&body=", body, NULL);
    gboolean ok = g_app_info_launch_default_for_uri(uri, NULL, NULL);
    g_free(body);
    g_free(s);
    g_free(uri);
    free(t);
    free(subj);
    return ok ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_contactsGetAll___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    /* The Linux desktop has no standard contacts store reachable without
     * Evolution Data Server (optional); report "no contacts" (empty) rather than
     * "inaccessible" (null). */
    return newStringFromCString(threadStateData, "");
}

/* ------------------------------------------------------------ biometrics */

JAVA_INT com_codename1_impl_linux_LinuxNative_biometricAvailability___R_int(CODENAME_ONE_THREAD_STATE) {
    /* 0 Available, 1 DeviceNotPresent. Probe fprintd on the system bus and ask for
     * a default device; absence -> not present. */
    GError* err = 0;
    GDBusConnection* bus = g_bus_get_sync(G_BUS_TYPE_SYSTEM, 0, &err);
    GVariant* res;
    int available = 1;
    if (!bus) {
        if (err) g_error_free(err);
        return 1;
    }
    res = g_dbus_connection_call_sync(bus, "net.reactivated.Fprint",
            "/net/reactivated/Fprint/Manager", "net.reactivated.Fprint.Manager",
            "GetDefaultDevice", 0, G_VARIANT_TYPE("(o)"),
            G_DBUS_CALL_FLAGS_NONE, 2000, 0, &err);
    if (res) {
        available = 0;
        g_variant_unref(res);
    } else if (err) {
        g_error_free(err);
    }
    g_object_unref(bus);
    return available;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_biometricAuthenticate___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT message) {
    /* A full fprintd Claim/VerifyStart/VerifyStop signal flow is involved; until
     * that is wired, report not-verified honestly rather than fabricating a pass. */
    (void) message;
    cn1LinuxStubOnce("biometricAuthenticate (fprintd verify flow pending)");
    return JAVA_FALSE;
}

/* GeoClue 2 one-shot fix, called by locationGetCurrent. out6 = {lat, lon,
 * accuracy(m), altitude(m), heading(deg), speed(m/s)}. Returns 1 on a fix. */
int cn1LinuxGeoclueFix(double* out6) {
    GError* err = 0;
    GClueSimple* simple;
    GClueLocation* loc;
    if (!cn1LoadGeoclue()) {
        return 0;
    }
    simple = p_gclue_simple_new_sync("codenameone", GCLUE_ACCURACY_LEVEL_EXACT, 0, &err);
    if (!simple) {
        if (err) {
            g_error_free(err);
        }
        return 0;
    }
    loc = p_gclue_simple_get_location(simple);
    if (!loc) {
        g_object_unref(simple);
        return 0;
    }
    out6[0] = p_gclue_location_get_latitude(loc);
    out6[1] = p_gclue_location_get_longitude(loc);
    out6[2] = p_gclue_location_get_accuracy(loc);
    out6[3] = p_gclue_location_get_altitude(loc);
    out6[4] = p_gclue_location_get_heading(loc);
    out6[5] = p_gclue_location_get_speed(loc);
    g_object_unref(simple);
    return 1;
}
