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

#ifndef _GNU_SOURCE
#define _GNU_SOURCE   /* pthread_getattr_np + ucontext REG_* gregs; before any libc include */
#endif
#include "cn1_linux.h"
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>
#include <ucontext.h>
#ifdef __GLIBC__
#include <execinfo.h> /* backtrace() -- glibc only; musl has no execinfo.h */
#endif
#include <unistd.h>

/* ---- hardware fault -> Java exception (POSIX analog of the iOS SignalHandler
 * and the Win32 cn1WinFaultToException) -------------------------------------
 *
 * ParparVM's clean C target only NULL-checks a method's `this` at entry. A null
 * *argument* deref, a virtual call on a null receiver, or an operand-stack slot
 * that legitimately reads back null all fault as a raw SIGSEGV in generated C
 * rather than a catchable NullPointerException -- which hard-kills the process
 * (and, on CI, the whole screenshot suite) with no Java stack trace. The iOS port
 * converts the identical fault into an NPE via a SIGSEGV handler that calls
 * throwException(); this is the Linux analog, hardened with the Win32 port's
 * "wild faulting address == real corruption, leave it diagnosable" guard.
 *
 * The handler runs on the faulting thread's own stack for a synchronous signal,
 * so longjmp-ing out of it via throwException() to the nearest CN1 try/catch
 * behaves exactly as a normally thrown Java exception (this is the documented
 * iOS technique). SA_NODEFER keeps the signal unblocked so faults remain
 * catchable on subsequent tests after we leave the handler.
 *
 * NOTE: like every such handler this interferes with a native debugger (gdb sees
 * the fault first-chance). It is a release/CI resilience mechanism, not a debug
 * aid -- set CN1_LINUX_NO_FAULT_HANDLER=1 to disable it when debugging under gdb.
 */
/* Dump the faulting thread's native backtrace to stderr (the app-output tee). Async-
 * signal-safe (write + backtrace). With the per-thread alternate signal stack this runs
 * even on a STACK OVERFLOW; the repeating frames are the recursion. Called from both the
 * SIGABRT path and the wild-address SIGSEGV path (stack-overflow guard-page faults land
 * there), so an overflow is finally legible instead of a bare, corrupt-unwind core. */
/* async-signal-safe "KEY=0xHEX\n" */
static void cn1LinuxWriteHexKV(const char* key, unsigned long v) {
    char buf[64]; int i = 0;
    while (key[i]) { buf[i] = key[i]; i++; }
    buf[i++] = '='; buf[i++] = '0'; buf[i++] = 'x';
    char hx[16]; int h = 0;
    if (v == 0) hx[h++] = '0';
    while (v) { int d = v & 0xf; hx[h++] = (char)(d < 10 ? '0' + d : 'a' + d - 10); v >>= 4; }
    while (h > 0) buf[i++] = hx[--h];
    buf[i++] = '\n';
    write(2, buf, i);
}

static void cn1LinuxDumpNativeBacktrace(const char* label, siginfo_t* si, void* ucv) {
    signal(SIGSEGV, SIG_DFL);
    signal(SIGBUS, SIG_DFL);
    write(2, "\n=====CN1 ", 10);
    write(2, label, strlen(label));
    write(2, " BACKTRACE=====\n", 16);
    if (si != NULL) cn1LinuxWriteHexKV("faultAddr", (unsigned long)(uintptr_t)si->si_addr);
    /* faulting PC + SP from the ucontext, and this thread's real stack bounds -- if the
     * used span (top - sp) is a fraction of a MB, this is a SMALL-STACK thread overflow
     * (e.g. CN1 rendering on a WebKit/Gallium/GLib worker thread), not deep recursion. */
    if (ucv != NULL) {
        ucontext_t* uc = (ucontext_t*)ucv;
        unsigned long sp = 0, pc = 0;
#if defined(__aarch64__)
        sp = (unsigned long)uc->uc_mcontext.sp;
        pc = (unsigned long)uc->uc_mcontext.pc;
#elif defined(__x86_64__)
        sp = (unsigned long)uc->uc_mcontext.gregs[REG_RSP];
        pc = (unsigned long)uc->uc_mcontext.gregs[REG_RIP];
#endif
        if (pc) cn1LinuxWriteHexKV("faultPC", pc);
        if (sp) cn1LinuxWriteHexKV("faultSP", sp);
        /* The integer registers. Reading them costs nothing and touches no memory, and
         * they are the only place the faulting POINTER survives: a wild address tells you
         * a read went wrong, the register file tells you what the value WAS and which
         * other register held the object it should have come from. gdb cannot supply this
         * from the core -- the suite is built -O3 with LTO, so `info args` and
         * `info locals` both answer "No locals" and every global reports "unknown type".
         * The one 0x100000028 fault that reached a core was diagnosed this far only
         * because faultAddr was printed here rather than recovered afterwards. */
#if defined(__x86_64__)
        {
            static const char* const rn[16] = {
                "rax","rbx","rcx","rdx","rsi","rdi","rbp","rsp",
                "r8","r9","r10","r11","r12","r13","r14","r15" };
            static const int ri[16] = {
                REG_RAX,REG_RBX,REG_RCX,REG_RDX,REG_RSI,REG_RDI,REG_RBP,REG_RSP,
                REG_R8,REG_R9,REG_R10,REG_R11,REG_R12,REG_R13,REG_R14,REG_R15 };
            int r;
            for (r = 0; r < 16; r++) {
                cn1LinuxWriteHexKV(rn[r], (unsigned long)uc->uc_mcontext.gregs[ri[r]]);
            }
        }
#elif defined(__aarch64__)
        {
            char nm[8]; int r;
            for (r = 0; r <= 30; r++) {
                nm[0] = 'x';
                if (r < 10) { nm[1] = (char)('0' + r); nm[2] = 0; }
                else { nm[1] = (char)('0' + r / 10); nm[2] = (char)('0' + r % 10); nm[3] = 0; }
                cn1LinuxWriteHexKV(nm, (unsigned long)uc->uc_mcontext.regs[r]);
            }
        }
#endif
        pthread_attr_t at;
        if (pthread_getattr_np(pthread_self(), &at) == 0) {
            void* base = 0; size_t sz = 0;
            if (pthread_attr_getstack(&at, &base, &sz) == 0) {
                cn1LinuxWriteHexKV("stackLo", (unsigned long)(uintptr_t)base);
                cn1LinuxWriteHexKV("stackSz", (unsigned long)sz);
                if (sp) cn1LinuxWriteHexKV("stackUsed", (unsigned long)(((uintptr_t)base + sz) - sp));
                /* The top of the faulting frame. Bounded by the real stack top, so this
                 * reads only mapped memory and cannot fault a second time -- the faulting
                 * ADDRESS is unmapped by definition and is deliberately not read.
                 *
                 * What it is for: a pointer-shaped fault whose low 32 bits are zero (the
                 * 0x100000000 case) is either a 64-bit value read as an object or a 32-bit
                 * write into a 64-bit slot, and the two look identical from the address
                 * alone. The neighbouring words separate them -- an adjacent half of the
                 * same 64-bit value is the first, an intact pointer beside a clobbered one
                 * is the second. */
                unsigned long hi = (unsigned long)(uintptr_t)base + (unsigned long)sz;
                if (sp && sp >= (unsigned long)(uintptr_t)base && sp < hi) {
                    unsigned long lim = sp + 32 * sizeof(unsigned long);
                    if (lim > hi) { lim = hi; }
                    unsigned long a;
                    int slot = 0;
                    char nm[16];
                    for (a = sp; a + sizeof(unsigned long) <= lim; a += sizeof(unsigned long)) {
                        nm[0]='s'; nm[1]='p'; nm[2]='[';
                        nm[3]=(char)('0' + slot / 10); nm[4]=(char)('0' + slot % 10);
                        nm[5]=']'; nm[6]=0;
                        cn1LinuxWriteHexKV(nm, *(unsigned long*)(uintptr_t)a);
                        slot++;
                    }
                }
            }
            pthread_attr_destroy(&at);
        }
    }
#ifdef __GLIBC__
    void* bt[96];
    int n = backtrace(bt, 96);
    backtrace_symbols_fd(bt, n, 2);
#else
    const char* na = "(native backtrace unavailable on this libc)\n";
    write(2, na, strlen(na));
#endif
    const char* ftr = "=====END CN1 BACKTRACE=====\n";
    write(2, ftr, strlen(ftr));
}

static void cn1LinuxFaultToException(int sig, siginfo_t* si, void* ucv) {
    (void) ucv;
    /* Only a genuine null-ish deref (null + a small field/vtable/array offset)
     * is converted to an NPE. A wild faulting address is real memory corruption OR a
     * stack overflow (the guard-page address is high): dump a backtrace so the failing
     * frame / recursion is identifiable from the CI log, then restore the default
     * disposition and return so the re-executed instruction faults again into a core
     * dump, instead of being masked as a recoverable NPE that silently corrupts state. */
    if ((sig == SIGSEGV || sig == SIGBUS) && si != NULL &&
            (uintptr_t) si->si_addr >= 0x10000) {
        cn1LinuxDumpNativeBacktrace("FAULT", si, ucv);
        signal(sig, SIG_DFL);
        return;
    }
    struct ThreadLocalData* t = getThreadLocalData();
    if (t == NULL || t->tryBlockOffset <= 0) {
        signal(sig, SIG_DFL);
        return;
    }
    throwException(t, __NEW_INSTANCE_java_lang_NullPointerException(t));
}

/* Diagnostic: when the process aborts (e.g. __stack_chk_fail / a glib g_error),
 * dump the native call stack so the failing frame is identifiable from the log
 * even where an interactive debugger is unavailable (CI containers). Enabled only
 * when CN1_LINUX_ABORT_BACKTRACE is set so it never interferes with normal runs. */
static void cn1LinuxAbortBacktrace(int sig) {
    cn1LinuxDumpNativeBacktrace("ABORT", NULL, NULL);
    signal(sig, SIG_DFL);
    raise(sig);
}

void cn1LinuxInstallFaultHandlers(void) {
#ifdef __GLIBC__
    {
        /* The first backtrace() loads libgcc and allocates; done here, so the
         * one in the fault dump runs without malloc inside a signal handler. */
        void* warm[1];
        (void) backtrace(warm, 1);
    }
#endif
    static int installed = 0;
    if (installed || getenv("CN1_LINUX_NO_FAULT_HANDLER") != NULL) {
        return;
    }
    installed = 1;
    /* Alternate signal stack for THIS (main) thread so SA_ONSTACK works here too. The CN1
     * worker/EDT/GC threads register their own in threadRunner/gcMarkWorkerMain. */
    {
        stack_t ss;
        ss.ss_sp = malloc(512 * 1024);
        if (ss.ss_sp != NULL) { ss.ss_size = 512 * 1024; ss.ss_flags = 0; sigaltstack(&ss, NULL); }
    }
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1LinuxFaultToException;
    /* SA_ONSTACK: run the handler on the per-thread alternate signal stack (registered in
     * threadRunner / gcMarkWorkerMain / crash-protection install) so it can execute even
     * on a STACK OVERFLOW, when the faulting thread's own stack is exhausted -- otherwise
     * the handler re-faults and the process dies silently with no backtrace. */
    sa.sa_flags = SA_SIGINFO | SA_NODEFER | SA_ONSTACK;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGBUS, &sa, NULL);
    /* Dump a native backtrace if the process aborts (e.g. __stack_chk_fail or a
     * glib g_error) so the failing frame is identifiable from the CI log without a
     * debugger. Silent unless an abort actually fires. */
    signal(SIGABRT, cn1LinuxAbortBacktrace);
}

