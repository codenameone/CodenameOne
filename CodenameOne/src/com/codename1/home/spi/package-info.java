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

/// The service-provider interface each platform port implements to back
/// [com.codename1.home.SmartHome].
///
/// **Application code never touches this.** Use `com.codename1.home`; the
/// bridge is obtained from
/// `com.codename1.ui.Display#getHomeBridge()`, and the base
/// implementation returns `null`, which is why the public API degrades to a
/// harmless no-op on ports with no smart-home support.
///
/// It is public rather than package-private because the ports live in
/// `com.codename1.impl.ios`, `com.codename1.impl.android` and
/// `com.codename1.impl.javase`.
package com.codename1.home.spi;
