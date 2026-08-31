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
/// The seam between the portable `com.codename1.call` API and each platform's
/// native call stack.
///
/// Application code never references anything here. [CallBridge] is
/// implemented by the ports, obtained by the public packages through
/// `com.codename1.ui.Display#getCallBridge()`, and the base implementation
/// returns `null` -- which is the capability query, and the reason the public
/// API degrades cleanly instead of throwing on a port that implements
/// nothing.
///
/// The interface's own documentation carries the rules that make it
/// implementable from Objective-C through ParparVM: primitives only, request
/// ids for asynchrony, and the requirement that every operation answers
/// exactly once.
package com.codename1.call.spi;
