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
 * method forwards to the in-process libcn1ble shared library (btleplug -> WinRT),
 * which is loaded at runtime via LoadLibraryA (see the cn1ble_* provider section
 * below). All engine state lives in the library, so the bridge is stateless and
 * ignores __cn1ThisObject.
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
 * CN1_INCLUDE_BLUETOOTH is defined only when the app actually uses
 * com.codename1.bluetooth (the builder injects it, like the iOS CoreBluetooth
 * path). When set, the cn1ble_* ABI below is provided by local wrappers that
 * load cn1ble.dll at RUNTIME via LoadLibraryA/GetProcAddress rather than
 * linking it at build time -- this mirrors the Linux port's dlopen idiom for
 * optional libraries and avoids needing a Windows import library. When unset,
 * the calls resolve to the local no-op definitions in the #else branch, so the
 * port links standalone and Bluetooth reports unavailable. If the DLL cannot be
 * loaded, or any entry point is missing, the runtime wrappers degrade to
 * exactly the same behaviour as those stubs.
 */
#ifdef CN1_INCLUDE_BLUETOOTH
#include <windows.h>

typedef int   (*cn1ble_start_fn)(void);
typedef int   (*cn1ble_is_alive_fn)(void);
typedef char* (*cn1ble_poll_event_fn)(long);
typedef void  (*cn1ble_free_fn)(char*);
typedef void  (*cn1ble_scan_start_fn)(long, const char*);
typedef void  (*cn1ble_scan_stop_fn)(long);
typedef void  (*cn1ble_connect_fn)(long, const char*);
typedef void  (*cn1ble_disconnect_fn)(long, const char*);
typedef void  (*cn1ble_discover_fn)(long, const char*);
typedef void  (*cn1ble_read_fn)(long, const char*, const char*, const char*);
typedef void  (*cn1ble_write_fn)(long, const char*, const char*, const char*, const unsigned char*, int, int);
typedef void  (*cn1ble_subscribe_fn)(long, const char*, const char*, const char*, int);
typedef void  (*cn1ble_read_descriptor_fn)(long, const char*, const char*, const char*, const char*);
typedef void  (*cn1ble_write_descriptor_fn)(long, const char*, const char*, const char*, const char*, const unsigned char*, int);
typedef void  (*cn1ble_read_rssi_fn)(long, const char*);
typedef void  (*cn1ble_close_fn)(void);

static HMODULE g_lib = 0;
static int     g_state = 0; /* 0 untried, 1 loaded, -1 failed */

static cn1ble_start_fn            p_start;
static cn1ble_is_alive_fn         p_is_alive;
static cn1ble_poll_event_fn       p_poll_event;
static cn1ble_free_fn             p_free;
static cn1ble_scan_start_fn       p_scan_start;
static cn1ble_scan_stop_fn        p_scan_stop;
static cn1ble_connect_fn          p_connect;
static cn1ble_disconnect_fn       p_disconnect;
static cn1ble_discover_fn         p_discover;
static cn1ble_read_fn             p_read;
static cn1ble_write_fn            p_write;
static cn1ble_subscribe_fn        p_subscribe;
static cn1ble_read_descriptor_fn  p_read_descriptor;
static cn1ble_write_descriptor_fn p_write_descriptor;
static cn1ble_read_rssi_fn        p_read_rssi;
static cn1ble_close_fn            p_close;

/* Loads cn1ble.dll and resolves every entry point exactly once. Prefers the
 * copy sitting next to the executable, then falls back to the standard DLL
 * search path. On the first failure -- DLL missing or any symbol absent --
 * g_state latches to -1 and every wrapper degrades to its no-op behaviour.
 * Single-guarded (the reader thread calls first); no locking. Returns nonzero
 * once the DLL is loaded and all pointers are non-NULL. */
static int cn1ble_ensure(void) {
    char path[MAX_PATH];
    DWORD n;
    HMODULE lib = 0;
    if (g_state != 0) {
        return g_state == 1;
    }
    n = GetModuleFileNameA(NULL, path, sizeof(path));
    if (n > 0 && n < sizeof(path)) {
        char* slash = strrchr(path, '\\');
        if (slash != 0 && (size_t) (slash - path) + sizeof("\\cn1ble.dll") <= sizeof(path)) {
            strcpy(slash, "\\cn1ble.dll");
            lib = LoadLibraryA(path);
        }
    }
    if (lib == 0) {
        lib = LoadLibraryA("cn1ble.dll");
    }
    if (lib == 0) {
        g_state = -1;
        return 0;
    }
    p_start            = (cn1ble_start_fn)            GetProcAddress(lib, "cn1ble_start");
    p_is_alive         = (cn1ble_is_alive_fn)         GetProcAddress(lib, "cn1ble_is_alive");
    p_poll_event       = (cn1ble_poll_event_fn)       GetProcAddress(lib, "cn1ble_poll_event");
    p_free             = (cn1ble_free_fn)             GetProcAddress(lib, "cn1ble_free");
    p_scan_start       = (cn1ble_scan_start_fn)       GetProcAddress(lib, "cn1ble_scan_start");
    p_scan_stop        = (cn1ble_scan_stop_fn)        GetProcAddress(lib, "cn1ble_scan_stop");
    p_connect          = (cn1ble_connect_fn)          GetProcAddress(lib, "cn1ble_connect");
    p_disconnect       = (cn1ble_disconnect_fn)       GetProcAddress(lib, "cn1ble_disconnect");
    p_discover         = (cn1ble_discover_fn)         GetProcAddress(lib, "cn1ble_discover");
    p_read             = (cn1ble_read_fn)             GetProcAddress(lib, "cn1ble_read");
    p_write            = (cn1ble_write_fn)            GetProcAddress(lib, "cn1ble_write");
    p_subscribe        = (cn1ble_subscribe_fn)        GetProcAddress(lib, "cn1ble_subscribe");
    p_read_descriptor  = (cn1ble_read_descriptor_fn)  GetProcAddress(lib, "cn1ble_read_descriptor");
    p_write_descriptor = (cn1ble_write_descriptor_fn) GetProcAddress(lib, "cn1ble_write_descriptor");
    p_read_rssi        = (cn1ble_read_rssi_fn)        GetProcAddress(lib, "cn1ble_read_rssi");
    p_close            = (cn1ble_close_fn)            GetProcAddress(lib, "cn1ble_close");
    if (p_start == 0 || p_is_alive == 0 || p_poll_event == 0 || p_free == 0 ||
        p_scan_start == 0 || p_scan_stop == 0 || p_connect == 0 || p_disconnect == 0 ||
        p_discover == 0 || p_read == 0 || p_write == 0 || p_subscribe == 0 ||
        p_read_descriptor == 0 || p_write_descriptor == 0 || p_read_rssi == 0 ||
        p_close == 0) {
        FreeLibrary(lib);
        g_state = -1;
        return 0;
    }
    g_lib = lib;
    g_state = 1;
    return 1;
}

int cn1ble_start(void) { return cn1ble_ensure() ? p_start() : 0; }
int cn1ble_is_alive(void) { return cn1ble_ensure() ? p_is_alive() : 0; }
char* cn1ble_poll_event(long timeoutMs) { return cn1ble_ensure() ? p_poll_event(timeoutMs) : strdup(""); }
void cn1ble_free(char* p) { if (cn1ble_ensure()) { p_free(p); } else { free(p); } }
void cn1ble_scan_start(long id, const char* csv) { if (cn1ble_ensure()) { p_scan_start(id, csv); } }
void cn1ble_scan_stop(long id) { if (cn1ble_ensure()) { p_scan_stop(id); } }
void cn1ble_connect(long id, const char* a) { if (cn1ble_ensure()) { p_connect(id, a); } }
void cn1ble_disconnect(long id, const char* a) { if (cn1ble_ensure()) { p_disconnect(id, a); } }
void cn1ble_discover(long id, const char* a) { if (cn1ble_ensure()) { p_discover(id, a); } }
void cn1ble_read(long id, const char* a, const char* s, const char* c) { if (cn1ble_ensure()) { p_read(id, a, s, c); } }
void cn1ble_write(long id, const char* a, const char* s, const char* c, const unsigned char* v, int n, int nr) { if (cn1ble_ensure()) { p_write(id, a, s, c, v, n, nr); } }
void cn1ble_subscribe(long id, const char* a, const char* s, const char* c, int e) { if (cn1ble_ensure()) { p_subscribe(id, a, s, c, e); } }
void cn1ble_read_descriptor(long id, const char* a, const char* s, const char* c, const char* d) { if (cn1ble_ensure()) { p_read_descriptor(id, a, s, c, d); } }
void cn1ble_write_descriptor(long id, const char* a, const char* s, const char* c, const char* d, const unsigned char* v, int n) { if (cn1ble_ensure()) { p_write_descriptor(id, a, s, c, d, v, n); } }
void cn1ble_read_rssi(long id, const char* a) { if (cn1ble_ensure()) { p_read_rssi(id, a); } }
void cn1ble_close(void) { if (cn1ble_ensure()) { p_close(); } }
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
