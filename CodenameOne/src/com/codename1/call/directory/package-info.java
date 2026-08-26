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
/// Naming and blocking numbers, for calls this app has nothing to do with.
///
/// [CallDirectory] is the entry point. This is the caller-ID and
/// spam-blocking feature: an ordinary cellular call arrives and the system
/// asks the installed directories whether any of them recognises the number.
///
/// Deliberately **not** part of [com.codename1.call.session] and not a
/// superset of it. An app that only labels numbers never owns a call, so it
/// must not carry `MANAGE_OWN_CALLS` or a VoIP background mode -- and since
/// the build's opt-in is a package-name prefix that cannot express an
/// exclusion, the only way to say so is to be a different package.
package com.codename1.call.directory;
