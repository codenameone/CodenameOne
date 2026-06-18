/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/*
 * Linux native crash protection -- the ELF-binary analog of
 * Ports/iOSPort/nativeSources/CN1CrashProtection.{h,m}. Same three
 * entry points, same on-disk record format (CN1NATIVECRASH v1) so the
 * BuildCloud NativeStackParser handles all desktop ports through one
 * code path.
 *
 * Implementation notes:
 *
 * - sigaction()-installed handlers for SIGSEGV/SIGABRT/SIGBUS/SIGILL/
 *   SIGFPE/SIGTRAP/SIGPIPE. The handler runs ONLY async-signal-safe
 *   calls (write, open, close, backtrace, backtrace_symbols_fd, _exit
 *   etc) and chains to the previous handler so any other library
 *   that installed one still runs.
 *
 * - dladdr(&__executable_start, &info) gives us the load base of the
 *   main ELF -- equivalent of the iOS dyld slide. We write that as
 *   LOAD_BASE in the record. The server's llvm-symbolizer call uses
 *   the offset (addr - load_base) so PIE / ASLR doesn't break
 *   resolution.
 *
 * - stderr ring buffer via pipe() + dup2() + a small reader thread.
 *   GTK / Cairo / glib are quite chatty on stderr so the dev sees
 *   the messages leading up to the crash in the issue body.
 */

#ifndef cn1_linux_crash_protection_h
#define cn1_linux_crash_protection_h

void cn1_crash_protection_install(void);
char * cn1_crash_protection_log_snapshot(void);   /* malloc; caller frees */
char * cn1_crash_protection_consume_pending(void); /* malloc; caller frees */

#endif /* cn1_linux_crash_protection_h */
