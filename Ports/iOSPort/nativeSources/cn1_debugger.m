/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * On-device-debug runtime. Single-file implementation: wire-protocol
 * encode/decode, breakpoint hash, per-thread suspend/resume, command
 * handlers. The translator emits per-frame metadata that this file reads
 * (callStackFrameInfo, callStackLocalsAddresses) — see cn1_globals.h and
 * BytecodeMethod.appendLocalsAddressTable for the format.
 *
 * Wire protocol (binary, network byte order, length-prefixed):
 *   [u32 payload-length] [u8 command/event-code] [payload-bytes...]
 *
 * Codes 0x01-0x7F are commands (proxy -> device);
 * codes 0x80-0xFF are events (device -> proxy). See the #defines below.
 */

#import "cn1_debugger.h"

#ifdef CN1_ON_DEVICE_DEBUG

#import <Foundation/Foundation.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <string.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdlib.h>
#include <errno.h>

#define CN1_DBG_PROTOCOL_VERSION 1

// Commands (proxy -> device)
#define CMD_SET_BREAKPOINT   0x02
#define CMD_CLEAR_BREAKPOINT 0x03
#define CMD_RESUME           0x04
#define CMD_SUSPEND          0x05
#define CMD_GET_THREADS      0x06
#define CMD_GET_STACK        0x07
#define CMD_GET_LOCALS       0x08
#define CMD_STEP             0x09
#define CMD_DISPOSE          0x0A
#define CMD_GET_STRING       0x0B
#define CMD_GET_OBJECT_CLASS 0x0C
#define CMD_GET_OBJECT_FIELDS 0x0D
#define CMD_INVOKE_METHOD    0x0E
#define CMD_GET_ARRAY_LENGTH 0x0F
#define CMD_GET_ARRAY_VALUES 0x10
#define CMD_GET_SYMBOLS      0x11

// Events (device -> proxy)
#define EVT_HELLO            0x80
#define EVT_THREAD_LIST      0x81
#define EVT_BP_HIT           0x82
#define EVT_STEP_COMPLETE    0x83
#define EVT_STACK            0x84
#define EVT_LOCALS           0x85
#define EVT_VM_DEATH         0x86
#define EVT_STRING_VALUE     0x87
#define EVT_REPLY_STATUS     0x88
#define EVT_OBJECT_CLASS     0x89
#define EVT_OBJECT_FIELDS    0x8A
#define EVT_STDOUT_LINE      0x8B
#define EVT_STDERR_LINE      0x8C
#define EVT_INVOKE_RESULT    0x8D
#define EVT_ARRAY_LENGTH     0x8E
#define EVT_ARRAY_VALUES     0x8F
#define EVT_SYMBOLS          0x90

// The gzip-compressed on-device-debug symbol table, emitted as a generated
// C source (cn1_debug_symbols.c) by the translator and linked into this
// binary. Served to the desktop proxy in chunks via CMD_GET_SYMBOLS so the
// proxy needs no local sidecar file — the crux of making on-device debugging
// work for cloud builds. Weakly imported so the runtime still links (and
// simply reports a zero-length table) if the generated source is absent.
extern const unsigned char* cn1_debug_symbols_data(void) __attribute__((weak));
extern int cn1_debug_symbols_length(void) __attribute__((weak));

// java.lang.String's clazz struct is emitted by the translator; we reference
// it by symbol so cn1_debugger.m doesn't depend on the generated
// cn1_class_method_index.h. Used in handleGetLocals to tag String references
// with JDWP type 's' so jdb can call StringReference.Value directly instead
// of falling through to a (currently unsupported) toString() InvokeMethod.
extern struct clazz class__java_lang_String;

// Step kinds
#define STEP_INTO 0
#define STEP_OVER 1
#define STEP_OUT  2

// Override the weak default in cn1_globals.m. Flipped to 1 once a proxy is
// connected and out of the HELLO handshake.
volatile int cn1DebuggerActive = 0;

static int g_proxyFd = -1;
static pthread_mutex_t g_writeMutex = PTHREAD_MUTEX_INITIALIZER;

// Wait-for-attach state. cn1_debugger_run_when_ready stashes the VM-callback
// block here; the listener thread invokes it on the main queue once the
// proxy has acked the IDE attach (the first CMD_RESUME). Releasing the wait
// on the main thread keeps UIKit free to draw the overlay during the wait.
static int g_waitForAttach = 0;
static int g_attachReady = 0;           // set when the IDE has signalled ready
static dispatch_block_t g_onReadyBlock = nil;
static pthread_mutex_t g_attachMutex = PTHREAD_MUTEX_INITIALIZER;

// "Waiting for debugger" overlay view. Owned by cn1_debugger; shown from
// cn1_debugger_start when waitForAttach is set, dismissed once the IDE has
// attached. Installed as a subview of the app's keyWindow root view rather
// than as a separate UIWindow because iOS 13+ scene-based apps (which
// Codename One is by default) refuse to display non-scene UIWindows.
static UIView* g_waitOverlay = nil;

// Forward declaration; callers in the command dispatch sit above the
// definition further down the file.
static void cn1_debugger_fire_ready_block_if_pending(void);

/* --------------------------------------------------------------------- */
/* stdout / stderr forwarding. dup2()'s the original FDs to a pipe, then */
/* a reader thread chunks the pipe by '\n' and emits each completed line */
/* as an EVT_STDOUT_LINE / EVT_STDERR_LINE so the proxy can surface them */
/* in the IDE debug console. Partial lines buffer until a newline lands. */
/* --------------------------------------------------------------------- */
static int g_origStdoutFd = -1;
static int g_origStderrFd = -1;

// Forward declaration so the capture thread can emit events; the body sits
// further down with the rest of the wire-protocol helpers.
static void sendEvent(uint8_t cmd, const void* payload, uint32_t len);

struct stream_capture {
    int readFd;            // read end of the pipe; we own it
    int origFd;            // copy of the original FD so we can also forward to it
    uint8_t evtCode;       // EVT_STDOUT_LINE / EVT_STDERR_LINE
};

static void forwardLineToOriginal(int origFd, const uint8_t* line, size_t len) {
    if (origFd < 0) return;
    // Best-effort write; missing bytes here only cost us a console line.
    ssize_t w = write(origFd, line, len);
    (void)w;
    uint8_t nl = '\n';
    w = write(origFd, &nl, 1);
    (void)w;
}

static void* streamCaptureThread(void* arg) {
    struct stream_capture* cap = (struct stream_capture*)arg;
    uint8_t buf[1024];
    uint8_t line[2048];
    size_t lineLen = 0;
    for (;;) {
        ssize_t n = read(cap->readFd, buf, sizeof(buf));
        if (n <= 0) {
            if (n < 0 && (errno == EINTR || errno == EAGAIN)) continue;
            break;
        }
        for (ssize_t i = 0; i < n; i++) {
            uint8_t c = buf[i];
            if (c == '\n' || lineLen == sizeof(line)) {
                // Mirror to the host console (Xcode/simulator log) so local
                // debugging without an attached proxy still sees prints.
                forwardLineToOriginal(cap->origFd, line, lineLen);
                if (g_proxyFd >= 0) {
                    uint8_t* payload = (uint8_t*)malloc(4 + lineLen);
                    if (payload) {
                        uint32_t lenBE = htonl((uint32_t)lineLen);
                        memcpy(payload, &lenBE, 4);
                        if (lineLen > 0) memcpy(payload + 4, line, lineLen);
                        sendEvent(cap->evtCode, payload, 4 + (uint32_t)lineLen);
                        free(payload);
                    }
                }
                lineLen = 0;
                if (c == '\n') continue;
            }
            line[lineLen++] = c;
        }
    }
    return NULL;
}

static void startStreamCapture(int* origFdSlot, FILE* stream, int fdNo, uint8_t evtCode) {
    int pfd[2];
    if (pipe(pfd) != 0) return;
    int savedOrig = dup(fdNo);
    if (savedOrig < 0) {
        close(pfd[0]); close(pfd[1]);
        return;
    }
    *origFdSlot = savedOrig;
    // Re-route the target FD so anything writing to it (printf, NSLog into
    // os_log_t fallback, fprintf(stderr, ...)) goes into our pipe.
    if (dup2(pfd[1], fdNo) < 0) {
        close(savedOrig); close(pfd[0]); close(pfd[1]);
        *origFdSlot = -1;
        return;
    }
    close(pfd[1]);
    // Line-buffer so we don't sit on a partial line.
    setvbuf(stream, NULL, _IOLBF, 0);

    struct stream_capture* cap = (struct stream_capture*)malloc(sizeof(*cap));
    if (!cap) { close(pfd[0]); return; }
    cap->readFd = pfd[0];
    cap->origFd = savedOrig;
    cap->evtCode = evtCode;
    pthread_t t;
    pthread_create(&t, NULL, streamCaptureThread, cap);
    pthread_detach(t);
}

/* --------------------------------------------------------------------- */
/* Breakpoint hash. Open-addressed, lock-free reads via atomic 64-bit    */
/* slots. Key = (methodId << 32) | line; zero is the empty sentinel,     */
/* which means line 0 of methodId 0 is reserved (no real source uses it).*/
/* --------------------------------------------------------------------- */

#define BP_TABLE_SIZE 1024
#define BP_TABLE_MASK (BP_TABLE_SIZE - 1)
static _Atomic uint64_t g_bps[BP_TABLE_SIZE];

static inline uint32_t bp_hash(uint64_t key) {
    // splitmix64-ish reduction
    key ^= key >> 33;
    key *= 0xff51afd7ed558ccdULL;
    key ^= key >> 33;
    return (uint32_t)key & BP_TABLE_MASK;
}

static int bp_contains(int methodId, int line) {
    uint64_t key = ((uint64_t)(uint32_t)methodId << 32) | (uint32_t)line;
    uint32_t h = bp_hash(key);
    for (int i = 0; i < BP_TABLE_SIZE; i++) {
        uint64_t v = atomic_load_explicit(&g_bps[(h + i) & BP_TABLE_MASK], memory_order_acquire);
        if (v == 0) return 0;
        if (v == key) return 1;
    }
    return 0;
}

static void bp_add(int methodId, int line) {
    uint64_t key = ((uint64_t)(uint32_t)methodId << 32) | (uint32_t)line;
    uint32_t h = bp_hash(key);
    for (int i = 0; i < BP_TABLE_SIZE; i++) {
        uint64_t expected = 0;
        if (atomic_compare_exchange_strong_explicit(
                &g_bps[(h + i) & BP_TABLE_MASK], &expected, key,
                memory_order_acq_rel, memory_order_relaxed)) {
            return;
        }
        if (expected == key) return; // already present
    }
    NSLog(@"cn1_debugger: breakpoint table full");
}

/* --------------------------------------------------------------------- */
/* Field-offset registry. Each translator-emitted class .m calls         */
/* cn1_debugger_register_fields from a __attribute__((constructor)), so  */
/* by the time the listener thread is alive every class has registered   */
/* its instance fields. The table is sparse (indexed by classId so       */
/* gaps are common) but classIds are dense enough that a plain array     */
/* outperforms a hash table here for the read pattern (one lookup per    */
/* CMD_GET_OBJECT_FIELDS request).                                       */
/* --------------------------------------------------------------------- */

struct cn1_field_class_entry {
    const cn1_field_entry* table;
    int count;
};

#define CN1_FIELD_REG_INITIAL_CAP 2048
static struct cn1_field_class_entry* g_fieldsByClass = NULL;
static int g_fieldsByClassCap = 0;
static pthread_mutex_t g_fieldsRegMutex = PTHREAD_MUTEX_INITIALIZER;

void cn1_debugger_register_fields(int classId, const cn1_field_entry* table, int count) {
    if (classId < 0) return;
    pthread_mutex_lock(&g_fieldsRegMutex);
    if (classId >= g_fieldsByClassCap) {
        int newCap = g_fieldsByClassCap == 0 ? CN1_FIELD_REG_INITIAL_CAP : g_fieldsByClassCap * 2;
        while (classId >= newCap) newCap *= 2;
        struct cn1_field_class_entry* n =
            (struct cn1_field_class_entry*)realloc(g_fieldsByClass,
                newCap * sizeof(struct cn1_field_class_entry));
        if (!n) { pthread_mutex_unlock(&g_fieldsRegMutex); return; }
        memset(n + g_fieldsByClassCap, 0,
               (newCap - g_fieldsByClassCap) * sizeof(struct cn1_field_class_entry));
        g_fieldsByClass = n;
        g_fieldsByClassCap = newCap;
    }
    g_fieldsByClass[classId].table = table;
    g_fieldsByClass[classId].count = count;
    pthread_mutex_unlock(&g_fieldsRegMutex);
}

/**
 * Looks up the (table, count) for a classId. Walks the parent chain via
 * the class's vtable since field tables only cover own + inherited
 * declared on THIS class; CMD_GET_OBJECT_FIELDS resolves by classId of
 * the actual runtime class (which already includes inherited fields in
 * its layout-order list — see ByteCodeClass.getAllInstanceFieldsInLayoutOrder).
 */
static const cn1_field_entry* field_lookup_by_class_and_id(int classId, int fieldId, int* outCount) {
    if (classId < 0 || classId >= g_fieldsByClassCap) return NULL;
    const cn1_field_entry* table = g_fieldsByClass[classId].table;
    int count = g_fieldsByClass[classId].count;
    if (outCount) *outCount = count;
    if (!table) return NULL;
    for (int i = 0; i < count; i++) {
        if (table[i].fieldId == fieldId) return &table[i];
    }
    return NULL;
}

/* --------------------------------------------------------------------- */
/* Invoke-thunk registry. Indexed by methodId; translator-emitted        */
/* constructors fill the table at process load. Lookup is O(1).          */
/* --------------------------------------------------------------------- */

#define CN1_INVOKE_REG_INITIAL_CAP 4096
static cn1_invoke_thunk_t* g_invokeThunks = NULL;
static int g_invokeThunkCap = 0;
static pthread_mutex_t g_invokeRegMutex = PTHREAD_MUTEX_INITIALIZER;

void cn1_debugger_register_invoke_thunk(int methodId, cn1_invoke_thunk_t thunk) {
    if (methodId < 0) return;
    pthread_mutex_lock(&g_invokeRegMutex);
    if (methodId >= g_invokeThunkCap) {
        int newCap = g_invokeThunkCap == 0 ? CN1_INVOKE_REG_INITIAL_CAP : g_invokeThunkCap * 2;
        while (methodId >= newCap) newCap *= 2;
        cn1_invoke_thunk_t* n = (cn1_invoke_thunk_t*)realloc(g_invokeThunks,
                newCap * sizeof(cn1_invoke_thunk_t));
        if (!n) { pthread_mutex_unlock(&g_invokeRegMutex); return; }
        memset(n + g_invokeThunkCap, 0,
               (newCap - g_invokeThunkCap) * sizeof(cn1_invoke_thunk_t));
        g_invokeThunks = n;
        g_invokeThunkCap = newCap;
    }
    g_invokeThunks[methodId] = thunk;
    pthread_mutex_unlock(&g_invokeRegMutex);
}

static cn1_invoke_thunk_t invoke_thunk_for(int methodId) {
    if (methodId < 0 || methodId >= g_invokeThunkCap) return NULL;
    return g_invokeThunks[methodId];
}

/**
 * Read a field value into 8 host-endian bytes plus a JVM type-char.
 * Object refs become the JAVA_OBJECT pointer reinterpreted as uint64 so
 * the proxy can pass them straight to JDWP as objectIDs.
 */
static int field_read_into(JAVA_OBJECT obj, const cn1_field_entry* fe,
                           char* outType, uint64_t* outValue) {
    if (!obj || !fe) return 0;
    char* base = (char*)obj + fe->offset;
    *outType = fe->type;
    switch (fe->type) {
        case 'Z': *outValue = (uint64_t)(*(JAVA_BOOLEAN*)base & 1); return 1;
        case 'B': *outValue = (uint64_t)(uint8_t)(*(JAVA_BYTE*)base); return 1;
        case 'S': *outValue = (uint64_t)(uint16_t)(*(JAVA_SHORT*)base); return 1;
        case 'C': *outValue = (uint64_t)(uint16_t)(*(JAVA_CHAR*)base); return 1;
        case 'I': *outValue = (uint64_t)(uint32_t)(*(JAVA_INT*)base); return 1;
        case 'F': {
            JAVA_FLOAT f = *(JAVA_FLOAT*)base;
            uint32_t bits;
            memcpy(&bits, &f, 4);
            *outValue = (uint64_t)bits; return 1;
        }
        case 'J': *outValue = (uint64_t)(*(JAVA_LONG*)base); return 1;
        case 'D': {
            JAVA_DOUBLE d = *(JAVA_DOUBLE*)base;
            uint64_t bits;
            memcpy(&bits, &d, 8);
            *outValue = bits; return 1;
        }
        case 'L': default: {
            JAVA_OBJECT v = *(JAVA_OBJECT*)base;
            *outValue = (uint64_t)(uintptr_t)v;
            *outType = 'L';
            return 1;
        }
    }
}

static void bp_clear(int methodId, int line) {
    // Simple zero-out. Linear probing tolerates holes only because we stop
    // on zero, which would terminate searches early — so on delete we shift
    // subsequent matching slots up. With a small table and few breakpoints
    // a full re-probe is fine.
    uint64_t key = ((uint64_t)(uint32_t)methodId << 32) | (uint32_t)line;
    uint32_t h = bp_hash(key);
    int found = -1;
    for (int i = 0; i < BP_TABLE_SIZE; i++) {
        uint64_t v = atomic_load_explicit(&g_bps[(h + i) & BP_TABLE_MASK], memory_order_acquire);
        if (v == 0) break;
        if (v == key) {
            found = (h + i) & BP_TABLE_MASK;
            break;
        }
    }
    if (found < 0) return;
    atomic_store_explicit(&g_bps[found], 0, memory_order_release);
    // Re-insert any following non-empty slots whose ideal slot is <= found
    // so future lookups don't terminate prematurely.
    int j = (found + 1) & BP_TABLE_MASK;
    while (1) {
        uint64_t v = atomic_load_explicit(&g_bps[j], memory_order_acquire);
        if (v == 0) break;
        atomic_store_explicit(&g_bps[j], 0, memory_order_release);
        bp_add((int)(v >> 32), (int)(v & 0xFFFFFFFFULL));
        j = (j + 1) & BP_TABLE_MASK;
    }
}

/* --------------------------------------------------------------------- */
/* Per-thread suspend state. Keyed by ThreadLocalData->threadId hashed   */
/* down into a fixed-size table — ParparVM caps at 1024 simultaneous     */
/* threads so we mirror that. Each slot owns a mutex + condvar that the  */
/* hitting Java thread blocks on; the listener thread signals to resume. */
/* --------------------------------------------------------------------- */

struct sus_state {
    pthread_mutex_t mu;
    pthread_cond_t  cv;
    int suspended;
    int stepKind;       // -1 = none, otherwise STEP_INTO/OVER/OUT
    int stepFromDepth;  // callStackOffset captured at suspend
    struct ThreadLocalData* tsd; // current frame owner while suspended
    // Debugger-driven method invocation. The listener thread sets these
    // and signals s->cv; the suspended thread runs the thunk and signals
    // back. invokeReady is the predicate for the listener's wait — 0 =
    // running, 1 = finished, the result is in invokeResult.
    cn1_invoke_thunk_t invokeThunk;
    JAVA_OBJECT invokeThis;
    cn1_invoke_arg invokeArgs[16];
    cn1_invoke_result invokeResult;
    int invokeReady;
};

#define SUS_TABLE_SIZE 1024
static struct sus_state g_sus[SUS_TABLE_SIZE];
static _Atomic int g_susInit = 0;

static void ensureSusInit(void) {
    int expected = 0;
    if (atomic_compare_exchange_strong(&g_susInit, &expected, 1)) {
        for (int i = 0; i < SUS_TABLE_SIZE; i++) {
            pthread_mutex_init(&g_sus[i].mu, NULL);
            pthread_cond_init(&g_sus[i].cv, NULL);
            g_sus[i].suspended = 0;
            g_sus[i].stepKind = -1;
            g_sus[i].tsd = NULL;
        }
    }
}

static struct sus_state* susForThread(int64_t threadId) {
    ensureSusInit();
    return &g_sus[((uint64_t)threadId) & (SUS_TABLE_SIZE - 1)];
}

/* --------------------------------------------------------------------- */
/* Wire I/O. Send/recv all bytes or fail. The write side is serialised  */
/* by g_writeMutex so an event from a Java thread can't interleave with */
/* a reply from the command loop.                                       */
/* --------------------------------------------------------------------- */

static int sendAll(int fd, const void* buf, size_t n) {
    const char* p = (const char*)buf;
    while (n > 0) {
        ssize_t w = send(fd, p, n, 0);
        if (w <= 0) {
            if (w < 0 && errno == EINTR) continue;
            return -1;
        }
        p += w; n -= (size_t)w;
    }
    return 0;
}

static int readAll(int fd, void* buf, size_t n) {
    char* p = (char*)buf;
    while (n > 0) {
        ssize_t r = recv(fd, p, n, 0);
        if (r <= 0) {
            if (r < 0 && errno == EINTR) continue;
            return -1;
        }
        p += r; n -= (size_t)r;
    }
    return 0;
}

static void sendEvent(uint8_t cmd, const void* payload, uint32_t len) {
    if (g_proxyFd < 0) return;
    pthread_mutex_lock(&g_writeMutex);
    uint32_t lenBE = htonl(len);
    int ok = (sendAll(g_proxyFd, &lenBE, 4) == 0)
          && (sendAll(g_proxyFd, &cmd, 1) == 0)
          && (len == 0 || sendAll(g_proxyFd, payload, len) == 0);
    pthread_mutex_unlock(&g_writeMutex);
    if (!ok) {
        NSLog(@"cn1_debugger: send failed, closing");
        cn1DebuggerActive = 0;
        // Don't close fd here; let the read loop notice and tear down.
    }
}

/* --------------------------------------------------------------------- */
/* Suspend / resume. suspendCurrent releases the GC bit so a long pause */
/* doesn't block collection. resumeThread signals the condvar.          */
/* --------------------------------------------------------------------- */

static void suspendCurrent(struct ThreadLocalData* tsd) {
    int64_t threadId = (int64_t)tsd->threadId;
    struct sus_state* s = susForThread(threadId);
    pthread_mutex_lock(&s->mu);
    s->suspended = 1;
    s->stepFromDepth = tsd->callStackOffset;
    s->tsd = tsd;
    // Mark thread inactive so the concurrent GC can mark/sweep freely.
    tsd->threadActive = JAVA_FALSE;
    while (s->suspended) {
        pthread_cond_wait(&s->cv, &s->mu);
        // The listener thread may have queued a debugger-invoked method
        // call for us to run. Servicing it on this thread keeps the call
        // inside a valid Java context (right tsd, right call stack) and
        // gives ParparVM's throwException somewhere to longjmp back to —
        // we set up a catch-all try block inside the thunk itself.
        if (s->invokeThunk && !s->invokeReady) {
            cn1_invoke_thunk_t thunk = s->invokeThunk;
            JAVA_OBJECT thisObj = s->invokeThis;
            cn1_invoke_arg argsCopy[16];
            memcpy(argsCopy, s->invokeArgs, sizeof(argsCopy));
            // Re-activate the thread while running the thunk so any
            // allocations / GC interaction it triggers proceed normally;
            // we'll re-park before going back to wait.
            tsd->threadActive = JAVA_TRUE;
            pthread_mutex_unlock(&s->mu);
            cn1_invoke_result r;
            r.type = 'V';
            r.value.o = JAVA_NULL;
            thunk(tsd, thisObj, argsCopy, &r);
            pthread_mutex_lock(&s->mu);
            tsd->threadActive = JAVA_FALSE;
            s->invokeResult = r;
            s->invokeReady = 1;
            pthread_cond_broadcast(&s->cv);
        }
    }
    // GC may have parked us; wait for it to finish before resuming.
    while (tsd->threadBlockedByGC) {
        pthread_mutex_unlock(&s->mu);
        usleep(1000);
        pthread_mutex_lock(&s->mu);
    }
    tsd->threadActive = JAVA_TRUE;
    s->tsd = NULL;
    pthread_mutex_unlock(&s->mu);
}

/*
 * resumeThreadById signals a suspended thread to continue but does NOT
 * touch its stepKind — so a CMD_STEP that ran earlier (which sets the
 * step kind) survives a subsequent CMD_RESUME from jdb. Caller passes
 * preserveStep=1 to leave stepKind alone, or 0 to also reset to -1.
 */
static void resumeThreadById(int64_t threadId, int preserveStep) {
    struct sus_state* s = susForThread(threadId);
    pthread_mutex_lock(&s->mu);
    if (!preserveStep) {
        s->stepKind = -1;
    }
    s->suspended = 0;
    pthread_cond_signal(&s->cv);
    pthread_mutex_unlock(&s->mu);
}

static void resumeAll(int preserveStep) {
    for (int i = 0; i < SUS_TABLE_SIZE; i++) {
        pthread_mutex_lock(&g_sus[i].mu);
        if (g_sus[i].suspended) {
            if (!preserveStep) {
                g_sus[i].stepKind = -1;
            }
            g_sus[i].suspended = 0;
            pthread_cond_signal(&g_sus[i].cv);
        }
        pthread_mutex_unlock(&g_sus[i].mu);
    }
}

/* Sets the step state on a thread and wakes it if currently suspended. */
static void setStepAndResume(int64_t threadId, int stepKind) {
    struct sus_state* s = susForThread(threadId);
    pthread_mutex_lock(&s->mu);
    s->stepKind = stepKind;
    if (s->suspended) {
        s->suspended = 0;
        pthread_cond_signal(&s->cv);
    }
    pthread_mutex_unlock(&s->mu);
}

/* --------------------------------------------------------------------- */
/* Hot-path check called from __CN1_DEBUG_INFO. Strong definition that   */
/* shadows the weak stub in cn1_globals.m. Reached only when             */
/* cn1DebuggerActive is non-zero.                                        */
/* --------------------------------------------------------------------- */

/*
 * Byte-explicit big-endian writers. Avoids the htonl-and-shift trap on
 * little-endian hosts where the order of bytes inside a uint64_t depends
 * on host endianness and the shift composition doesn't actually yield
 * network byte order in memory.
 */
static inline void writeBE32(uint8_t* dst, uint32_t v) {
    dst[0] = (uint8_t)(v >> 24); dst[1] = (uint8_t)(v >> 16);
    dst[2] = (uint8_t)(v >>  8); dst[3] = (uint8_t)v;
}
static inline void writeBE64(uint8_t* dst, uint64_t v) {
    dst[0] = (uint8_t)(v >> 56); dst[1] = (uint8_t)(v >> 48);
    dst[2] = (uint8_t)(v >> 40); dst[3] = (uint8_t)(v >> 32);
    dst[4] = (uint8_t)(v >> 24); dst[5] = (uint8_t)(v >> 16);
    dst[6] = (uint8_t)(v >>  8); dst[7] = (uint8_t)v;
}

static void emitLocationEvent(uint8_t code, int64_t threadId, int methodId, int line) {
    uint8_t buf[16];
    writeBE64(buf,      (uint64_t)threadId);
    writeBE32(buf + 8,  (uint32_t)methodId);
    writeBE32(buf + 12, (uint32_t)line);
    sendEvent(code, buf, 16);
}

void cn1_debugger_check(struct ThreadLocalData* tsd, int line) {
    if (tsd->callStackOffset <= 0) return;
    const struct cn1_frame_info* fi = tsd->callStackFrameInfo[tsd->callStackOffset - 1];
    if (fi == NULL) return;
    int methodId = fi->methodId;
    int64_t threadId = (int64_t)tsd->threadId;

    // Stepping has priority over breakpoints so a step that lands on a
    // breakpoint reports once (as STEP_COMPLETE).
    struct sus_state* s = susForThread(threadId);
    int sk = s->stepKind;
    if (sk >= 0) {
        int depth = tsd->callStackOffset;
        int shouldStop = 0;
        switch (sk) {
            case STEP_INTO: shouldStop = 1; break;
            case STEP_OVER: shouldStop = (depth <= s->stepFromDepth); break;
            case STEP_OUT:  shouldStop = (depth <  s->stepFromDepth); break;
        }
        if (shouldStop) {
            // Clear step state under the per-thread mutex so a concurrent
            // CMD_STEP can't race the clear.
            pthread_mutex_lock(&s->mu);
            s->stepKind = -1;
            pthread_mutex_unlock(&s->mu);
            emitLocationEvent(EVT_STEP_COMPLETE, threadId, methodId, line);
            suspendCurrent(tsd);
            return;
        }
    }
    if (bp_contains(methodId, line)) {
        emitLocationEvent(EVT_BP_HIT, threadId, methodId, line);
        suspendCurrent(tsd);
    }
}

/* --------------------------------------------------------------------- */
/* Command-loop handlers. Stack / locals are read from the suspended    */
/* thread's tsd which was recorded into the sus_state when it parked.   */
/* --------------------------------------------------------------------- */

static void handleGetStack(int64_t threadId) {
    struct sus_state* s = susForThread(threadId);
    pthread_mutex_lock(&s->mu);
    struct ThreadLocalData* tsd = s->tsd;
    if (tsd == NULL || tsd->callStackOffset <= 0) {
        pthread_mutex_unlock(&s->mu);
        uint8_t buf[12];
        writeBE64(buf,     (uint64_t)threadId);
        writeBE32(buf + 8, 0);
        sendEvent(EVT_STACK, buf, 12);
        return;
    }
    int depth = tsd->callStackOffset;
    // Cap depth to keep payload sane.
    if (depth > 256) depth = 256;
    size_t sz = 8 + 4 + (size_t)depth * 8;
    uint8_t* buf = (uint8_t*)malloc(sz);
    writeBE64(buf,     (uint64_t)threadId);
    writeBE32(buf + 8, (uint32_t)depth);
    // Frames emitted innermost-first.
    for (int i = 0; i < depth; i++) {
        int frameIdx = (tsd->callStackOffset - 1 - i);
        int methodId = 0;
        const struct cn1_frame_info* fi = tsd->callStackFrameInfo[frameIdx];
        if (fi) methodId = fi->methodId;
        int line = tsd->callStackLine[frameIdx];
        writeBE32(buf + 12 + i * 8,     (uint32_t)methodId);
        writeBE32(buf + 12 + i * 8 + 4, (uint32_t)line);
    }
    pthread_mutex_unlock(&s->mu);
    sendEvent(EVT_STACK, buf, (uint32_t)sz);
    free(buf);
}

static void handleGetLocals(int64_t threadId, int frameOffsetFromTop) {
    struct sus_state* s = susForThread(threadId);
    pthread_mutex_lock(&s->mu);
    struct ThreadLocalData* tsd = s->tsd;
    if (tsd == NULL || tsd->callStackOffset <= 0
            || frameOffsetFromTop < 0
            || frameOffsetFromTop >= tsd->callStackOffset) {
        pthread_mutex_unlock(&s->mu);
        uint32_t zero = 0;
        sendEvent(EVT_LOCALS, &zero, 4);
        return;
    }
    int frameIdx = tsd->callStackOffset - 1 - frameOffsetFromTop;
    const struct cn1_frame_info* fi = tsd->callStackFrameInfo[frameIdx];
    void** addrs = tsd->callStackLocalsAddresses[frameIdx];
    if (fi == NULL || addrs == NULL) {
        pthread_mutex_unlock(&s->mu);
        uint32_t zero = 0;
        sendEvent(EVT_LOCALS, &zero, 4);
        return;
    }
    int count = fi->varTableCount;
    size_t sz = 4 + (size_t)count * (4 + 1 + 8);
    uint8_t* buf = (uint8_t*)malloc(sz);
    writeBE32(buf, (uint32_t)count);
    uint8_t* p = buf + 4;
    for (int i = 0; i < count; i++) {
        const struct cn1_var_entry* v = &fi->varTable[i];
        writeBE32(p, (uint32_t)v->slot); p += 4;
        uint8_t tag = (uint8_t)v->typeCode;
        uint64_t value = 0;
        if (v->slot >= 0 && v->slot < fi->numLocals && addrs[v->slot] != NULL) {
            switch (v->typeCode) {
                case 'I': case 'B': case 'S': case 'C': case 'Z':
                    value = (uint64_t)(*(int32_t*)addrs[v->slot]);
                    break;
                case 'J':
                    value = (uint64_t)(*(int64_t*)addrs[v->slot]);
                    break;
                case 'F': {
                    float f = *(float*)addrs[v->slot];
                    uint32_t u; memcpy(&u, &f, 4);
                    value = u;
                    break;
                }
                case 'D': {
                    double d = *(double*)addrs[v->slot];
                    uint64_t u; memcpy(&u, &d, 8);
                    value = u;
                    break;
                }
                case 'L': case '[': {
                    JAVA_OBJECT obj = *(JAVA_OBJECT*)addrs[v->slot];
                    value = (uint64_t)(uintptr_t)obj;
                    // Tag java.lang.String references with JDWP type 's'
                    // so the IDE can read their contents via
                    // StringReference.Value instead of invoking toString().
                    if (v->typeCode == 'L' && obj != JAVA_NULL
                            && obj->__codenameOneParentClsReference == &class__java_lang_String) {
                        tag = 's';
                    }
                    break;
                }
            }
        }
        *p++ = tag;
        writeBE64(p, value); p += 8;
    }
    pthread_mutex_unlock(&s->mu);
    sendEvent(EVT_LOCALS, buf, (uint32_t)sz);
    free(buf);
}

/* --------------------------------------------------------------------- */
/* Listener thread.                                                      */
/* --------------------------------------------------------------------- */

static int handleCommand(uint8_t cmd, const uint8_t* payload, uint32_t len) {
    switch (cmd) {
        case CMD_SET_BREAKPOINT: {
            if (len < 8) return 0;
            uint32_t mid, line;
            memcpy(&mid, payload, 4);
            memcpy(&line, payload + 4, 4);
            bp_add((int)ntohl(mid), (int)ntohl(line));
            return 0;
        }
        case CMD_CLEAR_BREAKPOINT: {
            if (len < 8) return 0;
            uint32_t mid, line;
            memcpy(&mid, payload, 4);
            memcpy(&line, payload + 4, 4);
            bp_clear((int)ntohl(mid), (int)ntohl(line));
            return 0;
        }
        case CMD_RESUME: {
            // Preserve any pending step kind so a SINGLE_STEP request
            // followed by VM.Resume keeps the step active for the next line.
            if (len < 8) {
                resumeAll(/*preserveStep*/ 1);
            } else {
                uint32_t hi, lo;
                memcpy(&hi, payload, 4);
                memcpy(&lo, payload + 4, 4);
                int64_t tid = ((int64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
                if (tid == 0) resumeAll(1); else resumeThreadById(tid, 1);
            }
            // The first RESUME after attach also fires the VM-callback the
            // AppDelegate registered via cn1_debugger_run_when_ready, so the
            // splash / waiting overlay gives way to the user app.
            cn1_debugger_fire_ready_block_if_pending();
            return 0;
        }
        case CMD_STEP: {
            if (len < 9) return 0;
            uint32_t hi, lo;
            memcpy(&hi, payload, 4);
            memcpy(&lo, payload + 4, 4);
            int64_t tid = ((int64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            uint8_t kind = payload[8];
            setStepAndResume(tid, (int)kind);
            return 0;
        }
        case CMD_GET_STACK: {
            if (len < 8) return 0;
            uint32_t hi, lo;
            memcpy(&hi, payload, 4);
            memcpy(&lo, payload + 4, 4);
            int64_t tid = ((int64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            handleGetStack(tid);
            return 0;
        }
        case CMD_GET_LOCALS: {
            if (len < 12) return 0;
            uint32_t hi, lo, frame;
            memcpy(&hi, payload, 4);
            memcpy(&lo, payload + 4, 4);
            memcpy(&frame, payload + 8, 4);
            int64_t tid = ((int64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            handleGetLocals(tid, (int)ntohl(frame));
            return 0;
        }
        case CMD_DISPOSE: {
            cn1DebuggerActive = 0;
            resumeAll(/*preserveStep*/ 0);
            return -1; // tells listener to tear down
        }
        case CMD_GET_OBJECT_CLASS: {
            if (len < 8) {
                sendEvent(EVT_OBJECT_CLASS, NULL, 0);
                return 0;
            }
            uint32_t hi, lo;
            memcpy(&hi, payload, 4);
            memcpy(&lo, payload + 4, 4);
            uint64_t ptr = ((uint64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            int classId = -1;
            uint8_t isArray = 0;
            JAVA_OBJECT obj = (JAVA_OBJECT)(uintptr_t)ptr;
            if (obj != JAVA_NULL && obj->__codenameOneParentClsReference != NULL) {
                struct clazz* cls = obj->__codenameOneParentClsReference;
                classId = cls->classId;
                isArray = cls->isArray ? 1 : 0;
            }
            // Reply: classId(4) + isArray(1). Older proxies that only read
            // 4 bytes still work.
            uint8_t reply[5];
            uint32_t cidBE = htonl((uint32_t)classId);
            memcpy(reply, &cidBE, 4);
            reply[4] = isArray;
            sendEvent(EVT_OBJECT_CLASS, reply, 5);
            return 0;
        }
        case CMD_GET_STRING: {
            if (len < 8) {
                sendEvent(EVT_STRING_VALUE, NULL, 0);
                return 0;
            }
            uint32_t hi, lo;
            memcpy(&hi, payload, 4);
            memcpy(&lo, payload + 4, 4);
            uint64_t ptr = ((uint64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            JAVA_OBJECT obj = (JAVA_OBJECT)(uintptr_t)ptr;
            NSString* ns = nil;
            @try {
                if (obj != JAVA_NULL) {
                    ns = toNSString(getThreadLocalData(), obj);
                }
            } @catch (NSException* e) {
                ns = nil;
            }
            const char* utf8 = ns ? [ns UTF8String] : "";
            size_t n = strlen(utf8);
            sendEvent(EVT_STRING_VALUE, utf8, (uint32_t)n);
            return 0;
        }
        case CMD_GET_OBJECT_FIELDS: {
            // Payload: objId(8) fieldCount(4) fieldIds[fieldCount](4 each)
            if (len < 12) {
                // Reply empty so the proxy doesn't hang on its synchronous wait.
                uint8_t empty[4] = {0,0,0,0};
                sendEvent(EVT_OBJECT_FIELDS, empty, 4);
                return 0;
            }
            uint32_t hi, lo;
            memcpy(&hi, payload, 4);
            memcpy(&lo, payload + 4, 4);
            uint64_t ptr = ((uint64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            uint32_t countBE;
            memcpy(&countBE, payload + 8, 4);
            int count = (int)ntohl(countBE);
            if (count < 0 || (uint32_t)(12 + count * 4) > len) {
                uint8_t empty[4] = {0,0,0,0};
                sendEvent(EVT_OBJECT_FIELDS, empty, 4);
                return 0;
            }
            JAVA_OBJECT obj = (JAVA_OBJECT)(uintptr_t)ptr;
            int classId = -1;
            if (obj != JAVA_NULL && obj->__codenameOneParentClsReference != NULL) {
                classId = obj->__codenameOneParentClsReference->classId;
            }
            // Reply payload: count(4) then per-field { type(1), value(8) }.
            uint32_t sz = 4 + (uint32_t)count * 9;
            uint8_t* buf = (uint8_t*)malloc(sz);
            if (!buf) {
                uint8_t empty[4] = {0,0,0,0};
                sendEvent(EVT_OBJECT_FIELDS, empty, 4);
                return 0;
            }
            uint32_t countOutBE = htonl((uint32_t)count);
            memcpy(buf, &countOutBE, 4);
            uint8_t* p = buf + 4;
            for (int i = 0; i < count; i++) {
                uint32_t fidBE;
                memcpy(&fidBE, payload + 12 + i * 4, 4);
                int fid = (int)ntohl(fidBE);
                char tc = 'L';
                uint64_t val = 0;
                const cn1_field_entry* fe = field_lookup_by_class_and_id(classId, fid, NULL);
                if (fe && obj != JAVA_NULL) {
                    field_read_into(obj, fe, &tc, &val);
                } else {
                    tc = 'L'; val = 0;
                }
                *p++ = (uint8_t)tc;
                writeBE64(p, val); p += 8;
            }
            sendEvent(EVT_OBJECT_FIELDS, buf, sz);
            free(buf);
            return 0;
        }
        case CMD_INVOKE_METHOD: {
            // Payload: threadId(8) methodId(4) thisObj(8) argCount(4) args[argCount]*9
            // Each arg: type-char(1) + value(8). Value layout matches
            // cn1_invoke_arg union per type.
            // Reply: EVT_INVOKE_RESULT with type-char(1) + value(8).
            if (len < 24) {
                uint8_t empty[9] = {'V',0,0,0,0,0,0,0,0};
                sendEvent(EVT_INVOKE_RESULT, empty, 9);
                return 0;
            }
            uint32_t hi, lo;
            memcpy(&hi, payload, 4); memcpy(&lo, payload + 4, 4);
            int64_t tid = ((int64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            uint32_t midBE;
            memcpy(&midBE, payload + 8, 4);
            int mid = (int)ntohl(midBE);
            uint32_t thisHi, thisLo;
            memcpy(&thisHi, payload + 12, 4); memcpy(&thisLo, payload + 16, 4);
            uint64_t thisRaw = ((uint64_t)ntohl(thisHi) << 32) | (uint32_t)ntohl(thisLo);
            JAVA_OBJECT thisObj = (JAVA_OBJECT)(uintptr_t)thisRaw;
            uint32_t cntBE;
            memcpy(&cntBE, payload + 20, 4);
            int argCount = (int)ntohl(cntBE);
            if (argCount < 0 || argCount > 16
                    || (uint32_t)(24 + argCount * 9) > len) {
                uint8_t err[9] = {'V',0,0,0,0,0,0,0,0};
                sendEvent(EVT_INVOKE_RESULT, err, 9);
                return 0;
            }
            cn1_invoke_arg argv[16];
            memset(argv, 0, sizeof(argv));
            for (int i = 0; i < argCount; i++) {
                uint8_t t = payload[24 + i * 9];
                uint32_t valHi, valLo;
                memcpy(&valHi, payload + 24 + i * 9 + 1, 4);
                memcpy(&valLo, payload + 24 + i * 9 + 5, 4);
                uint64_t v = ((uint64_t)ntohl(valHi) << 32) | (uint32_t)ntohl(valLo);
                switch ((char)t) {
                    case 'Z': case 'B': case 'S': case 'C': case 'I':
                        argv[i].i = (JAVA_INT)(uint32_t)v; break;
                    case 'J':
                        argv[i].j = (JAVA_LONG)v; break;
                    case 'F': {
                        uint32_t bits = (uint32_t)v;
                        memcpy(&argv[i].f, &bits, 4); break;
                    }
                    case 'D':
                        memcpy(&argv[i].d, &v, 8); break;
                    case 'L': case '[': default:
                        argv[i].o = (JAVA_OBJECT)(uintptr_t)v; break;
                }
            }
            cn1_invoke_thunk_t thunk = invoke_thunk_for(mid);
            if (thunk == NULL) {
                uint8_t notfound[9] = {'V',0,0,0,0,0,0,0,0};
                sendEvent(EVT_INVOKE_RESULT, notfound, 9);
                return 0;
            }
            // Queue the invoke on the target thread and wait for the
            // result. If the thread isn't currently suspended we can't
            // dispatch (JDWP requires it).
            struct sus_state* s = susForThread(tid);
            pthread_mutex_lock(&s->mu);
            if (s->tsd == NULL) {
                pthread_mutex_unlock(&s->mu);
                uint8_t notsuspended[9] = {'V',0,0,0,0,0,0,0,0};
                sendEvent(EVT_INVOKE_RESULT, notsuspended, 9);
                return 0;
            }
            s->invokeThunk = thunk;
            s->invokeThis = thisObj;
            memcpy(s->invokeArgs, argv, sizeof(argv));
            s->invokeReady = 0;
            pthread_cond_broadcast(&s->cv);
            // Block listener thread until result lands.
            while (!s->invokeReady) {
                pthread_cond_wait(&s->cv, &s->mu);
            }
            cn1_invoke_result r = s->invokeResult;
            s->invokeThunk = NULL;
            s->invokeReady = 0;
            pthread_mutex_unlock(&s->mu);

            uint8_t reply[9];
            reply[0] = (uint8_t)r.type;
            uint64_t bits = 0;
            switch (r.type) {
                case 'Z': case 'B': case 'S': case 'C': case 'I':
                    bits = (uint64_t)(uint32_t)r.value.i; break;
                case 'J':
                    bits = (uint64_t)r.value.j; break;
                case 'F': {
                    uint32_t fb;
                    memcpy(&fb, &r.value.f, 4);
                    bits = (uint64_t)fb; break;
                }
                case 'D':
                    memcpy(&bits, &r.value.d, 8); break;
                case 'L': case '[': case 'X':
                    bits = (uint64_t)(uintptr_t)r.value.o; break;
                case 'V': default:
                    bits = 0; break;
            }
            writeBE64(reply + 1, bits);
            sendEvent(EVT_INVOKE_RESULT, reply, 9);
            return 0;
        }
        case CMD_GET_ARRAY_LENGTH: {
            // Payload: objId(8). Reply: EVT_ARRAY_LENGTH with length(4).
            if (len < 8) {
                uint8_t err[4] = {0,0,0,0};
                sendEvent(EVT_ARRAY_LENGTH, err, 4);
                return 0;
            }
            uint32_t hi, lo;
            memcpy(&hi, payload, 4); memcpy(&lo, payload + 4, 4);
            uint64_t ptr = ((uint64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            JAVA_OBJECT obj = (JAVA_OBJECT)(uintptr_t)ptr;
            int length = 0;
            if (obj != JAVA_NULL) {
                length = ((JAVA_ARRAY)obj)->length;
            }
            uint8_t reply[4];
            writeBE32(reply, (uint32_t)length);
            sendEvent(EVT_ARRAY_LENGTH, reply, 4);
            return 0;
        }
        case CMD_GET_ARRAY_VALUES: {
            // Payload: objId(8) firstIndex(4) count(4)
            // Reply: tag(1) + count(4) + values. Element width depends on tag.
            if (len < 16) {
                uint8_t err[5] = {'L', 0,0,0,0};
                sendEvent(EVT_ARRAY_VALUES, err, 5);
                return 0;
            }
            uint32_t hi, lo;
            memcpy(&hi, payload, 4); memcpy(&lo, payload + 4, 4);
            uint64_t ptr = ((uint64_t)ntohl(hi) << 32) | (uint32_t)ntohl(lo);
            uint32_t fstBE, cntBE;
            memcpy(&fstBE, payload + 8, 4);
            memcpy(&cntBE, payload + 12, 4);
            int firstIdx = (int)ntohl(fstBE);
            int reqCount = (int)ntohl(cntBE);
            JAVA_OBJECT obj = (JAVA_OBJECT)(uintptr_t)ptr;
            if (obj == JAVA_NULL) {
                uint8_t err[5] = {'L', 0,0,0,0};
                sendEvent(EVT_ARRAY_VALUES, err, 5);
                return 0;
            }
            JAVA_ARRAY arr = (JAVA_ARRAY)obj;
            int arrLen = arr->length;
            if (firstIdx < 0) firstIdx = 0;
            if (firstIdx > arrLen) firstIdx = arrLen;
            int avail = arrLen - firstIdx;
            int count = (reqCount < 0 || reqCount > avail) ? avail : reqCount;

            // Derive the JVM type-char for elements. primitiveSize==0 means
            // object array; otherwise we use the element class's clsName to
            // distinguish among same-width primitives (byte vs boolean, int
            // vs float, ...).
            char tag = 'L';
            int elemSize = arr->primitiveSize;
            struct clazz* arrCls = obj->__codenameOneParentClsReference;
            struct clazz* elemCls = arrCls ? arrCls->arrayType : NULL;
            if (elemSize == 0) {
                tag = 'L';
            } else if (elemCls && elemCls->clsName) {
                const char* en = elemCls->clsName;
                if      (strcmp(en, "int")     == 0) tag = 'I';
                else if (strcmp(en, "long")    == 0) tag = 'J';
                else if (strcmp(en, "boolean") == 0) tag = 'Z';
                else if (strcmp(en, "byte")    == 0) tag = 'B';
                else if (strcmp(en, "char")    == 0) tag = 'C';
                else if (strcmp(en, "short")   == 0) tag = 'S';
                else if (strcmp(en, "float")   == 0) tag = 'F';
                else if (strcmp(en, "double")  == 0) tag = 'D';
                else {
                    // Unknown primitive — fall back to width-based guess.
                    switch (elemSize) {
                        case 1: tag = 'B'; break;
                        case 2: tag = 'S'; break;
                        case 4: tag = 'I'; break;
                        case 8: tag = 'J'; break;
                        default: tag = 'L'; break;
                    }
                }
            } else {
                switch (elemSize) {
                    case 1: tag = 'B'; break;
                    case 2: tag = 'S'; break;
                    case 4: tag = 'I'; break;
                    case 8: tag = 'J'; break;
                    default: tag = 'L'; break;
                }
            }

            // Element bytes on the wire match the tag's natural width.
            int perElem;
            switch (tag) {
                case 'Z': case 'B': perElem = 1; break;
                case 'S': case 'C': perElem = 2; break;
                case 'I': case 'F': perElem = 4; break;
                case 'J': case 'D': perElem = 8; break;
                case 'L': case '[': default: perElem = 8; break;
            }
            uint32_t sz = 1 + 4 + (uint32_t)count * (uint32_t)perElem;
            uint8_t* buf = (uint8_t*)malloc(sz);
            if (!buf) {
                uint8_t err[5] = {'L', 0,0,0,0};
                sendEvent(EVT_ARRAY_VALUES, err, 5);
                return 0;
            }
            buf[0] = (uint8_t)tag;
            writeBE32(buf + 1, (uint32_t)count);
            uint8_t* p = buf + 5;
            for (int i = 0; i < count; i++) {
                int idx = firstIdx + i;
                switch (tag) {
                    case 'Z': p[0] = ((JAVA_BOOLEAN*)arr->data)[idx] & 1; p += 1; break;
                    case 'B': p[0] = (uint8_t)((JAVA_BYTE*)arr->data)[idx]; p += 1; break;
                    case 'S': { JAVA_SHORT s = ((JAVA_SHORT*)arr->data)[idx];
                                p[0] = (uint8_t)((s >> 8) & 0xff); p[1] = (uint8_t)(s & 0xff); }
                              p += 2; break;
                    case 'C': { JAVA_CHAR c = ((JAVA_CHAR*)arr->data)[idx];
                                p[0] = (uint8_t)((c >> 8) & 0xff); p[1] = (uint8_t)(c & 0xff); }
                              p += 2; break;
                    case 'I': writeBE32(p, (uint32_t)((JAVA_INT*)arr->data)[idx]); p += 4; break;
                    case 'F': { JAVA_FLOAT f = ((JAVA_FLOAT*)arr->data)[idx];
                                uint32_t fb; memcpy(&fb, &f, 4);
                                writeBE32(p, fb); }
                              p += 4; break;
                    case 'J': writeBE64(p, (uint64_t)((JAVA_LONG*)arr->data)[idx]); p += 8; break;
                    case 'D': { JAVA_DOUBLE d = ((JAVA_DOUBLE*)arr->data)[idx];
                                uint64_t db; memcpy(&db, &d, 8);
                                writeBE64(p, db); }
                              p += 8; break;
                    case 'L': case '[': default: {
                        JAVA_OBJECT v = ((JAVA_OBJECT*)arr->data)[idx];
                        writeBE64(p, (uint64_t)(uintptr_t)v);
                        p += 8; break;
                    }
                }
            }
            sendEvent(EVT_ARRAY_VALUES, buf, sz);
            free(buf);
            return 0;
        }
        case CMD_GET_SYMBOLS: {
            // Payload: offset(4) maxLen(4). Reply EVT_SYMBOLS:
            // totalLen(4) offset(4) chunkLen(4) then chunkLen blob bytes.
            uint32_t offset = 0, maxLen = 0;
            if (len >= 8) {
                uint32_t o, m;
                memcpy(&o, payload, 4);
                memcpy(&m, payload + 4, 4);
                offset = ntohl(o);
                maxLen = ntohl(m);
            }
            const unsigned char* data = (cn1_debug_symbols_data != NULL) ? cn1_debug_symbols_data() : NULL;
            uint32_t total = (data != NULL && cn1_debug_symbols_length != NULL)
                    ? (uint32_t)cn1_debug_symbols_length() : 0;
            if (offset > total) offset = total;
            uint32_t remaining = total - offset;
            uint32_t chunk = (maxLen == 0 || maxLen > remaining) ? remaining : maxLen;
            // Keep any single frame comfortably under the proxy's 1 MiB cap.
            if (chunk > (256u * 1024u)) chunk = 256u * 1024u;
            uint8_t* buf = (uint8_t*)malloc(12 + chunk);
            if (!buf) {
                uint8_t empty[12] = {0};
                sendEvent(EVT_SYMBOLS, empty, 12);
                return 0;
            }
            writeBE32(buf,     total);
            writeBE32(buf + 4, offset);
            writeBE32(buf + 8, chunk);
            if (chunk > 0 && data != NULL) memcpy(buf + 12, data + offset, chunk);
            sendEvent(EVT_SYMBOLS, buf, 12 + chunk);
            free(buf);
            return 0;
        }
        case CMD_GET_THREADS:
        case CMD_SUSPEND:
            // Minimal viable: reply empty so the proxy doesn't hang.
            sendEvent(EVT_REPLY_STATUS, NULL, 0);
            return 0;
        default:
            NSLog(@"cn1_debugger: unknown command 0x%02x", cmd);
            return 0;
    }
}

// Opens a single outbound TCP connection to the proxy. Returns a connected
// fd, or -1 on any failure (caller decides whether to retry).
static int cn1_debugger_connect_once(NSString* host, int port) {
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) return -1;
    struct sockaddr_in sa;
    memset(&sa, 0, sizeof(sa));
    sa.sin_family = AF_INET;
    sa.sin_port = htons(port);
    if (inet_pton(AF_INET, [host UTF8String], &sa.sin_addr) != 1) {
        // Try resolving as a hostname.
        struct addrinfo hints = {0}, *res = NULL;
        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_STREAM;
        if (getaddrinfo([host UTF8String], NULL, &hints, &res) == 0 && res) {
            sa.sin_addr = ((struct sockaddr_in*)res->ai_addr)->sin_addr;
            freeaddrinfo(res);
        } else {
            close(fd);
            return -1;
        }
    }
    if (connect(fd, (struct sockaddr*)&sa, sizeof(sa)) == 0) {
        return fd;
    }
    close(fd);
    return -1;
}

static int cn1_debugger_is_attach_ready(void) {
    pthread_mutex_lock(&g_attachMutex);
    int r = g_attachReady;
    pthread_mutex_unlock(&g_attachMutex);
    return r;
}

static void* listenerThreadMain(void* arg) {
    @autoreleasepool {
        NSDictionary* info = [[NSBundle mainBundle] infoDictionary];
        NSString* host = info[@"CN1ProxyHost"];
        NSNumber* portN = info[@"CN1ProxyPort"];
        int port = portN ? [portN intValue] : 55333;
        if (!host) {
            NSLog(@"cn1_debugger: CN1ProxyHost not configured");
            return NULL;
        }

        // Outer (re)connect loop. With waitForAttach the app is parked on the
        // "Waiting for debugger" overlay and blocking must NEVER time out — so
        // we retry the connect forever, and if the proxy drops before the IDE
        // has attached we reconnect rather than booting the app. Without
        // waitForAttach we retry only briefly (so launching a moment before
        // the proxy is up still works) and then let the app boot normally.
        for (;;) {
            int fd = -1;
            for (int attempt = 0; ; attempt++) {
                fd = cn1_debugger_connect_once(host, port);
                if (fd >= 0) break;
                if (!g_waitForAttach && attempt >= 19) break; // ~10s, then give up
                usleep(500000);
            }
            if (fd < 0) {
                NSLog(@"cn1_debugger: could not connect to %@:%d, giving up", host, port);
                // Non-wait builds boot even if the proxy never comes up.
                cn1_debugger_fire_ready_block_if_pending();
                return NULL;
            }
            int nodelay = 1;
            setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &nodelay, sizeof(nodelay));
            g_proxyFd = fd;

            // Send HELLO.
            uint8_t hello[3];
            hello[0] = 0;
            hello[1] = 0;
            hello[2] = CN1_DBG_PROTOCOL_VERSION;
            sendEvent(EVT_HELLO, hello, 3);
            cn1DebuggerActive = 1;
            NSLog(@"cn1_debugger: connected to proxy %@:%d (proto v%d)", host, port, CN1_DBG_PROTOCOL_VERSION);

            for (;;) {
                uint32_t lenBE;
                if (readAll(fd, &lenBE, 4) < 0) break;
                uint32_t plen = ntohl(lenBE);
                if (plen > (1 << 20)) { // sanity cap
                    NSLog(@"cn1_debugger: oversized payload %u, closing", plen);
                    break;
                }
                uint8_t cmd;
                if (readAll(fd, &cmd, 1) < 0) break;
                uint8_t* payload = NULL;
                if (plen > 0) {
                    payload = (uint8_t*)malloc(plen);
                    if (readAll(fd, payload, plen) < 0) { free(payload); break; }
                }
                int rc = handleCommand(cmd, payload, plen);
                free(payload);
                if (rc < 0) break;
            }

            NSLog(@"cn1_debugger: proxy connection closed");
            cn1DebuggerActive = 0;
            resumeAll(/*preserveStep*/ 0);
            close(fd);
            g_proxyFd = -1;

            // If we're still waiting for the IDE and it never attached, the
            // proxy dropped prematurely — keep waiting (reconnect) instead of
            // booting. Otherwise release the AppDelegate's deferred callback so
            // the app boots after a normal/cancelled debug session.
            if (g_waitForAttach && !cn1_debugger_is_attach_ready()) {
                NSLog(@"cn1_debugger: proxy gone before attach; still waiting, will reconnect");
                usleep(500000);
                continue;
            }
            cn1_debugger_fire_ready_block_if_pending();
            return NULL;
        }
    }
    return NULL;
}

/* --------------------------------------------------------------------- */
/* Wait-for-attach plumbing. Non-blocking: the AppDelegate installs the */
/* "Waiting" overlay during didFinishLaunching, registers its VM-start  */
/* completion block via cn1_debugger_run_when_ready, and returns        */
/* promptly so UIKit can draw the overlay. The listener thread invokes  */
/* the completion on the main queue once the proxy reports the IDE has  */
/* attached (via CMD_RESUME).                                           */
/* --------------------------------------------------------------------- */

static UIView* cn1_debugger_active_host_view(void) {
    UIWindow* w = nil;
    if (@available(iOS 13.0, *)) {
        for (UIScene* s in UIApplication.sharedApplication.connectedScenes) {
            if ([s isKindOfClass:[UIWindowScene class]]) {
                for (UIWindow* candidate in ((UIWindowScene*)s).windows) {
                    if (candidate.isKeyWindow) { w = candidate; break; }
                }
                if (!w) {
                    UIWindow* anyVisible = ((UIWindowScene*)s).windows.firstObject;
                    if (anyVisible) { w = anyVisible; break; }
                }
            }
            if (w) break;
        }
    }
    if (!w) w = UIApplication.sharedApplication.keyWindow;
    if (!w) w = UIApplication.sharedApplication.windows.firstObject;
    return w ? w.rootViewController.view : nil;
}

static void cn1_debugger_install_wait_overlay_now(void); // forward
static int g_overlayInstallAttempts = 0;

static void cn1_debugger_try_install_wait_overlay(void) {
    UIView* host = cn1_debugger_active_host_view();
    if (host == nil) {
        // didFinishLaunching may not have set up the rootViewController yet;
        // retry on the next main-queue tick a few times.
        if (g_overlayInstallAttempts++ < 50) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(50 * NSEC_PER_MSEC)),
                           dispatch_get_main_queue(), ^{
                cn1_debugger_try_install_wait_overlay();
            });
        }
        return;
    }
    cn1_debugger_install_wait_overlay_now();
}

static void cn1_debugger_install_wait_overlay_now(void) {
    if (g_waitOverlay != nil) return;
    UIView* host = cn1_debugger_active_host_view();
    if (host == nil) return;

    UIView* overlay = [[UIView alloc] initWithFrame:host.bounds];
    overlay.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    overlay.backgroundColor = [UIColor colorWithWhite:0.0 alpha:0.85];

    UIActivityIndicatorView* spin;
    if (@available(iOS 13.0, *)) {
        spin = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleLarge];
    } else {
        spin = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    }
    spin.color = [UIColor whiteColor];
    [spin startAnimating];
    spin.translatesAutoresizingMaskIntoConstraints = NO;
    [overlay addSubview:spin];

    UILabel* lbl = [[UILabel alloc] init];
    lbl.text = @"Waiting for debugger to attach…";
    lbl.textColor = [UIColor whiteColor];
    lbl.textAlignment = NSTextAlignmentCenter;
    lbl.numberOfLines = 0;
    lbl.font = [UIFont systemFontOfSize:18 weight:UIFontWeightMedium];
    lbl.translatesAutoresizingMaskIntoConstraints = NO;
    [overlay addSubview:lbl];

    UILabel* sub = [[UILabel alloc] init];
    NSDictionary* info = [[NSBundle mainBundle] infoDictionary];
    NSString* host_s = info[@"CN1ProxyHost"] ?: @"?";
    NSNumber* portN = info[@"CN1ProxyPort"];
    sub.text = [NSString stringWithFormat:@"Proxy: %@:%@", host_s, portN ?: @"?"];
    sub.textColor = [UIColor colorWithWhite:0.85 alpha:1.0];
    sub.textAlignment = NSTextAlignmentCenter;
    sub.font = [UIFont systemFontOfSize:13];
    sub.translatesAutoresizingMaskIntoConstraints = NO;
    [overlay addSubview:sub];

    [host addSubview:overlay];
    overlay.translatesAutoresizingMaskIntoConstraints = NO;
    [NSLayoutConstraint activateConstraints:@[
        [overlay.leadingAnchor constraintEqualToAnchor:host.leadingAnchor],
        [overlay.trailingAnchor constraintEqualToAnchor:host.trailingAnchor],
        [overlay.topAnchor constraintEqualToAnchor:host.topAnchor],
        [overlay.bottomAnchor constraintEqualToAnchor:host.bottomAnchor],
        [spin.centerXAnchor constraintEqualToAnchor:overlay.centerXAnchor],
        [spin.centerYAnchor constraintEqualToAnchor:overlay.centerYAnchor constant:-32],
        [lbl.centerXAnchor constraintEqualToAnchor:overlay.centerXAnchor],
        [lbl.topAnchor constraintEqualToAnchor:spin.bottomAnchor constant:16],
        [lbl.widthAnchor constraintLessThanOrEqualToAnchor:overlay.widthAnchor multiplier:0.85],
        [sub.centerXAnchor constraintEqualToAnchor:overlay.centerXAnchor],
        [sub.topAnchor constraintEqualToAnchor:lbl.bottomAnchor constant:8]
    ]];

    g_waitOverlay = overlay;
}

static void cn1_debugger_install_wait_overlay(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        cn1_debugger_try_install_wait_overlay();
    });
}

static void cn1_debugger_dismiss_wait_overlay(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (g_waitOverlay == nil) return;
        [g_waitOverlay removeFromSuperview];
        g_waitOverlay = nil;
    });
}

/*
 * Invoked when the proxy confirms the IDE is ready. Fires the pending
 * VM-callback block on the main queue and dismisses the overlay. Safe to
 * call multiple times; only the first call has effect.
 */
static void cn1_debugger_fire_ready_block_if_pending(void) {
    dispatch_block_t block = nil;
    pthread_mutex_lock(&g_attachMutex);
    if (!g_attachReady) {
        g_attachReady = 1;
        block = g_onReadyBlock;
        g_onReadyBlock = nil;
    }
    pthread_mutex_unlock(&g_attachMutex);
    cn1_debugger_dismiss_wait_overlay();
    if (block) {
        dispatch_async(dispatch_get_main_queue(), block);
    }
}

void cn1_debugger_start(void) {
    NSDictionary* info = [[NSBundle mainBundle] infoDictionary];
    NSString* host = info[@"CN1ProxyHost"];
    if (!host) {
        NSLog(@"cn1_debugger: CN1ProxyHost not set in Info.plist; on-device-debug disabled at runtime");
        return;
    }
    NSNumber* wait = info[@"CN1ProxyWaitForAttach"];
    g_waitForAttach = (wait && [wait boolValue]) ? 1 : 0;

    // Capture stdout/stderr before any user code runs so prints during runApp
    // make it to the IDE. The reader threads also mirror lines back to the
    // saved original FDs so xcrun simctl log / Xcode still shows the output.
    startStreamCapture(&g_origStdoutFd, stdout, STDOUT_FILENO, EVT_STDOUT_LINE);
    startStreamCapture(&g_origStderrFd, stderr, STDERR_FILENO, EVT_STDERR_LINE);

    pthread_t t;
    pthread_create(&t, NULL, listenerThreadMain, NULL);
    pthread_detach(t);

    if (g_waitForAttach) {
        NSLog(@"cn1_debugger: waitForAttach=YES; installing overlay (non-blocking)");
        cn1_debugger_install_wait_overlay();
    }
}

void cn1_debugger_run_when_ready(void (^onReady)(void)) {
    if (onReady == nil) return;
    if (!g_waitForAttach) {
        onReady();
        return;
    }
    int alreadyReady = 0;
    pthread_mutex_lock(&g_attachMutex);
    if (g_attachReady) {
        alreadyReady = 1;
    } else {
        g_onReadyBlock = [onReady copy];
    }
    pthread_mutex_unlock(&g_attachMutex);
    if (alreadyReady) {
        // Proxy attached before the AppDelegate registered the callback.
        dispatch_async(dispatch_get_main_queue(), onReady);
    }
}

#endif // CN1_ON_DEVICE_DEBUG
