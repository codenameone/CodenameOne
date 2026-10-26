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
package com.codename1.desktopcompat.rt;

/// What Codename One knows about the keyboard and the pointer while an
/// input event is being delivered. [EventBridge] asks it for the modifiers
/// and the button of every AWT event it makes.
///
/// The default reads the display. Tests install their own with
/// [EventBridge#setInputState], because the headless implementation has
/// neither a keyboard nor mouse buttons.
public interface InputState {

    /// The keyboard modifiers held, as the extended masks of `InputEvent`
    /// (`SHIFT_DOWN_MASK`, `CTRL_DOWN_MASK`, `ALT_DOWN_MASK`,
    /// `META_DOWN_MASK`).
    int modifiers();

    /// The `MouseEvent` button of the pointer event being delivered:
    /// `BUTTON1`, `BUTTON2` or `BUTTON3`.
    int button();

    /// Whether the pointer is a finger or a stylus rather than a mouse. A
    /// long press is a popup trigger only then.
    boolean touch();
}
