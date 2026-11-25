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
 * Runs a program under a seccomp filter that refuses io_uring syscalls with
 * EPERM, the way a container runtime's default profile does -- without needing
 * a container runtime, root or a particular CI image.
 *
 *   seccomp-deny-io-uring all   PROGRAM [ARGS...]   refuse setup, enter, register
 *   seccomp-deny-io-uring enter PROGRAM [ARGS...]   refuse only io_uring_enter
 *
 * "enter" is the profile that allows creating and probing a ring but not
 * submitting to it; the backend has to notice that at setup and use its pool.
 * Linux only. BackendHttpIntegrationTest compiles and runs it.
 */
#define _GNU_SOURCE
#include <errno.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <linux/audit.h>
#include <linux/filter.h>
#include <linux/seccomp.h>

#ifndef __NR_io_uring_setup
#define __NR_io_uring_setup 425
#endif
#ifndef __NR_io_uring_enter
#define __NR_io_uring_enter 426
#endif
#ifndef __NR_io_uring_register
#define __NR_io_uring_register 427
#endif

#if defined(__x86_64__)
#define CN1_AUDIT_ARCH AUDIT_ARCH_X86_64
#elif defined(__aarch64__)
#define CN1_AUDIT_ARCH AUDIT_ARCH_AARCH64
#else
#error "unsupported architecture"
#endif

#define DENY(nr) \
    BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, (nr), 0, 1), \
    BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ERRNO | (EPERM & SECCOMP_RET_DATA))

int main(int argc, char** argv) {
    struct sock_filter all[] = {
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, CN1_AUDIT_ARCH, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
        DENY(__NR_io_uring_setup),
        DENY(__NR_io_uring_enter),
        DENY(__NR_io_uring_register),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
    };
    struct sock_filter enter[] = {
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, arch)),
        BPF_JUMP(BPF_JMP | BPF_JEQ | BPF_K, CN1_AUDIT_ARCH, 1, 0),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
        BPF_STMT(BPF_LD | BPF_W | BPF_ABS, offsetof(struct seccomp_data, nr)),
        DENY(__NR_io_uring_enter),
        BPF_STMT(BPF_RET | BPF_K, SECCOMP_RET_ALLOW),
    };
    struct sock_fprog prog;
    if(argc < 3 || (strcmp(argv[1], "all") != 0 && strcmp(argv[1], "enter") != 0)) {
        fprintf(stderr, "usage: %s all|enter PROGRAM [ARGS...]\n", argv[0]);
        return 2;
    }
    if(strcmp(argv[1], "all") == 0) {
        prog.len = (unsigned short)(sizeof(all) / sizeof(all[0]));
        prog.filter = all;
    } else {
        prog.len = (unsigned short)(sizeof(enter) / sizeof(enter[0]));
        prog.filter = enter;
    }
    if(prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0
            || prctl(PR_SET_SECCOMP, SECCOMP_MODE_FILTER, &prog) != 0) {
        perror("seccomp");
        return 3;
    }
    execv(argv[2], argv + 2);
    perror("execv");
    return 4;
}
