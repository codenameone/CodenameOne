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

/* See cn1_virtual_thread.h for the capability gate. On a target the switch is not
 * written for, this file is empty and the header supplies no-op stubs, so nothing
 * references the assembly. */
#include "cn1_virtual_thread.h"
#ifdef CN1_VIRTUAL_THREADS

#include "cn1_virtual_thread.h"
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>
#include <pthread.h>

/*
 * The switch saves the callee-saved registers on the outgoing stack, swaps the
 * stack pointer, and restores the incoming ones. Caller-saved registers need no
 * handling: the compiler already assumes a call clobbers them, and this IS a
 * call. That is the whole reason twenty instructions is enough.
 */
struct cn1VirtualThread {
    void*  sp;          /* saved stack pointer while suspended */
    struct cn1VirtualThread* registryNext;  /* every live virtual thread, for the GC */
    struct cn1VirtualThread* registryPrev;
    void*  vmState;     /* this virtual thread's ThreadLocalData */
    int    yieldReason; /* CN1_VT_YIELD_* -- why it last gave up its host */
    int    running;     /* executing on some OS thread right now */
    void*  stackLow;    /* mmap base */
    void*  stackHigh;   /* one past the usable end */
    size_t stackBytes;
    cn1VirtualThreadBody body;
    void*  arg;
    void*  returnSp;    /* the resumer's saved stack pointer */
    int    finished;
    int    started;
};

static __thread struct cn1VirtualThread* cn1CurrentVirtualThread = 0;

/*
 * Every live virtual thread, so the collector can find the parked ones.
 *
 * A parked virtual thread's stack is referenced by nothing else -- not by the
 * thread that created it, which has moved on, and not by the scheduler, which
 * only knows the ones it has queued. If it is not enumerable here then a Java
 * reference held in a C temporary of a parked request is invisible to the scan,
 * and the object under it is freed while the request still means to use it.
 */
static struct cn1VirtualThread* cn1VirtualThreadRegistry = 0;
static pthread_mutex_t cn1VirtualThreadRegistryLock = PTHREAD_MUTEX_INITIALIZER;

/*
 * Releasing a virtual thread while the collector is scanning is a use-after-free,
 * so it is deferred instead.
 *
 * The collector does not stop the world and then scan: it stops and scans ONE
 * thread at a time, rebuilding its virtual-thread snapshot inside that loop, and
 * every OTHER thread keeps running throughout -- including host threads, whose
 * whole job is finishing connections and freeing the virtual threads that served
 * them. So a pointer copied into the snapshot can be freed, and its stack
 * unmapped, before the scan that snapshot feeds ever reads it. The wider the
 * loop, the wider the window: with 64 idle Java threads padding it out this
 * segfaulted 2 runs in 6, and with 4 it never did -- the idle threads take no
 * part in the race, they only lengthen it.
 *
 * A free that lands during a scan therefore unlinks the virtual thread and parks
 * it here rather than releasing it; the collector drains the list when the scan
 * is over. Both the free and the snapshot serialise on the registry lock, which
 * is what makes the handoff exact rather than merely likely: whichever gets the
 * lock first decides, and there is no ordering in which one sees a half-state of
 * the other. The lock is never HELD across a scan -- a frozen thread can be
 * holding it, so waiting for it under a freeze would deadlock; the flag is what
 * crosses that boundary, not the mutex.
 */
static int cn1VirtualThreadScanActive = 0;              /* guarded by the registry lock */
static struct cn1VirtualThread* cn1VirtualThreadRetired = 0;  /* likewise */

static void cn1VirtualThreadRelease(struct cn1VirtualThread* co) {
    size_t pageSize = (size_t)sysconf(_SC_PAGESIZE);
    munmap((unsigned char*)co->stackLow - pageSize, co->stackBytes + pageSize);
    free(co);
}

static void cn1VirtualThreadRegister(struct cn1VirtualThread* vt) {
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    vt->registryPrev = 0;
    vt->registryNext = cn1VirtualThreadRegistry;
    if(cn1VirtualThreadRegistry != 0) {
        cn1VirtualThreadRegistry->registryPrev = vt;
    }
    cn1VirtualThreadRegistry = vt;
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
}

static void cn1VirtualThreadUnregister(struct cn1VirtualThread* vt) {
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    if(vt->registryPrev != 0) {
        vt->registryPrev->registryNext = vt->registryNext;
    } else if(cn1VirtualThreadRegistry == vt) {
        cn1VirtualThreadRegistry = vt->registryNext;
    }
    if(vt->registryNext != 0) {
        vt->registryNext->registryPrev = vt->registryPrev;
    }
    vt->registryNext = 0;
    vt->registryPrev = 0;
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
}

void cn1VirtualThreadForEach(void (*fn)(struct cn1VirtualThread* vt, void* ctx),
                             void* ctx) {
    struct cn1VirtualThread* vt;
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    for(vt = cn1VirtualThreadRegistry ; vt != 0 ; vt = vt->registryNext) {
        fn(vt, ctx);
    }
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
}

int cn1VirtualThreadSnapshot(struct cn1VirtualThread** out, int max) {
    struct cn1VirtualThread* vt;
    int n = 0;
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    for(vt = cn1VirtualThreadRegistry ; vt != 0 ; vt = vt->registryNext) {
        if(n < max) {
            out[n] = vt;
        }
        n++;
    }
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
    return n;
}

struct cn1VirtualThread* cn1VirtualThreadForStackAddress(void* addr, int count,
                                                         struct cn1VirtualThread** snapshot) {
    int i;
    if(addr == 0) {
        return 0;
    }
    for(i = 0 ; i < count ; i++) {
        struct cn1VirtualThread* vt = snapshot[i];
        if(vt != 0 && addr >= vt->stackLow && addr < vt->stackHigh) {
            return vt;
        }
    }
    return 0;
}

/** The high end of this virtual thread's stack, for a range the caller bounds. */
void* cn1VirtualThreadArg(struct cn1VirtualThread* vt) {
    return vt == 0 ? 0 : vt->arg;
}

void* cn1VirtualThreadStackHigh(struct cn1VirtualThread* vt) {
    return vt == 0 ? 0 : vt->stackHigh;
}

int cn1VirtualThreadIsRunning(struct cn1VirtualThread* vt) {
    return vt != 0 && vt->running;
}

void* cn1VirtualThreadResumerSp(struct cn1VirtualThread* vt) {
    return vt == 0 ? 0 : vt->returnSp;
}

void* cn1VirtualThreadState(struct cn1VirtualThread* vt) {
    return vt == 0 ? 0 : vt->vmState;
}

void cn1VirtualThreadSetState(struct cn1VirtualThread* vt, void* state) {
    if(vt != 0) {
        vt->vmState = state;
    }
}

/* Implemented in assembly: save callee-saved regs, switch sp, restore. */
extern void cn1VirtualThreadSwitch(void** saveSp, void* newSp);
/* The trampoline the new stack is primed to return into. */
extern void cn1VirtualThreadTrampoline(void);

/* Entered on the virtual thread's own stack, with the virtual thread in x19/rbx. */
void cn1VirtualThreadMain(struct cn1VirtualThread* co) {
    co->body(co->arg);
    co->finished = 1;
    /* The body returned: go back and never come here again. A virtual thread whose
     * body returns must not fall off the end of its stack. */
    for(;;) {
        cn1VirtualThreadSwitch(&co->sp, co->returnSp);
    }
}

struct cn1VirtualThread* cn1VirtualThreadCreate(cn1VirtualThreadBody body, void* arg,
                                        size_t stackBytes) {
    struct cn1VirtualThread* co;
    unsigned char* stack;
    size_t pageSize = (size_t)sysconf(_SC_PAGESIZE);
    if(stackBytes < 16384) {
        stackBytes = 16384;
    }
    stackBytes = (stackBytes + pageSize - 1) & ~(pageSize - 1);
    co = (struct cn1VirtualThread*)calloc(1, sizeof(struct cn1VirtualThread));
    if(co == 0) {
        return 0;
    }
    /* A guard page below the stack turns an overflow into a fault at the point
     * of overflow, rather than silent corruption of whatever is mapped next. */
    stack = (unsigned char*)mmap(0, stackBytes + pageSize, PROT_READ | PROT_WRITE,
                                 MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if(stack == MAP_FAILED) {
        free(co);
        return 0;
    }
    mprotect(stack, pageSize, PROT_NONE);
    co->stackLow  = stack + pageSize;
    co->stackHigh = stack + pageSize + stackBytes;
    co->stackBytes = stackBytes;
    co->body = body;
    co->arg = arg;
    co->finished = 0;
    co->started = 0;
    co->sp = 0;
    co->running = 0;
    co->vmState = 0;
    co->yieldReason = CN1_VT_YIELD_IO;
    cn1VirtualThreadRegister(co);
    return co;
}

void cn1VirtualThreadFree(struct cn1VirtualThread* co) {
    int deferred;
    if(co == 0) {
        return;
    }
    cn1VirtualThreadUnregister(co);
    /* Unregistered above, so no snapshot taken from here on can see it. One taken
     * BEFORE that unlink still can, and that is exactly what the flag catches --
     * read under the same lock the snapshot walks the list under. */
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    deferred = cn1VirtualThreadScanActive;
    if(deferred) {
        co->registryNext = cn1VirtualThreadRetired;
        cn1VirtualThreadRetired = co;
    }
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
    if(deferred) {
        return;
    }
    cn1VirtualThreadRelease(co);
}

/*
 * Called by the collector around the whole stop-and-scan loop, NOT around each
 * thread: the snapshot from one iteration is still being read while the next
 * iteration runs, so a per-iteration window would leave the same race in place.
 */
void cn1VirtualThreadGcScanBegin(void) {
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    cn1VirtualThreadScanActive = 1;
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
}

void cn1VirtualThreadGcScanEnd(void) {
    struct cn1VirtualThread* list;
    pthread_mutex_lock(&cn1VirtualThreadRegistryLock);
    cn1VirtualThreadScanActive = 0;
    list = cn1VirtualThreadRetired;
    cn1VirtualThreadRetired = 0;
    pthread_mutex_unlock(&cn1VirtualThreadRegistryLock);
    /* Released outside the lock: munmap under it would hold up every host thread
     * trying to retire a connection, for no reason -- these are already unlinked
     * and nothing can reach them. */
    while(list != 0) {
        struct cn1VirtualThread* next = list->registryNext;
        cn1VirtualThreadRelease(list);
        list = next;
    }
}

int cn1VirtualThreadFinished(struct cn1VirtualThread* co) {
    return co != 0 && co->finished;
}

struct cn1VirtualThread* cn1VirtualThreadCurrent(void) {
    return cn1CurrentVirtualThread;
}

void cn1VirtualThreadStackBounds(struct cn1VirtualThread* co, void** low, void** high) {
    /* Only the part between the saved sp and the high end holds anything. Below
     * the saved sp is dead space the collector must not read: it is untouched
     * mmap in the best case and a previous call's debris otherwise. */
    if(co == 0 || co->sp == 0) {
        *low = 0; *high = 0; return;
    }
    *low = co->sp;
    *high = co->stackHigh;
}

/* Set up the initial frame so the first switch lands in the trampoline. */
extern void* cn1VirtualThreadPrime(void* stackHigh, void* co, void* trampoline);

/* The default. Overridden by the VM's strong definition when one is linked in. */
__attribute__((weak)) void cn1VirtualThreadVmStateActive(void* vmState, int active) {
    (void)vmState; (void)active;
}

void cn1VirtualThreadResume(struct cn1VirtualThread* co) {
    struct cn1VirtualThread* previous = cn1CurrentVirtualThread;
    if(co == 0 || co->finished) {
        return;
    }
    if(!co->started) {
        co->started = 1;
        co->sp = cn1VirtualThreadPrime(co->stackHigh, co, (void*)cn1VirtualThreadTrampoline);
    }
    cn1CurrentVirtualThread = co;
    co->running = 1;
    /* The attached VM state has to become ACTIVE here, not just `running`. It was
     * created parked (cn1CreateThreadLocalData with bindToCallingOsThread false
     * leaves threadActive FALSE) and nothing else ever raises it, so without this a
     * collection running concurrently treats a mutator that is executing Java as
     * parked -- and scans or migrates its object stack and pending-allocation table
     * underneath it. Missed roots at best, corruption at worst. Lowered again on the
     * way out, because a SUSPENDED virtual thread genuinely is parked: the collector
     * reaches its roots through the registry snapshot instead. */
    if(co->vmState != 0) {
        cn1VirtualThreadVmStateActive(co->vmState, 1);
    }
    cn1VirtualThreadSwitch(&co->returnSp, co->sp);
    if(co->vmState != 0) {
        cn1VirtualThreadVmStateActive(co->vmState, 0);
    }
    co->running = 0;
    cn1CurrentVirtualThread = previous;
}

void cn1VirtualThreadSetYieldReason(int reason) {
    struct cn1VirtualThread* vt = cn1CurrentVirtualThread;
    if(vt != 0) {
        vt->yieldReason = reason;
    }
}

int cn1VirtualThreadYieldReason(struct cn1VirtualThread* vt) {
    return vt == 0 ? CN1_VT_YIELD_IO : vt->yieldReason;
}

int cn1VirtualThreadYieldIfVirtual(void) {
    if(cn1CurrentVirtualThread == 0) {
        return 0;
    }
    cn1VirtualThreadSetYieldReason(CN1_VT_YIELD_RUNNABLE);
    cn1VirtualThreadYield();
    return 1;
}

void cn1VirtualThreadYield(void) {
    struct cn1VirtualThread* co = cn1CurrentVirtualThread;
    if(co == 0) {
        return;             /* not on a virtual thread: nothing to yield from */
    }
    cn1VirtualThreadSwitch(&co->sp, co->returnSp);
}

#endif /* CN1_VIRTUAL_THREADS */
