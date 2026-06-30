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
package com.codename1.sensors;

/// Receives raw readings from a [MotionSensor]. Register an implementation with
/// [MotionSensor#addListener(MotionSensorListener)]. Callbacks are always
/// invoked on the EDT so it is safe to update the UI directly from them.
public interface MotionSensorListener {

    /// Invoked when a new sensor reading is available.
    ///
    /// #### Parameters
    ///
    /// - `evt`: the reading; its axis values are interpreted according to the
    ///   sensor type as documented in [MotionEvent]
    void motionReceived(MotionEvent evt);
}
