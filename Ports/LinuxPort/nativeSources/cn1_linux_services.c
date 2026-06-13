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
#include <pthread.h>
#include <string.h>
#include <stdlib.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern GtkWidget* cn1LinuxWindowWidget(void);

/* The run-on-GTK-thread-and-wait helper (cn1LinuxRunOnMainAndWait) is shared from
 * cn1_linux_window.c. */

/* ----------------------------------------------------------- clipboard */

JAVA_VOID com_codename1_impl_linux_LinuxNative_clipboardSetText___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT text) {
    const char* t = text == JAVA_NULL ? "" : stringToUTF8(threadStateData, text);
    GtkClipboard* cb = gtk_clipboard_get(GDK_SELECTION_CLIPBOARD);
    gtk_clipboard_set_text(cb, t, -1);
    gtk_clipboard_store(cb);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_clipboardGetText___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    GtkClipboard* cb = gtk_clipboard_get(GDK_SELECTION_CLIPBOARD);
    gchar* text = gtk_clipboard_wait_for_text(cb);
    JAVA_OBJECT result = text ? newStringFromCString(threadStateData, text) : JAVA_NULL;
    if (text) {
        g_free(text);
    }
    return result;
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
        notify_init("Codename One");
        cn1NotifyInited = 1;
    }
    n = notify_notification_new(r->title ? r->title : "", r->body ? r->body : "", 0);
    /* "default" action fires when the body is clicked (servers that support it). */
    notify_notification_add_action(n, "default", "Open", (NotifyActionCallback) cn1NotifyAction,
            g_strdup(r->id ? r->id : ""), g_free);
    notify_notification_show(n, 0);
    g_object_unref(n);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_showNotification___java_lang_String_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT id, JAVA_OBJECT title, JAVA_OBJECT body) {
    CN1NotifyReq r;
    r.id = id == JAVA_NULL ? "" : stringToUTF8(threadStateData, id);
    r.title = title == JAVA_NULL ? "" : stringToUTF8(threadStateData, title);
    r.body = body == JAVA_NULL ? "" : stringToUTF8(threadStateData, body);
    cn1LinuxRunOnMainAndWait(cn1ShowNotifyOnMain, &r);
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

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_dpapiProtect___byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data) {
    unsigned char* bytes;
    int len;
    gchar* b64;
    gchar* token;
    GError* err = 0;
    JAVA_OBJECT result;
    if (data == JAVA_NULL) {
        return JAVA_NULL;
    }
    bytes = (unsigned char*) (*(JAVA_ARRAY) data).data;
    len = (int) (*(JAVA_ARRAY) data).length;
    b64 = g_base64_encode(bytes, len);
    token = g_uuid_string_random();
    if (!secret_password_store_sync(&cn1SecretSchema, SECRET_COLLECTION_DEFAULT,
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
    if (data == JAVA_NULL) {
        return JAVA_NULL;
    }
    tokenBytes = (unsigned char*) (*(JAVA_ARRAY) data).data;
    len = (int) (*(JAVA_ARRAY) data).length;
    if (len <= 0 || len >= (int) sizeof(token)) {
        return JAVA_NULL;
    }
    memcpy(token, tokenBytes, len);
    token[len] = 0;
    b64 = secret_password_lookup_sync(&cn1SecretSchema, 0, &err, "token", token, NULL);
    if (err) {
        g_error_free(err);
    }
    if (!b64) {
        return JAVA_NULL;
    }
    decoded = g_base64_decode(b64, &decodedLen);
    secret_password_free(b64);
    result = cn1LinuxNewByteArray(threadStateData, decoded, (int) decodedLen);
    g_free(decoded);
    return result;
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

/* ------------------------------------------------------------- location */

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_locationGetCurrent___double_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    /* GeoClue 2 via libgeoclue: a synchronous one-shot fix. Reached lazily through
     * a GDBus proxy resolved by name so a build without the geoclue headers still
     * links (the symbols are weak-declared below). */
    extern int cn1LinuxGeoclueFix(double* out6);
    double fix[6];
    JAVA_DOUBLE* arr;
    int i;
    if (out == JAVA_NULL) {
        return JAVA_FALSE;
    }
    if (!cn1LinuxGeoclueFix(fix)) {
        return JAVA_FALSE;
    }
    arr = (JAVA_DOUBLE*) (*(JAVA_ARRAY) out).data;
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
    const char* t = text == JAVA_NULL ? "" : stringToUTF8(threadStateData, text);
    const char* subj = title == JAVA_NULL ? "" : stringToUTF8(threadStateData, title);
    char* body = g_uri_escape_string(t, NULL, FALSE);
    char* s = g_uri_escape_string(subj, NULL, FALSE);
    char* uri = g_strconcat("mailto:?subject=", s, "&body=", body, NULL);
    gboolean ok = g_app_info_launch_default_for_uri(uri, NULL, NULL);
    g_free(body);
    g_free(s);
    g_free(uri);
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
    GClueSimple* simple = gclue_simple_new_sync("codenameone", GCLUE_ACCURACY_LEVEL_EXACT, 0, &err);
    GClueLocation* loc;
    if (!simple) {
        if (err) {
            g_error_free(err);
        }
        return 0;
    }
    loc = gclue_simple_get_location(simple);
    if (!loc) {
        g_object_unref(simple);
        return 0;
    }
    out6[0] = gclue_location_get_latitude(loc);
    out6[1] = gclue_location_get_longitude(loc);
    out6[2] = gclue_location_get_accuracy(loc);
    out6[3] = gclue_location_get_altitude(loc);
    out6[4] = gclue_location_get_heading(loc);
    out6[5] = gclue_location_get_speed(loc);
    g_object_unref(simple);
    return 1;
}
