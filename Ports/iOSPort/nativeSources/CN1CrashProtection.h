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
 * iOS native crash protection helpers used by the Java
 * com.codename1.crash.CrashProtection runtime.
 *
 * Three plain C entry points; called from the ParparVM bridge that backs
 * the native methods declared on IOSNative.crashProtection*. Implementations
 * live in CN1CrashProtection.m. The matching Java side is
 *   CodenameOne/src/com/codename1/crash/CrashProtection.java
 * with platform glue in
 *   Ports/iOSPort/src/com/codename1/impl/ios/IOSImplementation.java
 *
 * Design notes:
 *
 * - cn1_crash_protection_install() registers signal + Mach exception +
 *   NSException handlers, and starts a stderr/NSLog ring-buffer. It's
 *   idempotent and safe to call before or after the existing
 *   installSignalHandlers() in CodenameOne_GLAppDelegate.m; both end up
 *   running on every signal because the new handler does its disk write
 *   first and then forwards to the previously-registered handler so the
 *   JVM-exception conversion path keeps working.
 *
 * - The signal handler itself uses ONLY async-signal-safe calls (write,
 *   open, close, _exit, backtrace, backtrace_symbols_fd). It writes a
 *   small text record to a pre-resolved file path inside the app's
 *   documents directory. backtrace_symbols (the malloc-using variant)
 *   is intentionally avoided.
 *
 * - cn1_crash_protection_consume_pending() reads that file on the next
 *   launch, deletes it, and returns the contents as an NSString. The
 *   Java side wraps the contents in a synthetic CrashReportPayload
 *   (exceptionClass=NativeCrash) and enqueues it for upload. Symbol
 *   resolution happens server-side using the uploaded dSYM, so the
 *   client only ships raw addresses + the image-slide info needed to
 *   compute offsets.
 *
 * - cn1_crash_protection_log_snapshot() returns a copy of the in-memory
 *   stderr ring buffer (capped at 32 KB). Attached to every regular
 *   crash payload as `nativeLog` so the developer can read the
 *   NSLog/os_log lines leading up to the failure.
 */

#ifndef CN1CrashProtection_h
#define CN1CrashProtection_h

#import <Foundation/Foundation.h>

/* Idempotent; safe to call multiple times. */
void cn1_crash_protection_install(void);

/* Returns a copy of the recent stderr/NSLog ring buffer, or nil if
 * empty / not yet installed. Caller owns the returned NSString. */
NSString * cn1_crash_protection_log_snapshot(void);

/* Returns the captured native crash evidence from the previous launch,
 * or nil if none. Deletes the underlying file before returning. */
NSString * cn1_crash_protection_consume_pending(void);

#endif /* CN1CrashProtection_h */
