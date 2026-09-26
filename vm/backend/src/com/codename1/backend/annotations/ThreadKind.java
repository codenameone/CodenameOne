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
package com.codename1.backend.annotations;

/// Which kind of thread runs background work: an [Async] method or a
/// [Scheduled] job.
///
/// A virtual thread is cheap to create and to switch, and many can wait on
/// sockets at once. But on this runtime only the SERVED socket parks a virtual
/// thread: a database query or an outbound HTTP call blocks the host thread
/// under it, and with it every other virtual thread that host is running. Work
/// that talks to a database belongs on a platform thread.
public enum ThreadKind {
    /// A virtual thread when this build and this server run them, a platform
    /// thread otherwise. The choice is logged once at start-up.
    AUTO,
    /// A virtual thread, falling back to a platform thread where there are none
    /// (the Java SE runtime, a TLS server, Windows).
    VIRTUAL,
    /// A thread of the executor's pool.
    PLATFORM;
}
