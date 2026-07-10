#ifndef CN1_WIN_COMPAT_H
#define CN1_WIN_COMPAT_H

/*
 * Minimal POSIX compatibility layer for building the ParparVM "clean" C
 * target on Windows with an LLVM toolchain (clang-cl / MSVC ABI).
 *
 * The runtime (cn1_globals / nativeMethods) is written against pthreads,
 * <unistd.h> (usleep) and <sys/time.h> (gettimeofday). The MSVC ABI provides
 * none of these, so this header declares the exact subset the runtime uses and
 * cn1_win_compat.c maps it onto Win32 primitives.
 *
 * The whole file is gated on _WIN32 so it is inert on iOS/macOS/Linux, where
 * the real POSIX headers are used instead. It is intentionally free of
 * <windows.h>: cn1_globals.h pulls this header into every translated
 * compilation unit, and leaking the Win32 macro soup that broadly would be a
 * recipe for collisions with generated symbol names. The Win32 lock/condvar
 * structs are mirrored by layout here and reinterpreted inside the .c file.
 */

#ifdef _WIN32

#include <time.h>   /* struct timespec (C11), localtime_s, _mkgmtime */
#include <stdint.h>
#include <stdlib.h> /* _putenv_s */

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Layout-compatible mirrors of Win32 SRWLOCK / CONDITION_VARIABLE. Both are
 * documented as a single pointer-sized field; cn1_win_compat.c static-asserts
 * the sizes match before reinterpreting these as the real types.
 */
typedef struct { void* Ptr; } cn1_srwlock_t;
typedef struct { void* Ptr; } cn1_condvar_t;

typedef struct { cn1_srwlock_t lock; } pthread_mutex_t;
typedef struct { cn1_condvar_t cond; } pthread_cond_t;

/* A zero-initialised SRWLOCK is a valid, unlocked lock (SRWLOCK_INIT == {0});
 * same for CONDITION_VARIABLE (CONDITION_VARIABLE_INIT == {0}). */
#define PTHREAD_MUTEX_INITIALIZER { { 0 } }
#define PTHREAD_COND_INITIALIZER  { { 0 } }

typedef unsigned long pthread_key_t;
typedef struct { void* handle; unsigned long id; } pthread_t;
typedef struct { int detachstate; } pthread_attr_t;

#define PTHREAD_CREATE_JOINABLE 0
#define PTHREAD_CREATE_DETACHED 1

#ifndef SCHED_OTHER
#define SCHED_OTHER 0
#endif

struct sched_param { int sched_priority; };

/* <sys/time.h> replacement. The Windows port's socket translation unit pulls in
 * winsock2.h, which already defines struct timeval; skip our definition when any
 * winsock header (or another timeval provider) is present in the translation
 * unit to avoid a redefinition. */
#if !defined(_TIMEVAL_DEFINED) && !defined(_WINSOCKAPI_) && !defined(_WINSOCK2API_)
struct timeval { long tv_sec; long tv_usec; };
#endif

/* --- mutex --- */
int pthread_mutex_init(pthread_mutex_t* mutex, const void* attr);
int pthread_mutex_destroy(pthread_mutex_t* mutex);
int pthread_mutex_lock(pthread_mutex_t* mutex);
int pthread_mutex_unlock(pthread_mutex_t* mutex);

/* --- condition variable --- */
int pthread_cond_init(pthread_cond_t* cond, const void* attr);
int pthread_cond_destroy(pthread_cond_t* cond);
int pthread_cond_wait(pthread_cond_t* cond, pthread_mutex_t* mutex);
int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, const struct timespec* abstime);
int pthread_cond_signal(pthread_cond_t* cond);
int pthread_cond_broadcast(pthread_cond_t* cond);

/* --- one-time initialization (BiBOP / nursery lazy init) --- */
typedef struct { long state; } pthread_once_t; /* 0=idle 1=running 2=done */
#define PTHREAD_ONCE_INIT { 0 }
int pthread_once(pthread_once_t* once_control, void (*init_routine)(void));

/* --- aligned allocation (BiBOP page arena) ---
   Maps onto _aligned_malloc. The arena NEVER frees page memory (the page
   registry is grow-only), so the _aligned_free pairing rule is moot. */
static __inline int posix_memalign(void** memptr, size_t alignment, size_t size) {
    void* p = _aligned_malloc(size, alignment);
    if (p == 0) {
        return 12; /* ENOMEM */
    }
    *memptr = p;
    return 0;
}

/* --- thread local storage --- */
int pthread_key_create(pthread_key_t* key, void (*destructor)(void*));
int pthread_key_delete(pthread_key_t key);
void* pthread_getspecific(pthread_key_t key);
int pthread_setspecific(pthread_key_t key, const void* value);

/* --- threads --- */
int pthread_attr_init(pthread_attr_t* attr);
int pthread_attr_destroy(pthread_attr_t* attr);
int pthread_attr_setdetachstate(pthread_attr_t* attr, int detachstate);
int pthread_create(pthread_t* thread, const pthread_attr_t* attr, void* (*start_routine)(void*), void* arg);
int pthread_detach(pthread_t thread);
pthread_t pthread_self(void);
int pthread_getschedparam(pthread_t thread, int* policy, struct sched_param* param);
int pthread_setschedparam(pthread_t thread, int policy, const struct sched_param* param);

/* --- <unistd.h> / <sys/time.h> replacements --- */
int usleep(unsigned int usec);
int gettimeofday(struct timeval* tv, void* tz);

/* --- environment / time.h POSIX helpers absent from MSVC ---
   Thin static-inline wrappers over the MSVC equivalents; used by the date /
   timezone runtime in nativeMethods. */
static __inline int setenv(const char* name, const char* value, int overwrite) {
    (void)overwrite; /* the runtime always overwrites, which _putenv_s does */
    return _putenv_s(name, value);
}

static __inline int unsetenv(const char* name) {
    return _putenv_s(name, "");
}

static __inline time_t timegm(struct tm* tm) {
    return _mkgmtime(tm);
}

static __inline struct tm* localtime_r(const time_t* timep, struct tm* result) {
    return localtime_s(result, timep) == 0 ? result : (struct tm*)0;
}

#ifdef __cplusplus
}
#endif

#endif /* _WIN32 */
#endif /* CN1_WIN_COMPAT_H */
