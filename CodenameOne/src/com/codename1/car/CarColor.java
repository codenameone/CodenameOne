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
package com.codename1.car;

/// A small, head-unit-safe colour palette for `CarAction` backgrounds and accents. In-car UIs do not
/// allow arbitrary ARGB colours (the system tints for legibility and theme/contrast compliance), so
/// the API exposes named roles that each platform maps to its own car colour
/// (`androidx.car.app.model.CarColor` / the CarPlay system tint).
public enum CarColor {

    /// The head unit's default colour for the element.
    DEFAULT,

    /// The app's primary/accent colour as resolved by the head unit theme.
    PRIMARY,

    /// A red role, typically for destructive or stop/cancel actions.
    RED,

    /// A green role, typically for confirm/go actions.
    GREEN,

    /// A blue role.
    BLUE,

    /// A yellow/amber role, typically for cautionary actions.
    YELLOW
}
