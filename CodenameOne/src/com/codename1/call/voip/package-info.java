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
/// Ringing when the app is not running.
///
/// [VoipPush] is the entry point, and its documentation carries the payload
/// contract a server has to honour. The one idea to take from it: on iOS the
/// call is reported to the operating system by native code **before any of
/// this app's code runs**, because the platform requires it and kills the app
/// otherwise. So a [PushedCall] is a call that is already ringing, not a
/// request to start ringing one.
///
/// This is a separate package from [com.codename1.call.session] because it
/// costs the `voip` background mode on iOS, and an app that carries that
/// without a working call implementation is rejected.
package com.codename1.call.voip;
