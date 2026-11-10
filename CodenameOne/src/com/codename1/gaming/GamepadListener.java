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
package com.codename1.gaming;

/// Told about controllers coming and going and about their buttons. Register one
/// with `Gamepads#addListener(GamepadListener)`; every callback arrives on the EDT.
///
/// Axis movement is not reported here, because a stick reports dozens of values
/// a second: read `Gamepad#getAxis(int)` from the update loop instead.
public interface GamepadListener {
    /// A controller was connected.
    void gamepadConnected(Gamepad pad);

    /// A controller was disconnected; `Gamepad#isConnected()` is already false.
    void gamepadDisconnected(Gamepad pad);

    /// A button went down or up.
    ///
    /// #### Parameters
    ///
    /// - `pad`: the controller
    ///
    /// - `button`: one of the `Gamepad.BUTTON_*` constants
    ///
    /// - `down`: true when pressed, false when released
    ///
    /// #### Returns
    ///
    /// true to consume the event, which stops `Gamepads` from also turning it
    /// into focus navigation
    boolean gamepadButton(Gamepad pad, int button, boolean down);
}
