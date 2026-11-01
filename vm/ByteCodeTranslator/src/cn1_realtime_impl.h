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
 * The operating-system half of com.codename1.realtime, shared by the Linux port
 * (cn1_linux_realtime.c) and the headless backend (vm/backend/native/cn1_backend_realtime.c).
 * Each defines the natives under its own class's name and forwards here, as
 * cn1_hardware_impl.h does. The VM half -- what the collector and the allocator do for a
 * real-time thread -- is cn1Rt* at the end of cn1_globals.m.
 *
 * Every function acts on the CALLING thread: a real-time thread enters, freezes, waits
 * and leaves from its own body, so there are no handles and the per-thread state is
 * __thread. Results are >= 0, or -errno.
 *
 * Linux gets the real thing: SCHED_FIFO through pthread_setschedparam, CPU affinity, the
 * other threads moved off a reserved CPU, mlockall, and an absolute periodic release on
 * CLOCK_MONOTONIC with clock_nanosleep(TIMER_ABSTIME), which is what keeps a period from
 * drifting by the cycle's own run time. PREEMPT_RT (mainline since 6.12) is what bounds
 * the kernel's side of the latency; on a stock kernel the same calls run, with worse
 * worst cases, and RT_FEATURE_PREEMPT_RT says which one this is. Elsewhere -- the
 * backend on macOS during development -- the VM half still works and the timing is a
 * relative sleep.
 */

#ifndef CN1_REALTIME_IMPL_H
#define CN1_REALTIME_IMPL_H

#include "cn1_globals.h"
#include <errno.h>
#include <string.h>
#include <time.h>
#include <pthread.h>
#include <sched.h>
#include <unistd.h>
#if defined(__linux__)
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#endif

/* Feature bits; the same values as Realtime.FEATURE_* in Java. */
#define CN1_RT_FEATURE_SCHEDULING   1
#define CN1_RT_FEATURE_MEMORY_LOCK  2
#define CN1_RT_FEATURE_GC_ISOLATION 4
#define CN1_RT_FEATURE_ALLOC_TRAP   8
#define CN1_RT_FEATURE_PREEMPT_RT   16
#define CN1_RT_FEATURE_CPU_AFFINITY 32

/* What rtEnter applied, as a mask; the Java side decides whether a missing piece is fatal. */
#define CN1_RT_APPLIED_SCHEDULING 1
#define CN1_RT_APPLIED_AFFINITY   2
#define CN1_RT_APPLIED_ISOLATION  4

/* Statistics slots, the layout RealtimeStatistics reads. Latency is release to wake-up,
 * execution is wake-up to the next wait; the histogram counts latencies by powers of two
 * of a microsecond: [0] under 1us, [1] under 2us, ... [15] 16ms and over. */
#define CN1_RT_STAT_CYCLES        0
#define CN1_RT_STAT_MISSED        1
#define CN1_RT_STAT_MAX_LATENCY   2
#define CN1_RT_STAT_SUM_LATENCY   3
#define CN1_RT_STAT_MAX_EXEC      4
#define CN1_RT_STAT_SUM_EXEC      5
#define CN1_RT_STAT_VIOLATIONS    6
#define CN1_RT_STAT_HIST          7
#define CN1_RT_HIST_BUCKETS       16
#define CN1_RT_STAT_COUNT         (CN1_RT_STAT_HIST + CN1_RT_HIST_BUCKETS)

static __thread long long cn1RtPeriodNs = 0;
static __thread long long cn1RtNextReleaseNs = 0;
static __thread long long cn1RtWokeNs = 0;
static __thread long long cn1RtStats[CN1_RT_STAT_COUNT];
static __thread int cn1RtOldPolicy = -1;
static __thread struct sched_param cn1RtOldParam;

static long long cn1RtNowNs(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long long) ts.tv_sec * 1000000000LL + ts.tv_nsec;
}

#if defined(__linux__)
static int cn1RtKernelIsPreemptRt(void) {
    FILE* f = fopen("/sys/kernel/realtime", "r");
    int c;
    if (f == 0) {
        return 0;
    }
    c = fgetc(f);
    fclose(f);
    return c == '1';
}

/* The threads cn1RtIsolateCpu moved, so cn1RtRestoreCpu gives the core back to exactly
 * those when the real-time thread ends -- otherwise the process would keep one core
 * fewer for good. Only the real-time thread that isolated touches its own list. */
#define CN1_RT_MAX_MOVED 512
static __thread pid_t cn1RtMoved[CN1_RT_MAX_MOVED];
static __thread int cn1RtMovedCount = 0;
static __thread int cn1RtIsolatedCpu = -1;

/* Takes `cpu` away from every other thread of the process, so the real-time thread has
 * it to itself. Threads started later inherit their creator's mask, and a real-time
 * thread never creates one, so they stay off it too. Best effort: a thread that exits
 * between the listing and the call is not an error. */
static int cn1RtIsolateCpu(int cpu) {
    DIR* d = opendir("/proc/self/task");
    struct dirent* e;
    pid_t self = (pid_t) syscall(SYS_gettid);
    int moved = 0;
    if (d == 0) {
        return -errno;
    }
    while ((e = readdir(d)) != 0) {
        cpu_set_t set;
        pid_t tid;
        if (e->d_name[0] < '0' || e->d_name[0] > '9') {
            continue;
        }
        tid = (pid_t) strtol(e->d_name, 0, 10);
        if (tid == self || sched_getaffinity(tid, sizeof(set), &set) != 0) {
            continue;
        }
        if (CPU_ISSET(cpu, &set) && CPU_COUNT(&set) > 1) {
            CPU_CLR(cpu, &set);
            if (sched_setaffinity(tid, sizeof(set), &set) == 0) {
                moved++;
                if (cn1RtMovedCount < CN1_RT_MAX_MOVED) {
                    cn1RtMoved[cn1RtMovedCount++] = tid;
                }
            }
        }
    }
    closedir(d);
    cn1RtIsolatedCpu = cpu;
    return moved;
}

/* Gives the isolated core back to the threads that had it. A thread that has exited,
 * or whose id was reused, fails or gains a core it may use anyway: best effort. */
static void cn1RtRestoreCpu(void) {
    int i;
    if (cn1RtIsolatedCpu < 0) {
        return;
    }
    for (i = 0; i < cn1RtMovedCount; i++) {
        cpu_set_t set;
        if (sched_getaffinity(cn1RtMoved[i], sizeof(set), &set) == 0) {
            CPU_SET(cn1RtIsolatedCpu, &set);
            sched_setaffinity(cn1RtMoved[i], sizeof(set), &set);
        }
    }
    cn1RtMovedCount = 0;
    cn1RtIsolatedCpu = -1;
}

/* Touches the stack the thread will run on, so a page fault never lands in a period.
 * With mlockall(MCL_FUTURE) the pages then stay. 128KB is well inside the
 * CN1_THREAD_STACK_BYTES (16MB) every VM thread is created with. */
static void cn1RtPrefaultStack(void) {
    volatile char buf[128 * 1024];
    size_t i;
    for (i = 0; i < sizeof(buf); i += 4096) {
        buf[i] = 0;
    }
}
#endif

static JAVA_INT cn1RtImpl_rtFeatures(CODENAME_ONE_THREAD_STATE) {
    int f = CN1_RT_FEATURE_GC_ISOLATION;
#ifdef CN1_VIRTUAL_THREADS
    f |= CN1_RT_FEATURE_ALLOC_TRAP;
#endif
#if defined(__linux__)
    f |= CN1_RT_FEATURE_SCHEDULING | CN1_RT_FEATURE_MEMORY_LOCK | CN1_RT_FEATURE_CPU_AFFINITY;
    if (cn1RtKernelIsPreemptRt()) {
        f |= CN1_RT_FEATURE_PREEMPT_RT;
    }
#endif
    return f;
}

static JAVA_INT cn1RtImpl_rtLockMemory(CODENAME_ONE_THREAD_STATE) {
#if defined(__linux__)
    /* MCL_ONFAULT: the heap and the virtual-thread stacks are large address
     * reservations of which a fraction is ever touched, and locking them up front
     * asks for all of it at once -- mlockall(MCL_CURRENT) fails with ENOMEM on any
     * machine with less RAM than the reservations. With MCL_ONFAULT a page is locked
     * when first touched and never leaves; the real-time thread's own stack is
     * touched in advance by cn1RtImpl_rtEnter. Kernels before 4.4 lack the flag and
     * reject it with EINVAL, so they get the plain call. */
#ifdef MCL_ONFAULT
    if (mlockall(MCL_CURRENT | MCL_FUTURE | MCL_ONFAULT) == 0) {
        return 0;
    }
    if (errno != EINVAL) {
        return -errno;
    }
#endif
    if (mlockall(MCL_CURRENT | MCL_FUTURE) != 0) {
        return -errno;
    }
    return 0;
#else
    return -ENOSYS;
#endif
}

/* Makes the calling thread real-time: the VM's part first, which cannot fail, then the
 * scheduler's, which needs CAP_SYS_NICE or an rtprio limit and so can. Returns the
 * CN1_RT_APPLIED_ mask, or -errno for an argument the scheduler rejects outright. */
static JAVA_INT cn1RtImpl_rtEnter(CODENAME_ONE_THREAD_STATE, JAVA_INT priority, JAVA_INT cpu, JAVA_BOOLEAN isolate) {
    int applied = 0;
    cn1RtEnter(threadStateData);
    memset(cn1RtStats, 0, sizeof(cn1RtStats));
    cn1RtPeriodNs = 0;
#if defined(__linux__)
    {
        struct sched_param p;
        int lo = sched_get_priority_min(SCHED_FIFO);
        int hi = sched_get_priority_max(SCHED_FIFO);
        if (priority < lo || priority > hi) {
            cn1RtLeave(threadStateData);
            return -EINVAL;
        }
        if (cn1RtOldPolicy < 0) {
            pthread_getschedparam(pthread_self(), &cn1RtOldPolicy, &cn1RtOldParam);
        }
        memset(&p, 0, sizeof(p));
        p.sched_priority = priority;
        if (pthread_setschedparam(pthread_self(), SCHED_FIFO, &p) == 0) {
            applied |= CN1_RT_APPLIED_SCHEDULING;
            threadStateData->rtPriority = priority;
        }
    }
    if (cpu >= 0) {
        cpu_set_t set;
        if (cpu >= CPU_SETSIZE) {
            cn1RtLeave(threadStateData);
            return -EINVAL;
        }
        CPU_ZERO(&set);
        CPU_SET(cpu, &set);
        if (pthread_setaffinity_np(pthread_self(), sizeof(set), &set) == 0) {
            applied |= CN1_RT_APPLIED_AFFINITY;
            if (isolate && cn1RtIsolateCpu(cpu) >= 0) {
                applied |= CN1_RT_APPLIED_ISOLATION;
            }
        }
    }
    cn1RtPrefaultStack();
#else
    (void) priority;
    (void) cpu;
    (void) isolate;
#endif
    return applied;
}

static JAVA_VOID cn1RtImpl_rtLeave(CODENAME_ONE_THREAD_STATE) {
    cn1RtLeave(threadStateData);
#if defined(__linux__)
    if (cn1RtOldPolicy >= 0) {
        pthread_setschedparam(pthread_self(), cn1RtOldPolicy, &cn1RtOldParam);
        cn1RtOldPolicy = -1;
    }
    cn1RtRestoreCpu();
#endif
}

/* Starts the periodic clock: the first release is one period from now. */
static JAVA_VOID cn1RtImpl_rtStartPeriod(CODENAME_ONE_THREAD_STATE, JAVA_LONG periodNs) {
    cn1RtPeriodNs = periodNs > 0 ? periodNs : 1000000LL;
    cn1RtNextReleaseNs = cn1RtNowNs() + cn1RtPeriodNs;
    cn1RtWokeNs = 0;
}

static void cn1RtRecord(long long latency, long long exec) {
    int b = 0;
    long long us = latency / 1000;
    cn1RtStats[CN1_RT_STAT_CYCLES]++;
    if (latency > cn1RtStats[CN1_RT_STAT_MAX_LATENCY]) {
        cn1RtStats[CN1_RT_STAT_MAX_LATENCY] = latency;
    }
    cn1RtStats[CN1_RT_STAT_SUM_LATENCY] += latency;
    if (exec > cn1RtStats[CN1_RT_STAT_MAX_EXEC]) {
        cn1RtStats[CN1_RT_STAT_MAX_EXEC] = exec;
    }
    cn1RtStats[CN1_RT_STAT_SUM_EXEC] += exec;
    while (us > 0 && b < CN1_RT_HIST_BUCKETS - 1) {
        us >>= 1;
        b++;
    }
    cn1RtStats[CN1_RT_STAT_HIST + b]++;
}

/* Sleeps to the next release and returns how many releases were missed (0 when on time).
 * Parked for the whole sleep, so the collector scans this thread then rather than
 * stopping it later; the wake-up is the only place it can be held, and only while its
 * own stack is being read. A missed release is skipped rather than run late in a burst:
 * a control loop wants the next period on time, not three in a row. */
static JAVA_INT cn1RtImpl_rtWaitNextPeriod(CODENAME_ONE_THREAD_STATE) {
    long long before = cn1RtNowNs();
    long long woke;
    long long exec = cn1RtWokeNs > 0 ? before - cn1RtWokeNs : 0;
    int missed = 0;
    if (cn1RtPeriodNs <= 0) {
        return -EINVAL;
    }
    CN1_RT_PARK(threadStateData);
#if defined(__linux__)
    {
        struct timespec t;
        int r;
        t.tv_sec = (time_t) (cn1RtNextReleaseNs / 1000000000LL);
        t.tv_nsec = (long) (cn1RtNextReleaseNs % 1000000000LL);
        do {
            r = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &t, 0);
        } while (r == EINTR);
    }
#else
    {
        long long left = cn1RtNextReleaseNs - before;
        if (left > 0) {
            struct timespec t;
            t.tv_sec = (time_t) (left / 1000000000LL);
            t.tv_nsec = (long) (left % 1000000000LL);
            while (nanosleep(&t, &t) != 0 && errno == EINTR) {
            }
        }
    }
#endif
    CN1_RT_UNPARK(threadStateData);
    woke = cn1RtNowNs();
    cn1RtRecord(woke > cn1RtNextReleaseNs ? woke - cn1RtNextReleaseNs : 0, exec);
    cn1RtNextReleaseNs += cn1RtPeriodNs;
    while (cn1RtNextReleaseNs <= woke) {
        cn1RtNextReleaseNs += cn1RtPeriodNs;
        missed++;
    }
    cn1RtStats[CN1_RT_STAT_MISSED] += missed;
    cn1RtWokeNs = woke;
    cn1RtNewPeriod(threadStateData);
    return missed;
}

static JAVA_VOID cn1RtImpl_rtFreeze(CODENAME_ONE_THREAD_STATE, JAVA_INT budget, JAVA_OBJECT error) {
    threadStateData->rtAllocError = error;
    cn1RtFreeze(threadStateData, budget);
}

static JAVA_VOID cn1RtImpl_rtThaw(CODENAME_ONE_THREAD_STATE) {
    cn1RtThaw(threadStateData);
}

/* Copies this thread's statistics into `out` (CN1_RT_STAT_COUNT longs). */
static JAVA_INT cn1RtImpl_rtStatistics(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    int i;
    int n;
    JAVA_LONG* d;
    if (out == JAVA_NULL) {
        return -EINVAL;
    }
    cn1RtStats[CN1_RT_STAT_VIOLATIONS] = cn1RtThreadAllocViolations(threadStateData);
    n = CN1_ARRAY_LENGTH(out);
    if (n > CN1_RT_STAT_COUNT) {
        n = CN1_RT_STAT_COUNT;
    }
    d = (JAVA_LONG*) CN1_ARRAY_DATA(out);
    for (i = 0; i < n; i++) {
        d[i] = (JAVA_LONG) cn1RtStats[i];
    }
    return n;
}

static JAVA_OBJECT cn1RtImpl_rtLastViolation(CODENAME_ONE_THREAD_STATE) {
    const char* c = cn1RtLastViolationClass();
    return c == 0 ? JAVA_NULL : newStringFromCString(threadStateData, c);
}

static JAVA_VOID cn1RtImpl_rtSetMemoryLimit(CODENAME_ONE_THREAD_STATE, JAVA_LONG bytes, JAVA_OBJECT error) {
    cn1RtSetMemoryLimit(bytes, error);
}

#endif /* CN1_REALTIME_IMPL_H */
