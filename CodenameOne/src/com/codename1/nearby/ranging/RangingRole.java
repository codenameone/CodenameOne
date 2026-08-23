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
package com.codename1.nearby.ranging;

/// Which end of a UWB session this device is.
///
/// The distinction is real on Android, where the controller owns the
/// channel and session parameters and the controlee joins them, and it
/// decides which side has to publish a token first. It is invisible on iOS:
/// Nearby Interaction negotiates the roles itself, both peers publish a
/// discovery token, and the value passed here is ignored. Code that will run
/// on both should still pick a role -- one side controller, the other
/// controlee -- because that costs nothing on iOS and is required on
/// Android.
public enum RangingRole {
    /// This device chooses the channel and session parameters and publishes
    /// them; peers join. Exactly one side of a session is the controller.
    CONTROLLER,

    /// This device joins a session whose parameters the controller chose.
    CONTROLEE
}
