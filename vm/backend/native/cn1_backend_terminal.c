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

#include "cn1_globals.h"
#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#endif

/* Whether a standard stream is a terminal; fd is 1 or 2. On Windows a console
   handle is one GetConsoleMode accepts -- a pipe or a file fails it. */
JAVA_INT com_codename1_cli_TerminalImpl_isTerminalImpl___int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT fd) {
#ifdef _WIN32
    DWORD mode;
    HANDLE h = GetStdHandle(fd == 2 ? STD_ERROR_HANDLE : STD_OUTPUT_HANDLE);
    return h != INVALID_HANDLE_VALUE && h != NULL && GetConsoleMode(h, &mode) ? 1 : 0;
#else
    return isatty(fd) ? 1 : 0;
#endif
}
