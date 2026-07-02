/*
 * Win32 implementation of the minimal POSIX surface declared in
 * cn1_win_compat.h. See that header for the rationale. Entirely gated on
 * _WIN32 so this is an empty translation unit on every other platform (it is
 * always copied into the generated "clean" project, and compiled away
 * elsewhere).
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <process.h>   /* _beginthreadex */
#include <stdlib.h>
#include <errno.h>

#include "cn1_win_compat.h"

/* The mirror structs in the header must match the real Win32 types exactly. */
_Static_assert(sizeof(cn1_srwlock_t) == sizeof(SRWLOCK), "SRWLOCK layout mismatch");
_Static_assert(sizeof(cn1_condvar_t) == sizeof(CONDITION_VARIABLE), "CONDITION_VARIABLE layout mismatch");

/* one-time init: double-checked CAS on the state word (0=idle 1=running 2=done) */
int pthread_once(pthread_once_t* once_control, void (*init_routine)(void)) {
    if (InterlockedCompareExchange((volatile LONG*)&once_control->state, 1, 0) == 0) {
        init_routine();
        InterlockedExchange((volatile LONG*)&once_control->state, 2);
    } else {
        while (InterlockedCompareExchange((volatile LONG*)&once_control->state, 2, 2) != 2) {
            Sleep(0);
        }
    }
    return 0;
}

/* --- mutex (non-recursive, like a default pthread mutex) --- */
int pthread_mutex_init(pthread_mutex_t* mutex, const void* attr) {
    (void)attr;
    InitializeSRWLock((PSRWLOCK)&mutex->lock);
    return 0;
}

int pthread_mutex_destroy(pthread_mutex_t* mutex) {
    (void)mutex; /* SRWLOCKs need no teardown */
    return 0;
}

int pthread_mutex_lock(pthread_mutex_t* mutex) {
    AcquireSRWLockExclusive((PSRWLOCK)&mutex->lock);
    return 0;
}

int pthread_mutex_unlock(pthread_mutex_t* mutex) {
    ReleaseSRWLockExclusive((PSRWLOCK)&mutex->lock);
    return 0;
}

/* --- condition variable --- */
int pthread_cond_init(pthread_cond_t* cond, const void* attr) {
    (void)attr;
    InitializeConditionVariable((PCONDITION_VARIABLE)&cond->cond);
    return 0;
}

int pthread_cond_destroy(pthread_cond_t* cond) {
    (void)cond;
    return 0;
}

int pthread_cond_wait(pthread_cond_t* cond, pthread_mutex_t* mutex) {
    SleepConditionVariableSRW((PCONDITION_VARIABLE)&cond->cond, (PSRWLOCK)&mutex->lock, INFINITE, 0);
    return 0;
}

int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, const struct timespec* abstime) {
    /* pthread passes an absolute CLOCK_REALTIME deadline; Win32 wants a
       relative millisecond timeout, so convert against the current time. */
    struct timeval now;
    long long now_ms, abs_ms, wait_ms;
    gettimeofday(&now, NULL);
    now_ms = (long long)now.tv_sec * 1000 + now.tv_usec / 1000;
    abs_ms = (long long)abstime->tv_sec * 1000 + abstime->tv_nsec / 1000000;
    wait_ms = abs_ms - now_ms;
    if (wait_ms < 0) {
        wait_ms = 0;
    }
    if (!SleepConditionVariableSRW((PCONDITION_VARIABLE)&cond->cond, (PSRWLOCK)&mutex->lock, (DWORD)wait_ms, 0)) {
        if (GetLastError() == ERROR_TIMEOUT) {
            return ETIMEDOUT;
        }
    }
    return 0;
}

int pthread_cond_signal(pthread_cond_t* cond) {
    WakeConditionVariable((PCONDITION_VARIABLE)&cond->cond);
    return 0;
}

int pthread_cond_broadcast(pthread_cond_t* cond) {
    WakeAllConditionVariable((PCONDITION_VARIABLE)&cond->cond);
    return 0;
}

/* --- thread local storage ---
   Keys are stored as (TLS index + 1) so that a zero-initialised key reliably
   reads as "uninitialised" (TLS index 0 is itself a valid slot). */
int pthread_key_create(pthread_key_t* key, void (*destructor)(void*)) {
    DWORD idx;
    (void)destructor; /* per-key destructors are not supported */
    idx = TlsAlloc();
    if (idx == TLS_OUT_OF_INDEXES) {
        return EAGAIN;
    }
    *key = (pthread_key_t)(idx + 1);
    return 0;
}

int pthread_key_delete(pthread_key_t key) {
    if (key == 0) {
        return EINVAL;
    }
    return TlsFree((DWORD)(key - 1)) ? 0 : EINVAL;
}

void* pthread_getspecific(pthread_key_t key) {
    if (key == 0) {
        return NULL;
    }
    return TlsGetValue((DWORD)(key - 1));
}

int pthread_setspecific(pthread_key_t key, const void* value) {
    if (key == 0) {
        return EINVAL;
    }
    return TlsSetValue((DWORD)(key - 1), (LPVOID)value) ? 0 : EINVAL;
}

/* --- threads --- */
struct cn1_thread_start {
    void* (*start)(void*);
    void* arg;
};

static unsigned __stdcall cn1_thread_trampoline(void* p) {
    struct cn1_thread_start s = *(struct cn1_thread_start*)p;
    free(p);
    s.start(s.arg);
    return 0;
}

int pthread_attr_init(pthread_attr_t* attr) {
    attr->detachstate = PTHREAD_CREATE_JOINABLE;
    return 0;
}

int pthread_attr_destroy(pthread_attr_t* attr) {
    (void)attr;
    return 0;
}

int pthread_attr_setdetachstate(pthread_attr_t* attr, int detachstate) {
    attr->detachstate = detachstate;
    return 0;
}

int pthread_create(pthread_t* thread, const pthread_attr_t* attr, void* (*start_routine)(void*), void* arg) {
    struct cn1_thread_start* s;
    uintptr_t h;
    unsigned tid = 0;
    s = (struct cn1_thread_start*)malloc(sizeof(struct cn1_thread_start));
    if (s == NULL) {
        return EAGAIN;
    }
    s->start = start_routine;
    s->arg = arg;
    /* The ParparVM clean target generates large per-method C stack frames, so
     * deep Codename One paint/layout recursion (complex forms, nested containers,
     * text wrapping in ChatView/TextArea) overflows the Win32 default 1MB thread
     * stack -> access violation. Reserve a generous 16MB stack for spawned
     * threads (notably the EDT, which performs the rendering). */
    h = _beginthreadex(NULL, (unsigned)(16 * 1024 * 1024), cn1_thread_trampoline, s, 0, &tid);
    if (h == 0) {
        free(s);
        return EAGAIN;
    }
    thread->handle = (void*)h;
    thread->id = tid;
    if (attr != NULL && attr->detachstate == PTHREAD_CREATE_DETACHED) {
        CloseHandle((HANDLE)h);
        thread->handle = NULL;
    }
    return 0;
}

pthread_t pthread_self(void) {
    pthread_t t;
    t.handle = GetCurrentThread(); /* pseudo-handle, valid for priority calls */
    t.id = GetCurrentThreadId();
    return t;
}

int pthread_getschedparam(pthread_t thread, int* policy, struct sched_param* param) {
    HANDLE h = thread.handle ? (HANDLE)thread.handle : GetCurrentThread();
    if (policy != NULL) {
        *policy = SCHED_OTHER;
    }
    if (param != NULL) {
        param->sched_priority = GetThreadPriority(h);
    }
    return 0;
}

int pthread_setschedparam(pthread_t thread, int policy, const struct sched_param* param) {
    HANDLE h = thread.handle ? (HANDLE)thread.handle : GetCurrentThread();
    (void)policy;
    if (param != NULL) {
        SetThreadPriority(h, param->sched_priority);
    }
    return 0;
}

/* --- <unistd.h> / <sys/time.h> replacements --- */
int usleep(unsigned int usec) {
    /* Millisecond granularity is sufficient for the runtime's polling loops. */
    Sleep((DWORD)((usec + 999) / 1000));
    return 0;
}

int gettimeofday(struct timeval* tv, void* tz) {
    FILETIME ft;
    ULARGE_INTEGER li;
    unsigned long long t;
    (void)tz;
    GetSystemTimePreciseAsFileTime(&ft); /* 100ns ticks since 1601-01-01 */
    li.LowPart = ft.dwLowDateTime;
    li.HighPart = ft.dwHighDateTime;
    t = li.QuadPart - 116444736000000000ULL; /* shift to the Unix epoch */
    tv->tv_sec = (long)(t / 10000000ULL);
    tv->tv_usec = (long)((t % 10000000ULL) / 10);
    return 0;
}

#endif /* _WIN32 */
