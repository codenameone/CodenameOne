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
 * Windows port BLE bridge: the C side of
 * com.codename1.impl.windows.WindowsBleBridge. Each ParparVM-translated native
 * method forwards to the in-process libcn1ble shared library (btleplug -> WinRT)
 * declared in cn1ble.h. All engine state lives in the library, so the bridge is
 * stateless and ignores __cn1ThisObject.
 *
 * pollEvent is the only blocking call; like the socket/subprocess bridges it is
 * bracketed with CN1_YIELD_THREAD / CN1_RESUME_THREAD so a thread parked waiting
 * for the next BLE event does not stall the concurrent GC. The command calls are
 * fire-and-forget (they only enqueue work in the engine and return at once), so
 * they need no yield. String args are converted with stringToUTF8, which returns
 * a per-thread buffer the next call overwrites -- multi-string methods therefore
 * strdup each converted value before converting the next.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <stdlib.h>
#include <string.h>

/*
 * libcn1ble is linked, and CN1_INCLUDE_BLUETOOTH defined, only when the app
 * actually uses com.codename1.bluetooth (the builder injects both, like the
 * iOS CoreBluetooth path). Otherwise the calls resolve to local no-op
 * definitions so the port links standalone and Bluetooth reports unavailable.
 */
#ifdef CN1_INCLUDE_BLUETOOTH
#include "cn1ble.h"
#else
int cn1ble_start(void) { return 0; }
int cn1ble_is_alive(void) { return 0; }
char* cn1ble_poll_event(long timeoutMs) { (void) timeoutMs; return strdup(""); }
void cn1ble_free(char* p) { free(p); }
void cn1ble_scan_start(long id, const char* csv) { (void) id; (void) csv; }
void cn1ble_scan_stop(long id) { (void) id; }
void cn1ble_connect(long id, const char* a) { (void) id; (void) a; }
void cn1ble_disconnect(long id, const char* a) { (void) id; (void) a; }
void cn1ble_discover(long id, const char* a) { (void) id; (void) a; }
void cn1ble_read(long id, const char* a, const char* s, const char* c) { (void) id; (void) a; (void) s; (void) c; }
void cn1ble_write(long id, const char* a, const char* s, const char* c, const unsigned char* v, int n, int nr) { (void) id; (void) a; (void) s; (void) c; (void) v; (void) n; (void) nr; }
void cn1ble_subscribe(long id, const char* a, const char* s, const char* c, int e) { (void) id; (void) a; (void) s; (void) c; (void) e; }
void cn1ble_read_descriptor(long id, const char* a, const char* s, const char* c, const char* d) { (void) id; (void) a; (void) s; (void) c; (void) d; }
void cn1ble_write_descriptor(long id, const char* a, const char* s, const char* c, const char* d, const unsigned char* v, int n) { (void) id; (void) a; (void) s; (void) c; (void) d; (void) v; (void) n; }
void cn1ble_read_rssi(long id, const char* a) { (void) id; (void) a; }
void cn1ble_close(void) { }
#endif

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

/* Converts a Java String to a freshly malloc'd UTF-8 C string (never NULL: a
 * null/failed conversion yields an empty string). stringToUTF8's per-thread
 * buffer is overwritten by the next call, so the result MUST be copied out
 * before another arg is converted. Caller frees with free(). */
static char* cn1BleDupString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {
    const char* s;
    char* dup;
    if (str == JAVA_NULL) {
        return strdup("");
    }
    s = stringToUTF8(threadStateData, str);
    dup = strdup(s ? s : "");
    return dup ? dup : strdup("");
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsBleBridge_start___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    int ok;
    /* Opening the shared library and the OS adapter manager can block briefly;
     * park across it so the GC is not held off waiting on this thread. */
    CN1_YIELD_THREAD;
    ok = cn1ble_start();
    CN1_RESUME_THREAD;
    return ok ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsBleBridge_isAlive___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return cn1ble_is_alive() ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsBleBridge_pollEvent___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG timeoutMillis) {
    char* ev;
    JAVA_OBJECT result;
    /* Blocks up to timeoutMillis for the next event -- park the thread across
     * the wait exactly like socketRead so the concurrent GC is not deadlocked. */
    CN1_YIELD_THREAD;
    ev = cn1ble_poll_event((long) timeoutMillis);
    CN1_RESUME_THREAD;
    if (ev == 0) {
        return JAVA_NULL; /* timeout -> caller polls again */
    }
    result = newStringFromCString(threadStateData, ev);
    cn1ble_free(ev);
    return result;
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_scanStart___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT serviceCsv) {
    char* csv = cn1BleDupString(threadStateData, serviceCsv);
    cn1ble_scan_start((long) id, csv);
    free(csv);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_scanStop___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id) {
    cn1ble_scan_stop((long) id);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_connect___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address) {
    char* a = cn1BleDupString(threadStateData, address);
    cn1ble_connect((long) id, a);
    free(a);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_disconnect___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address) {
    char* a = cn1BleDupString(threadStateData, address);
    cn1ble_disconnect((long) id, a);
    free(a);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_discover___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address) {
    char* a = cn1BleDupString(threadStateData, address);
    cn1ble_discover((long) id, a);
    free(a);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_read___long_java_lang_String_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address, JAVA_OBJECT service, JAVA_OBJECT characteristic) {
    char* a = cn1BleDupString(threadStateData, address);
    char* s = cn1BleDupString(threadStateData, service);
    char* c = cn1BleDupString(threadStateData, characteristic);
    cn1ble_read((long) id, a, s, c);
    free(a);
    free(s);
    free(c);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_write___long_java_lang_String_java_lang_String_java_lang_String_byte_1ARRAY_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address, JAVA_OBJECT service, JAVA_OBJECT characteristic, JAVA_OBJECT value, JAVA_BOOLEAN noResponse) {
    char* a = cn1BleDupString(threadStateData, address);
    char* s = cn1BleDupString(threadStateData, service);
    char* c = cn1BleDupString(threadStateData, characteristic);
    const unsigned char* bytes = 0;
    int len = 0;
    if (value != JAVA_NULL) {
        bytes = (const unsigned char*) (*(JAVA_ARRAY) value).data;
        len = (int) (*(JAVA_ARRAY) value).length;
    }
    /* The engine copies the bytes synchronously within this call (no parking),
     * so the array cannot be swept mid-use and needs no keep-alive anchor. */
    cn1ble_write((long) id, a, s, c, bytes, len, noResponse ? 1 : 0);
    free(a);
    free(s);
    free(c);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_subscribe___long_java_lang_String_java_lang_String_java_lang_String_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address, JAVA_OBJECT service, JAVA_OBJECT characteristic, JAVA_BOOLEAN enable) {
    char* a = cn1BleDupString(threadStateData, address);
    char* s = cn1BleDupString(threadStateData, service);
    char* c = cn1BleDupString(threadStateData, characteristic);
    cn1ble_subscribe((long) id, a, s, c, enable ? 1 : 0);
    free(a);
    free(s);
    free(c);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_readDescriptor___long_java_lang_String_java_lang_String_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address, JAVA_OBJECT service, JAVA_OBJECT characteristic, JAVA_OBJECT descriptor) {
    char* a = cn1BleDupString(threadStateData, address);
    char* s = cn1BleDupString(threadStateData, service);
    char* c = cn1BleDupString(threadStateData, characteristic);
    char* d = cn1BleDupString(threadStateData, descriptor);
    cn1ble_read_descriptor((long) id, a, s, c, d);
    free(a);
    free(s);
    free(c);
    free(d);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_writeDescriptor___long_java_lang_String_java_lang_String_java_lang_String_java_lang_String_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address, JAVA_OBJECT service, JAVA_OBJECT characteristic, JAVA_OBJECT descriptor, JAVA_OBJECT value) {
    char* a = cn1BleDupString(threadStateData, address);
    char* s = cn1BleDupString(threadStateData, service);
    char* c = cn1BleDupString(threadStateData, characteristic);
    char* d = cn1BleDupString(threadStateData, descriptor);
    const unsigned char* bytes = 0;
    int len = 0;
    if (value != JAVA_NULL) {
        bytes = (const unsigned char*) (*(JAVA_ARRAY) value).data;
        len = (int) (*(JAVA_ARRAY) value).length;
    }
    cn1ble_write_descriptor((long) id, a, s, c, d, bytes, len);
    free(a);
    free(s);
    free(c);
    free(d);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_readRssi___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG id, JAVA_OBJECT address) {
    char* a = cn1BleDupString(threadStateData, address);
    cn1ble_read_rssi((long) id, a);
    free(a);
}

JAVA_VOID com_codename1_impl_windows_WindowsBleBridge_close__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    cn1ble_close();
}

#endif /* _WIN32 */
