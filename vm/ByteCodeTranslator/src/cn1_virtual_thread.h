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

/*
 * Stackful virtual threads: a Java thread of control that is not an OS thread.
 *
 * WHY THIS EXISTS AT ALL, in one number: a mutex-and-condvar handoff between two
 * OS threads costs 21181ns on this hardware; switching a virtual thread costs 2.6ns.
 * Every design that moves a request between OS threads pays the first; this pays
 * the second.
 *
 * WHY IT IS ASSEMBLY rather than setjmp/longjmp, which also measured 4-8ns:
 * glibc's __longjmp_chk aborts a jump whose target stack is not the current one
 * -- "longjmp causes uninitialized stack frame" -- and it is compiled in by
 * -D_FORTIFY_SOURCE, which distributions enable by default. A setjmp switch is
 * therefore green everywhere we test and dead in somebody else's hardened build.
 * musl compounds it from the other side by not implementing makecontext at all,
 * so there is no portable way to CREATE the stack either. Twenty instructions of
 * our own depend on neither.
 *
 * WHAT A VIRTUAL THREAD HOLDS: only the machine stack. Java locals and the operand
 * stack live in threadStateData->threadObjectStack, which is a heap array the
 * collector already walks precisely, so a suspended virtual thread's C stack carries
 * just the C activation records -- a few pointers and temporaries per Java frame.
 * That is why these stacks can be small where a platform thread's cannot.
 */
#ifndef CN1_VIRTUAL_THREAD_H
#define CN1_VIRTUAL_THREAD_H

/*
 * Virtual threads are on wherever they CAN be, which is anywhere the hand-written
 * context switch has an implementation. That is deliberately a capability test and
 * not a build flag: there is no separate "server build" of the VM, so a flag would
 * only mean the feature is off in every build nobody remembered to set it in.
 *
 * The switch has to be assembly -- glibc aborts a cross-stack longjmp under
 * _FORTIFY_SOURCE and musl has no makecontext -- and it is written for aarch64 and
 * x86_64. Anywhere else, and on Windows (whose calling convention needs its own
 * prologue and whose stack has a guard page the switch would have to poke), the
 * declarations below collapse to the no-ops at the bottom of this header. Those
 * report "there is no virtual thread here", which is true, so the shared collector
 * in cn1_globals.m needs no #ifdefs of its own and every call folds away.
 *
 * CN1_DISABLE_VIRTUAL_THREADS forces the no-op path on a target that would
 * otherwise qualify.
 */
#if !defined(CN1_VIRTUAL_THREADS) && !defined(CN1_DISABLE_VIRTUAL_THREADS) \
        && !defined(_WIN32) && (defined(__aarch64__) || defined(__x86_64__))
#define CN1_VIRTUAL_THREADS 1
#endif

#ifdef CN1_VIRTUAL_THREADS

#include <stddef.h>

struct cn1VirtualThread;

/*
 * Tells the VM that the state attached to a virtual thread has started or stopped
 * running Java, so the collector stops or resumes treating it as parked.
 *
 * It is a WEAK symbol with a no-op default rather than a function pointer for two
 * reasons: an indirect call on a path whose whole point is that it costs 2.1ns is
 * not free, and this file has to keep linking on its own -- the standalone runtime
 * test builds it without any VM at all. nativeMethods.m provides the real one.
 *
 * Kept out of the header's no-op section deliberately: it is about the VM's view of
 * a virtual thread, not about the switch, so it exists on every target.
 */
void cn1VirtualThreadVmStateActive(void* vmState, int active);

/** The body of a virtual thread. Returning from it finishes the virtual thread. */
typedef void (*cn1VirtualThreadBody)(void* arg);

/**
 * Allocate a virtual thread with its own stack. It does not run until the first
 * cn1VirtualThreadResume. Returns 0 if the stack could not be allocated.
 */
struct cn1VirtualThread* cn1VirtualThreadCreate(cn1VirtualThreadBody body, void* arg,
                                        size_t stackBytes);

/**
 * Run `co` until it yields or finishes, then come back here. Must be called from
 * the thread that will own it for the duration -- see cn1VirtualThreadStackBounds.
 */
void cn1VirtualThreadResume(struct cn1VirtualThread* co);

/** Suspend the running virtual thread and return to whoever resumed it. */
void cn1VirtualThreadYield(void);

/**
 * Why a virtual thread gave up its host, which the scheduler has to know.
 *
 *   CN1_VT_YIELD_IO        waiting for its descriptor; put it back on the poller
 *                          and resume it when the descriptor is ready.
 *   CN1_VT_YIELD_RUNNABLE  gave up its turn but is ready to run RIGHT NOW -- it
 *                          is waiting on something that is not its socket.
 *
 * Confusing the two deadlocks the server, and not theoretically: a virtual
 * thread parked in the collector's allocation backpressure is not waiting for
 * bytes, so putting it on the poller waits for a client that is itself waiting
 * for the response this virtual thread owes it.
 */
#define CN1_VT_YIELD_IO       0
#define CN1_VT_YIELD_RUNNABLE 1

void cn1VirtualThreadSetYieldReason(int reason);
int  cn1VirtualThreadYieldReason(struct cn1VirtualThread* vt);

/**
 * Yield the CURRENT virtual thread as runnable, if there is one.
 *
 * Returns 0 when not on a virtual thread, so a caller can fall back to whatever
 * it did before -- which is what every existing blocking spin in the VM needs to
 * keep doing on a platform thread.
 */
int cn1VirtualThreadYieldIfVirtual(void);

/** The virtual thread running on this thread, or 0 when on the thread's own stack. */
struct cn1VirtualThread* cn1VirtualThreadCurrent(void);

/** True once the body has returned. */
int cn1VirtualThreadFinished(struct cn1VirtualThread* co);

/** Free it. Undefined before it has finished. */
void cn1VirtualThreadFree(struct cn1VirtualThread* co);

/**
 * The OS thread's own stack pointer at the point it resumed `vt`.
 *
 * The collector needs both halves: a thread running a virtual thread has its
 * live frames split, the ones below the resume on the OS stack and the ones
 * above it on the virtual thread's stack.
 */
void* cn1VirtualThreadResumerSp(struct cn1VirtualThread* vt);

/**
 * Walk every virtual thread that exists, for the collector.
 *
 * A parked virtual thread is a GC root source and nothing else refers to its
 * stack, so it must be enumerable independently of whoever created it. The walk
 * holds the registry lock, so `fn` must not create or free virtual threads.
 */
void cn1VirtualThreadForEach(void (*fn)(struct cn1VirtualThread* vt, void* ctx),
                             void* ctx);

/** True while this virtual thread is the one executing on some OS thread. */
int cn1VirtualThreadIsRunning(struct cn1VirtualThread* vt);

/** The argument the body was created with, so a caller can free it after. */
void* cn1VirtualThreadArg(struct cn1VirtualThread* vt);

/** The high end of a virtual thread's stack. */
void* cn1VirtualThreadStackHigh(struct cn1VirtualThread* vt);

/**
 * Copy the registry into `out` (at most `max`), returning how many exist.
 *
 * The collector must take a SNAPSHOT before it stops the world and walk that,
 * never the live registry. cn1VirtualThreadForEach holds a mutex, and a thread
 * frozen by the stop signal may be the one holding it -- the GC would then wait
 * for a thread that cannot run. The rest of this collector already follows the
 * same rule for its root snapshots, for the same reason.
 *
 * A return larger than `max` means the snapshot was truncated and the caller
 * must retry with a bigger buffer rather than scan a subset, because an
 * unscanned virtual thread is an unscanned root source.
 */
int cn1VirtualThreadSnapshot(struct cn1VirtualThread** out, int max);

/**
 * Bracket the collector's stop-and-scan loop.
 *
 * Between these two calls, cn1VirtualThreadFree unlinks a virtual thread but does
 * not release it; End releases everything that piled up. Without them the scan
 * reads through snapshot pointers that a still-running host thread has already
 * freed and unmapped, which is a segfault whose likelihood rises with the number
 * of Java threads -- the loop stops threads one at a time, so more threads simply
 * means more time spent holding a snapshot while other threads run.
 *
 * Call around the WHOLE loop. Per-iteration brackets would still leave one
 * iteration's snapshot exposed during the next.
 */
void cn1VirtualThreadGcScanBegin(void);
void cn1VirtualThreadGcScanEnd(void);

/**
 * The virtual thread whose stack contains `addr`, or 0.
 *
 * Lets the collector recognise that a stopped OS thread's stack pointer is not
 * in the OS thread's stack at all, because it is currently running a virtual
 * thread. Without this the existing bounds check simply fails and the thread is
 * skipped in silence, which loses every root it holds.
 */
struct cn1VirtualThread* cn1VirtualThreadForStackAddress(void* addr, int count,
                                                         struct cn1VirtualThread** snapshot);

/**
 * The VM thread state this virtual thread runs with.
 *
 * A virtual thread needs its own Java locals and operand stack -- that is the
 * whole point, since those are what a request's state lives in -- so it carries
 * its own ThreadLocalData rather than borrowing the host thread's. Opaque here
 * to keep this file independent of cn1_globals.h.
 */
void* cn1VirtualThreadState(struct cn1VirtualThread* vt);
void  cn1VirtualThreadSetState(struct cn1VirtualThread* vt, void* state);

/**
 * The live part of a suspended virtual thread's stack, for the collector.
 *
 * A conservative scan has to cover every suspended virtual thread as well as the
 * running threads: a Java reference held only in a C temporary of a parked
 * request is reachable from nowhere else. Returns the region between the saved
 * stack pointer and the stack's high end, which is exactly the part in use.
 */
void cn1VirtualThreadStackBounds(struct cn1VirtualThread* co, void** low, void** high);

#else /* !CN1_VIRTUAL_THREADS */

/*
 * Off-target stubs. Every one answers "there is no virtual thread here", which
 * is the truth on a device, and the collector's virtual-thread paths then fold
 * away at compile time.
 */
struct cn1VirtualThread;

static inline int   cn1VirtualThreadYieldIfVirtual(void) { return 0; }
static inline void  cn1VirtualThreadGcScanBegin(void) { }
static inline void  cn1VirtualThreadGcScanEnd(void) { }
static inline struct cn1VirtualThread* cn1VirtualThreadCurrent(void) { return 0; }
static inline int   cn1VirtualThreadSnapshot(struct cn1VirtualThread** out, int max) {
    (void)out; (void)max; return 0;
}
static inline struct cn1VirtualThread* cn1VirtualThreadForStackAddress(
        void* addr, int count, struct cn1VirtualThread** snapshot) {
    (void)addr; (void)count; (void)snapshot; return 0;
}
static inline int   cn1VirtualThreadIsRunning(struct cn1VirtualThread* co) { (void)co; return 0; }
static inline void  cn1VirtualThreadStackBounds(struct cn1VirtualThread* co, void** lo, void** hi) {
    (void)co; if(lo) { *lo = 0; } if(hi) { *hi = 0; }
}
static inline void* cn1VirtualThreadStackHigh(struct cn1VirtualThread* co) { (void)co; return 0; }
static inline void* cn1VirtualThreadResumerSp(struct cn1VirtualThread* co) { (void)co; return 0; }
static inline void* cn1VirtualThreadState(struct cn1VirtualThread* co) { (void)co; return 0; }
static inline void  cn1VirtualThreadSetState(struct cn1VirtualThread* co, void* st) { (void)co; (void)st; }
static inline void  cn1VirtualThreadFree(struct cn1VirtualThread* co) { (void)co; }

#endif /* CN1_VIRTUAL_THREADS */

#endif
