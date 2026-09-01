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

package com.codenameone.devruntime

/**
 * Presence of this file activates the `kotlin` Maven profile in
 * `common/pom.xml`, which pulls in `kotlin-stdlib`. The runtime app itself
 * has no Kotlin logic to run -- everything of substance is in Java -- but a
 * *pushed* Kotlin bundle records ordinary `kotlin.jvm.internal.Intrinsics`
 * calls (checkNotNullParameter at every method entry, checkNotNull for `!!`,
 * lambda helpers) as host externs. Without the stdlib compiled into this
 * app, the interpreter's linker cannot resolve those on the device and the
 * first Kotlin push fails at its first stdlib call -- which is every method
 * a Kotlin compiler emits.
 *
 * The file is deliberately trivial: the Maven profile's activation is
 * conditioned on `src/main/kotlin` existing, not on which .kt files are
 * inside it, so a marker is enough. Adding real Kotlin logic here would
 * work, but is not necessary and would only mean an entry point that never
 * runs.
 */
internal object KotlinBridge
