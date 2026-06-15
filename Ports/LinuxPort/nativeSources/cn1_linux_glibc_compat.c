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
 * glibc 2.38 C23 compatibility shim for portable (old-glibc-targeted) builds.
 *
 * For portability the shipping binary is linked against an OLD glibc (zig's
 * pinned 2.28 target), so it runs on virtually any desktop distro. But the build
 * HOST is modern: when a translation unit pulls in the host's glibc >= 2.38
 * <stdlib.h> (e.g. cn1_linux_net.c via curl's include path), the C23 headers
 * #define strtol -> __isoc23_strtol (and the rest of the strto* family). Those
 * __isoc23_* symbols only exist in glibc >= 2.38, so the link against 2.28 fails
 * with "undefined symbol: __isoc23_strtol".
 *
 * Provide WEAK forwarders to the classic functions. On an old-glibc link these
 * resolve the references; on a current glibc the real (strong) __isoc23_* are
 * picked instead and these are ignored -- so the file is safe in every build
 * (native gcc/glibc, zig/old-glibc, musl). The strto* functions are declared
 * locally so this file itself is not subject to the header redirect.
 */

extern long int strtol(const char *, char **, int);
extern long long int strtoll(const char *, char **, int);
extern unsigned long int strtoul(const char *, char **, int);
extern unsigned long long int strtoull(const char *, char **, int);
extern double strtod(const char *, char **);
extern float strtof(const char *, char **);
extern long double strtold(const char *, char **);

__attribute__((weak)) long int __isoc23_strtol(const char *n, char **e, int b) { return strtol(n, e, b); }
__attribute__((weak)) long long int __isoc23_strtoll(const char *n, char **e, int b) { return strtoll(n, e, b); }
__attribute__((weak)) unsigned long int __isoc23_strtoul(const char *n, char **e, int b) { return strtoul(n, e, b); }
__attribute__((weak)) unsigned long long int __isoc23_strtoull(const char *n, char **e, int b) { return strtoull(n, e, b); }
