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

/// Real-time threads: periodic work released on time, at a fixed operating-system
/// priority, that the garbage collector does not stop.
///
/// A [RealtimeTask] runs on its own [RealtimeThread]. Its `setup()` runs once and may
/// allocate; after it, every period wakes the thread and runs `cycle()`, which must
/// not allocate. That rule is what lets the thread run beside a collector: it is
/// checked when the application is built, and again while it runs. See [Realtime] for
/// the entry point, what each platform provides and what the guarantees are.
package com.codename1.realtime;
