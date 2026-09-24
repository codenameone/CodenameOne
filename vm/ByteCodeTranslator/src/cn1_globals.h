/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

#ifndef __CN1GLOBALS__
#define __CN1GLOBALS__

// A variable a frame macro declares for every method, which only some method
// bodies go on to reference: SP is unused by a method that touches no operand
// stack, methodBlockOffset by one with no try block, locals by one with no
// locals, and so on. The declaration is unconditional because the macro cannot
// know, and emitting a different macro per combination would multiply the frame
// variants without making any generated code better.
//
// Saying so at the declaration is the C idiom for exactly this, and it is worth
// more than silence: it suppresses only these variables, so a genuinely unused
// variable anywhere else still reports. Before this, the six frame macros
// produced roughly 48,000 -Wunused-variable warnings in one application build --
// enough on their own to bury every real diagnostic in the log.
//
// Keyed on the compiler feature rather than the vendor: clang-cl defines
// _MSC_VER as well as __clang__, so testing for MSVC first would silently drop
// the attribute on the Windows port and leave that leg noisy.
#if defined(__GNUC__) || defined(__clang__)
    #define CN1_UNUSED __attribute__((unused))
#else
    #define CN1_UNUSED
#endif

#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>   /* offsetof, for the array-header assertions */
#include <string.h>
#include <limits.h>
#include "cn1_class_method_index.h"
#ifdef _WIN32
#include "cn1_win_compat.h"
#else
#include <pthread.h>
#endif
#include <setjmp.h>
#include <math.h>
#include <stdatomic.h>
/* For CN1_RESUME_THREAD, which yields a virtual thread rather than sleeping the
   carrier it runs on. Where the switch is not implemented, every entry point here
   is a static inline stub answering "there is no virtual thread", so the macro
   folds back to the plain sleep and that platform is byte-for-byte unchanged. */
#include "cn1_virtual_thread.h"
#include <stdint.h>

// Darwin's setjmp/longjmp SAVE and RESTORE the caller's signal mask -- a sigprocmask
// SYSCALL on each side. Every Java try-block entry compiles to a setjmp (as does every
// synchronized method's monitor block), so an exception-scoped hot loop pays a kernel
// round-trip per iteration (measured ~19% of an MVT-decode render's mutator time on
// macOS). The VM never changes the signal mask per frame, so no try frame needs the
// mask restored: use the no-mask _setjmp/_longjmp on Apple platforms. glibc's plain
// setjmp already omits the mask and Windows has no signal mask, so both keep the
// standard names. The GC's register-capture setjmps only need the callee-saved
// register flush into the jmp_buf, which _setjmp performs identically.
#if defined(__APPLE__)
#define CN1_TRY_SETJMP _setjmp
#define CN1_TRY_LONGJMP _longjmp
#else
#define CN1_TRY_SETJMP setjmp
#define CN1_TRY_LONGJMP longjmp
#endif

// PHASE 3b DEFAULT ON: conservative native-stack GC as a real root source, paired
// with object/instance frameless codegen (BytecodeMethod cn1.frameless.objects/
// .instance, also default on). Validated bit-identical + GC-safe + MtStress
// deterministic on arm64 macOS (the dev + iOS arch). Disable with
// -DCN1_DISABLE_CONSERVATIVE_GC_ROOTS (and run the translator with
// -Dcn1.frameless.objects=false -Dcn1.frameless.instance=false) to revert to the
// precise threadObjectStack GC.
#ifndef CN1_DISABLE_CONSERVATIVE_GC_ROOTS
#define CN1_CONSERVATIVE_GC_ROOTS
#endif

// CN1_GC_CONFORM: the footprint probe and (later) the structural conformance verifier
// for issue 5537. UNLIKE CN1_GC_VERIFY it changes no allocator behaviour -- in particular
// it does NOT force cn1BibopReleaseOffset() to 0, so the page-release and major-sweep
// paths that CN1_GC_VERIFY compiles out entirely are live and measurable under it.
// It subsumes CN1_GC_INSTRUMENT because the probe reports that flag's counters, and
// those counters do not exist without it.
#ifdef CN1_GC_CONFORM
#ifndef CN1_GC_INSTRUMENT
#define CN1_GC_INSTRUMENT
#endif
#endif

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// PHASE 3b: conservative native-stack scanning as a REAL GC root source. Needs
// signal-based universal thread stopping (sig_atomic_t / sigaction / ucontext).
#include <signal.h>
#if !defined(_WIN32)
// macOS gates the ucontext routines behind _XOPEN_SOURCE; define it locally (only
// affects which symbols are exposed, never computation) before pulling the header.
#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE 700
#endif
#include <ucontext.h>
#endif
#endif

// =========================================================================
// UNCOOPERATIVE-MUTATOR ESCALATION (issue #5537)
// =========================================================================
// The mark phase stops each lightweight thread by setting threadBlockedByGC and
// then spinning on threadActive until the thread parks itself. That handshake is
// purely COOPERATIVE, and ParparVM emits no safepoint polls in generated code --
// not on method entry, not on loop back-edges (grep the translator for
// threadBlockedByGC: no hits). Every safepoint lives inside a runtime function:
// the codenameOneGcMalloc handshake, cn1BibopMaybeGc (reached only once per BiBOP
// PAGE, not per object), contended monitorEnter, Thread.sleep/Object.wait and the
// CN1_YIELD_THREAD native bracket.
//
// So a Java loop that allocates nothing new and enters no contended monitor never
// reaches a safepoint, and the collector's spin never ends. That is not a slow GC,
// it is a whole-VM freeze: every other thread parks at its next allocation waiting
// for a cycle that can never start. Issue #5537 caught it in the debugger with the
// collector 10 minutes 9 seconds into that spin (totalwait = 609,491,500us) while a
// game-tree search thread ran a compute-only evaluation loop.
//
// The escalation bounds the spin and then FREEZES the thread with the same SIGUSR2
// stop the collector already uses for genuine native threads (cn1GcSignalStopOne),
// which needs no cooperation at all. Only available where that machinery is
// compiled: conservative roots on, and not Windows (no POSIX signals -- there the
// spin stays unbounded, exactly as today, because proceeding without stopping the
// thread would miss its roots and free live objects).
//
// The proper long-term answer is a safepoint poll on loop back-edges in the
// translator, which costs throughput in every loop the VM ever runs; this makes the
// pathological case survivable without paying that everywhere.
//
// -DCN1_GC_NO_FORCE_STOP restores the unbounded cooperative spin. It is the ablation arm
// GcUncooperativeThreadIntegrationTest builds to prove the gate can fail -- without a
// build that still wedges, an assertion that the VM does not wedge proves nothing.
//
// ALSO OFF UNDER -DCN1_DISABLE_SATB, which is the interesting one. A freeze is only
// releasable early -- before the mark drain -- because the SATB deletion barrier covers a
// mutator released mid-mark, exactly as it already covers native threads, which are never
// blocked at all. With the barrier compiled out that argument is gone, and the two ways to
// keep the escalation would both be worse than not having it: releasing early would let
// the resumed thread move a child out of a captured root into a local no snapshot contains
// and have the sweep take it, while holding the freeze through the drain puts markStatics
// (force-marking, which mallocs through the force-visited table) and gcMarkDrainParallel
// (lazy pthread_create) inside a window where the frozen thread may own the allocator or
// pthread lock -- a wedge in the middle of the fix for a wedge. The frozen window has to
// stay small and enumerable, and a full parallel drain is neither. So this ablation keeps
// master's unbounded cooperative wait, which is the behaviour it is there to measure
// against anyway.
#if defined(CN1_CONSERVATIVE_GC_ROOTS) && !defined(_WIN32) \
        && !defined(CN1_GC_NO_FORCE_STOP) && !defined(CN1_DISABLE_SATB)
#define CN1_GC_CAN_FORCE_STOP 1
#endif

// How long the cooperative safepoint handshake may spin before escalating, in
// microseconds. A thread that is going to park does so in microseconds, so this is
// pure headroom -- it only has to exceed the longest legitimate gap between a
// mutator's safepoints, which is one BiBOP page (64KB) of allocation or one
// unbracketed native call. Raising it lengthens the freeze in the pathological case
// and buys nothing in the normal one.
#ifndef CN1_GC_SAFEPOINT_WAIT_MAX_US
#define CN1_GC_SAFEPOINT_WAIT_MAX_US 250000
#endif

//#define DEBUG_GC_ALLOCATIONS

#define NUMBER_OF_SUPPORTED_THREADS 1024
#define CN1_FINALIZER_QUEUE_SIZE 65536

//#define CN1_INCLUDE_NPE_CHECKS
#define CN1_INCLUDE_ARRAY_BOUND_CHECKS

// Uncommented by the translator (driven by the cn1.onDeviceDebug system
// property) when an on-device-debug build is requested. Enables per-frame
// locals-address tables, the cn1DebuggerActive hot-path check inside
// __CN1_DEBUG_INFO, and the proxy listener thread. Release builds leave
// this off and pay no overhead.
//#define CN1_ON_DEVICE_DEBUG

#ifdef DEBUG_GC_ALLOCATIONS
#define DEBUG_GC_VARIABLES int line; int className;
#define DEBUG_GC_INIT 0, 0,
#else
#define DEBUG_GC_VARIABLES
#define DEBUG_GC_INIT 
#endif

// THE OBJECT HEADER, in one place: every object struct, every array and every class
// descriptor (a java.lang.Class instance) starts with exactly these members, because
// each is cast to JavaObjectPrototype. Read and write them only through the CN1_OBJ_*
// accessors defined after JavaObjectPrototype.
#define CN1_OBJ_HEADER_FIELDS \
    DEBUG_GC_VARIABLES \
    struct clazz *__codenameOneParentClsReference; \
    int __codenameOneGcMark; \
    int __heapPosition;

/**
 * header file containing global CN1 constants and structs
 */


typedef void               JAVA_VOID;
typedef int                JAVA_BOOLEAN;
typedef int                JAVA_CHAR;
typedef int                JAVA_BYTE;
typedef int                JAVA_SHORT;
typedef int                JAVA_INT;
typedef long long          JAVA_LONG;
typedef float              JAVA_FLOAT;
typedef double             JAVA_DOUBLE;

/* MUST be signed char, not plain char: Java bytes are signed, but bare char is
 * UNSIGNED in the aarch64/arm Linux ABI (it is signed on x86/x64 and on all
 * Apple targets, which is why this never bit the iOS builds). On the Linux
 * arm64 port the unsigned reads broke every negative-byte round-trip -- seen as
 * SimdApiTest's saturating byte add and the allocaByteFilled readback failing
 * deterministically on that leg only. */
typedef signed char       JAVA_ARRAY_BYTE;
typedef char              JAVA_ARRAY_BOOLEAN;
typedef unsigned short    JAVA_ARRAY_CHAR;
typedef short             JAVA_ARRAY_SHORT;
typedef int               JAVA_ARRAY_INT;
typedef long long         JAVA_ARRAY_LONG;
typedef float             JAVA_ARRAY_FLOAT;
typedef double            JAVA_ARRAY_DOUBLE;

typedef struct JavaArrayPrototype*               JAVA_ARRAY;
typedef struct JavaObjectPrototype*              JAVA_OBJECT;

typedef JAVA_OBJECT       JAVA_ARRAY_OBJECT;

#define cn1_array_1_id_JAVA_BOOLEAN (cn1_array_start_offset + 1)
#define cn1_array_2_id_JAVA_BOOLEAN (cn1_array_start_offset + 2)
#define cn1_array_3_id_JAVA_BOOLEAN (cn1_array_start_offset + 3)

#define cn1_array_1_id_JAVA_CHAR (cn1_array_start_offset + 5)
#define cn1_array_2_id_JAVA_CHAR (cn1_array_start_offset + 6)
#define cn1_array_3_id_JAVA_CHAR (cn1_array_start_offset + 7)

#define cn1_array_1_id_JAVA_BYTE (cn1_array_start_offset + 9)
#define cn1_array_2_id_JAVA_BYTE (cn1_array_start_offset + 10)
#define cn1_array_3_id_JAVA_BYTE (cn1_array_start_offset + 11)

#define cn1_array_1_id_JAVA_SHORT (cn1_array_start_offset + 13)
#define cn1_array_2_id_JAVA_SHORT (cn1_array_start_offset + 14)
#define cn1_array_3_id_JAVA_SHORT (cn1_array_start_offset + 15)

#define cn1_array_1_id_JAVA_INT (cn1_array_start_offset + 17)
#define cn1_array_2_id_JAVA_INT (cn1_array_start_offset + 18)
#define cn1_array_3_id_JAVA_INT (cn1_array_start_offset + 19)

#define cn1_array_1_id_JAVA_LONG (cn1_array_start_offset + 21)
#define cn1_array_2_id_JAVA_LONG (cn1_array_start_offset + 22)
#define cn1_array_3_id_JAVA_LONG (cn1_array_start_offset + 23)

#define cn1_array_1_id_JAVA_FLOAT (cn1_array_start_offset + 25)
#define cn1_array_2_id_JAVA_FLOAT (cn1_array_start_offset + 26)
#define cn1_array_3_id_JAVA_FLOAT (cn1_array_start_offset + 27)

#define cn1_array_1_id_JAVA_DOUBLE (cn1_array_start_offset + 29)
#define cn1_array_2_id_JAVA_DOUBLE (cn1_array_start_offset + 30)
#define cn1_array_3_id_JAVA_DOUBLE (cn1_array_start_offset + 31)

struct CN1ThreadData {
    pthread_mutex_t __codenameOneMutex;
    pthread_cond_t __codenameOneCondition;
    JAVA_LONG ownerThread;
    int counter;
};

struct clazz {
    // A class descriptor is a java.lang.Class instance, so it starts with an object header.
    CN1_OBJ_HEADER_FIELDS

    void* finalizerFunction;
    void* releaseFieldsFunction;
    void* markFunction;
    
    JAVA_BOOLEAN initialized;
    int classId;
    const char* clsName;
    const JAVA_BOOLEAN isArray;
    
    // array type dimensions
    int dimensions;
    
    // array internal type
    struct clazz* arrayType;  // <---- The component type for an array class. 0 for scalars.
    JAVA_BOOLEAN primitiveType;
    
    const struct clazz* baseClass;
    const struct clazz** baseInterfaces;
    const int baseInterfaceCount;
    
    void* newInstanceFp;
    
    // virtual method table lookup
    void** vtable;
    
    void* enumValueOfFp;
    JAVA_BOOLEAN isSynthetic;
    JAVA_BOOLEAN isInterface;
    JAVA_BOOLEAN isAnonymous;
    JAVA_BOOLEAN isAnnotation;
    
    struct clazz* arrayClass;  // <----- The array type for a class.  if clazz=Object, then class->arrayClass=Object[]

    // TRAILING field on purpose: every generated clazz initializer is positional and
    // does not name it, so C zero-fills it -- no translator emission change needed.
    // Set once by cn1GcRegisterClazz when the first object of this class is allocated;
    // the conservative GC's mark guard then recognises the clazz ADDRESS as genuine via
    // an exact registry instead of a distance heuristic (see gcMarkObject). Only
    // meaningful under CN1_CONSERVATIVE_GC_ROOTS; stays zero otherwise.
    JAVA_BOOLEAN cn1ClazzRegistered;
#ifdef CN1_ALLOC_CENSUS
    // TRAILING for the same reason as cn1ClazzRegistered above: the generated
    // clazz initializers are positional and never name these, so C zero-fills
    // them and no translator change is needed. Plain non-atomic counters -- this
    // is a diagnostic build only, and an increment lost to a race costs nothing
    // a census is meant to answer.
    long cn1AllocCount;
    long cn1AllocBytes;
#endif
};

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// Registers a clazz address in the GC's exact clazz registry (idempotent, lock-free).
// Called on the first allocation of each class from every allocation entry point, so
// by construction every clazz that ever produced an object -- including objects later
// made immortal / removed from the heap table -- is registered before the GC can
// encounter one of its instances.
extern void cn1GcRegisterClazz(struct clazz* c);
#define CN1_CLAZZ_REGISTER(cptr) do { \
        if((cptr) != 0 && __builtin_expect(!(cptr)->cn1ClazzRegistered, 0)) { \
            cn1GcRegisterClazz((cptr)); \
        } \
    } while(0)
#else
#define CN1_CLAZZ_REGISTER(cptr) do {} while(0)
#endif

// Allocation volume BY CLASS. "The heap is 60MB" names nothing anyone can act
// on; "3.1MB of java_lang_Long" names a fix. Counted at every allocation path
// (both inline BiBOP bump paths and the out-of-line codenameOneGcMalloc), so
// unlike a walk of allObjectsInHeap it does not silently miss BiBOP objects --
// which is precisely where small, high-churn objects such as boxed values live.
#ifdef CN1_ALLOC_CENSUS
#define CN1_ALLOC_CENSUS_COUNT(cptr, sz) do { \
        struct clazz* __cc = (struct clazz*)(cptr); \
        if(__cc != 0) { __cc->cn1AllocCount++; __cc->cn1AllocBytes += (long)(sz); } \
    } while(0)
#else
#define CN1_ALLOC_CENSUS_COUNT(cptr, sz) do {} while(0)
#endif

#define EMPTY_INTERFACES ((const struct clazz**)0)

struct JavaObjectPrototype {
    CN1_OBJ_HEADER_FIELDS
};

// OBJECT HEADER ACCESS. Every read and write of an object's class and heap position goes
// through these, never through the members, so the header's layout is decided in one
// place. A read is an rvalue (the cast), so it cannot be assigned through by accident;
// writes use the SET forms. Static objects initialize their header with
// CN1_OBJ_HEADER_INIT. `o` is any object or array pointer.
#define CN1_OBJ_CLASS(o)            ((struct clazz*)((const struct JavaObjectPrototype*)(o))->__codenameOneParentClsReference)
#define CN1_OBJ_SET_CLASS(o, c)     (((struct JavaObjectPrototype*)(o))->__codenameOneParentClsReference = (c))
#define CN1_OBJ_HEAPPOS(o)          ((int)((const struct JavaObjectPrototype*)(o))->__heapPosition)
#define CN1_OBJ_SET_HEAPPOS(o, v)   (((struct JavaObjectPrototype*)(o))->__heapPosition = (v))
#define CN1_OBJ_HEAPPOS_PTR(o)      (&((struct JavaObjectPrototype*)(o))->__heapPosition)
#define CN1_OBJ_HEADER_INIT(cls)    .__codenameOneParentClsReference = (cls)
// THE MARK WORD STORES AN ENCODED EPOCH.
//
// The collector reasons in CYCLES: currentGcMarkValue counts up from 1 and never wraps in
// practice, and every epoch it keeps outside an object -- page and thread bookkeeping,
// cn1GcReclaimedBefore, the aging slack arithmetic -- is a cycle number compared with <.
// An object's mark does not need that range. What is stored is the cycle modulo
// CN1_GC_EPOCH_WINDOW (plus one, so 0 stays "never marked"), and a read turns it back into
// a cycle by taking its age relative to the current one. That round trip is exact while
// no mark that is READ is a full window old, which holds because every live object is
// re-marked at least once per full cycle (every concurrent cycle, every generational
// major), and a dead object's mark, which can sit unswept on an owned page or a page the
// sweep's shortcuts skip, is relabelled CN1_GC_MARK_ANCIENT every quarter window
// (cn1BibopRelabelStale).
//
// Negative values are sentinels and are stored as themselves: -1 fresh, the free and
// quarantine marks, the debug poison marks, and ANCIENT, which decodes to a cycle older
// than any the collector compares against, so every rule reads it as long dead.
//
// Code outside the collector reads marks only through CN1_OBJ_MARK / CN1_OBJ_MARK_LOAD
// (decoded) and writes them through CN1_OBJ_SET_MARK / CN1_OBJ_MARK_STORE (encoded). The
// raw word is compared directly only against an encoded epoch (cn1GcFieldMarkEpoch).
#ifndef CN1_GC_EPOCH_WINDOW
#define CN1_GC_EPOCH_WINDOW (1 << 30)
#endif
#define CN1_GC_MARK_FRESH           (-1)
#define CN1_GC_MARK_ANCIENT         (-2)
#define CN1_GC_ANCIENT_CYCLE        (-(1 << 29))
extern int currentGcMarkValue;
static inline __attribute__((always_inline)) int cn1GcMarkEncode(int v) {
    if(v <= 0) {
        return v == CN1_GC_ANCIENT_CYCLE ? CN1_GC_MARK_ANCIENT : v;
    }
    return 1 + (int)((unsigned)v % (unsigned)CN1_GC_EPOCH_WINDOW);
}
static inline __attribute__((always_inline)) int cn1GcMarkDecode(int raw) {
    if(raw <= 0) {
        return raw == CN1_GC_MARK_ANCIENT ? CN1_GC_ANCIENT_CYCLE : raw;
    }
    int cur = __atomic_load_n(&currentGcMarkValue, __ATOMIC_RELAXED);
    int age = cn1GcMarkEncode(cur) - raw;
    if(age < 0) {
        age += CN1_GC_EPOCH_WINDOW;
    }
    return cur - age;
}
#define CN1_OBJ_MARK_PTR(o)         (&((struct JavaObjectPrototype*)(o))->__codenameOneGcMark)
#define CN1_OBJ_MARK(o)             cn1GcMarkDecode(((const struct JavaObjectPrototype*)(o))->__codenameOneGcMark)
#define CN1_OBJ_SET_MARK(o, v)      (((struct JavaObjectPrototype*)(o))->__codenameOneGcMark = cn1GcMarkEncode(v))
#define CN1_OBJ_MARK_LOAD(o, ord)   cn1GcMarkDecode(__atomic_load_n(CN1_OBJ_MARK_PTR(o), (ord)))
#define CN1_OBJ_MARK_STORE(o, v, ord) __atomic_store_n(CN1_OBJ_MARK_PTR(o), cn1GcMarkEncode(v), (ord))

// THE ARRAY HEADER IS 32 BYTES, AND EIGHT OF THOSE WERE PURE PADDING PLUS SLACK.
//
// Array headers are the single largest line item in this VM's allocation volume: on the
// self-hosting corpus 551,580 of the 1,401,234 objects allocated per GC cycle are arrays,
// so at the previous 40-byte header they were 21.0MB of the 105.3MB allocated per cycle --
// TWENTY PERCENT of every byte this VM allocates, before a single element of payload.
//
// dimensions is at most 4 and primitiveSize at most 8 (sizeof(JAVA_ARRAY_DOUBLE) and
// sizeof(JAVA_OBJECT)); both were `int`. Narrowing them to a byte each lets `data` sit in
// the 4 bytes of tail padding the old layout already wasted, taking the header 40 -> 32.
//
// NARROWED RATHER THAN REMOVED, ON PURPOSE. Both values are derivable from the array's
// clazz (which carries its own `dimensions`, and whose component type fixes the element
// size), so deleting them outright would save the same 8 bytes -- and would break every
// native that reads `arr->primitiveSize`, including two in the iOS port, the on-device
// debugger, and any cn1lib's natives, which are not ours to break. Keeping the field
// NAMES keeps all of that source compiling and reading the correct value through integer
// promotion; only code taking their address or assuming sizeof(int) is affected, and
// there is none.
//
// THE PREFIX IS LOAD-BEARING: the first three members must match JavaObjectPrototype
// member for member, because array and object pointers are cast to each other throughout
// the collector. `length` keeps its offset too, which every generated bounds check reads.
// The static assertions below hold all of that, because none of it was checked before and
// all of it is silent when wrong -- a mis-set primitiveSize is a wrong element stride,
// which reads and writes past the end of the payload with nothing thrown.
struct JavaArrayPrototype {
    CN1_OBJ_HEADER_FIELDS
    int length;
    unsigned char dimensions;
    unsigned char primitiveSize;
    /* Byte distance from THIS HEADER to the payload, not a pointer to it.
     *
     * The pointer was 8 of the header's 32 bytes, and this header is 20% of the
     * VM's allocation volume -- so it was 5% of everything the collector ever has
     * to sweep, spent re-storing a value that is a small constant away from the
     * object it sits in. An offset makes the header 24 bytes:
     *
     *     16 object header | length 4 | dimensions 1 | primitiveSize 1 | offset 2
     *
     * and turns every array access from a dependent load into an add, which the
     * address-generation unit does for free alongside the index arithmetic.
     *
     * Two bytes rather than one because the offset is not always
     * CN1_ARRAY_PAYLOAD_OFFSET: allocArrayAligned and the SIMD stack path round the
     * payload start up to an alignment that is a PARAMETER, so the distance can be
     * the header plus up to alignment-1. One byte covers alignments to 224 and
     * silently truncates above that; two covers any alignment this VM could ask for
     * and costs nothing, because dimensions and primitiveSize leave exactly two
     * bytes before the next 4-byte boundary.
     *
     * Read it through CN1_ARRAY_DATA, never directly. */
    unsigned short dataOffset;
};

#ifndef DEBUG_GC_ALLOCATIONS
/* DEBUG_GC_VARIABLES adds two ints to BOTH structs, so the prefix assertions below hold
   in that configuration too, but the absolute sizes do not -- hence the guard. */
_Static_assert(sizeof(struct JavaArrayPrototype) == 24,
               "array header must stay 24 bytes; it is 20% of this VM's allocation volume");
/* The payload sits at sizeof(header) from the base, so a long[] or double[] element
 * is 8-aligned only if that offset is a multiple of 8. 24 is; 20 or 28 would not be,
 * and the failure would be a misaligned 64-bit load on some targets and a silent
 * performance cliff on the rest. */
_Static_assert(sizeof(struct JavaArrayPrototype) % 8 == 0,
               "array payload offset must stay 8-aligned for long[] and double[]");
_Static_assert(sizeof(struct JavaObjectPrototype) == 16, "object header must stay 16 bytes");
#endif
_Static_assert(offsetof(struct JavaArrayPrototype, __codenameOneParentClsReference)
               == offsetof(struct JavaObjectPrototype, __codenameOneParentClsReference),
               "array and object headers are cast to each other; the class pointer must align");
_Static_assert(offsetof(struct JavaArrayPrototype, __codenameOneGcMark)
               == offsetof(struct JavaObjectPrototype, __codenameOneGcMark),
               "array and object headers are cast to each other; the mark word must align");
_Static_assert(offsetof(struct JavaArrayPrototype, __heapPosition)
               == offsetof(struct JavaObjectPrototype, __heapPosition),
               "array and object headers are cast to each other; heapPosition must align");
_Static_assert(offsetof(struct JavaArrayPrototype, dataOffset) + sizeof(unsigned short)
               == sizeof(struct JavaArrayPrototype),
               "dataOffset must stay the LAST header member: allocArray sizes the block "
               "from sizeof(struct) while every placement site computes the payload "
               "address from the same value, and the two must be the same byte");

/* The payload of an array that already exists. Equals CN1_ARRAY_PAYLOAD_PTR for every
 * ordinarily allocated array, and differs only for the aligned and stack paths, which
 * push the start up to an alignment boundary. This replaced a `data` pointer field --
 * see the comment on dataOffset. */
#define CN1_ARRAY_DATA(a) ((void*)((char*)(a) + ((JAVA_ARRAY)(a))->dataOffset))

typedef union {
    JAVA_OBJECT  o;
    JAVA_INT     i;
    JAVA_FLOAT   f;
    JAVA_DOUBLE  d;
    JAVA_LONG    l;
} elementUnion;

#define CODENAME_ONE_ASSERT(assertion) assert(assertion)

typedef enum {
    CN1_TYPE_INVALID, CN1_TYPE_OBJECT, CN1_TYPE_INT, CN1_TYPE_FLOAT, CN1_TYPE_DOUBLE, CN1_TYPE_LONG, CN1_TYPE_PRIMITIVE
} javaTypes;

// type must be first so memsetting will first reset the type then the data preventing the GC
// from mistakingly detecting an object
struct elementStruct {
    javaTypes type;
    elementUnion data;
};


typedef struct clazz*       JAVA_CLASS;

#define JAVA_NULL ((JAVA_OBJECT) 0)

#define JAVA_FALSE ((JAVA_BOOLEAN) 0)
#define JAVA_TRUE ((JAVA_BOOLEAN) 1)


#define BC_ILOAD(local) { \
    (*SP).type = CN1_TYPE_INT; \
    (*SP).data.i = ilocals_##local##_; \
    SP++; \
}

#define BC_LLOAD(local) { \
    (*SP).type = CN1_TYPE_LONG; \
    (*SP).data.l = llocals_##local##_; \
    SP++; \
}

#define BC_FLOAD(local) { \
    (*SP).type = CN1_TYPE_FLOAT; \
    (*SP).data.f = flocals_##local##_; \
    SP++; \
}

#define BC_DLOAD(local) { \
    (*SP).type = CN1_TYPE_DOUBLE; \
    (*SP).data.d = dlocals_##local##_; \
    SP++; \
}

#define BC_ALOAD(local) { \
    (*SP).type = CN1_TYPE_INVALID; \
    (*SP).data.o = locals[local].data.o; \
    (*SP).type = CN1_TYPE_OBJECT; \
    SP++; \
}


#define BC_ISTORE(local) { SP--; \
    ilocals_##local##_ = (*SP).data.i; \
    }

#define BC_LSTORE(local) { SP--; \
    llocals_##local##_ = (*SP).data.l; \
    }

#define BC_FSTORE(local) { SP--; \
    flocals_##local##_ = (*SP).data.f; \
    }

#define BC_DSTORE(local) { SP--; \
    dlocals_##local##_ = (*SP).data.d; \
    }

#define BC_ASTORE(local) { SP--; \
    locals[local].type = CN1_TYPE_INVALID; \
    locals[local].data.o = (*SP).data.o; \
    locals[local].type = CN1_TYPE_OBJECT; \
    }

// todo map instanceof and throw typecast exception
// CHECKCAST is a no-op by default: ParparVM has always let a failed cast through,
// so the wrong object reaches the next instruction and the target type's fields
// get read out of it (issue #5531). That is a native crash no Java catch can see.
//
// BC_CHECKCAST_CHECKED is the enforcing form. The translator emits it in place of
// BC_CHECKCAST only when -Dcn1.checkedCasts=true, and that same flag is what makes
// the translator retain java.lang.ClassCastException -- so the emission and the
// class's survival can never disagree and leave an unresolved symbol. Enforcement
// is opt-in rather than the default because turning it on changes the outcome of
// app builds that succeed today; server-side (clean-target) builds, which parse
// untrusted input, should always turn it on.
//
// The cost is one instanceofFunction call, the same check INSTANCEOF already pays.
#define BC_CHECKCAST(type)
// AASTORE's companion hole: the array store is only bounds-checked, never
// covariance-checked, so `Object[] o = new String[1]; o[0] = anInteger;` silently
// stores the wrong type and the next reader gets an Integer where it expects a
// String. Emitted by BasicInstruction under the same -Dcn1.checkedCasts flag that
// drives BC_CHECKCAST_CHECKED, so ArrayStoreException's retention and the check's
// emission cannot disagree.
//
/* The arrayObj null test is not redundant. This runs BEFORE
   CN1_SET_ARRAY_ELEMENT_OBJECT, which is where a null array is turned into a
   NullPointerException; CN1_CLASS_OF below would dereference the null first and
   take the process down instead. Java also orders it this way -- NPE wins over
   ArrayStoreException -- so falling through to the setter is both safe and
   correct.

   ONE DIMENSION ONLY, and that restriction is load-bearing. arrayType is NOT the
   immediate component type: a generated array class records the BASE element class,
   so String[][] has dimensions 2 and arrayType String rather than String[]. Asking
   whether a String[] is an instance of String is the wrong question and answers no,
   so without the dimensions test this REJECTED valid stores into every
   multidimensional array. Skipping them is the conservative direction -- a genuine
   ArrayStoreException there goes unreported, exactly as it did before this check
   existed, where the alternative was breaking correct programs. Covering them needs
   the immediate component type, which means emitting it per array class or
   reconstructing it from dimensions at runtime. */
#define CN1_ARRAY_STORE_CHECK(arrayObj, value) { \
    if((value) != JAVA_NULL && (arrayObj) != JAVA_NULL \
            && CN1_CLASS_OF(arrayObj)->dimensions == 1) { \
        struct clazz* cn1__comp = CN1_CLASS_OF(arrayObj)->arrayType; \
        /* Constructed inline rather than behind a cold helper like the others:
         * java_lang_ArrayStoreException is culled from any application that never
         * stores into a reference array, so a reference to it from cn1_globals.m --
         * which every application links -- fails to link for those. The macro only
         * expands where an aastore exists, which is exactly where the class is kept
         * alive. The branch hint is the part that carries over. */ \
        if(__builtin_expect(cn1__comp != NULL && !instanceofFunction(cn1__comp->classId, GET_CLASS_ID(value)), 0)) { \
            cn1ThrowTypeError(threadStateData, __NEW_INSTANCE_java_lang_ArrayStoreException(threadStateData), CN1_CLASS_OF(value)->clsName, NULL); \
        } \
    } \
}

#define BC_CHECKCAST_CHECKED(typeOfCheckCast, targetName) { \
    if(SP[-1].data.o != JAVA_NULL) { \
        int tmpCheckCastId = GET_CLASS_ID(SP[-1].data.o); \
        if(!instanceofFunction(typeOfCheckCast, tmpCheckCastId)) { \
            cn1ThrowTypeError(threadStateData, __NEW_INSTANCE_java_lang_ClassCastException(threadStateData), CN1_CLASS_OF(SP[-1].data.o)->clsName, targetName); \
        } \
    } \
}

#define BC_SWAP() swapStack(SP)


// (*--SP) rather than (*pop(&SP)), which is the same decrement-then-dereference
// and avoids taking SP's address. A method that emits setjmp declares SP volatile
// so longjmp cannot clobber it (see CN1_DECLARE_SP); &SP is then
// `struct elementStruct *volatile *`, and pop() takes `struct elementStruct **`,
// so every pop in such a method passed a pointer that discarded the very
// qualifier the frame variant exists to apply. That was ~9,100 warnings in one
// application build, and it was the compiler being right: writing through the
// unqualified pointer is not the volatile access the declaration asked for.
#define POP_INT() ((*--SP).data.i)
#define POP_OBJ() ((*--SP).data.o)
#define POP_OBJ_NO_RELEASE() ((*--SP).data.o)
#define POP_LONG() ((*--SP).data.l)
#define POP_DOUBLE() ((*--SP).data.d)
#define POP_FLOAT() ((*--SP).data.f)

#define PEEK_INT(offset) SP[-offset].data.i
#define PEEK_OBJ(offset) SP[-offset].data.o
#define PEEK_LONG(offset) SP[-offset].data.l
#define PEEK_DOUBLE(offset) SP[-offset].data.d
#define PEEK_FLOAT(offset) SP[-offset].data.f

// Value in, value out, for the same reason as the POP_* macros above: popMany
// has to move SP by an amount it computes from the slot types, and taking &SP
// discarded volatile on the frames that declare it.
#define POP_MANY(offset) (SP = cn1PopMany(threadStateData, offset, SP))

#define BC_IADD() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i + (*SP).data.i; \
}

#define BC_LADD() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l + (*SP).data.l; \
}

#define BC_FADD() { \
    SP--; \
    SP[-1].data.f = SP[-1].data.f + (*SP).data.f; \
}

#define BC_DADD() { \
    SP--; \
    SP[-1].data.d = SP[-1].data.d + (*SP).data.d; \
}

#define BC_IMUL() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i * (*SP).data.i; \
}

#define BC_LMUL() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l * (*SP).data.l; \
}

#define BC_FMUL() { \
    SP--; \
    SP[-1].data.f = SP[-1].data.f * (*SP).data.f; \
}

#define BC_DMUL() { \
    SP--; \
    SP[-1].data.d = SP[-1].data.d * (*SP).data.d; \
}

#define BC_INEG() SP[-1].data.i *= -1

#define BC_LNEG() SP[-1].data.l *= -1

#define BC_FNEG() SP[-1].data.f *= -1

#define BC_DNEG() SP[-1].data.d *= -1

#define BC_IAND() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i & (*SP).data.i; \
}

#define BC_LAND() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l & (*SP).data.l; \
}

#define BC_IOR() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i | (*SP).data.i; \
}

#define BC_LOR() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l | (*SP).data.l; \
}

#define BC_IXOR() { \
    SP--; \
    SP[-1].data.i = SP[-1].data.i ^ (*SP).data.i; \
}

#define BC_LXOR() { \
    SP--; \
    SP[-1].data.l = SP[-1].data.l ^ (*SP).data.l; \
}

// Conversion macros must rewrite the runtime type tag too. BC_DUP2_X1 /
// BC_DUP2_X2 / BC_DUP_X2 dispatch via IS_DOUBLE_WORD on the tag, so a stale
// tag corrupts the stack on chained assignments (issue #3108).
#define BC_I2L() do { SP[-1].data.l = SP[-1].data.i; SP[-1].type = CN1_TYPE_LONG; } while(0)

#define BC_L2I() do { SP[-1].data.i = (JAVA_INT)SP[-1].data.l; SP[-1].type = CN1_TYPE_INT; } while(0)

#define BC_L2F() do { SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.l; SP[-1].type = CN1_TYPE_FLOAT; } while(0)

#define BC_L2D() do { SP[-1].data.d = (JAVA_DOUBLE)SP[-1].data.l; SP[-1].type = CN1_TYPE_DOUBLE; } while(0)

#define BC_I2F() do { SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.i; SP[-1].type = CN1_TYPE_FLOAT; } while(0)

// JLS 5.1.3: narrowing a float or double to an int or long SATURATES -- NaN becomes 0,
// anything at or above the target's maximum becomes MAX_VALUE, anything at or below its
// minimum becomes MIN_VALUE. C's cast is UNDEFINED out of range instead, and the two
// architectures shipped here disagree about what it actually does: arm64's fcvtzs
// saturates, so this was accidentally correct on Apple silicon, while x86-64's cvttsd2si
// yields the "integer indefinite" value 0x80000000 -- so (int) 1.7976931348623157E308 came
// out as Integer.MIN_VALUE rather than Integer.MAX_VALUE on every x86 target.
//
// The thresholds are compared in DOUBLE, and float goes through the double form because
// float-to-double is exact and lossless. Note 2147483647.0 is exactly representable so the
// int bounds are exact, while Long.MAX_VALUE is NOT -- 9223372036854775808.0 is 2^63, the
// first double above it, which is why the upper long test is >= that rather than > it.
#define CN1_D2L_LIMIT 9223372036854775808.0

// -DCN1_NO_SATURATING_NARROWING restores the old undefined cast. All three emission sites
// route through these two functions, so that one macro ablates the whole change and an A/B
// needs no second translator build -- which matters, because this host cannot resolve a 5%
// difference across sessions and the arms have to be interleaved inside one.
static inline JAVA_INT cn1SaturateToInt(JAVA_DOUBLE cn1__d) {
#ifdef CN1_NO_SATURATING_NARROWING
    return (JAVA_INT)cn1__d;
#else
    if(cn1__d != cn1__d) return 0;
    if(cn1__d >= 2147483647.0) return (JAVA_INT)2147483647;
    if(cn1__d <= -2147483648.0) return (JAVA_INT)(-2147483647 - 1);
    return (JAVA_INT)cn1__d;
#endif
}

static inline JAVA_LONG cn1SaturateToLong(JAVA_DOUBLE cn1__d) {
#ifdef CN1_NO_SATURATING_NARROWING
    return (JAVA_LONG)cn1__d;
#else
    if(cn1__d != cn1__d) return 0;
    if(cn1__d >= CN1_D2L_LIMIT) return (JAVA_LONG)9223372036854775807LL;
    if(cn1__d <= -CN1_D2L_LIMIT) return (JAVA_LONG)(-9223372036854775807LL - 1);
    return (JAVA_LONG)cn1__d;
#endif
}

#define BC_F2I() do { SP[-1].data.i = cn1SaturateToInt((JAVA_DOUBLE)SP[-1].data.f); SP[-1].type = CN1_TYPE_INT; } while(0)

#define BC_F2L() do { SP[-1].data.l = cn1SaturateToLong((JAVA_DOUBLE)SP[-1].data.f); SP[-1].type = CN1_TYPE_LONG; } while(0)

#define BC_F2D() do { SP[-1].data.d = SP[-1].data.f; SP[-1].type = CN1_TYPE_DOUBLE; } while(0)

#define BC_D2I() do { SP[-1].data.i = cn1SaturateToInt(SP[-1].data.d); SP[-1].type = CN1_TYPE_INT; } while(0)

#define BC_D2L() do { SP[-1].data.l = cn1SaturateToLong(SP[-1].data.d); SP[-1].type = CN1_TYPE_LONG; } while(0)

#define BC_I2D() do { SP[-1].data.d = SP[-1].data.i; SP[-1].type = CN1_TYPE_DOUBLE; } while(0)

#define BC_D2F() do { SP[-1].data.f = (JAVA_FLOAT)SP[-1].data.d; SP[-1].type = CN1_TYPE_FLOAT; } while(0)

#ifdef CN1_INCLUDE_NPE_CHECKS
#define BC_ARRAYLENGTH() { \
    if(__builtin_expect(SP[-1].data.o == JAVA_NULL, 0)) { \
        cn1ThrowNullPointerHere(threadStateData); \
    }; \
    SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = (*((JAVA_ARRAY)SP[-1].data.o)).length; \
}
#else
#define BC_ARRAYLENGTH() { \
    SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = (*((JAVA_ARRAY)SP[-1].data.o)).length; \
}
#endif

#define BC_IF_ICMPEQ() SP-=2; if((*SP).data.i == SP[1].data.i)

#define BC_IF_ICMPNE() SP-=2; if((*SP).data.i != SP[1].data.i)

#define BC_IF_ICMPLT() SP-=2; if((*SP).data.i < SP[1].data.i)

#define BC_IF_ICMPGE() SP-=2; if((*SP).data.i >= SP[1].data.i)

#define BC_IF_ICMPGT() SP-=2; if((*SP).data.i > SP[1].data.i)

#define BC_IF_ICMPLE() SP-=2; if((*SP).data.i <= SP[1].data.i)

#define BC_IF_ACMPEQ() SP-=2; if((*SP).data.o == SP[1].data.o)

#define BC_IF_ACMPNE() SP-=2; if((*SP).data.o != SP[1].data.o)

//#define POP_TYPE(type) (*((type*)POP_OBJ()))

// we assign the value to trigger the expression in the macro
// then set the type to invalid first so we don't get a race condition where the value is
// incomplete and the GC goes crazy
#define PUSH_POINTER(value) { JAVA_OBJECT ppX = value; (*SP).type = CN1_TYPE_INVALID; \
    (*SP).data.o = ppX; (*SP).type = CN1_TYPE_OBJECT; \
    SP++; }

#define PUSH_OBJ(value)  { JAVA_OBJECT ppX = value; (*SP).type = CN1_TYPE_INVALID; \
    (*SP).data.o = ppX; (*SP).type = CN1_TYPE_OBJECT; \
    SP++; }

#define PUSH_INT(value) { JAVA_INT pInt = value; (*SP).type = CN1_TYPE_INT; \
    (*SP).data.i = pInt; \
    SP++; }

#define PUSH_LONG(value) { JAVA_LONG plong = value; (*SP).type = CN1_TYPE_LONG; \
    (*SP).data.l = plong; \
    SP++; }

#define PUSH_DOUBLE(value) { JAVA_DOUBLE pdob = value; (*SP).type = CN1_TYPE_DOUBLE; \
    (*SP).data.d = pdob; \
    SP++; }

#define PUSH_FLOAT(value) { JAVA_FLOAT pFlo = value; (*SP).type = CN1_TYPE_FLOAT; \
    (*SP).data.f = pFlo; \
    SP++; }

#define POP_MANY_AND_PUSH_OBJ(value, offset) {  \
    JAVA_OBJECT pObj = value; SP[-offset].type = CN1_TYPE_INVALID; \
    SP[-offset].data.o = pObj; SP[-offset].type = CN1_TYPE_OBJECT; \
    SP = cn1PopMany(threadStateData, MAX(1, offset) - 1, SP); }

#define POP_MANY_AND_PUSH_INT(value, offset) {  \
    JAVA_INT pInt = value; SP[-offset].type = CN1_TYPE_INT; \
    SP[-offset].data.i = pInt; \
    SP = cn1PopMany(threadStateData, MAX(1, offset) - 1, SP); }

#define POP_MANY_AND_PUSH_LONG(value, offset) {  \
    JAVA_LONG pLong = value; SP[-offset].type = CN1_TYPE_LONG; \
    SP[-offset].data.l = pLong; \
    SP = cn1PopMany(threadStateData, MAX(1, offset) - 1, SP); }

#define POP_MANY_AND_PUSH_DOUBLE(value, offset) {  \
    JAVA_DOUBLE pDob = value; SP[-offset].type = CN1_TYPE_DOUBLE; \
    SP[-offset].data.d = pDob; \
    SP = cn1PopMany(threadStateData, MAX(1, offset) - 1, SP); }

#define POP_MANY_AND_PUSH_FLOAT(value, offset) {  \
    JAVA_FLOAT pFlo = value; SP[-offset].type = CN1_TYPE_FLOAT; \
    SP[-offset].data.f = pFlo; \
    SP = cn1PopMany(threadStateData, MAX(1, offset) - 1, SP); }


#define BC_IDIV() SP--; SP[-1].data.i = SP[-1].data.i / (*SP).data.i

#define BC_LDIV() SP--; SP[-1].data.l = SP[-1].data.l / (*SP).data.l

#define BC_FDIV() SP--; SP[-1].data.f = SP[-1].data.f / (*SP).data.f

#define BC_DDIV() SP--; SP[-1].data.d = SP[-1].data.d / (*SP).data.d

#define BC_IREM() SP--; SP[-1].data.i = SP[-1].data.i % (*SP).data.i

#define BC_LREM() SP--; SP[-1].data.l = SP[-1].data.l % (*SP).data.l

#define BC_FREM() SP--; SP[-1].data.f = fmod(SP[-1].data.f, (*SP).data.f)

#define BC_DREM() SP--; SP[-1].data.d = fmod(SP[-1].data.d, (*SP).data.d)

#define BC_LCMP() SP--; if(SP[-1].data.l == (*SP).data.l) { \
        SP[-1].data.i = 0; \
    } else { \
        if(SP[-1].data.l > (*SP).data.l) { \
            SP[-1].data.i = 1; \
        } else { \
            SP[-1].data.i = -1; \
        } \
    } \
    SP[-1].type = CN1_TYPE_INT;

#define BC_FCMPL() SP--; if(SP[-1].data.f == (*SP).data.f) { \
        SP[-1].data.i = 0; \
    } else { \
        if(SP[-1].data.f > (*SP).data.f) { \
            SP[-1].data.i = 1; \
        } else { \
            SP[-1].data.i = -1; \
        } \
    } \
    SP[-1].type = CN1_TYPE_INT;

#define BC_DCMPL() SP--; if(SP[-1].data.d == (*SP).data.d) { \
        SP[-1].data.i = 0; \
    } else { \
        if(SP[-1].data.d > (*SP).data.d) { \
            SP[-1].data.i = 1; \
        } else { \
            SP[-1].data.i = -1; \
        } \
    } \
    SP[-1].type = CN1_TYPE_INT;

#define CN1_CMP_EXPR(val1, val2) ((val1 == val2) ? 0 : (val1 > val2) ? 1 :  -1)

#define BC_DUP()  { \
        JAVA_LONG plong = SP[-1].data.l; \
        (*SP).type = CN1_TYPE_INVALID; \
        (*SP).data.l = plong; (*SP).type = CN1_TYPE_LONG; \
        SP++; \
    } \
    SP[-1].type = SP[-2].type; 

#define BC_DUP2()  \
if(SP[-1].type == CN1_TYPE_LONG || SP[-1].type == CN1_TYPE_DOUBLE) {\
    BC_DUP(); \
} else {\
    { \
        JAVA_LONG plong = SP[-2].data.l; \
        JAVA_LONG plong2 = SP[-1].data.l; \
        (*SP).type = CN1_TYPE_INVALID; \
        SP[1].type = CN1_TYPE_INVALID; \
        (*SP).data.l = plong; \
        SP[1].data.l = plong2; \
        SP+=2; \
    } \
    SP[-1].type = SP[-3].type; \
    SP[-2].type = SP[-4].type; \
}

#define BC_DUP2_X1() {\
    if (IS_DOUBLE_WORD(-1)){\
        (*SP).data.l = SP[-1].data.l; \
        SP[-1].data.l = SP[-2].data.l; \
        SP[-2].data.l = (*SP).data.l; \
        (*SP).type = SP[-1].type; \
        SP[-1].type = SP[-2].type; \
        SP[-2].type = (*SP).type; \
        SP++; \
    } else {\
        SP[1].data.l = SP[-1].data.l; \
        (*SP).data.l = SP[-2].data.l; \
        SP[-1].data.l = SP[-3].data.l; \
        SP[-2].data.l = SP[1].data.l; \
        SP[-3].data.l = (*SP).data.l;\
        SP[1].type = SP[-1].type;\
        (*SP).type = SP[-2].type; \
        SP[-1].type = SP[-3].type; \
        SP[-2].type = SP[1].type; \
        SP[-3].type = (*SP).type;\
        SP+=2;\
    }\
}

#define BC_DUP_X1() {\
    (*SP).data.l = SP[-1].data.l; \
    SP[-1].data.l = SP[-2].data.l; \
    SP[-2].data.l = (*SP).data.l; \
    (*SP).type = SP[-1].type; \
    SP[-1].type = SP[-2].type; \
    SP[-2].type = (*SP).type; \
    SP++; \
}

struct elementStruct* BC_DUP2_X2_DD(struct elementStruct* SP);
struct elementStruct* BC_DUP2_X2_DSS(struct elementStruct* SP);
struct elementStruct* BC_DUP2_X2_SSD(struct elementStruct* SP);
struct elementStruct* BC_DUP2_X2_SSSS(struct elementStruct* SP);
struct elementStruct* BC_DUP_X2_SD(struct elementStruct* SP);
struct elementStruct* BC_DUP_X2_SSS(struct elementStruct* SP);

#define IS_DOUBLE_WORD(offset) (SP[offset].type == CN1_TYPE_LONG || SP[offset].type == CN1_TYPE_DOUBLE)

#define BC_DUP_X2() {\
    if (IS_DOUBLE_WORD(-2)) SP=BC_DUP_X2_SD(SP);\
    else SP=BC_DUP_X2_SSS(SP);\
}

#define BC_DUP2_X2() { \
    if (IS_DOUBLE_WORD(-2)) SP=BC_DUP2_X2_DD(SP);\
else if (IS_DOUBLE_WORD(-1)) SP=BC_DUP2_X2_DSS(SP);\
    else if (IS_DOUBLE_WORD(-3)) SP=BC_DUP2_X2_SSD(SP);\
    else SP=BC_DUP2_X2_SSSS(SP);\
}


#define BC_I2B() SP[-1].data.i = ((SP[-1].data.i << 24) >> 24)

#define BC_I2S() SP[-1].data.i = ((SP[-1].data.i << 16) >> 16)

#define BC_I2C() SP[-1].data.i = (SP[-1].data.i & 0xffff)

// Java defines << as two's-complement wraparound. C does not: shifting a signed
// value left so that the result is not representable -- 1 << 31, or any negative
// left operand -- is UNDEFINED, not merely implementation-defined, so the
// optimizer is entitled to assume it never happens. Shifting through the
// unsigned type of the same width gives Java's answer with no undefined step,
// and the conversion back is the ordinary two's-complement reinterpretation.
//
// The signed RIGHT shifts below are left alone deliberately: a negative >> n is
// implementation-defined rather than undefined, and every compiler this VM is
// built with defines it as the arithmetic shift Java specifies.
#define BC_ISHL() SP--; SP[-1].data.i = (JAVA_INT)(((unsigned int)SP[-1].data.i) << (0x1f & (*SP).data.i))
#define BC_ISHL_EXPR(val1, val2) ((JAVA_INT)(((unsigned int)(JAVA_INT)(val1)) << (0x1f & (val2))))
#define BC_LSHL() SP--; SP[-1].data.l = (JAVA_LONG)(((unsigned long long)SP[-1].data.l) << (0x3f & (*SP).data.l))
/* val1 is CAST, and that cast is the whole point.
 *
 * The translator emits a long constant as a bare C literal, so LCONST_1 reaches
 * here as `BC_LSHL_EXPR(1, n)` -- and in C `1` is an int, which makes this an
 * int shift no matter what the 0x3f mask says. The result was silently wrong for
 * every shift of a long CONSTANT by 31 or more:
 *
 *     1L << 31  gave -2147483648   (int overflow, then sign-extended)
 *     1L << 32  gave 1             (int shift counts are masked to 5 bits)
 *     1L << 33  gave 2
 *
 * `x << n` for a long VARIABLE was always right, which is why this survived: the
 * variable carries JAVA_LONG into the macro and the constant does not. Found by a
 * histogram whose bucket labels came out negative.
 *
 * BC_LUSHR_EXPR below already casts, so this class of bug was fixed once for the
 * unsigned shift and not carried across to its two siblings. */
// The inner JAVA_LONG cast is load-bearing and separate from the unsigned one:
// the translator emits long constants as bare C literals, so without it LCONST_1
// arrives as an int and 1L << 32 evaluates to 1. See the LongShift benchmark.
#define BC_LSHL_EXPR(val1, val2) ((JAVA_LONG)(((unsigned long long)(JAVA_LONG)(val1)) << (0x3f & (val2))))

#define BC_ISHR() SP--; SP[-1].data.i = (SP[-1].data.i >> (0x1f & (*SP).data.i))
#define BC_ISHR_EXPR(val1, val2) (val1 >> (0x1f & val2))

#define BC_LSHR() SP--; SP[-1].data.l = (SP[-1].data.l >> (0x3f & (*SP).data.l))
/* Cast for the same reason as BC_LSHL_EXPR above: a long constant arrives as an
 * int literal and would otherwise be shifted 32 bits wide. */
#define BC_LSHR_EXPR(val1, val2) (((JAVA_LONG)(val1)) >> (0x3f & (val2)))

#define BC_IUSHL() SP--; SP[-1].data.i = (((unsigned int)SP[-1].data.i) << (0x1f & ((unsigned int)(*SP).data.i)))
#define BC_IUSHL_EXPR(val1, val2) (((unsigned int)val1) << (0x1f & ((unsigned int)val2)))

#define BC_LUSHL() SP--; SP[-1].data.l = (((unsigned long long)SP[-1].data.l) << (0x3f & ((unsigned long long)(*SP).data.l)))
#define BC_LUSHL_EXPR(val1, val2) (((unsigned long long)val1) << (0x3f & ((unsigned long long)val2)))

#define BC_IUSHR() SP--; SP[-1].data.i = (((unsigned int)SP[-1].data.i) >> (0x1f & ((unsigned int)(*SP).data.i)))
#define BC_IUSHR_EXPR(val1, val2) (((unsigned int)val1) >> (0x1f & ((unsigned int)val2)))

#define BC_LUSHR() SP--; SP[-1].data.l = (((unsigned long long)SP[-1].data.l) >> (0x3f & ((unsigned long long)(*SP).data.l)))
#define BC_LUSHR_EXPR(val1, val2) (((unsigned long long)val1) >> (0x3f & ((unsigned long long)val2)))

#define BC_ISUB() SP--; SP[-1].data.i = (SP[-1].data.i - (*SP).data.i)

#define BC_LSUB() SP--; SP[-1].data.l = (SP[-1].data.l - (*SP).data.l)

#define BC_FSUB() SP--; SP[-1].data.f = (SP[-1].data.f - (*SP).data.f)

#define BC_DSUB() SP--; SP[-1].data.d = (SP[-1].data.d - (*SP).data.d)

extern JAVA_OBJECT* constantPoolObjects;

extern int classListSize;
extern struct clazz* classesList[];

/**
 * The String object for a literal, materialised on FIRST USE.
 *
 * initConstantPool used to build every literal in the application before main
 * ran. On a large transpiled application that was 38,238 java.lang.String
 * objects and their backing arrays -- 61% of every live object in the process --
 * for an application that touches a few thousand of them: every localisation of
 * every string for every locale, every UIID, every demo description, all
 * allocated, all pinned by the pool's own GC root, none of them ever read.
 *
 * The fast path is a load and a predicted-taken branch, which is what the old
 * macro compiled to anyway. Literal IDENTITY is preserved -- the slow path is
 * serialised and double-checked, so "x" == "x" stays true, which Java requires
 * and generated code relies on.
 */
extern JAVA_OBJECT cn1MaterializeConstantPoolString(int off);
/// Start-up attribution probe; prints elapsed-since-process-start when
/// CN1_STARTUP_PHASES is set, and costs one cached getenv otherwise.
extern void cn1StartupPhase(const char* name);
/// ACQUIRE, paired with the RELEASE store in cn1MaterializeConstantPoolString.
/// The mutex there serialises writers and keeps literal identity, but it
/// establishes nothing with these readers -- they never take it. Without the
/// pairing a thread may observe the published pointer while the String's fields
/// are still invisible to it (a plain concurrent read/write is a data race in
/// any case, and on arm64 it is one that actually reorders).
#define CN1_CONSTANT_POOL_LOAD(off) \
    ((JAVA_OBJECT)__atomic_load_n(&constantPoolObjects[off], __ATOMIC_ACQUIRE))
/// Reads the slot ONCE. The macro this replaces named CN1_CONSTANT_POOL_LOAD
/// twice, and clang cannot fold the pair away: an acquire load is a
/// synchronisation point it will not CSE, and for all the optimiser knows that
/// very acquire synchronises-with a writer to constantPoolObjects itself, so
/// even the base pointer had to be re-loaded. Every string literal in the
/// program therefore paid ldr/add/ldapr twice -- six instructions and two
/// ordering primitives where three and one do.
///
/// Folding them is safe because a slot is written exactly once, null -> object
/// under constantPoolMutex, and never cleared; the base is assigned once during
/// init. The two loads could only ever have returned the same pointer.
static inline JAVA_OBJECT cn1ConstantPoolString(int off) {
    JAVA_OBJECT o = CN1_CONSTANT_POOL_LOAD(off);
    if(__builtin_expect(o != JAVA_NULL, 1)) {
        return o;
    }
    return cn1MaterializeConstantPoolString(off);
}
#define STRING_FROM_CONSTANT_POOL_OFFSET(off) cn1ConstantPoolString(off)

#define BC_IINC(val, num) ilocals_##val##_ += num;

extern int instanceofFunction(int sourceClass, int destId);

// Tagged small-integer ("poor man's Valhalla"): Integer.valueOf returns an immediate
// tagged pointer (low bit = 1, the int in the high bits) instead of allocating, and the
// GC ignores it while every class/dispatch lookup substitutes Integer's class. 64-bit
// POINTERS ONLY: on a 32-bit-pointer target a 32-bit int can't be tagged losslessly, so
// it must fall back to heap boxing. That includes armv7/armv7k AND arm64_32 (Apple Watch
// Series 4+, which is 64-bit hardware but uses 32-bit pointers) -- hence the gate is on
// __SIZEOF_POINTER__, not the architecture. DEFAULT ON for 64-bit-pointer
// targets (the shipping iOS/tv/desktop shape); opt out with
// -DCN1_DISABLE_TAGGED_INT. The gate below still auto-disables it wherever
// pointers are 32-bit, so no per-target configuration is needed.
#if !defined(CN1_DISABLE_TAGGED_INT) && !defined(CN1_TAGGED_INT)
#define CN1_TAGGED_INT
#endif
#if defined(CN1_TAGGED_INT) && !defined(CN1_DISABLE_TAGGED_INT) && defined(__SIZEOF_POINTER__) && (__SIZEOF_POINTER__ >= 8)
#define CN1_TAGGED_ACTIVE 1
#else
#define CN1_TAGGED_ACTIVE 0
#endif
extern struct clazz class__java_lang_Integer;
extern struct clazz class__java_lang_Long;
extern struct clazz class__java_lang_Double;
extern struct clazz class__java_lang_Float;
extern struct clazz class__java_lang_Character;
extern struct clazz class__java_lang_Short;

// The tag is the LOW THREE BITS of the word, and code 0 means "an ordinary heap pointer".
// Three bits rather than one because 8-byte object alignment is ALREADY load-bearing:
// cn1ConservativeResolve rejects any word with a bit set in (sizeof(void*) - 1), and
// conservative roots are on by default, so an object living at a 4-byte address would
// already be invisible to the root scan and freed under a live reference. Widening the tag
// therefore rests on an invariant the collector enforces rather than introducing a new one,
// and it buys one code per boxed type over a 61-bit payload.
//
// One code per type is what makes the immediate SELF-DESCRIBING, which is the whole point:
// a value is boxed precisely because it is flowing through an Object / Number / Comparable
// slot, so the callsite that has to dispatch hashCode() on it has no static type to consult.
#define CN1_TAG_MASK      7
#define CN1_TAG_SHIFT     3
#define CN1_TAG_NONE      0
#define CN1_TAG_INTEGER   1
#define CN1_TAG_LONG      2
#define CN1_TAG_DOUBLE    3
#define CN1_TAG_FLOAT     4
#define CN1_TAG_CHARACTER 5
#define CN1_TAG_SHORT     6
#define CN1_TAG_COUNT     8

// Ablation arm for the five types added after Integer. It gates only the PRODUCTION side --
// the valueOf natives -- and never the consumption side (cn1Value, the proxy table,
// cn1ClassOf), so turning it off simply stops those immediates being created and cannot
// leave the runtime half-taught about a tag it might still meet.
#if !defined(CN1_DISABLE_TAGGED_VALUES)
#define CN1_TAGGED_EXTRA_ACTIVE CN1_TAGGED_ACTIVE
#else
#define CN1_TAGGED_EXTRA_ACTIVE 0
#endif

#if CN1_TAGGED_ACTIVE
struct JavaObjectPrototype;
// Static object-shaped proxies, one per tag code, each carrying its boxed class in the
// header slot. cn1ClassOf selects a VALID object pointer (this code's proxy, else the object
// itself) BEFORE the single header load, so clang's if-conversion can branchlessly select
// the pointer yet the load is always on a dereferenceable address -- a plain ternary lets
// clang speculate the faulting `tagged->header` load above the tag test (observed: a SIGSEGV
// in interface dispatch like Comparable.compareTo, where no inline fast path guards it
// first). Do not "simplify" this back into a conditional over the loaded value.
//
// Indexing by tag code costs an address computation and NOT a memory access, because the
// proxies are one contiguous array: &cn1TaggedProxy[code] is a shifted add on both arm64 and
// x86-64. That is the entire runtime price of knowing which boxed type an immediate is.
extern struct JavaObjectPrototype cn1TaggedProxy[CN1_TAG_COUNT];
#define CN1_TAG_CODE(o) (((uintptr_t)(o)) & CN1_TAG_MASK)
#define CN1_IS_TAGGED(o) (CN1_TAG_CODE(o) != 0)
// A function rather than a macro so the argument is evaluated exactly once; every caller
// passes an lvalue, but SP[-1].data.o through a volatile SP would otherwise be three loads.
static inline struct clazz* cn1ClassOf(JAVA_OBJECT o) {
    uintptr_t cn1__code = ((uintptr_t)o) & CN1_TAG_MASK;
    struct JavaObjectPrototype* cn1__p = cn1__code ? &cn1TaggedProxy[cn1__code]
                                                   : (struct JavaObjectPrototype*)o;
    return CN1_OBJ_CLASS(cn1__p);
}
#define CN1_TAG_INT(v) ((JAVA_OBJECT)((((uintptr_t)(intptr_t)(JAVA_INT)(v)) << CN1_TAG_SHIFT) | CN1_TAG_INTEGER))
#define CN1_UNTAG_INT(o) ((JAVA_INT)(((intptr_t)(o)) >> CN1_TAG_SHIFT))

// Short and Character are narrower than int and always fit. The arithmetic shift recovers
// the sign correctly across the OR'd tag because the tag occupies exactly the bits the left
// shift zero-filled: -32768 tags to (-262144 | 6) and shifts back to -32768.
#define CN1_TAG_SHORT_VAL(v) ((JAVA_OBJECT)((((uintptr_t)(intptr_t)(JAVA_SHORT)(v)) << CN1_TAG_SHIFT) | CN1_TAG_SHORT))
#define CN1_UNTAG_SHORT(o) ((JAVA_SHORT)(((intptr_t)(o)) >> CN1_TAG_SHIFT))
#define CN1_TAG_CHAR_VAL(v) ((JAVA_OBJECT)(((((uintptr_t)(JAVA_CHAR)(v)) & 0xFFFF) << CN1_TAG_SHIFT) | CN1_TAG_CHARACTER))
#define CN1_UNTAG_CHAR(o) ((JAVA_CHAR)((((uintptr_t)(o)) >> CN1_TAG_SHIFT) & 0xFFFF))

// A float is 32 bits, so its RAW bit pattern always fits. Raw, not canonicalized: the
// immediate has to round-trip Float.floatToRawIntBits, and equals/hashCode canonicalize NaN
// for themselves through floatToIntBits.
#define CN1_TAG_FLOAT_BITS(b) ((JAVA_OBJECT)((((uintptr_t)(uint32_t)(b)) << CN1_TAG_SHIFT) | CN1_TAG_FLOAT))
#define CN1_UNTAG_FLOAT_BITS(o) ((uint32_t)(((uintptr_t)(o)) >> CN1_TAG_SHIFT))

// Long and Double are the two that do NOT always fit in the 61-bit payload, so both are
// PARTIAL: the test below decides, and anything that fails it takes the ordinary heap path.
// A partial scheme can look excellent on a benchmark and never fire on real data, so
// whatever measures this has to report the hit rate rather than the speedup alone.
//
// Long: 61 signed bits, i.e. [-2^60, 2^60). Timestamps, ids, counters and sizes are far
// inside it; a uniformly random 64-bit value is outside it seven times in eight.
#define CN1_LONG_TAGGABLE(v) (((JAVA_LONG)(v)) >= -(((JAVA_LONG)1) << 60) && ((JAVA_LONG)(v)) < (((JAVA_LONG)1) << 60))
#define CN1_TAG_LONG_VAL(v) ((JAVA_OBJECT)((((uintptr_t)(intptr_t)(JAVA_LONG)(v)) << CN1_TAG_SHIFT) | CN1_TAG_LONG))
#define CN1_UNTAG_LONG(o) ((JAVA_LONG)(((intptr_t)(o)) >> CN1_TAG_SHIFT))

// Double: a 64-bit pattern cannot be shifted at all, so instead of narrowing the value the
// scheme narrows the SET -- a double whose low three mantissa bits are already zero carries
// its own tag space, making tag an OR and untag a mask with no shifting and no value loss.
// That set is every small integer, half, quarter and eighth (what JSON numbers overwhelmingly
// are), and it excludes 0.1 and 3.14. -0.0 and +0.0 stay distinct, which Double.equals needs.
#define CN1_DOUBLE_TAGGABLE_BITS(b) ((((uint64_t)(b)) & CN1_TAG_MASK) == 0)
#define CN1_TAG_DOUBLE_BITS(b) ((JAVA_OBJECT)(((uintptr_t)(uint64_t)(b)) | CN1_TAG_DOUBLE))
#define CN1_UNTAG_DOUBLE_BITS(o) ((uint64_t)(((uintptr_t)(o)) & ~((uintptr_t)CN1_TAG_MASK)))
#define CN1_CLASS_OF(o) cn1ClassOf((JAVA_OBJECT)(o))
#else
#define CN1_TAG_CODE(o) (0)
#define CN1_IS_TAGGED(o) (0)
#define CN1_CLASS_OF(o) (CN1_OBJ_CLASS((o)))
#endif

/* The class word, read DIRECTLY, for a receiver that cannot be a tagged immediate.
 *
 * CN1_CLASS_OF masks, tests the tag and selects between a proxy entry and the
 * object -- five instructions before it loads anything. That work only ever finds
 * something for a boxed Integer/Long/Double/Float/Character/Short, so a thunk
 * whose owner none of those is assignable to can skip it entirely. The translator
 * decides that from the hierarchy (Parser.canReceiveTagged); using this where a
 * tagged value CAN arrive dereferences a small integer.
 */
#define CN1_CLASS_OF_UNTAGGED(o) (CN1_OBJ_CLASS((o)))

#define GET_CLASS_ID(JavaObj) ((CN1_CLASS_OF(JavaObj))->classId)

/* ---- java.lang.String TWIN CLASS -------------------------------------------
 *
 * Two clazz structs, one classId. class__java_lang_String_i8 is a byte-for-byte
 * copy of class__java_lang_String, so it carries the same classId, name, vtable
 * pointer, mark function and type-test row. Everything that decides behaviour
 * keys on the ID -- instanceof, getClass, the interface map row, the thunk
 * switch, the type-test bitset -- so an object of the twin is a java.lang.String
 * in every way a program can observe.
 *
 * What differs is the ADDRESS, and that is the point: the class word is already
 * in every object header, so a pointer compare against it carries a bit that
 * costs nothing to store. String needs exactly such a bit -- its coder -- and
 * the only place that bit lives today is a 24-byte JavaArrayPrototype wrapped
 * around a payload no one else can reach.
 *
 * Anything comparing a class pointer to &class__java_lang_String by IDENTITY has
 * to ask cn1IsStringClass instead. Missing one does not fail loudly: it compiles,
 * and simply answers "not a String".
 */
extern struct clazz class__java_lang_String;
extern struct clazz class__java_lang_String_i8;
extern struct clazz class__java_lang_String_i16;
static inline int cn1IsStringClass(const struct clazz* c) {
    return c == &class__java_lang_String
        || c == &class__java_lang_String_i8
        || c == &class__java_lang_String_i16;
}
/* MUST be called before either twin is used as an allocation class. The twins are
 * filled by a lazy memcpy from the primary -- lazy so it lands after
 * java.lang.String's clinit, where the vtable pointer is real -- and until that
 * runs they are all zeros: null vtable, classId 0. Every path that allocates with
 * a twin calls this first. Getting it wrong is not subtle but it IS remote: the
 * first virtual call on such a String dereferences vtable[n] off NULL, which is
 * where this was found. */
extern void cn1InitStringTwin(void);

/* A String whose characters sit INSIDE it: value is JAVA_NULL and the payload
 * begins at the first 8-aligned byte after the fields. The coder is the twin. */
static inline int cn1IsInlineStringClass(const struct clazz* c) {
    return c == &class__java_lang_String_i8 || c == &class__java_lang_String_i16;
}

/* How a String's characters are reached. DECLARED here and defined in
 * nativeMethods.m, because a declaration needs no struct while the definition
 * needs the generated obj__java_lang_String -- and this header is included long
 * before that one exists.
 *
 * Not static inline in a header, which is what the first attempt tried:
 * cn1_intrinsics.h is pulled into generated .c files that do not necessarily
 * include java_lang_String.h, and its __has_include guard cannot help because
 * __has_include asks whether the FILE EXISTS, not whether this translation unit
 * included it -- and it always exists. Out-of-line is the answer, and costs
 * nothing where it matters: -O3 ships -flto=thin, so these inline across
 * translation units anyway.
 *
 * String is special-cased in the runtime on purpose. It is the most allocated
 * class in the VM and the only one whose storage the collector, the intrinsics
 * and the natives all have opinions about, so its access belongs in one place
 * rather than re-derived at each. */
extern void* cn1StrChars(JAVA_OBJECT s);
extern int cn1StrIsLatin1(JAVA_OBJECT s);
extern JAVA_CHAR cn1StrCharAtRaw(JAVA_OBJECT s, JAVA_INT i);


/* Constant-time instanceof against a type the translator assigned a dense bit
 * index to (see Parser.typeTestIds). One load of the runtime class id, one load
 * from the class's bitmap row, a shift and an and -- no call and no loop, where
 * BC_INSTANCEOF walks the supertype list one dependent load at a time.
 *
 * cn1TypeTestBits has a row per ordinary class id only. An array's id lives above
 * cn1_array_start_offset, i.e. past CN1_TYPETEST_ROWS, so those fall back to
 * instanceofFunction, which has always handled array types before it reaches the
 * scan. The bound test is also what keeps a row lookup in range. */
/* instanceof against a class NOTHING LIVE EXTENDS.
 *
 * The general form reads the class word, then reads classId out of it -- a second
 * load, dependent on the first -- and indexes the type-test bitmap. Against a leaf
 * the class word IS the answer, so this is ONE load and a compare.
 *
 * The tagged test stays and is cheap: a tagged immediate is a boxed value, whose
 * class is never a leaf the application tests for, so !CN1_IS_TAGGED settles it
 * without resolving anything. It must come BEFORE the field read, because
 * dereferencing a tagged word reads whatever that small integer points at.
 *
 * Measured against C2's output for the same test: HotSpot compares the klass word
 * directly because the klass is the identity there, and that single dependent load
 * was most of the 31% load gap on BytecodeMethod.equals.
 */
/* instanceof java.lang.String.
 *
 * String is final, so it is a leaf and the leaf form above would apply -- except
 * that its class word is not unique. A fused String carries a TWIN clazz, same
 * classId and vtable, different address, so a plain identity compare against
 * &class__java_lang_String answers FALSE for exactly the Strings the VM creates
 * most. The twin commit said in as many words that identity comparisons must ask
 * cn1IsStringClass; this is that rule applying to code written after it.
 *
 * Still worth having: one load and up to three compares, against the general
 * form's two dependent loads plus a bitmap index.
 */
#define BC_INSTANCEOF_STRING() { \
        JAVA_OBJECT cn1__io = SP[-1].data.o; \
        SP[-1].type = CN1_TYPE_INT; \
        SP[-1].data.i = (cn1__io != JAVA_NULL && !CN1_IS_TAGGED(cn1__io) \
                && cn1IsStringClass(CN1_OBJ_CLASS(cn1__io))) ? 1 : 0; \
    }

#define BC_INSTANCEOF_LEAF(clsSymbol) { \
        JAVA_OBJECT cn1__io = SP[-1].data.o; \
        SP[-1].type = CN1_TYPE_INT; \
        SP[-1].data.i = (cn1__io != JAVA_NULL && !CN1_IS_TAGGED(cn1__io) \
                && CN1_OBJ_CLASS(cn1__io) == &(clsSymbol)) ? 1 : 0; \
    }

#define BC_INSTANCEOF_FAST(typeTestIdx, typeOfInstanceOf) { \
    if(SP[-1].data.o != JAVA_NULL) { \
        int tmpInstanceOfId = GET_CLASS_ID(SP[-1].data.o); \
        SP[-1].type = CN1_TYPE_INVALID; \
        SP[-1].data.i = (tmpInstanceOfId < CN1_TYPETEST_ROWS) \
            ? (JAVA_INT)((cn1TypeTestBits[(size_t)tmpInstanceOfId * CN1_TYPETEST_WORDS \
                    + ((typeTestIdx) >> 6)] >> ((typeTestIdx) & 63)) & 1ULL) \
            : instanceofFunction( typeOfInstanceOf, tmpInstanceOfId ); \
    } \
    SP[-1].type = CN1_TYPE_INT; \
}

#define BC_INSTANCEOF(typeOfInstanceOf) { \
    if(SP[-1].data.o != JAVA_NULL) { \
        int tmpInstanceOfId = GET_CLASS_ID(SP[-1].data.o); \
        SP[-1].type = CN1_TYPE_INVALID; \
        SP[-1].data.i = instanceofFunction( typeOfInstanceOf, tmpInstanceOfId ); \
    } \
    SP[-1].type = CN1_TYPE_INT; \
}

#define BC_IALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_INT*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-1].data.o))[(*SP).data.i]; \
    }

#define BC_LALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_LONG; \
    SP[-1].data.l = LONG_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i); \
    }

#define BC_FALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_FLOAT; \
    SP[-1].data.f = FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i); \
    }

#define BC_DALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_DOUBLE; \
    SP[-1].data.d = DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)SP[-1].data.o, (*SP).data.i); \
    }

#define BC_AALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INVALID; \
    SP[-1].data.o = ((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-1].data.o))[(*SP).data.i]; \
    SP[-1].type = CN1_TYPE_OBJECT;  }

#define BC_BALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-1].data.o))[(*SP).data.i]; \
    }

#define BC_CALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-1].data.o))[(*SP).data.i]; \
    }

#define BC_SALOAD() { CHECK_ARRAY_ACCESS(2, SP[-1].data.i); \
    SP--; SP[-1].type = CN1_TYPE_INT; \
    SP[-1].data.i = ((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-1].data.o))[(*SP).data.i]; \
    }


#define BC_BASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-3].data.o))[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_CASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-3].data.o))[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_SASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-3].data.o))[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_IASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    ((JAVA_ARRAY_INT*) CN1_ARRAY_DATA((JAVA_ARRAY)SP[-3].data.o))[SP[-2].data.i] = SP[-1].data.i; SP-=3

#define BC_LASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    LONG_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.l; SP-=3

#define BC_FASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    FLOAT_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.f; SP-=3

#define BC_DASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); \
    DOUBLE_ARRAY_LOOKUP((JAVA_ARRAY)SP[-3].data.o, SP[-2].data.i) = SP[-1].data.d; SP-=3

#define BC_AASTORE() CHECK_ARRAY_ACCESS(3, SP[-2].data.i); { \
    JAVA_OBJECT aastoreTmp = SP[-3].data.o; \
    CN1_WRITE_BARRIER(aastoreTmp, SP[-1].data.o); \
    ((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)aastoreTmp))[SP[-2].data.i] = SP[-1].data.o; \
    SP-=3; \
}
#define BC_AASTORE_WITH_ARGS(array, index, value) CHECK_ARRAY_ACCESS(3, SP[-2].data.i); { \
    JAVA_OBJECT aastoreTmp = SP[-3].data.o; \
    CN1_WRITE_BARRIER(aastoreTmp, SP[-1].data.o); \
    ((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)aastoreTmp))[SP[-2].data.i] = SP[-1].data.o; \
    SP-=3; \
}


//#define BYTE_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA(array))[offset]
//#define SHORT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA(array))[offset]
//#define CHAR_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA(array))[offset]
//#define INT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_INT*) CN1_ARRAY_DATA(array))[offset]

#define LONG_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_LONG*) CN1_ARRAY_DATA(array))[offset]

#define FLOAT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA(array))[offset]

#define DOUBLE_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_DOUBLE*) CN1_ARRAY_DATA(array))[offset]

//#define OBJECT_ARRAY_LOOKUP(array, offset) ((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA(array))[offset]

// Native buffers owned by an escape-proven C stack object. Java exceptions
// unwind this chain before longjmp; C cleanup attributes handle ordinary returns.
struct CN1StackBuffer {
    struct CN1StackBuffer* previous;
    struct ThreadLocalData* thread;
    JAVA_OBJECT owner;
    void* initialData;
    JAVA_LONG owned;
    int initialBytes;
};

// indicates a try/catch block currently in frame
struct TryBlock {
    jmp_buf destination;
    struct CN1StackBuffer* nativeBuffers;
    
    // -1 for all exceptions
    JAVA_INT exceptionClass;

    // Synchronized methods will use a TryBlock for its monitor
    // so that the monitor will be exited when an exception is thrown.
    // This will be 0 for regular TryBlock.
    JAVA_OBJECT monitor;
};

/*
 * Per-thread sizing. These three are what a thread costs before it runs a single
 * instruction, so they are the numbers that decide whether a server-side binary
 * can afford a thread per connection. #ifndef-guarded so an A/B can override them
 * with -D without editing this file -- an unconditional #define silently ignores
 * the -D (the redefinition warning is suppressed by the generated code's -w).
 */
#ifndef CN1_MAX_STACK_CALL_DEPTH
#define CN1_MAX_STACK_CALL_DEPTH 1024
#endif
#define CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT CN1_MAX_STACK_CALL_DEPTH
#ifndef CN1_MAX_OBJECT_STACK_DEPTH
#define CN1_MAX_OBJECT_STACK_DEPTH 16536
#endif

#ifndef PER_THREAD_ALLOCATION_COUNT
#define PER_THREAD_ALLOCATION_COUNT 4096
#endif

/*
 * Try-block depth. Each entry carries a jmp_buf (~200 bytes on arm64 macOS,
 * ~320 on arm64 musl), so 500 of them is 100-160KB per thread -- comparable to
 * the shadow stack and much less obvious.
 */
#ifndef CN1_MAX_TRY_BLOCKS
#define CN1_MAX_TRY_BLOCKS 500
#endif

/*
 * Native stack per spawned thread on Linux. musl defaults to 128KB, which the
 * recursive generated C overflows on a deep call chain, so it is pinned to a
 * JVM-sized reservation. Reserved, not committed -- but it is the largest single
 * number attached to a thread, so it is a knob rather than a literal.
 */
#ifndef CN1_THREAD_STACK_BYTES
#define CN1_THREAD_STACK_BYTES (16 * 1024 * 1024)
#endif

// THE OBJECT-STORE WRITE BARRIER IS THE SATB INSERTION HALF.
// The translator emits CN1_WRITE_BARRIER at every object store, so it is the natural
// place for it. During the mark, enqueue the NEW reference being stored so an
// object linked into the graph mid-mark is kept alive even if the container it is stored
// into is a fresh grace object not yet reachable (the residual Property->Double /
// container->content crash). Pairs with CN1_SATB_DELETE (the deletion half) for a
// complete snapshot + incremental barrier. Off-mark: one predicted-not-taken flag load.
// FRESH VALUES ARE FILTERED INLINE. cn1SatbEnqueue discards a fresh (mark == -1)
// reference anyway -- the sweep's grace rule keeps it -- but only after the call and
// three acquire loads. In allocation-heavy code nearly every stored reference is a
// newly allocated object, so that out-of-line call was the cost: 12.6-26.4% of
// objectAllocation's main thread. The mark word is exactly what the out-of-line
// filter reads first, so skipping here drops no reference it would have kept.
// -DCN1_SATB_LOG_FRESH logs fresh references on purpose (it is the negative control
// GcSteadyStateIntegrationTest rebuilds with), so the filter is off in that arm.
#ifdef CN1_SATB_LOG_FRESH
#define CN1_SATB_FRESH_INLINE(o) 0
#else
// The filter is only sound while the sweep grants EVERY fresh object a cycle of grace.
// Single-core mode (see cn1GcSingleCore in cn1_globals.m) reclaims fresh objects that
// were allocated before the cycle began, so there a fresh reference is logged like any
// other; cn1GcFreshFilter is set once, at the first collection, and never changes.
extern JAVA_BOOLEAN cn1GcFreshFilter;
#define CN1_SATB_FRESH_INLINE(o) (cn1GcFreshFilter && CN1_OBJ_MARK_LOAD((o), __ATOMIC_RELAXED) == -1)
#endif
#if defined(CN1_DISABLE_SATB)
#define CN1_WRITE_BARRIER(target, value) do { } while(0)
#else
// Two halves share the gate. The SATB half runs only while a mark is in progress. The
// GENERATIONAL half (single-core mode only, see cn1GcSingleCore) runs always: a young
// (mark == -1) value stored into an OLD object records that object in the remembered
// set, which is what lets a minor cycle skip tracing the old generation.
extern volatile int cn1GcGenBarrier;
extern void cn1GcRememberSlow(JAVA_OBJECT target);
// A native reference block (NativeStorage) is remembered itself, not through an object.
extern void cn1GcRememberBlock(JAVA_LONG block);
#define CN1_GEN_REMEMBER_BLOCK(block, v) \
    do { JAVA_OBJECT cn1__bv = (JAVA_OBJECT)(v); \
         if(__builtin_expect(cn1GcGenBarrier, 0) && cn1__bv != JAVA_NULL && !CN1_IS_TAGGED(cn1__bv) \
            && CN1_OBJ_MARK_LOAD(cn1__bv, __ATOMIC_RELAXED) == -1) \
             cn1GcRememberBlock(block); } while(0)
#define CN1_GEN_REMEMBER(target, v) \
    do { JAVA_OBJECT cn1__gt = (JAVA_OBJECT)(target); \
         if(cn1__gt != JAVA_NULL \
            && CN1_OBJ_MARK_LOAD((v), __ATOMIC_RELAXED) == -1 \
            && CN1_OBJ_MARK_LOAD(cn1__gt, __ATOMIC_RELAXED) > 0) \
             cn1GcRememberSlow(cn1__gt); } while(0)
#ifdef CN1_GC_GEN_CHECK2
extern void cn1GcGenNoteStore(JAVA_OBJECT target, JAVA_OBJECT value);
#define CN1_GEN_NOTE(t, v) cn1GcGenNoteStore((JAVA_OBJECT)(t), (v))
#else
#define CN1_GEN_NOTE(t, v) ((void)0)
#endif
#define CN1_WRITE_BARRIER(target, value) \
    do { if(__builtin_expect(gcSatbActive | cn1GcGenBarrier, 0)) { \
             JAVA_OBJECT cn1__nv = (JAVA_OBJECT)(value); \
             if(cn1__nv != JAVA_NULL && !CN1_IS_TAGGED(cn1__nv)) { \
                 if(gcSatbActive && !CN1_SATB_FRESH_INLINE(cn1__nv)) cn1SatbEnqueue(cn1__nv); \
                 CN1_GEN_NOTE(target, cn1__nv); \
                 if(cn1GcGenBarrier) CN1_GEN_REMEMBER(target, cn1__nv); } } } while(0)
#endif

// ---- Snapshot-at-the-beginning (Yuasa) DELETION write barrier ---------------
// The concurrent collector marks each thread's roots while that thread is paused,
// then RELEASES the thread before the others are scanned (cn1_globals.m:963), and
// never pauses native threads at all. With no barrier, a mutator that moves or
// nulls the last snapshot-time reference to a live object -- in the window between
// its own scan and the end of mark -- makes that object unreachable to the
// collector, so it is swept while still live; a surviving stale reference then
// crashes the NEXT cycle in gcMarkObject. This is the intermittent Linux mid-suite
// SIGSEGV, reproducible only with the live GTK/WebKit/Gallium threads that do such
// concurrent mutation (GcStress/MtStress have none).
//
// The fix: while a mark is in progress (gcSatbActive), every store that OVERWRITES
// an object reference in a heap location first hands the collector the OLD value,
// so a reference present in the start-of-cycle snapshot is preserved for this cycle
// no matter where the mutator moves it. Off-mark the barrier is a single relaxed
// flag load (gcSatbActive is 0), so store-heavy code pays nothing outside GC. The
// old value is read from the field address ONLY when marking, so there is no extra
// load on the common path. Covers native threads too (they take the barrier on any
// heap ref store), which thread-pausing structurally cannot.
extern volatile int gcSatbActive;
extern void cn1SatbEnqueue(JAVA_OBJECT old);
// The bulk trio is DECLARED here and DEFINED unconditionally in cn1_globals.m, because
// nativeMethods' arraycopy/cloneArray bulk barrier and CN1_REF_LOAD_BEGIN/END call it
// with no #ifdef around them. Keep it that way: these declarations previously sat inside
// a configuration branch, and the arm that did not see them compiled those calls as
// implicit C89 declarations returning int -- which clang has rejected since C99 became
// the default, so that arm did not build at all and no gate noticed.
extern volatile int gcSatbTerminating;
extern JAVA_BOOLEAN cn1SatbBulkBegin(void);
extern void cn1SatbEnqueueRangeLocked(JAVA_ARRAY_OBJECT* refs, int count);
/// A MOVE WITHIN ONE REFERENCE BLOCK OWES THE DELETION BARRIER EVERY SLOT IT OVERWRITES.
///
/// The tempting narrowing is to log only the min(count, |to-from|) slots whose old
/// value leaves the block, since a shift otherwise only permutes references. It is
/// wrong, and it shipped for a while: a marker can be scanning this same block WHILE
/// the memmove runs, and nothing orders the two. For ArrayList.remove(0) the memmove
/// runs upward faster than the marker does, overtakes it, and the one element that
/// slides from the unscanned side of the scan position to the scanned side is seen
/// in neither place. That element is logged by nobody and swept under a live list:
/// the self-hosting translator lost a LineNumber out of an instruction list this way,
/// about one run in twenty. MoveRace (run-gc-verify.sh) reproduces it.
///
/// Logging the whole overwritten range [to, to+count) is sound for any scan order and
/// any store ordering: a moved value either had its old slot inside that range, so its
/// old value is logged, or it never leaves its old slot during the move (the tail of a
/// left shift, the head of a right shift), so a scan finds it there whenever it looks.
/// No insertion half is needed for the same reason -- nothing is written that was not
/// already in the block.
///
/// That is the FALLBACK now, not the common case. On 64-bit targets a move during a mark
/// queues its block for a collector re-scan and logs only the min(count, |to-from|)
/// slots that leave -- the narrow form made sound by the re-scan, which finds whatever
/// the concurrent scan missed. See DEFERRED BLOCK RE-SCAN in cn1_globals.m.
extern void cn1SatbBulkEnd(void);
extern void cn1SatbBulkQuiesce(void);
#if defined(CN1_DISABLE_SATB)
// Escape hatch to A/B the barrier cost or fall back if a regression appears. When
// disabled, gcSatbActive is never armed (see codenameOneGCMark) AND the per-store
// barrier compiles out entirely, so there is zero footprint on the store hot path.
#define CN1_SATB_DELETE(fieldAddr) do { } while(0)
#else
#define CN1_SATB_DELETE(fieldAddr) \
    do { if(__builtin_expect(gcSatbActive, 0)) { \
             JAVA_OBJECT cn1__old = *(JAVA_OBJECT volatile*)(fieldAddr); \
             if(cn1__old != JAVA_NULL && !CN1_IS_TAGGED(cn1__old) && !CN1_SATB_FRESH_INLINE(cn1__old)) \
                 cn1SatbEnqueue(cn1__old); \
         } } while(0)
#endif

// ---- java.lang.ref support -------------------------------------------------
// A WeakReference's referent is NOT traced by the generated mark function. That
// function calls cn1GcDiscoverReference instead (see ByteCodeClass), handing over
// the addresses of the reference's fields, and the collector decides for itself
// whether the referent lives.
//
// Field pointers rather than the object, because this file is a fixed template
// compiled beside whatever the translator emitted: `struct obj__java_lang_ref_Reference`
// does not exist in a program that never uses a reference, so naming it here would
// break the build for those. The layout stays on the generated side.
//
// `strength` is CN1_REF_WEAK or CN1_REF_SOFT, taken from a field the subclass
// constructor sets. Deliberately not a class-pointer comparison: the dead-code pass
// is entitled to remove a class symbol this file would then fail to link against,
// and a user-written subclass of either would compare unequal to both.
#define CN1_REF_WEAK 0
#define CN1_REF_SOFT 1
// cn1TouchAge value written by get_field_java_lang_ref_Reference_objReference on
// every read. The collector turns it back into an age in cycles.
#define CN1_REF_TOUCHED (-1)
extern const char* volatile cn1LastNamSetter; // diagnosis: last bracket toucher
#ifdef CN1_CONSERVATIVE_GC_ROOTS
// The bracket's purpose was to suppress GC interaction while native C code
// holds heap references in UNROOTED C locals. Under conservative roots the
// native stack IS scanned, so those locals are roots like any other -- the
// flag stores (two of them per native call, one a global write) were pure
// hot-path overhead on every String/StringBuilder/HashMap native.
#define enteringNativeAllocations() do { } while(0)
#define finishedNativeAllocations() do { } while(0)
#else
#define enteringNativeAllocations() do { threadStateData->nativeAllocationMode = JAVA_TRUE; cn1LastNamSetter = __FUNCTION__; } while(0)
#define finishedNativeAllocations() do { threadStateData->nativeAllocationMode = JAVA_FALSE; cn1LastNamSetter = 0; } while(0)
#endif

/* A malloc buffer is not a conservative Java root. Keep its Java owner live
 * until the borrowing method returns, even when C optimization retains only an
 * interior buffer pointer. The empty asm is a compiler lifetime fence; it emits
 * no call or instruction. cleanup covers every normal return. A Java exception
 * leaves the borrowing scope via longjmp, after which the borrow is no longer used. */
static inline void cn1NativeOwnerFence(JAVA_OBJECT* owner) {
    __asm__ __volatile__("" : : "r"(*owner) : "memory");
}
#define CN1_KEEP_NATIVE_OWNER(name, value) \
    JAVA_OBJECT name __attribute__((cleanup(cn1NativeOwnerFence))) = (value)

// Shared by ThreadLocalData's adaptive state and the page allocator below.
#ifndef CN1_BIBOP_NUM_CLASSES
#define CN1_BIBOP_NUM_CLASSES 32
#endif

// handles the stack used for print stack trace and GC
struct ThreadLocalData {
    JAVA_LONG threadId;
    JAVA_OBJECT currentThreadObject;
    struct TryBlock* blocks;
    struct CN1StackBuffer* nativeBuffers;
    int tryBlockOffset;
    JAVA_OBJECT exception;
    
    JAVA_BOOLEAN lightweightThread;
    JAVA_BOOLEAN threadActive;
    JAVA_BOOLEAN threadBlockedByGC;
    JAVA_BOOLEAN nativeAllocationMode;
    JAVA_BOOLEAN threadRemoved;

    // used by the GC to traverse the objects pointed to by this thread
    struct elementStruct* threadObjectStack;
    /* How threadObjectStack was obtained, because the two allocators are not
       interchangeable at free time: mmap pairs with munmap, calloc with free.
       cn1AllocThreadStack falls back to calloc when mmap runs out of MAPPINGS
       rather than out of memory, and munmap on an allocator-owned pointer fails
       with EINVAL and leaks the whole shadow stack -- or, if the allocator handed
       back a page-aligned block, unmaps memory the allocator still believes it
       owns. */
    int threadObjectStackMapped;
    int threadObjectStackOffset;
    
    // allocations are stored here and then copied to the big memory pool during
    // the mark sweep
    void** pendingHeapAllocations;
    JAVA_INT heapAllocationSize;
    JAVA_INT threadHeapTotalSize;


    // used to construct stack trace
    int* callStackClass;
    int* callStackLine;
    int* callStackMethod;
    int callStackOffset;

    // Native C-stack low-water mark used by frameless methods (see
    // CN1_FRAMELESS_SOE_GUARD). Frameless primitive-only methods don't bump
    // callStackOffset, so the call-depth limit can't catch their native C-stack
    // recursion; this per-thread limit lets the guard throw a catchable
    // StackOverflowError instead of overrunning the stack into a SIGSEGV.
    // 0 == not yet computed (lazily initialized once per thread on first use).
    JAVA_LONG nativeStackLimit;

    /* Per-size-class current BiBOP page. See the note at the fast path: this was
     * `__thread`, which costs an indirect call per allocation on Darwin. */
    struct CN1BibopPage* bibopCurrent[CN1_BIBOP_NUM_CLASSES];

#ifdef CN1_GC_VERIFY
    // The thread's whole C stack, [low, high), recorded beside the limit above and
    // used by nothing but the verifier's escaped-stack-object check. A heap object's
    // reference field pointing in here is a stack-allocated object that outlived its
    // frame -- the failure mode every stack-allocation analysis in this translator
    // risks, and the one the verifier could not see: an address in a dead frame is
    // not in a BiBOP page or a legacy extent, so cn1GcVerifyClassify calls it
    // UNKNOWN and SKIPS it, which is the same answer it gives a static or an
    // immortal. Ranges make the three distinguishable without dereferencing
    // anything. 0/0 means not yet computed, which costs a missed detection and
    // never a false one.
    JAVA_LONG nativeStackLow;
    JAVA_LONG nativeStackHigh;
#endif

    // LEVER A (perf-tier1): per-thread, plain-add accumulator for BiBOP allocation
    // volume. Replaces the per-object atomic_fetch_add on the global bibopBytesSinceGc
    // (which an uncontended single thread still pays as an arm64 exclusive-monitor RMW,
    // and which bounces a cache line across threads). Flushed to the global atomic once
    // per page-acquire (~64KB of allocation), so the GC-trigger cadence is unchanged to
    // within (nthreads * page) -- negligible vs the 24MB trigger, and the trigger is a
    // pure heuristic with NO correctness role (see CN1_BIBOP_FLUSH_BYTES).
    JAVA_LONG bibopBytesLocal;
    // Runtime policy state. These are part of the one production collector; the
    // compile-time collector variants are QA baselines only.
    JAVA_LONG bibopEpochBytes;
    int bibopObservedGcEpoch;
    int bibopHighThroughputUntilEpoch;

#ifdef CN1_ON_DEVICE_DEBUG
    // Per-frame pointer to a stack-allocated array of void* addresses, one per
    // JVM local slot in the current method. Populated by translator-emitted
    // prologue code in debug builds; consulted by the debugger thread to read
    // primitive locals (the auto C variables are volatile so their address is
    // stable for the duration of the frame).
    void*** callStackLocalsAddresses;
    // Per-frame pointer to the static cn1_frame_info struct for the current
    // method. Carries the variable side-table the debugger uses to map source
    // lines to slot/type info.
    const struct cn1_frame_info** callStackFrameInfo;
#endif

    char* utf8Buffer;
    int utf8BufferSize;
    JAVA_BOOLEAN threadKilled;      // we don't expect to see this in the GC
    JAVA_BOOLEAN interrupted;

    // Dead-thread pending-migration queue (single-writer allObjectsInHeap):
    // markDeadThread queues the dying thread's TLD (critical section held)
    // instead of migrating pendingHeapAllocations itself; the GC thread drains
    // the queue at mark start. gcReleaseRequested defers the TLD free (Thread
    // finalizer) until after that drain. See cn1DeadPendingThreads in
    // cn1_globals.m for the invariant and the race this closes.
    struct ThreadLocalData* gcDeadNext;
    JAVA_BOOLEAN gcQueuedForDrain;
    JAVA_BOOLEAN gcReleaseRequested;
    // THREAD IDENTITY, AND DELIBERATELY OUTSIDE THE CONSERVATIVE-ROOTS GUARD BELOW.
    // These two say "this TLD belongs to a real registered thread", which is a question
    // every configuration asks: CN1_RESUME_THREAD reads gcPthreadValid unconditionally
    // to decide whether it may republish threadActive. They used to sit inside the
    // #ifdef, so -DCN1_DISABLE_CONSERVATIVE_GC_ROOTS did not compile -- four errors in
    // nativeMethods.c, all of them CN1_RESUME_THREAD -- and had not for long enough that
    // nobody could tell when it broke. That is the same failure that retired the nursery
    // arm: a documented configuration no gate builds stops building and nothing says so.
    pthread_t    gcPthread;              // pthread_self() of THIS thread (set at startup)
    JAVA_BOOLEAN gcPthreadValid;         // gcPthread has been filled in
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: state for conservatively scanning this thread's native C stack as a
    // GC root source (so object-bearing FRAMELESS methods, whose object roots live in
    // native C locals / the method-local operand array rather than threadObjectStack,
    // are kept alive). Two stop mechanisms feed these:
    //   (1) COOPERATIVE park: a lightweight thread that pauses at an allocation
    //       safepoint runs CN1_GC_PARK_CAPTURE just before publishing threadActive=0.
    //   (2) SIGNAL stop: the GC pthread_kills any thread it could not cooperatively
    //       park; the async-signal-safe handler captures SP+regs and spins here.
    // cooperative-park capture
    jmp_buf      gcRegisterSnapshot;     // setjmp flushes callee-saved regs -> scanned
    void* volatile gcStackPointerAtPark; // SP-ish low bound captured at the park point
    volatile JAVA_BOOLEAN gcParkCaptured;// a fresh cooperative capture exists this cycle
    // signal-stop capture (async-signal-safe: handler only stores + spins).
    // GENERATION HANDSHAKE: request/stopped/release carry a per-thread generation
    // number (monotonic, GC-thread owned counter gcSigStopGen) instead of booleans,
    // so an abandoned stop (timeout) or a descheduled handler can never strand
    // spinning on a release that was reset -- see cn1GcSignalStopOne/ReleaseOne.
    volatile sig_atomic_t gcSigStopRequest; // GC sets to gen>0 to ask the handler to park
    volatile sig_atomic_t gcSigStopped;     // handler publishes the gen it parked for
    volatile sig_atomic_t gcSigRelease;     // GC publishes highest released gen (monotonic)
    volatile sig_atomic_t gcSigStopGen;     // generation counter (GC thread writes only)
    /* Consecutive cn1GcSignalStopOne timeouts for this thread. A thread that never
       answers the stop signal is not scanned either way -- the caller returns without
       reading its stack -- so signalling it at all, and then waiting, buys nothing.
       One such thread cost 267ms of a 280ms mark, every cycle. Stop attempting it once
       it has proved unresponsive, and clear this the moment it answers. */
    int gcStopFailures;
    void* volatile gcSigStackPointer;        // SP captured inside the signal handler
    // [sp,base) high bound and stack size, resolved BEFORE a forced freeze and reused
    // while it is held. cn1GcStackBase must not be called under one: it is two plain
    // accessors on Apple, but on Linux it is pthread_getattr_np, which mallocs (and
    // reads /proc/self/maps for the initial thread), so calling it while the target is
    // stopped can wait on an allocator lock the target owns. Written by the GC thread in
    // cn1GcMarkForceStopUncooperative, read by cn1GcScanThreadNativeStack.
    void* volatile gcSigStackBase;
    size_t       gcSigStackSize;
    char         gcSigRegs[4096];            // raw copy of the interrupted ucontext (GPRs)
    volatile sig_atomic_t gcSigRegsLen;      // valid bytes in gcSigRegs
    // Set while the MARK LOOP owns a signal freeze it took because this thread would
    // not reach a safepoint (see CN1_GC_CAN_FORCE_STOP). It tells
    // cn1GcScanThreadNativeStack to reuse that freeze rather than take its own: a
    // second SIGUSR2 aimed at a thread already spinning inside the handler stays
    // pending until the handler returns, so the nested stop would spin out its whole
    // timeout and then report failure on a thread that is in fact frozen. GC-thread
    // owned, cleared by cn1GcMarkReleaseForced.
    JAVA_BOOLEAN gcMarkForcedStop;
#endif
#ifdef CN1_GC_CONFORM
    // Monotonic ms at which this thread was registered. The duty denominator is the
    // integral of the live MUTATOR count over time, and sampling that population at slice
    // boundaries misses any thread that both starts and exits inside one slice -- its
    // stalls stay in the numerator while its lifetime is never counted. Stamping here and
    // banking the lifetime in markDeadThread makes the integral exact instead of sampled.
    long long gcThreadStartMs;
#endif
    /// A C stack buffer the CALLER has offered for the next iterator allocation, or NULL.
    ///
    /// An iterator is a parent pointer plus a couple of ints, it is created and discarded
    /// inside one loop, and it is by far the largest single source of allocation left in
    /// this VM. It cannot be stack-allocated the way @StackAllocate objects are, because
    /// the NEW is not in the loop's method at all -- it is inside whatever iterator()
    /// implementation the receiver turns out to have, and at the sites that matter the
    /// receiver's type is not provable. So the loop hands DOWN a buffer instead of the
    /// callee handing UP an object, and the allocation site takes it without ever knowing
    /// who offered it. That is what reaches the sites a receiver proof cannot.
    ///
    /// Strictly one-shot: whoever takes it clears it. An iterator that wraps another
    /// therefore puts the outer one on the stack and the inner one on the heap, which is
    /// correct rather than merely safe -- the inner one's lifetime is the outer one's.
    void* pendingStackIter;
};

//#define BLOCK_FOR_GC() while(threadStateData->threadBlockedByGC) { usleep(500); }

// WAITING OUT A STOP. A parked thread used to poll threadBlockedByGC with usleep(500)
// (usleep(1000) on the legacy allocation paths), so every handshake cost the mutator at
// least that long however quickly the collector released it -- the collector scans a
// parked thread in microseconds. Measured on objectAllocation: every start-of-mark
// handshake sat in the 512-1024us bucket, 9% of wall at one marker and the fixed cost
// that made small collection triggers slow. So the wait spins briefly, then yields, and
// only then sleeps -- in 50us steps, not 500.
//
// The flag is read with an explicit atomic load: it is a plain field, and the old loops
// only re-read it because usleep is an opaque call. Spinning is safe for the collector's
// scan of this thread: its stack was captured before it parked, and the spin only writes
// below the captured stack pointer, as the usleep call did.
#ifndef _WIN32
#include <unistd.h>
#include <sched.h>
#endif
static inline void cn1CpuRelax(void) {
#if defined(__aarch64__) || defined(__arm__)
    __asm__ __volatile__("yield" ::: "memory");
#elif defined(__x86_64__) || defined(__i386__)
    __asm__ __volatile__("pause" ::: "memory");
#else
    __asm__ __volatile__("" ::: "memory");
#endif
}
static inline void cn1ThreadYield(void) {
#ifdef _WIN32
    usleep(0);          /* the compatibility layer has no sched_yield */
#else
    sched_yield();
#endif
}
static inline void cn1GcHandshakeBackoff(int* spins) {
    int s = (*spins)++;
    if(s < 512) {
        cn1CpuRelax();
    } else if(s < 640) {
        cn1ThreadYield();
    } else {
        usleep(50);
    }
}
#define CN1_GC_WAIT_UNBLOCKED(ts) do { \
        int cn1__gcw = 0; \
        while(__atomic_load_n(&(ts)->threadBlockedByGC, __ATOMIC_ACQUIRE)) { \
            cn1GcHandshakeBackoff(&cn1__gcw); \
        } \
    } while(0)

#ifdef CN1_ON_DEVICE_DEBUG
// One row of the variable side-table: a single (line, slot, typeCode) tuple.
// typeCode is the JVM type descriptor first char (I/J/F/D/Z/B/S/C/L/[).
//
// The void* that backs row i is callStackLocalsAddresses[offset][i] -- indexed
// by ROW, not by slot. A JVM slot is reused across disjoint scopes and the two
// occupants need not share a type, so ParparVM emits separate storage for each
// (slot, qualifier) pair it sees ("ilocals_3_" and "locals[3].data.o" can both
// exist). A per-slot address table can only name one of them, which made the
// debugger read an 8-byte object reference out of a 4-byte JAVA_INT and then
// dereference the result -- a wild pointer, and the SIGSEGV behind issue #5333.
// One address per row keeps typeCode and storage in lockstep by construction.
// startLine/endLine bound the source lines this local is in scope for, with
// endLine exclusive. {0, 0} means "always live" — the class file carried no
// scope, or the translator synthesised the local from a store opcode; {n, 0}
// means "live from line n onwards". The debugger uses them to hide a local the
// frame has not reached, which matters most for the reused-slot case: without
// a scope, both occupants show at every breakpoint and one of them displays
// storage belonging to the other's scope.
struct cn1_var_entry {
    int startLine;
    int endLine;
    int slot;
    char typeCode;
};

// Per-method static metadata emitted once per translated method. Held alive
// for the life of the program. The translator emits an instance as
// "static const struct cn1_frame_info __cn1_finfo_<method> = { ... };" and
// passes &__cn1_finfo_<method> into the frame at method entry.
struct cn1_frame_info {
    int classId;
    int methodId;
    int numLocals;
    int varTableCount;
    const struct cn1_var_entry* varTable;
};

// Clears the debug side-channel for the frame about to be pushed.
//
// Only methods that carry a locals side-table publish these two pointers, and
// they publish them AFTER the frame is pushed. Native, eliminated and barebone
// methods never do -- so without this clear such a frame silently inherits
// whatever the previous occupant of that call depth left behind, including a
// callStackLocalsAddresses entry pointing into a C frame that has already
// returned. handleGetStack then reports a bogus method for the frame and
// handleGetLocals reads freed stack memory as object references. Must run
// before callStackOffset is incremented.
#define CN1_DEBUG_FRAME_ENTER(threadStateData) \
    threadStateData->callStackFrameInfo[threadStateData->callStackOffset] = 0; \
    threadStateData->callStackLocalsAddresses[threadStateData->callStackOffset] = 0;

// Set to non-zero by the debugger proxy listener once a proxy has connected
// and is ready to receive events. Read on the hot path of __CN1_DEBUG_INFO,
// so kept as a plain volatile int (predictable branch when zero).
extern volatile int cn1DebuggerActive;

// Cold-path callee invoked by __CN1_DEBUG_INFO when cn1DebuggerActive is set.
// Defined in cn1_debugger.m (iOS port) / a no-op shim in release builds.
extern void cn1_debugger_check(struct ThreadLocalData* threadStateData, int line);

// Marks every object reference the debugger has handed to the IDE, so the
// collector cannot reclaim one while an objectID for it is outstanding.
//
// Without this, validating an id proves only that it is shaped like an object
// of a registered class -- a class word survives reclamation, so a collected
// object still passes -- and the IDE's next field or array read touches freed
// memory. Rooting makes the issued set a liveness guarantee rather than a
// heuristic: an object stays alive exactly as long as an id for it is
// outstanding, and the debugger releases it when the owning thread resumes.
//
// Called from the GC's root pass next to the immortal roots. The strong
// definition is in Ports/iOSPort/nativeSources/cn1_debugger_objects.c; the
// weak stub in cn1_globals.m keeps release builds linking.
extern void cn1_debugger_mark_issued_roots(struct ThreadLocalData* threadStateData);

// Publishes a class's clazz address under its classId, from the constructor the
// translator emits per class.
//
// Declared here rather than only in cn1_debugger.h because the caller is
// generated code, which is compiled in more contexts than the iOS port's own
// headers reach -- the watchOS slice among them, where cn1_debugger.h can be an
// older copy and cn1_debugger_objects.c is not linked at all. Paired with a weak
// no-op in cn1_globals.m for exactly that case, so a target without the debugger
// runtime still compiles and links.
extern void cn1_debugger_register_class(int classId, struct clazz* cls);

#define __CN1_DEBUG_INFO_AT(slot, line) \
    do { \
        *(slot) = (line); \
        if (__builtin_expect(cn1DebuggerActive, 0)) { \
            cn1_debugger_check(threadStateData, (line)); \
        } \
    } while (0)
// Line-info store for a source line whose every instruction is provably
// non-throwing/non-calling (pure arithmetic, local load/store, constants,
// compares, branches, conversions). Such a line can NEVER be the line reported
// in a stack trace -- the trace line is always read at a call/throw/alloc site,
// which lives on a kept line -- so eliding its store is trace-identical. Kept
// fully under the on-device debugger (which steps line-by-line and needs every
// line); elided in release/device builds, where it removes the only per-line hot
// cost (lets clang keep tight loops in registers / vectorize).
#define __CN1_DEBUG_INFO_AT_NT(slot, line) __CN1_DEBUG_INFO_AT(slot, line)
#define __CN1_DEBUG_INFO_NT(line) __CN1_DEBUG_INFO(line)
#else
#define __CN1_DEBUG_INFO_AT(slot, line) (*(slot) = (line))
#define __CN1_DEBUG_INFO_AT_NT(slot, line) do {} while(0)
#define __CN1_DEBUG_INFO_NT(line) do {} while(0)
// Release builds carry no debug side-channel, so pushing a frame costs nothing.
#define CN1_DEBUG_FRAME_ENTER(threadStateData)
#endif

#define __CN1_DEBUG_INFO(line) \
    __CN1_DEBUG_INFO_AT(&threadStateData->callStackLine[threadStateData->callStackOffset - 1], line)

// we need to throw stack overflow error but its unavailable here...
/*#define ENTERING_CODENAME_ONE_METHOD(classIdNumber, methodIdNumber) { \
    assert(threadStateData->callStackOffset < CN1_MAX_STACK_CALL_DEPTH - 1); \
    threadStateData->callStackClass[threadStateData->callStackOffset] = classIdNumber; \
    threadStateData->callStackMethod[threadStateData->callStackOffset] = methodIdNumber; \
    threadStateData->callStackOffset++; \
} \
const int currentCodenameOneCallStackOffset = threadStateData->callStackOffset; 
*/

#define CODENAME_ONE_THREAD_STATE struct ThreadLocalData* threadStateData

// =========================================================================
// LEVER 1: inlined BiBOP bump fast-path (alloc fast-path, perf-tier1)
//
// The escaping `new X()` path is, at codenameOneGcMalloc, an OUT-OF-LINE cross
// translation-unit call (per-class .c -> cn1_globals.c, no LTO in this build),
// which clang therefore cannot inline. This block exposes the minimal BiBOP
// bump surface to every TU so the common case (per-thread current page has a
// free bump slot for this object's size class) can be emitted INLINE at the
// allocation site -- pointer-bump + header stamp, no call -- exactly mirroring
// HotSpot's inlined-TLAB-bump + slow-path-call. The size-class index is a
// compile-time constant per type (sizeof(struct obj__X) is known to clang), so
// CN1_BIBOP_CIDX folds to a literal; an oversized type folds the fast path away
// entirely and falls straight to the slow path.
//
// The bump replicates cn1BibopAlloc's bump branch BIT-FOR-BIT (relaxed load of
// bumpIndex, init the slot with the mark published LAST via an atomic-release
// store, release-store the new cursor AFTER the slot is fully initialized,
// relaxed add to bibopBytesSinceGc). Same memory ordering => the concurrent-GC
// overflow-rescan / sweep correctness argument is unchanged. The free-list and
// page-acquire cases stay on the slow path (codenameOneGcMalloc). isAppSuspended
// handling stays on the slow path too; a suspended-app bibop alloc still
// succeeds and bibopBytesSinceGc still drives collection, so the only behavioural
// delta is a delayed GC-thread restart while suspended (no checksum/leak impact).
//
// Gated by -DCN1_INLINE_ALLOC. OFF => CN1_FAST_NEW(X) is exactly __NEW_X.
// =========================================================================
// Low-memory backpressure pacing (issue #5482). After the OS reports memory
// pressure (iOS didReceiveMemoryWarning -> lowMemoryMode) allocators are slowed
// so the collector can catch up. The throttle parks a legacy-path allocation for
// one millisecond, but at most ONCE PER THREAD PER WINDOW: an unconditional park
// on every allocation caps a thread at ~1000 legacy allocations/second, which
// turns an allocation-heavy load into an apparent hang. With the window, the
// worst case a thread can lose is 1ms per window (about 10%), independent of how
// fast it allocates. Parking for a collector that has actually stopped the world
// (threadBlockedByGC) is a safepoint, not a throttle, and is never skipped.
#ifndef CN1_LOW_MEMORY_PARK_INTERVAL_MS
#define CN1_LOW_MEMORY_PARK_INTERVAL_MS 10
#endif
#ifndef CN1_BIBOP_PAGE_SIZE
#define CN1_BIBOP_PAGE_SIZE (64*1024)
#endif
// RAISED FROM 512. The legacy path costs a calloc, an allObjectsInHeap
// registration and an extent-snapshot entry PER OBJECT; BiBOP costs a bump.
// Measured on the hello corpus, interleaved, every run verified at 2,933 emitted
// files: legacy objects 28,000 -> 2,300, a 92% cut, and the 2048 arm was faster
// in 7 of 8 paired rounds (median 5.54s against 5.59s).
//
// It is a THROUGHPUT change, not a footprint one, and the measurement says why:
// the 26,000 objects that moved carried only ~6MB with them. What is left on the
// legacy path is 2,300 objects holding 140MB -- 61KB each, the class-file and
// emitted-source buffers, far above any size class worth having. Peak footprint
// is unchanged (medians 1556MB against 1560MB).
#ifndef CN1_BIBOP_MAX_OBJECT
#define CN1_BIBOP_MAX_OBJECT 2048
#endif
#ifndef CN1_BIBOP_HEAP_POS
#define CN1_BIBOP_HEAP_POS (-3)
#endif
// A MATURED (graduated) object: memory still lives in its BiBOP slot, but its
// lifecycle now belongs to the legacy mark/sweep -- it is registered in
// allObjectsInHeap and traced by the unconditional legacy rescan (always complete),
// while the BiBOP page sweep SKIPS it (no double-clearing) and its slot is reclaimed
// only after the legacy sweep hands it back (flips it to -3 on death). Poor-man's
// generational: BiBOP = fast young path; survivors mature into the real GC.
#ifndef CN1_BIBOP_ADOPTED
#define CN1_BIBOP_ADOPTED (-4)
#endif
// Slot sizes; a size maps to the smallest class >= size. EIGHT-byte steps up to 128:
// an object struct is a multiple of 8, and with only 16-byte steps every one whose size
// is an odd multiple of 8 paid 8 bytes of padding -- measured on the self-hosting corpus,
// 19MB of the ~350MB live set (VarOp 40 -> 48, Label 104 -> 112, ArrayList 40 -> 48,
// HashMap 72 -> 80, ...). Slots are 8-aligned, which is all a Java object needs (tagged
// references use the low 3 bits). A type that asks for more -- StringBuilder's inline
// storage is aligned(16) -- has a size that is a multiple of its alignment, so it lands
// in a class that is also a multiple of 16 and its slots stay 16-aligned.
// Compile-time size -> class-index. With a constant `sz` (sizeof(...)) clang
// folds the whole chain to an int literal (or -1 for oversized => fast path
// dead-code-eliminated, slow path only).
/* Size -> class index, for the INLINE bump path. It must agree with
 * cn1BibopClassSize[] in cn1_globals.m, and when it does not the failure is
 * silent and expensive: a size this macro answers -1 for skips the inlined bump
 * entirely and goes out of line to __NEW_X -> codenameOneGcMalloc, which then
 * finds a perfectly good class for it in the RUNTIME table and allocates from
 * BiBOP anyway. Everything still works; the object just paid for a call.
 *
 * That is exactly what happened when CN1_BIBOP_MAX_OBJECT went from 512 to 2048:
 * the runtime table learned the eight new classes, this macro did not, and every
 * 513..2048-byte object took the slow path while the census reported the ceiling
 * raise as working. The static assertion below is the part that matters -- it
 * makes the two definitions unable to drift again. */
#define CN1_BIBOP_CIDX(sz) ( \
  (sz)<=24?0: (sz)<=32?1: (sz)<=40?2: (sz)<=48?3: (sz)<=56?4: (sz)<=64?5: (sz)<=72?6: \
  (sz)<=80?7: (sz)<=88?8: (sz)<=96?9: (sz)<=104?10: (sz)<=112?11: (sz)<=120?12: \
  (sz)<=128?13: (sz)<=144?14: (sz)<=160?15: (sz)<=176?16: (sz)<=192?17: (sz)<=224?18: \
  (sz)<=256?19: (sz)<=320?20: (sz)<=384?21: (sz)<=448?22: (sz)<=512?23: \
  (CN1_BIBOP_NUM_CLASSES<=24)?-1: (sz)<=640?24: (sz)<=768?25: (sz)<=896?26: \
  (sz)<=1024?27: (sz)<=1280?28: (sz)<=1536?29: (sz)<=1792?30: (sz)<=2048?31: -1)

/* THE PAGE GEOMETRY IS A COMPILE-TIME CONSTANT, so stop loading it.
 *
 * cn1BibopFormatPage derives all three of slotSize, firstSlotOffset and
 * slotCount from ci and CN1_BIBOP_PAGE_SIZE alone, and ci is a constant at every
 * CN1_FAST_NEW call site (it comes from sizeof(struct obj__X)). Yet the fast path
 * loaded all three back out of the page header, and TWO OF THEM SAT ON THE
 * ADDRESS-GENERATION DEPENDENCY CHAIN:
 *
 *     ldr   x9,  [x0,#144]      ; p
 *     ldrsw x8,  [x9,#28]       ; firstSlotOffset   <- depends on p
 *     ldrsw x11, [x9,#20]       ; slotSize          <- depends on p
 *     ldr   w10, [x9,#32]       ; bumpIndex         <- depends on p
 *     smaddl x8, w11, w10, x8   ; slot address      <- depends on all three
 *
 * As constants that collapses to an add of an immediate plus a shifted index, the
 * multiply-accumulate disappears, and three loads leave the load ports. This is
 * not an instruction-count argument: the two geometry loads are ON the chain that
 * produces the address the allocation then writes through.
 *
 * Identical by construction -- the definitions below are the same expressions
 * cn1BibopFormatPage uses, and the assertions in cn1_globals.m check them against
 * the runtime table rather than trusting that they stay in step. */
#define CN1_BIBOP_CLASS_SIZE(ci) ( \
  (ci)==0?24: (ci)==1?32: (ci)==2?40: (ci)==3?48: (ci)==4?56: (ci)==5?64: (ci)==6?72: \
  (ci)==7?80: (ci)==8?88: (ci)==9?96: (ci)==10?104: (ci)==11?112: (ci)==12?120: \
  (ci)==13?128: (ci)==14?144: (ci)==15?160: (ci)==16?176: (ci)==17?192: (ci)==18?224: \
  (ci)==19?256: (ci)==20?320: (ci)==21?384: (ci)==22?448: (ci)==23?512: (ci)==24?640: \
  (ci)==25?768: (ci)==26?896: (ci)==27?1024: (ci)==28?1280: (ci)==29?1536: \
  (ci)==30?1792: (ci)==31?2048: 0)
#define CN1_BIBOP_HDR_BYTES ((int)((sizeof(CN1BibopPage) + 15) & ~((size_t)15)))
#define CN1_BIBOP_SLOT_COUNT(ci) \
    ((CN1_BIBOP_PAGE_SIZE - CN1_BIBOP_HDR_BYTES) / CN1_BIBOP_CLASS_SIZE(ci))

/* The ceiling must name the LAST class the macro knows, and the macro must
 * refuse anything above it. Either half breaking is a silent slow path. */
_Static_assert(CN1_BIBOP_CIDX(CN1_BIBOP_MAX_OBJECT) == CN1_BIBOP_NUM_CLASSES - 1,
               "CN1_BIBOP_CIDX and CN1_BIBOP_MAX_OBJECT disagree about the last size class");
_Static_assert(CN1_BIBOP_CIDX(CN1_BIBOP_MAX_OBJECT + 1) == -1,
               "CN1_BIBOP_CIDX must refuse sizes above CN1_BIBOP_MAX_OBJECT");

typedef struct CN1BibopPage {
    struct CN1BibopPage* _Atomic nextAll; // append-only global registry chain
    struct CN1BibopPage* nextPool;        // FREE/PARTIAL pool / SWEEP stack link
    /* The one class every object in this page belongs to, or NULL while the page
     * is size-classed and may hold a mix -- which is every page today. Inert for
     * now: nothing sets it, and the validator's R10 check is therefore a no-op.
     * It is here so the rule can be CHECKED the moment typed pages exist, rather
     * than after they have already corrupted something.
     * See vm/BIBOP-INVARIANTS.md rules R10-R13. */
    struct clazz* pageClazz;
    int classIndex;
    int slotSize;
    int slotCount;
    int firstSlotOffset;                  // byte offset of slot 0 from page base
    /* ---- CACHE-LINE SPLIT, and it is load-bearing -----------------------------
     * bumpIndex is written by the OWNING MUTATOR on every single allocation (a
     * store-release), and gcAllocedSinceSweep on every allocation too. The GC
     * fields below -- nextAll walked to enumerate pages, gcGraceMarked,
     * gcLastMarkedEpoch, the sweep bookkeeping -- are read and written by the
     * MARKER THREADS concurrently. With the whole header in one 64/128B line
     * those are the same line, so every allocation and every marker touch
     * ping-pong it between cores.
     *
     * Measured before this split, objectAllocation mean over 5 reps at default
     * trigger, mutator single-threaded on a 16-core machine:
     *
     *     1 marker  78.5ms   2 markers  79.9ms   4 markers 103.4ms   8 markers 103.3ms
     *
     * i.e. 32% slower purely from adding marker threads that have idle cores to
     * run on, while intArithmetic in the same process stayed flat (56.7-61.7).
     * A collector on its own core is supposed to be free to the mutator; this is
     * the mechanism by which it was not.
     *
     * The alignment puts the mutator-hot pair on their own line. Do not move a
     * GC-written field above this boundary.
     *
     * WHAT THIS DID AND DID NOT FIX. After the split the marker-count dependence
     * is GONE -- 1/2/4/8 markers measure 97.1/102.7/95.5/90.4, flat, where before
     * they stepped 78.5/79.9/103.4/103.3. So the collector no longer charges the
     * mutator for having more marker threads, which is the property that matters.
     *
     * It did NOT move the headline. A 40-rep interleaved A/B at 8 markers:
     * mean 103.8 -> 100.9 (0.973) but median 94.2 -> 115.6, with the control
     * (intArithmetic) at 0.998. Those disagree, and with objectAllocation bimodal
     * at ~30ms/~150ms the benchmark cannot resolve the difference. Kept anyway:
     * a field written on every allocation sharing a line with fields concurrent
     * markers write is wrong independently of what one benchmark can measure, and
     * the padding costs ~8 slots of 2047 in a 64KB page.
     *
     * The 73% of objectAllocation that IS the collector survives this fix, so it
     * is the memory traffic of tracing the heap, not header contention. */
    _Atomic int bumpIndex __attribute__((aligned(128))); // next slot to bump-allocate (published)
    void* freeList;                       // intrusive free-list head (slot ptr)
    int freeCount;
    JAVA_BOOLEAN owned;
    // COLLECTOR-ONLY. Set on a page retired BEFORE the running cycle began, when that
    // cycle is a single-core stop-the-world one: its fresh slots predate every root
    // scan, so an unmarked one is garbage and gets no grace. Cleared by the sweep.
    JAVA_BOOLEAN gcPreCycle;
    // REMEMBERED SET, single-core generational mode: one bit per 1KB card of this page,
    // set by the barrier when a young reference is stored into an old object starting in
    // that card. gcRsetQueued says the page is on the dirty-page array; it is the ONLY
    // record of membership, so reformatting a page (which clears the cards) cannot drop
    // other pages' records the way an intrusive link reset did.
    // One bit per 64-byte chunk in which a remembered object STARTS. It was one bit per
    // 1KB card, and the scan traced every object starting on a dirty card: measured,
    // 1.79M objects traced for the ~0.2M actually remembered once owners (rather than
    // blocks) were remembered, since a collection's card also holds its neighbours.
    _Atomic uint64_t gcRsetCards[CN1_BIBOP_PAGE_SIZE / 64 / 64];
    _Atomic int gcRsetQueued;
    // YOUNG-SLOT MAP, so a minor sweep walks what was allocated since the last sweep
    // instead of the whole page. Bump allocation is the range [gcSweptBump, bumpIndex);
    // a slot recycled from the free list sets the bit of the 64-byte chunk it starts in
    // (one shift, no division, on the allocating thread, which owns the page). Cleared
    // by every walk and by format.
    int gcSweptBump;
    uint64_t gcYoungChunks[CN1_BIBOP_PAGE_SIZE / 64 / 64];
    // ---- O(live-pages) sweep bookkeeping (perf-tier1, gated by CN1_BIBOP_NO_FASTSWEEP)
    // These let cn1BibopSweep reclaim an all-dead page or skip an all-live (in-grace)
    // page in O(1) -- without the per-slot walk -- whenever it can PROVE the page is
    // homogeneous. The fields are always present (so the struct layout is identical in
    // A/B builds); only the writes/reads are gated. See cn1BibopSweep for the proof.
    JAVA_BOOLEAN gcAllocedSinceSweep;     // any alloc into the page since last sweep/reset.
                                          //  Alloc paths set it, the FREE-pool reformat
                                          //  resets it, and the grace pass reads it, all
                                          //  via relaxed __atomic ops (those can overlap
                                          //  a concurrent mark). Only the sweep accesses
                                          //  it plain: it runs on the GC thread after
                                          //  mark (program-ordered vs the grace pass) on
                                          //  retired pages no mutator holds, and the
                                          //  pool handoff mutex orders it vs the next
                                          //  owner's stores
                                          // (owner-thread single-writer; published to the
                                          //  GC via the sweep-stack release-push)
    /* Everything from here on is GC-side: keep it off the mutator's line. */
    JAVA_BOOLEAN gcNeedsReclaim __attribute__((aligned(128))); // a survivor carries a finalizer or monitor ->
                                          //  dead slots must reach cn1BibopReclaimSlot
    JAVA_BOOLEAN gcHasMonitors;           // STICKY: a monitor was ever attached to an
                                          //  object in this page (set by
                                          //  cn1BibopNoteMonitorAttached, cleared only by
                                          //  cn1BibopFormatPage). Suppresses the O(1)
                                          //  all-dead reclaim for THIS page only, so a
                                          //  dead monitored slot always reaches
                                          //  cn1BibopReclaimSlot -- replacing the global
                                          //  monitor count that disabled the shortcut for
                                          //  EVERY page whenever any monitor existed
                                          //  (e.g. java.lang.System.LOCK, permanently).
                                          //  (recomputed at every full walk)
    JAVA_BOOLEAN gcHasAdopted;            // STICKY: an object on this page was ever MATURED
                                          //  into the legacy mark/sweep (heapPosition==-4).
                                          //  Suppresses the O(1) all-dead page reclaim for
                                          //  THIS page so its slots always reach the full
                                          //  per-slot walk, which correctly SKIPS live -4
                                          //  slots (owned by legacy) instead of resetting the
                                          //  whole page out from under them (set by
                                          //  cn1MatureObject, cleared only by
                                          //  cn1BibopFormatPage).
    // A legacy sweep returned an adopted slot to this page. Revisit a partial
    // page in this sweep instead of waiting for allocation or the major cadence.
    _Atomic int gcAdoptedDied;
    _Atomic int gcLastMarkedEpoch;        // currentGcMarkValue stamped by gcMarkObject when
                                          //  a slot on this page is marked live (relaxed;
                                          //  idempotent across parallel markers)
    int gcGraceEpoch;                     // upper bound on survivor epochs as of the last
                                          //  full walk (GC-thread only)
    _Atomic int gcGraceMarked;            // slots on this page that were marked live BY A
                                          //  GRACE PASS, i.e. whose previous mark was -1
                                          //  (never reached from a root this cycle). They
                                          //  survive, but nothing has PROVEN them live, so
                                          //  cn1BibopAdaptAfterSweep must not read them as
                                          //  survivors -- an allocation-rate-driven number
                                          //  masquerading as a live set is what made a pure
                                          //  garbage workload look survivor-heavy and
                                          //  diverted it onto the legacy heap. Reset as the
                                          //  sweep reaches the page; relaxed, idempotent
    JAVA_BOOLEAN gcMajorSpliced;          // pulled from a PARTIAL pool by the major sweep, so
                                          //  its slots are a one-off deep-sweep sample rather
                                          //  than the steady-state retirement sample the
                                          //  adaptive trigger is calibrated on (see
                                          //  cn1BibopAdaptAfterSweep). Transient: set at the
                                          //  splice, cleared as the sweep reaches the page
    JAVA_BOOLEAN gcPageReusableAdvice;    // the release used MADV_FREE_REUSABLE (Darwin), so
                                          //  the slot region is marked reusable and MUST be
                                          //  restored with MADV_FREE_REUSE before anything is
                                          //  allocated into it. FALSE when the release used an
                                          //  advice with no pairing (MADV_FREE, MADV_DONTNEED),
                                          //  which is what makes an EXPECTED reuse rejection
                                          //  distinguishable from a genuine failure to restore
    JAVA_BOOLEAN gcPageReleased;          // the slot region has been handed back to the OS
                                          //  (madvise) and must be re-acquired before use.
                                          //  Only ever set on a page that is unreachable
                                          //  from every pool, then published with the page
                                          //  onto bibopReleasedPool under bibopMutex, so
                                          //  no lock-free reader can observe it in flight
#ifdef CN1_GRACE_AUDIT
    int gcAuditSnapshot;                  // QA builds only: bumpIndex at mark start.
                                          //  Relaxed __atomic access everywhere -- the
                                          //  GC writes/reads it during marking while a
                                          //  mutator can reformat the page from the
                                          //  FREE pool
#endif
} CN1BibopPage;
#define CN1_BIBOP_NOTE_RECYCLED(p, o) do { \
        uintptr_t cn1__c = ((uintptr_t)(o) - (uintptr_t)(p)) >> 6; \
        (p)->gcYoungChunks[cn1__c >> 6] |= (uint64_t)1 << (cn1__c & 63); } while(0)

// Per-thread current page per size class; defined in cn1_globals.m. Touched only
// by the owning thread (alloc) and by that same thread on death.
/* The per-thread current page per size class. It USED TO BE `__thread`, and on
 * Darwin that is not a cheap addressing mode -- it is the TLS-descriptor
 * sequence, which is an adrp/ldr pair, a load of the descriptor and an INDIRECT
 * CALL, per allocation. Measured standalone against the same fast path reading
 * the array out of the thread state: 1.508ns -> 1.264ns, -16%, and the
 * disassembly loses a `blr`, two loads and the stack frame that only existed to
 * spill around that call.
 *
 * threadStateData is already parameter one of every generated function, so this
 * is the same storage reached by one `ldr` off x0. Declared here (rather than in
 * the struct's own section) so it sits next to the fast path that reads it. */
extern struct ThreadLocalData* getThreadLocalData(void);
extern _Atomic long bibopBytesSinceGc;
extern _Atomic long bibopGcTriggerBytes;
// Atomic mirror of currentGcMarkValue for mutator-side adaptive-policy
// decisions. currentGcMarkValue itself remains owned by the GC/mark threads.
extern _Atomic int bibopGcEpoch;
#if defined(CN1_GC_INSTRUMENT) && !defined(CN1_DISABLE_BIBOP)
// QA-only diagnostics. Production builds contain neither the counters nor
// their atomic updates.
extern _Atomic long cn1BibopHighThroughputPromotions;
extern _Atomic long cn1BibopBypassActivations;
extern _Atomic long cn1BibopBypassAllocations;
extern _Atomic long cn1BibopFreshPagesScanned;
extern _Atomic long cn1BibopBeltRuns;
extern _Atomic long cn1BibopAdoptedRescanSkips;
#endif
extern int currentGcMarkValue;

// Retire a statically-proven-dead, non-escaping object one cycle early.
// Defined in cn1_globals.m; see the block comment there for why the value it
// writes is an ordinary stale epoch and not a new mark state.
void cn1MarkDeadNow(JAVA_OBJECT o);

// ONE HOOK PER FRAME, NOT ONE PER RETURN.
//
// A method returns through many emitted paths -- one per return type, plus the
// exception variants -- and patching each is how one gets missed. A cleanup
// attribute fires on every ordinary return from the scope regardless of which
// path took it, which is the same mechanism CN1StackBuffer already uses for
// native buffers. Exception exits (longjmp) do NOT run it, and that is fine:
// missing the call skips an optimization, it never breaks a program.
struct CN1RetireScope {
    JAVA_OBJECT* slots[8];
    int count;
};

static inline void cn1RetireScopeLeave(struct CN1RetireScope* s) {
    for(int i = 0 ; i < s->count ; i++) {
        cn1MarkDeadNow(*(s->slots[i]));
    }
}
#ifndef CN1_BIBOP_NO_FASTSWEEP
// Called from monitorEnter (any thread) when a monitor (CN1ThreadData) is freshly
// attached to a heap object. If the object is a BiBOP slot it bumps a global live-monitor
// count so the O(1) all-dead page shortcut is suppressed until every BiBOP monitor has
// been freed by cn1BibopReclaimSlot. No-op for non-BiBOP objects.
extern void cn1BibopNoteMonitorAttached(JAVA_OBJECT obj);
#endif
// Monitor side table (relocated __codenameOneThreadData out of the per-object header).
extern void* cn1MonitorDataGet(JAVA_OBJECT o);
extern void cn1MonitorDataSet(JAVA_OBJECT o, void* data);
extern void* cn1MonitorDataRemove(JAVA_OBJECT o);
extern long long allocationsSinceLastGC;
extern long long totalAllocations;

// LEVER A (perf-tier1, enabled unless -DCN1_DISABLE_DEATOMIC_BYTES): per-object
// BiBOP byte accounting.
// CN1_BIBOP_ACCOUNT_BYTES is called once per allocation (inline fast path AND the
// .m slow path); CN1_BIBOP_FLUSH_BYTES is called once per page-acquire (slow path)
// and at thread death. The global bibopBytesSinceGc is read only by the GC-trigger
// heuristic (cn1BibopMaybeGc) and exchanged to 0 at GC start -- it has NO liveness/
// correctness role -- so deferring the per-thread total into it via plain adds and
// flushing it in bulk is safe; only the trigger cadence shifts (by < nthreads*page,
// negligible vs the 24MB trigger, and already racy today). The bump cursor / mark
// publication ordering is UNCHANGED (those are the GC-visible fields; see report).
#ifndef CN1_DISABLE_DEATOMIC_BYTES
#define CN1_BIBOP_ACCOUNT_BYTES(ts, n) do { \
    (ts)->bibopBytesLocal += (JAVA_LONG)(n); \
    (ts)->bibopEpochBytes += (JAVA_LONG)(n); \
} while(0)
// Flush the per-thread byte accumulator AND, in the same bulk step, the
// isHighFrequencyGC heuristic counters (allocationsSinceLastGC/totalAllocations) --
// which used to be two global stores per object on the hot path. Coarsening them to
// once-per-page-acquire is fine: both are pure heuristics with no correctness role.
#define CN1_BIBOP_FLUSH_BYTES(ts) do { \
    JAVA_LONG __bl = (ts)->bibopBytesLocal; \
    if(__bl) { \
        atomic_fetch_add_explicit(&bibopBytesSinceGc, __bl, memory_order_relaxed); \
        allocationsSinceLastGC += __bl; \
        totalAllocations += __bl; \
        (ts)->bibopBytesLocal = 0; } } while(0)
#else
#define CN1_BIBOP_ACCOUNT_BYTES(ts, n) do { \
    (ts)->bibopEpochBytes += (JAVA_LONG)(n); \
    atomic_fetch_add_explicit(&bibopBytesSinceGc, (n), memory_order_relaxed); \
    allocationsSinceLastGC += (n); totalAllocations += (n); } while(0)
#define CN1_BIBOP_FLUSH_BYTES(ts) do {} while(0)
#endif

#ifdef CN1_GC_CONFORM
// Defined in cn1_globals.m. Declared here because the BiBOP fast paths are inline
// in this header and are the route MOST small objects take -- profiling only
// codenameOneGcMalloc would miss them and blame whatever little reaches it.
//
// There are FOUR entry points, and the profile is only honest if every one of
// them records exactly once: codenameOneGcMalloc, cn1BibopFastAlloc (what
// CN1_FAST_NEW calls), cn1BibopFastAllocNoZero, cn1AllocFused and
// cn1FusedLatin1Begin. cn1BibopAlloc is deliberately NOT hooked -- it is an
// internal callee of three of those and hooking it would double-count.
// Each hook sits on the SUCCESS return rather than at function entry: a fast
// path that returns 0 falls back to __NEW_X -> codenameOneGcMalloc, so an
// entry-side hook counts that allocation twice.
void cn1RecordAllocation(struct clazz* parent, int size);
#endif

// Inlined bump fast path. Returns 0 (slow path: page full / free-list present /
// ineligible / oversized) -> caller falls back to __NEW_X / codenameOneGcMalloc.
// Out of line, and only reached when the inline bump path misses: a slot from the
// current page's FREE LIST, which the inline path does not take. See the definition.
extern JAVA_OBJECT cn1BibopAllocRecycled(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent, int ci);
static inline JAVA_OBJECT cn1BibopFastAlloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent, int ci) {
    if(ci < 0) return (JAVA_OBJECT)0; // oversized: folded away for big types
    // EVERY allocation path must register the class BEFORE the object publishes --
    // including this inline bump. That completes the invariant the GC mark guard
    // depends on: a resolved (current) slot whose class pointer is NOT in the
    // registry can only be a clobbered/reused header, and the guard skips it
    // WITHOUT dereferencing. (An earlier version left this path unhooked and had
    // the guard "adopt" unknown class values after resolving the slot -- a
    // conservatively-reached slot with a reused header then fed garbage into the
    // registry and the register write faulted: the arm64 suite SIGSEGV in
    // cn1GcRegisterClazz.) Cost is one predictable flag-test per alloc, measured
    // at noise level on allocation-heavy renders.
    CN1_CLAZZ_REGISTER(parent);
    CN1BibopPage* p = threadStateData->bibopCurrent[ci];
    if(__builtin_expect(p != (CN1BibopPage*)0 && p->freeList == (void*)0 &&
                        constantPoolObjects != (JAVA_OBJECT*)0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
                        && !threadStateData->nativeAllocationMode
#endif
                        , 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < CN1_BIBOP_SLOT_COUNT(ci), 1)) {
            JAVA_OBJECT o = (JAVA_OBJECT)((char*)p + CN1_BIBOP_HDR_BYTES
                    + (long)bi * CN1_BIBOP_CLASS_SIZE(ci));
#ifdef CN1_BIBOP_VALIDATE
            // INVARIANT: the per-thread current page must be OWNED and match this
            // size class, and the bumped slot must lie inside the page. A violation
            // means bibopCurrent[ci] points at a retired/recycled/reformatted page
            // (the intermittent x64 cn1BibopFastAlloc crash). Abort AT the source,
            // in a normal (non-ASan) build so ASan's layout changes can't mask it.
            if(p->classIndex != ci || p->owned != JAVA_TRUE ||
               (char*)o < (char*)p + p->firstSlotOffset ||
               (char*)o + p->slotSize > (char*)p + CN1_BIBOP_PAGE_SIZE) {
                fprintf(stderr, "CN1BIBOP FASTALLOC CORRUPT: ci=%d p=%p classIndex=%d owned=%d "
                        "bi=%d slotSize=%d slotCount=%d firstSlotOffset=%d o=%p pageEnd=%p\n",
                        ci, (void*)p, p->classIndex, (int)p->owned, bi, p->slotSize,
                        p->slotCount, p->firstSlotOffset, (void*)o,
                        (void*)((char*)p + CN1_BIBOP_PAGE_SIZE));
                fflush(stderr);
                abort();
            }
#endif
            int hdr = (int)sizeof(struct JavaObjectPrototype);
            if(size > hdr) {
                // NOT removable: skipping this is ~2x SLOWER -- uninitialized ref
                // fields get scanned during the mark==-1 grace window and retain
                // floating garbage. The body zero is load-bearing, not overhead.
                memset((char*)o + hdr, 0, size - hdr);
            }
            CN1_OBJ_SET_CLASS(o, parent);
            // __codenameOneReferenceCount + __codenameOneThreadData relocated out of the
            // header (force-visited / monitor side tables); no per-object stores.
            CN1_OBJ_SET_HEAPPOS(o, CN1_BIBOP_HEAP_POS);
            CN1_ALLOC_CENSUS_COUNT(parent, size);
#ifdef DEBUG_GC_ALLOCATIONS
            o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
            o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
            CN1_OBJ_MARK_STORE(o, -1, __ATOMIC_RELEASE);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
#ifndef CN1_BIBOP_NO_FASTSWEEP
            // Mark the page dirty: the O(1) sweep never treats a page that still has
            // fresh mark==-1 (grace-candidate) slots as homogeneous, and the grace
            // pass slot-scans exactly the flagged pages ("-1 slot present" implies
            // "allocated into since last sweep" -- the sweep converts every -1 it
            // sees). Relaxed atomic (compiles to the same plain store on the hot
            // path) because the GRACE PASS reads this concurrently: pre-mark stores
            // are ordered ahead of it by the mark-start thread pause, and a store
            // it can still miss is by definition a during-mark allocation --
            // SATB-covered this cycle and rescanned next cycle since only the
            // sweep (never a concurrent phase) clears the flag.
            __atomic_store_n(&p->gcAllocedSinceSweep, JAVA_TRUE, __ATOMIC_RELAXED);
#endif
            CN1_BIBOP_ACCOUNT_BYTES(threadStateData, CN1_BIBOP_CLASS_SIZE(ci));
            // allocationsSinceLastGC / totalAllocations (the isHighFrequencyGC heuristic)
            // are now bumped in bulk by CN1_BIBOP_FLUSH_BYTES once per page-acquire, not
            // per object -- removing two global-counter stores from the hot path.
#ifdef CN1_GC_CONFORM
            cn1RecordAllocation(parent, size);
#endif
            return o;
        }
    }
    return (JAVA_OBJECT)0;
}

// -------------------------------------------------------------------------
// PER-OBJECT MEMSET ELIMINATION (perf-tier1, init-before-publish).
// cn1BibopFastAllocNoZero is bit-for-bit cn1BibopFastAlloc WITHOUT the body
// memset. Its use is ONLY sound under the init-before-publish discipline the
// translator emits for it (BytecodeMethod.markInitBeforePublish): the object is
// built in a C temp and every field is either written by the inlined constructor
// or explicitly zeroed BEFORE the object is published as a GC root (written to
// an operand-stack slot). Between this alloc and that publish the emitted C is
// straight-line loads/stores ONLY -- no calls, no throws, no safepoint (ctor
// args that may call or throw are hoisted into temps BEFORE this alloc, see
// InlinableConstructor.appendArgTemps) -- and the object is not reachable from
// any root, so neither the precise nor the conservative-native-stack collector
// can trace its (garbage) body: conservative scans only run on threads stopped
// at a safepoint, and this window contains none. The mark==-1 grace window
// additionally keeps the object alive across a sweep (see the "load-bearing
// memset" note in cn1BibopFastAlloc and OVERFLOW RESCAN in cn1_globals.m). The
// header (parentCls / mark / heapPosition) is still initialized here; ONLY the
// body zero is elided.

static inline JAVA_OBJECT cn1BibopFastAllocNoZero(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent, int ci) {
    if(ci < 0) return (JAVA_OBJECT)0; // oversized: folded away for big types
    CN1_CLAZZ_REGISTER(parent); // see cn1BibopFastAlloc: every alloc path registers
    CN1BibopPage* p = threadStateData->bibopCurrent[ci];
    if(__builtin_expect(p != (CN1BibopPage*)0 && p->freeList == (void*)0 &&
                        constantPoolObjects != (JAVA_OBJECT*)0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
                        && !threadStateData->nativeAllocationMode
#endif
                        , 1)) {
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        if(__builtin_expect(bi < CN1_BIBOP_SLOT_COUNT(ci), 1)) {
            JAVA_OBJECT o = (JAVA_OBJECT)((char*)p + CN1_BIBOP_HDR_BYTES
                    + (long)bi * CN1_BIBOP_CLASS_SIZE(ci));
#ifdef CN1_BIBOP_VALIDATE
            if(p->classIndex != ci || p->owned != JAVA_TRUE ||
               (char*)o < (char*)p + p->firstSlotOffset ||
               (char*)o + p->slotSize > (char*)p + CN1_BIBOP_PAGE_SIZE) {
                fprintf(stderr, "CN1BIBOP NOZERO CORRUPT: ci=%d p=%p classIndex=%d owned=%d "
                        "bi=%d slotSize=%d slotCount=%d firstSlotOffset=%d o=%p\n",
                        ci, (void*)p, p->classIndex, (int)p->owned, bi, p->slotSize,
                        p->slotCount, p->firstSlotOffset, (void*)o);
                fflush(stderr);
                abort();
            }
#endif
            // BODY MEMSET ELIDED (init-before-publish -- see comment above).
            // parentCls is deliberately left 0 UNTIL THE PUBLISH: a thread can be
            // SIGNAL-STOPPED at an arbitrary instruction inside the construction
            // window, and the conservative scan then resolves this slot (heapPosition
            // is already CN1_BIBOP_HEAP_POS) and calls gcMarkObject on it -- whose
            // parentCls==0 guard is the ONLY thing preventing it from tracing the
            // garbage body. The translator stores &class__X right before publishing
            // the fully-built object (InlinableConstructor.appendInitBeforePublish);
            // the mark==-1 grace keeps the object alive through the skipped cycle.
            // The explicit 0 store matters: a bump slot recycled by the O(1)
            // homogeneous page reclaim still holds the DEAD previous occupant's
            // class pointer.
            CN1_OBJ_SET_CLASS(o, (struct clazz*)0);
            CN1_OBJ_SET_HEAPPOS(o, CN1_BIBOP_HEAP_POS);
            CN1_ALLOC_CENSUS_COUNT(parent, size);
#ifdef DEBUG_GC_ALLOCATIONS
            o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
            o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
            CN1_OBJ_MARK_STORE(o, -1, __ATOMIC_RELEASE);
            atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
#ifndef CN1_BIBOP_NO_FASTSWEEP
            // relaxed: concurrently read by the grace pass (see cn1BibopFastAlloc)
            __atomic_store_n(&p->gcAllocedSinceSweep, JAVA_TRUE, __ATOMIC_RELAXED);
#endif
            CN1_BIBOP_ACCOUNT_BYTES(threadStateData, CN1_BIBOP_CLASS_SIZE(ci));
#ifdef CN1_GC_CONFORM
            cn1RecordAllocation(parent, size);
#endif
            return o;
        }
    }
    // A RECYCLED page: take a slot its last sweep freed. This used to fall through to
    // __NEW_X and the full slow path for EVERY object once pages came back partial --
    // which is every allocation in a heap small enough to be reused. Profiled at an 8MB
    // trigger, that slow path (cn1BibopAlloc, codenameOneGcMalloc, its pthread_once and
    // the class-initializer call) was ~55% of objectAllocation's main thread, and it is
    // why small triggers were slow. Sound under the same argument as the bump slot above:
    // parentCls is 0 until the constructor publishes it, so a conservative scan of the
    // construction window stops at the guard, and a recycled bump slot already holds a
    // dead occupant's bytes exactly as this one does (the free-list link is in the word
    // zeroed here). The slot reads FREE_MARK until the release store of -1.
    if(__builtin_expect(p != (CN1BibopPage*)0 && p->freeList != (void*)0 &&
                        constantPoolObjects != (JAVA_OBJECT*)0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
                        && !threadStateData->nativeAllocationMode
#endif
                        , 1)) {
        JAVA_OBJECT o = (JAVA_OBJECT)p->freeList;
        p->freeList = *(void**)o;
        p->freeCount--;
        CN1_BIBOP_NOTE_RECYCLED(p, o);
        CN1_OBJ_SET_CLASS(o, (struct clazz*)0);
        CN1_OBJ_SET_HEAPPOS(o, CN1_BIBOP_HEAP_POS);
        CN1_ALLOC_CENSUS_COUNT(parent, size);
#ifdef DEBUG_GC_ALLOCATIONS
        o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
        o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
        CN1_OBJ_MARK_STORE(o, -1, __ATOMIC_RELEASE);
#ifndef CN1_BIBOP_NO_FASTSWEEP
        __atomic_store_n(&p->gcAllocedSinceSweep, JAVA_TRUE, __ATOMIC_RELAXED);
#endif
        CN1_BIBOP_ACCOUNT_BYTES(threadStateData, CN1_BIBOP_CLASS_SIZE(ci));
#ifdef CN1_GC_CONFORM
        cn1RecordAllocation(parent, size);
#endif
        return o;
    }
    return (JAVA_OBJECT)0;
}

// CN1_FAST_NEW(X): inlined alloc + static-init guard for a NEW of concrete type
// X. The static initializer is invoked only when the class isn't initialised yet
// (the bump fast path can be reached for a class whose <clinit> hasn't run,
// because bibopCurrent[] is shared across all classes of the same size class).
#if !defined(CN1_DISABLE_INLINE_ALLOC) && !defined(CN1_DISABLE_BIBOP)
/* THE ALLOCATION GUARD IS KEPT EVEN FOR AN EAGERLY INITIALIZED CLASS, and that is
 * measured rather than assumed. Dropping it here -- the class is initialized at
 * startup, so the check is always false -- made objectAllocation ~11% SLOWER, on every
 * one of four code layouts (function/loop alignment 16/32/64/128; mean 28.63 -> 31.90ms),
 * while the hot loop was otherwise instruction-for-instruction identical and the
 * collector counters did not move. The unproven explanation: the acquire load orders
 * the next iteration's bumpIndex load behind the previous iteration's store-release to
 * the same word, and without it the core speculates that load early and pays for it.
 * Static-method entries and interface thunks DO drop the guard for eager classes; only
 * allocation sites keep it. */
#define CN1_FAST_NEW(X) ({ \
    if(__builtin_expect(!__atomic_load_n(&class__##X.initialized, __ATOMIC_ACQUIRE), 0)) __STATIC_INITIALIZER_##X(threadStateData); \
    JAVA_OBJECT __cn1fo = cn1BibopFastAlloc(threadStateData, sizeof(struct obj__##X), &class__##X, CN1_BIBOP_CIDX(sizeof(struct obj__##X))); \
    if(__builtin_expect(__cn1fo == (JAVA_OBJECT)0, 0)) { \
        __cn1fo = cn1BibopAllocRecycled(threadStateData, sizeof(struct obj__##X), &class__##X, CN1_BIBOP_CIDX(sizeof(struct obj__##X))); \
        if(__cn1fo == (JAVA_OBJECT)0) __cn1fo = __NEW_##X(threadStateData); \
    } \
    __cn1fo; })
// No-body-zero variant (init-before-publish). The slow-path fallback __NEW_X
// still fully zeroes (calloc) -- correct, just un-elided on the rare page-full
// path.
#define CN1_FAST_NEW_NOZERO(X) ({ \
    if(__builtin_expect(!__atomic_load_n(&class__##X.initialized, __ATOMIC_ACQUIRE), 0)) __STATIC_INITIALIZER_##X(threadStateData); \
    JAVA_OBJECT __cn1fo = cn1BibopFastAllocNoZero(threadStateData, sizeof(struct obj__##X), &class__##X, CN1_BIBOP_CIDX(sizeof(struct obj__##X))); \
    if(__builtin_expect(__cn1fo == (JAVA_OBJECT)0, 0)) __cn1fo = __NEW_##X(threadStateData); \
    __cn1fo; })
#else
#define CN1_FAST_NEW(X) __NEW_##X(threadStateData)
#define CN1_FAST_NEW_NOZERO(X) __NEW_##X(threadStateData)
#endif

/// NEW of an iterator the translator proved cannot outlive the loop that creates it.
///
/// Takes the caller's pending stack buffer when there is one, and otherwise allocates
/// exactly as before. It has to exist separately from CN1_FAST_NEW because the hot
/// allocation never reaches __NEW_X at all: the generated code calls
/// CN1_FAST_NEW(java_util_ArrayList_ArrayListIterator), which goes straight to the BiBOP
/// fast path and only falls back to __NEW_X when a page is full. A hook placed in __NEW_X
/// is therefore dead on exactly the path that matters -- measured, before this macro
/// existed: 460 ArrayListIterator allocations with the mechanism on, 460 with it off.
///
/// The static initializer runs FIRST and unconditionally, because taking the buffer skips
/// CN1_FAST_NEW entirely and with it the initialized check that every other path performs.
#define CN1_ITER_NEW(X) ({ \
    if(__builtin_expect(!__atomic_load_n(&class__##X.initialized, __ATOMIC_ACQUIRE), 0)) __STATIC_INITIALIZER_##X(threadStateData); \
    JAVA_OBJECT __cn1io = cn1IterScopeTake(threadStateData, &class__##X, (int)sizeof(struct obj__##X)); \
    if(__cn1io == JAVA_NULL) __cn1io = CN1_FAST_NEW(X); \
    __cn1io; })

#define CN1_THREAD_STATE_SINGLE_ARG CODENAME_ONE_THREAD_STATE
#define CN1_THREAD_STATE_MULTI_ARG CODENAME_ONE_THREAD_STATE,
#define CN1_THREAD_STATE_PASS_ARG threadStateData,
#define CN1_THREAD_STATE_PASS_SINGLE_ARG threadStateData
#define CN1_THREAD_GET_STATE_PASS_ARG getThreadLocalData(),
#define CN1_THREAD_GET_STATE_PASS_SINGLE_ARG getThreadLocalData()
/* Park across a native blocking call. CRITICAL under CN1_CONSERVATIVE_GC_ROOTS: capture this
 * thread's stack pointer + callee-saved registers FIRST (CN1_GC_PARK_CAPTURE), THEN publish
 * threadActive=FALSE. The concurrent GC scans a parked lightweight thread ONLY through that
 * cooperative capture -- and on Windows there is NO signal-stop fallback (see
 * cn1GcScanThreadNativeStack), so without the capture the parked thread's native stack is
 * never scanned and every root on it (e.g. the fresh read buffer in a socketRead) is swept
 * mid-use -> the intermittent Windows WebSocket-reader use-after-free. On platforms with
 * signal-stop this just makes the cheaper cooperative path usable; a no-op when conservative
 * roots are off. */
#define CN1_YIELD_THREAD do { struct ThreadLocalData* __cn1yts = getThreadLocalData(); CN1_GC_PARK_CAPTURE(__cn1yts); __cn1yts->threadActive = JAVA_FALSE; } while(0)
/* The capture is cleared through a macro of its own because gcParkCaptured only
 * EXISTS when conservative roots are compiled in. Referencing it unconditionally
 * meant -DCN1_DISABLE_CONSERVATIVE_GC_ROOTS -- the A/B arm vm/CLAUDE.md documents
 * -- did not build at all, so the one measurement that isolates the conservative
 * scan's cost could not be taken. */
#ifdef CN1_CONSERVATIVE_GC_ROOTS
#define CN1_GC_PARK_RELEASE(ts) do { (ts)->gcParkCaptured = JAVA_FALSE; } while(0)
#else
#define CN1_GC_PARK_RELEASE(ts) do { (void)(ts); } while(0)
#endif
/* Sleeping here sleeps the CARRIER, and a carrier hosts many virtual threads --
 * hostCount is min(workers, cores), so on a 2-core pin 64 connections share 2
 * carriers. One carrier sleeping a millisecond therefore freezes ~32 connections
 * that were ready to run, which is why p50 stays good while p99 does not. Yield
 * instead when this is a virtual thread: the carrier goes and serves the others,
 * and the collector gets its safepoint just the same. The pacing park already
 * did this; this site, the hottest of the four (once per syscall return), did
 * not. Platform threads still sleep -- there is nothing to yield to. */
/* MARK ACTIVE ONLY WHAT THE COLLECTOR CAN STOP -- that is what gcPthreadValid
   means here, and the guard is not an optimisation.
   getThreadLocalData() resolves to the VIRTUAL thread's state while one is running,
   so without it every bracketed native -- a file read, a socket read -- left a
   virtual thread's state threadActive on the way out. Nothing lowers it again until
   the next yield, and the collector's wait for that flag is unbounded while the
   forced-stop escalation that would break the wait is gated on gcPthreadValid,
   permanently false for a virtual thread. A virtual thread that read a file and then
   computed would stall collection forever.
   This is the same hang as the reverted cn1VirtualThreadResume change, reached by a
   different path, which is why removing that assignment alone did not close it.
   Virtual-thread roots do not depend on the flag: cn1GcScanParkedVirtualThreads
   scans every registered virtual thread whether or not it is running. */
#define CN1_RESUME_THREAD do { struct ThreadLocalData* __cn1rts = getThreadLocalData(); CN1_STALL_T0(__cn1rt0); while (__cn1rts->threadBlockedByGC){ if(!cn1VirtualThreadYieldIfVirtual()) { usleep((JAVA_INT)1000); } } if(__cn1rts->gcPthreadValid) { __cn1rts->threadActive = JAVA_TRUE; } CN1_GC_PARK_RELEASE(__cn1rts); CN1_STALL_ADD(__cn1rt0, CN1_STALL_NATIVE_RESUME, __cn1rts); } while(0)

extern struct ThreadLocalData* getThreadLocalData();

/* Monitor-ownership identity for the reentrancy check in monitorEnter/wait.
 * This MUST identify the executing pthread, not the ThreadLocalData struct:
 * one pthread can run under two states (the main-thread-hosted EDT executes
 * with an explicitly-passed CodenameOneThread state, while natives calling
 * getThreadLocalData() get the pthread's own TLS struct), and an id-based
 * check then misses the reentrant case and self-deadlocks. pthread_t is a
 * pointer on Apple/glibc targets; the Windows compat shim's pthread_t is a
 * struct, so there GetCurrentThreadId() supplies the per-thread identity. */
#if defined(_WIN32)
/* The compat shim's pthread_t is {handle, id}; id is GetCurrentThreadId(),
 * unique per thread. Using the shim keeps windows.h out of this header. */
#define CN1_MONITOR_SELF() ((JAVA_LONG)pthread_self().id)
#else
#define CN1_MONITOR_SELF() ((JAVA_LONG)(uintptr_t)pthread_self())
#endif


#define BEGIN_TRY(classId, destinationJump) {\
        threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0; \
        threadStateData->blocks[threadStateData->tryBlockOffset].nativeBuffers = threadStateData->nativeBuffers; \
        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = classId; \
        memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, destinationJump, sizeof(jmp_buf)); \
        threadStateData->tryBlockOffset++; \
    }

#define JUMP_TO(labelToJumpTo, blockOffsetLevel) {\
        threadStateData->tryBlockOffset = methodBlockOffset + blockOffsetLevel; \
        goto labelToJumpTo; \
    }

static inline void releaseForReturn(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread) {
    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;
    threadStateData->callStackOffset--;
}


#define RETURN_AND_RELEASE_FROM_METHOD(returnVal, cn1SizeOfLocals) { \
        releaseForReturn(threadStateData, cn1LocalsBeginInThread); \
        return returnVal; \
    }

#define RETURN_AND_RELEASE_FROM_VOID(cn1SizeOfLocals) { \
        releaseForReturn(threadStateData, cn1LocalsBeginInThread); \
        return; \
    }

extern void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset);

#define RETURN_FROM_METHOD(returnVal, cn1SizeOfLocals) releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \
        return returnVal; \

#define RETURN_FROM_VOID(cn1SizeOfLocals) releaseForReturnInException(threadStateData, cn1LocalsBeginInThread, methodBlockOffset); \
        return; \

#define END_TRY(offset) threadStateData->tryBlockOffset = methodBlockOffset + offset - 1

#define DEFINE_CATCH_BLOCK(destinationJump, labelName, restoreToCn1LocalsBeginInThread) jmp_buf destinationJump; \
{ \
    int currentOffset CN1_UNUSED = threadStateData->tryBlockOffset; \
    if(CN1_TRY_SETJMP(destinationJump)) { \
        threadStateData->callStackOffset = currentCodenameOneCallStackOffset; \
        threadStateData->threadObjectStackOffset = restoreToCn1LocalsBeginInThread; \
        SP = &stack[1]; \
        stack[0].data.o = threadStateData->exception; \
        stack[0].type = CN1_TYPE_OBJECT; \
        goto labelName; \
    } \
}

extern JAVA_VOID java_lang_Throwable_fillInStack__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT ex);


/*
 * When nonzero, an exception that no handler catches prints itself and ends the
 * process instead of being silently discarded. Set by the clean (server-side)
 * target's generated main(); left at 0 everywhere else so app targets keep the
 * behaviour they ship with today.
 */
extern int cn1AbortOnUncaughtException;

extern void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);
extern JAVA_INT  throwException_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);
extern JAVA_BOOLEAN  throwException_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg);
extern JAVA_OBJECT __NEW_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_ClassCastException(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_ArrayStoreException(CODENAME_ONE_THREAD_STATE);
extern void cn1ThrowTypeError(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exception, const char* fromClass, const char* toClass);
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_StackOverflowError(CODENAME_ONE_THREAD_STATE);
// Throws the PREALLOCATED StackOverflowError (pre-filled trace, no allocation,
// no trace building) -- safe to call at stack exhaustion. See cn1_globals.m.
extern void cn1ThrowStackOverflow(CODENAME_ONE_THREAD_STATE);
extern JAVA_OBJECT __NEW_java_lang_ArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE);
extern JAVA_VOID java_lang_ArrayIndexOutOfBoundsException___INIT_____int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1);
extern void throwArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE, int index);
extern JAVA_BOOLEAN throwArrayIndexOutOfBoundsException_R_boolean(CODENAME_ONE_THREAD_STATE, int index);
extern JAVA_OBJECT __NEW_INSTANCE_java_util_ConcurrentModificationException(CODENAME_ONE_THREAD_STATE);
/// Thrown by the intrinsified for-each when the collection is structurally modified
/// under it. Declared here, exactly like the NullPointerException above, because the
/// intrinsic is emitted into arbitrary translation units that do not include the
/// collection headers. Keeping the check means the fast path is a faithful replacement
/// for ArrayListIterator rather than a quietly different loop.
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_IllegalStateException(CODENAME_ONE_THREAD_STATE);
#define CN1_THROW_ISE() throwException(threadStateData, __NEW_INSTANCE_java_lang_IllegalStateException(threadStateData))
#define CN1_THROW_CME()    throwException(threadStateData, __NEW_INSTANCE_java_util_ConcurrentModificationException(threadStateData))
// Same shape as CN1_THROW_CME above: a native raising OutOfMemoryError needs the
// constructor declared, because nothing in the generated headers reaches
// nativeMethods.m. Used by the C collection kernels, where a failed block
// allocation must surface as the Java error rather than as a null table.
extern JAVA_OBJECT __NEW_INSTANCE_java_lang_OutOfMemoryError(CODENAME_ONE_THREAD_STATE);
#define CN1_THROW_OOM() throwException(threadStateData, __NEW_INSTANCE_java_lang_OutOfMemoryError(threadStateData))
#define THROW_NULL_POINTER_EXCEPTION()    throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData))

#define THROW_ARRAY_INDEX_EXCEPTION(index)    throwArrayIndexOutOfBoundsException(threadStateData, index)

/* Marks a throw path. The branch is already predicted with __builtin_expect, but
 * that only orders the blocks -- clang still allocates registers for the throw as
 * though it were ordinary code, and every value the fast path wants in a register
 * has to survive across it. cold says the opposite: the call is rare, it is worth no
 * registers, and its block belongs out of the loop body entirely.
 *
 * It only pays if the exception is CONSTRUCTED behind the call too. A macro that
 * expands to throwException(ts, __NEW_INSTANCE_...(ts)) has the allocation inline on
 * the fast path's register pressure no matter how the call is annotated, which is
 * why the helpers below take no exception argument and build their own. */
#if defined(_MSC_VER)
    #define CN1_COLD
#else
    #define CN1_COLD __attribute__((cold))
#endif

/* A copy of at most 32 bytes WITHOUT a libc call. A memcpy whose length is not a
 * compile-time constant is emitted as a call to memmove, whatever the length --
 * and string building is dominated by exactly such copies: "item-", ":", a few
 * digits. Profiling stringBuilding put _platform_memmove at ~480 of ~3200 mutator
 * samples, all from appends and toString of strings well under 32 bytes.
 *
 * Overlapping fixed-width moves cover every length in a band with two loads and
 * two stores: [8,16] as two 8-byte words anchored at each end, [4,8) as two
 * 4-byte words, (16,32] as two 16-byte halves. Each fixed-size memcpy below is
 * folded to plain loads and stores. The source and destination must not overlap,
 * which holds at every caller: they copy between distinct objects. */
static inline void cn1SmallCopy(void* dst, const void* src, size_t n) {
    char* d = (char*)dst;
    const char* s = (const char*)src;
    if(n >= 8) {
        if(n <= 16) {
            uint64_t a, b;
            memcpy(&a, s, 8); memcpy(&b, s + n - 8, 8);
            memcpy(d, &a, 8); memcpy(d + n - 8, &b, 8);
        } else if(n <= 32) {
            uint64_t a, b, c, e;
            memcpy(&a, s, 8); memcpy(&b, s + 8, 8);
            memcpy(&c, s + n - 16, 8); memcpy(&e, s + n - 8, 8);
            memcpy(d, &a, 8); memcpy(d + 8, &b, 8);
            memcpy(d + n - 16, &c, 8); memcpy(d + n - 8, &e, 8);
        } else {
            memcpy(d, s, n);
        }
    } else if(n >= 4) {
        uint32_t a, b;
        memcpy(&a, s, 4); memcpy(&b, s + n - 4, 4);
        memcpy(d, &a, 4); memcpy(d + n - 4, &b, 4);
    } else if(n > 0) {
        d[0] = s[0];
        d[n - 1] = s[n - 1];
        if(n == 3) {
            d[1] = s[1];
        }
    }
}

/* The static initializer: called from the entry of every static method and every
 * allocation site of its class, behind a check that is false for the whole life of
 * the program after the first call. CN1_COLD alone does not keep it out of line --
 * with ThinLTO clang inlined the ENTIRE initializer (the monitorEnter, the usleep
 * wait loop, the vtable setup) into CommonWorkloads.fib, and to keep registers free
 * for that never-taken path every fib call saved and restored twelve callee-saved
 * registers and materialised ~10 global addresses before doing its eight
 * instructions of work. noinline is what keeps the cold body a call. */
#if defined(_MSC_VER)
    #define CN1_CLINIT_ATTR __declspec(noinline)
#else
    #define CN1_CLINIT_ATTR __attribute__((cold, noinline))
#endif

#if defined(_MSC_VER)
    #define CN1_NORETURN __declspec(noreturn)
#else
    #define CN1_NORETURN __attribute__((noreturn))
#endif

// Throws ArrayIndexOutOfBoundsException and NEVER returns to the caller.
//
// throwException() longjmps to a matching handler, but FALLS OFF THE END when no
// handler is on the block stack -- and the statement-form check macros below have
// no return value to bail with, so they used to fall straight through into the
// very access that was just reported out of bounds. Every Java thread root does
// install a catch-all (Thread.runImpl), but a native callback that enters Java
// need not, so the no-handler path is reachable. Terminating there is strictly
// better than committing an out-of-bounds read.
//
// The noreturn attribute is also what makes bounds checks cheap: without it clang
// must assume the (cold, never-taken) throw call may return and clobber memory, so
// it reloads the array header -- both ->length and ->dataOffset -- on EVERY iteration of
// a scanning loop rather than hoisting them into registers once.
extern CN1_NORETURN void cn1ThrowArrayIndexOrDie(CODENAME_ONE_THREAD_STATE, int index);

// Same contract for the null case. cn1_array_access_validate() -- the reduced
// expression path -- has always thrown NullPointerException on a null array in
// every configuration; the statement-form macros below only did so when
// CN1_INCLUDE_NPE_CHECKS was on, i.e. never in a shipping build.
//
// On iOS/tvOS/watchOS that did NOT leave null unhandled: the port installs a
// SIGSEGV handler that converts EXC_BAD_ACCESS into a NullPointerException
// (installSignalHandlers in CodenameOne_GLAppDelegate.m, mirrored in
// CN1WatchRuntime.m). Checking explicitly here makes the behaviour portable and
// defined rather than dependent on a fault handler that: is switched off by the
// ios.convertSignalsToExceptions=false build hint; does not work under the
// debugger (per its own comment); calls throwException -- which allocates and
// longjmps -- from a signal handler, which is not async-signal-safe; and has no
// counterpart on the other targets (the Windows handler only writes a crash
// report, and the clean/desktop target installs nothing).
//
// Note this never covered the out-of-bounds case above: an out-of-range index
// usually reads adjacent heap rather than faulting, so no signal ever arrives.
extern CN1_NORETURN void cn1ThrowNullPointerOrDie(CODENAME_ONE_THREAD_STATE);

/* Constructing throw helpers for the paths that must RETURN rather than die: a
 * frameless method hands the pending exception back to its caller's frame, and the
 * expression forms have to yield a value. Each allocates its own exception so the
 * allocation sits behind the cold call instead of in the caller's register
 * allocation. */
extern CN1_COLD void cn1ThrowNullPointerHere(CODENAME_ONE_THREAD_STATE);
extern CN1_COLD void cn1ThrowArrayIndexHere(CODENAME_ONE_THREAD_STATE, int index);
extern CN1_COLD JAVA_BOOLEAN cn1ThrowNullPointer_R_boolean(CODENAME_ONE_THREAD_STATE);
extern CN1_COLD JAVA_INT cn1ThrowNullPointer_R_int(CODENAME_ONE_THREAD_STATE);

// Array bounds checks are ALWAYS compiled in, in every configuration.
//
// They used to sit inside #ifdef CN1_INCLUDE_NPE_CHECKS, which is commented out
// just above (CN1_INCLUDE_ARRAY_BOUND_CHECKS was set, but it only ever selected
// between two branches that were BOTH already disabled by the outer NPE gate). So
// in a shipping app an out-of-bounds index became a raw C pointer read past the
// allocation: adjacent heap bytes, or SIGSEGV when it crossed an unmapped page.
// That turned a defect Java defines as an exception into silent corruption or a
// hard crash, and it was never a considered trade: the reduced-expression paths
// (cn1_array_element_* and CN1_ARRAY_CHECK_DIVERGE) already checked in every
// build, so only the un-optimized stack-machine fallback was unsafe.
//
// The single unsigned compare below folds "index < 0" and "index >= length" into
// one branch, and cn1ThrowArrayIndexOrDie is noreturn so the cold path does not
// stop clang hoisting the array header out of a loop. Provably-safe accesses are
// removed earlier and for free by the bounds-check-elimination pass
// (BytecodeMethod.analyzeBoundsChecks), and a method may opt out deliberately via
// the DisableNullAndArrayBoundsChecks annotation.
/* IMPLICIT NULL CHECKS -- a platform capability, not an option.
 *
 * Where the hardware can tell us, an array access does not need to TEST for null:
 * the bounds check already loads ->length, and on a null array that load faults at
 * offsetof(length) == 16. A handler turns a fault below CN1_NULL_GUARD_BYTES into
 * the NullPointerException the test would have thrown, so the compare and branch
 * disappear from every array access in the program while the semantics -- NPE
 * before ArrayIndexOutOfBounds -- are preserved by the ORDER: the length load is
 * what faults, and it happens before the comparison it feeds.
 *
 * Off where we cannot install the handler (Windows needs SEH, not sigaction), and
 * there the explicit test stays. That is a difference in what the platform can do,
 * not a switch anybody chooses.
 *
 * THE EXPOSURE, stated plainly: a wild pointer that happens to land below the
 * guard page becomes a NullPointerException instead of a crash. HotSpot carries
 * the same exposure. What bounds it here is that the window is one page and the
 * handler re-raises anything outside it, so corruption at a real address still
 * dies loudly.
 */
#if !defined(_WIN32) && (defined(__APPLE__) || defined(__linux__))
#define CN1_IMPLICIT_NULL_CHECKS 1
#endif
#define CN1_NULL_GUARD_BYTES 4096
extern void cn1InstallFaultHandler(void);

// One guard, used by every configuration: null then bounds, matching the order
// and the semantics cn1_array_access_validate() has always had. The bounds test
// is a single unsigned compare, so it covers index < 0 and index >= length.
#ifdef CN1_IMPLICIT_NULL_CHECKS
// No null test: the ->length load below faults on a null array and the handler
// raises the NullPointerException. Order is what keeps the semantics right.
#define CN1_ARRAY_ACCESS_GUARD(array, bounds) \
    do { \
        if(__builtin_expect(((unsigned int)(bounds)) >= (unsigned int)(((JAVA_ARRAY)(array))->length), 0)) { cn1ThrowArrayIndexOrDie(threadStateData, bounds); } \
    } while(0)
#else
#define CN1_ARRAY_ACCESS_GUARD(array, bounds) \
    do { \
        if(__builtin_expect((array) == JAVA_NULL, 0)) { cn1ThrowNullPointerOrDie(threadStateData); } \
        if(__builtin_expect(((unsigned int)(bounds)) >= (unsigned int)(((JAVA_ARRAY)(array))->length), 0)) { cn1ThrowArrayIndexOrDie(threadStateData, bounds); } \
    } while(0)
#endif

#define CN1_ARRAY_ACCESS_GUARD_EXPR(array, bounds) \
    (__builtin_expect((array) == JAVA_NULL, 0) ? cn1ThrowNullPointer_R_boolean(threadStateData) \
     : __builtin_expect(((unsigned int)(bounds)) >= (unsigned int)(((JAVA_ARRAY)(array))->length), 0) ? throwArrayIndexOutOfBoundsException_R_boolean(threadStateData, bounds) : JAVA_TRUE)

// DIVERGING form for FRAMELESS methods only: the failure path throws and RETURNS
// from the (frame-free) method instead of falling through, so it needs the
// returning throw helpers rather than the ...OrDie() pair.
#define CN1_ARRAY_CHECK_DIVERGE(array, bounds, retval) \
    do { \
        if(__builtin_expect((array) == JAVA_NULL, 0)) { cn1ThrowNullPointerHere(threadStateData); return retval; } \
        if(__builtin_expect(((unsigned int)(bounds)) >= (unsigned int)(((JAVA_ARRAY)(array))->length), 0)) { cn1ThrowArrayIndexHere(threadStateData, bounds); return retval; } \
    } while(0)

#define CHECK_ARRAY_ACCESS(array_pos, bounds) CN1_ARRAY_ACCESS_GUARD(SP[- array_pos].data.o, bounds)
#define CHECK_ARRAY_ACCESS_EXPR(array, bounds) CN1_ARRAY_ACCESS_GUARD_EXPR(array, bounds)
#define CHECK_ARRAY_ACCESS_WITH_ARGS(array, bounds) CN1_ARRAY_ACCESS_GUARD(array, bounds)

#ifdef CN1_INCLUDE_NPE_CHECKS
    #define CHECK_NPE_TOP_OF_STACK() if(SP[-1].data.o == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
    #define CHECK_NPE_AT_STACK(pos) if(SP[-pos].data.o == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#else
    #define CHECK_NPE_TOP_OF_STACK()
    #define CHECK_NPE_AT_STACK(pos)
#endif

// Currently unused -- the translator has no emission site for it. Kept (and kept
// correct) because native sources may reference it. Renaming CN1_ARRAY_BOUNDS_GUARD
// to CN1_ARRAY_ACCESS_GUARD left this pointing at a macro that no longer existed;
// nothing caught it precisely because nothing expands it.
#define CHECK_ARRAY_BOUNDS_AT_STACK(pos, bounds) CN1_ARRAY_ACCESS_GUARD(PEEK_OBJ(pos), bounds)

#ifdef CN1_INCLUDE_NPE_CHECKS
#define CN1_ARRAY_LENGTH(array) (__builtin_expect((array) == JAVA_NULL, 0) ? cn1ThrowNullPointer_R_int(threadStateData) : (*((JAVA_ARRAY)array)).length)
#else
#define CN1_ARRAY_LENGTH(array) ((*((JAVA_ARRAY)array)).length)
#endif

static inline JAVA_BOOLEAN cn1_array_access_in_bounds(JAVA_OBJECT array, JAVA_INT index) {
    return array != JAVA_NULL && index >= 0 && index < ((JAVA_ARRAY)array)->length;
}

static inline JAVA_BOOLEAN cn1_array_access_validate(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (array == JAVA_NULL) {
        throwException(threadStateData, __NEW_java_lang_NullPointerException(threadStateData));
        return JAVA_FALSE;
    }
    if (index < 0 || index >= ((JAVA_ARRAY)array)->length) {
        throwArrayIndexOutOfBoundsException(threadStateData, index);
        return JAVA_FALSE;
    }
    return JAVA_TRUE;
}

static inline JAVA_INT cn1_array_element_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_INT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_BYTE cn1_array_element_byte(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_FLOAT cn1_array_element_float(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_DOUBLE cn1_array_element_double(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_DOUBLE*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_LONG cn1_array_element_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_LONG*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_OBJECT cn1_array_element_object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return JAVA_NULL;
    }
    return ((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_SHORT cn1_array_element_short(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_CHAR cn1_array_element_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return 0;
    }
    return ((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index];
}

static inline JAVA_VOID cn1_set_array_element_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_INT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_INT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_byte(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_BYTE value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_float(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_FLOAT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_double(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_DOUBLE value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_DOUBLE*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_LONG value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_LONG*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_OBJECT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    CN1_WRITE_BARRIER(array, value); // SATB: record the ref being stored
    CN1_SATB_DELETE(&((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index]); // SATB: preserve overwritten ref
    ((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_short(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_SHORT value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

static inline JAVA_VOID cn1_set_array_element_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array, JAVA_INT index, JAVA_CHAR value) {
    if (!cn1_array_access_in_bounds(array, index) && !cn1_array_access_validate(threadStateData, array, index)) {
        return;
    }
    ((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA((JAVA_ARRAY)array))[index] = value;
}

#define CN1_ARRAY_ELEMENT_INT(array, index) cn1_array_element_int(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_BYTE(array, index) cn1_array_element_byte(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_FLOAT(array, index) cn1_array_element_float(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_DOUBLE(array, index) cn1_array_element_double(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_LONG(array, index) cn1_array_element_long(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_OBJECT(array, index) cn1_array_element_object(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_SHORT(array, index) cn1_array_element_short(threadStateData, array, index)
#define CN1_ARRAY_ELEMENT_CHAR(array, index) cn1_array_element_char(threadStateData, array, index)

// Unchecked array element reads. Emitted by the translator ONLY for accesses the
// prove-safe bounds-check-elimination pass proved are always in range and on a
// non-null array (canonical counted loops indexed by their own induction var,
// bounded by arr.length). No null/bounds branch -> the C compiler is free to keep
// the load in registers and auto-vectorize. If the proof is ever wrong this reads
// out of bounds, so the pass is deliberately conservative and fail-closed.
#define CN1_ARRAY_ELEMENT_INT_NOCHK(array, index) (((JAVA_ARRAY_INT*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_BYTE_NOCHK(array, index) (((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_FLOAT_NOCHK(array, index) (((JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_DOUBLE_NOCHK(array, index) (((JAVA_ARRAY_DOUBLE*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_LONG_NOCHK(array, index) (((JAVA_ARRAY_LONG*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_OBJECT_NOCHK(array, index) (((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_SHORT_NOCHK(array, index) (((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA(array))[(index)])
#define CN1_ARRAY_ELEMENT_CHAR_NOCHK(array, index) (((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA(array))[(index)])

// Unchecked array element WRITES, the store half of the reads above and emitted
// under the same proof. The pass marks a store only when the operand stack walks
// back from it to the same ALOAD a ; ILOAD i pair the loop's test bounded, so the
// value expression in between -- which is why a store cannot be matched by
// adjacency the way a load can -- is accounted for rather than assumed simple.
#define CN1_SET_ARRAY_ELEMENT_INT_NOCHK(array, index, value) (((JAVA_ARRAY_INT*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))
#define CN1_SET_ARRAY_ELEMENT_BYTE_NOCHK(array, index, value) (((JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))
#define CN1_SET_ARRAY_ELEMENT_FLOAT_NOCHK(array, index, value) (((JAVA_ARRAY_FLOAT*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))
#define CN1_SET_ARRAY_ELEMENT_DOUBLE_NOCHK(array, index, value) (((JAVA_ARRAY_DOUBLE*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))
#define CN1_SET_ARRAY_ELEMENT_LONG_NOCHK(array, index, value) (((JAVA_ARRAY_LONG*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))
// NOCHK means the BOUNDS are proven, not that the store is invisible to the collector.
// This used to be the bare assignment, so every bounds-proven object store -- 1,616 of
// them in one self-hosting build -- skipped BOTH SATB halves: during a concurrent mark a
// thread already scanned and released could load an element into a local, overwrite the
// slot here, and leave that element unmarked if the slot was its only snapshot path. The
// checked setter (cn1_set_array_element_object) always carried both halves; this now
// carries exactly the same pair, and outside a mark it costs the one gated load.
#define CN1_SET_ARRAY_ELEMENT_OBJECT_NOCHK(array, index, value) do { \
        JAVA_OBJECT cn1__na = (JAVA_OBJECT)(array); \
        JAVA_OBJECT* cn1__ns = &((JAVA_ARRAY_OBJECT*) CN1_ARRAY_DATA((JAVA_ARRAY)cn1__na))[(index)]; \
        JAVA_OBJECT cn1__nv2 = (JAVA_OBJECT)(value); \
        CN1_WRITE_BARRIER(cn1__na, cn1__nv2); \
        CN1_SATB_DELETE(cn1__ns); \
        *cn1__ns = cn1__nv2; } while(0)
#define CN1_SET_ARRAY_ELEMENT_SHORT_NOCHK(array, index, value) (((JAVA_ARRAY_SHORT*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))
#define CN1_SET_ARRAY_ELEMENT_CHAR_NOCHK(array, index, value) (((JAVA_ARRAY_CHAR*) CN1_ARRAY_DATA((JAVA_ARRAY)(array)))[(index)] = (value))

#define CN1_SET_ARRAY_ELEMENT_INT(array, index, value) cn1_set_array_element_int(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_BYTE(array, index, value) cn1_set_array_element_byte(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_FLOAT(array, index, value) cn1_set_array_element_float(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_DOUBLE(array, index, value) cn1_set_array_element_double(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_LONG(array, index, value) cn1_set_array_element_long(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_OBJECT(array, index, value) cn1_set_array_element_object(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_SHORT(array, index, value) cn1_set_array_element_short(threadStateData, array, index, value)
#define CN1_SET_ARRAY_ELEMENT_CHAR(array, index, value) cn1_set_array_element_char(threadStateData, array, index, value)

extern JAVA_VOID monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);
extern JAVA_VOID monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

extern void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array);


#define MONITOR_ENTER() monitorEnter(threadStateData, POP_OBJ())
#define MONITOR_EXIT() monitorExit(threadStateData, POP_OBJ())

extern void gcReleaseObj(JAVA_OBJECT o);

// ARRAY PAYLOAD PLACEMENT, IN ONE PLACE, BECAUSE THE SIZE AND THE ADDRESS DISAGREED.
//
// `data` is the LAST member of the header, so the two spellings of "where the
// elements start" -- (char*)a + a->dataOffset, which every placement site
// uses, and (char*)a + sizeof(struct JavaArrayPrototype), which the sweep uses --
// are the SAME address. The static assert below is what makes that a fact rather
// than a coincidence of the current field order.
//
// The ADDRESS was always computed that way; the SIZE asked for one pointer more.
// allocArray requested `sizeof(header) + elements + sizeof(void*)` while the
// payload has always ended at `sizeof(header) + elements`, so every heap array
// carried eight dead bytes.
//
// THIS IS NOT A CONSEQUENCE OF THE 40 -> 32 HEADER NARROWING, and saying so was
// wrong twice over. On origin/master the header is 40 (dimensions and
// primitiveSize are `int` there), `data` sits at 32, the payload begins at
// &data+8 == 40 == sizeof(struct), and allocArray asks for 40 + n + 8. Master
// over-allocates exactly the same eight bytes. The narrowing moved both numbers
// together and changed nothing about the bug; it is simply older than either.
// CN1_FUSED_ARR_BYTES below carried the identical `+ sizeof(void*)` and was
// corrected separately, so it was never the counterexample that revealed this.
//
// Eight bytes is the raw figure and it understates the cost: BiBOP rounds
// 32+payload up to a size class, so the real saving is a whole class step --
// measured, a char[8] moved from the 64-byte class to the 48 (-25%) and an
// Object[0] from 48 to 32 (-33%). See vm/benchmarks/memshape.sh.
//
// WHAT THIS REMOVED, AND WHY IT MATTERS ELSEWHERE: the slack used to absorb small
// fixed-width overruns. `spare` is now sizeclass(32+payload) - (32+payload), which
// is ZERO whenever 32+payload lands on a class boundary -- char[8], int[8] and
// Object[8] all do. Any native that writes more than `length * primitiveSize`
// bytes into a payload is now a heap corruption rather than a silent scribble on
// padding; IOSNative.m's NSData converters were exactly that and are fixed.
#define CN1_ARRAY_PAYLOAD_OFFSET (sizeof(struct JavaArrayPrototype))
#define CN1_ARRAY_ALLOC_BYTES(actualSize) (CN1_ARRAY_PAYLOAD_OFFSET + (size_t)(actualSize))
#define CN1_ARRAY_PAYLOAD_PTR(a) ((void*)((char*)(a) + CN1_ARRAY_PAYLOAD_OFFSET))

extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern JAVA_OBJECT allocArrayAligned(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim, int alignment);
// Fused-object block allocator (owner + encapsulated child in ONE BiBOP slot;
// see the FUSED OBJECTS comment in cn1_globals.m). NULL => caller must fall
// back to ordinary separate allocations (oversize / BiBOP unavailable).
extern JAVA_OBJECT cn1AllocFused(CODENAME_ONE_THREAD_STATE, int totalSize, struct clazz* cls);

// Bytes a fused primitive-array child occupies inside the owner block: array
// header + elements, 8-aligned so a
// following child's header is aligned.
#define CN1_FUSED_ARR_BYTES(len, esz) \
    ((int)((CN1_ARRAY_ALLOC_BYTES((size_t)(len) * (esz)) + 7) & ~(size_t)7))

// Embedded primitive arrays have no independent mark or sweep lifetime.
#define CN1_GC_EMBEDDED_PRIMITIVE (-5)
#define CN1_GC_STACK_BUILDER (-6)

// Lay out a fused child array INSIDE an owner block freshly returned by
// cn1AllocFused (zeroed, owner parentCls set). The child gets a full ordinary
// array header -- every reader sees a normal array -- but no independent GC
// identity: the page sweep walks slot boundaries only, and the conservative
// resolver maps any pointer into the block to the OWNER (slot base), so the
// child lives and dies with it. A distinct negative heap position identifies
// this storage to precise tracing. Element placement mirrors allocArray.
static inline JAVA_OBJECT cn1FusedInstallPrimArray(JAVA_OBJECT owner, int off, struct clazz* acls, int esz, int len) {
    struct JavaArrayPrototype* a = (struct JavaArrayPrototype*)((char*)owner + off);
    CN1_OBJ_SET_CLASS(a, acls);
    CN1_OBJ_SET_MARK(a, -1);   // not yet published; see codenameOneGcMalloc
    CN1_OBJ_SET_HEAPPOS(a, CN1_GC_EMBEDDED_PRIMITIVE);
    a->length = len;
    a->dimensions = 1;
    a->primitiveSize = esz;
    /* Set unconditionally, including for a zero-length array: there is nothing to
     * read there, and an offset of 0 would make CN1_ARRAY_DATA point at the header. */
    a->dataOffset = (unsigned short)CN1_ARRAY_PAYLOAD_OFFSET;
    return (JAVA_OBJECT)a;
}
// Register an object referenced only from C globals as a permanent GC root.
extern void cn1AddImmortalRoot(JAVA_OBJECT o);
// Flag a BiBOP object's page as holding a native peer (cached NSString etc.):
// its dead slots then always reach cn1BibopReclaimSlot, which releases the
// peer -- instead of the O(1) all-dead page reclaim, which would leak it.
extern void cn1BibopNoteNativePeer(JAVA_OBJECT o);
// Clear a fresh String's cached NSString peer. The field is only DECLARED on
// ObjC targets (see ByteCodeClass.targetGuardFor), so the stores that used to
// zero it by hand have to compile away everywhere else. Fused and NoZero slots
// can hand back garbage, so on iOS this must still happen.
#if defined(__APPLE__) && defined(__OBJC__)
#define CN1_STRING_CLEAR_PEER(s) do { (s)->java_lang_String_nsString = 0; } while(0)
#else
#define CN1_STRING_CLEAR_PEER(s) do { (void)(s); } while(0)
#endif
extern JAVA_OBJECT allocMultiArray(int* lengths, struct clazz* type, int primitiveSize, int dim);
#define CN1_SIMD_ALIGNMENT 16
/* Maximum payload size we are willing to alloca() on the per-thread stack
 * before falling back to a regular GC-tracked heap allocation. iOS secondary
 * threads default to a 512 KB stack, so any allocation that scales with image
 * dimensions (e.g. createMask / applyMask) can blow the stack at modest sizes
 * (a 410x410 ARGB image needs ~656 KB of int scratch). The cap is intentionally
 * conservative: the fallback path costs a normal heap allocation (cheap
 * relative to the SIMD work that follows it), while a stack overflow is fatal
 * with no chance to recover. */
#define CN1_SIMD_STACK_HEAP_THRESHOLD (32 * 1024)
#define CN1_SIMD_STACK_PRIMITIVE_ARRAY(length, arrayClass, primitiveSize) \
    __extension__ ({ \
        int __cn1StackLength = (length); \
        const int __cn1Alignment = CN1_SIMD_ALIGNMENT; \
        int __cn1ActualSize = __cn1StackLength * (primitiveSize); \
        JAVA_OBJECT __cn1Result; \
        if (__cn1StackLength < 0 || __cn1ActualSize > CN1_SIMD_STACK_HEAP_THRESHOLD) { \
            /* Too large to safely place on the stack - fall back to a regular */ \
            /* aligned heap allocation. The returned array still satisfies the */ \
            /* SIMD alignment contract; only the lifetime widens (GC-managed */ \
            /* instead of method-local), which is harmless for callers. */ \
            __cn1Result = allocArrayAligned(threadStateData, __cn1StackLength, (arrayClass), (primitiveSize), 1, __cn1Alignment); \
        } else { \
            /* header + payload + alignment slack for the payload start */ \
            char* __cn1StackMem = (char*)__builtin_alloca(CN1_ARRAY_ALLOC_BYTES(__cn1ActualSize) + __cn1Alignment - 1); \
            JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)__cn1StackMem; \
            *__cn1StackArray = (struct JavaArrayPrototype){DEBUG_GC_INIT (arrayClass), 0, 0, __cn1StackLength, 1, (primitiveSize), (unsigned short)CN1_ARRAY_PAYLOAD_OFFSET}; \
            if (__cn1ActualSize > 0) { \
                char* __cn1Data = (char*)CN1_ARRAY_PAYLOAD_PTR(__cn1StackArray); \
                /* round the payload start up by adding alignment-1 then masking off the low bits */ \
                uintptr_t __cn1Aligned = (((uintptr_t)__cn1Data) + ((uintptr_t)__cn1Alignment - 1)) & ~((uintptr_t)__cn1Alignment - 1); \
                /* Stored as a DISTANCE from the header, which is what makes the header \
                 * 24 bytes instead of 32. The slack above is what can push it past \
                 * CN1_ARRAY_PAYLOAD_OFFSET, and it is bounded by alignment-1. */ \
                __cn1StackArray->dataOffset = (unsigned short)(__cn1Aligned - (uintptr_t)__cn1StackArray); \
            } \
            __cn1Result = (JAVA_OBJECT)__cn1StackArray; \
        } \
        __cn1Result; \
    })
#define CN1_SIMD_ALLOCA_BYTE(length) CN1_SIMD_STACK_PRIMITIVE_ARRAY((length), &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE))
#define CN1_SIMD_ALLOCA_INT(length) CN1_SIMD_STACK_PRIMITIVE_ARRAY((length), &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT))
#define CN1_SIMD_ALLOCA_FLOAT(length) CN1_SIMD_STACK_PRIMITIVE_ARRAY((length), &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT))
#define CN1_SIMD_ALLOCA_BYTE_ZEROED(length) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_BYTE(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(CN1_ARRAY_DATA(__cn1StackArray), 0, (size_t)__cn1InitLength); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_INT_ZEROED(length) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_INT(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(CN1_ARRAY_DATA(__cn1StackArray), 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_INT)); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_FLOAT_ZEROED(length) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_FLOAT(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(CN1_ARRAY_DATA(__cn1StackArray), 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_FLOAT)); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_BYTE_FILLED(length, value) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_BYTE(__cn1InitLength); \
        if (__cn1InitLength > 0) { \
            memset(CN1_ARRAY_DATA(__cn1StackArray), (value), (size_t)__cn1InitLength); \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_INT_FILLED(length, value) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY_INT __cn1InitValue = (value); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_INT(__cn1InitLength); \
        JAVA_ARRAY_INT* __cn1Data = (JAVA_ARRAY_INT*)CN1_ARRAY_DATA(__cn1StackArray); \
        if (__cn1InitValue == 0 && __cn1InitLength > 0) { \
            memset(CN1_ARRAY_DATA(__cn1StackArray), 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_INT)); \
        } else { \
            for (int __cn1FillIndex = 0; __cn1FillIndex < __cn1InitLength; __cn1FillIndex++) { \
                __cn1Data[__cn1FillIndex] = __cn1InitValue; \
            } \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
#define CN1_SIMD_ALLOCA_FLOAT_FILLED(length, value) \
    __extension__ ({ \
        int __cn1InitLength = (length); \
        JAVA_ARRAY_FLOAT __cn1InitValue = (value); \
        JAVA_ARRAY __cn1StackArray = (JAVA_ARRAY)CN1_SIMD_ALLOCA_FLOAT(__cn1InitLength); \
        JAVA_ARRAY_FLOAT* __cn1Data = (JAVA_ARRAY_FLOAT*)CN1_ARRAY_DATA(__cn1StackArray); \
        if (__cn1InitValue == 0.0f && __cn1InitLength > 0) { \
            memset(CN1_ARRAY_DATA(__cn1StackArray), 0, (size_t)__cn1InitLength * sizeof(JAVA_ARRAY_FLOAT)); \
        } else { \
            for (int __cn1FillIndex = 0; __cn1FillIndex < __cn1InitLength; __cn1FillIndex++) { \
                __cn1Data[__cn1FillIndex] = __cn1InitValue; \
            } \
        } \
        (JAVA_OBJECT)__cn1StackArray; \
    })
extern JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, struct clazz* parentType, struct clazz* childType, int primitiveSize);
extern JAVA_OBJECT alloc3DArray(CODENAME_ONE_THREAD_STATE, int length1, int length2, int length3, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, int primitiveSize);

extern void lockCriticalSection();
extern void unlockCriticalSection();
extern void lockThreadHeapMutex();
extern void unlockThreadHeapMutex();

extern struct clazz class_array1__JAVA_BOOLEAN;
extern struct clazz class_array2__JAVA_BOOLEAN;
extern struct clazz class_array3__JAVA_BOOLEAN;

extern struct clazz class_array1__JAVA_CHAR;
extern struct clazz class_array2__JAVA_CHAR;
extern struct clazz class_array3__JAVA_CHAR;

extern struct clazz class_array1__JAVA_BYTE;
extern struct clazz class_array2__JAVA_BYTE;
extern struct clazz class_array3__JAVA_BYTE;

extern struct clazz class_array1__JAVA_SHORT;
extern struct clazz class_array2__JAVA_SHORT;
extern struct clazz class_array3__JAVA_SHORT;

extern struct clazz class_array1__JAVA_INT;
extern struct clazz class_array2__JAVA_INT;
extern struct clazz class_array3__JAVA_INT;

extern struct clazz class_array1__JAVA_LONG;
extern struct clazz class_array2__JAVA_LONG;
extern struct clazz class_array3__JAVA_LONG;

extern struct clazz class_array1__JAVA_FLOAT;
extern struct clazz class_array2__JAVA_FLOAT;
extern struct clazz class_array3__JAVA_FLOAT;

extern struct clazz class_array1__JAVA_DOUBLE;
extern struct clazz class_array2__JAVA_DOUBLE;
extern struct clazz class_array3__JAVA_DOUBLE;

#ifdef CN1_GC_VERIFY
extern void cn1GcVerifyFieldType(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_OBJECT value,
                                 int declaredClassId, const char* fieldName);
#endif
#define CN1_GC_CYCLE_IDLE    0
#define CN1_GC_CYCLE_RUNNING 1
#define CN1_GC_CYCLE_FROZEN  2
extern _Atomic int cn1GcCycleState;
extern JAVA_OBJECT newString(CODENAME_ONE_THREAD_STATE, int length, JAVA_CHAR data[]);
/**
 * Like newStringFromCString but DECODES, in the PLATFORM's encoding, instead of
 * widening bytes. Use it for text that came from outside the program (argv, the
 * environment); the widening one is right only for generated literals, which are
 * ASCII plus ~~uXXXX escapes.
 *
 * UTF-8 on POSIX; the active code page on Windows, where the CRT has already
 * converted the wide command line and environment down to it. Not named FromUtf8
 * for exactly that reason.
 */
extern JAVA_OBJECT newStringFromNative(CODENAME_ONE_THREAD_STATE, const char* str);
/**
 * UTF-8 to String, always, for a run of KNOWN LENGTH. Use it for data that is
 * UTF-8 by specification wherever it runs -- SQLite text, HTTP/2 header octets --
 * where newStringFromNative's platform encoding would be the ANSI code page on
 * Windows, and where newStringFromCString is not a decoder at all.
 */
extern JAVA_OBJECT newStringFromUtf8Len(CODENAME_ONE_THREAD_STATE, const char* str, int length);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);
extern JAVA_OBJECT newStringFromAsciiLen(CODENAME_ONE_THREAD_STATE, const char *src, int len);
// Single-allocation fused compact-String builder (see cn1_globals.m). Returns a valid empty
// String + inline byte buffer; fill it, then publish the real length with cn1FusedLatin1End().
// NULL => use the 2-object fallback.
extern JAVA_OBJECT cn1FusedLatin1Begin(CODENAME_ONE_THREAD_STATE, int len, JAVA_ARRAY_BYTE** dst);
// Publish the finished length: the fused String was created empty (count=0, a valid intermediate);
// set the real count LAST, after every byte is written, so a concurrent GC never sees count>0 over
// an unfinished value. Single word store.
#define cn1FusedLatin1End(so, n) (((struct obj__java_lang_String*)(so))->java_lang_String_count = (n))
extern JAVA_OBJECT cn1MainArgs(CODENAME_ONE_THREAD_STATE, int argc, char* argv[]);
extern void initConstantPool();
/* Generated into cn1_class_method_index: runs the static initializer of every class
 * and interface without a <clinit>, once, from the start of initConstantPool. */
extern void cn1EagerInitClasses(CODENAME_ONE_THREAD_STATE);
extern void cn1EagerInitPureClasses(CODENAME_ONE_THREAD_STATE);
extern void cn1RunEagerInitializer(CODENAME_ONE_THREAD_STATE, void (*initializer)(CODENAME_ONE_THREAD_STATE), const char* className);

extern void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId);
static inline void cn1_init_method_stack_fast(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, JAVA_BOOLEAN fullClear) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) {
        THROW_NULL_POINTER_EXCEPTION();
    }
#endif
    if (threadStateData->callStackOffset >= CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT - 1) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    /* The call-depth guard above does not protect the operand/locals stack: a
     * deep recursion of methods with large frames can exhaust threadObjectStack
     * before the call-depth limit, and without this check initMethodStack would
     * memset/write past the buffer end -> access violation instead of a catchable
     * StackOverflowError. The 1024-slot margin leaves room to build+throw it. */
    if (threadStateData->threadObjectStackOffset + localsStackSize + stackSize >= CN1_MAX_OBJECT_STACK_DEPTH - 1024) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    if (fullClear) {
        memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0,
                sizeof(struct elementStruct) * (localsStackSize + stackSize));
    } else {
        /*
         * Primitive-only fast frames intentionally use the same memset strategy.
         * A per-slot type-only loop was measurably slower in benchmarks and did
         * not improve generated-code performance.
         */
        memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0,
                sizeof(struct elementStruct) * (localsStackSize + stackSize));
    }
    threadStateData->threadObjectStackOffset += localsStackSize + stackSize;
    CN1_DEBUG_FRAME_ENTER(threadStateData)
    threadStateData->callStackOffset++;
}

// Inline frame setup WITH stack-trace name recording. Methods that make calls can't use
// the fast leaf frame (the trace must keep their frame), but they were paying a non-inline
// initMethodStack() call per invocation -- brutal for hot recursive methods (fib: ~30M
// calls, two extern calls each with releaseForReturn). This inlines it so the C compiler
// folds the offset arithmetic and the call overhead disappears.
static inline void cn1InitMethodStackInline(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#endif
    if (threadStateData->callStackOffset >= CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT - 1) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    if (threadStateData->threadObjectStackOffset + localsStackSize + stackSize >= CN1_MAX_OBJECT_STACK_DEPTH - 1024) {
        cn1ThrowStackOverflow(threadStateData);
        return;
    }
    memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0, sizeof(struct elementStruct) * (localsStackSize + stackSize));
    threadStateData->threadObjectStackOffset += localsStackSize + stackSize;
    threadStateData->callStackClass[threadStateData->callStackOffset] = classNameId;
    threadStateData->callStackMethod[threadStateData->callStackOffset] = methodNameId;
    CN1_DEBUG_FRAME_ENTER(threadStateData)
    threadStateData->callStackOffset++;
}

// How SP is declared, and why it is a parameter rather than a fixed token.
//
// ParparVM compiles a try/catch into a setjmp region, so the edges leaving it
// are abnormal ones. SP is modified all through that region -- every push and
// pop moves it -- and it is read again after a longjmp lands in the catch.
// C99 7.13.2.1p3 makes the value of a non-volatile automatic that was modified
// since the setjmp *indeterminate* after a longjmp, so the old unqualified
// declaration was undefined behaviour in every method that catches anything.
//
// It stayed invisible until a toolchain took it up: gcc on musl refuses the
// translation unit outright with `internal compiler error: SSA corruption` --
// "Unable to coalesce ssa_names 57 and 58 which are marked as MUST COALESCE,
// SP_57(ab) and SP_58(ab), during RTL pass: expand" -- because two versions of
// SP live across an abnormal edge must occupy one register and cannot. Any Java
// method with a short-circuit condition inside a try was enough to trigger it,
// which means app code, not just the framework.
//
// `volatile` goes on the POINTER, not the pointee: `struct elementStruct* volatile SP`
// keeps SP itself in memory so longjmp cannot clobber it, while stack slot
// accesses through it stay ordinary. Writing `volatile struct elementStruct* SP`
// would instead make every operand-stack read and write volatile, which is a
// different -- and expensive -- statement.
//
// Only frames that actually emit setjmp pay for it; BytecodeMethod picks the
// _VSP variant using the same test it already uses to decide whether locals are
// volatile.
#define CN1_DECLARE_SP(spQualifier, spPosition) \
    struct elementStruct* spQualifier SP CN1_UNUSED = &stack[spPosition];

// we need to zero out the values with memset otherwise we will run into a problem
// when invoking release on pre-existing object which might be garbage
#define DEFINE_METHOD_STACK_IMPL(spQualifier, stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals CN1_UNUSED = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition) \
    cn1InitMethodStackInline(threadStateData, (JAVA_OBJECT)1, stackSize, localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset;\
    int* const cn1CurrentLine CN1_UNUSED = &threadStateData->callStackLine[currentCodenameOneCallStackOffset - 1]; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

#define DEFINE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) DEFINE_METHOD_STACK_IMPL(, stackSize, localsStackSize, spPosition, classNameId, methodNameId)
#define DEFINE_METHOD_STACK_VSP(stackSize, localsStackSize, spPosition, classNameId, methodNameId) DEFINE_METHOD_STACK_IMPL(volatile, stackSize, localsStackSize, spPosition, classNameId, methodNameId)

#define DEFINE_INSTANCE_METHOD_STACK_IMPL(spQualifier, stackSize, localsStackSize, spPosition, classNameId, methodNameId) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals CN1_UNUSED = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition) \
    cn1InitMethodStackInline(threadStateData, __cn1ThisObject, stackSize, localsStackSize, classNameId, methodNameId); \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset;\
    int* const cn1CurrentLine CN1_UNUSED = &threadStateData->callStackLine[currentCodenameOneCallStackOffset - 1]; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

#define DEFINE_INSTANCE_METHOD_STACK(stackSize, localsStackSize, spPosition, classNameId, methodNameId) DEFINE_INSTANCE_METHOD_STACK_IMPL(, stackSize, localsStackSize, spPosition, classNameId, methodNameId)
#define DEFINE_INSTANCE_METHOD_STACK_VSP(stackSize, localsStackSize, spPosition, classNameId, methodNameId) DEFINE_INSTANCE_METHOD_STACK_IMPL(volatile, stackSize, localsStackSize, spPosition, classNameId, methodNameId)

#define DEFINE_METHOD_STACK_FAST_REF_IMPL(spQualifier, stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals CN1_UNUSED = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition) \
    cn1_init_method_stack_fast(threadStateData, (JAVA_OBJECT)1, stackSize, localsStackSize, JAVA_TRUE); \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset;\
    int* const cn1CurrentLine CN1_UNUSED = &threadStateData->callStackLine[currentCodenameOneCallStackOffset - 1]; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

#define DEFINE_METHOD_STACK_FAST_REF(stackSize, localsStackSize, spPosition) DEFINE_METHOD_STACK_FAST_REF_IMPL(, stackSize, localsStackSize, spPosition)
#define DEFINE_METHOD_STACK_FAST_REF_VSP(stackSize, localsStackSize, spPosition) DEFINE_METHOD_STACK_FAST_REF_IMPL(volatile, stackSize, localsStackSize, spPosition)

#define DEFINE_INSTANCE_METHOD_STACK_FAST_REF_IMPL(spQualifier, stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals CN1_UNUSED = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition) \
    cn1_init_method_stack_fast(threadStateData, __cn1ThisObject, stackSize, localsStackSize, JAVA_TRUE); \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset;\
    int* const cn1CurrentLine CN1_UNUSED = &threadStateData->callStackLine[currentCodenameOneCallStackOffset - 1]; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

#define DEFINE_INSTANCE_METHOD_STACK_FAST_REF(stackSize, localsStackSize, spPosition) DEFINE_INSTANCE_METHOD_STACK_FAST_REF_IMPL(, stackSize, localsStackSize, spPosition)
#define DEFINE_INSTANCE_METHOD_STACK_FAST_REF_VSP(stackSize, localsStackSize, spPosition) DEFINE_INSTANCE_METHOD_STACK_FAST_REF_IMPL(volatile, stackSize, localsStackSize, spPosition)

#define DEFINE_METHOD_STACK_FAST_PRIMITIVE_IMPL(spQualifier, stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals CN1_UNUSED = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition) \
    cn1_init_method_stack_fast(threadStateData, (JAVA_OBJECT)1, stackSize, localsStackSize, JAVA_FALSE); \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset;\
    int* const cn1CurrentLine CN1_UNUSED = &threadStateData->callStackLine[currentCodenameOneCallStackOffset - 1]; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

#define DEFINE_METHOD_STACK_FAST_PRIMITIVE(stackSize, localsStackSize, spPosition) DEFINE_METHOD_STACK_FAST_PRIMITIVE_IMPL(, stackSize, localsStackSize, spPosition)
#define DEFINE_METHOD_STACK_FAST_PRIMITIVE_VSP(stackSize, localsStackSize, spPosition) DEFINE_METHOD_STACK_FAST_PRIMITIVE_IMPL(volatile, stackSize, localsStackSize, spPosition)

#define DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE_IMPL(spQualifier, stackSize, localsStackSize, spPosition) \
    const int cn1LocalsBeginInThread = threadStateData->threadObjectStackOffset; \
    struct elementStruct* locals CN1_UNUSED = &threadStateData->threadObjectStack[cn1LocalsBeginInThread]; \
    struct elementStruct* stack = &threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset + localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition) \
    cn1_init_method_stack_fast(threadStateData, __cn1ThisObject, stackSize, localsStackSize, JAVA_FALSE); \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset;\
    int* const cn1CurrentLine CN1_UNUSED = &threadStateData->callStackLine[currentCodenameOneCallStackOffset - 1]; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

#define DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE(stackSize, localsStackSize, spPosition) DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE_IMPL(, stackSize, localsStackSize, spPosition)
#define DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE_VSP(stackSize, localsStackSize, spPosition) DEFINE_INSTANCE_METHOD_STACK_FAST_PRIMITIVE_IMPL(volatile, stackSize, localsStackSize, spPosition)

#define CN1_FAST_RETURN_RELEASE() \
    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread; \
    threadStateData->callStackOffset--;

// === Frameless frame (primitive-only static methods) ========================
// A method whose frame holds ZERO object references contributes no GC roots, so
// the precise collector has nothing to scan there and the per-call frame can be
// eliminated. The operand stack + locals live in a method-LOCAL C-stack array --
// NOT a slice of the global threadObjectStack -- so there is no per-call memset,
// no threadObjectStack offset bump/restore, no callStack class/method push, and
// no callStackOffset bump. The method body (PUSH/POP/SP ops, arithmetic, calls)
// is emitted byte-for-byte unchanged; it just operates on this local SP. Frame
// elimination is GC-trivial here -- it changes nothing the collector sees.
#define DEFINE_METHOD_STACK_FRAMELESS_IMPL(spQualifier, stackSize, localsStackSize, spPosition) \
    struct elementStruct cn1_frameless_frame[(localsStackSize) + (stackSize)]; \
    struct elementStruct* locals CN1_UNUSED = &cn1_frameless_frame[0]; \
    struct elementStruct* stack = &cn1_frameless_frame[localsStackSize]; \
    CN1_DECLARE_SP(spQualifier, spPosition)

#define DEFINE_METHOD_STACK_FRAMELESS(stackSize, localsStackSize, spPosition) DEFINE_METHOD_STACK_FRAMELESS_IMPL(, stackSize, localsStackSize, spPosition)
#define DEFINE_METHOD_STACK_FRAMELESS_VSP(stackSize, localsStackSize, spPosition) DEFINE_METHOD_STACK_FRAMELESS_IMPL(volatile, stackSize, localsStackSize, spPosition)

// The two names a try/catch needs that a frameless frame does not otherwise define.
// Emitted ONLY into frameless methods that contain one, so the other 90% of frameless
// methods stay byte-for-byte what they were.
//
//   methodBlockOffset                  END_TRY and JUMP_TO restore tryBlockOffset to it
//   currentCodenameOneCallStackOffset  DEFINE_CATCH_BLOCK restores callStackOffset to it
//
// Both are the value AT ENTRY, and for a frameless frame that is also the value the
// method never changes -- it pushes no call-stack entry and opens no thread-stack slice.
// The catch block's restore is therefore a no-op for this frame and the right thing for
// any CALLEE frame a longjmp unwound through.
#define CN1_FRAMELESS_TRY_FRAME() \
    const int currentCodenameOneCallStackOffset CN1_UNUSED = threadStateData->callStackOffset; \
    int methodBlockOffset CN1_UNUSED = threadStateData->tryBlockOffset;

// A RETURN out of a try block skips the end label where END_TRY would have run, so the
// block stack has to be unwound here instead. This is the frameless half of what
// releaseForReturnInException does for an ordinary frame -- only the tryBlockOffset
// part, because a frameless frame has no thread-stack slice to release and no call-stack
// entry to pop.
#define CN1_FRAMELESS_TRY_RETURN() threadStateData->tryBlockOffset = methodBlockOffset;

// An explicit THROW from a frameless frame. Such a frame pushes no call-stack entry,
// so a trace filled in by its own throw did not name it at all -- the uncaught report
// read "java.lang.IllegalStateException" and nothing else, which is the frame a crash
// report exists to find. So the entry is pushed for the throw alone, with the line the
// translator knows statically, and taken off again if throwException returns (an app
// target with no handler). When it longjmps instead, the handler's frame restores
// callStackOffset to its own entry value, which removes this one with it.
#define CN1_FRAMELESS_THROW(ex, classNameId, methodNameId, line) do { \
    JAVA_OBJECT cn1__thrown = (ex); \
    int cn1__depth = threadStateData->callStackOffset; \
    if(cn1__depth < CN1_MAX_STACK_CALL_DEPTH) { \
        threadStateData->callStackClass[cn1__depth] = (classNameId); \
        threadStateData->callStackMethod[cn1__depth] = (methodNameId); \
        threadStateData->callStackLine[cn1__depth] = (line); \
        threadStateData->callStackOffset = cn1__depth + 1; \
    } \
    throwException(threadStateData, cn1__thrown); \
    threadStateData->callStackOffset = cn1__depth; \
} while(0)

// Headroom (bytes) kept below the end of the native C stack: enough to detect the
// overflow and still build + throw the StackOverflowError without overrunning.
#define CN1_FRAMELESS_STACK_GUARD_BAND (256 * 1024)

// Computes (lazily, once per thread) ThreadLocalData.nativeStackLimit. Defined in
// cn1_globals.m so the hot header stays free of pthread stack-introspection.
extern void cn1ComputeNativeStackLimit(CODENAME_ONE_THREAD_STATE);

// Stack-overflow guard emitted at the top of every frameless method. Frameless
// frames don't bump callStackOffset, so the 1024-depth call-limit can't protect
// them; deep non-tail recursion (e.g. fib) would otherwise blow the native C
// stack into a SIGSEGV. Compare the current frame address against the per-thread
// low-water mark and throw a catchable StackOverflowError before that happens.
// Cost on the hot path: one load + a predicted-not-taken branch. `retval` is the
// method's default return ('' for void, 0 for primitives); throwException normally
// longjmps, so the return is just the unreachable fall-through the compiler needs.
// The trip test is TWO-SIDED: only an address inside the guard band
// [limit - BAND, limit) throws. A one-sided (addr < limit) test misfires when
// this thread-state executes on a FOREIGN stack -- iOS natives dispatch_sync
// blocks onto the main queue and call Java helpers with the EDT's captured
// threadStateData, so the main thread's frame addresses were compared against
// the EDT's stack bounds and getBytes/toNSString spuriously threw
// StackOverflowError the first time RichTextArea set a browser page (observed
// as build-ios/metal/mac-native dying at exactly 78 screenshots). A foreign
// stack essentially never maps into the 256KB band of another stack, while a
// genuinely overflowing stack must descend THROUGH the band (no single
// frameless frame approaches 256KB), so overflow detection is preserved.
/* -DCN1_COUNT_SOE_ENTRIES makes every guarded entry tick a plain counter, so the
 * guard's whole-program ceiling can be computed from a real call count instead of
 * guessed at. Not atomic and not meant to be: the count only needs an order of
 * magnitude, and an atomic here would cost more than the thing being measured. */
#ifdef CN1_COUNT_SOE_ENTRIES
extern long long cn1SoeEntryCount;
#define CN1_SOE_TICK() (cn1SoeEntryCount++)
#else
#define CN1_SOE_TICK() ((void)0)
#endif

#define CN1_FRAMELESS_SOE_GUARD(retval) \
    do { \
        CN1_SOE_TICK(); \
        JAVA_LONG __cn1FrameAddr = (JAVA_LONG)(intptr_t)__builtin_frame_address(0); \
        if (__builtin_expect(__cn1FrameAddr < threadStateData->nativeStackLimit \
                && __cn1FrameAddr >= threadStateData->nativeStackLimit - (JAVA_LONG)CN1_FRAMELESS_STACK_GUARD_BAND, 0)) { \
            cn1ThrowStackOverflow(threadStateData); \
            return retval; \
        } \
    } while(0)


#if defined(__APPLE__) && defined(__OBJC__)
@class NSString;
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
#else
#define NSLog(...) printf(__VA_ARGS__); printf("\n")
typedef int BOOL;
#define YES 1
#define NO 0
#endif

extern JAVA_OBJECT __NEW_ARRAY_JAVA_BOOLEAN(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_CHAR(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_BYTE(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_SHORT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_INT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_LONG(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_FLOAT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
extern JAVA_OBJECT __NEW_ARRAY_JAVA_DOUBLE(CODENAME_ONE_THREAD_STATE, JAVA_INT size);

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* stringToUTF8, plus the byte count, for callers that must not stop at the first
   NUL. A Java string may legally contain U+0000 and getBytes("UTF-8") encodes it
   as a single zero byte, so the returned buffer is not always a C string in the
   sense a length of -1 assumes -- see the sqlite3_bind_text call in the backend,
   where taking the C length silently persisted a prefix. lengthOut may be NULL. */
extern const char* stringToUTF8Len(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str, JAVA_INT* lengthOut);

JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent);

/// Uniform size of a stack iterator buffer. Every iterator in the closed world is a parent
/// reference plus a handful of primitives; the largest measured is ~56 bytes including the
/// 16-byte header. The bound is checked at COMPILE time in the taker below, so a class that
/// outgrows it silently falls back to the heap instead of overflowing the buffer.
#define CN1_ITER_BUF_BYTES 96

/// Offer this frame's buffer to the next eligible iterator allocation.
static inline void cn1IterScopeBegin(struct ThreadLocalData* threadStateData, void* buf) {
    threadStateData->pendingStackIter = buf;
}

/// Withdraw the offer. Called when the loop ends, so a buffer nobody took cannot be taken
/// later by an unrelated allocation further down the method.
static inline void cn1IterScopeEnd(struct ThreadLocalData* threadStateData) {
    threadStateData->pendingStackIter = 0;
}

/// Take the pending buffer for an object of `sz` bytes, or answer NULL to allocate
/// normally. Emitted into __NEW_X only for classes the translator proved cannot escape
/// the loop -- see IteratorEscape.
static inline JAVA_OBJECT cn1IterScopeTake(struct ThreadLocalData* threadStateData,
        struct clazz* cls, int sz) {
    void* b = threadStateData->pendingStackIter;
    if(b == 0 || sz > CN1_ITER_BUF_BYTES) {
        return JAVA_NULL;
    }
    threadStateData->pendingStackIter = 0;   // one-shot
    JAVA_OBJECT o = (JAVA_OBJECT)b;
    memset(b, 0, (size_t)sz);
    // EXACTLY the header the @StackAllocate path writes: a class pointer so the GC can
    // walk its fields when it finds the pointer on the C stack, mark -1 so no sweep
    // treats it as aged, and heapPosition -1 because it was never registered in the heap
    // table and must never be freed. It dies when the frame unwinds.
    CN1_OBJ_SET_CLASS(o, cls);
    CN1_OBJ_SET_MARK(o, -1);
    CN1_OBJ_SET_HEAPPOS(o, -1);
    return o;
}
void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

extern int currentGcMarkValue;
extern void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);
// One context lookup per generated tracing callback. A zero epoch retains the
// full conservative, serial forced-rescan, and verifier paths.
extern int cn1GcFieldMarkEpoch(JAVA_BOOLEAN force);
static inline __attribute__((always_inline)) void cn1GcMarkField(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force, int epoch) {
    if (obj == JAVA_NULL || CN1_IS_TAGGED(obj)) return;
    // epoch is ENCODED (cn1GcFieldMarkEpoch): a raw word compare.
    if (epoch != 0 && __atomic_load_n(CN1_OBJ_MARK_PTR(obj), __ATOMIC_ACQUIRE) == epoch) return;
    gcMarkObject(threadStateData, obj, force);
}


// Native reference blocks -- ArrayList/HashMap backing stores held in C memory rather
// than in a Java array. See the block comment beside the implementations in cn1_globals.m;
// the short version is that the elements are live Java references, so the owner's
// generated __GC_MARK_ must call cn1GcMarkRefBlock and its __FINALIZER_ must call
// cn1RefBlockFree, and every mutation below already carries the SATB barrier that AASTORE
// and System.arraycopy carry.
static inline JAVA_INT cn1InlTableNext(JAVA_LONG metadata, JAVA_INT from, JAVA_INT capacity) {
    if(metadata == 0) return -1;
    JAVA_INT* slots = (JAVA_INT*)(uintptr_t)metadata;
    for(JAVA_INT i = from; i < capacity; i++) if(slots[i] < 0) return i;
    return -1;
}

// Untraced, exclusively owned primitive storage; growth preserves existing bytes.
extern JAVA_LONG cn1PrimitiveBlockResize(JAVA_LONG block, JAVA_INT bytes);
extern JAVA_LONG cn1IntBlockAlloc(JAVA_INT capacity);
extern void cn1IntBlockClear(JAVA_LONG block, JAVA_INT capacity);
static inline JAVA_INT cn1IntBlockGet(JAVA_LONG block, JAVA_INT index) {
    return ((JAVA_INT*)(uintptr_t)block)[index];
}
static inline void cn1IntBlockSet(JAVA_LONG block, JAVA_INT index, JAVA_INT value) {
    ((JAVA_INT*)(uintptr_t)block)[index] = value;
}
extern JAVA_LONG cn1RefBlockAlloc(JAVA_INT capacity);
extern JAVA_LONG cn1TableAlloc(JAVA_INT capacity, JAVA_BOOLEAN ordered);
extern void cn1InvokeFinalizer(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, void (*ptr)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT));
extern void cn1RefBlockFree(JAVA_LONG block);
extern void cn1StackBufferUnwind(struct ThreadLocalData* thread, struct CN1StackBuffer* until);
static inline void cn1StackBufferReset(struct CN1StackBuffer* scope) {
    if(scope->owned != 0) { cn1RefBlockFree(scope->owned); scope->owned = 0; }
}
static inline void cn1StackBufferLeave(struct CN1StackBuffer* scope) {
    cn1StackBufferReset(scope);
    scope->thread->nativeBuffers = scope->previous;
}
extern void cn1RefBlockRetire(JAVA_LONG block);
extern void cn1RefBlockDrainRetired(void);
extern void cn1RefBlockBeginCycle(void);
extern void cn1RefBlockEndCycle(void);
extern void cn1NativeBlockAccounting(size_t* live, size_t* retired, size_t* released);
extern void cn1RefBlockSet(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT index, JAVA_OBJECT value);
extern void cn1TableRefSet(CODENAME_ONE_THREAD_STATE, JAVA_LONG table, JAVA_INT part, JAVA_INT index, JAVA_OBJECT value);
extern void cn1RefBlockMove(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT from, JAVA_INT to, JAVA_INT count);
extern void cn1RefBlockClear(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT from, JAVA_INT count);
extern void cn1GcMarkRefBlock(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_BOOLEAN force);
extern void cn1GcMarkTablePart(CODENAME_ONE_THREAD_STATE, JAVA_LONG table, JAVA_INT part, JAVA_BOOLEAN force);
// Deferred block re-scan, mover side (cn1_globals.m, DEFERRED BLOCK RE-SCAN). Only inside
// a cn1SatbBulkBegin() bracket that answered TRUE; begin answers whether the collector
// will re-scan the block after this write (1) or the caller must log every overwritten
// slot itself (0). Pair every begin with an end after the last write.
extern int cn1BlockMoveBegin(JAVA_LONG block);
extern void cn1BlockMoveEnd(JAVA_LONG block);
// All standalone buffers and table slices begin on a 16-byte boundary.
// The immutable capacity belongs to the published pointer, not a second field.
// 16 BYTES, not 32. A collection's storage is a block, and at the self-hosting peak
// 606k blocks are live, 89% of them 256 bytes or less; a 32-byte header was 19MB of the
// 127MB they held. The size fits 32 bits (a block's element count is an int and nothing
// here is that big), the allocation is the header less a small alignment offset (0
// wherever malloc aligns to 16), and the retirement link lives outside the block (see
// cn1RefBlockRetire). What the rest of the runtime reads stays where it was: the
// capacity 8 bytes before the data and the re-scan word 4 bytes before it.
typedef struct __attribute__((aligned(16))) CN1NativeBlock {
    uint32_t bytes;          // size of the whole allocation, header included
    uint8_t allocOffset;     // header - allocation
    uint8_t reserved[3];
    JAVA_INT capacity;
} CN1NativeBlock;

// Part `part` of the hash table whose root is `table` (0 for no table). A table is keys,
// values, then int metadata -- and, ordered, the prev and next links -- packed back to
// back with no header between them, so a part is arithmetic on the root's capacity and
// the owners keep no field for it. Everything that walks or writes a part derives it from
// the root it read ONCE; see cn1TableAlloc.
// A HashSet's table: `capacity` element references, then `capacity` int markers, in
// one allocation. The header's capacity is the element count, so the ordinary
// reference-block walk traces exactly the elements; the markers are found from it.
extern JAVA_LONG cn1SetTableAlloc(JAVA_INT capacity);
static inline JAVA_LONG cn1SetTableMeta(JAVA_LONG table) {
    if(table == 0) return 0;
    return table + (JAVA_LONG)((size_t)((const CN1NativeBlock*)(uintptr_t)table - 1)->capacity * sizeof(JAVA_OBJECT));
}
static inline JAVA_LONG cn1TablePart(JAVA_LONG table, JAVA_INT part) {
    if(table == 0) return 0;
    size_t cap = (size_t)((const CN1NativeBlock*)(uintptr_t)table - 1)->capacity;
    size_t offset = part < 2 ? (size_t)part * cap * sizeof(JAVA_OBJECT)
        : 2 * cap * sizeof(JAVA_OBJECT) + (size_t)(part - 2) * cap * sizeof(JAVA_INT);
    return table + (JAVA_LONG)offset;
}
// Fixed-width copies compile to unaligned word loads without aliasing UB. Both
// end loads stay inside the logical byte range; no padding or terminator is read.
static inline __attribute__((always_inline)) int cn1CompactBytesEqual(const void* left, const void* right, size_t count) {
    const uint8_t* a = (const uint8_t*)left;
    const uint8_t* b = (const uint8_t*)right;
    if (count > 32) return memcmp(a, b, count) == 0;
    if (count >= 16) {
        uint64_t a0, a1, a2, a3, b0, b1, b2, b3;
        memcpy(&a0, a, 8); memcpy(&b0, b, 8);
        memcpy(&a1, a + 8, 8); memcpy(&b1, b + 8, 8);
        memcpy(&a2, a + count - 16, 8); memcpy(&b2, b + count - 16, 8);
        memcpy(&a3, a + count - 8, 8); memcpy(&b3, b + count - 8, 8);
        return ((a0 ^ b0) | (a1 ^ b1) | (a2 ^ b2) | (a3 ^ b3)) == 0;
    }
    if (count >= 8) {
        uint64_t a0, a1, b0, b1;
        memcpy(&a0, a, 8); memcpy(&b0, b, 8);
        memcpy(&a1, a + count - 8, 8); memcpy(&b1, b + count - 8, 8);
        return ((a0 ^ b0) | (a1 ^ b1)) == 0;
    }
    if (count >= 4) {
        uint32_t a0, a1, b0, b1;
        memcpy(&a0, a, 4); memcpy(&b0, b, 4);
        memcpy(&a1, a + count - 4, 4); memcpy(&b1, b + count - 4, 4);
        return ((a0 ^ b0) | (a1 ^ b1)) == 0;
    }
    if (count >= 2) {
        uint16_t a0, a1, b0, b1;
        memcpy(&a0, a, 2); memcpy(&b0, b, 2);
        memcpy(&a1, a + count - 2, 2); memcpy(&b1, b + count - 2, 2);
        return ((a0 ^ b0) | (a1 ^ b1)) == 0;
    }
    return count == 0 || *a == *b;
}

// Read-only mixed-coder comparison. Keeping this independent of Java entry
// points exposes its lack of allocation, safepoints and writes to the optimizer.
static inline __attribute__((pure)) int cn1CompactMixedEquals(
        const uint8_t* latin, const uint16_t* wide, size_t count) {
    for (size_t i = 0; i < count; i++) {
        if ((uint16_t)latin[i] != wide[i]) return 0;
    }
    return 1;
}

static inline JAVA_INT cn1RefBlockCount(JAVA_LONG block) {
    return block == 0 ? 0 : *(const JAVA_INT*)((const char*)(uintptr_t)block
            - (sizeof(CN1NativeBlock) - offsetof(CN1NativeBlock, capacity)));
}

// READ is inline and unchecked on purpose: it is the operation the for-each lowering
// emits per element, and a C block has no header and no payload offset to add, so
// this is strictly cheaper than the array access it replaces.
static inline JAVA_OBJECT cn1RefBlockGet(JAVA_LONG block, JAVA_INT index) {
    return ((JAVA_OBJECT*)(uintptr_t)block)[index];
}


// Drop every soft referent at the next collection, whatever the retention policy would
// otherwise have decided. Called when an allocation has actually failed: SoftReference's
// one hard guarantee is that all of them are cleared before the VM gives up, and the
// retention ladder cannot see that coming on a platform with no per-process budget probe.
extern void cn1RefDropAllSoftReferents(void);
extern void cn1GcDiscoverReference(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT ref, JAVA_BOOLEAN force,
                                   JAVA_OBJECT* referentField, JAVA_INT* touchAgeField,
                                   JAVA_INT* agedCycleField, JAVA_INT strength);


// ---- the Reference.get() load barrier --------------------------------------
// Emitted into get_field_java_lang_ref_Reference_objReference, and the reason the
// clear pass is allowed to run with mutators still going.
//
// A thread whose stack was scanned and released early can pull the referent out of a
// reference and hold it in a local the collector has already walked past. That referent
// is then neither marked nor fresh, which is the one case the sweep's "already marked or
// FRESH" invariant does not cover, so without this it is freed under a live pointer.
// Enqueuing puts it in the snapshot: the trial clear of gcSatbActive finds a non-empty
// log, re-arms, marks it, and the reference is left alone.
//
// THE FILTER IS NOT AN OPTIMIZATION, it is what makes this affordable. cn1SatbEnqueue
// takes a mutex per accepted reference, and get() on a hot cache is called far more often
// than anything the per-store barrier sees -- measured on RefPolicy before this filter
// existed, a 400,000-access run put over 10,000 entries into the log per cycle and drove
// the SATB termination loop into its CN1_SATB_MAX_REOPENS cap on every single cycle,
// which is the collector failing to converge rather than a cost.
//
// It skips exactly the referents the clear pass would refuse to clear anyway: already
// marked this epoch, or fresh and therefore kept by the sweep's grace rule. Deliberately
// STRICTER than the clear pass's own test, which also spares mark == epoch - 1 (last
// cycle's slack): bibopGcEpoch is only exactly equal to currentGcMarkValue once
// cn1BibopBeginGcCycle has published it, and a barrier must not depend on a mirror being
// current. Skipping less is always safe; skipping more is not.
//
// A retained soft reference costs nothing here at all, because its referent is marked as
// an ordinary strong edge by cn1GcDiscoverReference before any get() can reach it.
// REGISTERS AROUND THE LOAD, via the same handshake the bulk copies use, and the
// registration is what the caller must hold ACROSS its load -- hence the awkward shape:
// the accessor calls cn1RefLoadBegin(), loads, calls this, then cn1RefLoadEnd().
//
// Checking gcSatbActive and then enqueuing is not enough on this path, and the reason it
// is enough for the per-store barrier does not carry over. cn1SatbEnqueue takes a mutex,
// so a thread can pass a flag check and then be delayed long enough for the collector to
// clear the field, finish its empty final take, lower gcSatbTerminating and quiesce -- and
// the entry then lands in a log nothing will ever drain, or is skipped entirely, while the
// sweep frees the referent the caller is about to return. The per-store barrier tolerates
// that window because a reference STORED after the fixpoint is already marked or FRESH and
// the sweep keeps both; a weak REFERENT handed out by get() is neither.
//
// AN OUTER "FAST PATH" FLAG CHECK BREAKS THIS, and did: gating entry to
// cn1SatbBulkBegin() on a prior read of the same flags reintroduces the race one level
// out, because the thread can be descheduled between that read and the registration. The
// whole value of cn1SatbBulkBegin is that it registers FIRST and reports afterwards, so
// nothing may be sampled before it.
//
// With the registration held across the load, a false answer is safe rather than merely
// unlikely: the collector cannot be mid-termination (its quiesce waits for this
// registration), so either no mark is running -- and one starting later scans this thread
// with the value already in a register -- or reference processing is complete, in which
// case a field still holding a pointer was not condemned and its referent is marked.
// The deletion barrier for the referent field, with an ATOMIC load.
//
// CN1_SATB_DELETE next door reads through a plain JAVA_OBJECT volatile*, which is right
// for every ordinary field because nothing else writes them concurrently. The referent is
// the exception: cn1GcProcessReferences stores JAVA_NULL into it atomically from the
// collector while Reference.clear() runs here, so the plain read would leave that pair a
// mixed atomic/non-atomic access -- undefined in C, and the same defect that was fixed for
// the getter and for this setter's own store. Making the store atomic and leaving the
// barrier's read plain fixes half a race.
#if defined(CN1_DISABLE_SATB)
#define CN1_SATB_DELETE_REF(fieldAddr) do { } while(0)
#else
#define CN1_SATB_DELETE_REF(fieldAddr) \
    do { if(__builtin_expect(gcSatbActive, 0)) { \
             JAVA_OBJECT cn1__old = __atomic_load_n((JAVA_OBJECT*)(fieldAddr), __ATOMIC_RELAXED); \
             if(cn1__old != JAVA_NULL && !CN1_IS_TAGGED(cn1__old)) cn1SatbEnqueue(cn1__old); \
         } } while(0)
#endif

#if defined(CN1_DISABLE_SATB)
#define CN1_REF_LOAD_BEGIN() JAVA_FALSE
#define CN1_REF_LOAD_END()   do { } while(0)
#define CN1_SATB_REF_KEEP(active, refVal) do { (void)(active); (void)(refVal); } while(0)
#else
// -DCN1_REF_NO_LOAD_BARRIER compiles the registration and the enqueue out, leaving the
// touch stamp and the load. It is UNSOUND -- it is the arm that measures what the barrier
// costs, not a configuration to ship -- and exists because "is get() too expensive?" has to
// be answered with a number rather than an intuition.
#if defined(CN1_REF_NO_LOAD_BARRIER)
#define CN1_REF_LOAD_BEGIN() JAVA_FALSE
#define CN1_REF_LOAD_END()   do { } while(0)
#else
#define CN1_REF_LOAD_BEGIN() cn1SatbBulkBegin()
#define CN1_REF_LOAD_END()   cn1SatbBulkEnd()
#endif
#ifdef CN1_GC_CONFORM
extern _Atomic long cn1RefGets;
#define CN1_REF_COUNT_GET() atomic_fetch_add_explicit(&cn1RefGets, 1, memory_order_relaxed)
#else
#define CN1_REF_COUNT_GET() do { } while(0)
#endif
#define CN1_SATB_REF_KEEP(active, refVal) \
    do { CN1_REF_COUNT_GET(); JAVA_OBJECT cn1__r = (refVal); \
         if((active) && cn1__r != JAVA_NULL && !CN1_IS_TAGGED(cn1__r)) { \
             int cn1__m = CN1_OBJ_MARK_LOAD(cn1__r, __ATOMIC_RELAXED); \
             int cn1__e = atomic_load_explicit(&bibopGcEpoch, memory_order_relaxed); \
             if(cn1__m != -1 && cn1__m != cn1__e) cn1SatbEnqueue(cn1__r); \
         } } while(0)
#endif

extern void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);
extern JAVA_BOOLEAN removeObjectFromHeapCollection(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);

extern void codenameOneGCMark();
extern void codenameOneGCSweep();

/* Thread-state and virtual-thread construction. Declared OUTSIDE the
   conservative-roots block: neither depends on how the collector finds its roots,
   and burying them there broke -DCN1_DISABLE_CONSERVATIVE_GC_ROOTS -- the precise
   threadObjectStack arm vm/CLAUDE.md documents -- with an undeclared
   cn1SpawnVirtualThread in any native source that spawns one. */
struct cn1VirtualThread;
/**
 * A VM thread state. bindToCallingOsThread false builds one for a VIRTUAL thread,
 * which owns it rather than borrowing the host's -- see the definition.
 */
extern struct ThreadLocalData* cn1CreateThreadLocalData(JAVA_BOOLEAN bindToCallingOsThread);
/**
 * EXPERIMENTAL and unfinished -- see the block above the definition in
 * nativeMethods.m for what is open. Nothing in this repository calls either of
 * these; they ship so the server work can build against them. The coroutine runtime
 * underneath (cn1_virtual_thread.h) is finished and is not experimental.
 */
/** A virtual thread with a Java stack of its own, ready to be resumed. */
extern struct cn1VirtualThread* cn1SpawnVirtualThread(void (*body)(void*), void* arg,
                                                      size_t stackBytes);
/**
 * The other half of cn1SpawnVirtualThread. Releases the coroutine AND the VM thread
 * state spawned with it -- including its allThreads slot, without which a virtual
 * thread per request exhausts NUMBER_OF_SUPPORTED_THREADS. cn1VirtualThreadFree
 * alone releases only the coroutine. Never call it from inside the virtual thread's
 * own body; it frees the stack that body is running on.
 */
extern void cn1RetireVirtualThread(struct cn1VirtualThread* vt);

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// PHASE 3b production conservative-root API. cn1ConservativeResolve maps an
// arbitrary machine word to the base of the live heap object it points into
// (interior pointers included) or JAVA_NULL, dereferencing nothing unproven.
// cn1ConservativeMarkRange reads every aligned word in [lo,hi) and gcMarkObject's
// what it resolves to -- a REAL root source (marks a superset of the precise set,
// so nothing live is ever freed). cn1GcBuildRootSnapshots rebuilds the resolver's
// page/extent index once per GC. cn1GcInstallSignalHandler installs the SIGUSR-based
// universal thread-stop handler (idempotent). See the big block in cn1_globals.m.
extern JAVA_OBJECT cn1ConservativeResolve(void* w);
extern void cn1ConservativeMarkRange(CODENAME_ONE_THREAD_STATE, char* lo, char* hi);
extern void cn1GcBuildRootSnapshots(void);
extern void cn1GcInstallSignalHandler(void);
// Per-thread self pointer, set at thread registration; read async-signal-safely by the
// universal-stop handler.
extern __thread struct ThreadLocalData* cn1TlsSelf;

// Capture a parking mutator's native register file + native-stack low bound so the
// concurrent GC can conservatively scan [sp, stackBase) for native-stack-held roots.
// MUST be a macro so setjmp + the SP marker live in the PARKING frame itself: that
// frame -- and the entire live mutator call chain above it (including any frameless
// object frame whose roots are native-C locals) -- stays resident while the thread
// spins in the GC-wait loop and the GC walks it. gcParkCaptured is published LAST,
// and the GC only scans a thread after observing threadActive==FALSE (set right after
// this macro), so it always reads a complete capture.
#define CN1_GC_PARK_CAPTURE(ts) do { \
        (void)CN1_TRY_SETJMP((ts)->gcRegisterSnapshot); \
        volatile void* cn1__sp = (void*)&cn1__sp; \
        (ts)->gcStackPointerAtPark = (void*)cn1__sp; \
        __atomic_thread_fence(__ATOMIC_RELEASE); \
        (ts)->gcParkCaptured = JAVA_TRUE; \
    } while(0)
#else
#define CN1_GC_PARK_CAPTURE(ts) do {} while(0)
#endif

// Bracket one mutator park so its duration is charged to a cause. Both halves compile to
// nothing without -DCN1_GC_CONFORM -- including the timestamp variable, which is why the
// name is a macro argument rather than a fixed identifier: several park sites sit in one
// scope in codenameOneGcMalloc and a fixed name would not survive there.
#ifdef CN1_GC_CONFORM
extern long long cn1StallNowNs(void);
extern void cn1StallRecord(int cause, long long ns, struct ThreadLocalData* ts);
/* Stall causes. Declared HERE rather than in cn1_globals.m because
   CN1_RESUME_THREAD below expands to CN1_STALL_ADD(..., CN1_STALL_NATIVE_RESUME,
   ...), and every native file that wraps a blocking call uses that macro. With
   the codes private to cn1_globals.m, any other native source failed to compile
   under -DCN1_GC_CONFORM with "use of undeclared identifier", which is every port
   whose sockets, database or crypto natives wrap a blocking call this way. */
#define CN1_STALL_PACING_VOLUME 0   // regime-A run-ahead cap (cn1PacingPark, no budget)
#define CN1_STALL_PACING_BUDGET 1   // regime-B admission wait (cn1PacingPark, under a ceiling)
#define CN1_STALL_LOWMEM        2   // the low-memory allocation throttle
#define CN1_STALL_HANDSHAKE     3   // threadBlockedByGC: this thread's own share of the mark
#define CN1_STALL_PENDING_FULL  4   // per-thread pending table full: waits out a WHOLE cycle
#define CN1_STALL_NATIVE_RESUME 5   // returning from a native call into a running mark
#define CN1_STALL_SIGNAL_STOP   6   // parked inside the GC's stop signal handler
#define CN1_STALL_CAUSES        7

#define CN1_STALL_T0(v) long long v = cn1StallNowNs()
#define CN1_STALL_ADD(v, cause, ts) cn1StallRecord((cause), cn1StallNowNs() - (v), (ts))
#else
#define CN1_STALL_T0(v) ((void)0)
#define CN1_STALL_ADD(v, cause, ts) ((void)0)
#endif

typedef JAVA_OBJECT (*newInstanceFunctionPointer)(CODENAME_ONE_THREAD_STATE);
typedef JAVA_OBJECT (*enumValueOfFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);

extern void** initVtableForInterface();

extern JAVA_OBJECT cloneArray(JAVA_OBJECT array);
extern int byteSizeForArray(struct clazz* cls);
extern void markStatics(CODENAME_ONE_THREAD_STATE);

/*#define safeRelease(threadStateData, es) { \
    if(es != 0 && (es)->type == CN1_TYPE_OBJECT) { releaseObj(threadStateData, (es)->data.o); } \
}

static inline struct elementStruct* pop(struct elementStruct* array, int* sp) {
    --(*sp);
    struct elementStruct* retVal = &array[*sp];
    return retVal;
}

static inline struct elementStruct* popAndRelease(CODENAME_ONE_THREAD_STATE, struct elementStruct* array, int* sp) {
    --(*sp);
    struct elementStruct* retVal = &array[*sp];
    releaseObj(threadStateData, retVal->data.o);
    retVal->type = CN1_TYPE_INVALID;
    return retVal;
}

#define popMany(threadStateData, count, array, sp) { \
    int countVal = count; \
    while(countVal > 0) { \
        --sp; \
        struct elementStruct* ddd = &array[sp]; \
        if(ddd != 0 && (ddd)->type == CN1_TYPE_OBJECT) { releaseObj(threadStateData, (ddd)->data.o); } \
        countVal--; \
    } \
}
*/

// Inlined: POP_INT/POP_LONG/POP_OBJ hit this on every pop, including hot return paths
// (return POP_LONG()). It was a non-inline call -- pure overhead for a pointer decrement.
// Returns the new SP rather than writing through a pointer to it, so the caller's
// SP keeps whatever qualifiers its frame gave it. The pointer-taking form
// discarded volatile on every frame that declares SP volatile; see POP_MANY.
extern struct elementStruct* cn1PopMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct* sp);


#define swapStack(sp) { \
    struct elementStruct t = sp[-1]; \
    sp[-1] = sp[-2]; \
    sp[-2] = t; \
}

extern struct clazz class__java_lang_Class;
extern struct clazz ClazzClazz;

#endif //__CN1GLOBALS__
