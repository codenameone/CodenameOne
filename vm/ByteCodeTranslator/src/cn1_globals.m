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

// glibc/musl hide pthread_getattr_np and the ucontext REG_* gregs indices
// behind _GNU_SOURCE; must be defined before the first libc include.
#if defined(__linux__) && !defined(_GNU_SOURCE)
#define _GNU_SOURCE
#endif
#include "cn1_globals.h"
#include <assert.h>
#include <time.h>   // clock_gettime: paces the low-memory allocation throttle
#ifndef _WIN32
#include <unistd.h>    // getpagesize: sizes the BiBOP page-release window
#include <sys/mman.h>  // madvise: hands surplus empty BiBOP pages back to the OS
#include <errno.h>
#endif
#if defined(__APPLE__)
#include <mach/mach.h>
#include <mach/task_info.h>
#endif
#include "java_lang_Class.h"
#include "java_lang_Object.h"
#include "java_lang_Boolean.h"
#include "java_lang_String.h"
#include "java_lang_Integer.h"
#include "java_lang_Byte.h"
#include "java_lang_Short.h"
#include "java_lang_Character.h"
#include "java_lang_Thread.h"
#include "java_lang_Long.h"
#include "java_lang_Double.h"
#include "java_lang_Float.h"
#include "java_lang_Runnable.h"
#include "java_lang_System.h"
#include "java_lang_ArrayIndexOutOfBoundsException.h"
#if defined(__APPLE__) && defined(__OBJC__)
#import <TargetConditionals.h>
#import <mach/mach.h>
#import <mach/mach_host.h>
// os_proc_available_memory reports the bytes this PROCESS has left before it hits
// its dirty-memory limit -- the figure the kernel actually meters an app against,
// and the one the GC pacing cap needs (issue #5537). It is API_UNAVAILABLE(macos),
// which covers Mac Catalyst too, so it is only reachable on a real iOS/tvOS/watchOS
// target; the __has_include keeps an older SDK compiling.
#if TARGET_OS_IPHONE && !TARGET_OS_MACCATALYST && __has_include(<os/proc.h>)
#import <os/proc.h>
#define CN1_HAS_PROC_AVAILABLE_MEMORY 1
#endif
#else
#include <time.h>
#ifndef _WIN32
#include <unistd.h>
#endif
#if defined(__APPLE__) && !defined(__OBJC__)
#include <sys/sysctl.h>   // sysctlbyname for the non-OBJC get_free_memory headroom proxy
#endif
#define NSLog(...) printf(__VA_ARGS__); printf("\n")
#endif

#ifdef CN1_GC_VERIFY
// QA-only heap verifier (see the block next to cn1ConservativeResolve). It
// classifies references against the same page/extent index the conservative
// root scan builds, so it is only meaningful in that (shipping) configuration.
#ifndef CN1_CONSERVATIVE_GC_ROOTS
#error "CN1_GC_VERIFY requires CN1_CONSERVATIVE_GC_ROOTS"
#endif
// malloc's size accounting supplies the extent of a legacy block to poison --
// the object header does not record instance size.
#if defined(__APPLE__)
#include <malloc/malloc.h>
#elif defined(__linux__)
#include <malloc.h>
#endif
// Which mark pass is currently running. CN1_GC_TRACE_MARK reports it, which is
// how "what is still keeping this object alive?" gets an answer -- most often
// the conservative native-stack scan holding a dead frame's leftover word.
const char* cn1GcMarkPhase = "?";
// CN1_GC_FAULT=<name>: deliberately break one collector invariant so a run can
// prove the verifier still has teeth. A gate nobody has ever watched fail is not
// a gate. Supported: "nograce" -- skip the BiBOP grace-subtree pass, which is
// exactly the defect that shipped in #5436 and was fixed in #5442.
int cn1GcFaultNoGrace = 0;
// CN1_GC_FAULT=earlyfree restores the pre-fix O(1) page-reclaim bound
// (gcLastMarkedEpoch != V), which frees slots the per-slot walk would keep.
int cn1GcFaultEarlyFree = 0;
void cn1GcFaultInitPublic(void);
static void cn1GcFaultInit(void) {
    static int done = 0;
    if(done) return;
    done = 1;
    const char* f = getenv("CN1_GC_FAULT");
    if(f == 0) return;
    if(strcmp(f, "nograce") == 0) {
        cn1GcFaultNoGrace = 1;
        fprintf(stderr, "[GC-FAULT] grace-subtree pass DISABLED (fault injection)\n");
    } else if(strcmp(f, "earlyfree") == 0) {
        cn1GcFaultEarlyFree = 1;
        fprintf(stderr, "[GC-FAULT] O(1) page reclaim restored to the pre-fix bound\n");
    } else {
        fprintf(stderr, "[GC-FAULT] unknown fault '%s'\n", f);
    }
    fflush(stderr);
}
void cn1GcFaultInitPublic(void) { cn1GcFaultInit(); }
#endif

#if defined(__APPLE__) && defined(__OBJC__)
#if TARGET_OS_SIMULATOR
#define CN1_GC_ASSERT(condition, message) \
    do { \
        if (!(condition)) { \
            __assert_rtn(__func__, __FILE__, __LINE__, message); \
        } \
    } while (0)
#else
#define CN1_GC_ASSERT(condition, message) \
    do { \
        (void)(condition); \
        (void)(message); \
    } while (0)
#endif
#else
#define CN1_GC_ASSERT(condition, message) CODENAME_ONE_ASSERT(condition)
#endif

// The amount of memory allocated between GC cycle checks (generally 30 seconds)
// that triggers "High-frequency" GC mode.  When "High-frequency" mode is triggered,
// it will only wait 200ms before triggering another GC cycle after completing the
// previous one.  Normally it's 30 seconds.
// This value is in bytes
long CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD = 1024 * 1024;

// "High frequency" GC mode won't be enabled until the "total" allocated memory
// in the app reaches this threshold
// This value is in bytes
long CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD = 10 * 1024 * 1024;


// The number of allocations (not measured in bytes, but actual allocation count) made on
// a thread that will result in the thread being treated as an aggressive allocator.
// If, during GC, it hits a thread that is an aggressive allocator, GC will lock that thread
// until the sweep is complete for all threads.  Normally, the thread is only locked while
// its objects are being marked.
// If the EDT is hitting this threshold, we'll have problems
long CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD = 5000;

long CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD_EDT = 10000;

// The max number of allocations (not bytes, but number) on a thread before 
// it will refuse to increase its size.  This is checked when allocating objects.
// If the thread is at its max size during allocation, it will aggressively call
// the GC and wait until the GC is complete before the allocation can occur.
// On the EDT, this has usability consequences.
// If the allocation array is maxed out, but hasn't reached this max size,
// it will double the size of the allocation array and trigger a GC (but not wait
// for the GC to complete).  
long CN1_MAX_HEAP_SIZE = 10000;

// Special value for the EDT to possibly allow for a larger allocation stack on the 
// EDT.
long CN1_MAX_HEAP_SIZE_EDT = 10000;

// THE THREAD ID OF THE EDT.  We'll treat the EDT specially.
long CN1_EDT_THREAD_ID = -1;

// A flag to indicate if the GC thresholds are initialized yet
// @see init_gc_thresholds
static JAVA_BOOLEAN GC_THRESHOLDS_INITIALIZED = JAVA_FALSE;

int currentGcMarkValue = 1;
#if defined(__APPLE__) && defined(__OBJC__)
extern _Atomic JAVA_BOOLEAN lowMemoryMode;
#else
_Atomic JAVA_BOOLEAN lowMemoryMode = JAVA_FALSE;
#endif

static JAVA_BOOLEAN isEdt(long threadId) {
    return (CN1_EDT_THREAD_ID == threadId);
}

// Gets the amount of free memory in the system.
 static long get_free_memory(void)
 {
#if defined(__APPLE__) && defined(__OBJC__)
   mach_port_t host_port;
   mach_msg_type_number_t host_size;
   vm_size_t pagesize;
   host_port = mach_host_self();
   host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
   host_page_size(host_port, &pagesize);
   vm_statistics_data_t vm_stat;
   if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS)
   {
     #if defined(__OBJC__)
     NSLog(@"Failed to fetch vm statistics");
     #endif
     return 0;
   }
   /* Stats in bytes */
   long mem_free = vm_stat.free_count * pagesize;
   return mem_free;
#else
   return 1024 * 1024 * 100; // Stub: 100MB
#endif
 }

// AVAILABLE memory (not just free_count) -- the HOST-WIDE headroom reading, used by
// the dynamic GC pacing cap on platforms that impose no per-process memory limit.
// Where there IS such a limit, cn1ProcessHeadroom below supersedes this; see the
// comment there for why the distinction is the whole of issue #5537.
// iOS/macOS keep RAM full of reclaimable file cache, so free_count alone is always
// tiny (~100MB) and badly under-reports what the process can still allocate; inactive
// + purgeable pages are reclaimable under pressure, so free + inactive + purgeable ~=
// the real headroom the collector can safely let a high-throughput thread run into.
// Non-OBJC Apple (bench/desktop) lacks the mach vm_statistics headers here, so it
// falls back to half of physical RAM via sysctl.
static long cn1_available_memory(void)
 {
#if defined(__APPLE__) && defined(__OBJC__)
   mach_port_t host_port = mach_host_self();
   mach_msg_type_number_t host_size = sizeof(vm_statistics_data_t) / sizeof(integer_t);
   vm_size_t pagesize;
   host_page_size(host_port, &pagesize);
   vm_statistics_data_t vm_stat;
   if (host_statistics(host_port, HOST_VM_INFO, (host_info_t)&vm_stat, &host_size) != KERN_SUCCESS) {
     return 1024L * 1024 * 100;
   }
   return ((long)vm_stat.free_count + (long)vm_stat.inactive_count
           + (long)vm_stat.purgeable_count) * (long)pagesize;
#elif defined(__APPLE__)
   uint64_t __total = 0; size_t __len = sizeof(__total);
   if (sysctlbyname("hw.memsize", &__total, &__len, NULL, 0) == 0 && __total > 0) {
     return (long)(__total / 2);
   }
   return 1024L * 1024 * 100;
#else
   return 1024L * 1024 * 100;
#endif
 }

// PER-PROCESS memory headroom: the bytes this process has left before the kernel
// kills it, or 0 on a platform that imposes no such limit. This is the figure the
// GC pacing cap has to be sized against, and using the host-wide reading instead is
// the whole of issue #5537.
//
// iOS terminates an app that crosses its dirty-memory ceiling -- roughly 1.4GB on an
// iPad -- with EXC_RESOURCE (RESOURCE_TYPE_MEMORY: high watermark memory limit
// exceeded). That ceiling is a property of the PROCESS and has nothing to do with how
// much RAM the device has spare, so cn1_available_memory's host-wide answer let
// cn1BibopPacingCap hand a high-throughput thread fm/2 of slack -- gigabytes on a
// large-RAM iPad. The mutator was licensed to run further ahead of the collector than
// the process was allowed to exist, which is precisely the failure that function's own
// comment warns about ("removing it unconditionally let the mutator outrun the
// collector and balloon RSS to ~2GB"), reintroduced by measuring the wrong quantity.
// A deep game-tree search hit it reproducibly on device while running fine in the
// simulator and on Android, where no such per-process ceiling exists.
//
// os_proc_available_memory() is the documented cheap probe (equivalent to
// task_vm_info.limit_bytes_remaining without task_info's cost). Apple advises against
// caching the result, and the pacing cap consults it only on the rare page-acquire
// path, so it is read live rather than through cn1CachedFreeMem.
//
// AMBIGUOUS ZERO. The call returns 0 both when the process has NO limit and when it
// has already EXCEEDED one -- opposite meanings, and the second is the emergency where
// pacing matters most, so it must not be read as "unlimited". They are separated by
// history rather than by the call: a process that has ever reported a positive figure
// demonstrably has a limit, so once that is latched a later 0 can only mean the budget
// is gone. Before the first positive reading 0 is taken at face value as "no limit",
// which is correct for macOS/Catalyst/Linux/Windows and for an SDK or OS too old to
// have the symbol.
// TEST HOOK. CN1_SIMULATE_PROC_MEMORY_LIMIT=<bytes> gives this process a synthetic
// per-process ceiling. The clamp in cn1BibopPacingCap only engages under a hard
// budget, and the only targets that impose one are iOS/tvOS/watchOS devices, so
// without this hook the issue-5537 fix is untestable anywhere CI can run -- which is
// how the bug survived in the first place. Off unless set. -1 = env not probed yet.
// long long, NOT long: on the Windows LLP64 target long is 32-bit and this holds a
// byte count that can exceed 2GB.
static _Atomic long long cn1SimulatedProcLimit = -1;
static long long cn1SimulatedProcLimitBytes(void) {
    long long v = atomic_load_explicit(&cn1SimulatedProcLimit, memory_order_relaxed);
    if(v < 0) {
        const char* e = getenv("CN1_SIMULATE_PROC_MEMORY_LIMIT");
        v = e ? atoll(e) : 0;
        if(v < 0) {
            v = 0;
        }
        atomic_store_explicit(&cn1SimulatedProcLimit, v, memory_order_relaxed);
    }
    return v;
}

// Bytes this process is metered at, for the simulated-limit hook only. phys_footprint
// on Apple and RSS on Linux, matching what nativeMethods.m reports through Runtime, so
// a test can compare the two readings directly. Anything else has no probe and simply
// never reports a simulated limit.
static long cn1ProcFootprintBytes(void) {
#if defined(__APPLE__)
    task_vm_info_data_t info;
    mach_msg_type_number_t count = TASK_VM_INFO_COUNT;
    if(task_info(mach_task_self(), TASK_VM_INFO, (task_info_t)&info, &count) == KERN_SUCCESS) {
        return (long)info.phys_footprint;
    }
    return 0;
#elif defined(__linux__)
    // /proc/self/statm field 2 is resident pages. Parsed with fgets + strtoul rather
    // than the scanf family: glibc 2.38 redirects fscanf to __isoc23_fscanf in
    // <stdio.h>, and the cross-linked Linux target resolves against a sysroot that has
    // no such symbol, so any retained scanf call fails the link outright
    // (ld.lld: undefined symbol: __isoc23_fscanf).
    FILE* f = fopen("/proc/self/statm", "r");
    if(f == 0) {
        return 0;
    }
    char buf[128];
    char* line = fgets(buf, sizeof(buf), f);
    fclose(f);
    if(line == 0) {
        return 0;
    }
    char* end = 0;
    strtoul(line, &end, 10);            // field 1: total program size, unused
    if(end == line) {
        return 0;
    }
    char* residentStart = end;
    unsigned long resident = strtoul(residentStart, &end, 10);
    if(end == residentStart) {
        return 0;
    }
    long ps = sysconf(_SC_PAGESIZE);
    if(ps <= 0) {
        return 0;
    }
    return (long)(resident * (unsigned long)ps);
#else
    return 0;
#endif
}

static _Atomic int cn1ProcHasMemoryLimit = 0;
static long cn1ProcessHeadroom(void) {
    long long simLimit = cn1SimulatedProcLimitBytes();
    if(simLimit > 0) {
        long long used = (long long)cn1ProcFootprintBytes();
        if(used <= 0) {
            return -1;      // no footprint probe on this platform; hook inert
        }
        return used >= simLimit ? 0 : (long)(simLimit - used);
    }
#ifdef CN1_HAS_PROC_AVAILABLE_MEMORY
    if(__builtin_available(iOS 13.0, tvOS 13.0, watchOS 6.0, *)) {
        size_t remaining = os_proc_available_memory();
        if(remaining > 0) {
            atomic_store_explicit(&cn1ProcHasMemoryLimit, 1, memory_order_relaxed);
            return (long)remaining;
        }
        if(atomic_load_explicit(&cn1ProcHasMemoryLimit, memory_order_relaxed)) {
            return 0;   // over the limit: no headroom, pace as hard as we can
        }
    }
#endif
    return -1;          // this platform has no per-process limit
}

// Bytes admitted for dirtying but not yet reflected in phys_footprint; see
// cn1PacingTryAdmit. Declared here rather than beside the rest of the pacing state
// because collectThreadResources, far above it, has to be able to hand a claim back.
static _Atomic long long cn1PacingClaimed = 0;
// This thread's outstanding claim. __thread rather than a ThreadLocalData field, matching
// cn1LowMemoryParkStampMs: zero-initialized per thread with no malloc'd-not-zeroed trap.
static __thread long long cn1MyPacingClaim = 0;

// GC epoch this thread's claim belongs to; see cn1PacingExpireThreadClaim.
static __thread int cn1MyPacingClaimEpoch = 0;

static void cn1PacingReleaseThreadClaim(void) {
    if(cn1MyPacingClaim != 0) {
        atomic_fetch_sub_explicit(&cn1PacingClaimed, cn1MyPacingClaim,
                                  memory_order_relaxed);
        cn1MyPacingClaim = 0;
    }
}

// Drop this thread's accumulated claim once a collection boundary has passed.
//
// A claim covers the window between allocating a block and writing to it, which is
// invisible to phys_footprint. The tempting release point is the thread's NEXT check --
// "by now it must have written the last one" -- but that is not something Java
// guarantees: `a = new byte[32MB]; b = new byte[32MB];` allocates both before touching
// either, and releasing a's claim while allocating b hands back a reservation for pages
// that are still absent from the footprint. So claims ACCUMULATE within a cycle window
// instead, which over-counts a thread holding several untouched blocks -- and
// over-counting is the safe direction, since it only paces harder.
//
// A cycle boundary is a sound expiry point: a block allocated before it has either been
// written (so phys_footprint counts it and the claim would double-charge) or is garbage
// (so the sweep reclaimed it and the claim is meaningless). Every pacing park requests a
// collection, so under pressure boundaries arrive continuously and the accumulation
// stays small.
// NOT held for live-but-untouched blocks, deliberately. A never-written array is not
// footprint at all, so reserving for it past the boundary would charge the process for
// memory that does not exist and throttle every allocator for the life of the app --
// an unbounded over-reservation traded for a bounded under-reservation. Dirtying an
// old block also never reaches the allocator, so no expiry policy could catch it; live
// headroom paces on the next allocation once those pages are really written.
static void cn1PacingExpireThreadClaim(void) {
    int epochNow = atomic_load_explicit(&bibopGcEpoch, memory_order_relaxed);
    if(cn1MyPacingClaimEpoch != epochNow) {
        cn1PacingReleaseThreadClaim();
        cn1MyPacingClaimEpoch = epochNow;
    }
}

// Monotonic milliseconds, used to pace the low-memory allocation throttle.
// Monotonic (not wall clock) so a clock adjustment cannot make the throttle
// either fire on every allocation or stop firing for hours.
static JAVA_LONG cn1MonotonicMillis(void) {
#ifdef _WIN32
    /* clock_gettime / CLOCK_MONOTONIC are absent from the MSVC / clang-cl target.
       gettimeofday would compile, but it is the WALL clock: an NTP step or a user
       clock change would either suppress the throttle for the length of the jump
       or park on every allocation until the clock caught up. cn1_win_compat's
       cn1_monotonic_micros is QueryPerformanceCounter-backed, i.e. actually
       monotonic. */
    return (JAVA_LONG)(cn1_monotonic_micros() / 1000LL);
#else
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (((JAVA_LONG)ts.tv_sec) * 1000LL) + (((JAVA_LONG)ts.tv_nsec) / 1000000LL);
#endif
}

// Monotonic-millisecond stamp of this thread's last low-memory throttle park,
// which bounds the throttle to one park per CN1_LOW_MEMORY_PARK_INTERVAL_MS
// instead of one per allocation. __thread rather than a ThreadLocalData field:
// zero-initialized per thread with no malloc'd-not-zeroed trap to remember, and
// it leaves the struct layout (and therefore every generated translation unit's
// codegen) untouched.
static __thread JAVA_LONG cn1LowMemoryParkStampMs = 0;

// Low-memory throttle accounting, reported by CN1_LOG_LOWMEM_PARKS at exit and
// asserted on by LowMemoryThrottleIntegrationTest: a regression that restores the
// park-on-every-allocation behaviour shows up as parks ~= throttledAllocations.
// Counting is gated on the tracer so a shipping build pays nothing for it -- the
// counters live on the legacy allocation path, which is hot during exactly the
// pressure this throttle responds to. -1 = env not probed yet.
static _Atomic long cn1LowMemoryParks = 0;
static _Atomic long cn1LowMemoryThrottledAllocations = 0;
static _Atomic int cn1LowMemoryTrace = -1;
static int cn1LowMemoryTraceOn(void) {
    int on = atomic_load_explicit(&cn1LowMemoryTrace, memory_order_relaxed);
    if(on < 0) {
        on = getenv("CN1_LOG_LOWMEM_PARKS") ? 1 : 0;
        atomic_store_explicit(&cn1LowMemoryTrace, on, memory_order_relaxed);
    }
    return on;
}

// TEST HOOK. CN1_SIMULATE_MEMORY_WARNING_MS=<ms> raises lowMemoryMode at the
// given cadence, standing in for the sustained UIApplicationDidReceiveMemoryWarning
// delivery a memory-constrained device produces (each warning raises the flag; the
// next completed collection cycle lowers it). Nothing else can reach this state off
// iOS, so without the hook the throttle path is untestable on CI. Off unless set.
static long cn1SimulateMemoryWarningMs = 0;
static void* cn1SimulateMemoryWarningMain(void* arg) {
    (void)arg;
    for(;;) {
        lowMemoryMode = JAVA_TRUE;
        usleep((JAVA_INT)(cn1SimulateMemoryWarningMs * 1000));
    }
    return 0;
}
static void cn1StartSimulatedMemoryWarnings(void) {
    const char* e = getenv("CN1_SIMULATE_MEMORY_WARNING_MS");
    if(e == 0) {
        return;
    }
    cn1SimulateMemoryWarningMs = atol(e);
    if(cn1SimulateMemoryWarningMs <= 0) {
        return;
    }
    pthread_t tid;
    if(pthread_create(&tid, 0, cn1SimulateMemoryWarningMain, 0) == 0) {
        pthread_detach(tid);
        fprintf(stderr, "[LOWMEM] simulating a memory warning every %ld ms\n",
                cn1SimulateMemoryWarningMs);
    }
}

// Pacing-park accounting, reported by CN1_LOG_PACING_PARKS at exit and asserted on by
// ProcessBudgetPacingIntegrationTest. Peak footprint alone is a poor regression signal
// -- whether an unpaced mutator actually outruns the collector depends on how loaded
// the machine is, and the same binary was measured peaking anywhere from 114MB to
// 562MB on an idle laptop -- whereas "did backpressure engage, and on which path" is a
// property of the code under test rather than of the runner.
static _Atomic long cn1PacingParksBibop = 0;
static _Atomic long cn1PacingParksLegacy = 0;
// Smallest cap any thread computed this run. Park COUNTS alone cannot tell budget-derived
// sizing from the old host-wide sizing -- a 768MB churn parks against the 72MB static cap
// too -- but the cap VALUE can, because that 72MB is a hard floor off the budget path:
// bibopGcTriggerBytes is clamped to never fall below CN1_BIBOP_GC_TRIGGER_BYTES in either
// direction, so base = trigger * CN1_BIBOP_GC_HARD_CAP_MULTIPLIER is always at least 72MB
// and every unbounded branch takes the larger of that and a fraction of host RAM. Only the
// process-budget clamp can produce less. LONG_MAX until something computes a cap.
static _Atomic long cn1PacingMinCap = 0x7fffffffffffffffLL;
// Bounded-path telemetry. boundedChecks counts how often admission was decided against
// a real process budget, which is what tells a budgeted run apart from an unbudgeted one
// -- a park count cannot, since the unbudgeted path parks too. minHeadroom is the least
// remaining budget ever observed; -1 means the bounded path never ran.
static _Atomic long cn1PacingBoundedChecks = 0;
static _Atomic long cn1PacingMinHeadroom = -1;
static _Atomic int cn1PacingTrace = -1;
static int cn1PacingTraceOn(void) {
    int on = atomic_load_explicit(&cn1PacingTrace, memory_order_relaxed);
    if(on < 0) {
        on = getenv("CN1_LOG_PACING_PARKS") ? 1 : 0;
        atomic_store_explicit(&cn1PacingTrace, on, memory_order_relaxed);
    }
    return on;
}

static void cn1ReportPacingParks(void) {
    if(!cn1PacingTraceOn()) {
        return;
    }
    long minCap = atomic_load_explicit(&cn1PacingMinCap, memory_order_relaxed);
    long minHead = atomic_load_explicit(&cn1PacingMinHeadroom, memory_order_relaxed);
    fprintf(stderr, "[PACING] bibopParks=%ld legacyParks=%ld minCapKb=%ld"
                    " boundedChecks=%ld minHeadroomKb=%ld\n",
            atomic_load_explicit(&cn1PacingParksBibop, memory_order_relaxed),
            atomic_load_explicit(&cn1PacingParksLegacy, memory_order_relaxed),
            minCap == 0x7fffffffffffffffLL ? -1L : minCap / 1024,
            atomic_load_explicit(&cn1PacingBoundedChecks, memory_order_relaxed),
            minHead < 0 ? -1L : minHead / 1024);
}

// Mark-worklist overflow accounting, reported by CN1_LOG_GC_OVERFLOW at exit and
// asserted on by GcOverflowSpiralIntegrationTest. Overflow is a correctness backstop that is
// meant to be rare: recovering from it costs a full O(heap) rescan, so a workload that
// overflows EVERY cycle has a collector several times slower than the one it is supposed
// to have -- the runaway of issue #5537. The cycle count is the direct signal for that,
// and unlike a footprint reading it is a property of the code rather than of how much
// RAM the machine running it happened to have free.
static _Atomic long cn1GcOverflowCycles = 0;
static _Atomic long cn1GcGraceDrains = 0;
// Calls to the FULL gcMarkDrain, which rescans allObjectsInHeap from index 0 every time.
// A cycle makes a fixed handful of them by construction (roots, each grace pass, the
// belt, the SATB fixpoint), so this figure tracks the cycle count and nothing else.
// Cheap enough to leave in: one relaxed increment on a path that already walks the whole
// heap.
static _Atomic long cn1GcFullDrains = 0;
// Of those, the ones made while a GRACE PASS is running. Exactly one per pass -- the full
// drain that ends it -- and that is the point: the passes ALSO drain periodically to keep
// the worklist from overflowing, and those drains must be worklist-only. Pointing them at
// gcMarkDrain instead makes the cost of a pass quadratic in the heap; measured as a Mac
// Catalyst screenshot suite that never finished a single collection, with the EDT stacked
// up in the pacing park behind it. A count that tracks the number of periodic drains
// rather than the number of passes is that regression, and it is a property of the code
// rather than a timing, so it fails the same way on any machine.
static _Atomic long cn1GcGraceFullDrains = 0;
// Set only while a grace pass is running, on the GC thread that runs it.
static __thread int cn1GcInGracePass = 0;
// Set for the current cycle when a rebuild was needed and did NOT complete, so the
// index is missing pages that have been registered for an unbounded number of cycles.
// A miss makes cn1ConservativeResolve reject every reference into such a page and
// gcMarkObject's guard skip the object, so the mark is UNSOUND and the sweep must not
// act on it -- see codenameOneGCSweep. Distinct from the ordinary case of a page
// registered after the snapshot was taken, which is missing for exactly one cycle and
// whose objects are mark == -1 and covered by the sweep's grace rule; the next cycle
// rebuilds and includes them. It is the REPEAT that is fatal: on the second miss those
// objects are no longer fresh, still do not resolve, and age into the sweep's
// m < V - 1 reclamation while a live field still points at them.
static JAVA_BOOLEAN cn1GcPageIndexStale = JAVA_FALSE;
// Page-heap bytes allocated across the whole run, charged cycle by cycle. Divided by
// the cycle count it says how far the mutator ran ahead of the collector, which is what
// "the collector is keeping up" means as a number: a healthy run allocates about one
// collection trigger per cycle, and a collector that cannot keep up simply coalesces the
// crossings it missed into the one cycle it did manage. That ratio is a property of the
// two speeds rather than of either, so it reads the same on a loaded machine, unlike a
// peak footprint. Tracer-gated, like the counters above.
static _Atomic long long cn1GcAllocatedTotal = 0;
static _Atomic int cn1GcOverflowTrace = -1;
static int cn1GcOverflowTraceOn(void) {
    int on = atomic_load_explicit(&cn1GcOverflowTrace, memory_order_relaxed);
    if(on < 0) {
        on = getenv("CN1_LOG_GC_OVERFLOW") ? 1 : 0;
        atomic_store_explicit(&cn1GcOverflowTrace, on, memory_order_relaxed);
    }
    return on;
}

static void cn1ReportGcOverflow(void) {
    if(!cn1GcOverflowTraceOn()) {
        return;
    }
#ifdef CN1_DISABLE_BIBOP
    long triggerKb = 0;   // no page heap in this configuration, so no page-heap trigger
#else
    long triggerKb = (long)(atomic_load_explicit(&bibopGcTriggerBytes,
                                                 memory_order_relaxed) / 1024);
#endif
    fprintf(stderr, "[GC-OVERFLOW] overflowCycles=%ld graceDrains=%ld fullDrains=%ld"
                    " graceFullDrains=%ld cycles=%d allocatedKb=%lld triggerKb=%ld\n",
            atomic_load_explicit(&cn1GcOverflowCycles, memory_order_relaxed),
            atomic_load_explicit(&cn1GcGraceDrains, memory_order_relaxed),
            atomic_load_explicit(&cn1GcFullDrains, memory_order_relaxed),
            atomic_load_explicit(&cn1GcGraceFullDrains, memory_order_relaxed),
            currentGcMarkValue,
            atomic_load_explicit(&cn1GcAllocatedTotal, memory_order_relaxed) / 1024,
            triggerKb);
}

static void cn1ReportLowMemoryParks(void) {
    if(!cn1LowMemoryTraceOn()) {
        return;
    }
    fprintf(stderr, "[LOWMEM] parks=%ld throttledAllocations=%ld\n",
            atomic_load_explicit(&cn1LowMemoryParks, memory_order_relaxed),
            atomic_load_explicit(&cn1LowMemoryThrottledAllocations, memory_order_relaxed));
}

// Initializes the GC thresholds based on the free memory on the device.
// This is run inside the gc mark method.
// Previously we had been hardcoding this stuff, but that causes us to miss out
// on the greater capacity of newer devices.
static void init_gc_thresholds() {
    if (!GC_THRESHOLDS_INITIALIZED) {
        GC_THRESHOLDS_INITIALIZED = JAVA_TRUE;
        
        // On iPhone X, this generally starts with a figure like 388317184 (i.e. ~380 MB)
        long freemem = get_free_memory();
        
        // com.codename1.ui.Container is approx 900 bytes
        // Most allocations are 32 bytes though... so we're making an estimate
        // of the average size of an allocation.  This is based on experimentation and it is crude
        // This is used for trying to estimate how many allocations can be made on a thread
        // before we need to worry.
        long avgAllocSize = 128;
        
        // Estimate the number of allocation slots available in all of memory
        // On iPhone X, this will generally give around 38000 allocation slots
        long maxAllocationSlots = freemem / avgAllocSize;
        
        
        // Set the number of allocations allowed on a thread before it is considered
        // an aggressive allocator.  Aggressive allocator status will cause
        // the thread to lock until the sweep is complete, whereas other threads
        // are only locked during the mark() method.
        // The EDT is treated specially here
        CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD = maxAllocationSlots / 3;
        if (CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD < 5000) {
            CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD = 5000;
        }
        
        // For the EDT, experimenting with never declaring it aggressive (we don't want to block it)
        // unless we've received a low memory warning
        CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD_EDT = maxAllocationSlots * 10;
        
        // Set the high frequency allocation threshold.  If the app has allocated more
        // than the given threshold (in bytes) between GC cycles, it will issue an additional
        // GC cycle immediately after the last one (200ms) (sort of like doubling up.
        // Kind of picking numbers out of the air here.  one fifth of free memory
        // seems alright.
        //CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD = freemem / 5;
        //if (CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD < 1024 * 1024) {
        //    CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD = 1024 * 1024;
        //}
        
        // Set the threshold of total allocated memory before the high-frequency GC cycles
        // are started.
        //CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD = freemem/2;
        //if (CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD < 10 * 1024 * 1024) {
        //    CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD = 10 * 1024 * 1024;
        //}
        
        // GC will be triggered if the the number of allocations on any thread
        // reaches this threshold.  It is checked during malloc, so that
        // if we try to allocate and the number of allocations exceeds this threshold
        // then the thread is stopped until a GC cycle is completed.
        CN1_MAX_HEAP_SIZE = maxAllocationSlots / 3;
        if (CN1_MAX_HEAP_SIZE < 10000) {
            CN1_MAX_HEAP_SIZE = 10000;
        }

        // This might be a bit permissive (allowing the EDT to grow) to the total 
        // max allocation slots - but there are other safeguards in place that should
        // mitigate the harm done.
        CN1_MAX_HEAP_SIZE_EDT = maxAllocationSlots;
        if (CN1_MAX_HEAP_SIZE_EDT < 10000) {
            CN1_MAX_HEAP_SIZE_EDT = 10000;
        }
    }
}

//#define DEBUG_GC_OBJECTS_IN_HEAP

struct clazz class_array1__JAVA_BOOLEAN = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 1, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BOOLEAN = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 2, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BOOLEAN = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_BOOLEAN, "boolean[]", JAVA_TRUE, 3, &class__java_lang_Boolean, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_CHAR = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_CHAR, "char[]", JAVA_TRUE, 1, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_CHAR = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_CHAR, "char[]", JAVA_TRUE, 2, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_CHAR = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_CHAR, "char[]", JAVA_TRUE, 3, &class__java_lang_Character, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_BYTE = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 1, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_BYTE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 2, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_BYTE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_BYTE, "byte[]", JAVA_TRUE, 3, &class__java_lang_Byte, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_SHORT = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_SHORT, "short[]", JAVA_TRUE, 1, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_SHORT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_SHORT, "short[]", JAVA_TRUE, 2, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_SHORT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_SHORT, "short[]", JAVA_TRUE, 3, &class__java_lang_Short, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_INT = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_INT, "int[]", JAVA_TRUE, 1, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_INT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_INT, "int[]", JAVA_TRUE, 2, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_INT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_INT, "int[]", JAVA_TRUE, 3, &class__java_lang_Integer, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_LONG = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_LONG, "long[]", JAVA_TRUE, 1, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_LONG = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_LONG, "long[]", JAVA_TRUE, 2, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_LONG = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_LONG, "long[]", JAVA_TRUE, 3, &class__java_lang_Long, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_FLOAT = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 1, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_FLOAT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 2, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_FLOAT = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_FLOAT, "float[]", JAVA_TRUE, 3, &class__java_lang_Float, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array1__JAVA_DOUBLE = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_1_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 1, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array2__JAVA_DOUBLE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_2_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 2, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};

struct clazz class_array3__JAVA_DOUBLE = {
   DEBUG_GC_INIT 0, 0, 0, 0, 0, &gcMarkArrayObject, 0, cn1_array_3_id_JAVA_DOUBLE, "double[]", JAVA_TRUE, 3, &class__java_lang_Double, JAVA_TRUE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};


void popMany(CODENAME_ONE_THREAD_STATE, int count, struct elementStruct** SP) {
    while(count > 0) {
        --(*SP);
        javaTypes t = (*SP)->type;
        if(t == CN1_TYPE_DOUBLE || t == CN1_TYPE_LONG) {
            count -= 2;
        } else {
            count--;
        }
    }
}


JAVA_OBJECT* constantPoolObjects = 0;

struct elementStruct* BC_DUP2_X2_DD(struct elementStruct* SP) {
    (*SP).data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = (*SP).data.l;
    (*SP).type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = (*SP).type;
    return (struct elementStruct*)(SP+1);
}
struct elementStruct* BC_DUP2_X2_DSS(struct elementStruct* SP) {
    SP[0].data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = SP[-3].data.l;
    SP[-3].data.l = SP[0].data.l;
    SP[0].type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = SP[-3].type;
    SP[-3].type = SP[0].type;
    return SP+1;
}
struct elementStruct* BC_DUP2_X2_SSD(struct elementStruct* SP) {
    SP[1].data.l = SP[-1].data.l;
    SP[0].data.l = SP[-2].data.l;
    SP[-1].data.l = SP[-3].data.l;
    SP[-2].data.l = SP[1].data.l;
    SP[-3].data.l = SP[0].data.l;
    SP[1].type = SP[-1].type;
    SP[0].type = SP[-2].type;
    SP[-1].type = SP[-3].type;
    SP[-2].type = SP[1].type;
    SP[-3].type = SP[0].type;
    return SP+2;
}
struct elementStruct* BC_DUP2_X2_SSSS(struct elementStruct* SP) {
    SP[1].data.l = SP[-1].data.l;
    SP[0].data.l = SP[-2].data.l;
    SP[-1].data.l = SP[-3].data.l;
    SP[-2].data.l = SP[-4].data.l;
    SP[-3].data.l = SP[1].data.l;
    SP[-4].data.l = SP[0].data.l;
    SP[1].type = SP[-1].type;
    SP[0].type = SP[-2].type;
    SP[-1].type = SP[-3].type;
    SP[-2].type = SP[-4].type;
    SP[-3].type = SP[1].type;
    SP[-4].type = SP[0].type;
    return SP+2;
}

struct elementStruct* BC_DUP_X2_SD(struct elementStruct* SP) {
    SP[0].data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = SP[0].data.l;
    SP[0].type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = SP[0].type;
    return SP+1;
}

struct elementStruct* BC_DUP_X2_SSS(struct elementStruct* SP) {
    SP[0].data.l = SP[-1].data.l;
    SP[-1].data.l = SP[-2].data.l;
    SP[-2].data.l = SP[-3].data.l;
    SP[-3].data.l = SP[0].data.l;
    SP[0].type = SP[-1].type;
    SP[-1].type = SP[-2].type;
    SP[-2].type = SP[-3].type;
    SP[-3].type = SP[0].type;
    return SP+1;
}


int instanceofFunction(int sourceClass, int destId) {
    if(sourceClass == destId) {
        return JAVA_TRUE;
    }
    if (sourceClass == cn1_array_1_id_JAVA_INT && destId == cn1_class_id_java_lang_Object) {
        int foo = 1;
    }
    if (destId == cn1_array_1_id_JAVA_INT && sourceClass == cn1_class_id_java_lang_Object) {
        int foo = 1;
    }
    if(sourceClass >= cn1_array_start_offset || destId >= cn1_array_start_offset) {
        
        // (destId instanceof sourceClass)
        // E.g. (new int[0] instanceof Object) ===> sourceClass==Object and destId=int[]
        
        if (sourceClass < cn1_array_start_offset) {
            return sourceClass == cn1_class_id_java_lang_Object;
        }  else if (destId < cn1_array_start_offset) {
            return JAVA_FALSE;
        }
        
        // At this point we know that both sourceClass and destId are array types
        
        // The start offset for reference array types
        int refArrayStartOffset = cn1_array_start_offset+100;
        if (sourceClass < refArrayStartOffset || destId < refArrayStartOffset) {
            if (sourceClass >= refArrayStartOffset) {
                // We need to deal with things like (int[][] instanceof Object[])
                int srcDim = (sourceClass - refArrayStartOffset)%3+1;
                int destDim = (destId - cn1_array_start_offset)%4;
                
                if (srcDim < destDim) {
                    if (srcDim > 1) {
                        sourceClass = sourceClass-1;
                    } else {
                        sourceClass =(sourceClass - refArrayStartOffset)/3;
                    }
                    return instanceofFunction(sourceClass, destId-1);
                }
            }
            // if either is primitive, then they must be the same type.
            return sourceClass == destId;
        }
        int srcDimension = (sourceClass - refArrayStartOffset)%3+1;
        int destDimension = (destId - refArrayStartOffset)%3+1;
        
        int sourceClassComponentTypeId = srcDimension > 1 ? sourceClass-1 : (sourceClass - refArrayStartOffset)/3;
        int destClassComponentTypeId = destDimension > 1 ? destId-1 : (destId - refArrayStartOffset)/3;
        return instanceofFunction(sourceClassComponentTypeId, destClassComponentTypeId);
    }
    
    int* i = classInstanceOf[destId];
    int counter = 0;
    while(i[counter] > -1) {
        if(i[counter] == sourceClass) {
            return JAVA_TRUE;
        }
        i++;
    }
    return JAVA_FALSE;
}



JAVA_OBJECT* releaseQueue = 0;
JAVA_INT releaseQueueSize = 0;
typedef void (*finalizerFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj);

// invokes finalizers and iterates over the release queue
void flushReleaseQueue() {
}

// java.lang.String has NO Java finalizer (it would tax every string-bearing
// page with per-slot reclaim walks on every platform). Its cached NSString
// peer is released here instead -- a cost only ObjC targets pay, and (for
// BiBOP pages) only on pages flagged by cn1BibopNoteNativePeer at cache time.
#if defined(__APPLE__) && defined(__OBJC__)
extern struct clazz class__java_lang_String;
static inline void cn1ReleaseStringPeer(JAVA_OBJECT o) {
    if(o->__codenameOneParentClsReference == &class__java_lang_String) {
        struct obj__java_lang_String* s = (struct obj__java_lang_String*)o;
        if(s->java_lang_String_nsString != 0) {
            void* v = (void*)s->java_lang_String_nsString;
            [(__bridge NSString*)v release];
            s->java_lang_String_nsString = 0;
        }
    }
}
#else
#define cn1ReleaseStringPeer(o) do {} while(0)
#endif

void freeAndFinalize(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    finalizerFunctionPointer ptr = (finalizerFunctionPointer)obj->__codenameOneParentClsReference->finalizerFunction;
    if(ptr != 0) {
        // Per the Java spec, an exception thrown by finalize() is IGNORED -- and it must
        // NEVER escape the collector. This runs during sweep on the GC thread; if a
        // finalizer throws (observed on iOS: a native peer finalizer during DrawGradientStops)
        // and the exception is allowed to unwind, it propagates out through codenameOneGCSweep
        // -> java_lang_System_gcMarkSweep__ (whose gcCurrentlyRunning=FALSE reset is then
        // SKIPPED, leaving the flag stuck TRUE) -> the GC thread's run loop, which only catches
        // InterruptedException -> the GC thread dies WITHOUT clearing gcThreadInstance. The EDT
        // then deadlocks forever in cn1BibopMaybeGc's allocation backpressure spin (it can never
        // trigger a GC because gcCurrentlyRunning is stuck true, and spins because the dead GC
        // thread's instance is still non-null) -> deterministic mid-suite hang. Run the finalizer
        // inside a catch-all try block and swallow anything it throws.
        int __savedTryBlock = threadStateData->tryBlockOffset;
        jmp_buf __finTryJmp;
        if(CN1_TRY_SETJMP(__finTryJmp) == 0) {
            threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0;
            threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = 0; // catch-all
            memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, __finTryJmp, sizeof(jmp_buf));
            threadStateData->tryBlockOffset++;
            ptr(threadStateData, obj);
            threadStateData->tryBlockOffset = __savedTryBlock;
        } else {
            // finalizer threw -> restore the try-block stack and drop the exception
            threadStateData->tryBlockOffset = __savedTryBlock;
            threadStateData->exception = JAVA_NULL;
        }
    }
    cn1ReleaseStringPeer(obj);
    codenameOneGcFree(threadStateData, obj);
}

/**
 * Invoked to destroy an array and release all the objects within it
 */
void arrayFinalizerFunction(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
}

BOOL invokedGC = NO;
extern int findPointerPosInHeap(JAVA_OBJECT obj);
extern pthread_mutex_t* getMemoryAccessMutex();
extern long gcThreadId;

void gcReleaseObj(JAVA_OBJECT o) {
}

// Lazily computes the per-thread native C-stack low-water mark consulted by
// CN1_FRAMELESS_SOE_GUARD. On Apple/macOS/iOS pthread exposes the stack base and
// size directly; elsewhere we fall back to anchoring off the current frame with a
// generous (8MB) assumed stack. The guard band is subtracted so there is room to
// build + throw the StackOverflowError once the limit is crossed.
void cn1ComputeNativeStackLimit(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) || defined(__MACH__)
    void* stackBase = pthread_get_stackaddr_np(pthread_self());
    size_t stackSize = pthread_get_stacksize_np(pthread_self());
    threadStateData->nativeStackLimit = (JAVA_LONG)(intptr_t)stackBase
            - (JAVA_LONG)stackSize
            + (JAVA_LONG)CN1_FRAMELESS_STACK_GUARD_BAND;
#else
    // Portable fallback: pthread stack introspection is unavailable, so anchor off
    // the current frame and assume an 8MB stack below it.
    threadStateData->nativeStackLimit = (JAVA_LONG)(intptr_t)__builtin_frame_address(0)
            - (JAVA_LONG)(8L * 1024L * 1024L)
            + (JAVA_LONG)CN1_FRAMELESS_STACK_GUARD_BAND;
#endif
    // Guard against a degenerate (0) result, which would re-trigger computation
    // every call; if introspection yielded nothing usable, disable the limit.
    if (threadStateData->nativeStackLimit == 0) {
        threadStateData->nativeStackLimit = 1;
    }
}

// memory map of all the heap objects which we can walk over to delete/deallocate
// unused objects
const char* volatile cn1LastNamSetter = 0;
JAVA_OBJECT* allObjectsInHeap = 0;
int sizeOfAllObjectsInHeap = 30000;
int currentSizeOfAllObjectsInHeap = 0;

// SINGLE-WRITER INVARIANT for allObjectsInHeap: the table is grown and walked
// ONLY on the GC thread (mark migration, the dead-thread drain below, sweep,
// root-snapshot build, overflow rescan -- all phases of the same thread, so
// they are mutually sequential). The only non-GC-thread accesses are one-shot
// slot writes under the critical section (getStack's immortal-string removal).
// Dying threads therefore must NOT call placeObjectInHeapCollection (a growth's
// realloc-and-free would race an in-flight GC-thread walk -- observed shape:
// a sweep's slot-NULL lost in the memcpy'd copy resurrects a freed pointer, or
// two growths during one hoisted-pointer walk free the array under the reader).
// Instead markDeadThread queues the dying thread's TLD here (critical section
// held) and the GC thread drains it at the start of the next mark -- strictly
// before that cycle's sweep, so the migration always precedes any possible
// finalization of the Thread object. Objects still in a queued TLD's pending
// list are invisible to the sweep (only table entries are swept), so the
// deferral can never free them early.
static struct ThreadLocalData* cn1DeadPendingThreads = 0;  // guarded by criticalSection
extern void cn1ReleaseThreadLocalData(struct ThreadLocalData* head);

// ---- Immortal roots ------------------------------------------------------
// VM-internal objects referenced ONLY from C globals (e.g. getStack's cached
// separator strings) are invisible to every root source; the old trick of
// removeObjectFromHeapCollection'ing them only worked while such objects lived
// in the legacy table -- for BiBOP-resident objects (all small objects now,
// arrays too) removal is a no-op and the object WOULD be swept while the C
// global still points at it. Register them here instead; the mark phase treats
// the array as a root set every cycle. Tiny and append-only.
static JAVA_OBJECT* cn1ImmortalRoots = 0;
static int cn1ImmortalRootsN = 0, cn1ImmortalRootsCap = 0;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
void cn1GcRegisterImmortalObj(JAVA_OBJECT o); // defined near the clazz registry below
#endif
void cn1AddImmortalRoot(JAVA_OBJECT o) {
    if(o == JAVA_NULL) return;
    lockCriticalSection();
    if(cn1ImmortalRootsN == cn1ImmortalRootsCap) {
        cn1ImmortalRootsCap = cn1ImmortalRootsCap ? cn1ImmortalRootsCap * 2 : 64;
        cn1ImmortalRoots = (JAVA_OBJECT*)realloc(cn1ImmortalRoots, cn1ImmortalRootsCap * sizeof(JAVA_OBJECT));
    }
    cn1ImmortalRoots[cn1ImmortalRootsN++] = o;
    unlockCriticalSection();
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // Recognizable to the mark guard even though most immortal roots are BiBOP
    // slots (resolvable anyway) -- covers any legacy/off-heap registrant too.
    cn1GcRegisterImmortalObj(o);
#endif
}
pthread_mutex_t* memoryAccessMutex = NULL;

pthread_mutex_t* getMemoryAccessMutex() {
    if(memoryAccessMutex == NULL) {
        memoryAccessMutex = malloc(sizeof(pthread_mutex_t));
        pthread_mutex_init(memoryAccessMutex, NULL);
    }
    return memoryAccessMutex;
}

int findPointerPosInHeap(JAVA_OBJECT obj) {
    // Tagged Integers are immediate values, not heap objects, and therefore have
    // no header/heap position to read.  Keep this low-level helper total so a
    // caller can never turn a legal boxed Integer into a tagged-pointer fault.
    if(obj == 0 || CN1_IS_TAGGED(obj)) {
        return -1;
    }
    return obj->__heapPosition;
}

// this is an optimization allowing us to continue searching for available space in RAM from the previous position
// that way we avoid looping over elements that we already probably checked
int lastOffsetInRam = 0;
void placeObjectInHeapCollection(JAVA_OBJECT obj) {
    if(allObjectsInHeap == 0) {
        allObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
        memset(allObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
    }
    if(currentSizeOfAllObjectsInHeap < sizeOfAllObjectsInHeap) {
        allObjectsInHeap[currentSizeOfAllObjectsInHeap] = obj;
        obj->__heapPosition = currentSizeOfAllObjectsInHeap;
        currentSizeOfAllObjectsInHeap++;
    } else {
        int pos = -1;
        JAVA_OBJECT* currentAllObjectsInHeap = allObjectsInHeap;
        int currentSize = currentSizeOfAllObjectsInHeap;
        for(int iter = lastOffsetInRam ; iter < currentSize ; iter++) {
            if(currentAllObjectsInHeap[iter] == JAVA_NULL) {
                pos = iter;
                lastOffsetInRam = pos;
                break;
            }
        }
        if(pos < 0 && lastOffsetInRam > 0) {
            // just make sure there is nothing at the start
            for(int iter = 0 ; iter < lastOffsetInRam ; iter++) {
                if(currentAllObjectsInHeap[iter] == JAVA_NULL) {
                    pos = iter;
                    lastOffsetInRam = pos;
                    break;
                }
            }
        }
        if(pos < 0) {
            // we need to enlarge the block
            JAVA_OBJECT* tmpAllObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap * 2);
            memset(tmpAllObjectsInHeap + sizeOfAllObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
            memcpy(tmpAllObjectsInHeap, allObjectsInHeap, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
            sizeOfAllObjectsInHeap *= 2;
            // Immediate free is safe under the SINGLE-WRITER INVARIANT (see the
            // cn1DeadPendingThreads comment): growth only ever runs on the GC
            // thread, whose own walks are sequential with it, and every non-GC-
            // thread access holds the critical section that the growth callers
            // (mark migration / dead-thread drain) also hold. The old one-growth
            // deferral could still double-free under two growths in one walk.
            JAVA_OBJECT* replaced = allObjectsInHeap;
            allObjectsInHeap = tmpAllObjectsInHeap;
            free(replaced);
            // record the real slot -- leaving pos at -1 here left the object's
            // __heapPosition unset, so a later reference-counted free could not null
            // its slot and the sweep would dereference the dangling pointer.
            pos = currentSizeOfAllObjectsInHeap;
            allObjectsInHeap[pos] = obj;
            currentSizeOfAllObjectsInHeap++;
        } else {
            allObjectsInHeap[pos] = obj;
        }
        obj->__heapPosition = pos;
    }
}

extern struct ThreadLocalData** allThreads;
extern int nThreadsToKill;

// Graduate a surviving BiBOP object into the legacy mark/sweep (poor-man's generational
// promotion). NON-MOVING: the object's memory stays in its BiBOP slot; we only register
// it in allObjectsInHeap (so the unconditional legacy rescan traces it -- always complete,
// unlike the overflow-gated BiBOP rescan) and flag its slot CN1_BIBOP_ADOPTED so the BiBOP
// page sweep skips it (one owner, no double-clearing). Runs on the GC thread during the
// mark; mutators never touch allObjectsInHeap directly and nothing moves, so no reference
// can dangle. placeObjectInHeapCollection sets heapPosition to the array index; we override
// it to the -4 sentinel (the legacy sweep finds the object by walk-index, not heapPosition).
#ifndef CN1_DISABLE_BIBOP
// Objects matured during a mark are buffered here and registered into allObjectsInHeap
// AFTER the mark completes (cn1DrainAdoptBuffer). placeObjectInHeapCollection reallocs +
// frees that table, which is UNSAFE during the mark: the drain walks the same table, and
// under parallel markers several threads would grow it at once. So maturation only FLAGS
// the object (-4) during the mark and defers the table mutation to a single-threaded,
// locked, post-mark pass.
static pthread_mutex_t gcAdoptMutex = PTHREAD_MUTEX_INITIALIZER;
static JAVA_OBJECT* gcAdoptStack = 0;
static long gcAdoptTop = 0, gcAdoptCap = 0;
#endif

static void cn1MatureObject(JAVA_OBJECT obj) {
#ifndef CN1_DISABLE_BIBOP
    // Claim the object for adoption exactly ONCE with a CAS -3 -> -4. Under parallel
    // markers two threads can both reach the same object; the CAS loser must not
    // double-buffer/double-register. The -4 flag takes effect immediately so the cascade,
    // the mark-stamp and the sweep-skip all see it during THIS mark.
    int expected = CN1_BIBOP_HEAP_POS;
    if(!__atomic_compare_exchange_n(&obj->__heapPosition, &expected, CN1_BIBOP_ADOPTED,
                                    0, __ATOMIC_RELAXED, __ATOMIC_RELAXED)) {
        return;
    }
    // Sticky-flag the host page so its slots always take the full per-slot sweep walk
    // (which skips live -4 slots) instead of the O(1) page reset, which would recycle this
    // still-live object's memory out from under the legacy collector.
    ((CN1BibopPage*)(((uintptr_t)obj) & ~((uintptr_t)CN1_BIBOP_PAGE_SIZE - 1)))->gcHasAdopted = JAVA_TRUE;
    // Buffer for post-mark registration (NOT placeObjectInHeapCollection here -- see above).
    pthread_mutex_lock(&gcAdoptMutex);
    if(gcAdoptTop >= gcAdoptCap) {
        long ncap = gcAdoptCap ? gcAdoptCap * 2 : 4096;
        JAVA_OBJECT* n = (JAVA_OBJECT*)realloc(gcAdoptStack, (size_t)ncap * sizeof(JAVA_OBJECT));
        if(n == 0) { pthread_mutex_unlock(&gcAdoptMutex); return; } // OOM: leave it flagged -4, unregistered (alive; retried next cycle it survives)
        gcAdoptStack = n; gcAdoptCap = ncap;
    }
    gcAdoptStack[gcAdoptTop++] = obj;
    pthread_mutex_unlock(&gcAdoptMutex);
#endif
}

#ifndef CN1_DISABLE_BIBOP
// Register every object matured during the just-finished mark into allObjectsInHeap.
// Runs on the GC thread AFTER the parallel mark has joined (single-threaded) and holds the
// critical section -- the invariant placeObjectInHeapCollection's grow-and-free relies on.
// Called before the sweep, so the legacy sweep sees these -4 objects (marked live) and
// keeps them; from next cycle the complete legacy rescan traces them.
static void cn1DrainAdoptBuffer() {
    if(gcAdoptTop == 0) {
        return;
    }
    lockCriticalSection();
    for(long i = 0 ; i < gcAdoptTop ; i++) {
        JAVA_OBJECT o = gcAdoptStack[i];
        placeObjectInHeapCollection(o);      // sets heapPosition to the array index...
        o->__heapPosition = CN1_BIBOP_ADOPTED; // ...restore the -4 sentinel (found by walk-index)
    }
    gcAdoptTop = 0;
    unlockCriticalSection();
}
#endif

JAVA_BOOLEAN hasAgressiveAllocator;

#ifndef CN1_DISABLE_BIBOP
extern void cn1BibopRetireThreadPages();
#endif

// the thread just died, mark its remaining resources
void collectThreadResources(struct ThreadLocalData *current)
{
#ifndef CN1_DISABLE_BIBOP
    // Retire this (dying) thread's current BiBOP pages so their slots become
    // collectable. Runs on the dying thread, so its __thread current pages are
    // reachable here.
    cn1BibopRetireThreadPages();
    // LEVER A: flush any unaccounted per-thread bytes into the global GC trigger.
    CN1_BIBOP_FLUSH_BYTES(current);
    // Release this thread's pacing claim. A claim is normally handed back at the
    // thread's NEXT allocation check, so a thread that is admitted and then exits would
    // never return it -- and unlike a thread that merely goes idle, there is nothing
    // left to hand it back later. Allocator-thread churn would accumulate phantom
    // reservations until admission could never succeed and every allocator paced its
    // full budget on every check. Runs on the dying thread, so the __thread claim is
    // reachable here, same as the pages and bytes above.
    cn1PacingReleaseThreadClaim();
#endif
    if(current->utf8Buffer != 0) {
        free(current->utf8Buffer);
        current->utf8Buffer = 0;
    }
    // SINGLE-WRITER allObjectsInHeap: do NOT migrate pendingHeapAllocations here
    // -- this runs on the DYING thread, and a table growth here races the GC
    // thread's lock-free sweep/snapshot walks (see cn1DeadPendingThreads above).
    // Queue the TLD instead; the GC drains it at the start of the next mark.
    // Caller (markDeadThread) holds the critical section that guards the list.
    current->gcQueuedForDrain = JAVA_TRUE;
    current->gcDeadNext = cn1DeadPendingThreads;
    cn1DeadPendingThreads = current;
}

// Drain the dead-thread queue on the GC thread at mark start: migrate each queued
// TLD's pending allocations into allObjectsInHeap (the only place besides the
// live-thread mark migration where the table may grow) and perform any TLD free
// that the Thread finalizer requested while the TLD was still queued.
static void cn1DrainDeadThreadPending() {
    lockCriticalSection();
    struct ThreadLocalData* head = cn1DeadPendingThreads;
    cn1DeadPendingThreads = 0;
    while(head != 0) {
        struct ThreadLocalData* next = head->gcDeadNext;
        for(int heapTrav = 0 ; heapTrav < head->heapAllocationSize ; heapTrav++) {
            JAVA_OBJECT obj = (JAVA_OBJECT)head->pendingHeapAllocations[heapTrav];
            if(obj) {
                head->pendingHeapAllocations[heapTrav] = 0;
                placeObjectInHeapCollection(obj);
            }
        }
        head->heapAllocationSize = 0;
        head->gcDeadNext = 0;
        head->gcQueuedForDrain = JAVA_FALSE;
        if(head->gcReleaseRequested) {
            // the Thread object was finalized while this TLD awaited the drain
            cn1ReleaseThreadLocalData(head);
        }
        head = next;
    }
    unlockCriticalSection();
}
static void gcMarkDrain(CODENAME_ONE_THREAD_STATE);
// Worklist-only drain (no heap rescan) -- see its definition for why the two are
// separate functions and which callers may use which.
static void gcMarkDrainWorklist(CODENAME_ONE_THREAD_STATE);
// Parallel variant of gcMarkDrain: fans the transitive mark-drain out across a small
// pool of worker threads. Falls back to the serial gcMarkDrain when only one marker
// is configured. Defined further down (after gcMarkDrain). See the big comment block
// at the worklist declarations for the design and the invariants it preserves.
static void gcMarkDrainParallel(CODENAME_ONE_THREAD_STATE);

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// PHASE 3b forward declarations (definitions live after the BiBOP block because the
// resolver reuses the BiBOP page structures). These make the conservative native-stack
// scan a REAL root source for object-bearing FRAMELESS frames. See the big block below.
static void cn1GcScanThreadNativeStack(CODENAME_ONE_THREAD_STATE, struct ThreadLocalData* t);
static void cn1GcScanOwnStack(CODENAME_ONE_THREAD_STATE);
static void cn1GcSignalStopThreads(struct ThreadLocalData* self);
static void cn1GcSignalReleaseThreads(struct ThreadLocalData* self);
void cn1GcBuildRootSnapshots(void);
JAVA_OBJECT cn1ConservativeResolve(void* w);
#ifdef CN1_CONSERVATIVE_GC_SELFCHECK
// Transient ⊇ self-check (NOT in the shipping path): asserts every precise object
// root on a paused thread's object stack is also resolved by the conservative scan.
static void cn1GcSelfCheckThreadStack(struct ThreadLocalData* t, int stackSize);
#endif
#endif

/**
 * A simple concurrent mark algorithm that traverses the currently running threads
 */
extern int recursionKey; // force-mark pass epoch (defined below, near gcMarkObject)

// ---- SATB (snapshot-at-the-beginning) deletion-barrier log -------------------
// gcSatbActive is set for the whole concurrent mark and read by CN1_SATB_DELETE on
// every heap object-reference store (all mutators, including native threads). The
// barrier pushes the OVERWRITTEN reference here; codenameOneGCMark drains it to a
// fixpoint before sweep, so a reference present at the start of the cycle is never
// lost to a concurrent move/null between a thread's scan and the end of mark.
volatile int gcSatbActive = 0;
static JAVA_OBJECT* gcSatbStack = 0;
static long gcSatbTop = 0;                 // guarded by gcSatbMutex
static long gcSatbCap = 0;
static pthread_mutex_t gcSatbMutex = PTHREAD_MUTEX_INITIALIZER;
// Monotonic count of objects transitioned unmarked->marked this process; the SATB
// drain snapshots it around a batch to detect "marked nothing new" (fixpoint).
long gcMarkNewObjectCount = 0;

// ---- Poor-man's generational adoption: mature surviving BiBOP subtrees into legacy ----
// Policy (compile-time A/B): TENURE(1) matures a reachable non-leaf BiBOP object once it
// has SURVIVED a prior cycle (its old mark was a positive prior-cycle value), plus a
// CASCADE so the whole reachable subtree matures together (never half a tree). ONMARK(2)
// matures every reachable non-leaf BiBOP object immediately. 0 disables adoption.
#ifndef CN1_ADOPT_POLICY
#define CN1_ADOPT_POLICY 1
#endif
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
// Set by gcMarkDrain while running a MATURED object's mark function, so the children it
// marks are matured too -- this is the cascade that keeps the whole subtree in one system.
// THREAD-LOCAL: each parallel mark worker cascades within the subtree it is draining
// without racing the others on this flag.
static __thread JAVA_BOOLEAN gcCurrentlyMaturing = JAVA_FALSE;
#endif
// Forward (tentative) declarations -- the real definitions are below near the worklist.
// The belt pass in codenameOneGCMark forces the overflow flag to trigger the full BiBOP
// rescan, and the grace pass reads the cursor to drain before it can overflow (see the
// interleaved drain there). The size macro is hoisted with them for the same reason;
// its rationale stays at the worklist definition.
#ifndef CN1_GC_MARK_WORKLIST_SIZE
#define CN1_GC_MARK_WORKLIST_SIZE 65536
#endif
static JAVA_BOOLEAN gcMarkWorklistOverflow;
static int gcMarkWorklistTop;
static _Atomic JAVA_BOOLEAN gcMarkOverflowSeen = JAVA_FALSE;
#ifndef CN1_DISABLE_BIBOP
// Forward declarations -- defined below; the grace-subtree pass in codenameOneGCMark
// walks the page registry and its slots before their definitions.
static inline JAVA_OBJECT cn1BibopSlot(CN1BibopPage* p, int i);
static CN1BibopPage* _Atomic bibopAllPages;
#ifdef CN1_GRACE_AUDIT
static void cn1GraceAuditPreSweep(CODENAME_ONE_THREAD_STATE);
#endif
#endif
#ifdef CN1_GRACE_AUDIT
static void cn1GraceAuditLegacy(CODENAME_ONE_THREAD_STATE);
#endif
#ifdef CN1_GC_VERIFY
// QA heap verifier (defined next to cn1ConservativeResolve, which supplies the
// page/extent index it classifies against).
static int cn1GcVerifyActive;
void cn1GcVerifyHeap(CODENAME_ONE_THREAD_STATE);
void cn1GcVerifyChild(JAVA_OBJECT child, void* markSite);
void cn1GcVerifyPoisonSlot(JAVA_OBJECT o, int slotSize);
JAVA_BOOLEAN cn1GcVerifyQuarantineFree(JAVA_OBJECT obj);
#endif
#ifdef CN1_BIBOP_VALIDATE
// Belt diagnostic: while set, gcMarkObject logs the class of each newly-marked object
// (a reachable object the main drain missed) and its drain parent, to name the
// systematic drain-incompleteness pattern. Throttled by gcBeltDiagCount.
static int gcBeltDiagActive = 0;
static int gcBeltDiagCount = 0;
#endif

void cn1SatbEnqueue(JAVA_OBJECT old) {
    pthread_mutex_lock(&gcSatbMutex);
    if(gcSatbTop >= gcSatbCap) {
        long ncap = gcSatbCap ? gcSatbCap * 2 : 8192;
        JAVA_OBJECT* n = (JAVA_OBJECT*)realloc(gcSatbStack, (size_t)ncap * sizeof(JAVA_OBJECT));
        if(n == 0) { pthread_mutex_unlock(&gcSatbMutex); return; } // OOM: drop (rare; only re-opens the original race)
        gcSatbStack = n; gcSatbCap = ncap;
    }
    gcSatbStack[gcSatbTop++] = old;
    pthread_mutex_unlock(&gcSatbMutex);
}

// Atomically take the current SATB batch (swap the log empty) into *out (caller owns
// the returned buffer contents until the next take). Returns the count taken.
static long cn1SatbTake(JAVA_OBJECT** out) {
    pthread_mutex_lock(&gcSatbMutex);
    long n = gcSatbTop;
    static JAVA_OBJECT* scratch = 0; static long scratchCap = 0;
    if(n > scratchCap) {
        long nc = n < 8192 ? 8192 : n;
        scratch = (JAVA_OBJECT*)realloc(scratch, (size_t)nc * sizeof(JAVA_OBJECT));
        scratchCap = nc;
    }
    if(n > 0 && scratch != 0) memcpy(scratch, gcSatbStack, (size_t)n * sizeof(JAVA_OBJECT));
    gcSatbTop = 0;
    pthread_mutex_unlock(&gcSatbMutex);
    *out = scratch;
    return (scratch != 0) ? n : 0;
}

void cn1RefreshFreeMemCache(void);   // defined near cn1BibopMaybeGc; drives the dynamic pacing cap
#ifndef CN1_DISABLE_BIBOP
void cn1BibopBeginGcCycle(void);
#endif

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// Immortal object registry (defined near the clazz registry below): objects removed
// from the heap table must be recognizable to the mark guard, which otherwise skips
// unresolvable pointers as garbage. Forward-declared here for the sweep /
// removeObjectFromHeapCollection / cn1AddImmortalRoot sites above the definitions.
void cn1GcRegisterImmortalObj(JAVA_OBJECT o);
static int cn1GcImmortalObjContains(JAVA_OBJECT o);
static JAVA_BOOLEAN cn1SweepRemoving;
// Set ONLY around passes that iterate authoritative object registries
// (allObjectsInHeap, the BiBOP page registry). See the guard in gcMarkObject.
//
// PER THREAD, deliberately. Marking can run as a worker pool
// (gcMarkDrainParallel); a process-wide flag would let a CONCURRENT worker bypass
// the resolve guard on pointers that are not authoritative -- the precise case the
// guard exists for. Serial marking is the current default
// (gcMarkResolveThreadCount returns 1), so a global would happen to be safe today
// and would silently stop being safe the moment parallel marking is re-enabled,
// which the isolation comment there says is intended.
//
// Use CN1_GC_TRUSTED_BEGIN/END rather than assigning directly: they save and
// restore, so nesting and any future early return cannot leak a trusted window
// into unrelated marking.
static __thread int cn1GcTrustedRoots = 0;
#define CN1_GC_TRUSTED_BEGIN() int __cn1TrustSaved = cn1GcTrustedRoots; cn1GcTrustedRoots = 1
#define CN1_GC_TRUSTED_END()   cn1GcTrustedRoots = __cn1TrustSaved
// An untrusted HOLE inside a trusted walk, for a pass that has to drain the mark
// worklist part-way through its registry walk (the grace passes). BEGIN/END cannot
// express that: they save and restore a block-scoped local, so a pair inside the walk
// would shadow the walk's own saved value and would restore whatever the ENCLOSING
// window held rather than the "untrusted" a drain requires. Trust is a property of the
// POINTERS being read -- authoritative in a registry walk, arbitrary in a drain that
// follows child words out of mark functions -- so the two really are independent here.
#define CN1_GC_TRUSTED_SUSPEND() cn1GcTrustedRoots = 0
#define CN1_GC_TRUSTED_RESUME()  cn1GcTrustedRoots = 1
#endif

#ifdef CN1_GC_VERIFY
/*
 * The handshake GcVerifyApp waits on, in place of sleeping.
 *
 * Its hazard is an object allocated WHILE a mark is running and dropped before the mark ends:
 * that is the shape a missing grace pass loses. The app used to arrange it by calling
 * System.gc() and sleeping, which is a guess about how long a cycle takes -- and on a loaded
 * runner the guess is wrong in both directions. When it is, the fault-injected half of the gate
 * allocates outside the mark, produces no dangling reference, and the run fails saying the
 * verifier cannot detect a defect that was never actually created.
 *
 * The flag says whether a mark is in progress; the counter says how many have finished. Both
 * are written by the collector thread only, and only in a verification build -- there is no
 * such symbol in a shipping app.
 */
_Atomic int cn1GcVerifyMarkActive = 0;
_Atomic int cn1GcVerifyMarksDone = 0;

/*
 * Packed so one call answers both without tearing: the count in the high bits, the in-progress
 * flag in bit 0. Reading them separately let a mark begin and end between the two reads, which
 * is exactly the window the app is trying to observe.
 */
JAVA_LONG GcVerifyApp_gcMarkState___R_long(CODENAME_ONE_THREAD_STATE) {
    int done = atomic_load_explicit(&cn1GcVerifyMarksDone, memory_order_acquire);
    int active = atomic_load_explicit(&cn1GcVerifyMarkActive, memory_order_acquire);
    return (((JAVA_LONG)done) << 1) | (active ? 1 : 0);
}
#endif

void codenameOneGCMark() {
    currentGcMarkValue++;
#ifdef CN1_GC_VERIFY
    atomic_store_explicit(&cn1GcVerifyMarkActive, 1, memory_order_release);
#endif
    atomic_store_explicit(&gcMarkOverflowSeen, JAVA_FALSE, memory_order_relaxed);
    // Env-gated cycle tracer (same pattern as CN1_LEGACY_DEBUG): one stderr line
    // per collection cycle. Costs one cached getenv when disabled. Used by
    // LargeArrayGcIntegrationTest to assert the issue-5425 fix deterministically
    // (cycle COUNT is load-independent, unlike wall-clock phase timings) and
    // generally useful for diagnosing trigger storms on device.
    {
        // -1 = uninitialized. Lazy init is race-free in practice (collection
        // cycles run on the GC thread only), but keep the gate atomic so the
        // idempotent getenv probe is well-defined C even if a future caller
        // runs a cycle from another thread.
        static _Atomic int cn1LogCycles = -1;
        int logCycles = atomic_load_explicit(&cn1LogCycles, memory_order_relaxed);
        if(logCycles < 0) {
            logCycles = getenv("CN1_GC_LOG_CYCLES") ? 1 : 0;
            atomic_store_explicit(&cn1LogCycles, logCycles, memory_order_relaxed);
        }
        if(logCycles) {
            fprintf(stderr, "[GC-CYCLE] %d\n", currentGcMarkValue);
        }
    }
#ifndef CN1_DISABLE_BIBOP
    cn1BibopBeginGcCycle();
#endif
    cn1RefreshFreeMemCache();   // snapshot free RAM once per cycle for the dynamic pacing cap
    // Bump the force-mark pass epoch so the force-visited side table's prior-cycle entries
    // read as not-visited (relocated from the old per-object __codenameOneReferenceCount).
    recursionKey++;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: ensure the universal thread-stop signal handler is installed (idempotent,
    // first GC only). Used to stop+scan threads we cannot cooperatively park.
    cn1GcInstallSignalHandler();
    // Build the page/extent snapshot BEFORE the first gcMarkObject of the cycle
    // (immortal roots / currentThreadObject below): the mark guard resolves every
    // pointer against it, and on the very first cycle no snapshot exists yet --
    // an empty snapshot would make the guard skip every root (grace would save
    // the objects, but their subtrees would go untraced for a cycle). Rebuilt
    // per-thread below as before; this only guarantees a non-empty baseline.
    cn1GcBuildRootSnapshots();
#endif
    init_gc_thresholds();
    hasAgressiveAllocator = JAVA_FALSE;
    // Arm the SATB deletion barrier for the whole mark (drained to a fixpoint + cleared
    // before sweep, below). Released mutators and native threads take the barrier on any
    // heap ref store, preserving snapshot-time references they concurrently overwrite.
    // The release fence orders this ahead of any thread being unblocked (963).
#if !defined(CN1_DISABLE_SATB)
    __atomic_store_n(&gcSatbActive, 1, __ATOMIC_RELEASE);
#endif
    struct ThreadLocalData* d = getThreadLocalData();
    //int marked = 0;
    
    // copy the allocated objects from already deleted threads so we can delete that data
    #if defined(__OBJC__)
    //NSLog(@"GC mark, %d dead processes pending",nThreadsToKill);
    #endif

    // Migrate dead threads' pending allocations into allObjectsInHeap NOW, on the
    // GC thread, before any table walk of this cycle (root snapshots, sweep) --
    // the single place besides the live-thread migration below where the table
    // may grow. See the cn1DeadPendingThreads single-writer comment.
    cn1DrainDeadThreadPending();

    // Immortal roots: VM-internal objects held only by C globals (see
    // cn1AddImmortalRoot). Marked as roots every cycle; the per-thread drains
    // below trace them transitively.
    lockCriticalSection();
    for(int ir = 0 ; ir < cn1ImmortalRootsN ; ir++) {
        gcMarkObject(d, cn1ImmortalRoots[ir], JAVA_FALSE);
    }
    unlockCriticalSection();

#ifdef CN1_ON_DEVICE_DEBUG
    // Objects the debugger has handed to the IDE as objectIDs. Rooted for as
    // long as the id can come back, so an id the IDE still holds always names
    // a live object -- otherwise validating it proves only its shape, since a
    // class word survives reclamation. Released when the owning thread
    // resumes, so this is bounded by the suspension rather than permanent.
    cn1_debugger_mark_issued_roots(d);
#endif

    for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
        lockCriticalSection();
        struct ThreadLocalData* t = allThreads[iter];
        unlockCriticalSection();
        if(t != 0) {
            if(t->currentThreadObject != JAVA_NULL) {
                gcMarkObject(t, t->currentThreadObject, JAVA_FALSE);
            }
            if(t != d) {
                struct elementStruct* objects = t->threadObjectStack;

#ifdef CN1_CONSERVATIVE_GC_ROOTS
                // PHASE 3b: demand a FRESH native-stack capture this round. Only a
                // thread that actually parks at a safepoint (CN1_GC_PARK_CAPTURE)
                // re-raises this; a stale capture from a previous cycle is never
                // reused (the scanner falls back to a signal-stop instead).
                // This must run for EVERY thread -- a NATIVE (non-lightweight)
                // thread can also park once (lowMemoryMode / max-heap backpressure)
                // and would otherwise satisfy useCoop with that stale SP forever,
                // silently skipping the live region below it (missed roots -> UAF).
                t->gcParkCaptured = JAVA_FALSE;
#endif
                // wait for the thread to pause so we can traverse its stack but not for native threads where
                // we don't have much control and who barely call into Java anyway
                if(t->lightweightThread) {
                    t->threadBlockedByGC = JAVA_TRUE;
                    int totalwait = 0;
                    long now = time(0);
                    while(t->threadActive) {
                        usleep(500);
                        totalwait += 500;
                        if((totalwait%10000)==0)
                        {   long later = time(0)-now;
                            if(later>10000)
                            {
#if defined(__OBJC__)
                            NSLog(@"GC trapped for %d seconds waiting for thread %d in slot %d (%d)",
                                  (int)(later/1000),(int)t->threadId,iter,t->threadKilled);
#endif
                            }
                        }
                    }
                }
                
                // place allocations from the local thread into the global heap list.
                // The critical section serializes this migration against
                // markDeadThread/collectThreadResources: the pause-wait above ends when
                // threadActive drops, but a thread that finishes runImpl drops
                // threadActive through markDeadThread, so without the lock both sides
                // migrate the same pendingHeapAllocations concurrently -- double-placing
                // objects and racing placeObjectInHeapCollection's grow-and-free of
                // allObjectsInHeap (double free / use-after-free, observed as random
                // SIGSEGV or a libmalloc abort that wedges the VM). If the slot no
                // longer holds this thread it died and markDeadThread already migrated
                // everything under this same lock; skip.
                lockCriticalSection();
                if(allThreads[iter] == t) {
                    if (!t->lightweightThread) {
                        // For native threads, we need to actually lock them while we traverse the
                        // heap allocations because we can't use the usual locking mechanisms on
                        // them.
                        lockThreadHeapMutex();
                    }
                    for(int heapTrav = 0 ; heapTrav < t->heapAllocationSize ; heapTrav++) {
                        JAVA_OBJECT obj = (JAVA_OBJECT)t->pendingHeapAllocations[heapTrav];
                        if(obj) {
                            t->pendingHeapAllocations[heapTrav] = 0;
                            placeObjectInHeapCollection(obj);
                        }
                    }
                    if (!t->lightweightThread) {
                        unlockThreadHeapMutex();
                    }
                }
                unlockCriticalSection();
                
                // this is a thread that allocates a lot and might demolish RAM. We will hold it until the sweep is finished...
                
                JAVA_INT allocSize = t->heapAllocationSize;
                JAVA_BOOLEAN agressiveAllocator = JAVA_FALSE;
                if (isEdt(t->threadId) && !lowMemoryMode) {
                    agressiveAllocator = allocSize > CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD_EDT;
                } else {
                    agressiveAllocator = allocSize > CN1_AGRESSIVE_ALLOCATOR_THREAD_HEAP_ALLOCATIONS_THRESHOLD;
                }
                if (CN1_EDT_THREAD_ID == t->threadId && agressiveAllocator) {
                    long freeMemory = get_free_memory();
                    #if defined(__OBJC__)
                    NSLog(@"[GC] Blocking EDT as aggressive allocator, free memory=%lld", freeMemory);
                    #endif
                    
                }
                
                t->heapAllocationSize = 0;

                int stackSize = t->threadObjectStackOffset;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                // Refresh the page/extent snapshot for the VALIDATED precise scan
                // below (also rebuilt in cn1GcScanThreadNativeStack before any
                // signal-stop; building here first only makes it fresher).
                cn1GcBuildRootSnapshots();
#endif
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "precise-thread-stack"; }
#endif
                for(int stackIter = 0 ; stackIter < stackSize ; stackIter++) {
                    struct elementStruct* current = &t->threadObjectStack[stackIter];
                    if (current->type < CN1_TYPE_INVALID || current->type > CN1_TYPE_PRIMITIVE) {
#if defined(__APPLE__) && defined(__OBJC__)
#if TARGET_OS_SIMULATOR
                        CN1_GC_ASSERT(current->type >= CN1_TYPE_INVALID && current->type <= CN1_TYPE_PRIMITIVE,
                            "CN1_GC_STACK_ENTRY_TYPE");
#else
                        #if defined(__OBJC__)
                        NSLog(@"[GC] Invalid stack entry type %d at index %d; skipping entry", current->type, stackIter);
                        #endif
                        continue;
#endif
#else
                        CN1_GC_ASSERT(current->type >= CN1_TYPE_INVALID && current->type <= CN1_TYPE_PRIMITIVE,
                            "CN1_GC_STACK_ENTRY_TYPE");
#endif
                    }
                    if(current != 0 && current->type == CN1_TYPE_OBJECT && current->data.o != JAVA_NULL) {
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                        // VALIDATED precise scan: a SIGNAL-STOPPED thread can be frozen
                        // between the type and data stores of a push -- and since the
                        // elementStruct fields are plain (non-volatile) stores, clang may
                        // also reorder them -- so a type==OBJECT slot can transiently hold
                        // a stale primitive (observed: gcMarkObject(0x4e20) from a frozen
                        // PUSH_INT window). Resolve the word against the page/extent
                        // snapshot exactly like a conservative root: garbage never reaches
                        // gcMarkObject; every live PUBLISHED object resolves to itself
                        // (objects in pages/extents newer than the snapshot are mark==-1
                        // fresh and survive via the sweep's grace rule; immortal class
                        // objects are skipped by gcMarkObject anyway). Liveness of a value
                        // hidden by a torn window is covered by the conservative native
                        // stack + register scan -- the value is still in a C temp.
                        JAVA_OBJECT resolved = cn1ConservativeResolve((void*)current->data.o);
                        if(resolved != JAVA_NULL) {
                            gcMarkObject(t, resolved, JAVA_FALSE);
                        }
#else
                        gcMarkObject(t, current->data.o, JAVA_FALSE);
#endif
                        //marked++;
                    }
                }
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                // PHASE 3b HYBRID GC: in ADDITION to the precise threadObjectStack scan
                // above (which still covers legacy frames), conservatively scan this
                // thread's native C stack [sp, base) + its register snapshot and MARK
                // every resolved live object. This is the ONLY root source for object-
                // bearing FRAMELESS frames, whose object refs live in native C locals /
                // the method-local operand array rather than threadObjectStack. A given
                // object is reachable from whichever frame holds it, so the boundary
                // between a legacy caller and a frameless callee (or vice versa) is
                // covered: the conservative scan walks the WHOLE native stack regardless.
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "conservative-native-stack"; }
#endif
                cn1GcScanThreadNativeStack(d, t);
#ifdef CN1_CONSERVATIVE_GC_SELFCHECK
                cn1GcSelfCheckThreadStack(t, stackSize);
#endif
#endif
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "statics"; }
#endif
                markStatics(d);
                // Drain the worklist before unblocking the thread so that every object
                // transitively reachable from this thread's roots is fully marked while the
                // thread is still paused -- matching the snapshot-at-the-beginning property
                // the recursive implementation had. Without this drain, an unblocked mutator
                // can read a still-grey field reference into a new local and null the field;
                // the captured object would never be visited by the final drain, sweep would
                // reclaim it, and a later monitorEnter on its freed pthread_mutex_t would
                // silently deadlock. Earlier attempts at this drain hung at app startup
                // because the overflow rescan path had a cursor-reset bug; with that fixed
                // below, the drain runs to completion in O(reachable) time.
                //
                // The drain is fanned out across a worker pool (gcMarkDrainParallel).
                // This still satisfies snapshot-at-the-beginning: the roots were already
                // pushed onto the worklist serially above (while this thread is paused),
                // and gcMarkDrainParallel does not return until the entire reachable set
                // is marked -- it just marks it faster. With a single configured marker
                // it degrades to the serial gcMarkDrain and is byte-for-byte identical.
                gcMarkDrainParallel(d);
                if(!agressiveAllocator) {
                    t->threadBlockedByGC = JAVA_FALSE;
                } else {
                    hasAgressiveAllocator = JAVA_TRUE;
                }
            }
        }
    }
    #if defined(__OBJC__)
    //NSLog(@"Mark set %i objects to %i", marked, currentGcMarkValue);
    #endif
    // since they are immutable this probably doesn't need as much sync as the statics...
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "constant-pool"; }
#endif
    for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
        gcMarkObject(d, (JAVA_OBJECT)constantPoolObjects[iter], JAVA_TRUE);
    }

#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: scan the GC thread's OWN native stack last -- a root could be live only
    // in a GC-thread C local. Marks for real; the drain below propagates it.
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "gc-own-stack"; }
#endif
    cn1GcScanOwnStack(d);
#endif

    // Drain the worklist that the calls above populated. gcMarkObject no longer recurses
    // through reference fields, so we need an explicit drain pass before sweep runs.
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "root-drain"; }
#endif
    gcMarkDrain(d);

#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
    // Make already-matured slots visible in the legacy table before any safety
    // rescan. The page rescan can then skip them instead of invoking every adopted
    // object's mark function twice.
    cn1DrainAdoptBuffer();
#endif

    // NOTE: the SATB log is drained + gcSatbActive cleared AFTER the grace pass and belt
    // below, so the insertion/deletion barriers stay armed through them -- a mutator that
    // links an object into a fresh grace object DURING those phases still gets it logged
    // and marked, closing the residual window.

    // The overflow belt is retained as a correctness backstop, but runs only when a
    // worklist push was actually dropped. Normal cycles already drain every pushed
    // object and must not pay a second O(reachable) traversal.
#ifndef CN1_DISABLE_BIBOP
    // Grace-subtree marking (CORRECTNESS): a fresh BiBOP object (gcMark==-1) survives this
    // cycle via grace, and the sweep promotes it to live (gcMark=V, cn1BibopSweep) or pools
    // its whole grace page WITHOUT draining it -- so an OLD object reachable ONLY through a
    // fresh, not-yet-linked object is left unmarked and swept. When a mutator later links
    // that fresh object into the live graph, next cycle it is drained and marks the now
    // dangling child -> the intermittent Property->Double / container->content crash. Drain
    // every fresh NON-LEAF object here so a surviving grace object's subtree survives
    // WITH it. Primitive arrays and other leaf classes have no subtree and are left to
    // the sweep's normal one-cycle grace.
    //
    // Walk the FULL page registry, pruned by gcAllocedSinceSweep. The invariant is
    // exact: a mark==-1 slot can only exist on a page allocated into since that
    // page's last sweep (the sweep converts every -1 it sees to V), and EVERY
    // allocation path sets the flag before the mark-start thread sync publishes it
    // -- so a flag-FALSE page provably holds no fresh slot and is skipped without
    // touching its slots. The flag is read with a relaxed atomic (its writers
    // mirror this; same machine code as the old plain access): pre-mark stores
    // are ordered ahead of this pass by the mark-start thread pause, a store
    // this read can still miss is by definition a during-mark allocation (SATB
    // covers its links this cycle), and only the sweep -- never a phase running
    // concurrently with mutators or with this pass -- clears the flag, so a
    // missed store is re-observed next cycle. This
    // replaced a queue-of-fresh-pages scheme (issue 5425): queue-once-per-epoch
    // dedup left every allocation AFTER the queue was consumed (rest of the mark
    // plus the whole unbarriered inter-cycle window) untraced when the page was
    // not re-queued the next epoch, and the sweep then freed objects reachable
    // only through those untraced fresh objects -> user-visible heap corruption.
    {
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "grace-pass"; }
#endif
#ifdef CN1_GC_VERIFY
        // Fault injection (CN1_GC_FAULT=nograce): skip this pass entirely, which
        // reproduces the defect #5442 fixed. Used to prove the verifier fails.
        //
        // Resolve the switch HERE, at its first use. Leaving it to
        // cn1GcVerifyHeap would initialize it only after a sweep, so the first
        // cycle of every run would trace grace subtrees normally -- and a
        // workload that completes a single cycle would never inject the defect
        // at all, reporting a gate that "cannot fail" purely because it was
        // never faulted. cn1GcFaultInit is idempotent and GC-thread only.
        cn1GcFaultInit();
        extern int cn1GcFaultNoGrace;
        CN1BibopPage* gp = cn1GcFaultNoGrace ? (CN1BibopPage*)0
                         : atomic_load_explicit(&bibopAllPages, memory_order_acquire);
#else
        CN1BibopPage* gp = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
#endif
        cn1GcInGracePass = 1;   // see cn1GcGraceFullDrains
        while(gp != 0) {
#ifndef CN1_BIBOP_NO_FASTSWEEP
            if(__atomic_load_n(&gp->gcAllocedSinceSweep, __ATOMIC_RELAXED) == JAVA_FALSE) {
                gp = atomic_load_explicit(&gp->nextAll, memory_order_acquire);
                continue;
            }
#endif
#ifdef CN1_GC_INSTRUMENT
            atomic_fetch_add_explicit(&cn1BibopFreshPagesScanned, 1,
                                      memory_order_relaxed);
#endif
            int gn = atomic_load_explicit(&gp->bumpIndex, memory_order_acquire);
            CN1_GC_TRUSTED_BEGIN();  // page-slot walk: authoritative references
            for(int gi = 0 ; gi < gn ; gi++) {
                JAVA_OBJECT go = cn1BibopSlot(gp, gi);
                if(__atomic_load_n(&go->__codenameOneGcMark, __ATOMIC_ACQUIRE) == -1
                   && go->__codenameOneParentClsReference != 0
                   && go->__codenameOneParentClsReference->markFunction != 0) {
                    gcMarkObject(d, go, JAVA_FALSE);
                }
            }
            CN1_GC_TRUSTED_END();
            gp = atomic_load_explicit(&gp->nextAll, memory_order_acquire);
            // DRAIN AS WE GO (issue #5537). This pass pushes EVERY fresh object on
            // every page, and "fresh" means "allocated since the last cycle" -- a
            // number set by the mutator's allocation rate, not by the live set. A
            // thread churning short-lived objects produces far more than the worklist
            // holds (65536 entries against ~500K fresh objects per cycle on the
            // reporter's game-tree search), so pushing the whole walk before draining
            // once overflowed the worklist as a matter of course.
            //
            // Overflow is survivable but ruinously expensive: it arms the belt, whose
            // recovery pass is a full O(heap) rescan. That makes the cycle several
            // times longer, which lets the mutator produce several times more fresh
            // objects before the next one, which overflows again -- the collector
            // never returns to the fast path, RSS climbs without bound (measured 90MB
            // to 6.2GB in 20 seconds with a live set of a few hundred bytes) and the
            // app is killed by the iOS per-process ceiling, or, once the process-budget
            // pacing of #5563 holds it under that ceiling, parks on every allocation
            // and appears frozen. Both were reported on this issue.
            //
            // Draining between pages costs nothing that the end-of-pass drain would
            // not have cost anyway -- the same objects are scanned, just sooner -- and
            // it bounds the cursor, so the pass cannot overflow by volume. It must run
            // OUTSIDE the trusted window: a drain follows child words out of arbitrary
            // mark functions, which is exactly what the resolve guard is there for.
            //
            // gcMarkDrainWorklist, NOT gcMarkDrain: the latter also rescans
            // allObjectsInHeap from index 0 on every call, which is affordable a few
            // times a cycle and quadratic for a caller that drains periodically. Doing
            // it here hung the Mac Catalyst suite outright. The pass still ends with a
            // full gcMarkDrain, which is what closes the fixpoint.
            if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE / 2) {
                atomic_fetch_add_explicit(&cn1GcGraceDrains, 1, memory_order_relaxed);
                gcMarkDrainWorklist(d);
            }
        }
        gcMarkDrain(d);
        cn1GcInGracePass = 0;
    }
    // A single page's slot walk runs between two of those checks, so the worklist must
    // have room for a whole page of pushes above the drain threshold. True by a wide
    // margin at the defaults (2048 slots against 32768 of headroom); asserted so that
    // raising CN1_BIBOP_PAGE_SIZE or shrinking the worklist fails the build instead of
    // quietly restoring the overflow spiral above.
    _Static_assert(CN1_BIBOP_PAGE_SIZE / 32 <= CN1_GC_MARK_WORKLIST_SIZE / 2,
                   "a BiBOP page's slots must fit in the grace pass's worklist headroom");
#endif

    // GRACE-SUBTREE MARKING, LEGACY HALF (same correctness argument as the page
    // walk above, applied to the other heap).
    //
    // codenameOneGCSweep grants a fresh (gcMark == -1) legacy object exactly the
    // BiBOP grace rule -- it promotes the object to the current epoch instead of
    // freeing it -- so an older object reachable ONLY through such an object must
    // be marked here or the same sweep frees it while it is still referenced.
    // The page walk above cannot cover these: they are not page-resident.
    // Everything above CN1_BIBOP_MAX_OBJECT lands here (the retained large
    // byte[] blocks and Hashtable bucket arrays of issue 5425), as does every
    // allocation the adaptive survivor-heavy bypass diverts off the page heap,
    // and MATURED survivors, whose table entry is what the sweep consults.
    //
    // Only the entries already migrated into the table can be fresh here: a
    // mutator's pending allocations are not swept at all until the mark that
    // migrates them, and migration happens with the owning thread paused,
    // upstream of this pass. Cost is one extra pass over an array the sweep
    // already walks in full, and only fresh entries are traced.
#ifndef CN1_DISABLE_LEGACY_GRACE
    {
        CN1_GC_TRUSTED_BEGIN();  // walking allObjectsInHeap: authoritative references
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "legacy-grace-pass"; }
#endif
        cn1GcInGracePass = 1;   // see cn1GcGraceFullDrains
        int gt = currentSizeOfAllObjectsInHeap;
        for(int gi = 0 ; gi < gt ; gi++) {
            JAVA_OBJECT go = allObjectsInHeap[gi];
            // ACQUIRE: the mark word is the publication point (see gcMarkObject);
            // reading it plainly can let a weak-memory target observe the object
            // without the preceding parentClsReference store.
            if(go != JAVA_NULL
               && __atomic_load_n(&go->__codenameOneGcMark, __ATOMIC_ACQUIRE) == -1
               && go->__codenameOneParentClsReference != 0
               && go->__codenameOneParentClsReference->markFunction != 0) {
                gcMarkObject(d, go, JAVA_FALSE);
                // Same interleaved drain as the page walk above, and for the same
                // reason: the number of fresh entries here is set by the allocation
                // rate (everything over CN1_BIBOP_MAX_OBJECT lands in this table, as
                // does everything the survivor-heavy bypass diverts off the page
                // heap), so a busy cycle can push more of them than the worklist
                // holds and drop the collector into the overflow spiral described
                // there. Checked only on a push, since nothing else moves the cursor,
                // and outside the trusted window for the reason spelled out below.
                if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE / 2) {
                    atomic_fetch_add_explicit(&cn1GcGraceDrains, 1, memory_order_relaxed);
                    CN1_GC_TRUSTED_SUSPEND();
                    gcMarkDrainWorklist(d);
                    CN1_GC_TRUSTED_RESUME();
                }
            }
        }
        // Trust covers ONLY the registry walk above. Every fresh entry is already
        // stamped and queued, so the drain needs no trust -- and must not have it:
        // it follows child words out of arbitrary mark functions, including those
        // of dead objects kept alive by a conservative native-stack false positive,
        // whose fields can dangle. That is precisely what the guard exists to stop.
        CN1_GC_TRUSTED_END();
        gcMarkDrain(d);
        cn1GcInGracePass = 0;
    }
#endif /* CN1_DISABLE_LEGACY_GRACE -- A/B escape hatch, mirrors CN1_DISABLE_SATB */

    if(atomic_load_explicit(&gcMarkOverflowSeen, memory_order_acquire)) {
        long __beltBefore = gcMarkNewObjectCount;
#ifdef CN1_BIBOP_VALIDATE
        gcBeltDiagActive = 1;
#endif
        // One forced full rescan+drain, recovering marked-but-undrained subtrees before
        // sweep. NOTE: must NOT loop to convergence -- mutators are still active during
        // this phase, so a "mark nothing new" fixpoint can livelock against ongoing
        // allocation (observed hanging/breaking FusedTest). A single pass is bounded and
        // safe; residual incompleteness is handled by the drain-gap fix, not by looping.
#if defined(CN1_GC_INSTRUMENT) && !defined(CN1_DISABLE_BIBOP)
        atomic_fetch_add_explicit(&cn1BibopBeltRuns, 1, memory_order_relaxed);
#endif
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "overflow-belt"; }
#endif
        gcMarkWorklistOverflow = JAVA_TRUE;   // force the BiBOP page-rescan path on
        gcMarkDrain(d);
#ifdef CN1_BIBOP_VALIDATE
        gcBeltDiagActive = 0;
        if(gcMarkNewObjectCount != __beltBefore) {
            fprintf(stderr, "CN1BIBOP DRAIN INCOMPLETE: belt recovered %ld "
                    "reachable-but-unmarked object(s) before sweep\n",
                    gcMarkNewObjectCount - __beltBefore);
            fflush(stderr);
        }
#endif
    }

    // SATB termination (LAST, after grace+belt): mark everything the deletion+insertion
    // barriers logged during the WHOLE mark (including the grace pass and belt above), to
    // a fixpoint -- gcMarkObject is idempotent, so once a take-and-drain marks nothing new
    // the start-of-cycle snapshot is closed. Draining it here (not before grace+belt) is
    // what keeps the barrier armed through those phases and closes the residual grace
    // window. Bounded by the live set (only genuinely-new marks reset the fixpoint).
    for(;;) {
#ifdef CN1_GC_VERIFY
    { extern const char* cn1GcMarkPhase; cn1GcMarkPhase = "satb-drain"; }
#endif
        JAVA_OBJECT* batch;
        long n = cn1SatbTake(&batch);
        if(n == 0) break;                    // log empty at this instant
        long before = gcMarkNewObjectCount;
        for(long i = 0 ; i < n ; i++) {
            gcMarkObject(d, batch[i], JAVA_FALSE);
        }
        gcMarkDrain(d);
        if(gcMarkNewObjectCount == before) break; // processed a batch, marked nothing new -> closed
    }
    // Snapshot closed; stop logging. A store racing this clear either logged already
    // (drained just below) or overwrites/adds an already-marked reference (harmless).
    __atomic_store_n(&gcSatbActive, 0, __ATOMIC_RELEASE);
    {
        JAVA_OBJECT* batch;
        long n = cn1SatbTake(&batch);        // final catch of anything logged during the tail
        for(long i = 0 ; i < n ; i++) {
            gcMarkObject(d, batch[i], JAVA_FALSE);
        }
        if(n > 0) gcMarkDrain(d);
    }
#ifdef CN1_GC_VERIFY
    // Check the objects revived this cycle before the sweep acts on anything.
    { extern void cn1GcResurrectAudit(CODENAME_ONE_THREAD_STATE); cn1GcResurrectAudit(d); }
#endif
#if defined(CN1_GRACE_AUDIT) && !defined(CN1_DISABLE_BIBOP)
    // QA builds only: right before the sweep, verify the grace pass reached every
    // pre-mark fresh object; trace and report anything it missed (issue 5425).
    cn1GraceAuditPreSweep(d);
#endif
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
    // Marking (incl. grace, belt and SATB) is fully done. Register the objects matured
    // this cycle into allObjectsInHeap now -- single-threaded, locked, before the sweep.
    cn1DrainAdoptBuffer();
#endif
#ifdef CN1_GC_VERIFY
    // Marking is over -- including the grace pass, the belt and the SATB drain. The count moves
    // after the flag clears, so an app that sees the count advance knows the whole mark is done.
    atomic_store_explicit(&cn1GcVerifyMarkActive, 0, memory_order_release);
    atomic_fetch_add_explicit(&cn1GcVerifyMarksDone, 1, memory_order_release);
#endif
}

#ifdef DEBUG_GC_OBJECTS_IN_HEAP
int totalAllocatedHeap = 0;
int getObjectSize(JAVA_OBJECT o) {
    int* ptr = (int*)o;
    ptr--;
    return *ptr;
}

int classTypeCountPreSweep[cn1_array_3_id_java_util_Vector + 1];
int sizeInHeapForTypePreSweep[cn1_array_3_id_java_util_Vector + 1];
int nullSpacesPreSweep = 0;
int preSweepRam;
void preSweepCount(CODENAME_ONE_THREAD_STATE) {
    preSweepRam = totalAllocatedHeap;
    memset(classTypeCountPreSweep, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    memset(sizeInHeapForTypePreSweep, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    int t = currentSizeOfAllObjectsInHeap;
    int nullSpacesPreSweep = 0;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            classTypeCountPreSweep[o->__codenameOneParentClsReference->classId]++;
            sizeInHeapForTypePreSweep[o->__codenameOneParentClsReference->classId] += getObjectSize(o);
        } else {
            nullSpacesPreSweep++;
        }
    }
}

void printObjectsPostSweep(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
#endif
    
    // this should be the last class used
    int classTypeCount[cn1_array_3_id_java_util_Vector + 1];
    int sizeInHeapForType[cn1_array_3_id_java_util_Vector + 1];
    memset(classTypeCount, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    memset(sizeInHeapForType, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    int nullSpaces = 0;
    const char** arrayOfNames = malloc(sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    memset(arrayOfNames, 0, sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            classTypeCount[o->__codenameOneParentClsReference->classId]++;
            sizeInHeapForType[o->__codenameOneParentClsReference->classId] += getObjectSize(o);
            if(o->__codenameOneParentClsReference->classId > cn1_array_start_offset) {
                if(arrayOfNames[o->__codenameOneParentClsReference->classId] == 0) {
                    arrayOfNames[o->__codenameOneParentClsReference->classId] = o->__codenameOneParentClsReference->clsName;
                }
            }
        } else {
            nullSpaces++;
        }
    }
    int actualTotalMemory = 0;
    #if defined(__OBJC__)
    NSLog(@"\n\n**** There are %i - %i = %i nulls available entries out of %i objects in heap which take up %i, sweep saved %i ****", nullSpaces, nullSpacesPreSweep, nullSpaces - nullSpacesPreSweep, t, totalAllocatedHeap, preSweepRam - totalAllocatedHeap);
    #endif
    for(int iter = 0 ; iter < cn1_array_3_id_java_util_Vector ; iter++) {
        if(classTypeCount[iter] > 0) {
            if(classTypeCountPreSweep[iter] - classTypeCount[iter] > 0) {
                if(iter > cn1_array_start_offset) {
#if defined(__APPLE__) && defined(__OBJC__)
                    #if defined(__OBJC__)
                    NSLog(@"There are %i instances of %@ taking up %i bytes, %i were cleaned which saved %i bytes", classTypeCount[iter], [NSString stringWithUTF8String:arrayOfNames[iter]], sizeInHeapForType[iter], classTypeCountPreSweep[iter] - classTypeCount[iter], sizeInHeapForTypePreSweep[iter] - sizeInHeapForType[iter]);
                    #endif
#endif
                } else {
                    JAVA_OBJECT str = STRING_FROM_CONSTANT_POOL_OFFSET(classNameLookup[iter]);
#if defined(__APPLE__) && defined(__OBJC__)
                    #if defined(__OBJC__)
                    NSLog(@"There are %i instances of %@ taking up %i bytes, %i were cleaned which saved %i bytes", classTypeCount[iter], toNSString(threadStateData, str), sizeInHeapForType[iter], classTypeCountPreSweep[iter] - classTypeCount[iter], sizeInHeapForTypePreSweep[iter] - sizeInHeapForType[iter]);
                    #endif
#endif
                }
            }
            actualTotalMemory += sizeInHeapForType[iter];
        }
    }
    #if defined(__OBJC__)
    //NSLog(@"Actual ram = %i vs total mallocs = %i", actualTotalMemory, totalAllocatedHeap);
    #endif
    #if defined(__OBJC__)
    NSLog(@"**** GC cycle complete ****");
    #endif
    
    free(arrayOfNames);
#if defined(__APPLE__) && defined(__OBJC__)
    [pool release];
#endif
}

void printObjectTypesInHeap(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
#endif
    
    // this should be the last class used
    int classTypeCount[cn1_array_3_id_java_util_Vector + 1];
    int sizeInHeapForType[cn1_array_3_id_java_util_Vector + 1];
    memset(classTypeCount, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    memset(sizeInHeapForType, 0, sizeof(int) * cn1_array_3_id_java_util_Vector + 1);
    int nullSpaces = 0;
    const char** arrayOfNames = malloc(sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    memset(arrayOfNames, 0, sizeof(char*) * cn1_array_3_id_java_util_Vector + 1);
    
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            classTypeCount[o->__codenameOneParentClsReference->classId]++;
            sizeInHeapForType[o->__codenameOneParentClsReference->classId] += getObjectSize(o);
            if(o->__codenameOneParentClsReference->classId > cn1_array_start_offset) {
                if(arrayOfNames[o->__codenameOneParentClsReference->classId] == 0) {
                    arrayOfNames[o->__codenameOneParentClsReference->classId] = o->__codenameOneParentClsReference->clsName;
                }
            }
        } else {
            nullSpaces++;
        }
    }
    int actualTotalMemory = 0;
    #if defined(__OBJC__)
    NSLog(@"There are %i null available entries out of %i objects in heap which take up %i", nullSpaces, t, totalAllocatedHeap);
    #endif
    for(int iter = 0 ; iter < cn1_array_3_id_java_util_Vector ; iter++) {
        if(classTypeCount[iter] > 0) {
            float f = ((float)classTypeCount[iter]) / ((float)t) * 100.0f;
            float f2 = ((float)sizeInHeapForType[iter]) / ((float)totalAllocatedHeap) * 100.0f;
            if(iter > cn1_array_start_offset) {
#if defined(__APPLE__) && defined(__OBJC__)
                #if defined(__OBJC__)
                NSLog(@"There are %i instances of %@ which is %i percent its %i bytes which is %i mem percent", classTypeCount[iter], [NSString stringWithUTF8String:arrayOfNames[iter]], (int)f, sizeInHeapForType[iter], (int)f2);
                #endif
#endif
            } else {
                JAVA_OBJECT str = STRING_FROM_CONSTANT_POOL_OFFSET(classNameLookup[iter]);
#if defined(__APPLE__) && defined(__OBJC__)
                #if defined(__OBJC__)
                NSLog(@"There are %i instances of %@ which is %i percent its %i bytes which is %i mem percent", classTypeCount[iter], toNSString(threadStateData, str), (int)f, sizeInHeapForType[iter], (int)f2);
                #endif
#endif
            }
            actualTotalMemory += sizeInHeapForType[iter];
        }
    }
    #if defined(__OBJC__)
    NSLog(@"Actual ram = %i vs total mallocs = %i", actualTotalMemory, totalAllocatedHeap);
    #endif
    
    free(arrayOfNames);
#if defined(__APPLE__) && defined(__OBJC__)
    [pool release];
#endif
}
#endif

/**
 * The sweep GC phase iterates the memory block and deletes unmarked memory
 * since it always runs from the same thread and concurrent work doesn't matter
 * it can just delete everything it finds
 */
#ifndef CN1_DISABLE_BIBOP
static void cn1BibopSweep(CODENAME_ONE_THREAD_STATE);
#endif
// Release the threads the mark parked as aggressive allocators. Called on both exits
// from codenameOneGCSweep -- see the one that skips the reclaim.
static void cn1GcReleaseBlockedThreads(void) {
    if(!hasAgressiveAllocator) {
        return;
    }
    for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
        lockCriticalSection();
        struct ThreadLocalData* t = allThreads[iter];
        unlockCriticalSection();
        if(t != 0) {
            t->threadBlockedByGC = JAVA_FALSE;
        }
    }
}

// One line the first time the index goes stale and then once per doubling, so a run
// that is quietly not reclaiming says why without a per-cycle line on a dying process.
static void cn1GcReportStaleIndexSkip(void) {
    static long skips = 0;
    static long next = 1;
    skips++;
    if(skips >= next) {
        next *= 2;
        fprintf(stderr, "CN1 GC: page resolver index could not be rebuilt; skipped the "
                        "sweep to avoid freeing on an incomplete mark (%ld so far)\n",
                skips);
        fflush(stderr);
    }
}

void codenameOneGCSweep() {
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    // THE MARK THIS SWEEP WOULD ACT ON MAY BE INCOMPLETE. cn1GcPageIndexStale says the
    // page index could not be rebuilt, so every reference into a page registered since
    // the last successful rebuild failed to resolve and its object was never marked --
    // reachable or not. Freeing on that basis is the one thing this collector must
    // never do, and skipping the reclaim costs only the memory this cycle would have
    // returned. It is self-correcting: the rebuild is retried every cycle, and the
    // first one that succeeds marks the whole live set before this runs again.
    //
    // The blocked-thread release below still has to happen. A thread parked in
    // threadBlockedByGC is waiting on the collector, not on the reclaim, and leaving it
    // parked because the index could not be built would hang the app instead.
    if(cn1GcPageIndexStale) {
        cn1GcReportStaleIndexSkip();
        cn1GcReleaseBlockedThreads();
        return;
    }
#ifndef CN1_DISABLE_BIBOP
    // Reclaim dead slots on retired BiBOP pages (rebuild per-page free-lists from
    // the header epoch marks). Runs first, on the GC thread, with no marking in
    // flight and no mutator owning these pages.
    cn1BibopSweep(threadStateData);
#endif
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    preSweepCount(threadStateData);
#endif
    //int counter = 0;
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL) {
            if(o->__codenameOneGcMark != -1) {
                if(o->__codenameOneGcMark < currentGcMarkValue - 1) {
                    if (o->__codenameOneGcMark <= 0) {
#if defined(__APPLE__) && defined(__OBJC__)
#if TARGET_OS_SIMULATOR
                        CN1_GC_ASSERT(o->__codenameOneGcMark > 0, "CN1_GC_INVALID_MARK");
#else
                        #if defined(__OBJC__)
                        NSLog(@"[GC] Invalid GC mark %d for object %p; skipping sweep", o->__codenameOneGcMark, o);
                        #endif
                        continue;
#endif
#else
                        CN1_GC_ASSERT(o->__codenameOneGcMark > 0, "CN1_GC_INVALID_MARK");
#endif
                    }
                    allObjectsInHeap[iter] = JAVA_NULL;
#ifndef CN1_DISABLE_BIBOP
                    // A MATURED (-4) object that died: its memory is a BiBOP slot, so its
                    // DEATH belongs entirely to the BiBOP collector. The slot is already
                    // deregistered (nulled above); revert it to a normal dead -3 slot and
                    // let the next BiBOP sweep run cn1BibopReclaimSlot ONCE (finalizer +
                    // native peer + monitor). Do NOT freeAndFinalize here: that runs the
                    // finalizer a SECOND time (cn1BibopReclaimSlot runs it too), which
                    // double-frees a native-resource finalizer's buffer -- the deterministic
                    // mid-suite "corrupted unsorted chunks" heap abort.
                    if(o->__heapPosition == CN1_BIBOP_ADOPTED) {
                        o->__heapPosition = CN1_BIBOP_HEAP_POS;
                        continue;
                    }
#endif
                    //if(o->__codenameOneReferenceCount > 0) {
                    #if defined(__OBJC__)
                    //    NSLog(@"Sweped %X", (int)o);
                    #endif
                    //}
                    
#ifdef DEBUG_GC_ALLOCATIONS
                    int classId = o->className;
#if defined(__APPLE__) && defined(__OBJC__)
                    NSString* whereIs;
                    if(classId > 0) {
                        whereIs = (NSString*)((struct obj__java_lang_String*)STRING_FROM_CONSTANT_POOL_OFFSET(classId))->java_lang_String_nsString;
                    } else {
                        whereIs = @"unknown";
                    }
                    
                    if(o->__codenameOneParentClsReference->isArray) {
                        JAVA_ARRAY arr = (JAVA_ARRAY)o;
                        if(arr->__codenameOneParentClsReference == &class_array1__JAVA_CHAR) {
                            JAVA_ARRAY_CHAR* ch = (JAVA_ARRAY_CHAR*)arr->data;
                            char data[arr->length + 1];
                            for(int iter = 0 ; iter < arr->length ; iter++) {
                                data[iter] = ch[iter];
                            }
                            data[arr->length] = 0;
                            #if defined(__OBJC__)
                            NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i type: %@, which is: '%@'", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName], [NSString stringWithUTF8String:data]);
                            #endif
                        } else {
                            #if defined(__OBJC__)
                            NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i , type: %@", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName]);
                            #endif
                        }
                    } else {
                        JAVA_OBJECT str = java_lang_Object_toString___R_java_lang_String(threadStateData, o);
                        NSString* ns = toNSString(threadStateData, str);
                        if(ns == nil) {
                            ns = @"[NULL]";
                        }
                        #if defined(__OBJC__)
                        NSLog(@"Sweeping: %X, Mark: %i, Allocated: %@ %i , type: %@, toString: '%@'", (int)o, o->__codenameOneGcMark, whereIs, o->line, [NSString stringWithUTF8String:o->__codenameOneParentClsReference->clsName], ns);
                        #endif
                    }
#endif
#endif
                    
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                    // Flag the ONE remove call whose intent is deletion, not
                    // make-immortal, so removeObjectFromHeapCollection doesn't
                    // register swept objects in the immortal registry.
                    cn1SweepRemoving = JAVA_TRUE;
                    removeObjectFromHeapCollection(threadStateData, o);
                    cn1SweepRemoving = JAVA_FALSE;
#else
                    removeObjectFromHeapCollection(threadStateData, o);
#endif
                    freeAndFinalize(threadStateData, o);
                    //counter++;
                }
            } else {
                o->__codenameOneGcMark = currentGcMarkValue;
            }
        }
    }
    
    // we had a thread that really ripped into the GC so we only release that thread now after cleaning RAM
    cn1GcReleaseBlockedThreads();
    
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    //printObjectTypesInHeap(threadStateData);
    printObjectsPostSweep(threadStateData);
#endif
#ifdef CN1_RESOLVE_DIAG
    { extern void cn1ResolveDiagReport(void); cn1ResolveDiagReport(); }
#endif
#ifdef CN1_GC_VERIFY
    // The sweep is the only phase that frees memory, so this is the moment the
    // "no survivor references reclaimed memory" invariant is either intact or
    // permanently broken.
    cn1GcVerifyHeap(threadStateData);
#endif
}

JAVA_BOOLEAN removeObjectFromHeapCollection(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    // A tagged Integer has no heap entry to remove (and no object header).  Static
    // final boxed values reach this path because the translator historically
    // removes immutable static finals from the heap collection.  Treat the
    // immediate as already outside the heap instead of falling through to a
    // header dereference in findPointerPosInHeap().
    if(o != JAVA_NULL && CN1_IS_TAGGED(o)) {
        return JAVA_TRUE;
    }

    // Initialize allObjectsInHeap if it hasn't been initialized yet
    // This can happen if GC runs before any objects are allocated
    if(allObjectsInHeap == 0) {
        allObjectsInHeap = malloc(sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
        memset(allObjectsInHeap, 0, sizeof(JAVA_OBJECT) * sizeOfAllObjectsInHeap);
    }

    // BiBOP-resident object: there is no table entry to remove -- the page sweep
    // frees it regardless, so the caller's intent ("make this immortal", used by
    // the generated static-final removal and VM-internal caches) would silently
    // do NOTHING and the object would be swept while a static/C global still
    // points at it (observed: java.lang.System.LOCK freed under the GC thread's
    // own wait()). Deliver the intended semantics: register it as a permanent
    // GC root instead.
    // ONLY a plain BiBOP slot (-3) gets the make-immortal shortcut: dead -3 objects never
    // reach this function (the legacy sweep only walks allObjectsInHeap, which they are not
    // in), so a -3 here can only be a caller asking to pin a live object as a root.
    // A MATURED (-4) object must NOT take this branch: the legacy sweep DOES call this on
    // dead -4 objects to remove them before freeing, and pinning a dead object as an
    // immortal root while freeAndFinalize frees it corrupts the heap. -4 falls through to
    // findPointerPosInHeap removal, which is correct for both the sweep and the (never
    // observed in practice) make-immortal-of-an-already-matured-object case.
    if(o != JAVA_NULL && !CN1_IS_TAGGED(o) && o->__heapPosition == CN1_BIBOP_HEAP_POS) {
        cn1AddImmortalRoot(o);
        return JAVA_TRUE;
    }

    int pos = findPointerPosInHeap(o);

    // double deletion might occur when the GC and the reference counting collide
    if(pos < 0) {
        // check the local thread heap
        for(int heapTrav = 0 ; heapTrav < threadStateData->heapAllocationSize ; heapTrav++) {
            JAVA_OBJECT obj = (JAVA_OBJECT)threadStateData->pendingHeapAllocations[heapTrav];
            if(obj == o) {
                threadStateData->pendingHeapAllocations[heapTrav] = JAVA_NULL;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                if(!cn1SweepRemoving) {
                    cn1GcRegisterImmortalObj(o);
                }
#endif
                return JAVA_TRUE;
            }
        }
        return JAVA_FALSE;
    }
    o->__heapPosition = -1;

    allObjectsInHeap[pos] = JAVA_NULL;

#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // Every caller except the sweep means "make this immortal" (interned constant-
    // pool strings, static-final removal, VM caches). The object leaves the heap
    // table -- and with it the conservative resolver's extents -- so the mark
    // guard would treat it as garbage and skip tracing its children. Register it
    // so the guard recognizes it and its subtree keeps getting marked.
    if(!cn1SweepRemoving) {
        cn1GcRegisterImmortalObj(o);
    }
#endif
    return JAVA_TRUE;
}

extern JAVA_BOOLEAN gcCurrentlyRunning;
// BYTES since the last cycle -- MUST be 64-bit: an allocation-churn workload
// moves multiple GB between cycles, and the old int accumulator wrapped
// NEGATIVE, so isHighFrequencyGC returned false and the GC thread slept its
// full 30s idle wait while dead pages ballooned into the GB range.
long long allocationsSinceLastGC = 0;
long long totalAllocations = 0;
long long cn1_instr_allocCount = 0;

#ifndef CN1_DISABLE_BIBOP
// BYTES allocated through the LEGACY (non-BiBOP) path since the last cycle:
// large arrays and anything above CN1_BIBOP_MAX_OBJECT. These allocations never
// feed bibopBytesSinceGc, so before this counter existed the ONLY byte-based
// signal they produced was the 1MB isHighFrequencyGC re-arm below -- a
// threshold tuned for the pre-BiBOP collector, 24x more aggressive than the
// BiBOP trigger. On the issue-5425 workload (~800 retained 10K byte[] blocks
// over a ~500K-object survivor set) that kept the collector in back-to-back
// 200ms cycles for an allocation phase that produced NO garbage at all.
// Instead, mirror the BiBOP design: an event-driven System.gc() when legacy
// volume since the last cycle crosses the same modern budget (reset in
// cn1BibopBeginGcCycle alongside bibopBytesSinceGc).
#ifndef CN1_LEGACY_GC_TRIGGER_BYTES
#define CN1_LEGACY_GC_TRIGGER_BYTES (24*1024*1024)
#endif
// long long, NOT long: on the Windows LLP64 target long is 32-bit and this
// counter accumulates raw allocation bytes between cycle resets.
static _Atomic long long cn1LegacyBytesSinceGc = 0;
// One-shot per-cycle latch for the legacy volume trigger. The trigger is
// LEVEL-triggered on the counter -- so a crossing that lands while the
// allocating thread is inside a native-allocation bracket (skipped below when
// conservative roots are off) is simply retried by the next out-of-bracket
// legacy allocation on ANY thread, instead of being lost until the GC thread's
// idle wake -- but LATCHED so the async System.gc() (a lock+notify) is
// scheduled at most once per cycle window, not on every allocation after the
// crossing. Cleared in cn1BibopBeginGcCycle after the counter reset.
static _Atomic int cn1LegacyGcScheduled = 0;
#endif

JAVA_BOOLEAN java_lang_System_isHighFrequencyGC___R_boolean(CODENAME_ONE_THREAD_STATE) {
    long long alloc = allocationsSinceLastGC;
    allocationsSinceLastGC = 0;
    long long threshold = CN1_HIGH_FREQUENCY_ALLOCATION_THRESHOLD;
#ifndef CN1_DISABLE_BIBOP
    // Align the 200ms re-arm with the adaptive BiBOP trigger: collection
    // SCHEDULING is event-driven (cn1BibopMaybeGc for BiBOP bytes, the legacy
    // trigger in codenameOneGcMalloc for legacy bytes), so the high-frequency
    // loop only needs to re-arm when allocation volume since the last cycle
    // would have crossed the trigger anyway. The old fixed 1MB re-arm predates
    // BiBOP and turned a small retained large-array load into a continuous GC
    // storm over the whole live set (issue 5425, final Dtest shape).
    long adaptive = atomic_load_explicit(&bibopGcTriggerBytes, memory_order_relaxed);
    if(adaptive > threshold) {
        threshold = adaptive;
    }
#endif
    return alloc > threshold && totalAllocations > CN1_HIGH_FREQUENCY_ALLOCATION_ACTIVATED_THRESHOLD;
}

JAVA_INT java_lang_System_identityHashCode___java_lang_Object_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    return (JAVA_INT)__cn1Arg1;
}

#if defined(__APPLE__) && defined(__OBJC__)
extern int mallocWhileSuspended;
extern BOOL isAppSuspended;
#else
int mallocWhileSuspended = 0;
BOOL isAppSuspended = 0;
#endif

#ifndef CN1_DISABLE_BIBOP
// =========================================================================
// BiBOP: non-moving segregated-fits page heap + mark-sweep for SMALL non-array
// objects. Objects NEVER move (stable addresses, real pointers, real array
// offsets / SIMD alignment preserved). Arrays and objects larger than
// CN1_BIBOP_MAX_OBJECT keep the legacy calloc + allObjectsInHeap + table-sweep
// path verbatim. Gate the whole thing off with -DCN1_DISABLE_BIBOP for A/B.
//
// LIVENESS SOURCE OF TRUTH = the per-object header epoch mark
// (__codenameOneGcMark), exactly as the legacy collector. gcMarkObject is
// therefore UNCHANGED and works uniformly on page slots and table objects: a
// page slot has the same header layout as any object. We deliberately did NOT
// introduce a separate per-page mark bitmap; reusing the proven epoch + grace
// semantics (mark==-1 => grace; mark < cur-1 => dead) eliminates an entire
// class of mark-path races (no new claim path, no bitmap/epoch skew) and is
// what makes the "bit-identical + TSan-clean" gates reachable. The win is the
// dropped per-object registration (no placeObjectInHeapCollection, small
// objects absent from the giant allObjectsInHeap table) + word-free-list sweep.
//
// PAGE LIFECYCLE (a page is in exactly ONE role at a time):
//   FREE pool      empty page, reusable for any size class
//   PARTIAL pool   swept page w/ free slots AND some live objects (per class)
//   OWNED          a single thread bump/free-list allocates from it (NOT swept)
//   SWEEP stack    retired page (full, or from a dead thread) awaiting sweep
// Transitions: alloc pulls PARTIAL|FREE -> OWNED (under bibopMutex); a full
// OWNED page is retired -> SWEEP stack (under bibopMutex); the sweep snapshots
// the SWEEP stack via an atomic head-swap and routes each page to FREE/PARTIAL.
// A page is NEVER simultaneously allocated-into and swept (hard point #2).
//
// HARD POINT #1 (allocate-during-GC / new objects survive): a freshly
// allocated slot gets header mark = -1, and the sweep gives mark==-1 the same
// one-cycle grace the legacy table sweep does (sets it to currentGcMarkValue).
// New objects also live on the thread's OWNED current page, which the
// concurrent sweep never touches (only retired pages are swept) -- mirroring
// how legacy new objects sit in pendingHeapAllocations until a paused mark.
//
// HARD POINT #2 (sweep vs mutator alloc on same page): the sweep only ever
// processes pages it took off the SWEEP stack (retired, owner==0). The owning
// thread's current page is never on that stack, so its free-list / bump cursor
// are never touched by the sweep. No data race on a page's free-list/cursor.
//
// HARD POINT #3 (no page-table lookup race): there is NO page table. Address ->
// page is never needed on a hot path: gcMarkObject uses the header (no lookup),
// the sweep walks pages it already holds, and free() of a page slot never
// happens (slots are recycled into the page free-list, identified by the
// __heapPosition==-3 sentinel). The only cross-thread page structures are the
// pools/stack (bibopMutex) and the append-only all-pages registry used by the
// overflow rescan (atomic head, release/acquire) -- both lock/atomic safe.
//
// OVERFLOW RESCAN (invariant #3): if the mark worklist overflows, a marked
// object whose mark-function has not yet run must be re-discovered. Legacy
// rescans allObjectsInHeap; we additionally rescan every page slot via the
// all-pages registry. Concurrent reads are race-free because (a) page bump
// cursors are published with release / read with acquire, and (b) a slot's
// header fields are only dereferenced when its (atomically read) mark equals
// the current cycle -- which is impossible for a slot a mutator is mid-
// initializing (its mark transitions oldDead/FREE -> -1, never through cur).
// =========================================================================

#include <stdatomic.h>

#ifndef CN1_BIBOP_PAGE_SIZE
#define CN1_BIBOP_PAGE_SIZE (64*1024)
#endif
#ifndef CN1_BIBOP_MAX_OBJECT
#define CN1_BIBOP_MAX_OBJECT 512
#endif
// Bytes bump/free-list-allocated through BiBOP since the last GC that force a
// collection so RSS stays bounded even for an all-small-object workload (these
// objects bypass the legacy per-thread heapAllocationSize GC trigger).
#ifndef CN1_BIBOP_GC_TRIGGER_BYTES
#define CN1_BIBOP_GC_TRIGGER_BYTES (24*1024*1024)
#endif
#ifndef CN1_BIBOP_GC_MAX_TRIGGER_BYTES
#define CN1_BIBOP_GC_MAX_TRIGGER_BYTES (192*1024*1024)
#endif
#ifndef CN1_BIBOP_HIGH_THROUGHPUT_BYTES
#define CN1_BIBOP_HIGH_THROUGHPUT_BYTES (8*1024*1024)
#endif
#ifndef CN1_BIBOP_BYPASS_ALLOCATIONS
#define CN1_BIBOP_BYPASS_ALLOCATIONS 65536
#endif
#ifndef CN1_BIBOP_BYPASS_MIN_SLOTS
#define CN1_BIBOP_BYPASS_MIN_SLOTS 4096
#endif
#ifndef CN1_BIBOP_BYPASS_SURVIVAL_PERCENT
#define CN1_BIBOP_BYPASS_SURVIVAL_PERCENT 25
#endif
// Header mark sentinel for a slot sitting on a page free-list (distinct from
// -1 "fresh", and from any real epoch >= 1). The free-list link is stored in
// the slot's first pointer word (the __codenameOneParentClsReference slot),
// which a free slot does not otherwise use.
#define CN1_BIBOP_FREE_MARK (-7)
// CN1_BIBOP_HEAP_POS is defined in cn1_globals.h (shared with the inlined
// bump fast path); keep the .m self-consistent if the header changes.
#ifndef CN1_BIBOP_HEAP_POS
#define CN1_BIBOP_HEAP_POS   (-3)
#endif

// Size classes (slot sizes, 16-aligned). size <= CN1_BIBOP_MAX_OBJECT maps to
// the smallest class >= size; everything else takes the legacy path.
// CN1_BIBOP_NUM_CLASSES is fixed in cn1_globals.h (must equal this array length).
static const int cn1BibopClassSize[] = {
    32, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512
};
_Static_assert(sizeof(cn1BibopClassSize)/sizeof(int) == CN1_BIBOP_NUM_CLASSES,
               "cn1BibopClassSize length must match CN1_BIBOP_NUM_CLASSES in cn1_globals.h");
static signed char cn1BibopSizeToClass[CN1_BIBOP_MAX_OBJECT + 1];

// struct CN1BibopPage is defined in cn1_globals.h (shared with the inlined bump).

static CN1BibopPage* _Atomic bibopAllPages = 0;   // registry head (atomic)
static _Atomic long long bibopAllPagesCount = 0;  // grow-only registration count
static CN1BibopPage* bibopFreePool = 0;           // bibopMutex
static CN1BibopPage* bibopPartialPool[CN1_BIBOP_NUM_CLASSES]; // bibopMutex
static CN1BibopPage* _Atomic bibopSweepStack = 0; // Treiber-ish (push CAS / swap)
static pthread_mutex_t bibopMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t  bibopOnce  = PTHREAD_ONCE_INIT;
// Non-static: also read/written by the inlined bump fast path (cn1_globals.h).
_Atomic long bibopBytesSinceGc = 0;
_Atomic long bibopGcTriggerBytes = CN1_BIBOP_GC_TRIGGER_BYTES;
_Atomic int bibopGcEpoch = 1;
_Atomic int bibopBypassGeneration[CN1_BIBOP_NUM_CLASSES];
static long bibopCycleAllocatedBytes = 0;
// LEGACY bytes charged to the cycle that is starting -- the twin of
// bibopCycleAllocatedBytes for allocations above CN1_BIBOP_MAX_OBJECT. The
// exchange result used to be discarded, which made a large-array workload look
// QUIET to the major-sweep test below however hard it was allocating (its bytes
// never reach bibopBytesSinceGc). That is the one shape where splicing every
// partial page into every sweep is worst: heavy legacy churn driving the cycles
// while a large BiBOP survivor set supplies the pages to walk -- precisely the
// issue-5425 workload. GC-thread only, published at cycle begin.
// long long, NOT long: cn1LegacyBytesSinceGc is long long for the same reason --
// on the Windows LLP64 target long is 32 bits, so a collector that falls more
// than 2GB of legacy allocation behind would truncate this to a negative value
// and the quiet-cycle test below would read a furiously allocating app as idle.
static long long legacyCycleAllocatedBytes = 0;
static long bibopLastCycleOccupiedBytes = 0;
static long bibopLastCycleLiveBytes = 0;
static long bibopLastCycleReclaimedBytes = 0;
static int bibopHighSurvivalStreak[CN1_BIBOP_NUM_CLASSES];

// QA instrumentation only. The adaptive policy itself is always enabled;
// production builds contain neither these counters nor their atomic RMWs.
#ifdef CN1_GC_INSTRUMENT
_Atomic long cn1BibopHighThroughputPromotions = 0;
_Atomic long cn1BibopBypassActivations = 0;
_Atomic long cn1BibopBypassAllocations = 0;
_Atomic long cn1BibopFreshPagesScanned = 0;
_Atomic long cn1BibopBeltRuns = 0;
_Atomic long cn1BibopAdoptedRescanSkips = 0;
#endif
static int bibopTriggerHighSurvivalStreak = 0;

// (The old global BiBOP-monitor count that suppressed the O(1) all-dead reclaim
// for EVERY page while ANY monitor existed is gone: java.lang.System.LOCK is a
// permanently-monitored BiBOP object, so the gate degraded every sweep to the
// full per-slot walk -- measured 3-4x on allocation churn. Replaced by the
// STICKY per-page gcHasMonitors flag: the visibility concern the global count
// sidestepped (attach by a foreign thread with no happens-before to the page's
// retire-push) is covered by the mark handshake -- a page can only be PROVEN all-dead
// after a full mark in which the attaching thread was stopped and scanned,
// which orders its attach store before the sweep's read.)

// Per-thread current page per size class. Only ever touched by the owning
// thread (allocation) and by that same thread on death (collectThreadResources
// runs on the dying thread), so __thread is correct and the GC never needs to
// reach into it -- retired pages are handed to the GC via the global stack.
// Non-static: the inlined bump fast path (cn1_globals.h) reads bibopCurrent[ci].
__thread CN1BibopPage* bibopCurrent[CN1_BIBOP_NUM_CLASSES];

static void cn1BibopDoInit() {
    int ci = 0;
    for(int s = 0 ; s <= CN1_BIBOP_MAX_OBJECT ; s++) {
        while(ci < CN1_BIBOP_NUM_CLASSES && cn1BibopClassSize[ci] < s) {
            ci++;
        }
        cn1BibopSizeToClass[s] = (signed char)(ci < CN1_BIBOP_NUM_CLASSES ? ci : -1);
    }
    for(int i = 0 ; i < CN1_BIBOP_NUM_CLASSES ; i++) {
        bibopPartialPool[i] = 0;
        atomic_store_explicit(&bibopBypassGeneration[i], 0, memory_order_relaxed);
        bibopHighSurvivalStreak[i] = 0;
    }
}

static void cn1BibopFormatPage(CN1BibopPage* p, int ci) {
    int slotSize = cn1BibopClassSize[ci];
    // slot 0 starts after the page header, rounded up to 16-byte alignment so
    // every slot is at least 16-aligned (matches/exceeds calloc's guarantee).
    int hdr = (int)((sizeof(CN1BibopPage) + 15) & ~((size_t)15));
    p->classIndex = ci;
    p->slotSize = slotSize;
    p->firstSlotOffset = hdr;
    p->slotCount = (CN1_BIBOP_PAGE_SIZE - hdr) / slotSize;
    atomic_store_explicit(&p->bumpIndex, 0, memory_order_relaxed);
    p->freeList = 0;
    p->freeCount = 0;
    p->owned = JAVA_FALSE;
    atomic_store_explicit(&p->gcGraceMarked, 0, memory_order_relaxed);
    // Page-release state. Both MUST be initialized here: a page from
    // cn1BibopRawPage is indeterminate memory, and cn1BibopTrimFreePool READS
    // gcPageReleased before anything has written it. A stale nonzero value would
    // make the trim skip the madvise, mark the page released anyway and file it
    // under bibopReleasedPool -- the release silently doing nothing for that page
    // -- while a stale gcMajorSpliced would drop an ordinary page out of the
    // adaptive-trigger statistics. Reformatting a recycled page also lands here,
    // where both are already false, so the writes are idempotent.
    p->gcPageReleased = JAVA_FALSE;
    p->gcPageReusableAdvice = JAVA_FALSE;
    p->gcMajorSpliced = JAVA_FALSE;
#ifdef CN1_GC_VERIFY
    // QA: a recycled page still holds the DEAD previous occupants' headers, so a
    // reference that dangles into it would resolve to a plausible-looking object
    // whose slot boundaries have since moved. Stamp every slot dead and poison
    // its payload; the bump cursor is 0, so the verifier classifies any hit as a
    // recycled slot regardless, and this additionally destroys stale payload.
    {
        int __hdr = (int)((sizeof(CN1BibopPage) + 15) & ~((size_t)15));
        char* __s = (char*)p + __hdr;
        int __n = (CN1_BIBOP_PAGE_SIZE - __hdr) / slotSize;
        for(int __i = 0 ; __i < __n ; __i++) {
            JAVA_OBJECT __o = (JAVA_OBJECT)(__s + (long)__i * slotSize);
            __o->__codenameOneParentClsReference = 0;
            __o->__heapPosition = CN1_BIBOP_HEAP_POS;
            cn1GcVerifyPoisonSlot(__o, slotSize);
            __atomic_store_n(&__o->__codenameOneGcMark, CN1_BIBOP_FREE_MARK, __ATOMIC_RELEASE);
        }
    }
#endif
#ifndef CN1_BIBOP_NO_FASTSWEEP
    // Relaxed atomic, not plain: the acquire-path format (cn1BibopAcquirePage)
    // reformats a FREE-pool page that is already in the registry, on a mutator
    // thread, possibly while the grace pass concurrently reads this flag. The
    // store is value-identical (the sweep already reset the flag before pooling
    // the page) so any interleaving reads FALSE; the atomic just keeps the
    // concurrent read/write pair well-defined. The new-page path formats before
    // registry insertion, where nothing can observe the page.
    __atomic_store_n(&p->gcAllocedSinceSweep, JAVA_FALSE, __ATOMIC_RELAXED);
    p->gcNeedsReclaim = JAVA_FALSE;
    p->gcHasMonitors = JAVA_FALSE;
    p->gcHasAdopted = JAVA_FALSE;
    atomic_store_explicit(&p->gcLastMarkedEpoch, 0, memory_order_relaxed);
    p->gcGraceEpoch = 0;
#endif
#ifdef CN1_GRACE_AUDIT
    // Same registry-visible reformat race as the flag above: the GC thread reads
    // and rewrites this field during marking in audit builds.
    __atomic_store_n(&p->gcAuditSnapshot, 0, __ATOMIC_RELAXED);
#endif
}

void cn1BibopBeginGcCycle(void) {
    // Publish the new GC-owned epoch separately for mutators. They must never
    // read currentGcMarkValue while the collector increments it concurrently.
    atomic_store_explicit(&bibopGcEpoch, currentGcMarkValue, memory_order_relaxed);
    // Charge allocations racing this mark to the NEXT cycle. The old sweep-end
    // store lost those bytes and could delay a collection indefinitely under a
    // sustained allocator.
    bibopCycleAllocatedBytes = atomic_exchange_explicit(&bibopBytesSinceGc, 0,
                                                         memory_order_acq_rel);
    if(cn1GcOverflowTraceOn()) {
        atomic_fetch_add_explicit(&cn1GcAllocatedTotal,
                                  (long long)bibopCycleAllocatedBytes,
                                  memory_order_relaxed);
    }
    // Same charge-to-next-cycle rule for the legacy byte counter (large arrays
    // and anything above CN1_BIBOP_MAX_OBJECT): see cn1LegacyBytesSinceGc.
    // Same atomic-exchange idiom as the BiBOP reset above: a racing fetch_add
    // lands either before the swap (covered by the cycle that is starting) or
    // after it (charged to the next cycle) -- never dropped.
    legacyCycleAllocatedBytes = atomic_exchange_explicit(&cn1LegacyBytesSinceGc, 0,
                                                        memory_order_acq_rel);
    // Latch AFTER the counter: an allocator racing between the two exchanges
    // still sees the old latch and skips, so the fresh latch can never be
    // consumed by bytes just charged to the cycle that is starting.
    (void)atomic_exchange_explicit(&cn1LegacyGcScheduled, 0, memory_order_acq_rel);
#ifdef CN1_GRACE_AUDIT
    // QA builds only: snapshot every page's cursor at mark start. Slots below the
    // snapshot existed before the grace pass ran, so a complete grace pass must
    // have traced every one of them that is still fresh at pre-sweep time.
    // Mutators are still running here, so a snapshot may trail a page's true
    // cursor by the allocations racing mark start. That is INTENTIONAL
    // under-approximation: boundary slots are during-mark allocations -- the
    // class the grace guarantee does not cover this cycle (SATB + the sticky
    // dirty flag cover them) -- and excluding them keeps the audit free of
    // false positives. The audited set still spans every clearly-pre-mark slot,
    // which is exactly the population the issue-5425 bug dropped; snapshotting
    // later (after the pause) would widen coverage by only those boundary slots
    // while making benign mid-mark allocations report as misses.
    {
        CN1BibopPage* ap = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
        while(ap != 0) {
            __atomic_store_n(&ap->gcAuditSnapshot,
                             atomic_load_explicit(&ap->bumpIndex, memory_order_acquire),
                             __ATOMIC_RELAXED);
            ap = atomic_load_explicit(&ap->nextAll, memory_order_acquire);
        }
    }
#endif
}

// Raw 64KB page memory comes from large arenas -- one posix_memalign per
// CN1_BIBOP_ARENA_PAGES pages -- instead of one syscall per page. Under
// allocation churn the free pool drains faster than the concurrent sweep refills
// it, so cn1BibopNewPage was hitting posix_memalign -> mach_vm_map (a kernel
// trap) per 64KB page (~17% of an alloc-heavy benchmark, profiled). A 64KB-
// aligned arena yields 64KB-aligned pages (the mask-to-page resolve is
// unaffected), the arena is lazily faulted (RSS tracks touched pages, not the
// reservation), and BiBOP never free()s a page (swept pages are pooled), so the
// interior arena pointers are never individually released. Disable for A/B with
// -DCN1_BIBOP_NO_ARENA.
#ifndef CN1_BIBOP_ARENA_PAGES
#define CN1_BIBOP_ARENA_PAGES 64   /* 64 * 64KB = 4MB reserved per kernel mmap */
#endif
static char* bibopArenaBase = 0;
static size_t bibopArenaUsed = 0;
static size_t bibopArenaCap = 0;
static pthread_mutex_t bibopArenaMutex = PTHREAD_MUTEX_INITIALIZER;

static void* cn1BibopRawPage(void) {
#ifdef CN1_BIBOP_NO_ARENA
    void* mem = 0;
    if(posix_memalign(&mem, CN1_BIBOP_PAGE_SIZE, CN1_BIBOP_PAGE_SIZE) != 0) return 0;
    return mem;
#else
    pthread_mutex_lock(&bibopArenaMutex);
    if(bibopArenaBase == 0 || bibopArenaUsed + CN1_BIBOP_PAGE_SIZE > bibopArenaCap) {
        size_t sz = (size_t)CN1_BIBOP_PAGE_SIZE * CN1_BIBOP_ARENA_PAGES;
        void* mem = 0;
        if(posix_memalign(&mem, CN1_BIBOP_PAGE_SIZE, sz) != 0 || mem == 0) {
            pthread_mutex_unlock(&bibopArenaMutex);
            return 0;
        }
        bibopArenaBase = (char*)mem;
        bibopArenaUsed = 0;
        bibopArenaCap = sz;
    }
    void* p = bibopArenaBase + bibopArenaUsed;
    bibopArenaUsed += CN1_BIBOP_PAGE_SIZE;
    pthread_mutex_unlock(&bibopArenaMutex);
    return p;
#endif
}

// ===================== PAGE RELEASE (issue 5537) =====================
// A swept-empty page used to stay resident forever: it went to bibopFreePool and
// BiBOP had no munmap/madvise/free path at all. Because pages are ALSO
// size-class segregated, that memory was not merely idle, it was unusable by
// anything except a future block of CN1_BIBOP_MAX_OBJECT bytes or less -- a
// large array, an image buffer or a native texture could not touch it. So a
// transient small-object peak permanently subtracted its own size from the
// process budget, which on iOS is a jetsam ceiling of roughly 1.4GB rather than
// a desktop's many gigabytes. BibopPageFloorIntegrationTest measures it: after
// holding then dropping 192MB of small objects and forcing six collection
// cycles, allocating a 192MB texture set cost the full 192MB again, while the
// identical allocation over a legacy-freed hole cost nothing.
//
// The fix hands the slot region of surplus empty pages back to the OS. Three
// properties keep it cheap and safe:
//
//  - ONLY fully-empty pages, and only the ones beyond a warm cache of
//    CN1_BIBOP_FREE_POOL_KEEP. Steady-state churn cycles pages through the warm
//    cache and never calls madvise at all; a workload whose small-object demand
//    SHRINKS is the only one that pays, which is exactly the 5537 shape.
//  - The page HEADER stays resident. Only the slot region is released, starting
//    at the first system-page boundary at or after sizeof(CN1BibopPage), so the
//    pool links, the class index and the bump cursor survive. BiBOP pages are
//    CN1_BIBOP_PAGE_SIZE-aligned and that is a multiple of every system page
//    size we run on, so the page base is always system-page-aligned.
//  - Reads of a released region cannot fault -- the mapping is still there, they
//    return zero or stale bytes. That matters because the conservative root
//    resolver can still probe such a page from a stale stack word. It rejects
//    what it finds either way: a zeroed slot has __heapPosition 0, which is
//    neither CN1_BIBOP_HEAP_POS nor CN1_BIBOP_ADOPTED. Nothing live can be lost
//    because a page only reaches this path with liveCount == 0, which the sweep
//    established AFTER the mark that the conservative scan feeds.
#ifndef CN1_BIBOP_FREE_POOL_KEEP
#define CN1_BIBOP_FREE_POOL_KEEP 64   /* 64 * 64KB = 4MB kept warm, never released */
#endif
// Bound on the madvise work one sweep may do, so a collapse from a huge pool
// cannot turn a single cycle into a syscall storm. The remainder is released by
// the following sweeps.
#ifndef CN1_BIBOP_RELEASE_PER_SWEEP
#define CN1_BIBOP_RELEASE_PER_SWEEP 1024  /* up to 64MB per cycle */
#endif
// A cycle that allocated less than this is treated as QUIET: the app is not
// churning, so the major sweep below is both cheap (no mutator contending for
// the pages) and exactly what is wanted (a burst has just ended and its memory
// should go back). A quarter of the base trigger is comfortably below any
// allocation-driven cycle, which by construction crosses the whole trigger.
#ifndef CN1_BIBOP_MAJOR_SWEEP_QUIET_BYTES
#define CN1_BIBOP_MAJOR_SWEEP_QUIET_BYTES (CN1_BIBOP_GC_TRIGGER_BYTES / 4)
#endif
// Backstop cadence for an app that never goes quiet, so a long-running churn
// still eventually returns pages. Every cycle would make the sweep O(all pages),
// which is the regression issue 5425 fixed.
#ifndef CN1_BIBOP_MAJOR_SWEEP_CYCLES
#define CN1_BIBOP_MAJOR_SWEEP_CYCLES 16
#endif
// Cycles since the last major sweep. GC-thread only.
static int bibopCyclesSinceMajorSweep = 0;

// Env-gated tracer, same pattern as CN1_LOG_LOWMEM_PARKS: one line per sweep
// that actually released pages. Costs a cached getenv when disabled.
static _Atomic int cn1PageReleaseTrace = -1;
static int cn1PageReleaseTraceOn(void) {
    int on = atomic_load_explicit(&cn1PageReleaseTrace, memory_order_relaxed);
    if(on < 0) {
        on = getenv("CN1_LOG_PAGE_RELEASE") ? 1 : 0;
        atomic_store_explicit(&cn1PageReleaseTrace, on, memory_order_relaxed);
    }
    return on;
}

// Last errno from a rejected MADV_FREE_REUSABLE, surfaced by the tracer. Only
// MADV_FREE_REUSABLE decrements phys_footprint; the MADV_FREE fallback leaves
// the pages charged to the process, so a nonzero value here means the release
// ran but bought nothing.
static int cn1PageReleaseReusableErrno = 0;
// CN1_BIBOP_FAIL_REUSE forces every MADV_FREE_REUSE to report failure. That path
// is otherwise unreachable -- the call does not fail in practice -- so the code
// that has to cope with a page which cannot be restored would never run outside
// a test. Deliberately NOT part of the CN1_GC_FAULT family: those live under
// CN1_GC_VERIFY, and page release is disabled in verifier builds, so a fault
// declared there could never fire.
// CN1_BIBOP_FAIL_REUSABLE=<n> makes the first n MADV_FREE_REUSABLE calls report
// failure so the MADV_FREE fallback is taken, then behaves normally. A count
// rather than a switch because the interesting behaviour is what happens AFTER
// the transient clears: pages released through the fallback are still charged to
// the process and have to be upgraded by a later trim.
static _Atomic int cn1ReusableFailBudget = -1;
static int cn1ReusableFailInjectTake(void) {
    int n = atomic_load_explicit(&cn1ReusableFailBudget, memory_order_relaxed);
    if(n < 0) {
        const char* e = getenv("CN1_BIBOP_FAIL_REUSABLE");
        n = (e != 0) ? atoi(e) : 0;
        if(n < 0) n = 0;
        atomic_store_explicit(&cn1ReusableFailBudget, n, memory_order_relaxed);
    }
    if(n == 0) {
        return 0;
    }
    atomic_store_explicit(&cn1ReusableFailBudget, n - 1, memory_order_relaxed);
    return 1;
}

static _Atomic int cn1ReuseFailInject = -1;
static int cn1ReuseFailInjectOn(void) {
    int on = atomic_load_explicit(&cn1ReuseFailInject, memory_order_relaxed);
    if(on < 0) {
        on = getenv("CN1_BIBOP_FAIL_REUSE") ? 1 : 0;
        atomic_store_explicit(&cn1ReuseFailInject, on, memory_order_relaxed);
    }
    return on;
}

// Last errno from a rejected MADV_FREE_REUSE on a page that WAS marked reusable.
// Distinct from the release errno above: this one means a page could not be
// taken back out of the reusable state and was therefore not handed out.
static int cn1PageReuseFailErrno = 0;

// Empty pages whose slot region has been given back to the OS. Kept OFF
// bibopFreePool so the acquire path always prefers a warm page and only pays the
// re-acquire plus refault when no warm page is left. bibopMutex.
static CN1BibopPage* bibopReleasedPool = 0;

// Released pages whose MADV_FREE_REUSE was rejected, so they cannot be handed to
// the allocator yet. Kept OFF bibopReleasedPool so one unrestorable page cannot
// stand in front of a stocked pool; consulted only when that pool is empty, and
// then at most one per acquisition.
//
// A FIFO, with an explicit tail, and that detail is the whole point. Pushing a
// failure back at the HEAD means the next acquisition pops the same page, fails
// again, and puts it back -- so one permanently unrestorable page is retried
// forever while every other parked page behind it is never reached again. That
// is the same defect this pool was introduced to fix, one level down. Rotating
// the failure to the tail gives round-robin retry: each acquisition tries a
// different page, and a page that becomes restorable is eventually reached.
// bibopMutex.
static CN1BibopPage* bibopReuseFailedPool = 0;
static CN1BibopPage* bibopReuseFailedTail = 0;

// Pages sitting in bibopReleasedPool that only got the MADV_FREE fallback, so
// they are still charged to phys_footprint and want the reusable advice retried.
// Lets the upgrade pass skip its walk entirely in the normal case, which is
// every case: the fallback is only taken when MADV_FREE_REUSABLE is rejected,
// which does not happen in ordinary operation.
static _Atomic long bibopFallbackPageCount = 0;

// Park a page whose restore was rejected at the TAIL of the retry pool.
static void cn1BibopParkReuseFailure(CN1BibopPage* p) {
    p->nextPool = 0;
    if(bibopReuseFailedTail != 0) {
        bibopReuseFailedTail->nextPool = p;
    } else {
        bibopReuseFailedPool = p;
    }
    bibopReuseFailedTail = p;
}

// How many released pages one acquisition may try to restore before giving up
// and allocating a fresh page. Bounds the syscalls a run of rejections can cost
// while still stepping past a bad page rather than stalling on it.
#ifndef CN1_BIBOP_REUSE_ATTEMPTS
#define CN1_BIBOP_REUSE_ATTEMPTS 4
#endif

#if defined(CN1_GC_INSTRUMENT) && !defined(CN1_BIBOP_NO_PAGE_RELEASE)
static _Atomic long cn1BibopPagesReleased = 0;
static _Atomic long cn1BibopPagesReacquired = 0;
#endif

// Byte offset within a page at which the releasable slot region begins, or 0 if
// releasing is impossible on this configuration (system page so large that the
// header plus one system page would not fit).
static size_t cn1BibopReleaseOffset(void) {
#if defined(CN1_BIBOP_NO_PAGE_RELEASE) || defined(_WIN32)
    return 0;
#elif defined(CN1_GC_VERIFY)
    // The heap verifier works by INSPECTING memory the allocator has logically
    // freed: cn1BibopFormatPage poisons every recycled slot and the verifier
    // classifies a reference that lands on one as a violation. Handing those
    // pages back to the OS erases that evidence -- they fault back in as zeroes,
    // the conservative resolver rejects a zeroed slot on __heapPosition, and a
    // genuine dangling reference reads as "no such object" instead of being
    // reported. GcHeapIntegrityIntegrationTest catches this directly: with page
    // release on, its deliberately re-injected grace-pass defect (issue 5425)
    // stops being detected and the gate goes inert. QA builds therefore keep
    // pages resident; shipping builds, which is where footprint matters, do not
    // define CN1_GC_VERIFY.
    return 0;
#else
    static size_t cached = (size_t)-1;
    if(cached == (size_t)-1) {
        size_t ps = (size_t)getpagesize();
        if(ps == 0 || (ps & (ps - 1)) != 0) {
            cached = 0;
        } else {
            size_t hdr = (sizeof(CN1BibopPage) + ps - 1) & ~(ps - 1);
            cached = (hdr + ps > (size_t)CN1_BIBOP_PAGE_SIZE) ? 0 : hdr;
        }
    }
    return cached;
#endif
}

// Hand a fully-empty page's slot region back to the OS. Returns whether the
// advice was actually accepted: madvise can fail (a transient EAGAIN from
// Linux MADV_DONTNEED, or a range Darwin refuses), and a page whose memory was
// NOT handed back must not be recorded as released -- it would be filed under
// bibopReleasedPool and never retried, so the footprint would stay up forever
// with nothing to show that anything went wrong. The caller keeps a rejected
// page in bibopFreePool so the next sweep tries it again.
// The caller must have made the page unreachable from every pool first.
static JAVA_BOOLEAN cn1BibopReleasePageMemory(CN1BibopPage* p) {
#if !defined(CN1_BIBOP_NO_PAGE_RELEASE) && !defined(_WIN32)
    size_t off = cn1BibopReleaseOffset();
    if(off == 0) {
        return JAVA_FALSE;
    }
    void* addr = (char*)p + off;
    size_t len = (size_t)CN1_BIBOP_PAGE_SIZE - off;
    JAVA_BOOLEAN ok = JAVA_FALSE;
#if defined(__APPLE__)
    // MADV_FREE_REUSABLE is what libmalloc uses to return large blocks, and
    // unlike plain MADV_FREE it decrements phys_footprint immediately -- which
    // is the figure iOS jetsam meters, so it is the one that has to move.
    // MADV_FREE is kept as a fallback: it still lets the kernel take the pages
    // under pressure, just without moving the accounting. Pairing MADV_FREE_REUSE
    // with a range that only got MADV_FREE is harmless (it is rejected and there
    // is no accounting to restore), so one released flag covers both.
    if(!cn1ReusableFailInjectTake() && madvise(addr, len, MADV_FREE_REUSABLE) == 0) {
        ok = JAVA_TRUE;
        p->gcPageReusableAdvice = JAVA_TRUE;   // must be restored before reuse
    } else {
        cn1PageReleaseReusableErrno = errno;
        // FALLBACK. MADV_FREE lets the kernel take the pages under pressure but
        // does NOT reduce phys_footprint, so this page is still charged to the
        // process -- it is released in the sense that matters to the allocator
        // but not in the sense this whole feature exists for. It is left with
        // gcPageReusableAdvice clear, which is what cn1BibopUpgradeFallbackPages
        // uses to find it and retry the reusable advice on a later trim.
        ok = (madvise(addr, len, MADV_FREE) == 0) ? JAVA_TRUE : JAVA_FALSE;
        p->gcPageReusableAdvice = JAVA_FALSE;  // no pairing to restore
        if(ok) {
            atomic_fetch_add_explicit(&bibopFallbackPageCount, 1, memory_order_relaxed);
        }
    }
#elif defined(MADV_DONTNEED)
    // Linux: drops the pages and re-faults them as zero, which is exactly the
    // contract the acquire-path format expects.
    ok = (madvise(addr, len, MADV_DONTNEED) == 0) ? JAVA_TRUE : JAVA_FALSE;
#endif
#if defined(CN1_GC_INSTRUMENT)
    if(ok) {
        atomic_fetch_add_explicit(&cn1BibopPagesReleased, 1, memory_order_relaxed);
    }
#endif
    return ok;
#else
    (void)p;
    return JAVA_FALSE;
#endif
}

// Take a released page back into service. Returns whether the page is safe to
// allocate into.
//
// On Darwin a page released with MADV_FREE_REUSABLE is still classified by the
// kernel as reusable storage. MADV_FREE_REUSE is what takes it back out of that
// state and restores the footprint accounting, so if it FAILS the page must not
// be handed to the allocator: the kernel would be free to treat storage that is
// about to hold live objects as discardable, and the process would under-report
// memory it is genuinely using. A page released with an advice that has no
// pairing (MADV_FREE, or Linux MADV_DONTNEED) has nothing to restore and is
// always safe -- which is exactly why the advice kind is recorded per page, so
// an expected rejection there is not mistaken for a real failure here.
static JAVA_BOOLEAN cn1BibopReusePageMemory(CN1BibopPage* p) {
#if !defined(CN1_BIBOP_NO_PAGE_RELEASE) && !defined(_WIN32)
    size_t off = cn1BibopReleaseOffset();
    if(off == 0) {
        p->gcPageReleased = JAVA_FALSE;
        return JAVA_TRUE;
    }
#if defined(__APPLE__)
    if(!p->gcPageReusableAdvice) {
        // Released through the fallback: nothing to restore, but it is leaving
        // bibopReleasedPool, so it is no longer waiting for an upgrade.
        atomic_fetch_sub_explicit(&bibopFallbackPageCount, 1, memory_order_relaxed);
    }
    if(p->gcPageReusableAdvice) {
        if(cn1ReuseFailInjectOn()) {
            return JAVA_FALSE;               // fault injection; see the helper
        }
        if(madvise((char*)p + off, (size_t)CN1_BIBOP_PAGE_SIZE - off,
                   MADV_FREE_REUSE) != 0) {
            cn1PageReuseFailErrno = errno;
            return JAVA_FALSE;                 // leave it released; caller retries later
        }
        p->gcPageReusableAdvice = JAVA_FALSE;
    }
#endif
#if defined(CN1_GC_INSTRUMENT)
    atomic_fetch_add_explicit(&cn1BibopPagesReacquired, 1, memory_order_relaxed);
#endif
#endif
    p->gcPageReleased = JAVA_FALSE;
    return JAVA_TRUE;
}

// Retry MADV_FREE_REUSABLE on pages that only got the MADV_FREE fallback.
//
// Without this a transient rejection is permanent in effect: the page is filed
// in bibopReleasedPool with gcPageReleased set, so every later trim skips it,
// and it is only ever offered the reusable advice again if the workload happens
// to exhaust the warm pool and reacquire it. A post-burst app that never does
// leaves the page charged to phys_footprint indefinitely -- which is the exact
// figure this feature exists to reduce.
//
// Bounded per trim, and it holds bibopMutex across the syscalls. Both are
// deliberate: the list must not change under the walk, and this path only has
// work to do when a rejection actually happened, which does not occur in normal
// operation at all.
// Matches CN1_BIBOP_RELEASE_PER_SWEEP: an upgrade costs exactly the same single
// madvise a release does, so budgeting it lower only means a burst of transient
// rejections takes proportionally longer to stop being charged. Measured with
// 2000 injected rejections: at 64 per trim the probe ended at 179,776KB with the
// backlog still draining, at this budget it lands on the uninjected figure.
#ifndef CN1_BIBOP_UPGRADE_PER_SWEEP
#define CN1_BIBOP_UPGRADE_PER_SWEEP CN1_BIBOP_RELEASE_PER_SWEEP
#endif
static int cn1BibopUpgradeFallbackPages(void) {
#if !defined(CN1_BIBOP_NO_PAGE_RELEASE) && !defined(_WIN32) && defined(__APPLE__)
    size_t off = cn1BibopReleaseOffset();
    if(off == 0) {
        return 0;
    }
    if(atomic_load_explicit(&bibopFallbackPageCount, memory_order_relaxed) <= 0) {
        return 0;                            // nothing is waiting; skip the walk
    }
    int upgraded = 0;
    int attempted = 0;
    pthread_mutex_lock(&bibopMutex);
    // The budget counts ATTEMPTS, not pages looked at. Counting skips would mean
    // that once the leading budget-sized window is upgraded, every later pass
    // spends its whole budget re-walking that window and never reaches the
    // fallback pages behind it -- they would stay charged forever unless
    // allocation happened to reacquire them. Walking past an already-upgraded
    // page is a pointer dereference; only the madvise is worth budgeting.
    for(CN1BibopPage* p = bibopReleasedPool ;
        p != 0 && attempted < CN1_BIBOP_UPGRADE_PER_SWEEP ;
        p = p->nextPool) {
        if(p->gcPageReusableAdvice) {
            continue;                        // already off the footprint
        }
        attempted++;
        if(!cn1ReusableFailInjectTake()
           && madvise((char*)p + off, (size_t)CN1_BIBOP_PAGE_SIZE - off,
                      MADV_FREE_REUSABLE) == 0) {
            p->gcPageReusableAdvice = JAVA_TRUE;
            upgraded++;
            atomic_fetch_sub_explicit(&bibopFallbackPageCount, 1, memory_order_relaxed);
        }
    }
    pthread_mutex_unlock(&bibopMutex);
    return upgraded;
#else
    return 0;
#endif
}

// Release the surplus of bibopFreePool. Called at the end of a sweep, on the GC
// thread. The surplus is UNLINKED under the mutex before any madvise runs, so an
// allocator can never acquire a page while its slot region is being dropped; the
// pages are then published onto bibopReleasedPool in one O(1) splice.
static void cn1BibopTrimFreePool(void) {
#if !defined(CN1_BIBOP_NO_PAGE_RELEASE) && !defined(_WIN32)
    if(cn1BibopReleaseOffset() == 0) {
        return;
    }
    pthread_mutex_lock(&bibopMutex);
    CN1BibopPage* keepTail = 0;
    CN1BibopPage* p = bibopFreePool;
    int kept = 0;
    while(p != 0 && kept < CN1_BIBOP_FREE_POOL_KEEP) {
        keepTail = p;
        p = p->nextPool;
        kept++;
    }
    // p is the head of the surplus; bound how much of it this sweep takes.
    CN1BibopPage* surplus = p;
    CN1BibopPage* surplusTail = 0;
    int taken = 0;
    while(p != 0 && taken < CN1_BIBOP_RELEASE_PER_SWEEP) {
        surplusTail = p;
        p = p->nextPool;
        taken++;
    }
    if(surplusTail != 0) {
        surplusTail->nextPool = 0;          // detach the taken run
        if(keepTail != 0) {
            keepTail->nextPool = p;         // splice any untaken remainder back
        } else {
            bibopFreePool = p;
        }
    }
    pthread_mutex_unlock(&bibopMutex);

    if(surplusTail == 0) {
        // Still worth a pass: pages released through the fallback on an earlier
        // trim are charged to the footprint until the reusable advice takes.
        int upgradedOnly = cn1BibopUpgradeFallbackPages();
        if(upgradedOnly != 0 && cn1PageReleaseTraceOn()) {
            fprintf(stderr, "[PAGE-RELEASE] kept=%d surplus=0 upgraded=%d\n",
                    kept, upgradedOnly);
        }
        return;                             // nothing above the warm cache
    }
    // Partition the detached run: pages whose memory the kernel actually took go
    // to bibopReleasedPool, pages it rejected go back to bibopFreePool. The
    // rejected ones are still perfectly good empty pages -- they simply have not
    // been handed back yet -- so returning them to the free pool both keeps them
    // allocatable and lets a later sweep retry the release.
    CN1BibopPage* relHead = 0;
    CN1BibopPage* relTail = 0;
    CN1BibopPage* retryHead = 0;
    CN1BibopPage* retryTail = 0;
    int releasedNow = 0;
    int rejected = 0;
    CN1BibopPage* q = surplus;
    while(q != 0) {
        CN1BibopPage* next = q->nextPool;
        JAVA_BOOLEAN released = q->gcPageReleased;
        if(!released) {
            released = cn1BibopReleasePageMemory(q);
            if(released) {
                q->gcPageReleased = JAVA_TRUE;
                releasedNow++;
            } else {
                rejected++;
            }
        }
        if(released) {
            q->nextPool = relHead;
            relHead = q;
            if(relTail == 0) relTail = q;
        } else {
            q->nextPool = retryHead;
            retryHead = q;
            if(retryTail == 0) retryTail = q;
        }
        q = next;
    }
    int upgraded = cn1BibopUpgradeFallbackPages();
    if(cn1PageReleaseTraceOn()) {
        fprintf(stderr, "[PAGE-RELEASE] kept=%d taken=%d released=%d rejected=%d "
                "upgraded=%d headerBytes=%zu releaseErrno=%d reuseFailErrno=%d\n",
                kept, taken, releasedNow, rejected, upgraded,
                cn1BibopReleaseOffset(), cn1PageReleaseReusableErrno,
                cn1PageReuseFailErrno);
    }
    pthread_mutex_lock(&bibopMutex);
    if(relTail != 0) {
        relTail->nextPool = bibopReleasedPool;
        bibopReleasedPool = relHead;
    }
    if(retryTail != 0) {
        retryTail->nextPool = bibopFreePool;
        bibopFreePool = retryHead;
    }
    pthread_mutex_unlock(&bibopMutex);
#endif
}

static CN1BibopPage* cn1BibopNewPage(int ci) {
    void* mem = cn1BibopRawPage();
    if(mem == 0) {
        return 0;
    }
    CN1BibopPage* p = (CN1BibopPage*)mem;
    // cn1BibopRawPage hands back indeterminate memory (an arena carved from
    // posix_memalign, which malloc may have recycled from its own free list), so
    // every header field is garbage until written. cn1BibopFormatPage sets the
    // ones it knows about; zeroing first means a field added later cannot be
    // silently read before its first assignment, which is the defect this guards
    // against. Once per NEW page only -- the pool hit path never reaches here.
    memset(p, 0, sizeof(CN1BibopPage));
    cn1BibopFormatPage(p, ci);
    // Publish into the append-only registry: set nextAll (release) BEFORE the
    // head CAS so a concurrent rescan that reads the new head (acquire) sees a
    // fully-linked node.
    CN1BibopPage* head = atomic_load_explicit(&bibopAllPages, memory_order_relaxed);
    do {
        atomic_store_explicit(&p->nextAll, head, memory_order_relaxed);
    } while(!atomic_compare_exchange_weak_explicit(&bibopAllPages, &head, p,
                memory_order_release, memory_order_relaxed));
    // grow-only registration count: the snapshot builder keys its cached
    // base-sorted page array off this (nodes never unlink or reorder)
    atomic_fetch_add_explicit(&bibopAllPagesCount, 1, memory_order_release);
    return p;
}

static inline JAVA_OBJECT cn1BibopSlot(CN1BibopPage* p, int i) {
    return (JAVA_OBJECT)((char*)p + p->firstSlotOffset + (long)i * p->slotSize);
}

// Trigger a full GC if BiBOP allocation volume since the last collection has
// crossed the threshold (these objects don't feed the legacy heapAllocationSize
// trigger). Mirrors codenameOneGcMalloc's simple self-triggering branch.
// Adaptive-backpressure ceiling. The old design paced every mutator a fixed
// Thread.sleep(2) on each GC trigger (in System.gc()), which is pure stall when
// the concurrent collector is keeping up -- on allocate-and-drop churn that was
// the dominant cost (objectAllocation 54ms -> 18ms once removed). But removing it
// unconditionally let the mutator outrun the collector and balloon RSS to ~2GB.
// Instead, pace PROPORTIONALLY: only when uncollected BiBOP volume since the last
// GC exceeds this hard cap does the mutator wait for the collector to catch up,
// bounding RSS. When the collector keeps up (bytes stays near the trigger) this
// never waits. Disable with -DCN1_BIBOP_NO_PACING for A/B.
#ifndef CN1_BIBOP_GC_HARD_CAP_MULTIPLIER
#define CN1_BIBOP_GC_HARD_CAP_MULTIPLIER 3
#endif
// Bound on the dynamic widening below, in collection triggers, applied ONLY once the
// process is already large (issue #5537). Free RAM is a reason to let a fast thread run
// FURTHER ahead of the collector; it is not a reason to accumulate an unbounded amount
// of garbage. On a host with no per-process ceiling -- the iOS Simulator, macOS,
// Catalyst, a desktop build -- the fractions below evaluate to gigabytes, and nothing
// stopped a game-tree search from reaching a 15.6GB footprint against a 4MB live set
// while the collector completed 7 cycles in five seconds. Under a ceiling that shape is
// the app being killed; off one it is the "memory usage escalates, 500MB to 5GB in five
// minutes" the reporter saw in the simulator.
//
// Expressed in TRIGGERS rather than as a constant because the trigger already tracks the
// heap: cn1BibopAdaptAfterSweep doubles it (to CN1_BIBOP_GC_MAX_TRIGGER_BYTES) for a
// survivor-heavy workload and returns it to the base for a churning one. So an app that
// needs the headroom -- a render holding a large live set -- keeps 8 of its own enlarged
// triggers, while pure churn is held to 8 of the base one.
//
// GATED ON FOOTPRINT because the point is to stop unbounded GROWTH, not to stop a thread
// from running ahead. A volume cap is a cliff: crossing it parks the mutator for a whole
// collection, and applying one unconditionally costs 47% on the objectAllocation
// microbenchmark (measured) for a process that was never going to grow anyway. Below the
// floor nothing is clamped and the pacing is exactly what it was; above it the app has
// demonstrated it can grow, and bounding the run-ahead is what keeps it from continuing.
#ifndef CN1_BIBOP_GC_MAX_CAP_MULTIPLIER
#define CN1_BIBOP_GC_MAX_CAP_MULTIPLIER 8
#endif
// Footprint at which the bound above starts applying. Well above what a healthy app of
// any size settles at with the collector keeping up, and well below the point where an
// unbounded run-ahead has done real damage. Where there is no footprint probe (Windows)
// the reading is 0 and the bound never engages, which leaves that platform on the static
// cap it already had.
#ifndef CN1_PACING_GROWTH_FLOOR_BYTES
#define CN1_PACING_GROWTH_FLOOR_BYTES (512LL*1024*1024)
#endif
// How stale a below-floor footprint reading may be before the bound re-probes it. The
// probe is task_info on Apple and one /proc read on Linux -- a microsecond or two -- and
// it is taken at most once per interval across the whole process, and only when the bound
// would otherwise bind. 25ms caps the overshoot at that much allocation.
#ifndef CN1_PACING_FOOTPRINT_REFRESH_MS
#define CN1_PACING_FOOTPRINT_REFRESH_MS 25
#endif
// A thread with more than this many legacy allocations since the last GC (heapAllocationSize,
// reset each cycle) is treated as high-throughput and gets the deeper pacing headroom below.
#ifndef CN1_BIBOP_HIGH_THROUGHPUT_ALLOCS
#define CN1_BIBOP_HIGH_THROUGHPUT_ALLOCS 50000
#endif
// How much of the process budget stays unspent. Under a ceiling a thread is admitted
// only when the remaining budget can absorb the block it is about to dirty PLUS this,
// so the process settles at roughly limit-minus-margin instead of riding the limit.
//
// It has to cover two things the pacing path cannot see. Other threads dirty memory
// between their own checks; and native allocation -- an image buffer, a Metal texture,
// a glyph atlas -- never passes through here at all while still spending the same
// budget. 64MB is comfortably above both on the workloads this was measured on, and
// small against a ceiling of roughly 1.4GB.
#ifndef CN1_PACING_HEADROOM_MARGIN
#define CN1_PACING_HEADROOM_MARGIN (64LL*1024*1024)
#endif

// Poll interval for a budgeted wait. What we are waiting for is a completed collection,
// which takes hundreds of milliseconds, so 50us granularity bought nothing and cost a
// headroom probe 20000 times a second on a thread that is doing no work anyway.
#ifndef CN1_PACING_WAIT_SLEEP_US
#define CN1_PACING_WAIT_SLEEP_US 1000
#endif

// Absolute backstop on a budgeted wait: 10000 * 1ms = 10s, matching the unbudgeted
// spin's bound. A collector that is dead or wedged degrades to footprint growth rather
// than a permanent hang. Normally the barren-cycle rule below ends the wait far sooner.
#ifndef CN1_PACING_MAX_WAIT_SPINS
#define CN1_PACING_MAX_WAIT_SPINS 10000
#endif

// How often a parked thread re-requests collection, in poll intervals: 200ms, matching
// the collector's own high-frequency cadence.
#ifndef CN1_PACING_GC_REQUEST_SPINS
#define CN1_PACING_GC_REQUEST_SPINS 200
#endif

// Give up after this many CONSECUTIVE completed collections that freed nothing useful.
// Waiting on a fixed timeout is the wrong rule in both directions: it abandons a
// collection that is still returning memory, and it keeps waiting long after collection
// has stopped helping. bibopGcEpoch is published at cycle START, so two advances mean at
// least one full mark-and-sweep completed in between -- the earlier version could give up
// mid-sweep, having never seen a completed collection at all.
#ifndef CN1_PACING_BARREN_CYCLES
#define CN1_PACING_BARREN_CYCLES 2
#endif

// Headroom gain that counts as a collection having helped.
#ifndef CN1_PACING_PROGRESS_EPSILON
#define CN1_PACING_PROGRESS_EPSILON (1024L*1024)
#endif

// How much legacy allocation the PROCESS may do between two pacing evaluations. This
// bounds the overshoot past the cap, so it has to stay well under the smallest cap the
// clamp can produce; it is deliberately independent of CN1_LEGACY_GC_TRIGGER_BYTES,
// which sizes when to SCHEDULE a cycle rather than when to stop running ahead of one.
//
// Process-wide, NOT per-thread. A thread-local interval bounds nothing on a machine
// with several allocators: sixteen workers can each allocate and dirty just under the
// interval without a single one of them reaching a check, while the shared counter and
// the footprint grow by sixteen times it. Crossings are detected on the GLOBAL counter
// instead, using the pre-add value the trigger below already computes -- so a crossing
// is attributed to exactly the one allocation that passed the boundary, whichever
// thread made it, and the bound holds however many threads are allocating. It also
// needs no per-thread state at all: a shift and a compare on a value already in hand.
#ifndef CN1_PACING_CHECK_INTERVAL_SHIFT
#define CN1_PACING_CHECK_INTERVAL_SHIFT 20
#endif
#define CN1_PACING_CHECK_INTERVAL_BYTES (1LL << CN1_PACING_CHECK_INTERVAL_SHIFT)

// Cached free-memory reading, refreshed once per GC cycle (cn1RefreshFreeMemCache, called from
// codenameOneGCMark) so the dynamic pacing cap costs no per-page-acquire syscall.
_Atomic long cn1CachedFreeMem = 0;
// This process's own metered size, sampled on the same once-per-cycle cadence. Read by
// the pacing cap to decide whether the growth bound above applies; 0 where the platform
// has no probe, which reads as "not large" and leaves the cap alone.
static _Atomic long long cn1CachedProcFootprint = 0;
// TEST HOOK. CN1_SIMULATE_FREE_MEMORY=<bytes> substitutes a fixed reading for the host's
// available memory. The dynamic pacing cap is a FRACTION of that reading, so how far a
// mutator may run ahead of the collector -- and therefore, off a per-process ceiling,
// how large the process gets -- depends on how much RAM the machine happened to have
// free. That makes the issue-5537 growth shape reproduce on an idle developer machine
// and vanish on a busy one, in both directions, which is no basis for a guard: without
// this hook the same test passes for opposite reasons on the same host an hour apart.
// Off unless set. -1 = env not probed yet. long long for the LLP64 target.
static _Atomic long long cn1SimulatedFreeMem = -1;
static long long cn1SimulatedFreeMemBytes(void) {
    long long v = atomic_load_explicit(&cn1SimulatedFreeMem, memory_order_relaxed);
    if(v < 0) {
        const char* e = getenv("CN1_SIMULATE_FREE_MEMORY");
        v = e ? atoll(e) : 0;
        if(v < 0) {
            v = 0;
        }
        atomic_store_explicit(&cn1SimulatedFreeMem, v, memory_order_relaxed);
    }
    return v;
}
void cn1RefreshFreeMemCache(void) {
    long long simFree = cn1SimulatedFreeMemBytes();
    atomic_store_explicit(&cn1CachedFreeMem,
                          simFree > 0 ? (long)simFree : cn1_available_memory(),
                          memory_order_relaxed);
    atomic_store_explicit(&cn1CachedProcFootprint, (long long)cn1ProcFootprintBytes(),
                          memory_order_relaxed);
}

// DYNAMIC PACING CAP (perf-tier1). The fixed 3x-trigger cap starves a high-throughput allocator:
// a thread that allocates faster than the collector (e.g. the EDT decoding/parsing a heavy
// vector-map tile) is parked in cn1BibopMaybeGc's backpressure spin for most of each GC cycle,
// so the render is serialized behind the collector instead of overlapping it (~20% of wall on
// the MvtBench repro, larger on device). Instead of a constant, allow the thread to run further
// ahead of the collector when free RAM is ample (they overlap -> the fast thread is not starved)
// and tighten toward the static cap as free memory shrinks (RSS stays bounded). The EDT -- the
// thread we most want to keep responsive -- gets double headroom; it must never be throttled
// unless memory is genuinely tight. Never returns LESS than the old static cap, so no workload
// gets a tighter bound than before. -DCN1_BIBOP_NO_PACING still disables pacing entirely for A/B.
static void cn1BibopUpdateThreadPolicy(CODENAME_ONE_THREAD_STATE) {
    int epoch = atomic_load_explicit(&bibopGcEpoch, memory_order_relaxed);
    if(threadStateData->bibopObservedGcEpoch != epoch) {
        threadStateData->bibopObservedGcEpoch = epoch;
        threadStateData->bibopEpochBytes = 0;
    }
    if(threadStateData->bibopEpochBytes >= CN1_BIBOP_HIGH_THROUGHPUT_BYTES &&
       threadStateData->bibopHighThroughputUntilEpoch < epoch + 2) {
        threadStateData->bibopHighThroughputUntilEpoch = epoch + 2;
#ifdef CN1_GC_INSTRUMENT
        atomic_fetch_add_explicit(&cn1BibopHighThroughputPromotions, 1,
                                  memory_order_relaxed);
#endif
    }
    // Survivor-heavy size classes publish a new generation at sweep. Threads
    // consume that signal only on their rare page-acquire path, then route a
    // bounded allocation sample through the legacy collector before reprobing.
    for(int ci = 0 ; ci < CN1_BIBOP_NUM_CLASSES ; ci++) {
        int generation = atomic_load_explicit(&bibopBypassGeneration[ci],
                                               memory_order_relaxed);
        if(threadStateData->bibopBypassSeen[ci] != generation) {
            threadStateData->bibopBypassSeen[ci] = generation;
            threadStateData->bibopBypassRemaining[ci] = CN1_BIBOP_BYPASS_ALLOCATIONS;
        }
    }
}

// Monotonic stamp of the last footprint probe taken by cn1PacingPastGrowthFloor.
static _Atomic JAVA_LONG cn1ProcFootprintStampMs = 0;

// Is this process past the size at which the run-ahead bound applies?
//
// Answers from the once-per-cycle sample while that says YES -- the footprint of a
// process that has crossed the floor does not fall back under it without a sweep, which
// refreshes the sample anyway. While it says NO the sample is the one that can be stale
// in the direction that matters, so re-probe it, rate-limited to one syscall per
// CN1_PACING_FOOTPRINT_REFRESH_MS across all threads. That bounds how far the mutator can
// run past the floor before the bound engages to one refresh interval's worth of
// allocation, instead of one COLLECTION's worth.
static JAVA_BOOLEAN cn1PacingPastGrowthFloor(void) {
    if(atomic_load_explicit(&cn1CachedProcFootprint, memory_order_relaxed)
            > CN1_PACING_GROWTH_FLOOR_BYTES) {
        return JAVA_TRUE;
    }
    JAVA_LONG now = cn1MonotonicMillis();
    JAVA_LONG last = atomic_load_explicit(&cn1ProcFootprintStampMs, memory_order_relaxed);
    if(now - last < CN1_PACING_FOOTPRINT_REFRESH_MS) {
        return JAVA_FALSE;      // probed recently and it was under; believe that
    }
    if(!atomic_compare_exchange_strong_explicit(&cn1ProcFootprintStampMs, &last, now,
                                                memory_order_relaxed,
                                                memory_order_relaxed)) {
        return JAVA_FALSE;      // another thread is taking this interval's probe
    }
    long long fp = (long long)cn1ProcFootprintBytes();
    if(fp <= 0) {
        return JAVA_FALSE;      // no probe on this platform; the bound stays off
    }
    atomic_store_explicit(&cn1CachedProcFootprint, fp, memory_order_relaxed);
    return fp > CN1_PACING_GROWTH_FLOOR_BYTES;
}

static long cn1BibopPacingCap(CODENAME_ONE_THREAD_STATE) {
    long trigger = atomic_load_explicit(&bibopGcTriggerBytes, memory_order_relaxed);
    long base = trigger * CN1_BIBOP_GC_HARD_CAP_MULTIPLIER;
    long fm = atomic_load_explicit(&cn1CachedFreeMem, memory_order_relaxed);
    long cap = fm / 8;                                       // baseline: 1/8 of available RAM of slack
    if(cap < base) cap = base;                              // never tighter than before
    // High-throughput threads must not be starved by the collector. The EDT (UI/render thread)
    // always qualifies; so does any thread allocating hard RIGHT NOW (heapAllocationSize is its
    // legacy allocations since the last GC, reset each cycle) -- e.g. a worker decoding a heavy
    // vector-map tile. Give them up to 1/2 of AVAILABLE RAM of headroom so they keep running
    // while the concurrent GC catches up, instead of parking in the backpressure spin. Still
    // bounded by real available memory, so RSS stays safe and the collector reclaims the churn.
    if(isEdt(threadStateData->threadId)
       || threadStateData->bibopHighThroughputUntilEpoch >= threadStateData->bibopObservedGcEpoch
       || threadStateData->heapAllocationSize > CN1_BIBOP_HIGH_THROUGHPUT_ALLOCS) {
        long hi = fm / 2;
        if(hi > cap) cap = hi;
    }
    // Bound the widening, once this process has shown it can grow. Never below the
    // static cap, which is what "never tighter than before" means once the multiplier is
    // applied to the same trigger.
    //
    // The footprint is READ FRESH here rather than off the once-per-cycle cache, and the
    // order matters: ask whether the bound would bind at all first, so the probe is paid
    // for only on the path that needs it. A cycle that starts just under the floor and
    // then runs long would otherwise keep a below-floor reading for its whole duration --
    // and a long cycle is exactly the runaway this bound exists to stop, so the clamp
    // would sit disarmed through the one interval that matters. At a couple of GB/s a
    // 750ms cycle is more than a gigabyte of that.
    {
        long capCeiling = trigger * CN1_BIBOP_GC_MAX_CAP_MULTIPLIER;
        if(capCeiling < base) {
            capCeiling = base;
        }
        if(cap > capCeiling && cn1PacingPastGrowthFloor()) {
            cap = capCeiling;
        }
    }
    if(cn1PacingTraceOn()) {
        long seen = atomic_load_explicit(&cn1PacingMinCap, memory_order_relaxed);
        while(cap < seen &&
              !atomic_compare_exchange_weak_explicit(&cn1PacingMinCap, &seen, cap,
                                                     memory_order_relaxed,
                                                     memory_order_relaxed)) {
            // seen was reloaded by the failed exchange; retry only while still smaller.
        }
    }
    return cap;
}

// Backpressure: bound the footprint when the collector falls behind, by parking the
// allocating thread until uncollected volume (*counter) drops back under the cap.
//
// This wait MUST be a GC safepoint -- otherwise the collector blocks waiting for this
// spinning thread to become scannable and never advances, so the counter never resets
// and the spin livelocks (observed as an MtStress hang). Mark the thread inactive (as
// the legacy alloc-path park does) so the collector can scan/pass it; restore on exit.
// Bounded spin with a safety cap so a dead/stuck GC degrades to footprint growth,
// never a permanent hang.
//
// Shared by both allocation paths, which is the point: they hold separate counters and
// only the BiBOP one was ever paced (issue #5537). Under a budget they are paced against
// the SUM of the two, since that is what spends the budget; without one they keep their
// own separate bounds, so no path off iOS gets a tighter bound than it had -- the legacy
// path simply gains one it never had. See cn1PacingVolume.
// Which uncollected-volume counter a park is waiting on. The two are separate types
// (the legacy one is long long: on the Windows LLP64 target long is 32-bit and it
// accumulates raw allocation bytes), so the caller names the counter rather than
// passing a pointer to it.
#define CN1_PACE_BIBOP  0
#define CN1_PACE_LEGACY 1

static long long cn1PacingVolume(int which) {
    if(which == CN1_PACE_LEGACY) {
        return atomic_load_explicit(&cn1LegacyBytesSinceGc, memory_order_relaxed);
    }
    return (long long)atomic_load_explicit(&bibopBytesSinceGc, memory_order_relaxed);
}

// Atomically admit this thread if the live budget, minus what other threads have already
// been admitted to dirty, still covers this block plus the margin. Test and claim must be
// one step: a plain check followed by a separate add lets every waiter observe the same
// pre-claim total and admit together, which is the whole failure this guards.
static JAVA_BOOLEAN cn1PacingTryAdmit(long headroom, long long need, long long pendingBytes) {
    long long claimed = atomic_load_explicit(&cn1PacingClaimed, memory_order_relaxed);
    for(;;) {
        if((long long)headroom - claimed < need) {
            return JAVA_FALSE;
        }
        if(atomic_compare_exchange_weak_explicit(&cn1PacingClaimed, &claimed,
                                                 claimed + pendingBytes,
                                                 memory_order_acq_rel,
                                                 memory_order_relaxed)) {
            cn1MyPacingClaim += pendingBytes;
            return JAVA_TRUE;
        }
        // claimed was reloaded by the failed exchange; re-test against the new total.
    }
}

static void cn1PacingPark(CODENAME_ONE_THREAD_STATE, int which, long long pendingBytes) {
    if(get_static_java_lang_System_gcThreadInstance() == JAVA_NULL) {
        return;
    }
    long procHeadroom = cn1ProcessHeadroom();
    if(procHeadroom < 0) {
        // ---- NO PER-PROCESS CEILING -------------------------------------------------
        // Unchanged behaviour. The legacy path is not paced at all here: its
        // backpressure exists to keep a process inside a hard ceiling, and off Apple
        // cn1_available_memory is a flat 100MB placeholder, so pacing against it would
        // engage constantly on machines in no danger (measured: a 768MB churn parked 10
        // times on a Linux runner). The BiBOP path keeps the volume cap it always had.
        if(which == CN1_PACE_LEGACY) {
            return;
        }
        long cap = cn1BibopPacingCap(threadStateData);
        if(cn1PacingVolume(which) <= (long long)cap) {
            return;
        }
        if(cn1PacingTraceOn()) {
            atomic_fetch_add_explicit(&cn1PacingParksBibop, 1, memory_order_relaxed);
        }
        CN1_GC_PARK_CAPTURE(threadStateData);
        threadStateData->threadActive = JAVA_FALSE;
        int spins = 0;
        while(cn1PacingVolume(which) > (long long)cap &&
              get_static_java_lang_System_gcThreadInstance() != JAVA_NULL &&
              spins++ < 200000) {
            usleep(50);
        }
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(500));
        }
        threadStateData->threadActive = JAVA_TRUE;
        return;
    }

    // ---- UNDER A PER-PROCESS CEILING (issue #5537) -----------------------------------
    // Admission is decided by the LIVE remaining budget, not by an allocation-volume
    // counter. That indirection is what made every earlier version of this wrong, and
    // each defect was a different way for the counter to diverge from the truth: threads
    // deferring bytes into per-thread accumulators the counter could not see; the
    // start-of-marking reset erasing blocks that were allocated but not yet dirtied;
    // simultaneous waiters all observing that reset before any of them re-charged; the
    // two allocation paths each running a full cap ahead of a cap derived from the same
    // budget. phys_footprint has none of those failure modes: the kernel maintains it,
    // every thread and every non-Java allocation is already in it, and it is the exact
    // figure the process is killed against. So ask it directly.
    //
    // What phys_footprint does NOT include is a block that has been allocated but not yet
    // written -- calloc'd pages cost nothing until touched. That window is why admission
    // still needs a shared reservation: without one, N mutators all read the same
    // headroom before any of them dirties anything and each independently concludes it
    // fits. cn1PacingClaimed carries those in-flight blocks so every thread sees what the
    // others are about to spend.
    //
    // Release this thread's previous claim first: by the time it allocates again, the
    // earlier block has been written and the kernel has counted it, so holding the claim
    // any longer would double-charge it.
    cn1PacingExpireThreadClaim();
    long long need = pendingBytes + CN1_PACING_HEADROOM_MARGIN;
    if(cn1PacingTraceOn()) {
        atomic_fetch_add_explicit(&cn1PacingBoundedChecks, 1, memory_order_relaxed);
        long seen = atomic_load_explicit(&cn1PacingMinHeadroom, memory_order_relaxed);
        while((seen < 0 || procHeadroom < seen) &&
              !atomic_compare_exchange_weak_explicit(&cn1PacingMinHeadroom, &seen,
                                                     procHeadroom,
                                                     memory_order_relaxed,
                                                     memory_order_relaxed)) {
        }
    }
    JAVA_BOOLEAN admitted = cn1PacingTryAdmit(procHeadroom, need, pendingBytes);
    if(admitted) {
        return;
    }
    if(cn1PacingTraceOn()) {
        atomic_fetch_add_explicit(which == CN1_PACE_LEGACY ? &cn1PacingParksLegacy
                                                           : &cn1PacingParksBibop,
                                  1, memory_order_relaxed);
    }
    CN1_GC_PARK_CAPTURE(threadStateData);   // fresh capture for the coop conservative scan
    threadStateData->threadActive = JAVA_FALSE;
    int spins = 0;
    int lastEpoch = atomic_load_explicit(&bibopGcEpoch, memory_order_relaxed);
    int barrenCycles = 0;
    long bestHeadroom = procHeadroom;
    while(spins < CN1_PACING_MAX_WAIT_SPINS &&
          barrenCycles < CN1_PACING_BARREN_CYCLES &&
          get_static_java_lang_System_gcThreadInstance() != JAVA_NULL) {
        // Keep asking for collection while we wait. Requesting once is not enough: a
        // parked thread allocates nothing, so isHighFrequencyGC() goes false and the
        // collector drops to its 30s idle wait -- and we would then sit out the whole
        // budget waiting for reclamation nobody was doing.
        if((spins % CN1_PACING_GC_REQUEST_SPINS) == 0) {
            JAVA_BOOLEAN wasNam = threadStateData->nativeAllocationMode;
            threadStateData->nativeAllocationMode = JAVA_TRUE;
            java_lang_System_gc__(threadStateData);
            threadStateData->nativeAllocationMode = wasNam;
        }
        usleep((JAVA_INT)CN1_PACING_WAIT_SLEEP_US);
        spins++;
        long headroomNow = cn1ProcessHeadroom();
        if(headroomNow < 0) {
            break;                       // budget disappeared under us; nothing to honour
        }
        if(cn1PacingTryAdmit(headroomNow, need, pendingBytes)) {
            admitted = JAVA_TRUE;
            break;
        }
        // Decide whether waiting is still buying anything, on COMPLETED collections
        // rather than on elapsed time. bibopGcEpoch is published at cycle start, so an
        // advance means the previous cycle's mark and sweep both finished.
        int epochNow = atomic_load_explicit(&bibopGcEpoch, memory_order_relaxed);
        if(epochNow != lastEpoch) {
            lastEpoch = epochNow;
            if(headroomNow > bestHeadroom + CN1_PACING_PROGRESS_EPSILON) {
                bestHeadroom = headroomNow;
                barrenCycles = 0;        // still returning memory; keep waiting
            } else {
                barrenCycles++;          // a whole cycle bought nothing
            }
        }
    }
    // Proceeding here rather than failing the allocation is deliberate: ParparVM has no
    // way to fail one. codenameOneGcMalloc answers a NULL calloc by forcing a cycle and
    // recursing, so there is no OutOfMemoryError path to reuse, and the block is already
    // allocated and registered by this point. Adding one belongs in its own change.
    //
    // GIVING UP STILL DIRTIES THE BLOCK. If the wait ended on barren cycles or the
    // backstop rather than on admission, this thread proceeds to write its block anyway
    // -- so the block has to be claimed regardless, or it is invisible to every other
    // thread's admission test for the window before the kernel counts it. Claiming only
    // on the success path would leave exactly the over-admission the claim exists to
    // prevent, in the case where memory is tightest.
    if(!admitted && pendingBytes > 0) {
        atomic_fetch_add_explicit(&cn1PacingClaimed, pendingBytes, memory_order_relaxed);
        cn1MyPacingClaim += pendingBytes;
    }
    // Honour a stop-the-world before resuming, exactly like every other park here: the
    // loop above can exit while a mark is still running and the collector believes this
    // thread is paused.
    while(threadStateData->threadBlockedByGC) {
        usleep((JAVA_INT)(500));
    }
    threadStateData->threadActive = JAVA_TRUE;
}

static void cn1BibopMaybeGc(CODENAME_ONE_THREAD_STATE) {
    // LEVER A: flush this thread's plain-add byte accumulator into the global atomic
    // (once per page-acquire). No-op only with -DCN1_DISABLE_DEATOMIC_BYTES.
    CN1_BIBOP_FLUSH_BYTES(threadStateData);
    cn1BibopUpdateThreadPolicy(threadStateData);
    if(constantPoolObjects == 0) {
        return;
    }
#ifndef CN1_CONSERVATIVE_GC_ROOTS
    // Without conservative roots, a thread inside a native-allocation bracket
    // must be neither parked nor made to trigger a cycle (its half-built
    // objects aren't rooted). Under conservative roots the native C locals ARE
    // scanned, so both are safe -- and skipping here was starving the 24MB
    // trigger entirely on workloads whose every allocation happens inside a
    // native (e.g. StringBuilder.toString): the GC thread then slept its idle
    // wait while dead pages accumulated into the GB range.
    if(threadStateData->nativeAllocationMode) {
        return;
    }
#endif
    // GC SAFEPOINT (once per page-acquire): a thread allocating ONLY small BiBOP
    // objects never reaches the legacy alloc path's pending-buffer park, so without
    // this check the collector's while(threadActive) wait is satisfied only by
    // monitor/sleep/native yields -- a tight allocation loop could stall the GC's
    // root scan for a long time. Same idiom as the legacy park: capture the native
    // stack for the cooperative conservative scan, mark inactive, wait out the GC.
    if(threadStateData->threadBlockedByGC) {
        CN1_GC_PARK_CAPTURE(threadStateData);
        threadStateData->threadActive = JAVA_FALSE;
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(500));
        }
        threadStateData->threadActive = JAVA_TRUE;
    }
    long __gcTrigger = atomic_load_explicit(&bibopGcTriggerBytes, memory_order_relaxed);
    if(!gcCurrentlyRunning &&
       atomic_load_explicit(&bibopBytesSinceGc, memory_order_relaxed) > __gcTrigger) {
        // save/restore: we may already be INSIDE a caller's native-allocation
        // bracket (reachable here under CN1_CONSERVATIVE_GC_ROOTS)
        JAVA_BOOLEAN wasNam = threadStateData->nativeAllocationMode;
        threadStateData->nativeAllocationMode = JAVA_TRUE;
        java_lang_System_gc__(threadStateData);
        threadStateData->nativeAllocationMode = wasNam;
    }
#ifndef CN1_BIBOP_NO_PACING
    // 0 pending: a BiBOP thread dirties at most one CN1_BIBOP_PAGE_SIZE page before its
    // next page-acquire check, which the interval reservation already covers.
    cn1PacingPark(threadStateData, CN1_PACE_BIBOP, 0);
#endif
}

// Retire the thread's current page for class ci (if any) onto the global SWEEP
// stack and adopt a PARTIAL (preferred) or FREE page, formatting a fresh one
// only as a last resort. Runs on the owning thread.
static CN1BibopPage* cn1BibopAcquirePage(int ci) {
    pthread_mutex_lock(&bibopMutex);
    CN1BibopPage* old = bibopCurrent[ci];
    if(old != 0) {
        old->owned = JAVA_FALSE;
        // push onto the SWEEP stack (single producer here holds bibopMutex, but
        // the sweep swaps the head atomically, so use an atomic CAS push).
        CN1BibopPage* sh = atomic_load_explicit(&bibopSweepStack, memory_order_relaxed);
        do {
            old->nextPool = sh;
        } while(!atomic_compare_exchange_weak_explicit(&bibopSweepStack, &sh, old,
                    memory_order_release, memory_order_relaxed));
        bibopCurrent[ci] = 0;
    }
    CN1BibopPage* np = bibopPartialPool[ci];
    if(np != 0) {
        bibopPartialPool[ci] = np->nextPool;
    } else if(bibopFreePool != 0) {
        np = bibopFreePool;
        bibopFreePool = np->nextPool;
        cn1BibopFormatPage(np, ci);
    } else if(bibopReleasedPool != 0 || bibopReuseFailedPool != 0) {
        // Warm pages are gone; take one whose slot region was handed back to the
        // OS. The REUSE call must precede the format, which writes into that
        // region (and under CN1_GC_VERIFY writes every slot).
        //
        // A page whose restore FAILS must not go back at the head: every later
        // acquisition would pop the same page, fail again, and allocate a fresh
        // arena page, so a single unrestorable page would hide an entire stocked
        // pool behind it and the heap would grow without bound. Failures are
        // parked on a separate list that is only consulted once the good pool is
        // empty, which both keeps them out of the way and still retries them
        // eventually if the cause was transient.
        for(int attempt = 0 ; np == 0 && attempt < CN1_BIBOP_REUSE_ATTEMPTS ; attempt++) {
            CN1BibopPage* cand;
            if(bibopReleasedPool != 0) {
                cand = bibopReleasedPool;
                bibopReleasedPool = cand->nextPool;
            } else if(attempt == 0 && bibopReuseFailedPool != 0) {
                // Only ever one previously-failed page per acquisition, so a
                // permanently unrestorable page costs one syscall and never
                // starves the fresh-page fallback. Taken from the HEAD and, on
                // failure, returned to the TAIL, so successive acquisitions
                // rotate through the parked pages instead of retrying one.
                cand = bibopReuseFailedPool;
                bibopReuseFailedPool = cand->nextPool;
                if(bibopReuseFailedPool == 0) {
                    bibopReuseFailedTail = 0;
                }
            } else {
                break;
            }
            if(cn1BibopReusePageMemory(cand)) {
                np = cand;
                cn1BibopFormatPage(np, ci);
            } else {
                cn1BibopParkReuseFailure(cand);
            }
        }
    }
    pthread_mutex_unlock(&bibopMutex);
    if(np == 0) {
        np = cn1BibopNewPage(ci);
        if(np == 0) {
            return 0;
        }
    }
    np->owned = JAVA_TRUE;
    np->nextPool = 0;
    bibopCurrent[ci] = np;
    return np;
}

// Initialize a freshly-claimed slot's header EXACTLY like codenameOneGcMalloc,
// publishing the mark field LAST with an atomic release store so a concurrent
// overflow-rescan never observes a half-initialized object as live (its mark
// goes oldDead/FREE -> -1, never through the current epoch). Only the object
// body (after the fixed header) is zeroed, never the mark word, so there is no
// plain-write-vs-atomic-read race on the mark.
static inline void cn1BibopInitSlot(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o, int size, struct clazz* parent) {
    int hdr = (int)sizeof(struct JavaObjectPrototype);
    if(size > hdr) {
        memset((char*)o + hdr, 0, size - hdr);
    }
    o->__codenameOneParentClsReference = parent;
    // __codenameOneReferenceCount + __codenameOneThreadData relocated out of the header
    // (force-visited / monitor side tables); no per-object stores.
    o->__heapPosition = CN1_BIBOP_HEAP_POS;
#ifdef DEBUG_GC_ALLOCATIONS
    o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
    o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
    __atomic_store_n(&o->__codenameOneGcMark, -1, __ATOMIC_RELEASE);
}

#endif /* CN1_DISABLE_BIBOP */

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// ============================ Exact clazz registry ============================
// The conservative scan/drain can hand gcMarkObject a pointer to a FREED object
// whose first header word (over __codenameOneParentClsReference) now holds
// arbitrary reused data. Validating that value with a "close to the code"
// distance heuristic proved UNSOUND: on a non-PIE Linux binary loaded low, the
// whole malloc heap sits within the +-512MB window, so a freed object whose
// offset-0 word was a heap pointer passed the filter -- and when that heap page
// had been returned to the OS, the parentCls->markFunction LOAD itself faulted
// (the recurred arm64 suite SIGSEGV at cn1_globals.c:3751).
//
// Replace the heuristic with an EXACT registry of every genuine clazz address:
// every allocation entry point (cn1BibopFastAlloc/NoZero in the header,
// cn1BibopAlloc, codenameOneGcMalloc -- allocArray funnels into the latter)
// registers the class on its first allocation, so by construction the registry
// contains the clazz of every object the GC can ever encounter -- including
// objects later made immortal and removed from the heap table. Lookup is a
// lock-free open-addressing probe; insert is a CAS (idempotent, grow-never:
// the table is sized far beyond any real app's class count).
#define CN1_CLAZZ_SET_BITS 15
#define CN1_CLAZZ_SET_SIZE (1 << CN1_CLAZZ_SET_BITS)
static _Atomic(uintptr_t) cn1ClazzSet[CN1_CLAZZ_SET_SIZE];
static _Atomic(int) cn1ClazzSetCount;

static inline int cn1ClazzSetSlot(uintptr_t v) {
    // clazz statics are pointer-aligned; fold the address into the table.
    uintptr_t h = (v >> 4) * (uintptr_t)2654435761u;
    return (int)(h & (CN1_CLAZZ_SET_SIZE - 1));
}

// TRUE when c is a registered (genuine) clazz address. Never dereferences c.
// Acquire pairs with the release CAS in cn1GcRegisterClazz: an object is
// published to the GC only after its allocation entry point registered the
// class, so a GC thread that sees the object also sees the registration.
static int cn1ClazzRegistryContains(uintptr_t v) {
    int i = cn1ClazzSetSlot(v);
    for(;;) {
        uintptr_t cur = atomic_load_explicit(&cn1ClazzSet[i], memory_order_acquire);
        if(cur == v) {
            return 1;
        }
        if(cur == 0) {
            return 0;
        }
        i = (i + 1) & (CN1_CLAZZ_SET_SIZE - 1);
    }
}

void cn1GcRegisterClazz(struct clazz* c) {
    uintptr_t v = (uintptr_t)c;
    if(v == 0) {
        return;
    }
    // Safety valve: a table this size can never fill from real classes (tens of
    // thousands of slots vs a few thousand classes); if it somehow neared full,
    // stop inserting -- lookups then miss and the guard falls back to the
    // authoritative resolver, which is correct just slower.
    if(atomic_load_explicit(&cn1ClazzSetCount, memory_order_relaxed) > (CN1_CLAZZ_SET_SIZE / 2)) {
        return;
    }
    int i = cn1ClazzSetSlot(v);
    for(;;) {
        uintptr_t cur = atomic_load_explicit(&cn1ClazzSet[i], memory_order_acquire);
        if(cur == v) {
            break; // already registered (racing first allocations -- fine)
        }
        if(cur == 0) {
            uintptr_t expected = 0;
            if(atomic_compare_exchange_strong_explicit(&cn1ClazzSet[i], &expected, v,
                    memory_order_release, memory_order_acquire)) {
                atomic_fetch_add_explicit(&cn1ClazzSetCount, 1, memory_order_relaxed);
                break;
            }
            if(expected == v) {
                break; // another thread inserted the same clazz
            }
            // slot taken by a different clazz -> keep probing
        }
        i = (i + 1) & (CN1_CLAZZ_SET_SIZE - 1);
    }
    // Plain flag store AFTER the table insert: the flag is only an allocation-
    // side fast-path skip; the GC never reads it (it probes the table).
    c->cn1ClazzRegistered = JAVA_TRUE;
}

#if defined(CN1_ALLOC_CENSUS) && defined(__APPLE__)
// cn1HeapAccounting weighs legacy-heap blocks with malloc_size (the object
// header does not record instance size). The other include of this header is
// scoped to CN1_GC_VERIFY builds, so the census needs its own.
#include <malloc/malloc.h>
#endif

#ifdef CN1_ALLOC_CENSUS
/**
 * Prints allocation volume by class, biggest first.
 *
 * Deliberately a census of what was ALLOCATED rather than of what is live: churn
 * is what costs, and a live-object walk cannot see the BiBOP or nursery objects
 * at all (they never enter allObjectsInHeap), which is exactly where the small
 * high-turnover objects sit. Counters are read without synchronisation; a
 * diagnostic wants the shape, not the last digit.
 */
/**
 * Separates the JAVA heap from native allocation.
 *
 * vmmap cannot do this: BiBOP arenas are posix_memalign'd so they land in
 * MALLOC_LARGE and the legacy heap in MALLOC_SMALL, side by side with every
 * Metal, CoreGraphics and image buffer the process owns. Comparing "our malloc
 * total" against another runtime's figures therefore compares a heap against a
 * heap PLUS a renderer. These numbers are the heap on its own.
 *
 * Reserved is what BiBOP has taken from the allocator; live is what objects
 * actually occupy. The difference is the honest cost of the page pool: BiBOP
 * never frees a page back per-object, it pools swept pages by size class.
 */
void cn1HeapAccounting(const char* label) {
    long long pages = 0, capBytes = 0, liveBytes = 0, ownedPages = 0, emptyPages = 0;
    CN1BibopPage* p = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
    while(p != 0) {
        pages++;
        capBytes += CN1_BIBOP_PAGE_SIZE;
        int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
        int live = bi - p->freeCount;
        if(live < 0) {
            live = 0;
        }
        if(live == 0) {
            emptyPages++;
        }
        liveBytes += (long long)live * (long long)p->slotSize;
        if(p->owned) {
            ownedPages++;
        }
        p = atomic_load_explicit(&p->nextAll, memory_order_acquire);
    }
    // The legacy heap is NOT part of the BiBOP figures above and is easy to
    // forget: objects too big for the largest size class go through calloc and
    // the allObjectsInHeap table instead. Reporting only the BiBOP total
    // understates the Java heap by whatever these weigh, so weigh them --
    // malloc_size gives the true block size for a calloc'd pointer.
    long long legacyBytes = 0, legacyLive = 0;
    int nHeap = currentSizeOfAllObjectsInHeap;
    for(int i = 0 ; i < nHeap ; i++) {
        JAVA_OBJECT o = allObjectsInHeap[i];
        if(o == JAVA_NULL) {
            continue;
        }
        legacyLive++;
#if defined(__APPLE__)
        legacyBytes += (long long)malloc_size((void*)o);
#endif
    }
    fprintf(stderr,
            "[JHEAP:%s] bibop pages=%lld reserved=%.2fMB live=%.2fMB slack=%.2fMB "
            "(owned=%lld empty=%lld) | legacy objects=%lld bytes=%.2fMB | "
            "JAVA TOTAL live=%.2fMB resident=%.2fMB\n",
            label, pages, capBytes / 1048576.0, liveBytes / 1048576.0,
            (capBytes - liveBytes) / 1048576.0, ownedPages, emptyPages,
            legacyLive, legacyBytes / 1048576.0,
            (liveBytes + legacyBytes) / 1048576.0,
            (capBytes + legacyBytes) / 1048576.0);
    fflush(stderr);
}

void cn1AllocCensus(const char* label) {
    struct Row { const char* name; long count; long bytes; };
    static struct Row rows[4096];
    int used = 0;
    long long totalBytes = 0, totalCount = 0;
    for(int i = 0 ; i < CN1_CLAZZ_SET_SIZE ; i++) {
        uintptr_t v = atomic_load_explicit(&cn1ClazzSet[i], memory_order_relaxed);
        if(v == 0) {
            continue;
        }
        struct clazz* c = (struct clazz*)v;
        if(c->cn1AllocBytes == 0 && c->cn1AllocCount == 0) {
            continue;
        }
        totalBytes += c->cn1AllocBytes;
        totalCount += c->cn1AllocCount;
        if(used < 4096) {
            rows[used].name = c->clsName ? c->clsName : "?";
            rows[used].count = c->cn1AllocCount;
            rows[used].bytes = c->cn1AllocBytes;
            used++;
        }
    }
    fprintf(stderr, "[ALLOC:%s] %lld objects, %lld bytes (%.1fMB) across %d classes\n",
            label, totalCount, totalBytes, totalBytes / (1024.0 * 1024.0), used);
    for(int shown = 0 ; shown < 30 ; shown++) {
        int best = -1;
        for(int i = 0 ; i < used ; i++) {
            if(rows[i].bytes > 0 && (best < 0 || rows[i].bytes > rows[best].bytes)) {
                best = i;
            }
        }
        if(best < 0) {
            break;
        }
        fprintf(stderr, "[ALLOC:%s]   %10ld bytes %9ld objs  %s\n",
                label, rows[best].bytes, rows[best].count, rows[best].name);
        rows[best].bytes = 0;
    }
    fflush(stderr);
}
#endif


// ========================== Immortal object registry ==========================
// Objects deliberately REMOVED from the heap table (interned constant-pool
// strings, static-final removal values, VM cache singletons) are unresolvable
// by cn1ConservativeResolve -- they live outside every BiBOP page and legacy
// extent -- yet they must still be TRACED: a static-final java.lang value (a
// Throwable, a String held in a field declared Object) can reference normal
// heap children that would otherwise be swept from under it. The mark guard in
// gcMarkObject therefore accepts a pointer when it resolves to itself OR when
// it is a registered immortal. Same lock-free set pattern as the clazz
// registry above; immortals are few (hundreds at most) and registered once.
#define CN1_IMMORTAL_SET_BITS 13
#define CN1_IMMORTAL_SET_SIZE (1 << CN1_IMMORTAL_SET_BITS)
static _Atomic(uintptr_t) cn1ImmortalObjSet[CN1_IMMORTAL_SET_SIZE];
static _Atomic(int) cn1ImmortalObjSetCount;

static inline int cn1ImmortalObjSlot(uintptr_t v) {
    uintptr_t h = (v >> 4) * (uintptr_t)2654435761u;
    return (int)(h & (CN1_IMMORTAL_SET_SIZE - 1));
}

static int cn1GcImmortalObjContains(JAVA_OBJECT o) {
    uintptr_t v = (uintptr_t)o;
    int i = cn1ImmortalObjSlot(v);
    for(;;) {
        uintptr_t cur = atomic_load_explicit(&cn1ImmortalObjSet[i], memory_order_acquire);
        if(cur == v) {
            return 1;
        }
        if(cur == 0) {
            return 0;
        }
        i = (i + 1) & (CN1_IMMORTAL_SET_SIZE - 1);
    }
}

void cn1GcRegisterImmortalObj(JAVA_OBJECT o) {
    uintptr_t v = (uintptr_t)o;
    if(v == 0) {
        return;
    }
    if(atomic_load_explicit(&cn1ImmortalObjSetCount, memory_order_relaxed) > (CN1_IMMORTAL_SET_SIZE / 2)) {
        return; // safety valve; cannot fill from real immortal counts
    }
    int i = cn1ImmortalObjSlot(v);
    for(;;) {
        uintptr_t cur = atomic_load_explicit(&cn1ImmortalObjSet[i], memory_order_acquire);
        if(cur == v) {
            return;
        }
        if(cur == 0) {
            uintptr_t expected = 0;
            if(atomic_compare_exchange_strong_explicit(&cn1ImmortalObjSet[i], &expected, v,
                    memory_order_release, memory_order_acquire)) {
                atomic_fetch_add_explicit(&cn1ImmortalObjSetCount, 1, memory_order_relaxed);
                return;
            }
            if(expected == v) {
                return;
            }
        }
        i = (i + 1) & (CN1_IMMORTAL_SET_SIZE - 1);
    }
}

// TRUE while the sweep (GC thread) is removing a DEAD object from the heap
// table prior to freeing it -- the one removeObjectFromHeapCollection caller
// whose intent is NOT "make this immortal". Single-writer (the sweep runs on
// the GC thread only), read in the same thread.
static JAVA_BOOLEAN cn1SweepRemoving = JAVA_FALSE;
#endif // CN1_CONSERVATIVE_GC_ROOTS

#ifndef CN1_DISABLE_BIBOP

// Allocate a small non-array object from the per-thread page for its size class.
// Returns 0 only if pages cannot be obtained (caller falls back to the heap).
static JAVA_OBJECT cn1BibopAlloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    pthread_once(&bibopOnce, cn1BibopDoInit);
    CN1_CLAZZ_REGISTER(parent);
    int ci = cn1BibopSizeToClass[size];
    if(ci < 0) {
        return 0;
    }
    if(threadStateData->bibopBypassRemaining[ci] > 0) {
        threadStateData->bibopBypassRemaining[ci]--;
#ifdef CN1_GC_INSTRUMENT
        atomic_fetch_add_explicit(&cn1BibopBypassAllocations, 1,
                                  memory_order_relaxed);
#endif
        return 0;
    }
    CN1BibopPage* p = bibopCurrent[ci];
    JAVA_OBJECT o = 0;
    for(;;) {
        if(p != 0) {
            if(p->freeList != 0) {
                o = (JAVA_OBJECT)p->freeList;
                p->freeList = *(void**)o;
                p->freeCount--;
                break;
            }
            int bi = atomic_load_explicit(&p->bumpIndex, memory_order_relaxed);
            if(bi < p->slotCount) {
                o = cn1BibopSlot(p, bi);
                cn1BibopInitSlot(threadStateData, o, size, parent);
                // publish the new cursor with release AFTER the slot (incl. its
                // mark) is fully initialized.
                atomic_store_explicit(&p->bumpIndex, bi + 1, memory_order_release);
#ifndef CN1_BIBOP_NO_FASTSWEEP
                // relaxed: concurrently read by the grace pass (see cn1BibopFastAlloc)
                __atomic_store_n(&p->gcAllocedSinceSweep, JAVA_TRUE, __ATOMIC_RELAXED);
#endif
                CN1_BIBOP_ACCOUNT_BYTES(threadStateData, p->slotSize);
                return o;
            }
        }
        // Need a fresh/partial page. This is the rare slow path (~once per page).
        cn1BibopMaybeGc(threadStateData);
        p = cn1BibopAcquirePage(ci);
        if(p == 0) {
            return 0; // out of pages -> legacy heap path
        }
        // loop: allocate from the freshly-acquired page (free-list or bump).
    }
    // free-list slot path
    cn1BibopInitSlot(threadStateData, o, size, parent);
#ifndef CN1_BIBOP_NO_FASTSWEEP
    // relaxed: concurrently read by the grace pass (see cn1BibopFastAlloc)
    __atomic_store_n(&p->gcAllocedSinceSweep, JAVA_TRUE, __ATOMIC_RELAXED);
#endif
    CN1_BIBOP_ACCOUNT_BYTES(threadStateData, p->slotSize);
    return o;
}

#endif /* CN1_DISABLE_BIBOP */

// ---- Monitor side table (relocated __codenameOneThreadData out of the object header) ----
// The lazily-attached per-object monitor (CN1ThreadData*) is NULL on virtually every
// object, so storing it in every header wasted 8 bytes/object. It now lives in an
// address-keyed chained hash map; only objects that are actually monitorEnter'd ever
// get an entry. All ops take a single dedicated mutex (monitor ops are rare relative to
// allocation). Lock discipline: callers NEVER hold this mutex across lockCriticalSection
// or across a blocking pthread_mutex_lock(data->mutex) -- the data pointer is copied out
// and the table mutex released first -- so there is no inversion with the GC critical
// section (which only ever takes the table mutex AFTER it, during reclaim/free).
struct CN1MonitorEntry { JAVA_OBJECT key; void* data; struct CN1MonitorEntry* next; };
#define CN1_MON_BUCKETS 4096
static struct CN1MonitorEntry* cn1MonitorBuckets[CN1_MON_BUCKETS];
static pthread_mutex_t cn1MonitorTableMutex = PTHREAD_MUTEX_INITIALIZER;

static inline unsigned cn1MonHash(JAVA_OBJECT o) {
    uintptr_t p = (uintptr_t)o;
    p >>= 4; // objects are at least 16-byte aligned
    return (unsigned)((p ^ (p >> 16)) & (CN1_MON_BUCKETS - 1));
}

// Lookup: returns the attached CN1ThreadData* (or 0). Safe for concurrent callers.
void* cn1MonitorDataGet(JAVA_OBJECT o) {
    unsigned h = cn1MonHash(o);
    pthread_mutex_lock(&cn1MonitorTableMutex);
    struct CN1MonitorEntry* e = cn1MonitorBuckets[h];
    void* r = 0;
    while(e) { if(e->key == o) { r = e->data; break; } e = e->next; }
    pthread_mutex_unlock(&cn1MonitorTableMutex);
    return r;
}

// Insert or overwrite the monitor for o.
void cn1MonitorDataSet(JAVA_OBJECT o, void* data) {
    unsigned h = cn1MonHash(o);
    pthread_mutex_lock(&cn1MonitorTableMutex);
    struct CN1MonitorEntry* e = cn1MonitorBuckets[h];
    while(e) { if(e->key == o) { e->data = data; pthread_mutex_unlock(&cn1MonitorTableMutex); return; } e = e->next; }
    e = (struct CN1MonitorEntry*)malloc(sizeof(struct CN1MonitorEntry));
    e->key = o; e->data = data; e->next = cn1MonitorBuckets[h];
    cn1MonitorBuckets[h] = e;
    pthread_mutex_unlock(&cn1MonitorTableMutex);
}

// Remove o's entry and return its data (or 0 if none). The caller frees the data.
void* cn1MonitorDataRemove(JAVA_OBJECT o) {
    unsigned h = cn1MonHash(o);
    pthread_mutex_lock(&cn1MonitorTableMutex);
    struct CN1MonitorEntry** pp = &cn1MonitorBuckets[h];
    void* r = 0;
    while(*pp) {
        if((*pp)->key == o) {
            struct CN1MonitorEntry* d = *pp;
            r = d->data; *pp = d->next; free(d);
            break;
        }
        pp = &(*pp)->next;
    }
    pthread_mutex_unlock(&cn1MonitorTableMutex);
    return r;
}

#ifndef CN1_DISABLE_BIBOP

// Run finalizer + free monitor for a dead page slot (does NOT free() the slot;
// the slot is recycled into the page free-list by the caller). Mirrors
// freeAndFinalize / codenameOneGcFree minus the free().
static void cn1BibopReclaimSlot(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    // An unpublished (parentCls==0) NoZero slot has garbage in every field (nsString,
    // monitor, ...) -- reclaiming it would deref NULL->finalizerFunction and CFRelease
    // a garbage peer. It holds no finalizer/peer/monitor by construction, so there is
    // nothing to release: skip. (Fixed at the source too -- see cn1InlSbToString -- so
    // this is defense in depth for any future memset-elided allocator.)
    if(o->__codenameOneParentClsReference == 0) {
        return;
    }
    finalizerFunctionPointer ptr = (finalizerFunctionPointer)o->__codenameOneParentClsReference->finalizerFunction;
    if(ptr != 0) {
        ptr(threadStateData, o);
    }
    cn1ReleaseStringPeer(o);
    void* md = cn1MonitorDataRemove(o);
    if(md) {
        free(md);
    }
}

#ifndef CN1_BIBOP_NO_FASTSWEEP
// A native peer (cached NSString) was attached to a BiBOP object: flag its
// page exactly like a monitor so the dead slot always reaches
// cn1BibopReclaimSlot (which releases the peer) instead of the O(1) all-dead
// page reclaim. Same sticky-flag visibility argument as monitors.
void cn1BibopNoteNativePeer(JAVA_OBJECT obj) {
    if(obj != JAVA_NULL && (obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED)) {
        CN1BibopPage* p = (CN1BibopPage*)((uintptr_t)obj & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        p->gcHasMonitors = JAVA_TRUE;
    }
}

void cn1BibopNoteMonitorAttached(JAVA_OBJECT obj) {
    // A tagged Integer is an immediate, not a slot: no page to flag, and
    // dereferencing its bit pattern as a header would read a garbage address
    // (monitorEnter now creates REAL side-table monitors for tagged values).
    if(CN1_IS_TAGGED(obj)) {
        return;
    }
    if(obj != JAVA_NULL && (obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED)) {
        // STICKY per-page flag (plain store): visible to any sweep that could
        // legitimately take the all-dead shortcut for this page, because that
        // requires a full mark completed AFTER this (live) object died, and the
        // mark's thread-stop handshake orders this store before the GC's reads.
        CN1BibopPage* p = (CN1BibopPage*)((uintptr_t)obj & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        p->gcHasMonitors = JAVA_TRUE;
    }
}
#else
// When the O(live-pages) fast sweep is disabled there is no all-dead shortcut to
// suppress: every retired page is full-walked and every dead slot reaches
// cn1BibopReclaimSlot (which releases the native peer) regardless. The header
// declares cn1BibopNoteNativePeer unconditionally and toNSString() (Apple/ObjC
// builds) calls it unconditionally, so provide a no-op definition here to keep
// the symbol resolvable in the CN1_BIBOP_NO_FASTSWEEP configuration.
// (cn1BibopNoteMonitorAttached is declared and called only under !NO_FASTSWEEP,
// so it needs no counterpart here.)
void cn1BibopNoteNativePeer(JAVA_OBJECT obj) { (void)obj; }
#endif

static void cn1BibopAdaptAfterSweep(long occupiedBytes, long liveBytes,
                                    long reclaimedBytes,
                                    long* classSlots, long* classLive) {
    bibopLastCycleOccupiedBytes = occupiedBytes;
    bibopLastCycleLiveBytes = liveBytes;
    bibopLastCycleReclaimedBytes = reclaimedBytes;

    if(lowMemoryMode) {
        long oldTrigger = atomic_load_explicit(&bibopGcTriggerBytes,
                                               memory_order_relaxed);
        bibopTriggerHighSurvivalStreak = 0;
        if(oldTrigger != CN1_BIBOP_GC_TRIGGER_BYTES) {
            atomic_store_explicit(&bibopGcTriggerBytes,
                                  CN1_BIBOP_GC_TRIGGER_BYTES,
                                  memory_order_relaxed);
        }
    } else if(occupiedBytes >= (2 * 1024 * 1024)) {
        int survival = (int)((liveBytes * 100) / occupiedBytes);
        long oldTrigger = atomic_load_explicit(&bibopGcTriggerBytes,
                                               memory_order_relaxed);
        long newTrigger = oldTrigger;
        if(survival >= CN1_BIBOP_BYPASS_SURVIVAL_PERCENT) {
            bibopTriggerHighSurvivalStreak++;
            if(bibopTriggerHighSurvivalStreak >= 2) {
                long ceiling = CN1_BIBOP_GC_MAX_TRIGGER_BYTES;
                long freeMem = atomic_load_explicit(&cn1CachedFreeMem,
                                                    memory_order_relaxed);
                if(freeMem > 0 && freeMem / 8 < ceiling) ceiling = freeMem / 8;
                if(ceiling < CN1_BIBOP_GC_TRIGGER_BYTES) {
                    ceiling = CN1_BIBOP_GC_TRIGGER_BYTES;
                }
                newTrigger = oldTrigger * 2;
                if(newTrigger > ceiling) newTrigger = ceiling;
                bibopTriggerHighSurvivalStreak = 0;
            }
        } else if(survival <= 20) {
            bibopTriggerHighSurvivalStreak = 0;
            if(oldTrigger > CN1_BIBOP_GC_TRIGGER_BYTES) {
                newTrigger = oldTrigger / 2;
                if(newTrigger < CN1_BIBOP_GC_TRIGGER_BYTES) {
                    newTrigger = CN1_BIBOP_GC_TRIGGER_BYTES;
                }
            }
        }
        if(newTrigger != oldTrigger) {
            atomic_store_explicit(&bibopGcTriggerBytes, newTrigger,
                                  memory_order_relaxed);
        }
    }

    for(int ci = 0 ; ci < CN1_BIBOP_NUM_CLASSES ; ci++) {
        if(classSlots[ci] < CN1_BIBOP_BYPASS_MIN_SLOTS) {
            bibopHighSurvivalStreak[ci] = 0;
            continue;
        }
        int survival = (int)((classLive[ci] * 100) / classSlots[ci]);
        if(survival >= CN1_BIBOP_BYPASS_SURVIVAL_PERCENT) {
            if(++bibopHighSurvivalStreak[ci] >= 2) {
                atomic_fetch_add_explicit(&bibopBypassGeneration[ci], 1,
                                          memory_order_relaxed);
#ifdef CN1_GC_INSTRUMENT
                atomic_fetch_add_explicit(&cn1BibopBypassActivations, 1,
                                          memory_order_relaxed);
#endif
                bibopHighSurvivalStreak[ci] = 0;
            }
        } else if(survival <= 20) {
            bibopHighSurvivalStreak[ci] = 0;
        }
    }
#ifdef CN1_GC_INSTRUMENT
    fprintf(stderr,
            "[BIBOP-ADAPT] epoch=%d allocatedMB=%.1f triggerMB=%.1f occupiedMB=%.1f "
            "liveMB=%.1f reclaimedMB=%.1f promotions=%ld bypass=%ld bypassAllocs=%ld freshPages=%ld "
            "beltRuns=%ld adoptedSkips=%ld\n",
            currentGcMarkValue,
            bibopCycleAllocatedBytes / (1024.0 * 1024.0),
            atomic_load_explicit(&bibopGcTriggerBytes, memory_order_relaxed) /
                (1024.0 * 1024.0),
            bibopLastCycleOccupiedBytes / (1024.0 * 1024.0),
            bibopLastCycleLiveBytes / (1024.0 * 1024.0),
            bibopLastCycleReclaimedBytes / (1024.0 * 1024.0),
            atomic_load_explicit(&cn1BibopHighThroughputPromotions, memory_order_relaxed),
            atomic_load_explicit(&cn1BibopBypassActivations, memory_order_relaxed),
            atomic_load_explicit(&cn1BibopBypassAllocations, memory_order_relaxed),
            atomic_load_explicit(&cn1BibopFreshPagesScanned, memory_order_relaxed),
            atomic_load_explicit(&cn1BibopBeltRuns, memory_order_relaxed),
            atomic_load_explicit(&cn1BibopAdoptedRescanSkips, memory_order_relaxed));
#endif
}

// Sweep all retired pages. Runs on the GC thread AFTER mark completes; the
// pages it processes are off the SWEEP stack (owner==0), so no mutator is
// allocating into them and no marking is in flight -> plain header access.
static void cn1BibopSweep(CODENAME_ONE_THREAD_STATE) {
#ifdef CN1_GC_VERIFY
    extern int cn1GcFaultEarlyFree;
    { extern void cn1GcFaultInitPublic(void); cn1GcFaultInitPublic(); }
#else
    const int cn1GcFaultEarlyFree = 0;
#endif
    CN1BibopPage* list = atomic_exchange_explicit(&bibopSweepStack, (CN1BibopPage*)0, memory_order_acquire);
#if !defined(CN1_BIBOP_NO_PAGE_RELEASE)
    // MAJOR SWEEP (issue 5537). The ordinary sweep only ever sees RETIRED pages
    // -- ones a thread filled and handed back. A page that was swept while it
    // still held live objects goes to bibopPartialPool and is never looked at
    // again until some later allocation happens to re-acquire it. So when a big
    // live set dies during a quiet period, its pages keep every dead slot: they
    // are never re-swept, never become empty, never reach bibopFreePool, and the
    // trim below has nothing to hand back. That is why the free pool measured
    // empty on the very workload this was meant to fix.
    //
    // Splicing the partial pools onto the sweep list re-examines them. It is
    // correct at any time -- marking reaches an object through its header
    // wherever the object lives, so live slots on a partial page carry the
    // current epoch exactly as retired pages do, and the full walk rebuilds
    // freeList/freeCount from scratch, so re-walking a page is idempotent.
    //
    // It is NOT free, though: it makes the sweep O(all pages) instead of
    // O(retired pages), which is the cost issue 5425 was about. So it runs only
    // on a cadence, or immediately when the OS has told us memory is short --
    // the moment actually worth paying for.
    if(cn1BibopReleaseOffset() != 0) {
        // Run a major sweep when the OS says memory is short, when the app has
        // gone QUIET (a burst just ended -- the case that matters, and the case
        // where the extra walk costs least), or as a periodic backstop for an app
        // that never goes quiet. A cycle driven by allocation volume is none of
        // those and keeps the O(retired pages) fast path.
        bibopCyclesSinceMajorSweep++;
        // "Quiet" has to mean quiet on BOTH allocation paths. A cycle driven by
        // legacy volume allocates nothing through BiBOP, so testing
        // bibopCycleAllocatedBytes alone would call it quiet and splice every
        // partial page in every sweep -- the O(all pages) regression issue 5425
        // fixed, reintroduced for exactly the workload that reported it.
        JAVA_BOOLEAN quiet =
                ((long long)bibopCycleAllocatedBytes + legacyCycleAllocatedBytes)
                        < (long long)CN1_BIBOP_MAJOR_SWEEP_QUIET_BYTES;
        int major = atomic_load_explicit(&lowMemoryMode, memory_order_relaxed)
                || quiet
                || bibopCyclesSinceMajorSweep >= CN1_BIBOP_MAJOR_SWEEP_CYCLES;
        if(major) {
            bibopCyclesSinceMajorSweep = 0;
            int spliced = 0;
            pthread_mutex_lock(&bibopMutex);
            for(int ci = 0 ; ci < CN1_BIBOP_NUM_CLASSES ; ci++) {
                CN1BibopPage* p = bibopPartialPool[ci];
                while(p != 0) {
                    CN1BibopPage* next = p->nextPool;
                    p->gcMajorSpliced = JAVA_TRUE;
                    p->nextPool = list;
                    list = p;
                    p = next;
                    spliced++;
                }
                bibopPartialPool[ci] = 0;
            }
            pthread_mutex_unlock(&bibopMutex);
            if(cn1PageReleaseTraceOn()) {
                fprintf(stderr, "[MAJOR-SWEEP] cycle=%d spliced=%d bibopKb=%ld legacyKb=%ld\n",
                        currentGcMarkValue, spliced,
                        (long)(bibopCycleAllocatedBytes / 1024),
                        (long)(legacyCycleAllocatedBytes / 1024));
            }
        }
    }
#endif
    int V = currentGcMarkValue;  // stable during the sweep (mark done, not yet incremented)
    long occupiedBytes = 0;
    long liveBytes = 0;
    long reclaimedBytes = 0;
    long classSlots[CN1_BIBOP_NUM_CLASSES] = {0};
    long classLive[CN1_BIBOP_NUM_CLASSES] = {0};
#ifndef CN1_BIBOP_NO_FASTSWEEP
    // Snapshot once: if ANY BiBOP object currently carries a monitor, suppress the O(1)
    // all-dead shortcut this whole sweep so dead monitored slots are full-walked and
    // their monitor freed. Safe to cache: a homogeneous all-dead page's slots are
    // unreachable (not marked this cycle, aged >=2 cycles), so no mutator can hold a
    // reference to monitorEnter one during this sweep; any concurrent attach is to a live
    // object on some OTHER page and is observed by the next sweep.
#endif
    while(list != 0) {
        CN1BibopPage* page = list;
        list = page->nextPool;
        // A page the major sweep pulled out of a PARTIAL pool is a one-off deep
        // sample: mostly-dead slots that the ordinary sweep would never have
        // looked at again. Feeding it to cn1BibopAdaptAfterSweep drags the
        // measured survival ratio down, which halves bibopGcTriggerBytes toward
        // its base and buys more collection cycles for no reason -- measured 5
        // cycles to 8 on the issue-5425 workload, eating most of that guard's
        // headroom. The page is still swept and still reclaimed; only its
        // contribution to the trigger POLICY is withheld.
        JAVA_BOOLEAN statsExcluded = page->gcMajorSpliced;
        page->gcMajorSpliced = JAVA_FALSE;
#ifdef CN1_BIBOP_VALIDATE
        // INVARIANT: only RETIRED (non-owned) pages reach the sweep. If an OWNED
        // page (some thread's live bibopCurrent[ci]) is on the sweep stack, the
        // sweep will reset/recycle it out from under that thread -> the
        // intermittent cn1BibopFastAlloc crash. Catch it here, at the source.
        if(page->owned == JAVA_TRUE) {
            fprintf(stderr, "CN1BIBOP SWEEP OF OWNED PAGE: page=%p classIndex=%d bumpIndex=%d\n",
                    (void*)page, page->classIndex,
                    atomic_load_explicit(&page->bumpIndex, memory_order_relaxed));
            fflush(stderr);
            abort();
        }
#endif
        int n = atomic_load_explicit(&page->bumpIndex, memory_order_acquire);
        // Take (and clear) the grace-mark tally before any branch below can leave the
        // page: the O(1) decisions add no live count at all, so a tally left behind
        // would be subtracted from a LATER cycle's survivors. Marking is finished, so
        // this is the complete count for the window since this page was last swept.
        int graceMarked = atomic_exchange_explicit(&page->gcGraceMarked, 0,
                                                   memory_order_relaxed);
#ifndef CN1_BIBOP_NO_FASTSWEEP
        // ---- O(1) page decision (no per-slot walk). -------------------------------
        // A page is HOMOGENEOUS when every occupied slot is a dead-or-graced object
        // sitting at a single (upper-bounded) epoch. That holds iff:
        //   * !gcAllocedSinceSweep  -> nothing was allocated into it since its last
        //       sweep, so it has NO fresh mark==-1 grace-candidate slots; and
        //   * gcLastMarkedEpoch < V-1 -> nothing on it was marked THIS cycle OR THE
        //       PREVIOUS one, so every occupant is below the per-slot walk's free
        //       threshold. "!= V" is NOT sufficient and was the bug fixed here: it
        //       admits a page whose newest slot is at V-1, which that walk keeps; and
        //   * !gcNeedsReclaim       -> no survivor carries a finalizer (monitors handled
        //       by the page's sticky gcHasMonitors flag); and
        //   * freeList == 0         -> the page is full (defensive: a homogeneous page
        //       can only reach here full -- a partial page, once adopted, is always
        //       allocated into before re-retire, which sets gcAllocedSinceSweep).
        // gcGraceEpoch is the upper bound on survivor epochs as of the last full walk.
        // Why that bound matters, since this shortcut claims to reproduce the
        // per-slot walk exactly: testing != V let it drop whole pages holding
        // V-1 slots, freeing them a full cycle earlier than the walk would.
        // Measured on the issue-5425 workload: 26,924 slots in one run.
        //
        // That is what left kept objects pointing into reclaimed memory. The
        // legacy sweep ages on the same m < V-1 rule, so a matured
        // Hashtable.Entry at V-1 is kept while its page-resident byte[] payload
        // at V-1 was already gone -- and this collector resurrects unreachable
        // objects routinely (a stale native-stack word conservatively marks
        // whatever it points at), which turns that pairing into a drain
        // following a field into a recycled slot.
        //
        // (cn1GcFaultEarlyFree restores the old bound for A/B measurement.)
        //
        // Both bounds are needed and neither implies the other: gcLastMarkedEpoch
        // covers slots marked by gcMarkObject since the last full walk, while
        // gcGraceEpoch covers slots the sweep itself promoted out of grace.
        if(page->gcAllocedSinceSweep == JAVA_FALSE &&
           page->gcNeedsReclaim == JAVA_FALSE &&
           page->gcHasAdopted == JAVA_FALSE &&
           page->freeList == 0 &&
           atomic_load_explicit(&page->gcLastMarkedEpoch, memory_order_relaxed)
               < (cn1GcFaultEarlyFree ? V : V - 1)) {
            int graceEpoch = page->gcGraceEpoch;
            if(graceEpoch >= V - 1) {
                // STILL IN GRACE -> all occupants survive this cycle. The full walk would
                // grace/keep them and route the full all-live page to partialPool; do
                // exactly that without a walk. Leave gcGraceEpoch UNCHANGED so the page
                // ages and a later cycle re-evaluates it (-> all-dead) rather than pinning
                // the garbage forever.
                pthread_mutex_lock(&bibopMutex);
                page->nextPool = bibopPartialPool[page->classIndex];
                bibopPartialPool[page->classIndex] = page;
                pthread_mutex_unlock(&bibopMutex);
                if(!statsExcluded) {
                    occupiedBytes += (long)n * page->slotSize;
                    classSlots[page->classIndex] += n;
                }
                continue;
            } else if(!page->gcHasMonitors) {
                // AGED PAST GRACE (even the youngest survivor at gcGraceEpoch < V-1 is
                // dead) and no BiBOP monitor exists to free -> the full walk would
                // reclaim every slot (no finalizers) and route the page to freePool,
                // where it is reformatted on reuse. Byte-identical outcome WITHOUT
                // touching a single slot: just reset the page and pool it.
#ifdef CN1_GC_VERIFY
                // QA: this shortcut is where nearly all BiBOP memory is actually
                // reclaimed -- it drops a whole page without writing a single
                // slot, so every dead object keeps an intact-looking header and a
                // dangling reference into it stays plausible indefinitely (the
                // "corrupted word rather than crash" shape of issue 5425). Poison
                // the slots before the page is pooled. Resetting the bump cursor
                // below is what makes the verifier classify any surviving
                // reference into this page as a recycled slot.
                {
                    extern long cn1GcVerifyFreedSlots;
                    extern long cn1GcVerifyEarlyFreed;
                    for(int __i = 0 ; __i < n ; __i++) {
                        JAVA_OBJECT __o = cn1BibopSlot(page, __i);
                        // Slots at V-1 are ones the per-slot walk KEEPS (it frees
                        // on m < V-1). Counting them measures how far this
                        // shortcut departs from the rule it claims to match.
                        if(__o->__codenameOneGcMark == V - 1) {
                            cn1GcVerifyEarlyFreed++;
                            static int __dbg = 0;
                            // Cached: the earlyfree self-test drives tens of
                            // thousands of these, and getenv scans the block.
                            static int __dbgOn = -1;
                            if(__dbgOn < 0) __dbgOn = getenv("CN1_GC_DEBUG_EARLY") ? 1 : 0;
                            if(__dbgOn && __dbg < 6) {
                                __dbg++;
                                fprintf(stderr, "[EARLY] V=%d graceEpoch=%d lastMarked=%d "
                                        "slotMark=%d heapPos=%d cls=%s alloced=%d adopted=%d\n",
                                        V, page->gcGraceEpoch,
                                        atomic_load_explicit(&page->gcLastMarkedEpoch, memory_order_relaxed),
                                        __o->__codenameOneGcMark, __o->__heapPosition,
                                        (__o->__codenameOneParentClsReference &&
                                         __o->__codenameOneParentClsReference->clsName)
                                            ? __o->__codenameOneParentClsReference->clsName : "?",
                                        (int)page->gcAllocedSinceSweep, (int)page->gcHasAdopted);
                                fflush(stderr);
                            }
                        }
                        cn1GcVerifyPoisonSlot(__o, page->slotSize);
                        __o->__codenameOneGcMark = CN1_BIBOP_FREE_MARK;
                        cn1GcVerifyFreedSlots++;
                    }
                }
#endif
                atomic_store_explicit(&page->bumpIndex, 0, memory_order_relaxed);
                page->freeList = 0;
                page->freeCount = 0;
                page->gcAllocedSinceSweep = JAVA_FALSE;
                page->gcNeedsReclaim = JAVA_FALSE;
                pthread_mutex_lock(&bibopMutex);
                page->nextPool = bibopFreePool;
                bibopFreePool = page;
                pthread_mutex_unlock(&bibopMutex);
                if(!statsExcluded) {
                    occupiedBytes += (long)n * page->slotSize;
                    reclaimedBytes += (long)n * page->slotSize;
                    classSlots[page->classIndex] += n;
                }
                continue;
            }
            // else: all-dead but a BiBOP monitor is live -> fall through to the full walk
            // so the dead monitored slot(s) reach cn1BibopReclaimSlot.
        }
#endif
        // ACQUIRE pairs with the allocator's RELEASE store of bumpIndex: for every
        // slot i < n the header stores (parentCls / heapPosition / mark) that
        // preceded that release are visible to this walk. Relaxed could observe a
        // freshly-bumped slot with a garbage header.
        int oldFreeCount = page->freeCount;
        void* fl = 0;
        int freeCount = 0;
        int liveCount = 0;
        int policyLiveCount = 0;
#ifndef CN1_BIBOP_NO_FASTSWEEP
        JAVA_BOOLEAN needsReclaim = JAVA_FALSE;
#endif
        for(int i = 0 ; i < n ; i++) {
            JAVA_OBJECT o = cn1BibopSlot(page, i);
            int m = o->__codenameOneGcMark;
            // MATURED (adopted) slot: its lifecycle belongs to the legacy mark/sweep now.
            // Skip it entirely (no double-clearing) -- BiBOP counts it as occupied/live so
            // the page isn't reclaimed. The legacy sweep flips it back to -3 on death, and a
            // LATER BiBOP sweep of this page then reclaims it as a normal dead slot.
            if(o->__heapPosition == CN1_BIBOP_ADOPTED) {
                liveCount++;
                if(m == V) policyLiveCount++;
                continue;
            }
            if(m == CN1_BIBOP_FREE_MARK) {
                *(void**)o = fl; fl = o; freeCount++;
            } else if(m == -1) {
                // fresh, never marked -> one cycle of grace (legacy parity)
                o->__codenameOneGcMark = V;
                liveCount++;
#ifndef CN1_BIBOP_NO_FASTSWEEP
                // parentCls==0 => a MID-CONSTRUCTION memset-elided object (the
                // translator publishes the class pointer only once every field is
                // written -- see cn1BibopFastAllocNoZero). Such classes are barred
                // from the elision if they declare a finalizer, so skipping the
                // finalizer probe here is exact, and dereferencing would be a NULL
                // crash (this sweep runs concurrently with the constructing thread).
                if(o->__codenameOneParentClsReference != 0 &&
                   o->__codenameOneParentClsReference->finalizerFunction != 0) needsReclaim = JAVA_TRUE;
#endif
            } else if(m < V - 1) {
                cn1BibopReclaimSlot(threadStateData, o);
#ifdef CN1_GC_VERIFY
                { extern long cn1GcVerifyFreedSlots; cn1GcVerifyFreedSlots++; }
                // QA: destroy the payload before the slot joins the free list.
                // The free-list link (first word) and the FREE sentinel below
                // are written after, so the page structure is unaffected.
                cn1GcVerifyPoisonSlot(o, page->slotSize);
#endif
                o->__codenameOneGcMark = CN1_BIBOP_FREE_MARK;
                *(void**)o = fl; fl = o; freeCount++;
            } else {
                liveCount++;
                if(m == V) policyLiveCount++;
#ifndef CN1_BIBOP_NO_FASTSWEEP
                // parentCls==0 guard mirrors the mark==-1 grace branch above: a
                // memset-elided object can be published only once every field is
                // written, and an abandoned NoZero slot (never published) can reach
                // here after grace-aging -- dereferencing NULL->finalizerFunction
                // would crash at offset 0x10.
                if(o->__codenameOneParentClsReference != 0 &&
                   o->__codenameOneParentClsReference->finalizerFunction != 0) needsReclaim = JAVA_TRUE;
#endif
            }
        }
        page->freeList = fl;
        page->freeCount = freeCount;
        int sampledSlots = n - oldFreeCount;
        // Survivors the policy may act on: slots at the current epoch MINUS the ones a
        // grace pass put there. A grace mark says only "allocated since the last cycle
        // and not proven dead", so counting it as a survivor makes the measured survival
        // ratio a function of the ALLOCATION RATE. A pure-churn workload then reads as
        // survivor-heavy and gets diverted onto the legacy heap by the bypass below --
        // measured on the issue-5537 game-tree search as 190K of 700K 48-byte slots
        // "surviving" against a real live set of a few hundred objects. Clamped rather
        // than allowed to go negative: a page swept several cycles after its grace marks
        // were taken can hold survivors that have since been proven live, and reading
        // those as zero only makes the policy more conservative.
        int policySurvivors = policyLiveCount - graceMarked;
        if(policySurvivors < 0) {
            policySurvivors = 0;
        }
        if(!statsExcluded) {
            occupiedBytes += (long)sampledSlots * page->slotSize;
            liveBytes += (long)policySurvivors * page->slotSize;
            reclaimedBytes += (long)(sampledSlots - liveCount) * page->slotSize;
            classSlots[page->classIndex] += sampledSlots;
            classLive[page->classIndex] += policySurvivors;
        }
#ifndef CN1_BIBOP_NO_FASTSWEEP
        // The monitor (CN1ThreadData) no longer lives in the object header, so the
        // per-slot "has a monitor" test is gone. Conservatively flag any page that still
        // has survivors while ANY BiBOP monitor is live globally: this can never miss a
        // monitored survivor (over-approximation only suppresses a future O(1) shortcut,
        // never a needed reclaim) and keeps the dead-monitor freeing exactly as before.
        if(page->gcHasMonitors && liveCount > 0) needsReclaim = JAVA_TRUE;
        // Refresh the per-page facts for the next sweep. gcGraceEpoch = V is a safe upper
        // bound on every survivor's epoch (survivors are at V from grace/mark-this-cycle,
        // or at V-1 from aging) so the all-dead test (gcGraceEpoch < V-1) can never fire
        // while a live/grace object remains.
        page->gcAllocedSinceSweep = JAVA_FALSE;
        page->gcNeedsReclaim = needsReclaim;
        page->gcGraceEpoch = V;
#endif
        pthread_mutex_lock(&bibopMutex);
        if(liveCount == 0) {
            page->nextPool = bibopFreePool;
            bibopFreePool = page;
        } else {
            page->nextPool = bibopPartialPool[page->classIndex];
            bibopPartialPool[page->classIndex] = page;
        }
        pthread_mutex_unlock(&bibopMutex);
    }
    cn1BibopAdaptAfterSweep(occupiedBytes, liveBytes, reclaimedBytes,
                            classSlots, classLive);
    // Hand surplus empty pages back to the OS (issue 5537). Last, so it sees the
    // pool this sweep just refilled, and outside the per-page loop so the madvise
    // work is batched rather than interleaved with the walk.
    cn1BibopTrimFreePool();
}

#ifdef CN1_GRACE_AUDIT
// QA builds only (grace-completeness gate, born from issue 5425): walk the FULL
// page registry right before the sweep, ignoring every pruning heuristic, and
// trace any slot that (a) existed before the grace pass ran (below the
// mark-start snapshot) and (b) is still fresh (gcMark == -1) with a published
// non-leaf class. missedFresh counts fresh objects the grace pass did not visit
// (small counts can be benign: a free-list slot re-allocated mid-mark below the
// snapshot after the grace pass ran is SATB-covered this cycle and re-traced
// next cycle). doomedChildren counts objects that became newly marked ONLY by
// tracing them -- ANY nonzero value is a collector bug: without this pass the
// sweep frees those children while a surviving fresh object still references
// them (dangling reference -> heap corruption).
static void cn1GraceAuditPreSweep(CODENAME_ONE_THREAD_STATE) {
    long missedFresh = 0;
    long beforeFresh = gcMarkNewObjectCount;
    CN1BibopPage* gp = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
    while(gp != 0) {
        int gn = __atomic_load_n(&gp->gcAuditSnapshot, __ATOMIC_RELAXED);
        int bi = atomic_load_explicit(&gp->bumpIndex, memory_order_acquire);
        if(gn > bi) gn = bi;   // page was reformatted mid-cycle; stale snapshot
        for(int gi = 0 ; gi < gn ; gi++) {
            JAVA_OBJECT go = cn1BibopSlot(gp, gi);
            if(__atomic_load_n(&go->__codenameOneGcMark, __ATOMIC_ACQUIRE) == -1
               && go->__codenameOneParentClsReference != 0
               && go->__codenameOneParentClsReference->markFunction != 0) {
                missedFresh++;
                gcMarkObject(threadStateData, go, JAVA_FALSE);
            }
        }
        gp = atomic_load_explicit(&gp->nextAll, memory_order_acquire);
    }
    long freshMarked = gcMarkNewObjectCount - beforeFresh;
    gcMarkDrain(threadStateData);
    long recovered = gcMarkNewObjectCount - beforeFresh - freshMarked;
    if(missedFresh > 0 || recovered > 0) {
        fprintf(stderr, "[GRACE-AUDIT] epoch=%d missedFresh=%ld doomedChildren=%ld\n",
                currentGcMarkValue, missedFresh, recovered);
        fflush(stderr);
    }
    cn1GraceAuditLegacy(threadStateData);
}
#endif

#ifdef CN1_GRACE_AUDIT
// The LEGACY half of the same guarantee. codenameOneGCSweep grants a fresh
// (gcMark == -1) legacy object exactly the BiBOP grace rule -- it promotes it to
// the current epoch instead of freeing it -- but nothing traces its subtree, so
// an older object reachable ONLY through it is freed while the graced object
// still references it. Every object above CN1_BIBOP_MAX_OBJECT takes this path,
// as does every allocation the adaptive survivor-heavy bypass diverts off the
// page heap, so the class of workload issue 5425 reported is squarely on it.
//
// Same reporting contract as the BiBOP half: missedFresh counts graced legacy
// objects nothing traced, doomedChildren counts objects that became marked ONLY
// through them -- each one the sweep would otherwise free while referenced.
static void cn1GraceAuditLegacy(CODENAME_ONE_THREAD_STATE) {
    long missedFresh = 0;
    long beforeFresh = gcMarkNewObjectCount;
    int t = currentSizeOfAllObjectsInHeap;
    for(int iter = 0 ; iter < t ; iter++) {
        JAVA_OBJECT o = allObjectsInHeap[iter];
        if(o != JAVA_NULL && o->__codenameOneGcMark == -1
           && o->__codenameOneParentClsReference != 0
           && o->__codenameOneParentClsReference->markFunction != 0) {
            missedFresh++;
            gcMarkObject(threadStateData, o, JAVA_FALSE);
        }
    }
    long freshMarked = gcMarkNewObjectCount - beforeFresh;
    gcMarkDrain(threadStateData);
    long recovered = gcMarkNewObjectCount - beforeFresh - freshMarked;
    if(missedFresh > 0 || recovered > 0) {
        fprintf(stderr, "[GRACE-AUDIT-LEGACY] epoch=%d missedFresh=%ld doomedChildren=%ld\n",
                currentGcMarkValue, missedFresh, recovered);
        fflush(stderr);
    }
}
#endif

// (The overflow-rescan helpers cn1BibopRescanStart / cn1BibopRescanStep live
// further down, next to gcMarkDrain, because they use the mark worklist.)

// Called on the dying thread (collectThreadResources): retire all of its
// current pages so their slots become collectable.
void cn1BibopRetireThreadPages() {
    pthread_once(&bibopOnce, cn1BibopDoInit);
    for(int ci = 0 ; ci < CN1_BIBOP_NUM_CLASSES ; ci++) {
        CN1BibopPage* old = bibopCurrent[ci];
        if(old != 0) {
            old->owned = JAVA_FALSE;
            CN1BibopPage* sh = atomic_load_explicit(&bibopSweepStack, memory_order_relaxed);
            do {
                old->nextPool = sh;
            } while(!atomic_compare_exchange_weak_explicit(&bibopSweepStack, &sh, old,
                        memory_order_release, memory_order_relaxed));
            bibopCurrent[ci] = 0;
        }
    }
}
#endif /* CN1_DISABLE_BIBOP */

#ifdef CN1_DISABLE_BIBOP
// The legacy collector still uses the shared free-memory snapshot during mark.
// Keep its QA build self-contained even though it has no BiBOP pacing policy.
_Atomic long cn1CachedFreeMem = 0;
void cn1RefreshFreeMemCache(void) {
    atomic_store_explicit(&cn1CachedFreeMem, cn1_available_memory(),
                          memory_order_relaxed);
}
#endif

#ifdef CN1_CONSERVATIVE_GC_ROOTS
// =========================================================================
// PHASE 3b: conservative native-C-stack scanning AS A REAL GC ROOT SOURCE.
//
// WHY: object-bearing FRAMELESS methods (BytecodeMethod.isFramelessEligible with
// -Dcn1.frameless.objects) keep their object operand stack + object locals in a
// method-LOCAL C array (cn1_frameless_frame) on the native C stack, NOT in the
// side-allocated threadObjectStack the precise collector walks. Their object roots
// are therefore invisible to the precise scan. To keep those objects alive we make
// the GC additionally walk each stopped thread's native C stack [sp, stackBase) +
// its register snapshot and mark every word that resolves to a live heap object.
// The collector is now HYBRID: precise threadObjectStack scan (legacy frames) PLUS
// conservative native-stack scan (frameless frames). An object is reachable from
// whichever frame holds it; the conservative scan covers the whole native stack so
// the legacy<->frameless caller/callee boundary is never a gap.
//
// (a) cn1ConservativeResolve(word) -> base of the live heap object the word points
//     into (interior pointers included) or JAVA_NULL, dereferencing nothing it has
//     not first proven to be a registered heap address:
//       * BiBOP small objects: (w & ~(PAGE-1)) is the candidate page base; confirm
//         it is a registered page by binary-searching a snapshot of bibopAllPages;
//         map the interior word to its slot; liveness = slot not on the page
//         free-list (mark != FREE) and header sentinel intact.
//       * large/array objects (allObjectsInHeap + every thread's pending): a sorted,
//         non-overlapping [lo,hi) extent table; array element blocks are covered so
//         an interior element pointer resolves to the array base.
//       * garbage robustness: bounds-checked before any deref; unaligned/tagged words
//         rejected (filters tagged-Integer immediates whose bit0 is set).
// (b) cn1ConservativeMarkRange([lo,hi)): read every aligned word, resolve, gcMarkObject
//     it for REAL (serial worklist push -- lock-free in the GC-thread context). Marks
//     a SUPERSET of the precise set: nothing live is freed; at most a little floating
//     garbage (a stale stack slot's referent) is retained one cycle until the frame is
//     reused. Measured <0.3% over live (Phase 2).
//
// THREAD STOPPING (every thread that can hold a frameless root must be scanned):
//   * COOPERATIVE (lightweight Java threads -- the common case, proven in Phase 3a):
//     a thread that pauses at an allocation safepoint runs CN1_GC_PARK_CAPTURE in the
//     parking frame (setjmp flushes callee-saved regs into a scanned jmp_buf; records
//     the parked SP). The GC already waits for threadActive==FALSE, so the capture is
//     complete and the whole live call chain (including any frameless frame above the
//     safepoint) is resident in [sp, base).
//   * SIGNAL (genuine native threads the GC does not park, OR validation forcing via
//     CN1_GC_SIGNAL_STOP=1): pthread_kill(thread, SIGUSR2); the async-signal-safe
//     handler captures the interrupted SP + a raw copy of the ucontext register file
//     and spins on a release flag (store + spin only -- no malloc/lock). LOCK SAFETY:
//     the resolver snapshot (which reallocs) is rebuilt BEFORE the thread is stopped,
//     so the GC never reallocs while a thread is frozen mid-malloc.
// =========================================================================

// SIGUSR2 is used so SIGUSR1 stays free for app/JNI use.
#define CN1_GC_STOP_SIGNAL SIGUSR2

// =========================================================================
// CONSERVATIVE ROOT RESOLVER -- architecture + perf notes
// =========================================================================
// Under CN1_CONSERVATIVE_GC_ROOTS the collector finds roots by scanning stopped
// threads' native stacks + register snapshots word by word. Every candidate word is
// a raw machine value; to decide "does this word point at (or into) a live heap
// object, and if so which one?" we need an address->object resolver. That resolver is
// what cn1ConservativeResolve() queries and what cn1GcBuildRootSnapshots() (re)builds
// at the start of every mark cycle. There are TWO backing structures, because the heap
// has two allocation regimes with very different lifecycle:
//
//   1. BiBOP objects (small, non-array): live in size-class pages held in a GROW-ONLY
//      registry (bibopAllPages -- pages are never unlinked or reordered). A pointer is
//      resolved by masking it to its 64KB page base, looking that base up in an
//      open-addressed table (cn1ConsPg) that stores the page geometry inline, then
//      indexing the slot arithmetically. Because the registry is grow-only, the table's
//      KEYS are stable and it is rebuilt only when the registration COUNT changes; the
//      geometry it caches is refreshed every cycle. See cn1ConsPgIndexedCount below.
//
//   2. Legacy objects (arrays + anything not BiBOP): tracked in allObjectsInHeap[]. These
//      are resolved via cn1ConsExt[] -- a flat array of (lo,hi,base) extents sorted by lo
//      address -- fronted by cn1ConsExtHash, an exact-base table that answers the common
//      case in one probe. Only a genuine INTERIOR pointer reaches the sorted search.
//
// WHY EITHER INDEX IS A HASH RATHER THAN A BINARY SEARCH: the resolver's dominant caller
//   is gcMarkObject's guard, which runs on every reference field the drain follows, and
//   log2(N) dependent cache-missing loads per field makes the collector's cost scale with
//   the SIZE OF THE HEAP instead of with the live set. On the issue-5537 game-tree search
//   (6.7K pages, 35K extents) that put 75% of the GC thread's wall time inside this
//   function: cycles stretched, the mutator allocated proportionally more during each one,
//   and the footprint settled at whatever ceiling the pacing allowed rather than at
//   anything related to the ~20MB that was actually live.
//
// WHY cn1ConsExt IS REBUILT + qsort()ed EVERY CYCLE (and the BiBOP pages are not):
//   allObjectsInHeap[] is NOT grow-only. The sweep removes a dead object by TOMBSTONING
//   its slot (allObjectsInHeap[pos] = JAVA_NULL), and a later allocation REFILLS that same
//   slot with a DIFFERENT object at a DIFFERENT address (placeObjectInHeapCollection's
//   NULL-slot scan from lastOffsetInRam). So neither the slot->address mapping nor the
//   address-sorted order is stable across cycles -- the cached-and-reuse trick that works
//   for the grow-only page registry does NOT apply. The whole extent array is therefore
//   rebuilt from the live legacy set and re-sorted each cycle.
//
// PERF: on allocation-heavy, array-heavy workloads with a large live set (e.g. the
//   vector-map MVT render: parparvm-bench MvtBench holds ~213k live legacy/array objects),
//   this per-cycle rebuild+qsort is a measurable slice of GC time (profiled ~8% of wall,
//   larger as a fraction of the collector itself). It is NOT the dominant cost of that
//   workload -- the conservative allocation path and marking the large live set dominate --
//   but it is the most self-contained target. Future optimization directions, in rough
//   order of payoff/risk: (a) incrementally maintain cn1ConsExt across cycles (the live
//   set is non-moving and largely stable between collections; only the transient churn and
//   the swept entries change) rather than a full rebuild; (b) route arrays through a
//   grow-only size-class arena so they resolve via the cached page path and skip cn1ConsExt
//   entirely; (c) replace the libc qsort (a function-pointer comparator call per compare)
//   with an inlined/radix sort. All three are correctness-critical (a resolver miss is a
//   use-after-free), so they must be validated against MvtBench + the GcStress/MtStress
//   gauntlet, not just the small-heap tests.
// =========================================================================

// ---- large/array snapshot: sorted by low address, non-overlapping extents ----
typedef struct { char* lo; char* hi; JAVA_OBJECT base; } CN1ConsExtent;
static CN1ConsExtent* cn1ConsExt = 0;
static int cn1ConsExtN = 0, cn1ConsExtCap = 0;

// Pointer mix used by both O(1) indices below. Finalizer of the 64-bit MurmurHash3
// mixer: the inputs are page bases (64KB-aligned, so their low 16 bits are always
// zero) and malloc'd object bases (16-aligned and strongly clustered), and a plain
// mask over either of those collides hard enough to turn linear probing back into
// a scan. Multiplying and folding spreads both into the whole table.
static inline unsigned cn1PtrMix(uintptr_t v) {
    unsigned long long h = (unsigned long long)v;
    h ^= h >> 33;
    h *= 0xff51afd7ed558ccdULL;
    h ^= h >> 29;
    h *= 0xc4ceb9fe1a85ec53ULL;
    h ^= h >> 32;
    return (unsigned)h;
}

// EXACT-BASE index over cn1ConsExt (open addressing, load factor <= 1/2; 0 = empty).
// Every extent's lo IS its object's base (cn1ConsExtAdd sets both from the same
// pointer), so a hit on this table answers the whole query without touching
// cn1ConsExt at all -- the resolved object is the probed pointer.
//
// It exists because the binary search below is the wrong shape for the caller that
// dominates: gcMarkObject's resolve guard runs on EVERY reference field the drain
// follows, and a Java reference is always an object BASE, never an interior pointer.
// On a legacy-array-heavy heap that guard was paying ~log2(N) dependent, cache-missing
// loads per field -- 15 on a 35K-extent heap -- and the collector spent most of its
// time in the index rather than in marking (profiled at 75% of GC-thread wall on the
// issue-5537 game-tree search). Interior pointers are real but rare: they come only
// from the conservative stack/register scan, which still falls through to the sorted
// binary search.
static char** cn1ConsExtHash = 0;
static int cn1ConsExtHashMask = -1;   // capacity-1, or -1 when unallocated

#ifndef CN1_DISABLE_BIBOP
// ---- BiBOP page snapshot: open-addressed on page base ----
// Entry holds the geometry INLINE so a hit costs one cache line, and the registry
// node so the per-cycle geometry refresh can walk the table linearly instead of
// scattering writes across it. base == 0 marks an empty slot.
typedef struct {
    char* base;
    CN1BibopPage* page;
    int firstSlotOffset;
    int slotSize;
    int slotCount;
    int bumpIndex;
} CN1ConsPage;
// Extra room the rebuild sizes for, over the registration count it read, so that
// pages registered while it walks do not cost it the whole table.
#ifndef CN1_CONS_PG_SLACK
#define CN1_CONS_PG_SLACK 256
#endif
// How many times the rebuild re-sizes when the registry outgrows the size it picked.
// Each attempt doubles, so losing this race three times running needs a mutator to
// register faster than the collector can walk a list, sustained -- at which point a
// cycle without a rebuild (the previous index, retried next cycle) is the right
// answer anyway.
#ifndef CN1_CONS_PG_REBUILD_ATTEMPTS
#define CN1_CONS_PG_REBUILD_ATTEMPTS 3
#endif
static CN1ConsPage* cn1ConsPg = 0;
static int cn1ConsPgN = 0;            // occupied entries
static int cn1ConsPgMask = -1;        // capacity-1, or -1 when unallocated
// Number of pages the last rebuild indexed. The registry is grow-only, so the table
// is valid until that count moves -- see cn1GcBuildRootSnapshots.
static long long cn1ConsPgIndexedCount = -1;

// Find the entry for a page base, or 0. Terminates on the empty slot that the
// <= 1/2 load factor guarantees exists.
static inline CN1ConsPage* cn1ConsPgFind(char* cand) {
    // A ZERO KEY IS THE EMPTY MARKER, so it must never be looked up. This function is
    // handed arbitrary machine words off a conservative stack scan, and every word
    // below CN1_BIBOP_PAGE_SIZE masks to page base 0 -- a small aligned integer left
    // in a stack slot is enough. Probing for 0 matches the first empty entry and
    // returns it as a hit: an all-zero CN1ConsPage whose slotSize the caller then
    // divides by. arm64 answers integer division by zero with 0, so the word resolved
    // to slot 0 of a page that does not exist and the damage stayed silent; x86-64
    // raises SIGFPE, which is how CI found it while every local run passed. The sorted
    // array this replaced could not be reached this way -- every element in it was a
    // real page base -- so the hazard arrived with the table, not with the workload.
    // No page can live at address 0, so rejecting the key outright loses nothing.
    if(cand == 0 || cn1ConsPgN == 0) {
        return 0;
    }
    unsigned mask = (unsigned)cn1ConsPgMask;
    unsigned i = cn1PtrMix((uintptr_t)cand) & mask;
    for(;;) {
        CN1ConsPage* e = &cn1ConsPg[i];
        if(e->base == cand) {
            return e;
        }
        if(e->base == 0) {
            return 0;
        }
        i = (i + 1) & mask;
    }
}

// Rebuild the page index from the registry, into a table sized once up front.
//
// ALL OR NOTHING, and never in place. The live table is not touched until a COMPLETE
// replacement exists, because a partial index is not a slow index -- it is a silently
// wrong one. A page missing from it makes cn1ConservativeResolve reject every
// reference into that page, gcMarkObject's guard then skips the object, and the sweep
// frees it while it is still reachable. The registry is a prepend list, so a rebuild
// that gave up part way through would keep the NEWEST pages and drop the oldest --
// precisely the ones holding a long-lived live set -- and it would do so on
// allocation failure, i.e. exactly when memory pressure makes a collection matter.
//
// So: size for the count we read (plus slack for pages registered while we walk),
// allocate, fill, and publish only on success. On any failure the previous table
// stays in place and cn1ConsPgIndexedCount is left alone, so the next cycle retries;
// what that table is missing is pages registered since it was built, whose objects
// are mark==-1 fresh and survive on the sweep's grace rule -- the same exposure a
// page registered mid-snapshot has always had.
//
// Returns CN1_CONS_PG_OK, or which of the two ways it failed -- they are not the same
// failure. Outgrowing the size picked is a lost race against a mutator registering
// pages, harmless and self-correcting; being unable to allocate at all is not.
#define CN1_CONS_PG_OK      0
#define CN1_CONS_PG_RACED   1
#define CN1_CONS_PG_NOMEM   2
static int cn1ConsPgRebuild(long long pageCount) {
    // Load factor <= 1/2, plus slack: the registry can grow between reading the count
    // and loading the head, and running out of room means discarding the work. Retry
    // at double the size if that happens anyway, so a burst of registrations costs a
    // walk rather than a cycle without a rebuild.
    long long want = (pageCount + CN1_CONS_PG_SLACK) * 2;
    for(int attempt = 0 ; attempt < CN1_CONS_PG_REBUILD_ATTEMPTS ; attempt++) {
        int cap = 256;
        while((long long)cap < want && cap < (1 << 30)) {
            cap *= 2;
        }
        CN1ConsPage* fresh = (CN1ConsPage*)calloc((size_t)cap, sizeof(CN1ConsPage));
        if(fresh == 0) {
            return CN1_CONS_PG_NOMEM;
        }
        unsigned mask = (unsigned)(cap - 1);
        int limit = cap / 2;
        int n = 0;
        JAVA_BOOLEAN outgrew = JAVA_FALSE;
        CN1BibopPage* p = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
        while(p != 0) {
            if(n >= limit) {
                outgrew = JAVA_TRUE;
                break;
            }
            char* base = (char*)p;
            unsigned i = cn1PtrMix((uintptr_t)base) & mask;
            while(fresh[i].base != 0) {
                if(fresh[i].base == base) {
                    break;          // already indexed (a registry cycle would be a bug)
                }
                i = (i + 1) & mask;
            }
            if(fresh[i].base == 0) {
                fresh[i].base = base;
                fresh[i].page = p;
                // Geometry stays zero until the per-cycle refresh reads it from the
                // page. bumpIndex zero is what makes the resolver reject every word
                // into this page in the meantime.
                n++;
            }
            p = atomic_load_explicit(&p->nextAll, memory_order_acquire);
        }
        if(outgrew) {
            // Publishing this would drop the TAIL of a prepend list, i.e. the oldest
            // pages -- the ones most likely to hold the live set. Throw it away.
            free(fresh);
            want = (long long)cap * 2;
            continue;
        }
        free(cn1ConsPg);
        cn1ConsPg = fresh;
        cn1ConsPgMask = cap - 1;
        cn1ConsPgN = n;
        return CN1_CONS_PG_OK;
    }
    return CN1_CONS_PG_RACED;
}
#endif

// Per-thread current ThreadLocalData, readable async-signal-safely from the stop
// handler (a plain TLS load). Set in getThreadLocalData (nativeMethods.m).
__thread struct ThreadLocalData* cn1TlsSelf = 0;
static volatile sig_atomic_t cn1GcSignalHandlerInstalled = 0;
static int cn1GcSignalStopMode = -1;  // -1 = uninit; 0 = cooperative+signal-for-native; 1 = signal-for-all

static void cn1ConsExtAdd(JAVA_OBJECT o) {
    if(o == JAVA_NULL) return;
    struct clazz* cls = o->__codenameOneParentClsReference;
    if(cls == 0) return;
    char* base = (char*)o;
    char* hi;
    if(cls->isArray) {
        JAVA_ARRAY a = (JAVA_ARRAY)o;
        char* hdrEnd = base + sizeof(struct JavaArrayPrototype);
        char* dataEnd = hdrEnd;
        if(a->data != 0 && a->length > 0 && a->primitiveSize > 0) {
            dataEnd = (char*)a->data + (long)a->length * a->primitiveSize;
        }
        hi = hdrEnd > dataEnd ? hdrEnd : dataEnd;
    } else {
        hi = base + sizeof(struct JavaObjectPrototype); // header only (instance size not recorded)
    }
    if(cn1ConsExtN == cn1ConsExtCap) {
        cn1ConsExtCap = cn1ConsExtCap ? cn1ConsExtCap * 2 : 4096;
        cn1ConsExt = (CN1ConsExtent*)realloc(cn1ConsExt, cn1ConsExtCap * sizeof(CN1ConsExtent));
    }
    cn1ConsExt[cn1ConsExtN].lo = base;
    cn1ConsExt[cn1ConsExtN].hi = hi;
    cn1ConsExt[cn1ConsExtN].base = o;
    cn1ConsExtN++;
}

static int cn1ConsExtCmp(const void* a, const void* b) {
    char* la = ((const CN1ConsExtent*)a)->lo;
    char* lb = ((const CN1ConsExtent*)b)->lo;
    return (la > lb) - (la < lb);
}

// Rebuild the resolver index. MUST be called while no thread we are about to scan is
// signal-stopped (it reallocs -> would deadlock against a thread frozen mid-malloc).
// O(allObjectsInHeap + pending + bibop pages) -- bounded by the heap the sweep already
// walks, so one rebuild per scanned thread is within the GC's existing complexity.
// Build ONCE PER MARK CYCLE: the walk over allObjectsInHeap + every thread's
// pending list plus the qsort is O(legacy objects * log) -- rebuilding it per
// scanned thread (as the per-thread scan paths naively would) made the GC thread
// spend more time in qsort than in marking on array-heavy workloads, stalling
// mutators parked behind threadBlockedByGC. Rebuilding within one cycle can only
// ever ADD objects allocated after the cycle started -- and those are mark==-1
// fresh, kept alive by the sweep's grace rule whether or not they resolve -- so
// the first build of the cycle is complete for correctness purposes. Nothing is
// freed during mark (sweep runs after), so entries can never go stale mid-cycle.
static int cn1ConsSnapEpoch = -1;
#ifdef CN1_GC_VERIFY
// The QA verifier runs AFTER the sweep, when the cycle's cached snapshot still
// lists every object the sweep just reclaimed. Invalidate it so the next build
// indexes the post-sweep heap.
int cn1ConsSnapEpochReset(void) {
    cn1ConsSnapEpoch = -1;
    return 0;
}
#endif
void cn1GcBuildRootSnapshots(void) {
    if(cn1ConsSnapEpoch == currentGcMarkValue) {
        return; // already built this cycle
    }
    cn1ConsSnapEpoch = currentGcMarkValue;
    cn1ConsExtN = 0;
    int n = currentSizeOfAllObjectsInHeap;
    for(int i = 0 ; i < n ; i++) {
        cn1ConsExtAdd(allObjectsInHeap[i]);
    }
    // A still-running LIGHTWEIGHT thread grows its pendingHeapAllocations lock-free in
    // codenameOneGcMalloc / cn1AddPending: malloc tmp; memcpy; free(old); pending = tmp.
    // Threads other than the one currently being scanned are NOT parked here, so reading
    // their array without serialization is a use-after-free: the free() of the old array
    // turns our read of th->pendingHeapAllocations[j] into a read of reclaimed memory, and
    // the resulting garbage word is taken as a heap extent base -> SIGBUS in gcMarkObject.
    // Take threadHeapMutex around the whole read loop; the realloc fast-paths now take the
    // SAME mutex (for lightweight threads too) so the malloc/memcpy/free/swap is atomic wrt
    // this read. The lock is acquired and RELEASED entirely here, before the caller signal-
    // stops any thread, so no thread is ever frozen mid-realloc holding it (no deadlock);
    // it is never inverted against lockCriticalSection (the migration path takes
    // criticalSection THEN threadHeapMutex -- this path takes only threadHeapMutex); and
    // the only libc-allocator calls under it (cn1ConsExtAdd's realloc) acquire the libc
    // lock in the same order as the realloc fast-paths, so there is no lock cycle.
    lockThreadHeapMutex();
    for(int ti = 0 ; ti < NUMBER_OF_SUPPORTED_THREADS ; ti++) {
        struct ThreadLocalData* th = allThreads[ti];
        if(th == 0 || th->pendingHeapAllocations == 0) continue;
        int pn = th->heapAllocationSize;
        for(int j = 0 ; j < pn ; j++) {
            JAVA_OBJECT o = (JAVA_OBJECT)th->pendingHeapAllocations[j];
            if(o != JAVA_NULL && o->__heapPosition == -1) cn1ConsExtAdd(o);
        }
    }
    unlockThreadHeapMutex();
    qsort(cn1ConsExt, cn1ConsExtN, sizeof(CN1ConsExtent), cn1ConsExtCmp);
    // Index the extents by exact base for the resolver's dominant caller. Built AFTER
    // the sort only because it must not be left describing a stale array; the table
    // stores the base pointers themselves, so the sort order is irrelevant to it.
    {
        int cap = 256;
        while(cap < cn1ConsExtN * 2) {
            cap *= 2;
        }
        if(cap - 1 != cn1ConsExtHashMask) {
            char** fresh = (char**)realloc(cn1ConsExtHash, (size_t)cap * sizeof(char*));
            if(fresh != 0) {
                cn1ConsExtHash = fresh;
                cn1ConsExtHashMask = cap - 1;
            }
        }
        if(cn1ConsExtHashMask >= 0) {
            memset(cn1ConsExtHash, 0, (size_t)(cn1ConsExtHashMask + 1) * sizeof(char*));
            unsigned mask = (unsigned)cn1ConsExtHashMask;
            // Stop at half capacity even if that leaves entries unindexed. A failed
            // realloc above leaves the table at its previous size, and filling one to
            // capacity would remove the empty slot that terminates both probe loops --
            // an infinite spin inside the collector. An unindexed extent is only
            // slower: the sorted search below still finds it.
            int limit = (cn1ConsExtHashMask + 1) / 2;
            if(limit > cn1ConsExtN) {
                limit = cn1ConsExtN;
            }
            for(int i = 0 ; i < limit ; i++) {
                char* lo = cn1ConsExt[i].lo;
                unsigned j = cn1PtrMix((uintptr_t)lo) & mask;
                while(cn1ConsExtHash[j] != 0 && cn1ConsExtHash[j] != lo) {
                    j = (j + 1) & mask;
                }
                cn1ConsExtHash[j] = lo;
            }
        }
    }
#ifndef CN1_DISABLE_BIBOP
    // The page registry is GROW-ONLY (nodes never unlink or reorder), so the set of
    // keys in the page index only changes when a page is registered. Rebuild it ONLY
    // when the registration count moved -- the per-cycle indexing of thousands of
    // pages was one of the largest GC costs on allocation-churn workloads (profiled:
    // ~1/3 of the snapshot build). A page registered mid-snapshot is missed by this
    // cycle exactly as it was by the old head-once walk (its objects are covered by
    // the mark==-1 grace); the count mismatch rebuilds on the NEXT cycle.
    {
        long long pageCount = atomic_load_explicit(&bibopAllPagesCount, memory_order_acquire);
        if(pageCount != cn1ConsPgIndexedCount) {
            int rebuilt = cn1ConsPgRebuild(pageCount);
            if(rebuilt == CN1_CONS_PG_OK) {
                // key on the number we actually WALKED: if registrations raced past
                // the count we read, the next cycle's count differs and rebuilds
                cn1ConsPgIndexedCount = cn1ConsPgN;
                cn1GcPageIndexStale = JAVA_FALSE;
            } else {
                // The previous index stays in place and the next cycle retries, but it
                // is now missing pages that are no longer new, so this cycle's mark
                // cannot see everything and the sweep must not run on it. Skipping a
                // collection costs memory; sweeping on an unsound mark costs the heap.
                // This also covers having no index at all (the very first rebuild
                // failing), which needs no separate answer: nothing is swept, so
                // nothing is lost, and the cycle that finally rebuilds marks the whole
                // live set again before anything is freed.
                cn1GcPageIndexStale = JAVA_TRUE;
            }
        }
    }
    // Per-cycle geometry refresh. Walks the table LINEARLY rather than probing it
    // page by page, so the refresh stays a sequential sweep over one array however
    // scattered the page bases are.
    for(int pgI = 0 ; pgI <= cn1ConsPgMask ; pgI++) {
        CN1ConsPage* e = &cn1ConsPg[pgI];
        if(e->base == 0) {
            continue;
        }
        CN1BibopPage* p = e->page;
        // Load bumpIndex FIRST (acquire), then the geometry. A page popped from
        // freePool is reformatted by the acquiring MUTATOR (cn1BibopFormatPage
        // rewrites slotSize/firstSlotOffset/slotCount) concurrently with this walk;
        // its bumpIndex is 0 from the O(1) reclaim until the first allocation's
        // RELEASE store raises it -- which also publishes the new geometry (same
        // thread). So: bump==0 -> the resolver rejects every word into this page
        // (geometry may be torn but is never used); bump>0 -> the acquire makes the
        // matching geometry visible. Reading geometry BEFORE the acquire could pair
        // old geometry with the new bump -> misresolved interior words.
        e->bumpIndex = atomic_load_explicit(&p->bumpIndex, memory_order_acquire);
        e->firstSlotOffset = p->firstSlotOffset;
        e->slotSize = p->slotSize;
        e->slotCount = p->slotCount;
    }
#endif
    if(getenv("CN1_SNAP_DEBUG")) {
        fprintf(stderr, "[SNAP] ext=%d pages=%d tableSize=%d\n",
            cn1ConsExtN,
#ifndef CN1_DISABLE_BIBOP
            cn1ConsPgN,
#else
            0,
#endif
            currentSizeOfAllObjectsInHeap);
    }
}

#ifdef CN1_RESOLVE_DIAG
static long cn1ResolveDiagCounts[4] = {0,0,0,0};
static const char* cn1ResolveDiagSampleCls[4] = {0,0,0,0};
void cn1ResolveDiagNote(int reason, JAVA_OBJECT o) {
    if(reason < 1 || reason > 3) return;
    cn1ResolveDiagCounts[reason]++;
    if(cn1ResolveDiagSampleCls[reason] == 0 && o->__codenameOneParentClsReference
       && o->__codenameOneParentClsReference->clsName) {
        cn1ResolveDiagSampleCls[reason] = o->__codenameOneParentClsReference->clsName;
    }
}
void cn1ResolveDiagReport(void) {
    if(cn1ResolveDiagCounts[1] || cn1ResolveDiagCounts[2] || cn1ResolveDiagCounts[3]) {
        fprintf(stderr, "CN1RESOLVEDIAG idx>=bump=%ld(%s) FREE=%ld(%s) heapPosBad=%ld(%s)\n",
            cn1ResolveDiagCounts[1], cn1ResolveDiagSampleCls[1] ? cn1ResolveDiagSampleCls[1] : "-",
            cn1ResolveDiagCounts[2], cn1ResolveDiagSampleCls[2] ? cn1ResolveDiagSampleCls[2] : "-",
            cn1ResolveDiagCounts[3], cn1ResolveDiagSampleCls[3] ? cn1ResolveDiagSampleCls[3] : "-");
        fflush(stderr);
    }
    cn1ResolveDiagCounts[1] = cn1ResolveDiagCounts[2] = cn1ResolveDiagCounts[3] = 0;
}
#endif

// (a) Resolve an arbitrary machine word to the base of the live heap object it points
// into (interior pointers included), or JAVA_NULL.
JAVA_OBJECT cn1ConservativeResolve(void* w) {
    uintptr_t v = (uintptr_t)w;
    if(v == 0) return JAVA_NULL;
    if((v & (sizeof(void*) - 1)) != 0) return JAVA_NULL; // reject unaligned / tagged-Integer

#ifndef CN1_DISABLE_BIBOP
    {
        char* cand = (char*)(v & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        CN1ConsPage* pg = cn1ConsPgFind(cand);
        if(pg != 0) {
                long off = (long)((char*)w - cand);
                if(off < pg->firstSlotOffset) return JAVA_NULL;  // inside page header
                int idx = (int)((off - pg->firstSlotOffset) / pg->slotSize);
#ifdef CN1_RESOLVE_DIAG
                // Forensic: count rejections of slot-region words whose slot LOOKS like a
                // live object (plausible aligned parentCls in the app text) but is rejected.
                // A nonzero count during the paint cycle = the conservative scan is dropping
                // a live frameless root. Distinguishes "resolve rejects it" from "not scanned
                // at all" (referenced from an untraced field elsewhere).
                if(idx >= 0 && idx < pg->slotCount) {
                    JAVA_OBJECT __o = (JAVA_OBJECT)(cand + pg->firstSlotOffset + (long)idx * pg->slotSize);
                    struct clazz* __pc = __o->__codenameOneParentClsReference;
                    extern void cn1ResolveDiagNote(int reason, JAVA_OBJECT o);
                    if(__pc != 0 && (((uintptr_t)__pc & 7) == 0)) {
                        int __m = __o->__codenameOneGcMark;
                        if(idx >= pg->bumpIndex) cn1ResolveDiagNote(1, __o);         // idx >= snapshot bump
                        else if(__m == CN1_BIBOP_FREE_MARK) cn1ResolveDiagNote(2, __o); // FREE_MARK
                        else if(__o->__heapPosition != CN1_BIBOP_HEAP_POS && __o->__heapPosition != CN1_BIBOP_ADOPTED) cn1ResolveDiagNote(3, __o); // heapPos
                    }
                }
#endif
                if(idx < 0 || idx >= pg->bumpIndex || idx >= pg->slotCount) return JAVA_NULL;
                JAVA_OBJECT o = (JAVA_OBJECT)(cand + pg->firstSlotOffset + (long)idx * pg->slotSize);
                // ACQUIRE: the slot may be getting reused RIGHT NOW by a mutator
                // (freelist pop -> header stores -> mark=-1 RELEASE). Pairing with
                // that release orders our subsequent __heapPosition and (in
                // gcMarkObject) parentCls/body reads after the mark read, so a
                // stale stack word can never resolve a half-reinitialized slot
                // whose first 8 bytes still hold the free-list next pointer.
                int m = __atomic_load_n(&o->__codenameOneGcMark, __ATOMIC_ACQUIRE);
                if(m == CN1_BIBOP_FREE_MARK) return JAVA_NULL;   // on the page free-list
                // Accept both a normal BiBOP slot and a MATURED (adopted) slot -- a
                // matured object's memory is still in this page, so a conservative stack
                // word must still resolve to it or it would be missed as a root and swept.
                if(o->__heapPosition != CN1_BIBOP_HEAP_POS && o->__heapPosition != CN1_BIBOP_ADOPTED) return JAVA_NULL;
                return o;                                        // interior -> slot base
        }
    }
#endif

    // EXACT BASE, O(1). Every caller that resolves a Java REFERENCE -- gcMarkObject's
    // guard on every field the drain follows, which is the overwhelming majority of
    // calls here -- hands us an object base, and a base is a key in this table. The
    // sorted search below exists for the interior pointers only the conservative
    // stack/register scan produces, and paying its ~log2(N) dependent cache misses on
    // every reference field is what made the collector's cost grow with the heap
    // rather than with the live set (issue #5537).
    // Zero is this table's empty marker too, and the same collision applies -- but the
    // key here is the word itself rather than a masked page base, and v == 0 was
    // rejected at the top of this function. No extent has a zero base either
    // (cn1ConsExtAdd drops JAVA_NULL), so a match is always a real key. Keep that
    // early return if this is ever restructured.
    if(cn1ConsExtHashMask >= 0 && cn1ConsExtN > 0) {
        unsigned mask = (unsigned)cn1ConsExtHashMask;
        unsigned i = cn1PtrMix(v) & mask;
        for(;;) {
            char* k = cn1ConsExtHash[i];
            if(k == (char*)w) {
                return (JAVA_OBJECT)w;   // lo == base for every extent (cn1ConsExtAdd)
            }
            if(k == 0) {
                break;
            }
            i = (i + 1) & mask;
        }
    }

    if(cn1ConsExtN > 0) {
        int lo = 0, hi = cn1ConsExtN - 1, found = -1;
        while(lo <= hi) {
            int mid = (lo + hi) >> 1;
            if(cn1ConsExt[mid].lo <= (char*)w) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        if(found >= 0 && (char*)w < cn1ConsExt[found].hi) {
            return cn1ConsExt[found].base;                       // base or array interior
        }
    }
    return JAVA_NULL;
}

#ifdef CN1_GC_VERIFY
// =========================================================================
// QA HEAP VERIFIER (-DCN1_GC_VERIFY). Never compiled into a shipping build.
//
// The collector's correctness hinges on a claim no checksum test can observe:
// after a sweep, NO surviving object may reference memory the sweep reclaimed.
// Every historical failure in this area (issue 5425 and the grace-pass bugs
// around it) violated exactly that claim, and stayed invisible for hours or
// days because the freed memory was immediately recycled into a plausible
// replacement object -- a dangling reference kept reading a valid-looking
// header, and the damage surfaced far away as corrupted String bytes or an
// "impossible" NPE.
//
// This mode makes that claim directly testable by DESTROYING the plausible
// replacement:
//
//   1. POISON. Every reclaimed BiBOP slot and every legacy block the sweep
//      frees is stamped with a poison header + payload instead of being handed
//      straight back to the mutator. Reading through a dangling reference no
//      longer finds a well-formed object.
//   2. QUARANTINE. Freed legacy blocks are NOT returned to the C allocator
//      until CN1_GC_VERIFY_QUARANTINE later frees push them out of a ring, so
//      the poisoned block stays mapped (and recognizable) instead of being
//      reused or unmapped underneath a dangling reference.
//   3. VERIFY. After every sweep, walk every surviving object through its
//      generated mark function with the collector in "verify" mode: instead of
//      marking, each reference field is classified against the page registry,
//      the live-extent index and the quarantine set. A field pointing at a
//      freed slot, a slot beyond its page's bump cursor, or a quarantined
//      block is a use-after-free IN THE MAKING -- reported with the holder's
//      class, the culprit field's mark call site and the victim's class, then
//      aborted at the exact GC cycle that created it.
//
// The pass is deliberately ASYMMETRIC about uncertainty: a reference it cannot
// place (allocated after the snapshot, unmapped, or a mid-construction body)
// is SKIPPED. It reports only what it can prove, so a violation is never a
// false alarm -- at the cost of catching some real defects a cycle later.
// =========================================================================
#define CN1_GC_POISON_MARK  (-77)
#define CN1_GC_POISON_POS   (-77)
#define CN1_GC_POISON_BYTE  0xDE
#ifndef CN1_GC_VERIFY_QUARANTINE
#define CN1_GC_VERIFY_QUARANTINE 65536
#endif
// Power-of-two set, sized well above the ring so probes stay short.
#define CN1_GC_QSET_SIZE (CN1_GC_VERIFY_QUARANTINE * 4)

typedef void (*cn1GcVerifyMarkFn)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);

static JAVA_OBJECT* cn1GcQRing = 0;        // quarantined blocks, insertion order
static int cn1GcQRingPos = 0;
static JAVA_OBJECT* cn1GcQSet = 0;         // open-addressed membership set
static pthread_mutex_t cn1GcQMutex = PTHREAD_MUTEX_INITIALIZER;
static long cn1GcVerifyViolations = 0;
static long cn1GcVerifyChecked = 0;
static int cn1GcVerifyReported = 0;
// cn1GcVerifyActive (declared with the forward declarations above, because
// gcMarkObject's hook reads it) is set only while cn1GcVerifyHeap drives mark
// functions on the GC thread.
static JAVA_OBJECT cn1GcVerifyHolder = JAVA_NULL;
// Where the shared per-field reporter is being driven from. The two callers
// examine the heap at different moments and a report that names the wrong one
// sends a reader looking at the wrong phase of the collector.
static const char* cn1GcVerifyWhen = "after sweep";
// CN1_GC_VERIFY_ALL=1: diagnostic mode that also walks objects the sweep did NOT
// keep. Dead-to-dead dangling is legal, so this is for investigation only --
// never a gate.
static int cn1GcVerifyAllHolders = 0;
static int cn1GcVerifyAging = 0;
static long cn1GcVerifyStatus[6];
static long cn1GcVerifyAge[5];            // referenced-child age histogram (epochs behind)
static const char* cn1GcVerifyCensusCls = 0;
static long cn1GcVerifyCensusAge[5];
static void cn1GcVerifyCensus(JAVA_OBJECT o, int m);

// Which survivors are held to the invariant.
//
// DEFAULT (the gate): objects carrying the CURRENT epoch. The sweep either
// marked them reachable -- and marking traces children, so a dangling field is
// a mark-completeness bug -- or promoted them by the grace rule, in which case
// tracing their subtree was the grace pass's job. Either way a dangling field
// is unambiguous, so the gate contains no judgement calls and cannot cry wolf.
//
// CN1_GC_VERIFY_AGING=1 additionally holds PREVIOUS-epoch survivors (garbage
// aging out of the collector's one-cycle safety margin) to the same rule.
// Those are landmines rather than proven defects: unreachable objects are
// entitled to dangle, but this collector RESURRECTS unreachable objects
// routinely -- a stale word left in a native stack frame conservatively marks
// whatever it points at -- and resurrecting one makes the drain follow its
// field into recycled memory. Use it to survey how many such objects a
// workload leaves lying around, not as a pass/fail gate.
//
// CN1_GC_VERIFY_ALL=1 walks everything, including objects already given up on.
static int cn1GcVerifyHolderKept(int m) {
    if(cn1GcVerifyAllHolders) {
        return m != CN1_BIBOP_FREE_MARK && m != CN1_GC_POISON_MARK;
    }
    if(m == currentGcMarkValue) {
        return 1;
    }
    return cn1GcVerifyAging && m == currentGcMarkValue - 1;
}
static const char* cn1GcVerifyDump = 0;   // CN1_GC_VERIFY_DUMP=<class substring>
static int cn1GcVerifyDumpCount = 0;
// Completed verify passes. A driver that never finishes a collection cycle
// never runs one, and its "no violations" result means only that nothing was
// ever checked -- so this is reported at exit and the harness requires it to be
// nonzero. (GcStress, for one, exits before its single cycle reaches the sweep.)
static long cn1GcVerifyPasses = 0;
static long cn1GcVerifyTotalRefs = 0;
static long cn1GcVerifyTotalViolations = 0;
long cn1GcVerifyFreedSlots = 0;   // page slots reclaimed since the last verify pass
long cn1GcVerifyEarlyFreed = 0;   // of those, slots the per-slot walk would have KEPT
long cn1GcVerifyFreedLegacy = 0;  // legacy blocks quarantined since the last verify pass

static inline int cn1GcQSlot(JAVA_OBJECT o) {
    uintptr_t h = ((uintptr_t)o >> 4) * (uintptr_t)2654435761u;
    return (int)(h & (CN1_GC_QSET_SIZE - 1));
}

// Caller holds cn1GcQMutex. Tombstone-free: removal happens only through
// cn1GcQSetRemove, which repairs the probe chain by reinserting the tail.
static void cn1GcQSetAdd(JAVA_OBJECT o) {
    int i = cn1GcQSlot(o);
    while(cn1GcQSet[i] != JAVA_NULL) {
        if(cn1GcQSet[i] == o) return;
        i = (i + 1) & (CN1_GC_QSET_SIZE - 1);
    }
    cn1GcQSet[i] = o;
}

static void cn1GcQSetRemove(JAVA_OBJECT o) {
    int i = cn1GcQSlot(o);
    while(cn1GcQSet[i] != JAVA_NULL) {
        if(cn1GcQSet[i] == o) {
            cn1GcQSet[i] = JAVA_NULL;
            // Re-insert the rest of this probe chain so no entry is stranded.
            int j = (i + 1) & (CN1_GC_QSET_SIZE - 1);
            while(cn1GcQSet[j] != JAVA_NULL) {
                JAVA_OBJECT moved = cn1GcQSet[j];
                cn1GcQSet[j] = JAVA_NULL;
                cn1GcQSetAdd(moved);
                j = (j + 1) & (CN1_GC_QSET_SIZE - 1);
            }
            return;
        }
        i = (i + 1) & (CN1_GC_QSET_SIZE - 1);
    }
}

static int cn1GcQSetContains(JAVA_OBJECT o) {
    if(cn1GcQSet == 0) return 0;
    int i = cn1GcQSlot(o);
    while(cn1GcQSet[i] != JAVA_NULL) {
        if(cn1GcQSet[i] == o) return 1;
        i = (i + 1) & (CN1_GC_QSET_SIZE - 1);
    }
    return 0;
}

// Poison an object body, leaving the header (class pointer for forensics, the
// poison mark, the poison heap position) intact. size 0 => header only.
static void cn1GcPoisonBody(JAVA_OBJECT o, long size) {
    long hdr = (long)sizeof(struct JavaObjectPrototype);
    if(size > hdr) {
        memset((char*)o + hdr, CN1_GC_POISON_BYTE, (size_t)(size - hdr));
    }
}

// Poison a reclaimed BiBOP slot. The first word is the page free-list link and
// the mark word is the FREE sentinel, so both are preserved; everything after
// the header is destroyed so a dangling read cannot find plausible payload
// (this is what turns "corrupted dictionary word" into a deterministic abort).
void cn1GcVerifyPoisonSlot(JAVA_OBJECT o, int slotSize) {
    long hdr = (long)sizeof(struct JavaObjectPrototype);
    if((long)slotSize > hdr) {
        memset((char*)o + hdr, CN1_GC_POISON_BYTE, (size_t)((long)slotSize - hdr));
    }
    o->__heapPosition = CN1_BIBOP_HEAP_POS;
}

// Replaces free() for legacy blocks. Stamps the poison header, poisons the
// payload (malloc's own size accounting gives the extent -- the object header
// does not record it) and parks the block in the quarantine ring, releasing
// whichever block that displaces.
//
// Returns TRUE when the block was quarantined and the caller must NOT free it,
// FALSE when the quarantine could not be allocated and the caller should free
// the block normally.
JAVA_BOOLEAN cn1GcVerifyQuarantineFree(JAVA_OBJECT obj) {
    pthread_mutex_lock(&cn1GcQMutex);
    if(cn1GcQRing == 0) {
        // Build both tables in locals and publish them together. Assigning the
        // globals as they are allocated would, on a half-failed allocation,
        // leave cn1GcQRing set and cn1GcQSet null -- and the next call, seeing a
        // non-null ring, would skip this block and dereference the null set.
        JAVA_OBJECT* ring = (JAVA_OBJECT*)calloc(CN1_GC_VERIFY_QUARANTINE, sizeof(JAVA_OBJECT));
        JAVA_OBJECT* set = (JAVA_OBJECT*)calloc(CN1_GC_QSET_SIZE, sizeof(JAVA_OBJECT));
        if(ring == 0 || set == 0) {
            free(ring);   // free(0) is a no-op; neither pointer is published
            free(set);
            pthread_mutex_unlock(&cn1GcQMutex);
            return JAVA_FALSE;   // no quarantine memory: fall back to a real free
        }
        cn1GcQRing = ring;
        cn1GcQSet = set;
    }
    long sz = 0;
#if defined(__APPLE__)
    sz = (long)malloc_size(obj);
#elif defined(__linux__)
    sz = (long)malloc_usable_size(obj);
#endif
    cn1GcVerifyFreedLegacy++;
    cn1GcPoisonBody(obj, sz);
    obj->__codenameOneGcMark = CN1_GC_POISON_MARK;
    obj->__heapPosition = CN1_GC_POISON_POS;
    JAVA_OBJECT evicted = cn1GcQRing[cn1GcQRingPos];
    cn1GcQRing[cn1GcQRingPos] = obj;
    cn1GcQSetAdd(obj);
    cn1GcQRingPos++;
    if(cn1GcQRingPos == CN1_GC_VERIFY_QUARANTINE) {
        cn1GcQRingPos = 0;
    }
    if(evicted != JAVA_NULL) {
        cn1GcQSetRemove(evicted);
    }
    pthread_mutex_unlock(&cn1GcQMutex);
    if(evicted != JAVA_NULL) {
        free(evicted);
    }
    return JAVA_TRUE;
}

#define CN1_GC_VS_OK        0
#define CN1_GC_VS_UNKNOWN   1
#define CN1_GC_VS_FREE_SLOT 2
#define CN1_GC_VS_STALE_SLOT 3
#define CN1_GC_VS_QUARANTINED 4
#define CN1_GC_VS_DEAD_AGE  5

// Classify a reference WITHOUT dereferencing anything it has not first proven
// to be mapped. BiBOP pages are never unmapped (the registry is grow-only) and
// quarantined blocks are held allocated, so both are safe to read once the
// address has been placed.
static int cn1GcVerifyClassify(JAVA_OBJECT o, CN1BibopPage** outPage, int* outIdx) {
    uintptr_t v = (uintptr_t)o;
    if(v == 0 || (v & (sizeof(void*) - 1)) != 0) return CN1_GC_VS_UNKNOWN;
#ifndef CN1_DISABLE_BIBOP
    {
        char* cand = (char*)(v & ~((uintptr_t)(CN1_BIBOP_PAGE_SIZE - 1)));
        if(cn1ConsPgFind(cand) != 0) {
            // The index key IS the page pointer; read geometry live so a page
            // reformatted since the snapshot is judged by its current shape
            // rather than a stale one.
            CN1BibopPage* p = (CN1BibopPage*)cand;
            long off = (long)((char*)o - cand);
            int first = p->firstSlotOffset;
            int ss = p->slotSize;
            if(ss <= 0 || off < first) return CN1_GC_VS_UNKNOWN;
            int idx = (int)((off - first) / ss);
            if(idx < 0 || idx >= p->slotCount) return CN1_GC_VS_UNKNOWN;
            if(outPage != 0) *outPage = p;
            if(outIdx != 0) *outIdx = idx;
            int bump = atomic_load_explicit(&p->bumpIndex, memory_order_acquire);
            if(idx >= bump) return CN1_GC_VS_STALE_SLOT;
            int m = __atomic_load_n(&o->__codenameOneGcMark, __ATOMIC_ACQUIRE);
            if(m == CN1_BIBOP_FREE_MARK) return CN1_GC_VS_FREE_SLOT;
            return CN1_GC_VS_OK;
        }
    }
#endif
    if(cn1GcQSetContains(o)) return CN1_GC_VS_QUARANTINED;
    if(cn1ConsExtN > 0) {
        int lo = 0, hi = cn1ConsExtN - 1, found = -1;
        while(lo <= hi) {
            int mid = (lo + hi) >> 1;
            if(cn1ConsExt[mid].lo <= (char*)o) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        if(found >= 0 && (char*)o < cn1ConsExt[found].hi) return CN1_GC_VS_OK;
    }
    return CN1_GC_VS_UNKNOWN;
}

static const char* cn1GcVerifyClsName(JAVA_OBJECT o) {
    if(o == JAVA_NULL) return "(null)";
    struct clazz* c = o->__codenameOneParentClsReference;
    if(c == 0) return "(unpublished)";
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    if(!cn1ClazzRegistryContains((uintptr_t)c)) return "(unregistered)";
#endif
    return c->clsName ? c->clsName : "(unnamed)";
}

// CN1_GC_VERIFY_CENSUS=<class substring>: age histogram of every object of a
// named class still resident in the heap, printed once per cycle. This is how a
// driver author confirms the hazard it thinks it built actually exists -- an
// object that never ages is being kept alive by something (very often the
// conservative stack scan), and a driver in that state is testing nothing.
static void cn1GcVerifyCensus(JAVA_OBJECT o, int m) {
    if(cn1GcVerifyCensusCls == 0) return;
    struct clazz* c = o->__codenameOneParentClsReference;
    if(c == 0 || c->clsName == 0 || m == CN1_BIBOP_FREE_MARK || m == CN1_GC_POISON_MARK) return;
    if(strstr(c->clsName, cn1GcVerifyCensusCls) == 0) return;
    int age = m == -1 ? 0 : currentGcMarkValue - m;
    if(age < 0) age = 0;
    if(age > 4) age = 4;
    cn1GcVerifyCensusAge[age]++;
}

// ---- Resurrection audit -------------------------------------------------
// Objects revived after aging past the keep threshold, recorded during the
// mark and re-examined just before the sweep. The pairing that matters is a
// resurrected object still holding a reference into reclaimed memory: the
// drain follows that field, and whatever now occupies the slot is traced as
// the old type.
#define CN1_GC_RESURRECT_RING 8192
static JAVA_OBJECT cn1GcResRing[CN1_GC_RESURRECT_RING];
static const char* cn1GcResPhase[CN1_GC_RESURRECT_RING];
static int cn1GcResCount = 0;
static long cn1GcResTotal = 0;
static long cn1GcResDangling = 0;

void cn1GcNoteResurrected(JAVA_OBJECT o, const char* phase) {
    cn1GcResTotal++;
    if(cn1GcResCount < CN1_GC_RESURRECT_RING) {
        cn1GcResRing[cn1GcResCount] = o;
        cn1GcResPhase[cn1GcResCount] = phase;
        cn1GcResCount++;
    }
}

// Run before the sweep, with the mark's resolver snapshot still valid.
void cn1GcResurrectAudit(CODENAME_ONE_THREAD_STATE) {
    if(cn1GcResCount == 0) return;
    long before = cn1GcVerifyViolations;
    int reported = cn1GcVerifyReported;
    // This runs at the END OF THE MARK, not after the sweep, so it classifies
    // against the snapshot the mark built -- correct here, because the memory
    // it looks for was reclaimed by EARLIER cycles. Label the shared reporter
    // accordingly; "after sweep" would point a reader at the wrong phase.
    cn1GcVerifyWhen = "at mark end (resurrection audit)";
    cn1GcVerifyActive = 1;
    for(int i = 0 ; i < cn1GcResCount ; i++) {
        JAVA_OBJECT o = cn1GcResRing[i];
        struct clazz* c = o->__codenameOneParentClsReference;
        if(c == 0 || c->markFunction == 0) continue;
        if(!cn1ClazzRegistryContains((uintptr_t)c)) continue;
        cn1GcVerifyHolder = o;
        long v0 = cn1GcVerifyViolations;
        ((cn1GcVerifyMarkFn)c->markFunction)(threadStateData, o, JAVA_FALSE);
        if(cn1GcVerifyViolations > v0) {
            cn1GcResDangling++;
            if(reported < 3) {
                reported++;
                fprintf(stderr, "[GC-RESURRECT] %s revived by %s still references "
                        "reclaimed memory (%ld dangling field(s))\n",
                        c->clsName ? c->clsName : "?",
                        cn1GcResPhase[i] ? cn1GcResPhase[i] : "?",
                        cn1GcVerifyViolations - v0);
                fflush(stderr);
            }
        }
    }
    cn1GcVerifyActive = 0;
    cn1GcVerifyHolder = JAVA_NULL;
    cn1GcVerifyWhen = "after sweep";
    cn1GcVerifyReported = reported;
    cn1GcVerifyViolations = before;   // counted separately from the post-sweep gate
    cn1GcResCount = 0;
}

// The verify-mode substitute for marking: called from gcMarkObject for every
// reference field of every surviving object.
// markSite is captured by the CALLER (gcMarkObject), where the return address
// is the instruction inside the generated mark function that read this field.
// Reading it here instead would name gcMarkObject itself -- one frame too deep,
// and useless for mapping a violation back to a field.
void cn1GcVerifyChild(JAVA_OBJECT child, void* markSite) {
    CN1BibopPage* pg = 0;
    int idx = -1;
    cn1GcVerifyChecked++;
    int st = cn1GcVerifyClassify(child, &pg, &idx);
    if(st == CN1_GC_VS_UNKNOWN) {
        // NOT PLACED -- and this is the one branch that must never touch the
        // object. An unplaceable value is exactly the case where the address
        // may be unmapped (a mid-construction body, a stale field), so every
        // read below, down to the age histogram and the class name in the dump
        // path, would fault. Skipping such references is the documented
        // contract; returning here is what makes it true.
        cn1GcVerifyStatus[st]++;
        return;
    }
    // Everything past this point has been placed in a live page, a live extent
    // or the quarantine, all of which are mapped for the duration of this pass.
    if(st == CN1_GC_VS_OK) {
        // The memory is still mapped and still looks like an object -- but the
        // sweep frees on AGE (mark < epoch-1), and physical reclamation lags
        // that decision by however long it takes the object's page to be
        // retired and swept. An object whose mark is already below the free
        // threshold has been given up on: a kept object still referencing it is
        // the same defect as referencing memory already handed back, just
        // observed one step earlier. This is the check that catches a
        // grace-kept parent whose subtree was never traced.
        //
        // Exclusions: mark==-1 (allocated after this cycle began), the class
        // objects gcMarkObject itself skips, and registered immortals (removed
        // from the heap table on purpose -- interned strings, static-final
        // values -- whose marks are not maintained).
        int cm = __atomic_load_n(&child->__codenameOneGcMark, __ATOMIC_ACQUIRE);
        if(cm != -1 && cm < currentGcMarkValue - 1
           && child->__codenameOneParentClsReference != (&class__java_lang_Class)
           && !cn1GcImmortalObjContains(child)) {
            st = CN1_GC_VS_DEAD_AGE;
        }
    }
    cn1GcVerifyStatus[st]++;
    {
        int __cm = child->__codenameOneGcMark;
        int __age = __cm == -1 ? 0 : currentGcMarkValue - __cm;
        if(__age < 0) __age = 0;
        if(__age > 4) __age = 4;
        cn1GcVerifyAge[__age]++;
    }
    if(cn1GcVerifyDump != 0 && cn1GcVerifyDumpCount < 12 && cn1GcVerifyHolder != JAVA_NULL) {
        const char* hn = cn1GcVerifyClsName(cn1GcVerifyHolder);
        if(strstr(hn, cn1GcVerifyDump) != 0) {
            cn1GcVerifyDumpCount++;
            fprintf(stderr, "[GC-VERIFY-DUMP] epoch=%d holder=%s mark=%d(%+d) -> child=%s mark=%d(%+d) heapPos=%d status=%d\n",
                    currentGcMarkValue, hn, cn1GcVerifyHolder->__codenameOneGcMark,
                    cn1GcVerifyHolder->__codenameOneGcMark - currentGcMarkValue,
                    cn1GcVerifyClsName(child), child->__codenameOneGcMark,
                    child->__codenameOneGcMark - currentGcMarkValue,
                    child->__heapPosition, st);
        }
    }
    if(st == CN1_GC_VS_OK) {
        return;
    }
    cn1GcVerifyViolations++;
    if(cn1GcVerifyReported >= 20) {
        return;
    }
    cn1GcVerifyReported++;
    // The holder's own mark function is the frame markSite points into (for an
    // array element it is gcMarkArrayObject, the array's mark function), so the
    // offset is small and positive -- which is also what makes the address
    // usable: add it to the mark function's symbol to reach the exact field.
    void* holderFn = (cn1GcVerifyHolder != JAVA_NULL
                      && cn1GcVerifyHolder->__codenameOneParentClsReference != 0)
        ? (void*)cn1GcVerifyHolder->__codenameOneParentClsReference->markFunction : (void*)0;
    const char* what = st == CN1_GC_VS_FREE_SLOT ? "FREED BiBOP slot"
                     : st == CN1_GC_VS_STALE_SLOT ? "RECYCLED page slot (above bump cursor)"
                     : st == CN1_GC_VS_DEAD_AGE ? "object AGED OUT by this sweep (mark below the free threshold)"
                     : "FREED legacy block (quarantined)";
    fprintf(stderr,
        "[GC-VERIFY] DANGLING REFERENCE %s at epoch %d\n"
        "            holder  = %p class=%s mark=%d (epoch%+d) heapPos=%d\n"
        "            field   -> %p class=%s mark=%d heapPos=%d\n"
        "            victim  = %s%s\n"
        "            markSite= %p = %s+%ld (the field read, inside the holder's mark function)\n",
        cn1GcVerifyWhen, currentGcMarkValue,
        (void*)cn1GcVerifyHolder, cn1GcVerifyClsName(cn1GcVerifyHolder),
        cn1GcVerifyHolder != JAVA_NULL ? cn1GcVerifyHolder->__codenameOneGcMark : 0,
        cn1GcVerifyHolder != JAVA_NULL
            ? cn1GcVerifyHolder->__codenameOneGcMark - currentGcMarkValue : 0,
        cn1GcVerifyHolder != JAVA_NULL ? cn1GcVerifyHolder->__heapPosition : 0,
        (void*)child, cn1GcVerifyClsName(child),
        child->__codenameOneGcMark, child->__heapPosition,
        what,
        pg != 0 ? " (page-resident)" : "",
        markSite, holderFn != 0 ? "markFn" : "?",
        holderFn != 0 ? (long)((char*)markSite - (char*)holderFn) : 0L);
    fflush(stderr);
}

// Walk every object that SURVIVED the sweep and validate its reference fields.
// Holders come from both halves of the heap: every occupied BiBOP slot and
// every live entry of the legacy table. Runs on the GC thread immediately
// after the sweep, before the collector hands the world back, so the freed
// memory it is looking for has had the least possible chance of being
// recycled into something plausible again.
static void cn1GcVerifySummary(void) {
    fprintf(stderr, "[GC-VERIFY] SUMMARY passes=%ld refs=%ld violations=%ld earlyFreed=%ld resurrected=%ld resurrectedDangling=%ld\n",
            cn1GcVerifyPasses, cn1GcVerifyTotalRefs, cn1GcVerifyTotalViolations,
            cn1GcVerifyEarlyFreed, cn1GcResTotal, cn1GcResDangling);
    fflush(stderr);
}

void cn1GcVerifyHeap(CODENAME_ONE_THREAD_STATE) {
    cn1GcFaultInit();   // idempotent; the mark's grace pass already resolved it
    if(cn1GcVerifyPasses == 0) {
        atexit(cn1GcVerifySummary);
    }
    cn1GcVerifyPasses++;
    extern void cn1GcBuildRootSnapshots(void);
    extern int cn1ConsSnapEpochReset(void);
    // Force a rebuild: the cached snapshot predates this sweep and still lists
    // the objects it just reclaimed.
    cn1ConsSnapEpochReset();
    cn1GcBuildRootSnapshots();
    cn1GcVerifyViolations = 0;
    cn1GcVerifyChecked = 0;
    cn1GcVerifyReported = 0;
    cn1GcVerifyAllHolders = getenv("CN1_GC_VERIFY_ALL") != 0;
    cn1GcVerifyAging = getenv("CN1_GC_VERIFY_AGING") != 0;
    cn1GcVerifyDump = getenv("CN1_GC_VERIFY_DUMP");
    cn1GcVerifyDumpCount = 0;
    memset(cn1GcVerifyAge, 0, sizeof(cn1GcVerifyAge));
    cn1GcVerifyCensusCls = getenv("CN1_GC_VERIFY_CENSUS");
    memset(cn1GcVerifyCensusAge, 0, sizeof(cn1GcVerifyCensusAge));
    memset(cn1GcVerifyStatus, 0, sizeof(cn1GcVerifyStatus));
    cn1GcVerifyActive = 1;
    long holders = 0;
#ifndef CN1_DISABLE_BIBOP
    {
        CN1BibopPage* p = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
        while(p != 0) {
            int n = atomic_load_explicit(&p->bumpIndex, memory_order_acquire);
            for(int i = 0 ; i < n ; i++) {
                JAVA_OBJECT o = cn1BibopSlot(p, i);
                int m = __atomic_load_n(&o->__codenameOneGcMark, __ATOMIC_ACQUIRE);
                cn1GcVerifyCensus(o, m);
                // HOLDERS ARE EVERYTHING THE SWEEP KEPT -- not just what it
                // proved reachable. The sweep keeps an object at the current epoch
                // (marked, or promoted by the grace rule) AND one at the previous
                // epoch (aging out), and the invariant under test covers both: the
                // collector kept that object precisely because it would not commit
                // to it being dead, so it must not be left pointing into memory the
                // same sweep reclaimed. A kept object whose child was freed one
                // epoch earlier is the exact signature of a keep-rule that did not
                // trace the object's subtree.
                //
                // mark==-1 is excluded: allocated after the sweep began, its body
                // can still be under construction; it is verified next cycle.
                if(!cn1GcVerifyHolderKept(m)) continue;
                struct clazz* c = o->__codenameOneParentClsReference;
                if(c == 0 || c->markFunction == 0) continue;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
                if(!cn1ClazzRegistryContains((uintptr_t)c)) continue;
#endif
                cn1GcVerifyHolder = o;
                holders++;
                ((cn1GcVerifyMarkFn)c->markFunction)(threadStateData, o, JAVA_FALSE);
            }
            p = atomic_load_explicit(&p->nextAll, memory_order_acquire);
        }
    }
#endif
    {
        int t = currentSizeOfAllObjectsInHeap;
        for(int iter = 0 ; iter < t ; iter++) {
            JAVA_OBJECT o = allObjectsInHeap[iter];
            if(o == JAVA_NULL) continue;
            if(o->__heapPosition != CN1_BIBOP_ADOPTED) {
                cn1GcVerifyCensus(o, o->__codenameOneGcMark);   // else counted by the page walk
            }
            struct clazz* c = o->__codenameOneParentClsReference;
            if(c == 0 || c->markFunction == 0) continue;
            int lm = o->__codenameOneGcMark;
            if(!cn1GcVerifyHolderKept(lm)) continue;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
            if(!cn1ClazzRegistryContains((uintptr_t)c)) continue;
#endif
            cn1GcVerifyHolder = o;
            holders++;
            ((cn1GcVerifyMarkFn)c->markFunction)(threadStateData, o, JAVA_FALSE);
        }
    }
    cn1GcVerifyActive = 0;
    cn1GcVerifyHolder = JAVA_NULL;
    cn1GcVerifyTotalRefs += cn1GcVerifyChecked;
    cn1GcVerifyTotalViolations += cn1GcVerifyViolations;
    long freedSlots = cn1GcVerifyFreedSlots;
    long freedLegacy = cn1GcVerifyFreedLegacy;
    cn1GcVerifyFreedSlots = 0;
    cn1GcVerifyFreedLegacy = 0;
    if(cn1GcVerifyViolations > 0) {
        fprintf(stderr, "[GC-VERIFY] epoch=%d holders=%ld refs=%ld VIOLATIONS=%ld "
                "(freeSlot=%ld recycledSlot=%ld quarantined=%ld agedOut=%ld)\n",
                currentGcMarkValue, holders, cn1GcVerifyChecked, cn1GcVerifyViolations,
                cn1GcVerifyStatus[CN1_GC_VS_FREE_SLOT], cn1GcVerifyStatus[CN1_GC_VS_STALE_SLOT],
                cn1GcVerifyStatus[CN1_GC_VS_QUARANTINED], cn1GcVerifyStatus[CN1_GC_VS_DEAD_AGE]);
        fflush(stderr);
        // A dangling reference is never survivable state: everything observed
        // after this point is reading recycled memory. Fail loudly, at the
        // cycle that produced it, unless a run is explicitly collecting a
        // census of every violating cycle.
        if(getenv("CN1_GC_VERIFY_SOFT") == 0) {
            abort();
        }
    } else if(cn1GcVerifyCensusCls != 0) {
        fprintf(stderr, "[GC-CENSUS] epoch=%d %s age[0..4+]=%ld/%ld/%ld/%ld/%ld\n",
                currentGcMarkValue, cn1GcVerifyCensusCls,
                cn1GcVerifyCensusAge[0], cn1GcVerifyCensusAge[1], cn1GcVerifyCensusAge[2],
                cn1GcVerifyCensusAge[3], cn1GcVerifyCensusAge[4]);
        fflush(stderr);
    } else if(getenv("CN1_GC_VERIFY_LOG") != 0) {
        fprintf(stderr, "[GC-VERIFY] epoch=%d holders=%ld refs=%ld clean (ok=%ld unknown=%ld free=%ld stale=%ld quar=%ld dead=%ld) reclaimed=%ld/%ld age[0..4+]=%ld/%ld/%ld/%ld/%ld\n",
                currentGcMarkValue, holders, cn1GcVerifyChecked,
                cn1GcVerifyStatus[0], cn1GcVerifyStatus[1], cn1GcVerifyStatus[2],
                cn1GcVerifyStatus[3], cn1GcVerifyStatus[4], cn1GcVerifyStatus[5],
                freedSlots, freedLegacy,
                cn1GcVerifyAge[0], cn1GcVerifyAge[1], cn1GcVerifyAge[2],
                cn1GcVerifyAge[3], cn1GcVerifyAge[4]);
        fflush(stderr);
    }
}
#endif /* CN1_GC_VERIFY */

// (b) Conservative range scan: read every aligned word in [lo,hi), resolve it, and MARK
// it for real. gcMarkObject in the GC-thread serial context just pushes to the worklist
// (no lock, no malloc), so this is safe to run while mutator threads are stopped.
//
// A conservative stack scan reads EVERY aligned word in [lo,hi), including the
// inter-variable padding a normal build treats as ordinary stack memory. Under
// -fsanitize=address those reads land in ASan's poisoned stack redzones and raise
// guaranteed stack-buffer-underflow false positives that bury any real finding.
// Exempt the scan (standard practice for conservative collectors) so ASan builds
// of the VM surface genuine heap bugs instead. No effect on a normal build.
__attribute__((no_sanitize("address")))
void cn1ConservativeMarkRange(CODENAME_ONE_THREAD_STATE, char* lo, char* hi) {
    if(lo == 0 || hi == 0 || hi <= lo) return;
    char* p = (char*)(((uintptr_t)lo + (sizeof(void*) - 1)) & ~((uintptr_t)(sizeof(void*) - 1)));
    for(; p + sizeof(void*) <= hi ; p += sizeof(void*)) {
        JAVA_OBJECT o = cn1ConservativeResolve(*(void**)p);
        if(o != JAVA_NULL) {
            gcMarkObject(threadStateData, o, JAVA_FALSE);
        }
    }
}

// Portable [high) stack base + size for a given pthread. Stacks grow DOWN, so the base
// is the HIGH address and the live region is [sp, base).
static char* cn1GcStackBase(pthread_t pt, size_t* outSize) {
#if defined(__APPLE__)
    void* base = pthread_get_stackaddr_np(pt);
    size_t ssz = pthread_get_stacksize_np(pt);
    *outSize = ssz;
    return (char*)base;
#elif defined(__linux__)
    pthread_attr_t attr;
    if(pthread_getattr_np(pt, &attr) != 0) { *outSize = 0; return 0; }
    void* addr = 0; size_t ssz = 0;
    pthread_attr_getstack(&attr, &addr, &ssz);  // addr = LOW end on Linux
    pthread_attr_destroy(&attr);
    *outSize = ssz;
    return (char*)addr + ssz;                    // convert to HIGH base
#else
    *outSize = 0; return 0;
#endif
}

// ---- async-signal-safe universal-stop handler ----------------------------------------
// Only stores + spins. Captures the interrupted SP (from the ucontext when available,
// else a handler local that is strictly deeper than the interrupted frame -- safe, it
// only widens the scanned range) and a raw copy of the ucontext (its inline mcontext
// holds the GPRs on macOS/Linux), then spins until the GC publishes gcSigRelease.
#if !defined(_WIN32)
static void cn1GcSignalHandler(int sig, siginfo_t* info, void* ucv) {
    struct ThreadLocalData* t = cn1TlsSelf;
    if(t == 0) return;
    // GENERATION HANDSHAKE (strand-proof): the stop request carries a generation
    // number > 0. We park by publishing gcSigStopped = gen and spin until the GC
    // publishes gcSigRelease >= gen (monotonic). This survives every abandonment
    // interleaving the old boolean protocol did not:
    //   * StopOne times out after the handler passed the request gate -> StopOne
    //     PRE-RELEASES the generation, so the late park exits immediately.
    //   * The GC's bounded release wait expires while we are descheduled, and a
    //     LATER cycle stops us again -> its release value is LARGER, so >= still
    //     frees us; a monotonic release is never reset to 0.
    int gen = (int)t->gcSigStopRequest;
    if(gen == 0) return;
    volatile int marker = 0;
    void* sp = (void*)&marker;
#if !defined(_WIN32)
    if(ucv != 0) {
        ucontext_t* uc = (ucontext_t*)ucv;
#if defined(__APPLE__)
        // CRITICAL (iOS/tvOS/macOS): on Apple, ucontext_t.uc_mcontext is a POINTER to the
        // register file, NOT an inline struct like glibc. memcpy'ing sizeof(ucontext_t) from
        // ucv here would capture only the ~56-byte ucontext header (the uc_mcontext pointer +
        // sigmask/stack), NOT the interrupted GPRs -- so an object reference that is live only
        // in a register (frameless codegen keeps hot object refs in callee-saved x19-x28 across
        // the native draw calls made from paintComponent) is invisible when the EDT is
        // signal-stopped mid-paint, and gets swept -> the intermittent paintComponent NPE /
        // use-after-free on tvOS. Copy the POINTED-TO mcontext (holds __ss with x0-x28/fp/lr/
        // sp/pc) so those registers are scanned by cn1ConservativeMarkRange below.
        if(uc->uc_mcontext) {
#if defined(__aarch64__)
            sp = (void*)uc->uc_mcontext->__ss.__sp;
#elif defined(__x86_64__)
            sp = (void*)uc->uc_mcontext->__ss.__rsp;
#endif
            // Scan ONLY the general-purpose thread state (__ss: x0-x28/fp/lr/sp/pc on arm64,
            // rax..r15/rip on x86_64). Object references only ever live in GPRs -- never in the
            // NEON/FP vector state (__ns is 528 of the 816-byte arm64 mcontext) or the exception
            // state (__es). Copying the whole mcontext would feed all that float data to the
            // conservative scan as spurious "pointers", pinning garbage and bloating the live heap
            // -> heavier/more-frequent GC on allocation-heavy paths (vector-tile rendering). __ss
            // alone captures every object root with the minimum false-positive surface.
            size_t mlen = sizeof(uc->uc_mcontext->__ss);
            if(mlen > sizeof(t->gcSigRegs)) mlen = sizeof(t->gcSigRegs);
            memcpy(t->gcSigRegs, (const void*)&uc->uc_mcontext->__ss, mlen); // GPRs only
            t->gcSigRegsLen = (sig_atomic_t)mlen;
        }
#else
        // Linux: uc_mcontext is inline in ucontext_t, and the GPRs (regs[]/gregs[]) sit at the
        // start, so a bounded copy of the ucontext captures them as scannable data.
#if defined(__x86_64__)
        sp = (void*)uc->uc_mcontext.gregs[REG_RSP];
#elif defined(__aarch64__)
        sp = (void*)uc->uc_mcontext.sp;
#endif
        size_t ulen = sizeof(ucontext_t);
        if(ulen > sizeof(t->gcSigRegs)) ulen = sizeof(t->gcSigRegs);
        memcpy(t->gcSigRegs, ucv, ulen);   // capture the interrupted GPRs as scannable data
        t->gcSigRegsLen = (sig_atomic_t)ulen;
#endif
    }
#endif
    t->gcSigStackPointer = sp;
    __atomic_thread_fence(__ATOMIC_RELEASE);
    t->gcSigStopped = (sig_atomic_t)gen;
    while((int)t->gcSigRelease < gen) { /* async-signal-safe spin */ }
    // Only clear our own park marker -- a late-exiting older handler must not
    // wipe a newer generation's park the GC is currently waiting on.
    if((int)t->gcSigStopped == gen) t->gcSigStopped = 0;
}
#endif // !_WIN32 (signal-stop unavailable; Windows uses the cooperative path)

void cn1GcInstallSignalHandler(void) {
    if(cn1GcSignalHandlerInstalled) return;
    if(cn1GcSignalStopMode < 0) {
        const char* e = getenv("CN1_GC_SIGNAL_STOP");
        cn1GcSignalStopMode = (e != 0 && e[0] == '1') ? 1 : 0;
    }
#if !defined(_WIN32)
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1GcSignalHandler;
    sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigemptyset(&sa.sa_mask);
    sigaction(CN1_GC_STOP_SIGNAL, &sa, 0);
#endif
    cn1GcSignalHandlerInstalled = 1;
}

// Signal-stop one thread, returning its captured SP (or 0 on failure/timeout). The
// resolver snapshot MUST already be built (we do not realloc after the thread freezes).
static char* cn1GcSignalStopOne(struct ThreadLocalData* t) {
#if !defined(_WIN32)
    if(!t->gcPthreadValid) return 0;
    // Next generation for this thread (only the GC thread writes it). gcSigRelease
    // is MONOTONIC and never reset -- see the handler's generation handshake.
    int gen = (int)t->gcSigStopGen + 1;
    t->gcSigStopGen = (sig_atomic_t)gen;
    t->gcSigRegsLen = 0;
    t->gcSigStackPointer = 0;
    __atomic_thread_fence(__ATOMIC_RELEASE);
    t->gcSigStopRequest = (sig_atomic_t)gen;
    if(pthread_kill(t->gcPthread, CN1_GC_STOP_SIGNAL) != 0) { t->gcSigStopRequest = 0; return 0; }
    // bounded wait for the handler to park THIS generation
    int spins = 0;
    while((int)t->gcSigStopped != gen) {
        if(++spins > 2000000) { /* ~timeout: could not stop */ break; }
        if((spins & 1023) == 0) usleep(50);
    }
    if((int)t->gcSigStopped != gen) {
        // Abandon: the signal may still be pending, and the handler may ALREADY be
        // past its request gate about to park. PRE-RELEASE the generation so that
        // park (whenever it happens) exits immediately instead of spinning forever
        // on a release nobody will send -- the strand bug of the boolean protocol.
        t->gcSigRelease = (sig_atomic_t)gen;
        t->gcSigStopRequest = 0;
        return 0;
    }
    return (char*)t->gcSigStackPointer;
#else
    return 0;
#endif
}

static void cn1GcSignalReleaseOne(struct ThreadLocalData* t) {
#if !defined(_WIN32)
    t->gcSigRelease = t->gcSigStopGen;   // monotonic: frees this AND any older park
    __atomic_thread_fence(__ATOMIC_RELEASE);
    int spins = 0;
    while(t->gcSigStopped) { if(++spins > 2000000) break; }
    // Clear the request so a stale still-queued SIGUSR2 delivered after this point
    // sees gen==0 at the handler gate and returns without parking.
    t->gcSigStopRequest = 0;
#endif
}

// Scan ONE thread's native C stack [sp, base) + its register snapshot, marking every
// resolved live object. threadStateData = the GC thread; t = the thread being scanned.
static void cn1GcScanThreadNativeStack(CODENAME_ONE_THREAD_STATE, struct ThreadLocalData* t) {
    if(!t->gcPthreadValid) return;
    size_t ssz = 0;
    char* base = cn1GcStackBase(t->gcPthread, &ssz);
    if(base == 0 || ssz == 0) return;

    // Snapshot rebuilt BEFORE any signal-stop (realloc-while-frozen would deadlock).
    cn1GcBuildRootSnapshots();

    // Cooperative scan iff the thread published a FRESH capture this cycle (it parked at
    // an allocation safepoint). This is the proven, race-free path for lightweight threads.
    // The signal path is reserved for threads with no fresh capture (genuine native threads
    // that the GC does not park) or the forced validation mode.
    int useCoop = t->gcParkCaptured && t->gcStackPointerAtPark != 0 && cn1GcSignalStopMode == 0;
    if(useCoop) {
        char* sp = (char*)t->gcStackPointerAtPark;
        if(sp >= base - (long)ssz && sp < base) {
            cn1ConservativeMarkRange(threadStateData, sp, base);
            cn1ConservativeMarkRange(threadStateData, (char*)&t->gcRegisterSnapshot,
                                     (char*)&t->gcRegisterSnapshot + sizeof(t->gcRegisterSnapshot));
            return;
        }
        // fall through to signal stop if the cooperative capture looked stale
    }

    // SIGNAL path: stop, scan, release. Used for native threads, for the forced
    // CN1_GC_SIGNAL_STOP=1 validation mode, or as a fallback for a stale capture.
    char* sp = cn1GcSignalStopOne(t);
    if(sp == 0) {
        // Could not stop the thread. If it is lightweight and cooperatively captured we
        // can still fall back to that capture; otherwise this thread's frameless roots
        // are at risk -- log once (honest gap).
        if(t->gcParkCaptured && t->gcStackPointerAtPark != 0) {
            char* csp = (char*)t->gcStackPointerAtPark;
            if(csp >= base - (long)ssz && csp < base) {
                cn1ConservativeMarkRange(threadStateData, csp, base);
                cn1ConservativeMarkRange(threadStateData, (char*)&t->gcRegisterSnapshot,
                                         (char*)&t->gcRegisterSnapshot + sizeof(t->gcRegisterSnapshot));
            }
        }
        return;
    }
    if(sp >= base - (long)ssz && sp < base) {
        cn1ConservativeMarkRange(threadStateData, sp, base);
    }
    if(t->gcSigRegsLen > 0) {
        cn1ConservativeMarkRange(threadStateData, t->gcSigRegs, t->gcSigRegs + t->gcSigRegsLen);
    }
    cn1GcSignalReleaseOne(t);
}

// Scan the GC thread's OWN native stack (a root could be live only in a GC-thread C
// local). flushes our callee-saved regs via setjmp into a scanned buffer.
static void cn1GcScanOwnStack(CODENAME_ONE_THREAD_STATE) {
    if(!threadStateData->gcPthreadValid) return;
    jmp_buf ownRegs; (void)CN1_TRY_SETJMP(ownRegs);
    volatile void* spv = (void*)&spv;
    char* sp = (char*)spv;
    size_t ssz = 0;
    char* base = cn1GcStackBase(threadStateData->gcPthread, &ssz);
    if(base == 0 || ssz == 0) return;
    if(sp < base - (long)ssz || sp >= base) return;
    cn1GcBuildRootSnapshots();
    cn1ConservativeMarkRange(threadStateData, sp, base);
    cn1ConservativeMarkRange(threadStateData, (char*)&ownRegs, (char*)&ownRegs + sizeof(ownRegs));
}

static void cn1GcSignalStopThreads(struct ThreadLocalData* self) { (void)self; }
static void cn1GcSignalReleaseThreads(struct ThreadLocalData* self) { (void)self; }

#ifdef CN1_CONSERVATIVE_GC_SELFCHECK
// Transient ⊇ self-check (NOT shipped): every precise OBJECT root on a paused thread's
// object stack must also be resolvable conservatively. A failure means an is-heap-
// address / interior-pointer bug. Counts unmanaged (static/VM-singleton) roots that
// live outside every GC region separately -- those are out of scope, never swept.
static long long cn1SelfMiss = 0, cn1SelfChecked = 0, cn1SelfUnmanaged = 0;
static void cn1GcSelfCheckThreadStack(struct ThreadLocalData* t, int stackSize) {
    cn1GcBuildRootSnapshots();
    for(int i = 0 ; i < stackSize ; i++) {
        struct elementStruct* e = &t->threadObjectStack[i];
        if(e->type != CN1_TYPE_OBJECT) continue;
        JAVA_OBJECT o = e->data.o;
        if(o == JAVA_NULL || CN1_IS_TAGGED(o)) continue;
        if(o->__codenameOneParentClsReference == 0 ||
           o->__codenameOneParentClsReference == (&class__java_lang_Class)) continue;
        cn1SelfChecked++;
        JAVA_OBJECT r = cn1ConservativeResolve((void*)o);
        if(r == JAVA_NULL) { cn1SelfUnmanaged++; continue; } // static/VM singleton, out of scope
        if(r != o) {
            cn1SelfMiss++;
            fprintf(stderr, "[CONS-GC][SELFCHECK][MISS] precise root %p resolved to %p (class=%s)\n",
                (void*)o, (void*)r,
                (o->__codenameOneParentClsReference ? o->__codenameOneParentClsReference->clsName : "?"));
        }
    }
    fprintf(stderr, "[CONS-GC][SELFCHECK] checked=%lld unmanaged=%lld MISS=%lld\n",
        cn1SelfChecked, cn1SelfUnmanaged, cn1SelfMiss);
}
#endif
#endif /* CN1_CONSERVATIVE_GC_ROOTS */

JAVA_OBJECT codenameOneGcMalloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    CN1_CLAZZ_REGISTER(parent); // first-alloc-per-class: exact clazz registry for the GC guard
    if(isAppSuspended) {
        mallocWhileSuspended += size;
        if(mallocWhileSuspended > 100000) {
            java_lang_System_startGCThread__(threadStateData);
            isAppSuspended = NO;
        }
    }
    allocationsSinceLastGC += size;
    totalAllocations += size;
    CN1_ALLOC_CENSUS_COUNT(parent, size);
#ifdef CN1_GC_INSTRUMENT
    extern long long cn1_instr_allocCount; cn1_instr_allocCount++;
#endif
#ifdef CN1_NURSERY
    // Small objects go to the thread-local young generation and bypass the global
    // heap table entirely. Returns 0 (arena exhausted) -> fall through to the heap.
    if(size <= CN1_NURSERY_MAX_OBJECT && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
        JAVA_OBJECT nurseryObj = cn1NurseryAlloc(threadStateData, size, parent);
        if(nurseryObj != JAVA_NULL) {
            return nurseryObj;
        }
    }
#endif
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    // Small objects AND small arrays: serve from the per-thread BiBOP page heap,
    // which skips placeObjectInHeapCollection / allObjectsInHeap entirely. Arrays
    // are single-block (header + contiguous data, see allocArray) so a slot holds
    // the whole thing; the page sweep walks slot boundaries only and the
    // conservative resolver maps interior element pointers to the slot base, so
    // no array-specific GC handling is needed. Moving arrays here removes the
    // dominant legacy-table traffic (char[] buffers, boxed-free int[] work sets):
    // no table registration, no extent-snapshot entry, no per-object free.
    // Objects larger than the biggest size class fall through to the legacy
    // calloc + table-registration path below. 0 => pages unavailable, fall back.
    // nativeAllocationMode no longer forces the legacy path: its purpose was to
    // keep native-held objects visible to the collector via the pending table,
    // and under CN1_CONSERVATIVE_GC_ROOTS (always on) a native's C locals are
    // scanned as roots directly. Keeping natives on the legacy path made every
    // native-bracketed allocation (e.g. StringBuilder.append's buffer growth) a
    // table+extent entry -- measured: 1.5M-entry extent snapshots qsorted per GC
    // cycle during string churn, stalling mutators. The mode still suppresses GC
    // triggers/parks inside the bracket (cn1BibopMaybeGc honors it).
    if(parent != 0 && size <= CN1_BIBOP_MAX_OBJECT && constantPoolObjects != 0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
       && !threadStateData->nativeAllocationMode
#endif
       ) {
        JAVA_OBJECT bibopObj = cn1BibopAlloc(threadStateData, size, parent);
        if(bibopObj != JAVA_NULL) {
            return bibopObj;
        }
    }
#endif
    // cache the getenv -- this is the legacy-allocation hot path (a per-alloc
    // getenv showed up as __findenv_locked in the MvtBench mutator profile)
    static int cn1LegacyDbgOn = -1;
    if(cn1LegacyDbgOn < 0) cn1LegacyDbgOn = getenv("CN1_LEGACY_DEBUG") ? 1 : 0;
    if(cn1LegacyDbgOn) {
        static _Atomic long cn1LegacyDbgCount = 0;
        long c = atomic_fetch_add_explicit(&cn1LegacyDbgCount, 1, memory_order_relaxed);
        if((c % 100000) == 0) {
            fprintf(stderr, "[LEGACY] #%ld cls=%s size=%d nativeMode=%d lastSetter=%s\n",
                c, parent ? parent->clsName : "?", size,
                (int)threadStateData->nativeAllocationMode,
                cn1LastNamSetter ? cn1LastNamSetter : "(cleared)");
        }
    }
    // Relaxed: this gate is read on every legacy allocation and the flag carries
    // no ordering relationship -- a raise seen one allocation late costs nothing,
    // and a seq_cst load here would put an acquire barrier on the hot path.
    if(atomic_load_explicit(&lowMemoryMode, memory_order_relaxed)
            && !threadStateData->nativeAllocationMode) {
        // Backpressure after an OS memory warning, PACED (issue #5482). This used
        // to park 1ms on every legacy allocation, which is not backpressure but a
        // hard ceiling of ~1000 legacy allocations/second per thread -- three
        // orders of magnitude under the un-throttled rate. A loader that allocates
        // a few hundred thousand buffers then takes minutes to hours instead of
        // seconds, with no crash and no log: an apparent hang. It only ever bit
        // iOS, because nothing else raises lowMemoryMode -- the simulator on a
        // large-RAM host essentially never delivers a memory warning, and Android
        // has no equivalent path -- so it read as "works everywhere but the
        // device". Parking is now capped at one per CN1_LOW_MEMORY_PARK_INTERVAL_MS
        // per thread, bounding the cost at that duty cycle however fast the thread
        // allocates. Waiting out a collector that has actually stopped the world is
        // a safepoint rather than a throttle, so it is honored every time.
        JAVA_BOOLEAN blockedByGc = threadStateData->threadBlockedByGC;
        JAVA_BOOLEAN throttle = JAVA_FALSE;
        if(!blockedByGc) {
            JAVA_LONG nowMs = cn1MonotonicMillis();
            if(nowMs - cn1LowMemoryParkStampMs >= CN1_LOW_MEMORY_PARK_INTERVAL_MS) {
                cn1LowMemoryParkStampMs = nowMs;
                throttle = JAVA_TRUE;
            }
        }
        if(cn1LowMemoryTraceOn()) {
            atomic_fetch_add_explicit(&cn1LowMemoryThrottledAllocations, 1, memory_order_relaxed);
            if(throttle) {
                // Counts THROTTLE parks only. A safepoint wait behind
                // threadBlockedByGC lasts as long as the collector needs and is
                // not part of the pacing budget the regression guard asserts on.
                atomic_fetch_add_explicit(&cn1LowMemoryParks, 1, memory_order_relaxed);
            }
        }
        if(blockedByGc || throttle) {
            CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
            threadStateData->threadActive = JAVA_FALSE;
            if(throttle) {
                usleep((JAVA_INT)(1000));
            }
            while(threadStateData->threadBlockedByGC) {
                usleep((JAVA_INT)(1000));
            }
            threadStateData->threadActive = JAVA_TRUE;
        }
    }
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    totalAllocatedHeap += size;
    int* ptr = (int*)malloc(size + sizeof(int));
    *ptr = size;
    ptr++;
    JAVA_OBJECT o = (JAVA_OBJECT)ptr;
    JAVA_BOOLEAN needsZeroing = JAVA_TRUE;
#else
    // calloc instead of malloc+memset: the allocator returns zero-filled memory,
    // and for large/array allocations the kernel can hand back lazily-zeroed
    // (copy-on-write zero) pages, avoiding an eager memset pass over memory that
    // is about to be written anyway. Object-header fields are set explicitly below.
    JAVA_OBJECT o = (JAVA_OBJECT)calloc(1, size);
    JAVA_BOOLEAN needsZeroing = JAVA_FALSE;
#endif
    if(o == NULL) {
        // malloc failed! We need to free up RAM FAST!
        invokedGC = YES;
        threadStateData->threadActive = JAVA_FALSE;
        java_lang_System_gc__(getThreadLocalData());
        while(threadStateData->threadBlockedByGC) {
            usleep((JAVA_INT)(1000));
        }
        invokedGC = NO;
        threadStateData->threadActive = JAVA_TRUE;
        return codenameOneGcMalloc(threadStateData, size, parent);
    }
    if(needsZeroing) {
        memset(o, 0, size);
    }
    o->__codenameOneParentClsReference = parent;
    o->__codenameOneGcMark = -1;
    o->__heapPosition = -1;
#ifdef DEBUG_GC_ALLOCATIONS
    o->className = threadStateData->callStackClass[threadStateData->callStackOffset - 1];
    o->line = threadStateData->callStackLine[threadStateData->callStackOffset - 1];
#endif
    
    if(threadStateData->heapAllocationSize == threadStateData->threadHeapTotalSize) {
        if(threadStateData->threadBlockedByGC && !threadStateData->nativeAllocationMode) {
            CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
            threadStateData->threadActive = JAVA_FALSE;
            while(threadStateData->threadBlockedByGC) {
                usleep(1000);
            }
            threadStateData->threadActive = JAVA_TRUE;
        }
        long maxHeapSize = CN1_MAX_HEAP_SIZE;
        if (isEdt(threadStateData->threadId) && !lowMemoryMode) {
            maxHeapSize = CN1_MAX_HEAP_SIZE_EDT;
        }
        
        
        
        if(threadStateData->heapAllocationSize > maxHeapSize && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
            CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
            threadStateData->threadActive=JAVA_FALSE;
            while(gcCurrentlyRunning) {
                usleep((JAVA_INT)(1000));
            }
            threadStateData->threadActive=JAVA_TRUE;
            
            if(threadStateData->heapAllocationSize > 0 ) {
                invokedGC = YES;
                threadStateData->nativeAllocationMode = JAVA_TRUE;
                java_lang_System_gc__(threadStateData);
                threadStateData->nativeAllocationMode = JAVA_FALSE;
                CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
                threadStateData->threadActive = JAVA_FALSE;
                while(threadStateData->threadBlockedByGC || threadStateData->heapAllocationSize > 0) {
                    if (get_static_java_lang_System_gcThreadInstance() == JAVA_NULL) {
                        // For some reason the gcThread is dead
                        threadStateData->nativeAllocationMode = JAVA_TRUE;
                        java_lang_System_gc__(threadStateData);
                        threadStateData->nativeAllocationMode = JAVA_FALSE;
                        threadStateData->threadActive = JAVA_FALSE;
                    }
                    usleep((JAVA_INT)(1000));
                }
                invokedGC = NO;
                threadStateData->threadActive = JAVA_TRUE;
            }
        } else {
            if(threadStateData->heapAllocationSize == threadStateData->threadHeapTotalSize) {
                
                // Let's trigger a GC here.
                if(!gcCurrentlyRunning && constantPoolObjects != 0 && !threadStateData->nativeAllocationMode) {
                    threadStateData->nativeAllocationMode = JAVA_TRUE;
                    java_lang_System_gc__(threadStateData);
                    threadStateData->nativeAllocationMode = JAVA_FALSE;
                }
                // Serialize the grow-and-free against cn1GcBuildRootSnapshots, which reads
                // this array from the GC thread while this (possibly lightweight, still-
                // running) thread is NOT parked. The OLD guard skipped the lock for
                // lightweight threads, so the GC could read pendingHeapAllocations right as
                // free() reclaimed it -> use-after-free. Lock unconditionally; the snapshot
                // reader takes the SAME mutex. Held only across malloc/memcpy/free/swap (no
                // park, no signal-stop inside), so it cannot deadlock the GC.
                lockThreadHeapMutex();
                void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
                memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
                threadStateData->threadHeapTotalSize *= 2;
                free(threadStateData->pendingHeapAllocations);
                threadStateData->pendingHeapAllocations = tmp;
                unlockThreadHeapMutex();
            }
        }
    }
    threadStateData->pendingHeapAllocations[threadStateData->heapAllocationSize] = o;
    threadStateData->heapAllocationSize++;
#ifndef CN1_DISABLE_BIBOP
    // Event-driven trigger for LEGACY allocation volume, mirroring
    // cn1BibopMaybeGc's simple self-triggering branch: without it, raising the
    // isHighFrequencyGC re-arm above 1MB would let a legacy-churn workload
    // (transient large arrays) accumulate uncollected garbage for up to the GC
    // thread's 30s idle wait, bounded only by the per-thread pending-table
    // count triggers above. The object is already registered in
    // pendingHeapAllocations, so triggering here is safe; System.gc() is
    // asynchronous (sets forceGc + notifies the GC thread) exactly as in the
    // BiBOP path. nativeAllocationMode save/restore matches cn1BibopMaybeGc:
    // we may already be inside a caller's native-allocation bracket.
    // LEVEL-triggered on volume, LATCHED to one schedule per cycle window
    // (cn1LegacyGcScheduled, cleared at cycle begin). Level-triggering means a
    // crossing suppressed by the native-bracket gate below is retried by the
    // next out-of-bracket legacy allocation on any thread -- a pure edge
    // trigger would lose that crossing permanently (prev already above the
    // threshold forever after) and delay collection up to the GC thread's 30s
    // idle wait. The latch supplies what edge-triggering was buying: no
    // per-allocation System.gc() (lock+notify) spam after the crossing. A
    // crossing that lands while a cycle is already running just sets forceGc
    // for the GC thread's next loop pass (deliberately no !gcCurrentlyRunning
    // suppression -- the latch makes it redundant anyway).
    long long prevLegacyBytes = atomic_fetch_add_explicit(&cn1LegacyBytesSinceGc,
                                                          (long long)size,
                                                          memory_order_relaxed);
    if(prevLegacyBytes + (long long)size > CN1_LEGACY_GC_TRIGGER_BYTES
       && constantPoolObjects != 0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
       // Mirror cn1BibopMaybeGc's gate exactly: without conservative roots a
       // thread inside a native-allocation bracket must not trigger a cycle
       // (its half-built objects aren't rooted). Under conservative roots the
       // native C locals ARE scanned, and gating unconditionally would starve
       // the trigger on workloads whose large allocations all happen inside
       // natives (the same starvation the BiBOP comment documents).
       && !threadStateData->nativeAllocationMode
#endif
       && atomic_load_explicit(&cn1LegacyGcScheduled, memory_order_relaxed) == 0) {
        int expectedLatch = 0;
        if(atomic_compare_exchange_strong_explicit(&cn1LegacyGcScheduled, &expectedLatch, 1,
                                                   memory_order_acq_rel,
                                                   memory_order_relaxed)) {
            JAVA_BOOLEAN wasNam = threadStateData->nativeAllocationMode;
            threadStateData->nativeAllocationMode = JAVA_TRUE;
            java_lang_System_gc__(threadStateData);
            threadStateData->nativeAllocationMode = wasNam;
        }
    }
#ifndef CN1_BIBOP_NO_PACING
    // BACKPRESSURE for the legacy path (issue #5537). The trigger above only
    // SCHEDULES an asynchronous cycle and returns; it never makes the allocating
    // thread wait. The only thing that did was the pending-table check near the top
    // of this function, and that is a COUNT (CN1_MAX_HEAP_SIZE, itself derived from
    // free RAM divided by a 128-byte average object) -- so a thread allocating
    // objects above CN1_BIBOP_MAX_OBJECT could run hundreds of megabytes to
    // gigabytes ahead of the collector before anything stalled it. Every array a
    // program allocates is above that threshold, which is why a game-tree search
    // churning board arrays could take the process past the iOS ceiling with a live
    // set of almost nothing.
    //
    // Evaluated every CN1_PACING_CHECK_INTERVAL_BYTES of process-wide legacy allocation
    // rather than on the 24MB scheduling trigger above. That trigger sizes when to
    // SCHEDULE a cycle, not how close to the ceiling a thread may get, and reusing it
    // fails exactly where this matters: near the ceiling a thread could spend the whole
    // remaining budget while legacy volume was still climbing toward 24MB. The interval
    // only decides HOW OFTEN admission is reconsidered -- what is actually tested is the
    // live remaining budget (see cn1PacingPark) -- so it bounds how much a thread can
    // dirty between two consultations of the truth. Detected on the shared counter using
    // the pre-add value the trigger already computes, so the interval is process-wide
    // rather than per-thread, and costs a shift and a compare on a value in hand.
    if(((unsigned long long)prevLegacyBytes >> CN1_PACING_CHECK_INTERVAL_SHIFT)
            != ((unsigned long long)(prevLegacyBytes + (long long)size)
                    >> CN1_PACING_CHECK_INTERVAL_SHIFT)
       && constantPoolObjects != 0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
       // Same bracket gate as the trigger: without conservative roots a thread inside
       // a native-allocation bracket must not be parked, since its half-built objects
       // are not rooted and the collector could sweep them while it waits.
       && !threadStateData->nativeAllocationMode
#endif
       ) {
        cn1PacingPark(threadStateData, CN1_PACE_LEGACY, (long long)size);
    }
#endif
#endif
    return o;
}

void codenameOneGcFree(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
#ifndef CN1_DISABLE_BIBOP
    // A BiBOP page slot must never be free()'d -- it is reclaimed in place into
    // its page free-list by cn1BibopSweep. This is a defensive guard; no legacy
    // path should reach here with a page slot (they are not in allObjectsInHeap).
    // A MATURED (-4) object's memory is also a page slot -- never free() it either;
    // the legacy sweep flips it back to -3 (below) and the BiBOP sweep reclaims it.
    if(obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED) {
        if(obj->__heapPosition == CN1_BIBOP_ADOPTED) {
            // Hand the slot back to BiBOP: revert to a normal dead BiBOP slot so the next
            // sweep of its (retired) page reclaims it to the page free-list.
            obj->__heapPosition = CN1_BIBOP_HEAP_POS;
        }
        return;
    }
#endif
    {
        void* md = cn1MonitorDataRemove(obj);
        if(md) {
            free(md);
        }
    }
#ifdef CN1_NURSERY
    // A promoted nursery object lives inside an arena block; never free() it -- just
    // drop the block's live count and recycle the whole block once it hits zero.
    if(cn1InNursery(obj)) {
        extern void cn1NurseryObjectFreed(JAVA_OBJECT o);
        cn1NurseryObjectFreed(obj);
        return;
    }
#endif
#ifdef DEBUG_GC_OBJECTS_IN_HEAP
    int* ptr = (int*)obj;
    ptr--;
    totalAllocatedHeap -= *ptr;
    free(ptr);
#else
#ifdef CN1_GC_VERIFY
    // QA: poison + quarantine rather than free, so a dangling reference reads a
    // recognizably dead object instead of whatever the allocator recycles into
    // the address next. Falls through to a real free only if the quarantine
    // could not be allocated.
    if(cn1GcVerifyQuarantineFree(obj)) {
        return;
    }
#endif
    free(obj);
#endif
}

typedef void (*gcMarkFunctionPointer)(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force);

//JAVA_OBJECT* recursionBlocker = 0;
//int recursionBlockerPosition = 0;
// recursionKey is the force-mark "pass epoch". It used to be a constant (1) compared
// against the per-object __codenameOneReferenceCount; that field has been relocated out
// of the header into the force-visited side table below. recursionKey is now bumped once
// per GC cycle (in codenameOneGCMark) so a stale table entry from a previous cycle reads
// as not-visited, and no per-pass clearing is needed.
int recursionKey = 1;

// ---- Force-visited side table (relocated __codenameOneReferenceCount recursion guard) ----
// The force-mark re-scan (gcMarkObject with force=JAVA_TRUE on an already-marked object)
// needs a per-object "already force-visited THIS pass" flag to terminate on cyclic
// already-marked subgraphs. It used to live in __codenameOneReferenceCount. It now lives
// here, keyed by object pointer, storing the recursionKey epoch of the last force visit.
// The force re-scan runs ONLY on the serial GC-mark path (the parallel worker path returns
// before ever touching this), so no locking is required.
struct CN1FVEntry { JAVA_OBJECT key; int epoch; struct CN1FVEntry* next; };
#define CN1_FV_BUCKETS 4096
static struct CN1FVEntry* cn1FVBuckets[CN1_FV_BUCKETS];

static inline unsigned cn1FVHash(JAVA_OBJECT o) {
    uintptr_t p = (uintptr_t)o; p >>= 4;
    return (unsigned)((p ^ (p >> 16)) & (CN1_FV_BUCKETS - 1));
}

// Returns 1 if obj was already force-visited at the current `key` epoch (caller should
// skip re-traversal); otherwise records this visit and returns 0.
static int cn1ForceVisitedTestAndSet(JAVA_OBJECT obj, int key) {
    unsigned h = cn1FVHash(obj);
    struct CN1FVEntry* e = cn1FVBuckets[h];
    while(e) {
        if(e->key == obj) {
            if(e->epoch == key) return 1;
            e->epoch = key; return 0;
        }
        e = e->next;
    }
    e = (struct CN1FVEntry*)malloc(sizeof(struct CN1FVEntry));
    e->key = obj; e->epoch = key; e->next = cn1FVBuckets[h];
    cn1FVBuckets[h] = e;
    return 0;
}

// Iterative mark using an explicit worklist. The previous implementation recursed
// through reference fields, building one C stack frame per Java reference traversed.
// iOS gives secondary pthreads a ~512KB stack, so a chain of a few thousand references
// (linked list, parse tree, deeply nested container) would SIGBUS the GC thread.
// Issue #3136.
//
// gcMarkObject now sets the mark bit and pushes onto a fixed worklist. gcMarkDrain
// pops entries and invokes their per-class mark function, which calls gcMarkObject on
// each reference field -- push, not recurse. If the worklist fills, the offending
// object is still marked (so sweep preserves it) but its field scan is deferred to
// the heap-rescan pass: walk the live-object table, re-invoke mark functions on
// already-marked objects to pick up children that were skipped on overflow. Idempotent
// because already-marked children are no-ops in gcMarkObject.
//
// CN1_GC_MARK_WORKLIST_SIZE is overridable at compile time (e.g. via -D in the Xcode
// build settings or the maven plugin). 65536 entries is ~1MB on 64-bit. Sized so the
// constant pool alone fits comfortably (HelloCodenameOne has ~15K entries, real apps
// can have more). Smaller sizes still work via the heap-rescan slow path, but the
// rescan adds non-trivial cost and the path is harder to test, so the default errs
// on the side of avoiding overflow for any normal app.
// (The #define itself is hoisted to the forward-declaration block far above, next to
// gcMarkWorklistTop, because the grace pass needs it; this #ifndef is what keeps a
// -D override authoritative in both places.)
#ifndef CN1_GC_MARK_WORKLIST_SIZE
#define CN1_GC_MARK_WORKLIST_SIZE 65536
#endif

struct gcMarkWorklistEntry {
    JAVA_OBJECT obj;
    JAVA_BOOLEAN force;
};

static struct gcMarkWorklistEntry gcMarkWorklist[CN1_GC_MARK_WORKLIST_SIZE];
static int gcMarkWorklistTop = 0;
static JAVA_BOOLEAN gcMarkWorklistOverflow = JAVA_FALSE;
// Set whenever gcMarkObject transitions an object from unmarked to marked. Used by
// the overflow-rescan loop to detect a fixed point: if a rescan+drain pass marks
// nothing new, the reachable set is fully closed under "marked" and we're done --
// otherwise we'd spin forever re-pushing the same marked-and-already-scanned
// objects when the marked set is larger than the worklist. Only touched on the
// serial path (gcMarkLocalBuf == 0); the parallel workers never run the rescan, and
// writing it from many workers would be a benign-value-but-still-reported data race.
static JAVA_BOOLEAN gcMarkFoundUnmarkedChildInPass = JAVA_FALSE;

#ifdef CN1_BIBOP_VALIDATE
// Forensic (serial mark): the object gcMarkDrain is currently tracing -- i.e. the
// PARENT whose mark function is marking children. The gcMarkObject child-side
// validation dumps this so a corrupt child tells us WHO referenced it, which
// distinguishes a conservative-scan root-miss (parent live, child wrongly freed)
// from a worklist/slot reuse (the parent itself is stale). Written on the single
// GC/mark thread only, read in the same-thread child-mark below.
volatile JAVA_OBJECT gcMarkCurrentDrainObj = JAVA_NULL;
#endif

// ===================== Parallel mark drain =====================
//
// The transitive drain (popping objects and running their per-class mark functions,
// which push reference fields back onto the worklist) is the dominant cost of a mark
// cycle, and it is embarrassingly parallel: marking is already type-specialized and
// the only shared mutable state is (a) each object's mark bit and (b) the worklist.
//
// We parallelize ONLY the drain, leaving codenameOneGCMark's per-thread park / root
// snapshot / aggressive-allocator handling exactly as it was. The roots are pushed
// onto the worklist serially while the mutator thread is paused (snapshot-at-the-
// beginning, invariant #1), then gcMarkDrainParallel marks the whole reachable set
// before the thread is released -- the workers just do it faster.
//
// Correctness rests on three things:
//  * The mark bit is claimed with an atomic compare-and-swap (gcMarkObject), so for
//    any object exactly one worker wins the unmarked->marked transition and exactly
//    one worker pushes it. No double-push, no double-scan, no torn mark bit.
//  * The shared worklist is guarded by gcMarkWorklistMutex. Workers pop a BATCH under
//    the lock and buffer the children they produce in a thread-local buffer, flushing
//    in batches, so the lock is taken ~once per CN1_GC_MARK_BATCH objects.
//  * Termination = worklist empty AND every worker idle, tracked by gcMarkActiveWorkers
//    under the lock. A worker stays "active" from the moment it pops work until, after
//    flushing everything it produced, it observes the global worklist empty; only then
//    does it go idle. So the count hits zero only when no in-hand or global work
//    remains anywhere -- a worker that produces new work re-wakes the idle ones.
//
// The bounded explicit worklist (invariant #2, no recursion) and the overflow->heap-
// rescan fixed point (invariant #3) are preserved: on overflow the parallel region
// still drains to empty (marked-but-unscanned objects are dropped from the worklist
// but stay marked) and then gcMarkDrainParallel finishes with one serial gcMarkDrain,
// which runs the rescan fixed point exactly as before.
//
// CN1_GC_MARK_THREADS overrides the worker count at compile time (-D...); when it is
// not set the count is min(4, online-cpus - 1) computed at runtime. A count of 1 (the
// historical behavior) takes the serial path with no pool, no atomics and no locks.
#ifndef CN1_GC_MARK_BATCH
#define CN1_GC_MARK_BATCH 64
#endif
#ifndef CN1_GC_MARK_LOCAL_CAP
#define CN1_GC_MARK_LOCAL_CAP 256
#endif

// Per-worker production buffer. While a thread is acting as a parallel mark worker its
// gcMarkLocalBuf points here (on that worker's stack); gcMarkWorklistPush appends to it
// and flushes to the shared worklist in batches. When NULL the thread is on the serial
// path and pushes straight to the shared worklist with no locking (single-threaded by
// construction: root snapshot, the serial drain, the nursery promote drain). Being a
// thread-local pointer it also doubles as the per-thread "am I a parallel worker?" flag
// that gcMarkObject uses to choose the atomic mark-claim path.
struct gcMarkLocalBuffer {
    int count;
    struct gcMarkWorklistEntry entries[CN1_GC_MARK_LOCAL_CAP];
};
static __thread struct gcMarkLocalBuffer* gcMarkLocalBuf = 0;

// Worklist / termination state (guarded by gcMarkWorklistMutex during a parallel drain)
static pthread_mutex_t gcMarkWorklistMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  gcMarkWorklistCond  = PTHREAD_COND_INITIALIZER;
static int gcMarkActiveWorkers = 0;
static JAVA_BOOLEAN gcMarkDone = JAVA_FALSE;
static struct ThreadLocalData* gcMarkThreadState = 0; // 'd' passed through to mark functions

// Pool dispatch / completion handshake (guarded by gcMarkCtlMutex)
static pthread_mutex_t gcMarkCtlMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  gcMarkCtlCond  = PTHREAD_COND_INITIALIZER;
static unsigned long gcMarkGeneration = 0;     // bumped to dispatch a drain
static int gcMarkWorkersFinished = 0;          // helpers that completed the current generation
static int gcMarkPoolSize = 0;                 // number of spawned helper threads (= count - 1)
static int gcMarkThreadCount = 0;              // total markers incl. the GC thread (resolved once)
static JAVA_BOOLEAN gcMarkPoolReady = JAVA_FALSE;

// Append a worker's local production buffer to the shared worklist. On overflow the
// surplus entries are dropped -- they are already MARKED (their mark bit was claimed
// before the push) so dropping only defers their field scan to the serial rescan, which
// is exactly the existing overflow contract.
static void gcMarkFlushLocal(struct gcMarkLocalBuffer* lb) {
    if(lb->count == 0) {
        return;
    }
    pthread_mutex_lock(&gcMarkWorklistMutex);
    int appended = 0;
    for(int i = 0 ; i < lb->count ; i++) {
        if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE) {
            gcMarkWorklistOverflow = JAVA_TRUE;
            // EXCHANGE, so the counter below reads once per CYCLE rather than once per
            // dropped push: what an overflow costs is the belt's O(heap) rescan, and
            // that runs once however many pushes were dropped. Only ever executed on
            // the overflow path, so the atomic RMW is off every normal mark.
            if(!atomic_exchange_explicit(&gcMarkOverflowSeen, JAVA_TRUE, memory_order_release)) {
                atomic_fetch_add_explicit(&cn1GcOverflowCycles, 1, memory_order_relaxed);
            }
            break;
        }
        gcMarkWorklist[gcMarkWorklistTop] = lb->entries[i];
        gcMarkWorklistTop++;
        appended++;
    }
    if(appended > 0) {
        pthread_cond_broadcast(&gcMarkWorklistCond);
    }
    pthread_mutex_unlock(&gcMarkWorklistMutex);
    lb->count = 0;
}

static inline void gcMarkWorklistPush(JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    struct gcMarkLocalBuffer* lb = gcMarkLocalBuf;
    if(lb != 0) {
        // Parallel worker: buffer locally, flush in batches (see gcMarkFlushLocal).
        if(lb->count >= CN1_GC_MARK_LOCAL_CAP) {
            gcMarkFlushLocal(lb);
        }
        lb->entries[lb->count].obj = obj;
        lb->entries[lb->count].force = force;
        lb->count++;
        return;
    }
    // Serial path: identical to the original single-threaded push.
    if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE) {
        gcMarkWorklistOverflow = JAVA_TRUE;
        // See the matching exchange in gcMarkFlushLocal.
        if(!atomic_exchange_explicit(&gcMarkOverflowSeen, JAVA_TRUE, memory_order_release)) {
            atomic_fetch_add_explicit(&cn1GcOverflowCycles, 1, memory_order_relaxed);
        }
        return;
    }
    gcMarkWorklist[gcMarkWorklistTop].obj = obj;
    gcMarkWorklist[gcMarkWorklistTop].force = force;
    gcMarkWorklistTop++;
}

#ifdef CN1_NURSERY
extern void cn1NurseryPromote(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
#endif

#if CN1_TAGGED_ACTIVE
// Object-shaped proxy whose header is Integer's class; see CN1_CLASS_OF in cn1_globals.h.
// Lets a tagged Integer resolve to Integer without dereferencing the tagged pointer.
struct JavaObjectPrototype cn1TaggedProxy = { .__codenameOneParentClsReference = &class__java_lang_Integer };
#endif

#if !defined(CN1_DISABLE_BIBOP) && !defined(CN1_BIBOP_NO_FASTSWEEP)
// Stamp the BiBOP page of a just-marked-live object with the current epoch so the sweep
// knows the page had a live slot THIS cycle (-> must full-walk, never O(1) all-dead).
// Relaxed + idempotent: every parallel marker that newly marks a slot on this page stores
// the same value; the GC-thread sweep reads it after the mark-pool join barrier.
static inline void cn1BibopStampMarked(JAVA_OBJECT obj, int markVal, int graceOnly) {
    // Stamp for a normal BiBOP slot AND a MATURED (-4) slot: a live matured object's
    // memory is still in this page, so its page must not be O(1) all-dead reclaimed.
    if(obj->__heapPosition == CN1_BIBOP_HEAP_POS || obj->__heapPosition == CN1_BIBOP_ADOPTED) {
        CN1BibopPage* pg = (CN1BibopPage*)(((uintptr_t)obj) & ~((uintptr_t)CN1_BIBOP_PAGE_SIZE - 1));
        atomic_store_explicit(&pg->gcLastMarkedEpoch, markVal, memory_order_relaxed);
        // The page address is already in hand, which is the whole cost of this
        // accounting -- see gcGraceMarked in cn1_globals.h for what it is for.
        if(graceOnly) {
            atomic_fetch_add_explicit(&pg->gcGraceMarked, 1, memory_order_relaxed);
        }
    }
}
#define CN1_BIBOP_STAMP_MARKED(o, m) cn1BibopStampMarked((o), (m), 0)
// A mark taken during a grace pass whose previous value was -1: the object survives
// on the grace rule alone, since the root drain ran to completion before the pass and
// did not reach it.
#define CN1_BIBOP_STAMP_MARKED_GRACE(o, m, snap) \
    cn1BibopStampMarked((o), (m), (cn1GcInGracePass != 0 && (snap) == -1))
#else
#define CN1_BIBOP_STAMP_MARKED(o, m) do {} while(0)
#define CN1_BIBOP_STAMP_MARKED_GRACE(o, m, snap) do {} while(0)
#endif

void gcMarkObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL || CN1_IS_TAGGED(obj)) {
        return;
    }
#ifdef CN1_GC_VERIFY
    // QA verifier mode: cn1GcVerifyHeap drives the SAME generated mark functions
    // the collector uses, so every reference field of every surviving object
    // arrives here. Classify it instead of marking it. This must precede the
    // conservative-resolve guard below -- that guard SKIPS exactly the dangling
    // pointers the verifier exists to catch.
    if(cn1GcVerifyActive) {
        // Capture the call site HERE: this frame's return address is inside the
        // generated mark function (or gcMarkArrayObject for an element), which
        // is what addr2line/atos needs to name the field. cn1GcVerifyChild
        // cannot obtain it -- from there this frame is in the way.
        cn1GcVerifyChild(obj, __builtin_return_address(0));
        return;
    }
#endif
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // TOTAL pointer validation BEFORE ANY dereference. The drain follows the
    // reference fields of conservatively-kept objects, and a conservatively-kept
    // DEAD object's fields can dangle into memory whose child was freed -- and
    // UNMAPPED -- in an EARLIER cycle. Reading even the mark word of such a
    // pointer faults (the recurred arm64 suite SIGSEGV at the acquire-load below;
    // the earlier clazz-registry guard fired too late because it already had to
    // read the header). Accept a pointer only if the authoritative resolver maps
    // it to itself (a CURRENT BiBOP slot or legacy extent -- never dereferences
    // the suspect) or it is a REGISTERED IMMORTAL (removed from the heap table on
    // purpose: interned strings, static-final removal values, VM cache roots --
    // unresolvable but must still be traced, since a static-final Throwable/String
    // can hold normal heap children).
    //
    // Skipping everything else is SOUND under snapshot-at-the-beginning:
    //  - garbage/dangling pointers have no live fields to trace;
    //  - a class static (java.lang.Class object) needs no marking (the old flow
    //    also returned before pushing it);
    //  - an object allocated AFTER the cycle's extent snapshot resolves as a miss,
    //    but it is FRESH (mark == -1 -> the sweep's grace rule keeps it) and any
    //    snapshot-live object reachable only through it is covered by the SATB
    //    deletion barrier (its old path was cut during the mark -> logged);
    //    BiBOP-fresh objects resolve anyway via the live-bump revalidation.
    // Cost: one page/extent binary search per mark call, on the GC thread /
    // workers only (the mutator never calls gcMarkObject) -- GC-side passes
    // measured ~0% of wall time on this workload class.
    // TRUSTED CALLERS BYPASS THIS (issue 5425). The guard's job is to reject
    // CONSERVATIVELY DERIVED pointers -- arbitrary machine words off a native stack
    // that may not be objects at all. A caller that walked the object out of
    // allObjectsInHeap or a BiBOP page slot has an authoritative reference and needs
    // no proof; making it pass anyway means every object allocated after the cycle's
    // extent snapshot is silently dropped, because the snapshot cannot contain it.
    // That is exactly how a fresh object came to be grace-promoted by the sweep with
    // its subtree never traced. Do NOT set this flag around anything that marks from
    // a stack scan or a register file.
    if(!cn1GcTrustedRoots
       && cn1ConservativeResolve((void*)obj) != obj && !cn1GcImmortalObjContains(obj)) {
        return;
    }
#endif
    // ACQUIRE-load the mark word (the object's publication point) BEFORE reading any
    // other header field. cn1BibopInitSlot writes parentClsReference/heapPosition and
    // THEN release-stores the mark word LAST, so the mark word is the single
    // happens-before edge that publishes a freshly-allocated object. The parallel
    // marker used a RELAXED load here: on arm64's weak memory model that let a worker
    // observe the object (mark word) WITHOUT observing the preceding parentClsReference
    // store, so it dereferenced a stale/garbage parentClsReference->markFunction and
    // crashed at a wild PC (random SIGSEGV in the theme phase, x86 masked it because
    // every x86 load is already acquire). The acquire here pairs with that release and
    // orders every parentClsReference read below -- the guard, the CAS-success deref,
    // and (transitively, through the worklist mutex) the drain worker's deref.
    int markSnapshot = __atomic_load_n(&obj->__codenameOneGcMark, __ATOMIC_ACQUIRE);
#if !defined(CN1_DISABLE_BIBOP)
    // A FREED BiBOP slot sits on its page free-list, which stores the intrusive
    // next pointer in the slot's first word -- i.e. OVER __codenameOneParentClsReference
    // (offset 0). Such a slot is NOT a live object: its parentCls is a garbage
    // free-list pointer and its class/mark functions are meaningless. The
    // conservative native-stack scan legitimately hands us interior pointers to
    // whatever a stack word happens to hold, including a free slot (this is how it
    // manifested: an x86 stack word pointed at a freed slot -- arm64's differing
    // layout, and ASan's redzone layout, simply never produced that word, which is
    // why the crash looked x64-only and vanished under ASan). Marking it would
    // stamp gcMark=current over FREE_MARK, push it, and the drain would then call
    // through its clobbered parentCls -> jump to a garbage address. Reject it here:
    // a free slot has no live fields to trace. FREE_MARK is never a live mark
    // (live == currentGcMarkValue or -1 grace), so this can't skip a real object.
    if(markSnapshot == CN1_BIBOP_FREE_MARK) {
        return;
    }
#endif
    // SINGLE load of the class pointer, reused for every deref below. The header of a
    // conservatively-reached object can be freed/reused WHILE this function runs; loading
    // once and validating that one value removes the read-validate-reread window (the
    // original crash note: "obj readable at acquire-load but header freed/reused mid-mark").
    struct clazz* __cls = obj->__codenameOneParentClsReference;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // Conservative-scan safety guard. The native-stack scan can legitimately false-positive-
    // mark a DEAD object (a stale native-stack word happens to resolve to it -- unavoidable
    // with conservative roots). Precisely draining that dead object then FOLLOWS its now-
    // dangling reference fields; e.g. a freed StringBuilder value[] whose first word (offset
    // 0, over __codenameOneParentClsReference) now holds a BiBOP free-list next pointer or
    // any other reused data. Left unchecked, gcMarkObject dereferences that garbage
    // parentCls -> markFunction: a wild JUMP when the value is mapped, or a wild LOAD fault
    // when it is not (both observed on the Linux arm64 suite).
    //
    // The previous guard accepted any aligned value within 512MB of the code as a
    // "plausible clazz" -- UNSOUND on a non-PIE Linux binary loaded low, where the whole
    // malloc heap lives inside that window: a freed object whose offset-0 word was a heap
    // pointer sailed through, and when that heap page had been MADV_FREE'd/unmapped the
    // markFunction LOAD itself faulted (recurred arm64 SIGSEGV at the line below).
    //
    // Now the test is EXACT and deref-free: EVERY allocation entry point (the inline
    // bump included) registers its class before the object publishes, so a genuine
    // object's class is ALWAYS in the registry -- including objects later made
    // immortal and removed from the heap table. An unregistered class pointer can
    // therefore only mean the header was clobbered/reused out from under us (a
    // conservatively-kept slot recycled mid-mark): skip WITHOUT dereferencing.
    // (An earlier version "adopted" unknown class values here after re-resolving the
    // slot -- but resolve() validates the SLOT, not the header word, and a reused
    // header fed garbage into the registry: the register write faulted on arm64.)
    if(__cls != 0 && __cls != (&class__java_lang_Class)
       && !cn1ClazzRegistryContains((uintptr_t)__cls)) {
        return;
    }
#endif
    if(__cls == 0 || __cls == (&class__java_lang_Class)) {
        return;
    }
#ifdef CN1_NURSERY
    // During THIS thread's minor collection we reuse the per-class mark functions to
    // walk the object graph, but PROMOTE nursery objects instead of marking (and stop
    // at heap objects -- the write barrier guarantees they don't point into the
    // nursery). The flag is per-thread so the concurrent GC thread is unaffected.
    if(threadStateData->nurseryPromoting) {
        if(cn1InNursery(obj) && obj->__heapPosition == -1) {
            cn1NurseryPromote(threadStateData, obj);
        }
        return;
    }
#endif

    int markVal = currentGcMarkValue;

    // Parallel worker path: claim the object's mark bit with an atomic CAS so exactly
    // one worker transitions it unmarked->marked and pushes it (no double-scan, no torn
    // mark bit). 'force' is never set on the parallel path -- the per-thread roots are
    // pushed with force==FALSE and mark functions propagate that -- so the force/
    // recursionKey re-scan (which writes __codenameOneReferenceCount and is rare) stays
    // entirely on the serial path below, keeping the parallel region race-free.
    if(gcMarkLocalBuf != 0) {
        int old = markSnapshot; // acquire-loaded above; pairs with the alloc release
        if(old == markVal) {
            return; // already marked this cycle
        }
        if(__sync_bool_compare_and_swap(&obj->__codenameOneGcMark, old, markVal)) {
            CN1_BIBOP_STAMP_MARKED_GRACE(obj, markVal, old);
            if(__cls->markFunction != 0) {
                gcMarkWorklistPush(obj, force);
            }
        }
        // else: another worker won the claim and is responsible for pushing it.
        return;
    }

    // Serial path: byte-for-byte the original behavior (single writer, plain store).
    // if this is a Class object or already marked this should be ignored
    if(obj->__codenameOneGcMark == markVal) {
        if(force) {
            if(cn1ForceVisitedTestAndSet(obj, recursionKey)) {
                return;
            }
            if(__cls->markFunction != 0) {
                gcMarkWorklistPush(obj, force);
            }
        }
        return;
    }
#ifdef CN1_BIBOP_VALIDATE
    // Forensic (child side): the intermittent arm64 suite crash faults EXACTLY at the
    // stamp below -- gcMarkObject marking a child (e.g. Component.BGPainter.this$0)
    // whose header is readable at the acquire-load (above) but reclaimed by the time
    // we write its mark. Detect a corrupt/reclaimed child BEFORE that write and dump
    // both the child and the parent that referenced it (gcMarkCurrentDrainObj), so a
    // reproduction says root-miss (parent live, child wrongly freed) vs slot-reuse.
    // A live child carries heapPosition CN1_BIBOP_HEAP_POS or >= -1, and its clazz's
    // markFunction lies in the app text (never a libc/heap address); anything else is
    // a freed/reused slot or a dangling reference.
    {
        gcMarkFunctionPointer __cfp = obj->__codenameOneParentClsReference
            ? obj->__codenameOneParentClsReference->markFunction : (gcMarkFunctionPointer)0;
        uintptr_t __anchor = (uintptr_t)(void*)&gcMarkObject;
        uintptr_t __fpv = (uintptr_t)(void*)__cfp;
        int __fpBad = (__cfp != 0) &&
            ((__fpv > __anchor ? __fpv - __anchor : __anchor - __fpv) > (256ULL << 20));
        int __hp = obj->__heapPosition;
        // CN1_BIBOP_ADOPTED (-4) is a live MATURED slot (owned by the legacy sweep), not
        // corruption -- accept it exactly like a normal BiBOP slot.
        if((__hp != CN1_BIBOP_HEAP_POS && __hp != CN1_BIBOP_ADOPTED && __hp < -1) || __fpBad) {
            JAVA_OBJECT __p = gcMarkCurrentDrainObj;
            // Name the culprit: the drain parent is validated live below, so its class
            // name is safe to read and identifies WHICH object holds the lost reference.
            // markCallSite is the return address into the parent's generated mark
            // function -> addr2line / the gdb bt maps it to the exact field being marked.
            const char* __pcls = "?";
            if(__p != JAVA_NULL
               && (__p->__heapPosition == CN1_BIBOP_HEAP_POS || __p->__heapPosition == CN1_BIBOP_ADOPTED)
               && __p->__codenameOneParentClsReference != 0
               && __p->__codenameOneParentClsReference->clsName != 0) {
                __pcls = __p->__codenameOneParentClsReference->clsName;
            }
            void* __callSite = __builtin_return_address(0);
            fprintf(stderr, "CN1BIBOP MARKOBJ CORRUPT CHILD: parentClass=%s markCallSite=%p :: "
                    "child=%p parentCls=%p childMarkFn=%p "
                    "childHeapPos=%d childGcMark=%d curMark=%d FREE_MARK=%d :: drainParent=%p "
                    "parentCls=%p parentMarkFn=%p parentHeapPos=%d parentGcMark=%d\n",
                    __pcls, __callSite,
                    (void*)obj, (void*)obj->__codenameOneParentClsReference, (void*)__cfp,
                    __hp, obj->__codenameOneGcMark, currentGcMarkValue, CN1_BIBOP_FREE_MARK,
                    (void*)__p,
                    __p ? (void*)__p->__codenameOneParentClsReference : (void*)0,
                    (__p && __p->__codenameOneParentClsReference)
                        ? (void*)__p->__codenameOneParentClsReference->markFunction : (void*)0,
                    __p ? __p->__heapPosition : -999,
                    __p ? __p->__codenameOneGcMark : -999);
            fflush(stderr);
            abort();
        }
    }
#endif
#ifdef CN1_GC_VERIFY
    // RESURRECTION RECORD. markSnapshot <= markVal-2 means this object had aged
    // past the sweep's keep threshold (it frees on m < V-1) and is being marked
    // live again -- overwhelmingly by the conservative native-stack scan, which
    // marks whatever a returned frame's leftover word points at. That is only a
    // correctness problem if such an object still references memory the
    // collector already reclaimed, so record it and check its fields before the
    // sweep (cn1GcResurrectAudit).
    if(markSnapshot >= 1 && markSnapshot <= markVal - 2) {
        extern void cn1GcNoteResurrected(JAVA_OBJECT o, const char* phase);
        extern const char* cn1GcMarkPhase;
        cn1GcNoteResurrected(obj, cn1GcMarkPhase);
    }
    // CN1_GC_TRACE_MARK=<class substring>: name what is keeping a given class
    // reachable, by reporting the drain parent that reached it.
    {
        static const char* __tm = (const char*)-1;
        if(__tm == (const char*)-1) __tm = getenv("CN1_GC_TRACE_MARK");
        if(__tm != 0 && markSnapshot > 0 && __cls->clsName != 0 && strstr(__cls->clsName, __tm) != 0) {
            static int __tmCount = 0;
            static int __tmEpoch = -1;
            if(__tmEpoch != markVal) { __tmEpoch = markVal; __tmCount = 0; }
            if(__tmCount < 4) {
                __tmCount++;
#ifdef CN1_BIBOP_VALIDATE
                JAVA_OBJECT __p = gcMarkCurrentDrainObj;
                const char* __pn = (__p != JAVA_NULL && __p->__codenameOneParentClsReference != 0
                                    && __p->__codenameOneParentClsReference->clsName != 0)
                        ? __p->__codenameOneParentClsReference->clsName : "(root/none)";
#else
                const char* __pn = "(build with -DCN1_BIBOP_VALIDATE for the drain parent)";
#endif
                extern const char* cn1GcMarkPhase;
                fprintf(stderr, "[GC-TRACE-MARK] epoch=%d phase=%s marking %s (was %d) drainParent=%s\n",
                        markVal, cn1GcMarkPhase, __cls->clsName, markSnapshot, __pn);
                fflush(stderr);
            }
        }
    }
#endif
    obj->__codenameOneGcMark = markVal;
    CN1_BIBOP_STAMP_MARKED_GRACE(obj, markVal, markSnapshot);
    gcMarkFoundUnmarkedChildInPass = JAVA_TRUE;
    gcMarkNewObjectCount++;   // SATB fixpoint detection (mark-thread only)
#ifdef CN1_BIBOP_VALIDATE
    // Belt diagnostic: name what the main drain systematically MISSED. When set, every
    // object the belt newly marks is a reachable-but-unmarked object; log its class and
    // its drain parent's class (throttled) to identify the drain-incompleteness pattern.
    if(gcBeltDiagActive && gcBeltDiagCount < 60) {
        JAVA_OBJECT __p = gcMarkCurrentDrainObj;
        const char* __cc = (obj->__codenameOneParentClsReference && obj->__codenameOneParentClsReference->clsName)
            ? obj->__codenameOneParentClsReference->clsName : "?";
        const char* __pc = (__p && __p->__heapPosition == CN1_BIBOP_HEAP_POS
            && __p->__codenameOneParentClsReference && __p->__codenameOneParentClsReference->clsName)
            ? __p->__codenameOneParentClsReference->clsName : "?";
        fprintf(stderr, "CN1BIBOP BELT RECOVERED: child=%s <- drainParent=%s\n", __cc, __pc);
        fflush(stderr);
        gcBeltDiagCount++;
    }
#endif
    gcMarkFunctionPointer __markFn = __cls->markFunction;
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
    // Poor-man's generational adoption: a reachable, non-leaf (markFunction != 0) BiBOP
    // object graduates into the legacy mark/sweep, which traces it unconditionally =
    // complete (the split-reachability bug only affects non-leaf BiBOP objects whose
    // subtree the overflow-gated BiBOP rescan can drop). Leaf objects have no subtree, so
    // they stay in the fast BiBOP path. TENURE waits for one survival (markSnapshot > 0)
    // but cascades (gcCurrentlyMaturing) so a maturing subtree matures WHOLE, never half a
    // tree. Stamp already fired above (heapPosition still -3), so ordering is fine.
    if(__markFn != 0 && obj->__heapPosition == CN1_BIBOP_HEAP_POS
#if CN1_ADOPT_POLICY == 1
       && (markSnapshot > 0 || gcCurrentlyMaturing)
#endif
       ) {
        cn1MatureObject(obj);
    }
#endif
    if(__markFn != 0) {
        gcMarkWorklistPush(obj, force);
    }
}

#ifdef CN1_NURSERY
// ===================== Thread-local young generation (nursery) =====================
char* cn1NurseryArenaStart = 0;
char* cn1NurseryArenaEnd = 0;
static int cn1NurseryBlockCount = 0;
// young: the block is in some thread's young set (being bump-allocated). A young block
// is reclaimed ONLY by that thread's minor collection. cn1NurseryObjectFreed (sweep
// thread) must never push a young block to the free stack, or it races the minor
// collection's release and double-pushes -> free-stack overflow -> SIGABRT.
typedef struct { int liveCount; JAVA_BOOLEAN tenured; JAVA_BOOLEAN young; } CN1NurseryBlockMeta;
static CN1NurseryBlockMeta* cn1NurseryBlocks = 0;
static int* cn1NurseryFreeStack = 0;
static int cn1NurseryFreeTop = 0;
static pthread_mutex_t cn1NurseryMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t cn1NurseryOnce = PTHREAD_ONCE_INIT;

static void cn1NurseryDoInit() {
    cn1NurseryArenaStart = (char*)malloc(CN1_NURSERY_ARENA_SIZE);
    cn1NurseryArenaEnd = cn1NurseryArenaStart + CN1_NURSERY_ARENA_SIZE;
    cn1NurseryBlockCount = CN1_NURSERY_ARENA_SIZE / CN1_NURSERY_BLOCK_SIZE;
    cn1NurseryBlocks = (CN1NurseryBlockMeta*)calloc(cn1NurseryBlockCount, sizeof(CN1NurseryBlockMeta));
    cn1NurseryFreeStack = (int*)malloc(sizeof(int) * cn1NurseryBlockCount);
    for(int i = 0 ; i < cn1NurseryBlockCount ; i++) {
        cn1NurseryFreeStack[i] = cn1NurseryBlockCount - 1 - i;
    }
    cn1NurseryFreeTop = cn1NurseryBlockCount;
}

static inline int cn1NurseryBlockIndex(void* p) {
    return (int)(((char*)p - cn1NurseryArenaStart) / CN1_NURSERY_BLOCK_SIZE);
}

static int cn1NurseryGrabBlock() {
    int idx = -1;
    pthread_mutex_lock(&cn1NurseryMutex);
    if(cn1NurseryFreeTop > 0) {
        idx = cn1NurseryFreeStack[--cn1NurseryFreeTop];
        // Reset under the mutex so the sweep thread can't observe a half-initialized
        // block (it reads liveCount/tenured/young in cn1NurseryObjectFreed).
        cn1NurseryBlocks[idx].liveCount = 0;
        cn1NurseryBlocks[idx].tenured = JAVA_FALSE;
        cn1NurseryBlocks[idx].young = JAVA_TRUE;
    }
    pthread_mutex_unlock(&cn1NurseryMutex);
    return idx;
}

// Called by the global sweep when it frees a promoted (tenured-block) object. The
// object stays in place; we just drop the block's live count and recycle the whole
// block once every survivor in it has died -- but ONLY if the block has been retired
// from its thread's young set. A still-young block is reclaimed by the minor
// collection instead; freeing it here too would double-push and overflow the stack.
void cn1NurseryObjectFreed(JAVA_OBJECT o) {
    int idx = cn1NurseryBlockIndex(o);
    pthread_mutex_lock(&cn1NurseryMutex);
    int lc = --cn1NurseryBlocks[idx].liveCount;
    if(lc <= 0 && cn1NurseryBlocks[idx].tenured && !cn1NurseryBlocks[idx].young) {
        cn1NurseryBlocks[idx].tenured = JAVA_FALSE;
        cn1NurseryFreeStack[cn1NurseryFreeTop++] = idx;
    }
    pthread_mutex_unlock(&cn1NurseryMutex);
}

static void cn1PromotePush(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    if(threadStateData->nurseryPromoteTop >= threadStateData->nurseryPromoteCap) {
        threadStateData->nurseryPromoteCap = threadStateData->nurseryPromoteCap ? threadStateData->nurseryPromoteCap * 2 : 8192;
        threadStateData->nurseryPromoteWorklist = (JAVA_OBJECT*)realloc(threadStateData->nurseryPromoteWorklist, sizeof(JAVA_OBJECT) * threadStateData->nurseryPromoteCap);
    }
    threadStateData->nurseryPromoteWorklist[threadStateData->nurseryPromoteTop++] = o;
}

// Add an object to this thread's pending-allocation buffer, exactly like a normal
// heap allocation. The mark phase migrates pending -> allObjectsInHeap while the
// thread is paused, so registration never races the concurrent sweep/mark.
static void cn1AddPending(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    if(threadStateData->heapAllocationSize >= threadStateData->threadHeapTotalSize) {
        // Same use-after-free as codenameOneGcMalloc: lock unconditionally (lightweight
        // threads included) so the GC's cn1GcBuildRootSnapshots never reads this array
        // mid-free. Held only across the grow; no park/signal-stop inside -> no deadlock.
        lockThreadHeapMutex();
        void** tmp = malloc(threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memset(tmp, 0, threadStateData->threadHeapTotalSize * 2 * sizeof(void *));
        memcpy(tmp, threadStateData->pendingHeapAllocations, threadStateData->threadHeapTotalSize * sizeof(void *));
        threadStateData->threadHeapTotalSize *= 2;
        free(threadStateData->pendingHeapAllocations);
        threadStateData->pendingHeapAllocations = tmp;
        unlockThreadHeapMutex();
    }
    threadStateData->pendingHeapAllocations[threadStateData->heapAllocationSize++] = o;
}

// Promote one nursery object IN PLACE (address unchanged): tenure its block and hand
// it to the normal pending-allocation path so the next paused mark registers it in
// allObjectsInHeap. __heapPosition: -1 = un-promoted nursery, -2 = promoted/pending,
// >=0 = migrated into the global table.
void cn1NurseryPromote(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    pthread_mutex_lock(&cn1NurseryMutex);
    int idx = cn1NurseryBlockIndex(o);
    cn1NurseryBlocks[idx].tenured = JAVA_TRUE;
    cn1NurseryBlocks[idx].liveCount++;
    pthread_mutex_unlock(&cn1NurseryMutex);
    o->__heapPosition = -2;
    threadStateData->nurseryPromotedSinceMinor++;
    cn1AddPending(threadStateData, o);
    cn1PromotePush(threadStateData, o);
}

static void cn1PromoteDrain(CODENAME_ONE_THREAD_STATE) {
    while(threadStateData->nurseryPromoteTop > 0) {
        JAVA_OBJECT o = threadStateData->nurseryPromoteWorklist[--threadStateData->nurseryPromoteTop];
        gcMarkFunctionPointer fp = o->__codenameOneParentClsReference->markFunction;
        if(fp != 0) {
            fp(threadStateData, o, JAVA_FALSE);
        }
    }
}

void cn1NurseryMinorCollect(CODENAME_ONE_THREAD_STATE) {
    threadStateData->nurseryPromoting = JAVA_TRUE;
    threadStateData->nurseryPromoteTop = 0;
    int top = threadStateData->threadObjectStackOffset;
    struct elementStruct* stack = threadStateData->threadObjectStack;
    for(int i = 0 ; i < top ; i++) {
        if(stack[i].type == CN1_TYPE_OBJECT) {
            JAVA_OBJECT o = stack[i].data.o;
            if(o != JAVA_NULL && cn1InNursery(o) && o->__heapPosition == -1) {
                cn1NurseryPromote(threadStateData, o);
            }
        }
    }
    JAVA_OBJECT ct = threadStateData->currentThreadObject;
    if(ct != JAVA_NULL && cn1InNursery(ct) && ct->__heapPosition == -1) cn1NurseryPromote(threadStateData, ct);
    JAVA_OBJECT ex = threadStateData->exception;
    if(ex != JAVA_NULL && cn1InNursery(ex) && ex->__heapPosition == -1) cn1NurseryPromote(threadStateData, ex);
    // Static fields are also roots. If the write barrier holds (statics never point
    // into the nursery) this is cheap -- it just walks heap objects, which the
    // promotion hook ignores -- but it also catches any store path that bypassed the
    // barrier, so a still-live nursery object can never be left unpromoted (and then
    // wrongly reclaimed). markStatics calls gcMarkObject, which promotes in this mode.
    extern void markStatics(CODENAME_ONE_THREAD_STATE);
    markStatics(threadStateData);
    cn1PromoteDrain(threadStateData);
    threadStateData->nurseryPromoting = JAVA_FALSE;
    // Retire every young block from the young set (under the mutex, so the sweep thread
    // sees a consistent young flag). A block with no live promoted survivors (liveCount
    // <= 0: never tenured, or every survivor it held already died) is reclaimed now;
    // one that still has survivors stays tenured and is freed later by
    // cn1NurseryObjectFreed when its last survivor dies. Clearing `young` first hands
    // that responsibility cleanly to the sweep with no double-push window.
    pthread_mutex_lock(&cn1NurseryMutex);
    for(int i = 0 ; i < threadStateData->nurseryYoungCount ; i++) {
        int idx = threadStateData->nurseryYoungBlocks[i];
        cn1NurseryBlocks[idx].young = JAVA_FALSE;
#ifndef CN1_NURSERY_NO_RECLAIM
        if(cn1NurseryBlocks[idx].liveCount <= 0) {
            cn1NurseryBlocks[idx].tenured = JAVA_FALSE;
            cn1NurseryFreeStack[cn1NurseryFreeTop++] = idx;
        }
#endif
    }
    pthread_mutex_unlock(&cn1NurseryMutex);
    threadStateData->nurseryYoungCount = 0;
    threadStateData->nurseryCurrentBlock = -1;
    threadStateData->nurseryBump = 0;
    threadStateData->nurseryEnd = 0;
    threadStateData->nurseryBytesSinceMinor = 0;
    // Adaptive bypass decision. If most of what we allocated since the last minor
    // survived, the nursery (bump + write barrier + promote-to-pending) was strictly
    // more work than allocating into the heap directly would have been. Bypass it for
    // a while, then re-probe. A churny phase reclaims whole blocks here and keeps the
    // nursery on; an escaping phase trips this and stops paying the overhead.
    int allocated = threadStateData->nurseryAllocSinceMinor;
    int promoted = threadStateData->nurseryPromotedSinceMinor;
    if(allocated >= CN1_NURSERY_BYPASS_MIN_SAMPLE &&
       promoted * 100 >= allocated * CN1_NURSERY_BYPASS_SURVIVAL_PCT) {
        threadStateData->nurseryBypass = JAVA_TRUE;
        threadStateData->nurseryBypassCountdown = CN1_NURSERY_BYPASS_ALLOCS;
    }
#ifdef CN1_NURSERY_DEBUG
    fprintf(stderr, "[NURSERY] minor: alloc=%d promoted=%d survival=%d%% reprobe=%d -> bypass=%d\n",
            allocated, promoted, allocated ? (promoted*100/allocated) : 0,
            threadStateData->nurseryReprobing, threadStateData->nurseryBypass);
#endif
    threadStateData->nurseryReprobing = JAVA_FALSE;
    threadStateData->nurseryAllocSinceMinor = 0;
    threadStateData->nurseryPromotedSinceMinor = 0;
}

JAVA_OBJECT cn1NurseryAlloc(CODENAME_ONE_THREAD_STATE, int size, struct clazz* parent) {
    pthread_once(&cn1NurseryOnce, cn1NurseryDoInit);
    // GC safepoint. The concurrent GC pauses lightweight threads (threadBlockedByGC +
    // wait on threadActive) before scanning their stacks/nursery objects. The normal
    // allocation path yields here too (~line 1141); the nursery fast path must as
    // well, otherwise the GC either scans this thread's nursery while a minor
    // collection mutates it (corruption) or waits forever. A minor collection itself
    // keeps threadActive true throughout, so the GC never scans mid-collection.
    if(threadStateData->threadBlockedByGC && !threadStateData->nativeAllocationMode) {
        CN1_GC_PARK_CAPTURE(threadStateData);   // PHASE 3b: native-stack capture at park
        threadStateData->threadActive = JAVA_FALSE;
        while(threadStateData->threadBlockedByGC) {
            usleep(1000);
        }
        threadStateData->threadActive = JAVA_TRUE;
    }
    // Adaptive bypass: a recent minor collection saw high survival, so skip the
    // nursery and let the caller allocate into the global heap. Decrement toward a
    // re-probe; when it elapses, allocate in the nursery again to re-measure survival.
    if(threadStateData->nurseryBypass) {
        if(--threadStateData->nurseryBypassCountdown > 0) {
            return JAVA_NULL;
        }
        threadStateData->nurseryBypass = JAVA_FALSE;
        threadStateData->nurseryReprobing = JAVA_TRUE;
    }
    if(threadStateData->nurseryYoungBlocks == 0) {
        threadStateData->nurseryYoungCapacity = 256;
        threadStateData->nurseryYoungBlocks = (int*)malloc(sizeof(int) * threadStateData->nurseryYoungCapacity);
        threadStateData->nurseryYoungCount = 0;
        threadStateData->nurseryCurrentBlock = -1;
    }
    int asize = (size + 15) & ~15;
    if(threadStateData->nurseryBump == 0 || threadStateData->nurseryBump + asize > threadStateData->nurseryEnd) {
        long minorTrigger = threadStateData->nurseryReprobing ? CN1_NURSERY_REPROBE_BYTES : CN1_NURSERY_MINOR_TRIGGER;
        if(threadStateData->nurseryBytesSinceMinor >= minorTrigger) {
            cn1NurseryMinorCollect(threadStateData);
        }
        int idx = cn1NurseryGrabBlock();
        if(idx < 0) {
            return JAVA_NULL; // arena exhausted -> use the global heap
        }
        if(threadStateData->nurseryYoungCount >= threadStateData->nurseryYoungCapacity) {
            threadStateData->nurseryYoungCapacity *= 2;
            threadStateData->nurseryYoungBlocks = (int*)realloc(threadStateData->nurseryYoungBlocks, sizeof(int) * threadStateData->nurseryYoungCapacity);
        }
        threadStateData->nurseryYoungBlocks[threadStateData->nurseryYoungCount++] = idx;
        threadStateData->nurseryCurrentBlock = idx;
        threadStateData->nurseryBump = cn1NurseryArenaStart + (long)idx * CN1_NURSERY_BLOCK_SIZE;
        threadStateData->nurseryEnd = threadStateData->nurseryBump + CN1_NURSERY_BLOCK_SIZE;
    }
    JAVA_OBJECT o = (JAVA_OBJECT)threadStateData->nurseryBump;
    threadStateData->nurseryBump += asize;
    threadStateData->nurseryBytesSinceMinor += asize;
    threadStateData->nurseryAllocSinceMinor++;
    memset(o, 0, size);
    o->__codenameOneParentClsReference = parent;
    o->__codenameOneGcMark = -1;
    o->__heapPosition = -1;
    return o;
}

// Write barrier: an object reference is being stored into a non-nursery location, so
// the value escapes the thread-local nursery and must be promoted to the global heap.
void cn1NurseryWriteBarrier(JAVA_OBJECT target, JAVA_OBJECT value) {
    if(value != JAVA_NULL && cn1InNursery(value) && value->__heapPosition == -1 && !cn1InNursery(target)) {
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        // Re-entrancy guard: promotion walks markFunctions which can store refs and
        // re-enter the barrier; the outermost call owns the worklist drain.
        if(threadStateData->nurseryPromoting) {
            cn1NurseryPromote(threadStateData, value);
            return;
        }
        threadStateData->nurseryPromoting = JAVA_TRUE;
        threadStateData->nurseryPromoteTop = 0;
        cn1NurseryPromote(threadStateData, value);
        cn1PromoteDrain(threadStateData);
        threadStateData->nurseryPromoting = JAVA_FALSE;
    }
}
#endif

void gcMarkArrayObject(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_BOOLEAN force) {
    if(obj == JAVA_NULL) {
        return;
    }
#ifdef CN1_NURSERY
    // The minor collection reuses array mark functions to walk arrays for promotion;
    // there the array's mark bit is NOT claimed through gcMarkObject, so set it as the
    // pre-existing code did.
    if(threadStateData->nurseryPromoting) {
        obj->__codenameOneGcMark = currentGcMarkValue;
    }
#endif
    // In the concurrent GC drain (serial or parallel) this array's mark bit was already
    // claimed atomically by the gcMarkObject that enqueued it. We must NOT rewrite it
    // here: a redundant non-atomic store would race with other workers reading the bit
    // (and with the winning worker's CAS) under ThreadSanitizer.
    JAVA_ARRAY arr = (JAVA_ARRAY)obj;
    if(arr->length > 0) {
        JAVA_ARRAY_OBJECT* data = (JAVA_ARRAY_OBJECT*)arr->data;
        for(int iter = 0 ; iter < arr->length ; iter++) {
            if(data[iter] != JAVA_NULL) {
                gcMarkObject(threadStateData, data[iter], force);
            }
        }
    }
}

#ifndef CN1_DISABLE_BIBOP
// ---- BiBOP overflow rescan (see the module header up top). Only engaged AFTER
// the mark worklist has overflowed, so the common (no-overflow) path pays
// nothing. Resumable cursor over the append-only all-pages registry. Only ever
// driven from the serial gcMarkDrain, so the static cursor is race-free. ----
static CN1BibopPage* bibopRescanPage = 0;
static int bibopRescanSlot = 0;
static void cn1BibopRescanStart() {
    bibopRescanPage = atomic_load_explicit(&bibopAllPages, memory_order_acquire);
    bibopRescanSlot = 0;
}
// Push every currently-marked page object whose class has a mark function,
// resuming where it left off when the worklist fills. Returns JAVA_TRUE once the
// whole registry has been scanned. A slot's header is dereferenced only when its
// (atomically read) mark equals the current cycle, which never holds for a slot
// a mutator is mid-initializing (its mark goes oldDead/FREE -> -1, not via cur).
static JAVA_BOOLEAN cn1BibopRescanStep() {
    while(bibopRescanPage != 0) {
        CN1BibopPage* p = bibopRescanPage;
        int n = atomic_load_explicit(&p->bumpIndex, memory_order_acquire);
        while(bibopRescanSlot < n) {
            if(gcMarkWorklistTop >= CN1_GC_MARK_WORKLIST_SIZE) {
                return JAVA_FALSE; // worklist full; caller drains and resumes
            }
            JAVA_OBJECT o = cn1BibopSlot(p, bibopRescanSlot);
            bibopRescanSlot++;
            int m = __atomic_load_n(&o->__codenameOneGcMark, __ATOMIC_ACQUIRE);
            if(m == currentGcMarkValue && o->__heapPosition == CN1_BIBOP_ADOPTED) {
                // Overflow setup drains the adoption buffer into allObjectsInHeap;
                // the legacy half of this same rescan owns this object now.
#ifdef CN1_GC_INSTRUMENT
                atomic_fetch_add_explicit(&cn1BibopAdoptedRescanSkips, 1,
                                          memory_order_relaxed);
#endif
                continue;
            }
            if(m == currentGcMarkValue && o->__codenameOneParentClsReference->markFunction != 0) {
                gcMarkWorklistPush(o, JAVA_FALSE);
            }
        }
        bibopRescanPage = atomic_load_explicit(&p->nextAll, memory_order_acquire);
        bibopRescanSlot = 0;
    }
    return JAVA_TRUE;
}
#endif

// Pops worklist entries and runs their mark functions. On overflow, rescans the live
// heap to push every marked-but-unscanned object so its children get visited (the
// children's pushes are what overflowed in the first place). The rescan uses a cursor
// that resumes across batches -- restarting from iter=0 on every batch would just
// re-push the same first WORKLIST_SIZE marked objects forever while later indices got
// starved, leaving their children unmarked and freeing reachable memory at sweep.
// BiBOP page slots are NOT in allObjectsInHeap, so once an overflow is seen the rescan
// additionally walks the page registry (cn1BibopRescan*) under the same fixed point.
// Run the mark functions of everything currently on the worklist, and of everything they
// push, until it is empty. NOTHING else -- no heap rescan, no fixpoint, no adopt-buffer
// registration. That distinction is the whole reason this exists as its own function
// (issue #5537).
//
// gcMarkDrain below is not "drain the worklist": every call to it also walks
// allObjectsInHeap from index 0 and re-pushes every object already marked this cycle, so
// that a marked-but-unscanned object left behind by an overflow gets its mark function
// run. That is right for the handful of times a cycle calls it, and catastrophic for a
// caller that needs to drain PERIODICALLY: the grace pass drains once per half-worklist,
// which on a real app turned one O(table) rescan per cycle into hundreds of them. The
// collector then never finished a cycle, mutators piled up in the pacing park, and the
// app hung -- measured on the Mac Catalyst screenshot suite, where the legacy table is a
// live UI rather than the handful of objects a translated micro-benchmark holds.
//
// Callers that need the fixpoint still call gcMarkDrain; a pass that only needs room in
// the worklist calls this. Anything this leaves behind is picked up by the full drain
// that ends the same pass.
static void gcMarkDrainWorklist(CODENAME_ONE_THREAD_STATE) {
    while(gcMarkWorklistTop > 0) {
        gcMarkWorklistTop--;
        JAVA_OBJECT obj = gcMarkWorklist[gcMarkWorklistTop].obj;
        JAVA_BOOLEAN force = gcMarkWorklist[gcMarkWorklistTop].force;
#ifdef CN1_BIBOP_VALIDATE
        // The (serial) mark drain crashed here calling obj->parentCls->markFunction
        // on an object whose parentCls is a non-null GARBAGE pointer (fp jumped into
        // libc). A live, correctly-enqueued object always carries gcMark ==
        // currentGcMarkValue (gcMarkObject stamps it BEFORE pushing) or -1 (grace),
        // and a real clazz's markFunction lies in the app text (never a libc/heap
        // address). Abort AT the source with the full object + fp state so the next
        // run says whether this is a freed/reused slot (stale gcMark) or a
        // corrupted-parentCls object.
        {
            gcMarkFunctionPointer __vfp = (obj != JAVA_NULL && !CN1_IS_TAGGED(obj)
                && obj->__codenameOneParentClsReference != 0)
                ? obj->__codenameOneParentClsReference->markFunction : (gcMarkFunctionPointer)0;
            int __vmark = (obj != JAVA_NULL && !CN1_IS_TAGGED(obj)) ? obj->__codenameOneGcMark : -999;
            int __vhp = (obj != JAVA_NULL && !CN1_IS_TAGGED(obj)) ? obj->__heapPosition : -999;
            // A real markFunction lives in the app text segment; anchor off a
            // known app function (&gcMarkObject) and flag any fp more than 256MB
            // away (the observed garbage fp was a libc-range address).
            uintptr_t __anchor = (uintptr_t)(void*)&gcMarkObject;
            uintptr_t __fpv = (uintptr_t)(void*)__vfp;
            int __fpBad = (__vfp != 0) &&
                ((__fpv > __anchor ? __fpv - __anchor : __anchor - __fpv) > (256ULL << 20));
            if(obj == JAVA_NULL || CN1_IS_TAGGED(obj) ||
               obj->__codenameOneParentClsReference == 0 ||
               (__vhp != CN1_BIBOP_HEAP_POS && __vhp != CN1_BIBOP_ADOPTED && __vhp < -1) ||
               (__vmark != currentGcMarkValue && __vmark != -1) ||
               __fpBad) {
                fprintf(stderr, "CN1BIBOP MARKDRAIN CORRUPT: obj=%p tagged=%d parentCls=%p "
                        "markFn=%p heapPosition=%d gcMark=%d curMark=%d FREE_MARK=%d force=%d\n",
                        (void*)obj, (int)CN1_IS_TAGGED(obj),
                        (obj && !CN1_IS_TAGGED(obj)) ? (void*)obj->__codenameOneParentClsReference : (void*)0,
                        (void*)__vfp, __vhp, __vmark, currentGcMarkValue,
                        CN1_BIBOP_FREE_MARK, (int)force);
                fflush(stderr);
                abort();
            }
        }
#endif
        gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
        if(fp != 0) {
#ifdef CN1_BIBOP_VALIDATE
            gcMarkCurrentDrainObj = obj;
#endif
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
            // Cascade: if this object has been MATURED, mature the children its mark
            // function is about to mark, so the whole reachable subtree graduates
            // together (never half a tree). Restored after the call.
            JAVA_BOOLEAN __savedMaturing = gcCurrentlyMaturing;
            gcCurrentlyMaturing = (obj->__heapPosition == CN1_BIBOP_ADOPTED) ? JAVA_TRUE : __savedMaturing;
#endif
            fp(threadStateData, obj, force);
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
            gcCurrentlyMaturing = __savedMaturing;
#endif
        }
    }
}

static void gcMarkDrain(CODENAME_ONE_THREAD_STATE) {
    atomic_fetch_add_explicit(&cn1GcFullDrains, 1, memory_order_relaxed);
    if(cn1GcInGracePass) {
        atomic_fetch_add_explicit(&cn1GcGraceFullDrains, 1, memory_order_relaxed);
    }
    int rescanCursor = 0;
#ifndef CN1_DISABLE_BIBOP
    JAVA_BOOLEAN bibopActive = JAVA_FALSE;
    JAVA_BOOLEAN bibopDone = JAVA_TRUE;
#endif
    while(JAVA_TRUE) {
        gcMarkDrainWorklist(threadStateData);
#ifndef CN1_DISABLE_BIBOP
#if CN1_ADOPT_POLICY != 0
        // A rescan drain can mature more descendants. Register each batch before
        // the next page-rescan step so every adopted slot skipped by that step is
        // already owned by the legacy half of the same fixed-point scan.
        if(gcMarkWorklistOverflow || bibopActive) {
            cn1DrainAdoptBuffer();
        }
#endif
        // First time we observe an overflow, start also rescanning page slots.
        if(gcMarkWorklistOverflow && !bibopActive) {
            bibopActive = JAVA_TRUE;
            bibopDone = JAVA_FALSE;
            cn1BibopRescanStart();
        }
#endif
        int total = currentSizeOfAllObjectsInHeap;
#ifndef CN1_DISABLE_BIBOP
        JAVA_BOOLEAN scanDone = (rescanCursor >= total) && bibopDone;
#else
        JAVA_BOOLEAN scanDone = (rescanCursor >= total);
#endif
        // Done when the worklist drained without re-overflow AND we've finished a full
        // sweep of the heap (cursor at end) AND nothing new got marked during the most
        // recent sweep. Without the cursor==total check, we'd return while there are
        // still marked objects past `cursor` whose mark functions haven't been called.
        if(!gcMarkWorklistOverflow && scanDone) {
            return;
        }
        gcMarkWorklistOverflow = JAVA_FALSE;
        if(scanDone) {
            if(!gcMarkFoundUnmarkedChildInPass) {
                // We finished a full heap sweep, drained the resulting pushes, and the
                // drain marked nothing new. Fixed point.
                return;
            }
            // Pushes from the previous sweep's drain may have marked new objects past
            // indices we already visited this round; restart the sweep so they get
            // their mark functions called too.
            rescanCursor = 0;
            gcMarkFoundUnmarkedChildInPass = JAVA_FALSE;
#ifndef CN1_DISABLE_BIBOP
            if(bibopActive) {
                cn1BibopRescanStart();
                bibopDone = JAVA_FALSE;
            }
#endif
        }
        while(rescanCursor < total && gcMarkWorklistTop < CN1_GC_MARK_WORKLIST_SIZE) {
            JAVA_OBJECT o = allObjectsInHeap[rescanCursor];
            rescanCursor++;
            if(o != JAVA_NULL && o->__codenameOneGcMark == currentGcMarkValue) {
                if(o->__codenameOneParentClsReference->markFunction != 0) {
                    gcMarkWorklistPush(o, JAVA_FALSE);
                }
            }
        }
#ifndef CN1_DISABLE_BIBOP
        // Once the table is exhausted, continue the single linear rescan space into
        // the page registry (resumes its own cursor when the worklist refills).
        if(bibopActive && rescanCursor >= total && !bibopDone) {
            bibopDone = cn1BibopRescanStep();
        }
#endif
    }
}

// Resolve the total number of markers (the GC thread + helper threads). Computed once.
static int gcMarkResolveThreadCount() {
#ifdef CN1_GC_MARK_THREADS
    int n = CN1_GC_MARK_THREADS;
#elif 1
    // ISOLATION EXPERIMENT (git-A/B): default to SERIAL marking. The acquire-load
    // fix removed the parallel mark-worker crash, but arm64 Linux still corrupts the
    // heap (crash moved to a frameless method reading a smashed threadStateData), so
    // a SECOND ordering hole remains somewhere in the branch-only parallel-GC work.
    // Forcing one marker here bypasses the entire parallel path (gcMarkDrainParallel
    // -> gcMarkDrain, no atomics, no pool, no local buffers). If arm64 goes green,
    // parallel marking is the sole remaining corruptor and the concurrency audit
    // continues offline with CN1_GC_MARK_THREADS>1; if it still crashes, the bug is
    // elsewhere in the branch GC changes (nursery / tagged-int / BiBOP sweep).
    int n = 1;
#elif defined(_WIN32)
    // no sysconf in the Win32 shim; NUMBER_OF_PROCESSORS is always set on Windows
    const char* np = getenv("NUMBER_OF_PROCESSORS");
    long ncpu = np != 0 ? atol(np) : 2;
    int n = (int)(ncpu - 1);
#else
    long ncpu = sysconf(_SC_NPROCESSORS_ONLN);
    int n = (int)(ncpu - 1);
    if(n > 4) {
        n = 4;
    }
#endif
    if(n < 1) {
        n = 1;
    }
    return n;
}

// The body each marker (GC thread + helpers) runs for one parallel drain. Pops batches
// from the shared worklist, runs their mark functions (which push children into this
// thread's local buffer), flushes, and repeats until the worklist is empty and every
// marker is idle. See the design note at the worklist declarations.
static void gcMarkWorkerDrainLoop() {
    struct gcMarkLocalBuffer localBuf;
    localBuf.count = 0;
    gcMarkLocalBuf = &localBuf;
    struct gcMarkWorklistEntry batch[CN1_GC_MARK_BATCH];
    struct ThreadLocalData* d = gcMarkThreadState;

    pthread_mutex_lock(&gcMarkWorklistMutex);
    for(;;) {
        if(gcMarkWorklistTop > 0) {
            int n = gcMarkWorklistTop;
            if(n > CN1_GC_MARK_BATCH) {
                n = CN1_GC_MARK_BATCH;
            }
            gcMarkWorklistTop -= n;
            memcpy(batch, &gcMarkWorklist[gcMarkWorklistTop], n * sizeof(struct gcMarkWorklistEntry));
            pthread_mutex_unlock(&gcMarkWorklistMutex);

            for(int i = 0 ; i < n ; i++) {
                JAVA_OBJECT obj = batch[i].obj;
                gcMarkFunctionPointer fp = obj->__codenameOneParentClsReference->markFunction;
                if(fp != 0) {
#if CN1_ADOPT_POLICY != 0 && !defined(CN1_DISABLE_BIBOP)
                    // Same cascade as the serial gcMarkDrain: a matured object's children
                    // mature with it. gcCurrentlyMaturing is thread-local, so workers don't
                    // race. (Registration is deferred + locked, so this is only about which
                    // objects get flagged -- always safe.)
                    JAVA_BOOLEAN __savedMaturing = gcCurrentlyMaturing;
                    gcCurrentlyMaturing = (obj->__heapPosition == CN1_BIBOP_ADOPTED) ? JAVA_TRUE : __savedMaturing;
                    fp(d, obj, batch[i].force);
                    gcCurrentlyMaturing = __savedMaturing;
#else
                    fp(d, obj, batch[i].force);
#endif
                }
            }
            gcMarkFlushLocal(&localBuf);

            pthread_mutex_lock(&gcMarkWorklistMutex);
            continue;
        }
        // No work in hand and the global worklist is empty: this marker goes idle.
        gcMarkActiveWorkers--;
        if(gcMarkActiveWorkers == 0) {
            // Empty worklist AND every marker idle => the reachable set is fully drained.
            gcMarkDone = JAVA_TRUE;
            pthread_cond_broadcast(&gcMarkWorklistCond);
            break;
        }
        while(gcMarkWorklistTop == 0 && !gcMarkDone) {
            pthread_cond_wait(&gcMarkWorklistCond, &gcMarkWorklistMutex);
        }
        if(gcMarkDone) {
            break;
        }
        // Work appeared (another marker produced children) -- become active again.
        gcMarkActiveWorkers++;
    }
    pthread_mutex_unlock(&gcMarkWorklistMutex);
    gcMarkLocalBuf = 0;
}

// Helper-thread entry point. Sleeps on the control condition until the GC thread bumps
// the generation to dispatch a drain, participates, then reports completion. Lives for
// the lifetime of the process (like the GC thread itself).
extern void cn1InstallThreadAltStack(void);
static void* gcMarkWorkerMain(void* arg) {
    cn1InstallThreadAltStack();  // so a fault in the GC mark dumps a backtrace, not a silent die
    unsigned long myGen = 0;
    for(;;) {
        pthread_mutex_lock(&gcMarkCtlMutex);
        while(gcMarkGeneration == myGen) {
            pthread_cond_wait(&gcMarkCtlCond, &gcMarkCtlMutex);
        }
        myGen = gcMarkGeneration;
        pthread_mutex_unlock(&gcMarkCtlMutex);

        gcMarkWorkerDrainLoop();

        pthread_mutex_lock(&gcMarkCtlMutex);
        gcMarkWorkersFinished++;
        pthread_cond_broadcast(&gcMarkCtlCond);
        pthread_mutex_unlock(&gcMarkCtlMutex);
    }
    return 0;
}

// Lazily create the helper pool. Only ever called from the GC thread (single-threaded),
// so no synchronization is needed around the one-time setup.
static void gcMarkPoolEnsure() {
    if(gcMarkPoolReady) {
        return;
    }
    gcMarkThreadCount = gcMarkResolveThreadCount();
    gcMarkPoolSize = gcMarkThreadCount - 1;
    for(int i = 0 ; i < gcMarkPoolSize ; i++) {
        pthread_t tid;
        if(pthread_create(&tid, 0, gcMarkWorkerMain, 0) == 0) {
            pthread_detach(tid);
        } else {
            // Could not spawn a helper; fall back to fewer markers (at least the GC thread).
            gcMarkPoolSize = i;
            gcMarkThreadCount = i + 1;
            break;
        }
    }
    gcMarkPoolReady = JAVA_TRUE;
}

// Parallel transitive drain. The worklist has already been seeded with roots (serially,
// while the relevant mutator thread is paused). Dispatches the helper pool, participates
// on the GC thread, waits for everyone to finish, then -- only if the worklist overflowed
// -- runs one serial gcMarkDrain to execute the heap-rescan fixed point (invariant #3).
static void gcMarkDrainParallel(CODENAME_ONE_THREAD_STATE) {
    gcMarkPoolEnsure();
    if(gcMarkThreadCount <= 1) {
        // Single marker configured: behave exactly like before -- no pool, no atomics.
        gcMarkDrain(threadStateData);
        return;
    }

    gcMarkThreadState = threadStateData;

    // Reset termination state. Safe to touch unlocked here: the previous generation's
    // helpers have all reported finished (we waited below) and are parked on the control
    // condition, and the GC thread is the only one running between generations.
    pthread_mutex_lock(&gcMarkWorklistMutex);
    gcMarkActiveWorkers = gcMarkThreadCount; // GC thread + helpers
    gcMarkDone = JAVA_FALSE;
    pthread_mutex_unlock(&gcMarkWorklistMutex);

    // Dispatch the helpers.
    pthread_mutex_lock(&gcMarkCtlMutex);
    gcMarkWorkersFinished = 0;
    gcMarkGeneration++;
    pthread_cond_broadcast(&gcMarkCtlCond);
    pthread_mutex_unlock(&gcMarkCtlMutex);

    // The GC thread participates as one marker.
    gcMarkWorkerDrainLoop();

    // Wait for the helpers to finish this generation before returning (so no helper is
    // still touching mark bits when the caller proceeds to release threads / sweep).
    pthread_mutex_lock(&gcMarkCtlMutex);
    while(gcMarkWorkersFinished < gcMarkPoolSize) {
        pthread_cond_wait(&gcMarkCtlCond, &gcMarkCtlMutex);
    }
    pthread_mutex_unlock(&gcMarkCtlMutex);

    // Overflow safety net: finish deferred field scans with the serial rescan fixed point.
    if(gcMarkWorklistOverflow) {
        gcMarkDrain(threadStateData);
    }
}

// ---- FUSED OBJECTS -------------------------------------------------------
// A fused object is an owner whose ENCAPSULATED child (e.g. java.lang.String's
// char[] value -- never exposed outside the class) is laid out INSIDE the
// owner's own allocation block instead of being a separate heap object. The
// child keeps a full, ordinary object header so every reader treats it as a
// normal object, but it has NO independent GC identity:
//   * it is never registered anywhere (no table entry, no page slot of its own),
//     so the sweep -- which walks BiBOP slot boundaries / table entries only --
//     can never free it separately: it dies with its owner's slot;
//   * the conservative resolver maps any pointer into the block (including the
//     child header and its interior data) to the SLOT BASE, i.e. the OWNER, so
//     a stack/register reference to the child keeps the whole block alive;
//   * the owner's generated mark function still gcMarkObject()s the child --
//     harmless stores into our own block (nothing ever sweeps by that mark).
// The block must live in a BiBOP page for the resolver-covers-interior property,
// so this returns NULL for oversized requests (or when BiBOP is unavailable)
// and the caller falls back to ordinary two-object allocation. The returned
// block is fully zeroed (cn1BibopAlloc mid-build safety) with the owner's
// parentCls set; the caller lays out the child header + data.
// OWNERSHIP CONTRACT (verified per class, not enforced at runtime): the child
// reference must never be stored anywhere that can outlive the owner. Reading
// it, passing it as a transient call argument, or returning copies is fine.
JAVA_OBJECT cn1AllocFused(CODENAME_ONE_THREAD_STATE, int totalSize, struct clazz* cls) {
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    if(totalSize <= CN1_BIBOP_MAX_OBJECT && constantPoolObjects != 0
#ifndef CN1_CONSERVATIVE_GC_ROOTS
       && !threadStateData->nativeAllocationMode
#endif
       ) {
        return cn1BibopAlloc(threadStateData, totalSize, cls);
    }
#endif
    return JAVA_NULL;
}

JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim) {
    int actualSize = length * primitiveSize;
    JAVA_ARRAY array = (JAVA_ARRAY)codenameOneGcMalloc(threadStateData, sizeof(struct JavaArrayPrototype) + actualSize + sizeof(void*), type);
    (*array).length = length;
    (*array).dimensions = dim;
    (*array).primitiveSize = primitiveSize;
    if(actualSize > 0) {
        void* arr = &(array->data);
        arr += sizeof(void*);
        (*array).data = arr;
    } else {
        (*array).data = 0;
    }
    return (JAVA_OBJECT)array;
}

JAVA_OBJECT allocArrayAligned(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim, int alignment) {
    int actualSize = length * primitiveSize;
    int requestedAlignment = alignment;
    if (requestedAlignment < (int)sizeof(void*)) {
        requestedAlignment = (int)sizeof(void*);
    }
    if ((requestedAlignment & (requestedAlignment - 1)) != 0) {
        requestedAlignment = 16;
    }
    int extraPadding = requestedAlignment - 1;
    JAVA_ARRAY array = (JAVA_ARRAY)codenameOneGcMalloc(threadStateData, sizeof(struct JavaArrayPrototype) + actualSize + sizeof(void*) + extraPadding, type);
    (*array).length = length;
    (*array).dimensions = dim;
    (*array).primitiveSize = primitiveSize;
    if (actualSize > 0) {
        char* arr = (char*)(&(array->data));
        arr += sizeof(void*);
        uintptr_t aligned = (((uintptr_t)arr) + ((uintptr_t)requestedAlignment - 1)) & ~((uintptr_t)requestedAlignment - 1);
        (*array).data = (void*)aligned;
    } else {
        (*array).data = 0;
    }
    return (JAVA_OBJECT)array;
}

JAVA_OBJECT alloc2DArray(CODENAME_ONE_THREAD_STATE, int length2, int length1, struct clazz* parentType, struct clazz* childType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 2);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, primitiveSize, 1);
        }
    }
    return (JAVA_OBJECT)base;
}

JAVA_OBJECT alloc3DArray(CODENAME_ONE_THREAD_STATE, int length3, int length2, int length1, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 3);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, sizeof(JAVA_OBJECT), 2);
            if(length3 > -1) {
                JAVA_ARRAY_OBJECT* internal = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)objs[iter])->data;
                for(int inner = 0 ; inner < length2 ; inner++) {
                    internal[inner] = allocArray(threadStateData, length3, grandChildType, primitiveSize, 1);
                }
            }
        }
    }
    return (JAVA_OBJECT)base;
}

JAVA_OBJECT alloc4DArray(CODENAME_ONE_THREAD_STATE, int length4, int length3, int length2, int length1, struct clazz* parentType, struct clazz* childType, struct clazz* grandChildType, struct clazz* greatGrandChildType, int primitiveSize) {
    JAVA_ARRAY base = (JAVA_ARRAY)allocArray(threadStateData, length1, parentType, sizeof(JAVA_OBJECT), 4);
    JAVA_ARRAY_OBJECT* objs = base->data;
    if(length2 > -1) {
        for(int iter = 0 ; iter < length1 ; iter++) {
            objs[iter] = allocArray(threadStateData, length2, childType, sizeof(JAVA_OBJECT), 3);
            if(length3 > -1) {
                JAVA_ARRAY_OBJECT* internal = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)objs[iter])->data;
                for(int inner = 0 ; inner < length2 ; inner++) {
                    internal[inner] = allocArray(threadStateData, length3, grandChildType, sizeof(JAVA_OBJECT), 2);
                    if(length4 > -1) {
                        JAVA_ARRAY_OBJECT* deep = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)internal[inner])->data;
                        for(int deepInner = 0 ; deepInner < length3 ; deepInner++) {
                            deep[deepInner] = allocArray(threadStateData, length4, greatGrandChildType, primitiveSize, 1);
                        }
                    }
                }
            }
        }
    }
    return (JAVA_OBJECT)base;
}

/**
 * Creates a java.lang.String object from an array of integers, this is useful
 * for the constant pool
 */
JAVA_OBJECT newString(CODENAME_ONE_THREAD_STATE, int length, JAVA_CHAR data[]) {
    enteringNativeAllocations();
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_CHAR, sizeof(JAVA_CHAR), 1);
    memcpy((*dat).data, data, length * sizeof(JAVA_ARRAY_CHAR));
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    java_lang_String___INIT____(threadStateData, o);
    struct obj__java_lang_String* str = (struct obj__java_lang_String*)o;
    str->java_lang_String_value = (JAVA_OBJECT)dat;
    str->java_lang_String_count = length;
    finishedNativeAllocations();
    return o;
}

/**
 * Creates a java.lang.String object from a c string
 */
JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str) {
    if(str == 0) {
        return JAVA_NULL;
    }
    enteringNativeAllocations();
    int length = (int)strlen(str);
    // Compact strings: decode into a temporary char buffer first so we can detect
    // whether the fully-decoded string is Latin-1 (every code unit <= 0xFF) and,
    // if so, store it as a compact byte[] instead of a char[]. The ~~uXXXX escape
    // can inject any UTF-16 code unit, so a blind byte[] copy would be wrong for a
    // literal carrying a code unit above 0xFF -- those still get a char[].
    // NOTE: the char produced per source char is (JAVA_ARRAY_CHAR)str[iter], the
    // SAME implicit char->uint16 widening the old code performed, so a source byte
    // with the high bit set (only possible for a raw, non-escaped byte) widens to
    // 0xFFxx and therefore stays on the char[] path -- bit-identical to before.
    JAVA_ARRAY_CHAR stackBuf[256];
    JAVA_ARRAY_CHAR* tmp = length <= 256 ? stackBuf : (JAVA_ARRAY_CHAR*)malloc((size_t)length * sizeof(JAVA_ARRAY_CHAR));
    int offset = 0;
    JAVA_BOOLEAN latin1 = JAVA_TRUE;
    for(int iter = 0 ; iter < length ; iter++) {
        JAVA_ARRAY_CHAR c = (JAVA_ARRAY_CHAR)str[iter];
        if(str[iter] == '~' && iter + 6 < length && str[iter+1] == '~' && str[iter+2] == 'u') {
            char constructB[5];
            constructB[0] = str[iter + 3];
            constructB[1] = str[iter + 4];
            constructB[2] = str[iter + 5];
            constructB[3] = str[iter + 6];
            constructB[4] = 0;
            c = (JAVA_ARRAY_CHAR)strtol(constructB, NULL, 16);
            iter += 6;
        }
        if(c > 0xff) latin1 = JAVA_FALSE;
        tmp[offset++] = c;
    }
    JAVA_ARRAY dat;
    if(latin1) {
        // compact Latin-1: one byte per code unit, (byte & 0xff) IS the char.
        dat = (JAVA_ARRAY)allocArray(threadStateData, offset, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
        JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) (*dat).data;
        for(int i = 0 ; i < offset ; i++) {
            b[i] = (JAVA_ARRAY_BYTE)tmp[i];
        }
    } else {
        dat = (JAVA_ARRAY)allocArray(threadStateData, offset, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
        JAVA_ARRAY_CHAR* a = (JAVA_ARRAY_CHAR*) (*dat).data;
        for(int i = 0 ; i < offset ; i++) {
            a[i] = tmp[i];
        }
    }
    if(tmp != stackBuf) {
        free(tmp);
    }
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    //java_lang_String___INIT_____char_1ARRAY(threadStateData, o, (JAVA_OBJECT)dat);
    //releaseObj(threadStateData, (JAVA_OBJECT)dat);
    java_lang_String___INIT____(threadStateData, o);
    struct obj__java_lang_String* ss = (struct obj__java_lang_String*)o;
    ss->java_lang_String_value = (JAVA_OBJECT)dat;
    ss->java_lang_String_count = offset;
    finishedNativeAllocations();
    return o;
}

// Build a compact Latin-1 String directly from a known-ASCII byte range (decimal
// digits, hex, boolean literals, ...). Unlike newStringFromCString it skips the
// strlen + char[] decode + Latin-1 detection: every byte is guaranteed <= 0x7F by
// the caller, so it is one alloc + one copy into a byte[]-backed String. This is
// the internal ASCII fast path for generators like Long/Integer.toString.
// Begin a SINGLE-allocation fused compact Latin-1 String: its byte[] lives INLINE in the String's
// own BiBOP block. Returns a valid empty String (count=0) with *dst pointing at the inline byte
// buffer; the caller fills dst[0..len) and then publishes the real length with
// cn1FusedLatin1End(). Returns NULL when a fused block is unavailable (oversize / BiBOP off), in
// which case the caller performs the ordinary 2-object build.
// This is the "1 alloc instead of byte[]+String" fast path shared by Long/Integer.toString and the
// String.cn1Concat helpers -- the bulk of a string-building workload's GC garbage.
JAVA_OBJECT cn1FusedLatin1Begin(CODENAME_ONE_THREAD_STATE, int len, JAVA_ARRAY_BYTE** dst) {
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    if(__builtin_expect(class__java_lang_String.initialized, 1)) {
        int off = (int)((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7);
        int total = off + CN1_FUSED_ARR_BYTES(len, sizeof(JAVA_ARRAY_BYTE));
        // Full BiBOP alloc (handles freeList / bump / page-acquire) so the fused path stays effective
        // for the WHOLE run. The no-zero fast path only bump-allocates FRESH pages and degrades to the
        // 2-object fallback once pages go partial -- which made an earlier version REGRESS (try-fused-
        // fail + fallback). The slot here is ZEROED and PUBLISHED, i.e. a valid EMPTY String (count=0):
        // a concurrent GC that traces it during the caller's fill sees an empty string (value marked, no
        // garbage), so there is no init-before-publish race. Caller fills *dst[0..len) then
        // cn1FusedLatin1End(so, len) sets the count LAST (count>0 always implies a fully-written value).
        if(total <= CN1_BIBOP_MAX_OBJECT) {
            JAVA_OBJECT so = cn1BibopAlloc(threadStateData, total, &class__java_lang_String);
            if(so != JAVA_NULL) {
                JAVA_OBJECT arr = cn1FusedInstallPrimArray(so, off, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), len);
                ((struct obj__java_lang_String*)so)->java_lang_String_value = arr; // count stays 0 until End
                *dst = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)arr)->data;
                return so;
            }
        }
    }
#endif
    (void)dst;
    return JAVA_NULL;
}

JAVA_OBJECT newStringFromAsciiLen(CODENAME_ONE_THREAD_STATE, const char *src, int len) {
    // Fused single-alloc fast path (byte[] inline in the String block).
    JAVA_ARRAY_BYTE* fdst;
    JAVA_OBJECT fso = cn1FusedLatin1Begin(threadStateData, len, &fdst);
    if(fso != JAVA_NULL) {
        for(int i = 0 ; i < len ; i++) {
            fdst[i] = (JAVA_ARRAY_BYTE)src[i];
        }
        cn1FusedLatin1End(fso, len);
        return fso;
    }
    // Fallback: separate byte[] + String (oversize / BiBOP unavailable).
    enteringNativeAllocations();
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, len, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) (*dat).data;
    for(int i = 0 ; i < len ; i++) {
        b[i] = (JAVA_ARRAY_BYTE)src[i];
    }
    JAVA_OBJECT o = __NEW_java_lang_String(threadStateData);
    java_lang_String___INIT____(threadStateData, o);
    struct obj__java_lang_String* ss = (struct obj__java_lang_String*)o;
    ss->java_lang_String_value = (JAVA_OBJECT)dat;
    ss->java_lang_String_count = len;
    finishedNativeAllocations();
    return o;
}

/**
 * XMLVM compatibility layer
 */
JAVA_OBJECT xmlvm_create_java_string(CODENAME_ONE_THREAD_STATE, const char *chr) {
    return newStringFromCString(threadStateData, chr);
}

// Preallocated StackOverflowError (the JDK does the same): an SOE is thrown at
// STACK EXHAUSTION, where building a fresh error's stack trace calls more
// methods -- each of which trips the same overflow guard and throws again,
// recursing until the hard guard page (observed as a 500+ frame
// throwException/fillInStack/getStack storm ending in SIGSEGV on iOS).
// The preallocated instance has its stack field PRE-FILLED, so
// fillInStack's null-check skips trace building entirely: throwing it
// allocates nothing and calls nothing.
JAVA_OBJECT cn1PreallocSOE = JAVA_NULL;
extern void set_field_java_lang_Throwable_stack(JAVA_OBJECT __cn1Val, JAVA_OBJECT __cn1T);

void cn1ThrowStackOverflow(CODENAME_ONE_THREAD_STATE) {
    JAVA_OBJECT soe = cn1PreallocSOE;
    if(soe == JAVA_NULL) {
        // startup-only fallback (before initConstantPool preallocates)
        soe = __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData);
    }
    throwException(threadStateData, soe);
}

void initConstantPool() {
    __STATIC_INITIALIZER_java_lang_Class(getThreadLocalData());
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    enteringNativeAllocations();
    JAVA_ARRAY arr = (JAVA_ARRAY)allocArray(threadStateData, CN1_CONSTANT_POOL_SIZE, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    JAVA_OBJECT* tmpConstantPoolObjects = (JAVA_ARRAY_OBJECT*)(*arr).data;
    
    // the constant pool should not be deleted...
    for(int iter = 0 ; iter < threadStateData->heapAllocationSize ; iter++) {
        if(threadStateData->pendingHeapAllocations[iter] == arr)  {
            threadStateData->pendingHeapAllocations[iter] = JAVA_NULL;
            break;
        }
    }
    invokedGC = YES;
    //int cStringSize = CN1_CONSTANT_POOL_SIZE * sizeof(char*);
    //int jStringSize = CN1_CONSTANT_POOL_SIZE * sizeof(JAVA_ARRAY);
    //JAVA_OBJECT internedStrings = get_static_java_lang_String_str();
    for(int iter = 0 ; iter < CN1_CONSTANT_POOL_SIZE ; iter++) {
        //long length = strlen(constantPool[iter]);
        //cStringSize += length + 1;
        //jStringSize += length * sizeof(JAVA_ARRAY_CHAR) + sizeof(struct JavaArrayPrototype) + sizeof(struct obj__java_lang_String);
        JAVA_OBJECT oo = newStringFromCString(threadStateData, constantPool[iter]);
        tmpConstantPoolObjects[iter] = oo;
       // java_util_ArrayList_add___java_lang_Object_R_boolean(threadStateData, internedStrings, oo);
    }
    #if defined(__OBJC__)
    //NSLog(@"Size of constant pool in c: %i and j: %i", cStringSize, jStringSize);
    #endif
    constantPoolObjects = tmpConstantPoolObjects;
    invokedGC = NO;

    // preallocate the shared StackOverflowError with a pre-filled trace (see
    // cn1ThrowStackOverflow above); built HERE where stack is plentiful
    cn1PreallocSOE = __NEW_INSTANCE_java_lang_StackOverflowError(threadStateData);
    set_field_java_lang_Throwable_stack(
        newStringFromCString(threadStateData, "java.lang.StackOverflowError\n    (trace suppressed: thrown at stack exhaustion)\n"),
        cn1PreallocSOE);
    cn1AddImmortalRoot(cn1PreallocSOE);

    enteringNativeAllocations();

    // Low-memory throttle diagnostics and the CN1_SIMULATE_MEMORY_WARNING_MS test
    // hook. Both are no-ops unless their environment variable is set.
    atexit(cn1ReportLowMemoryParks);
    atexit(cn1ReportPacingParks);
    atexit(cn1ReportGcOverflow);
    cn1StartSimulatedMemoryWarnings();

    // it will wait two seconds unless an explicit GC occurs
    java_lang_System_startGCThread__(threadStateData);
    finishedNativeAllocations();
}

JAVA_OBJECT utf8String = NULL;

#if defined(__APPLE__) && defined(__OBJC__)
JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str) {
    if (str == nil) {
        return JAVA_NULL;
    }
    enteringNativeAllocations();
    if (utf8String == JAVA_NULL) {
        utf8String = newStringFromCString(threadStateData, "UTF-8");
        removeObjectFromHeapCollection(threadStateData, utf8String);
        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)utf8String)->java_lang_String_value);
    }
    JAVA_OBJECT s = __NEW_java_lang_String(threadStateData);
    const char* chars = [str UTF8String];
    int length = (int)strlen(chars);
    
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    memcpy((*dat).data, chars, length * sizeof(JAVA_ARRAY_BYTE));
    java_lang_String___INIT_____byte_1ARRAY_java_lang_String(threadStateData, s, (JAVA_OBJECT)dat, utf8String);
    struct obj__java_lang_String* nnn = (struct obj__java_lang_String*)s;
    nnn->java_lang_String_nsString = str;
    [str retain];
    // The retained NSString peer must be released when this String's slot is
    // reclaimed. With String.finalize() gone, peer release happens in the BiBOP
    // page sweep -- but ONLY on pages flagged by cn1BibopNoteNativePeer: an
    // unflagged all-dead page takes the O(1) fast reclaim that never visits its
    // slots, and every retained peer on it would leak. toNSString() flags its
    // page (see below); this path cached a peer without flagging -- the leak a
    // review caught.
    cn1BibopNoteNativePeer(s);
    finishedNativeAllocations();
    return s;
}
#endif

const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {
    if(str == NULL) {
        return NULL;
    }
    if (utf8String == JAVA_NULL) {
        utf8String = newStringFromCString(threadStateData, "UTF-8");
        removeObjectFromHeapCollection(threadStateData, utf8String);
        removeObjectFromHeapCollection(threadStateData, ((struct obj__java_lang_String*)utf8String)->java_lang_String_value);
    }

    JAVA_ARRAY byteArray = (JAVA_ARRAY)java_lang_String_getBytes___java_lang_String_R_byte_1ARRAY(threadStateData, str, utf8String);
    JAVA_ARRAY_BYTE* data = (*byteArray).data;

    JAVA_INT len = byteArray->length;

    if(threadStateData->utf8Buffer == 0) {
        threadStateData->utf8Buffer = malloc(len + 1);
        threadStateData->utf8BufferSize = len+1;
    } else {
        if(threadStateData->utf8BufferSize < len + 1) {
            free(threadStateData->utf8Buffer);
            threadStateData->utf8Buffer = malloc(len + 1);
            threadStateData->utf8BufferSize = len+1;
        }
    }
    char* cs = threadStateData->utf8Buffer;
    memcpy(cs, data, len);
    cs[len] = '\0';
    return cs;
}

#if defined(__APPLE__) && defined(__OBJC__)
NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o) {
    if(o == JAVA_NULL) {
        return 0;
    }
    struct obj__java_lang_String* str = (struct obj__java_lang_String*)o;
    if(str->java_lang_String_nsString != 0) {
        void* v = (void*)str->java_lang_String_nsString;
        return (__bridge NSString*)v;
    }
    const char* chrs = stringToUTF8(threadStateData, o);
    NSString* st = [[NSString stringWithUTF8String:chrs] retain];
    void *x = (__bridge void *)(st);
    str->java_lang_String_nsString = (JAVA_LONG)x;
    // ensure the dead slot reaches cn1BibopReclaimSlot to release the peer
    cn1BibopNoteNativePeer(o);
    return st;
}
#endif

JAVA_OBJECT __NEW_ARRAY_JAVA_BOOLEAN(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_BOOLEAN, sizeof(JAVA_ARRAY_BOOLEAN), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_BOOLEAN;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_CHAR(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_CHAR;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_BYTE(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_BYTE;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_SHORT(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_SHORT, sizeof(JAVA_ARRAY_SHORT), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_SHORT;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_INT(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_INT;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_LONG(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_LONG, sizeof(JAVA_ARRAY_LONG), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_LONG;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_FLOAT(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_FLOAT;
    return o;
}

JAVA_OBJECT __NEW_ARRAY_JAVA_DOUBLE(CODENAME_ONE_THREAD_STATE, JAVA_INT size) {
    JAVA_OBJECT o = allocArray(threadStateData, size, &class_array1__JAVA_DOUBLE, sizeof(JAVA_ARRAY_DOUBLE), 1);
    (*o).__codenameOneParentClsReference = &class_array1__JAVA_DOUBLE;
    return o;
}

void throwException(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg) {
    #if defined(__OBJC__)
    //NSLog(@"Throwing exception!"); 
    #endif
    java_lang_Throwable_fillInStack__(threadStateData, exceptionArg); 
    threadStateData->exception = exceptionArg; 
    threadStateData->tryBlockOffset--; 
    while(threadStateData->tryBlockOffset >= 0) { 
        if (threadStateData->blocks[threadStateData->tryBlockOffset].monitor != 0) {
            // This tryblock was actually created by a synchronized method's monitorEnterBlock
            // We need to exit the monitor since the exception will cause us to 
            // leave the method.
            monitorExitBlock(threadStateData, threadStateData->blocks[threadStateData->tryBlockOffset].monitor);
            // Continue to search for a matching exception ...
            continue;
        } else if(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass <= 0 || instanceofFunction(threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass, exceptionArg->__codenameOneParentClsReference->classId)) {
            int off = threadStateData->tryBlockOffset;
            CN1_TRY_LONGJMP(threadStateData->blocks[off].destination, 1);
            return;
        } 
        threadStateData->tryBlockOffset--; 
    } 
}

JAVA_INT throwException_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg) {
    throwException(threadStateData, exceptionArg);
    return 0;
}

JAVA_BOOLEAN throwException_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT exceptionArg) {
    throwException(threadStateData, exceptionArg);
    return JAVA_FALSE;
}

void throwArrayIndexOutOfBoundsException(CODENAME_ONE_THREAD_STATE, int index) {
    JAVA_OBJECT arrayIndexOutOfBoundsException = __NEW_java_lang_ArrayIndexOutOfBoundsException(threadStateData);
    java_lang_ArrayIndexOutOfBoundsException___INIT_____int(threadStateData, arrayIndexOutOfBoundsException, index);
    throwException(threadStateData, arrayIndexOutOfBoundsException);
}

JAVA_BOOLEAN throwArrayIndexOutOfBoundsException_R_boolean(CODENAME_ONE_THREAD_STATE, int index) {
    throwArrayIndexOutOfBoundsException(threadStateData, index);
    return JAVA_FALSE;
}

// See the contract in cn1_globals.h. throwException() longjmps when a handler is
// found and returns when none is; the statement-form check macros have nothing to
// bail with, so returning here would fall through into the out-of-bounds access.
CN1_NORETURN void cn1ThrowArrayIndexOrDie(CODENAME_ONE_THREAD_STATE, int index) {
    throwArrayIndexOutOfBoundsException(threadStateData, index);
    // Unreachable while any handler is installed -- every Java thread root has one
    // (Thread.runImpl). Reached only from a native callback that entered Java
    // without a try block, where the alternative is committing the bad read.
    fprintf(stderr, "FATAL: array index %d out of bounds with no exception handler installed\n", index);
    fflush(stderr);
    abort();
}

// Null counterpart of cn1ThrowArrayIndexOrDie -- same reasoning: falling through
// would dereference the null array the check just rejected.
CN1_NORETURN void cn1ThrowNullPointerOrDie(CODENAME_ONE_THREAD_STATE) {
    throwException(threadStateData, __NEW_INSTANCE_java_lang_NullPointerException(threadStateData));
    fprintf(stderr, "FATAL: null array access with no exception handler installed\n");
    fflush(stderr);
    abort();
}

void** interfaceVtableGlobal = 0;

void** initVtableForInterface() {
    if(interfaceVtableGlobal == 0) {
        interfaceVtableGlobal = malloc(9 * sizeof(void*));
        interfaceVtableGlobal[0] = &java_lang_Object_equals___java_lang_Object_R_boolean;
        interfaceVtableGlobal[1] = &java_lang_Object_getClass___R_java_lang_Class;
        interfaceVtableGlobal[2] = &java_lang_Object_hashCode___R_int;
        interfaceVtableGlobal[3] = &java_lang_Object_notify__;
        interfaceVtableGlobal[4] = &java_lang_Object_notifyAll__;
        interfaceVtableGlobal[5] = &java_lang_Object_toString___R_java_lang_String;
        interfaceVtableGlobal[6] = &java_lang_Object_wait__;
        interfaceVtableGlobal[7] = &java_lang_Object_wait___long;
        interfaceVtableGlobal[8] = &java_lang_Object_wait___long_int;
        class_array1__JAVA_BOOLEAN.vtable = interfaceVtableGlobal;
        class_array2__JAVA_BOOLEAN.vtable = interfaceVtableGlobal;
        class_array3__JAVA_BOOLEAN.vtable = interfaceVtableGlobal;
        class_array1__JAVA_CHAR.vtable = interfaceVtableGlobal;
        class_array2__JAVA_CHAR.vtable = interfaceVtableGlobal;
        class_array3__JAVA_CHAR.vtable = interfaceVtableGlobal;
        class_array1__JAVA_BYTE.vtable = interfaceVtableGlobal;
        class_array2__JAVA_BYTE.vtable = interfaceVtableGlobal;
        class_array3__JAVA_BYTE.vtable = interfaceVtableGlobal;
        class_array1__JAVA_SHORT.vtable = interfaceVtableGlobal;
        class_array2__JAVA_SHORT.vtable = interfaceVtableGlobal;
        class_array3__JAVA_SHORT.vtable = interfaceVtableGlobal;
        class_array1__JAVA_INT.vtable = interfaceVtableGlobal;
        class_array2__JAVA_INT.vtable = interfaceVtableGlobal;
        class_array3__JAVA_INT.vtable = interfaceVtableGlobal;
        class_array1__JAVA_LONG.vtable = interfaceVtableGlobal;
        class_array2__JAVA_LONG.vtable = interfaceVtableGlobal;
        class_array3__JAVA_LONG.vtable = interfaceVtableGlobal;
        class_array1__JAVA_FLOAT.vtable = interfaceVtableGlobal;
        class_array2__JAVA_FLOAT.vtable = interfaceVtableGlobal;
        class_array3__JAVA_FLOAT.vtable = interfaceVtableGlobal;
        class_array1__JAVA_DOUBLE.vtable = interfaceVtableGlobal;
        class_array2__JAVA_DOUBLE.vtable = interfaceVtableGlobal;
        class_array3__JAVA_DOUBLE.vtable = interfaceVtableGlobal;
    }
    return interfaceVtableGlobal;
}

int byteSizeForArray(struct clazz* cls) {
    int byteSize = sizeof(JAVA_ARRAY_BYTE);
    if( cls->primitiveType ) {
        if((*cls).arrayType == &class__java_lang_Long) {
            byteSize = sizeof(JAVA_ARRAY_LONG);
        } else {
            if((*cls).arrayType == &class__java_lang_Double) {
                byteSize = sizeof(JAVA_ARRAY_DOUBLE);
            } else {
                if((*cls).arrayType == &class__java_lang_Float) {
                    byteSize = sizeof(JAVA_ARRAY_FLOAT);
                } else {
                    if((*cls).arrayType == &class__java_lang_Byte) {
                        byteSize = sizeof(JAVA_ARRAY_BYTE);
                    } else {
                        if((*cls).arrayType == &class__java_lang_Short) {
                            byteSize = sizeof(JAVA_ARRAY_SHORT);
                        } else {
                            if((*cls).arrayType == &class__java_lang_Integer) {
                                byteSize = sizeof(JAVA_ARRAY_INT);
                            } else {
                                if((*cls).arrayType == &class__java_lang_Character) {
                                    byteSize = sizeof(JAVA_ARRAY_CHAR);
                                } else {
                                    if((*cls).arrayType == &class__java_lang_Boolean) {
                                        byteSize = sizeof(JAVA_ARRAY_BOOLEAN);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        byteSize = sizeof(JAVA_OBJECT);
    }
    return byteSize;
}

JAVA_OBJECT cloneArray(JAVA_OBJECT array) {
    JAVA_ARRAY src = (JAVA_ARRAY)array;

    struct clazz* cls = array->__codenameOneParentClsReference;
    int byteSize = byteSizeForArray(cls);

    JAVA_ARRAY arr = (JAVA_ARRAY)allocArray(getThreadLocalData(), src->length, cls, byteSize, src->dimensions);
    memcpy( (*arr).data, (*src).data, arr->length * byteSize);
    return (JAVA_OBJECT)arr;
}

#ifdef CN1_ON_DEVICE_DEBUG
// Default-zero flag. The iOS on-device-debug listener flips this to 1 once
// it has accepted a proxy connection. Weak so a stronger definition in
// cn1_debugger.m (iOS port) wins when that file is linked into the build.
__attribute__((weak)) volatile int cn1DebuggerActive = 0;

// Weak stub that lets non-iOS / clean-output builds link even without the
// real listener. The strong implementation lives in
// Ports/iOSPort/nativeSources/cn1_debugger.m and is included in the iOS
// build when ios.onDeviceDebug=true.
__attribute__((weak)) void cn1_debugger_check(struct ThreadLocalData* threadStateData, int line) {
    (void)threadStateData;
    (void)line;
}

// Same arrangement: no debugger linked in means no issued ids to root.
__attribute__((weak)) void cn1_debugger_mark_issued_roots(struct ThreadLocalData* threadStateData) {
    (void)threadStateData;
}

// And again for the per-class registration the translator emits. Generated code
// calls this from every class's constructor, including in targets that do not
// link the debugger runtime (the watchOS slice), where it must simply do
// nothing.
__attribute__((weak)) void cn1_debugger_register_class(int classId, struct clazz* cls) {
    (void)classId;
    (void)cls;
}
#endif
